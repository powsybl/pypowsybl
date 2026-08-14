# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Any

import pyoptinterface as poi

from pypowsybl.opf.impl.model import network_cache
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class DcCurrentBalanceConstraints(Constraints):

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for expression in create_dc_current_balance_expressions(network_cache, variable_context):
            model.add_linear_constraint(expression, poi.Eq, 0.0)


def create_dc_current_balance_expressions(network_cache: NetworkCache,
                                           variable_context: VariableContext) -> list[poi.ExprBuilder]:

    grounded_dc_node_ids = set(network_cache.dc_grounds["dc_node_id"])  
    
    expressions_by_dc_node_id = {
        dc_node_id: poi.ExprBuilder()
        for dc_node_id in network_cache.dc_nodes.index
        if dc_node_id not in grounded_dc_node_ids
    }

    # Add current from DC lines to DC node current balance expression
    for dc_line_num, row in enumerate(network_cache.dc_lines.itertuples(index=False)):
        dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]
        if dc_line_index == -1:
            continue

        add_current_to_dc_node(expressions_by_dc_node_id,
                                str(row.dc_node1_id),
                                variable_context.closed_dc_line_i1_vars[dc_line_index])
        add_current_to_dc_node(expressions_by_dc_node_id,
                                str(row.dc_node2_id),
                                variable_context.closed_dc_line_i2_vars[dc_line_index])
        
    # Add current from converters to DC node current balance expression
    for converter_num, row in enumerate(network_cache.voltage_source_converters.itertuples(index=False)):
        converter_index = variable_context.conv_num_2_index[converter_num]
        if converter_index == -1:
            continue

        converter_current = variable_context.conv_i_vars[converter_index]

        add_current_to_dc_node(expressions_by_dc_node_id, str(row.dc_node1_id), converter_current)
        add_current_to_dc_node(expressions_by_dc_node_id, str(row.dc_node2_id), -converter_current)

    return list(expressions_by_dc_node_id.values())


def add_current_to_dc_node(expressions_by_dc_node_id: dict[str, poi.ExprBuilder],
                            dc_node_id: str,
                            current: Any) -> None:
    if dc_node_id not in expressions_by_dc_node_id:
        return

    expressions_by_dc_node_id[dc_node_id] = expressions_by_dc_node_id[dc_node_id] + current