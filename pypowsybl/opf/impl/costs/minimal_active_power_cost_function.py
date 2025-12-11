import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MinimalActivePowerCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimal active power')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for gen_num in range(len(variable_context.gen_p_vars)):
            a, b, c = 0, 1.0, 0  # TODO
            cost += a * variable_context.gen_p_vars[gen_num] * variable_context.gen_p_vars[gen_num] + b * variable_context.gen_p_vars[gen_num] + c
        return cost
