from math import hypot, atan2

from pyoptinterface import ipopt, nlfunc

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.function_context import FunctionContext
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class DanglingLineFlowConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, function_context: FunctionContext,
            model: ipopt.Model) -> None:
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
