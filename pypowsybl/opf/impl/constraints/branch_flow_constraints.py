from math import hypot, atan2

from pyoptinterface import ipopt, nl

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache

R2 = 1.0
A2 = 0.0

class BranchFlowConstraints(Constraints):
    @staticmethod
    def add_closed_branch_constraint(a1: float, b1: float, b2: float, g1: float, g2: float, ksi: float, model, p1_var, p2_var, ph1_var,
                                     ph2_var, q1_var, q2_var, r1: float, v1_var, v2_var, y: float):
        with nl.graph():
            sin_ksi = nl.sin(ksi)
            cos_ksi = nl.cos(ksi)
            theta1 = ksi - a1 + A2 - ph1_var + ph2_var
            theta2 = ksi + a1 - A2 + ph1_var - ph2_var
            sin_theta1 = nl.sin(theta1)
            cos_theta1 = nl.cos(theta1)
            sin_theta2 = nl.sin(theta2)
            cos_theta2 = nl.cos(theta2)

            p1_eq = r1 * v1_var * (g1 * r1 * v1_var + y * r1 * v1_var * sin_ksi - y * R2 * v2_var * sin_theta1) - p1_var
            q1_eq = r1 * v1_var * (-b1 * r1 * v1_var + y * r1 * v1_var * cos_ksi - y * R2 * v2_var * cos_theta1) - q1_var
            p2_eq = R2 * v2_var * (g2 * R2 * v2_var - y * r1 * v1_var * sin_theta2 + y * R2 * v2_var * sin_ksi) - p2_var
            q2_eq = R2 * v2_var * (-b2 * R2 * v2_var - y * r1 * v1_var * cos_theta2 + y * R2 * v2_var * cos_ksi) - q2_var

            model.add_nl_constraint(p1_eq == 0.0)
            model.add_nl_constraint(q1_eq == 0.0)
            model.add_nl_constraint(p2_eq == 0.0)
            model.add_nl_constraint(q2_eq == 0.0)

    @staticmethod
    def add_open_side1_branch_constraint(b1: float, b2: float, g1: float, g2: float, ksi: float, model, p2_var, q2_var, v2_var,
                                         y: float):
        with nl.graph():
            sin_ksi = nl.sin(ksi)
            cos_ksi = nl.cos(ksi)

            shunt = (g1 + y * sin_ksi) * (g1 + y * sin_ksi) + (-b1 + y * cos_ksi) * (-b1 + y * cos_ksi)
            p2_eq = R2 * R2 * v2_var * v2_var * (
                        g2 + y * y * g1 / shunt + (b1 * b1 + g1 * g1) * y * sin_ksi / shunt) - p2_var
            q2_eq = -R2 * R2 * v2_var * v2_var * (
                        b2 + y * y * b1 / shunt - (b1 * b1 + g1 * g1) * y * cos_ksi / shunt) - q2_var

            model.add_nl_constraint(p2_eq == 0.0)
            model.add_nl_constraint(q2_eq == 0.0)

    @staticmethod
    def add_open_side2_branch_constraint(b1: float, b2: float, g1: float, g2: float, ksi: float, model, p1_var, q1_var,
                                         r1: float, v1_var, y: float):
        with nl.graph():
            sin_ksi = nl.sin(ksi)
            cos_ksi = nl.cos(ksi)

            shunt = (g2 + y * sin_ksi) * (g2 + y * sin_ksi) + (-b2 + y * cos_ksi) * (-b2 + y * cos_ksi)
            p1_eq = r1 * r1 * v1_var * v1_var * (
                        g1 + y * y * g2 / shunt + (b2 * b2 + g2 * g2) * y * sin_ksi / shunt) - p1_var
            q1_eq = -r1 * r1 * v1_var * v1_var * (
                        b1 + y * y * b2 / shunt - (b2 * b2 + g2 * g2) * y * cos_ksi / shunt) - q1_var

            model.add_nl_constraint(p1_eq == 0.0)
            model.add_nl_constraint(q1_eq == 0.0)

    @staticmethod
    def add_branch_constraint(branch_index: int, bus1_id: str, bus2_id: str, network_cache: NetworkCache, model,
                              r: float, x: float, g1: float, b1: float, g2: float, b2: float, r1: float, a1: float,
                              variable_context: VariableContext) -> None:
        z = hypot(r, x)
        y = 1.0 / z
        ksi = atan2(r, x)

        if bus1_id and bus2_id:
            bus1_num = network_cache.buses.index.get_loc(bus1_id)
            bus2_num = network_cache.buses.index.get_loc(bus2_id)
            v1_var = variable_context.v_vars[bus1_num]
            v2_var = variable_context.v_vars[bus2_num]
            ph1_var = variable_context.ph_vars[bus1_num]
            ph2_var = variable_context.ph_vars[bus2_num]
            p1_var = variable_context.closed_branch_p1_vars[branch_index]
            q1_var = variable_context.closed_branch_q1_vars[branch_index]
            p2_var = variable_context.closed_branch_p2_vars[branch_index]
            q2_var = variable_context.closed_branch_q2_vars[branch_index]

            BranchFlowConstraints.add_closed_branch_constraint(a1, b1, b2, g1, g2, ksi, model, p1_var, p2_var,
                                                               ph1_var, ph2_var, q1_var, q2_var, r1, v1_var, v2_var, y)
        elif bus2_id:
            bus2_num = network_cache.buses.index.get_loc(bus2_id)
            v2_var = variable_context.v_vars[bus2_num]
            p2_var = variable_context.open_side1_branch_p2_vars[branch_index]
            q2_var = variable_context.open_side1_branch_q2_vars[branch_index]

            BranchFlowConstraints.add_open_side1_branch_constraint(b1, b2, g1, g2, ksi, model, p2_var, q2_var, v2_var, y)
        elif bus1_id:
            bus1_num = network_cache.buses.index.get_loc(bus1_id)
            v1_var = variable_context.v_vars[bus1_num]
            p1_var = variable_context.open_side2_branch_p1_vars[branch_index]
            q1_var = variable_context.open_side2_branch_q1_vars[branch_index]

            BranchFlowConstraints.add_open_side2_branch_constraint(b1, b2, g1, g2, ksi, model, p1_var, q1_var, r1,
                                                                   v1_var, y)

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for branch_num, row in enumerate(network_cache.lines.itertuples(index=False)):
            r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
            r1 = 1.0
            a1 = 0.0
            branch_index = variable_context.branch_num_2_index[branch_num]
            self.add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                       r, x, g1, b1, g2, b2, r1, a1,
                                       variable_context)

        for transfo_num, row in enumerate(network_cache.transformers_2w.itertuples(index=True)):
            r, x, g, b, rho, alpha = row.r_at_current_tap, row.x_at_current_tap, row.g_at_current_tap, row.b_at_current_tap, row.rho, row.alpha
            if parameters.twt_split_shunt_admittance:
                g1 = g / 2
                g2 = g / 2
                b1 = b / 2
                b2 = b / 2
            else:
                g1 = g
                g2 = 0
                b1 = b
                b2 = 0
            r1 = rho
            a1 = alpha
            branch_num = len(network_cache.lines) + transfo_num
            branch_index = variable_context.branch_num_2_index[branch_num]
            self.add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                       r, x, g1, b1, g2, b2, r1, a1,
                                       variable_context)

