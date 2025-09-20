from math import hypot, atan2

from pyoptinterface import ipopt

from pypowsybl.opf.impl.constraints.branch_flow_constraints import BranchFlowConstraints
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class Transformer3wFlowConstraints(Constraints):
    @staticmethod
    def _create_leg_constraint(leg_r: float, leg_x: float, leg_g: float, leg_b: float, rho: float, alpha: float,
                               bus_id: str, t3_index: int, leg_index: int, parameters: ModelParameters, network_cache: NetworkCache,
                               variable_context: VariableContext, model: ipopt.Model):
        r = leg_r
        x = leg_x
        if parameters.twt_split_shunt_admittance:
            g1 = leg_g / 2
            g2 = leg_g / 2
            b1 = leg_b / 2
            b2 = leg_b / 2
        else:
            g1 = leg_g
            g2 = 0.0
            b1 = leg_b
            b2 = 0.0
        r1 = rho
        a1 = alpha
        z = hypot(r, x)
        y = 1.0 / z
        ksi = atan2(r, x)
        if bus_id:
            bus_num = network_cache.buses.index.get_loc(bus_id)
            v1_var = variable_context.v_vars[bus_num]
            ph1_var = variable_context.ph_vars[bus_num]
            v2_var = variable_context.t3_middle_v_vars[t3_index]
            ph2_var = variable_context.t3_middle_ph_vars[t3_index]
            p1_var = variable_context.t3_closed_branch_p1_vars[leg_index]
            q1_var = variable_context.t3_closed_branch_q1_vars[leg_index]
            p2_var = variable_context.t3_closed_branch_p2_vars[leg_index]
            q2_var = variable_context.t3_closed_branch_q2_vars[leg_index]

            BranchFlowConstraints.add_closed_branch_constraint(a1, b1, b2, g1, g2, ksi, model, p1_var, p2_var, ph1_var, ph2_var, q1_var, q2_var, r1, v1_var, v2_var, y)
        else:
            v2_var = variable_context.t3_middle_v_vars[t3_index]
            ph2_var = variable_context.t3_middle_ph_vars[t3_index]
            p2_var = variable_context.t3_open_side1_branch_p2_vars[leg_index]
            q2_var = variable_context.t3_open_side1_branch_q2_vars[leg_index]
            BranchFlowConstraints.add_open_side1_branch_constraint(b1, b2, g1, g2, ksi, model, p2_var, q2_var, v2_var, y)

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for t3_num, row in enumerate(network_cache.transformers_3w.itertuples(index=False)):
            t3_index = variable_context.t3_num_2_index[t3_num]
            leg1_r, leg2_r, leg3_r = row.r1_at_current_tap, row.r2_at_current_tap, row.r3_at_current_tap
            leg1_x, leg2_x, leg3_x = row.x1_at_current_tap, row.x2_at_current_tap, row.x3_at_current_tap
            leg1_g, leg2_g, leg3_g = row.g1_at_current_tap, row.g2_at_current_tap, row.g3_at_current_tap
            leg1_b, leg2_b, leg3_b = row.b1_at_current_tap, row.b2_at_current_tap, row.b3_at_current_tap
            rho1, rho2, rho3 = row.rho1, row.rho2, row.rho3
            alpha1, alpha2, alpha3 = row.alpha1, row.alpha2, row.alpha3
            bus1_id, bus2_id, bus3_id = row.bus1_id, row.bus2_id, row.bus3_id
            if bus1_id or bus2_id or bus3_id:
                leg1_index = variable_context.t3_leg1_num_2_index[t3_num]
                leg2_index = variable_context.t3_leg2_num_2_index[t3_num]
                leg3_index = variable_context.t3_leg3_num_2_index[t3_num]
                self._create_leg_constraint(leg1_r, leg1_x, leg1_g, leg1_b, rho1, alpha1, bus1_id, t3_index, leg1_index, parameters, network_cache, variable_context, model)
                self._create_leg_constraint(leg2_r, leg2_x, leg2_g, leg2_b, rho2, alpha2, bus2_id, t3_index, leg2_index, parameters, network_cache, variable_context, model)
                self._create_leg_constraint(leg3_r, leg3_x, leg3_g, leg3_b, rho3, alpha3, bus3_id, t3_index, leg3_index, parameters, network_cache, variable_context, model)
