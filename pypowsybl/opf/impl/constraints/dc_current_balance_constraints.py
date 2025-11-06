import pyoptinterface as poi
from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class DcCurrentBalanceConstraints(Constraints):

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for dc_node_expr in self.create_dc_node_expr_list(network_cache, variable_context):
            model.add_linear_constraint(dc_node_expr, poi.Eq, 0.0)


    class DcNodesBalance:
        def __init__(self, dc_node_count: int):
            self.i_in = [[] for _ in range(dc_node_count)]
            self.i_out = [0.0 for _ in range(dc_node_count)]

        @staticmethod
        def _to_expr(dc_node_i_in: list, dc_node_i_out: list) -> list[poi.ExprBuilder]:
            return [poi.ExprBuilder() + poi.quicksum(i_in) - i_out
                    for i_in, i_out in zip(dc_node_i_in, dc_node_i_out)]

        def to_expr(self) -> list[poi.ExprBuilder]:
            return self._to_expr(self.i_in, self.i_out)


    @classmethod
    def create_dc_node_expr_list(cls, network_cache, variable_context):
        dc_node_grounded_ids = [row.dc_node_id for row in network_cache.dc_grounds.itertuples(index=False)]
        dc_nodes_balance = cls.DcNodesBalance(len(network_cache.dc_nodes))

        # dc_lines
        for dc_line_num, row in enumerate(network_cache.dc_lines.itertuples(index=False)):
            dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]
            if row.dc_node1_id not in dc_node_grounded_ids:
                dc_node1_num = network_cache.dc_nodes.index.get_loc(row.dc_node1_id)
                dc_nodes_balance.i_in[dc_node1_num].append(variable_context.closed_dc_line_i1_vars[dc_line_index])
            if row.dc_node2_id not in dc_node_grounded_ids:
                dc_node2_num = network_cache.dc_nodes.index.get_loc(row.dc_node2_id)
                dc_nodes_balance.i_in[dc_node2_num].append(variable_context.closed_dc_line_i2_vars[dc_line_index])

        # voltage source converters
        for conv_num, row in enumerate(network_cache.voltage_source_converters.itertuples(index=False)):
            dc_node1_id = row.dc_node1_id
            dc_node2_id = row.dc_node2_id
            if row.dc_connected1:
                conv_index = variable_context.conv_num_2_index[conv_num]
                dc_node1_num = network_cache.dc_nodes.index.get_loc(dc_node1_id)
                dc_nodes_balance.i_in[dc_node1_num].append(variable_context.conv_i_vars[conv_index])
            if row.dc_connected2:
                conv_index = variable_context.conv_num_2_index[conv_num]
                dc_node2_num = network_cache.dc_nodes.index.get_loc(dc_node2_id)
                dc_nodes_balance.i_out[dc_node2_num] = variable_context.conv_i_vars[conv_index]

        return dc_nodes_balance.to_expr()
