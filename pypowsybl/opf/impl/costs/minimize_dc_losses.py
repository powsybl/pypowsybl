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
            p_line_loss = dc_line_row.r * variable_context.closed_dc_line_i1_vars[dc_line_num]*variable_context.closed_dc_line_i1_vars[dc_line_num]
            cost+= p_line_loss
        # for conv_num, conv_row in enumerate(network_cache.voltage_source_converters.itertuples()):
        #     if conv_row.bus_id:
        #         conv_q_var = variable_context.conv_q_vars[conv_num]
        #         conv_p_var = variable_context.conv_p_vars[conv_num]
        #         cost += conv_q_var * conv_q_var + conv_p_var * conv_p_var
        #         if conv_row.dc_connected1 and conv_row.dc_connected2:
        #             dc_node1_num = network_cache.dc_nodes.index.get_loc(conv_row.dc_node1_id)
        #             dc_node2_num = network_cache.dc_nodes.index.get_loc(conv_row.dc_node2_id)
        #             v1_var = variable_context.v_dc_vars[dc_node1_num]
        #             v2_var = variable_context.v_dc_vars[dc_node2_num]
        #             cost += (v1_var - v2_var) * (v1_var - v2_var)
        #         elif conv_row.dc_connected1:
        #             dc_node1_num = network_cache.dc_nodes.index.get_loc(conv_row.dc_node1_id)
        #             v1_var = variable_context.v_dc_vars[dc_node1_num]
        #             cost += v1_var * v1_var
        return cost
