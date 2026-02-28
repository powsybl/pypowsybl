import logging

import pyoptinterface as poi
import numpy as np
from scipy.spatial import ConvexHull

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds

logger = logging.getLogger(__name__)


class ReactiveCapabilityCurveConstraints(Constraints):

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for element_id, points_df in network_cache.reactive_capability_curve_points.groupby(level='id'):
            # find corresponding element
            if points_df.empty:
                continue
                
            element_type = points_df['element_type'].iloc[0]
            if element_type == 'GENERATOR':
                gen_num = network_cache.generators.index.get_loc(element_id)
                gen_p_index = variable_context.gen_p_num_2_index[gen_num]
                gen_q_index = variable_context.gen_q_num_2_index[gen_num]
                if gen_q_index == -1:
                    continue
                p = variable_context.gen_p_vars[gen_p_index]
                q = variable_context.gen_q_vars[gen_q_index]
            elif element_type == 'VSC':
                vsc_num = network_cache.vsc_converter_stations.index.get_loc(element_id)
                vsc_p_index = variable_context.vsc_cs_num_2_index[vsc_num]
                vsc_q_index = variable_context.vsc_cs_num_2_index[vsc_num]
                if vsc_q_index == -1:
                    continue
                p = variable_context.vsc_cs_p_vars[vsc_p_index]
                q = variable_context.vsc_cs_q_vars[vsc_q_index]
            else:
                raise ValueError(f"Unknown element type: {element_type}")

            # extract points (p, min_q) and (p, max_q) to form the boundary
            all_points_list = []
            for _, point_row in points_df.iterrows():
                q_bounds = Bounds(point_row['min_q'], point_row['max_q']).reduce(parameters.reactive_bounds_reduction).mirror()
                fixed_q_bounds = Bounds.fix(str(element_id), q_bounds.min_value, q_bounds.max_value)
                all_points_list.append([point_row['p'], fixed_q_bounds[0]])
                all_points_list.append([point_row['p'], fixed_q_bounds[1]])

            # combine them to form the set of points for the hull
            all_points = np.array(all_points_list)

            # remove duplicates
            all_points = np.unique(all_points, axis=0)

            # calculate convex hull
            hull = ConvexHull(all_points)

            # check if the original set of points is convex
            # we use a small tolerance for the check as we only care if the points
            # significantly differ from the hull. Note: aligned points are not hull vertices.
            if len(hull.vertices) < len(all_points):
                # find points that are not vertices
                is_vertex = np.zeros(len(all_points), dtype=bool)
                is_vertex[hull.vertices] = True
                non_vertices = all_points[~is_vertex]

                redundant_count = 0
                non_convex_count = 0
                for p_non_v in non_vertices:
                    on_boundary = False
                    for eq in hull.equations:
                        dist = np.dot(eq[:-1], p_non_v) + eq[-1]
                        if np.isclose(dist, 0, atol=1e-8):
                            on_boundary = True
                            break
                    if on_boundary:
                        redundant_count += 1
                    else:
                        non_convex_count += 1

                if non_convex_count > 0:
                    logger.warning(f"Capability curve for '{element_id}' is NON-CONVEX. "
                                   f"It has {non_convex_count} points strictly inside the convex hull. "
                                   f"The OPF will use the convexified region.")
                if redundant_count > 0:
                    logger.debug(f"Capability curve for '{element_id}' has {redundant_count} redundant (collinear) points on the boundary.")

            # hull.equations is an array of [A, B, C] where Ax + By + C <= 0
            # which is Ax + By <= -C
            for eq in hull.equations:
                a, b, c = eq
                expr = poi.ExprBuilder()
                expr += a * p + b * q
                model.add_linear_constraint(expr, poi.Leq, float(-c))
