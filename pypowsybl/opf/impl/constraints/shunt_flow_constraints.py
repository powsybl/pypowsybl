from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class ShuntFlowConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for shunt_num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            g, b, bus_id = row.g, row.b, row.bus_id
            if bus_id:
                shunt_index = variable_context.shunt_num_2_index[shunt_num]
                p_var = variable_context.shunt_p_vars[shunt_index]
                q_var = variable_context.shunt_q_vars[shunt_index]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = variable_context.v_vars[bus_num]

                p_eq = g * v_var * v_var - p_var
                q_eq = -b * v_var * v_var - q_var

                model.add_quadratic_constraint(p_eq == 0.0)
                model.add_quadratic_constraint(q_eq == 0.0)
