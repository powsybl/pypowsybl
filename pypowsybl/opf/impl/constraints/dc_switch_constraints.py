# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pyoptinterface as poi

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class DcSwitchConstraints(Constraints):
    """Ohm's law across each closed DC switch."""

    def add(self, parameters: ModelParameters, network_cache: NetworkCache, variable_context: VariableContext,
            model: Model) -> None:
        for dc_switch_num, dc_switch_row in enumerate(network_cache.dc_switches.itertuples(index=False)):
            dc_switch_index = variable_context.dc_switch_num_2_index[dc_switch_num]
            if dc_switch_index == -1:  # open switch, no current variable
                continue

            dc_node1_num = network_cache.dc_nodes.index.get_loc(dc_switch_row.dc_node1_id)
            dc_node2_num = network_cache.dc_nodes.index.get_loc(dc_switch_row.dc_node2_id)

            v1_var = variable_context.v_dc_vars[dc_node1_num]
            v2_var = variable_context.v_dc_vars[dc_node2_num]
            i_var = variable_context.closed_dc_switch_i_vars[dc_switch_index]

            # i is oriented dc_node1 -> dc_node2; DcCurrentBalanceConstraints matches it.
            # Not the (v1 - v2)/r - i = 0 form used by DcLineConstraints: a DC switch normally has
            # r = 0, which that form divides by. This one degrades to v1 == v2.
            model.add_linear_constraint(v1_var - v2_var - dc_switch_row.r * i_var, poi.Eq, 0.0)
