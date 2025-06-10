import pyoptinterface as poi
from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.function_context import FunctionContext
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class CurrentLimitConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, function_context: FunctionContext, model: ipopt.Model) -> None:
        current_limits = network_cache.current_limits
        for branch_num, branch_row in enumerate(network_cache.lines.itertuples()):
            if branch_row.Index in current_limits.index:
                limit_row = current_limits.loc[branch_row.Index]
                branch_index = variable_context.branch_num_2_index[branch_num]
                if branch_row.bus1_id and branch_row.bus2_id:
                    p1 = variable_context.closed_branch_p1_vars[branch_index]
                    q1 = variable_context.closed_branch_q1_vars[branch_index]
                    i1 = poi.ExprBuilder()
                    i1 += p1 * p1 + q1 * q1
                    model.add_quadratic_constraint(i1, poi.Leq, limit_row.value * limit_row.value)
