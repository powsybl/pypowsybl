import pyoptinterface as poi
from pyoptinterface import ipopt

from pypowsybl.opf.impl.ac_constraints import AcConstraints
from pypowsybl.opf.impl.ac_function_context import AcFunctionContext
from pypowsybl.opf.impl.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.network_cache import NetworkCache


class StaticVarCompensatorReactiveLimitsConstraints(AcConstraints):
    def add(self, parameters: AcOptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: AcVariableContext, function_context: AcFunctionContext, model: ipopt.Model) -> None:
        for svc_num, row in enumerate(network_cache.static_var_compensators.itertuples(index=False)):
            b_min, b_max, bus_id = row.b_min, row.b_max, row.bus_id
            if bus_id:
                svc_index = variable_context.svc_num_2_index[svc_num]
                q_var = variable_context.svc_q_vars[svc_index]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = variable_context.v_vars[bus_num]
                q_min_expr = poi.ExprBuilder()
                q_min_expr += b_min * v_var * v_var
                q_min_expr -= q_var
                model.add_quadratic_constraint(q_min_expr, poi.Leq, 0.0)
                q_max_expr = poi.ExprBuilder()
                q_max_expr += b_max * v_var * v_var
                q_max_expr -= q_var
                model.add_quadratic_constraint(q_max_expr, poi.Geq, 0.0)
