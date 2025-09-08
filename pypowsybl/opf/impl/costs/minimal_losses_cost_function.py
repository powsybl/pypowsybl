import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MinimalLossesCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimal losses power')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for branch_index in range(len(variable_context.closed_branch_p1_vars)):
            cost += variable_context.closed_branch_p1_vars[branch_index] - variable_context.closed_branch_p2_vars[branch_index]
        return cost

