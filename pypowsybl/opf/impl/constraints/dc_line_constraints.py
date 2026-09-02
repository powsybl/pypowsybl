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


class DcLineConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache, variable_context: VariableContext,
            model: Model) -> None:

        for dc_line_num, dc_line_row in enumerate(network_cache.dc_lines.itertuples(index=False)):
            dc_node1_id, dc_node2_id, r = dc_line_row.dc_node1_id, dc_line_row.dc_node2_id, dc_line_row.r
            dc_node1_num = network_cache.dc_nodes.index.get_loc(dc_node1_id)
            dc_node2_num = network_cache.dc_nodes.index.get_loc(dc_node2_id)
            dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]

            v1_var = variable_context.v_dc_vars[dc_node1_num]
            v2_var = variable_context.v_dc_vars[dc_node2_num]
            i_var = variable_context.closed_dc_line_i_vars[dc_line_index]

            # i is oriented from dc_node1 to dc_node2.
            i_eq = (v1_var - v2_var) / r - i_var

            model.add_linear_constraint(i_eq, poi.Eq, 0.0)
