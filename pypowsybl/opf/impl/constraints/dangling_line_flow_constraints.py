from math import hypot, atan2

from pyoptinterface import ipopt

from pypowsybl.opf.impl.constraints.branch_flow_constraints import R2, A2, BranchFlowConstraints
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class DanglingLineFlowConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for dl_num, row in enumerate(network_cache.dangling_lines.itertuples(index=False)):
            r, x, g, b, bus_id = row.r, row.x, row.g, row.b, row.bus_id
            if bus_id:
                g1 = g
                b1 = b
                g2 = 0
                b2 = 0
                r1 = 1.0
                a1 = 0.0
                dl_index = variable_context.dl_num_2_index[dl_num]
                z = hypot(r, x)
                y = 1.0 / z
                ksi = atan2(r, x)
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v1_var = variable_context.v_vars[bus_num]
                ph1_var = variable_context.ph_vars[bus_num]
                v2_var = variable_context.dl_v_vars[dl_index]
                ph2_var = variable_context.dl_ph_vars[dl_index]
                p1_var = variable_context.dl_branch_p1_vars[dl_index]
                q1_var = variable_context.dl_branch_q1_vars[dl_index]
                p2_var = variable_context.dl_branch_p2_vars[dl_index]
                q2_var = variable_context.dl_branch_q2_vars[dl_index]

                BranchFlowConstraints.add_closed_branch_constraint(a1, b1, b2, g1, g2, ksi, model, p1_var, p2_var, ph1_var, ph2_var, q1_var, q2_var, r1, v1_var, v2_var, y)
