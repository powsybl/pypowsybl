from math import hypot, atan2

from pyoptinterface import ipopt, nlfunc

from pypowsybl.opf.impl.model.ac_constraints import AcConstraints
from pypowsybl.opf.impl.model.ac_function_context import AcFunctionContext
from pypowsybl.opf.impl.model.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.model.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class AcBranchFlowConstraints(AcConstraints):
    @staticmethod
    def _add_branch_constraint(branch_index: int, bus1_id: str, bus2_id: str, network_cache: NetworkCache, model,
                               r: float, x: float, g1: float, b1: float, g2: float, b2: float, r1: float, a1: float,
                               variable_context: AcVariableContext, function_context: AcFunctionContext) -> None:
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
            model.add_nl_constraint(
                function_context.cbf_index,
                vars=nlfunc.Vars(
                    v1=v1_var,
                    v2=v2_var,
                    ph1=ph1_var,
                    ph2=ph2_var,
                    p1=p1_var,
                    q1=q1_var,
                    p2=p2_var,
                    q2=q2_var
                ),
                params=nlfunc.Params(
                    y=y,
                    ksi=ksi,
                    g1=g1,
                    b1=b1,
                    g2=g2,
                    b2=b2,
                    r1=r1,
                    a1=a1
                ),
                eq=0.0,
            )
        elif bus2_id:
            bus2_num = network_cache.buses.index.get_loc(bus2_id)
            v2_var = variable_context.v_vars[bus2_num]
            ph2_var = variable_context.ph_vars[bus2_num]
            p2_var = variable_context.open_side1_branch_p2_vars[branch_index]
            q2_var = variable_context.open_side1_branch_q2_vars[branch_index]
            model.add_nl_constraint(
                function_context.o1bf_index,
                vars=nlfunc.Vars(
                    v2=v2_var,
                    ph2=ph2_var,
                    p2=p2_var,
                    q2=q2_var
                ),
                params=nlfunc.Params(
                    y=y,
                    ksi=ksi,
                    g1=g1,
                    b1=b1,
                    g2=g2,
                    b2=b2,
                ),
                eq=0.0,
            )
        elif bus1_id:
            bus1_num = network_cache.buses.index.get_loc(bus1_id)
            v1_var = variable_context.v_vars[bus1_num]
            ph1_var = variable_context.ph_vars[bus1_num]
            p1_var = variable_context.open_side2_branch_p1_vars[branch_index]
            q1_var = variable_context.open_side2_branch_q1_vars[branch_index]
            model.add_nl_constraint(
                function_context.o2bf_index,
                vars=nlfunc.Vars(
                    v1=v1_var,
                    ph1=ph1_var,
                    p1=p1_var,
                    q1=q1_var
                ),
                params=nlfunc.Params(
                    y=y,
                    ksi=ksi,
                    g1=g1,
                    b1=b1,
                    g2=g2,
                    b2=b2,
                    a1=a1,
                    r1=r1,
                ),
                eq=0.0,
            )

    def add(self, parameters: AcOptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: AcVariableContext, function_context: AcFunctionContext,
            model: ipopt.Model) -> None:
        for branch_num, row in enumerate(network_cache.lines.itertuples(index=False)):
            r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
            r1 = 1.0
            a1 = 0.0
            branch_index = variable_context.branch_num_2_index[branch_num]
            self._add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                        r, x, g1, b1, g2, b2, r1, a1,
                                        variable_context, function_context)

        for transfo_num, row in enumerate(network_cache.transformers.itertuples(index=False)):
            r, x, g, b, rho, alpha = row.r_tap, row.x_tap, row.g_tap, row.b_tap, row.rho, row.alpha
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
            self._add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                        r, x, g1, b1, g2, b2, r1, a1,
                                        variable_context, function_context)

