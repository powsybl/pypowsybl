import pyoptinterface as poi
from pyoptinterface import ExprBuilder, nl

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MinimizeDcLossesFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimal Dc Losses')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for dc_line_num, dc_line_row in enumerate(network_cache.dc_lines.itertuples()):
            cost+= variable_context.closed_dc_line_i1_vars[dc_line_num]**2 + variable_context.closed_dc_line_i2_vars[dc_line_num]**2
        return cost
