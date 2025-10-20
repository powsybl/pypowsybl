import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MaximalDcVoltageCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Maximal Dc Voltage')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for num, row in enumerate(network_cache.dc_nodes.itertuples()):
            cost += variable_context.v_dc_vars[num]*variable_context.v_dc_vars[num]
        return -cost

