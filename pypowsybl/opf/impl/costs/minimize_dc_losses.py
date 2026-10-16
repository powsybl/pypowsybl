# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MinimizeDcLossesFunction(CostFunction):
    def __init__(self) -> None:
        super().__init__('Minimal Dc Losses')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext, model: Model) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for dc_line_num, dc_line_row in enumerate(network_cache.dc_lines.itertuples()):
            dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]
            if dc_line_index == -1:
                continue
            i_var = variable_context.closed_dc_line_i_vars[dc_line_index]
            cost += dc_line_row.r * i_var**2

        for conv_num, conv_row in enumerate(network_cache.voltage_source_converters.itertuples()):
            conv_index = variable_context.conv_num_2_index[conv_num]
            if conv_index == -1:
                continue
            p_var = variable_context.conv_p_vars[conv_index]
            i_var = variable_context.conv_i_vars[conv_index]
            v1_var = variable_context.v_dc_vars[network_cache.dc_nodes.index.get_loc(conv_row.dc_node1_id)]
            v2_var = variable_context.v_dc_vars[network_cache.dc_nodes.index.get_loc(conv_row.dc_node2_id)]
            # Converter losses read from the power balance constraint (P_loss = P_AC + P_DC), which keeps the cost quadratic.
            cost += p_var + i_var * (v1_var - v2_var)
        return cost
