from pyoptinterface import ipopt, nlfunc

from pypowsybl.opf.impl.model.ac_constraints import AcConstraints
from pypowsybl.opf.impl.model.ac_function_context import AcFunctionContext
from pypowsybl.opf.impl.model.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.model.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class ShuntFlowConstraints(AcConstraints):
    def add(self, parameters: AcOptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: AcVariableContext, function_context: AcFunctionContext, model: ipopt.Model) -> None:
        for shunt_num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            g, b, bus_id = row.g, row.b, row.bus_id
            if bus_id:
                shunt_index = variable_context.shunt_num_2_index[shunt_num]
                p_var = variable_context.shunt_p_vars[shunt_index]
                q_var = variable_context.shunt_q_vars[shunt_index]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = variable_context.v_vars[bus_num]
                model.add_nl_constraint(
                    function_context.sf_index,
                    vars=nlfunc.Vars(
                        v=v_var,
                        p=p_var,
                        q=q_var,
                    ),
                    params=nlfunc.Params(
                        g=g,
                        b=b,
                    ),
                    eq=0.0,
                )
