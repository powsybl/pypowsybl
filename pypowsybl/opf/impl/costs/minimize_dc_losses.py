# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pyoptinterface as poi
from pyoptinterface import ExprBuilder, nl

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
            i_var = variable_context.conv_i_vars[conv_index]
            # switching_loss*|i| is not a polynomial; needs its own variable tied by an nl constraint.
            conv_loss_var = model.add_m_variables(1, name=f'conv_loss_{conv_num}')[0]
            with nl.graph():
                i_dc_var = nl.abs(i_var)
                p_loss = conv_row.idle_loss + conv_row.switching_loss * i_dc_var + conv_row.resistive_loss * nl.pow(i_dc_var, 2)
                model.add_nl_constraint(conv_loss_var - p_loss, poi.Eq, 0.0)
            cost += conv_loss_var
        return cost
