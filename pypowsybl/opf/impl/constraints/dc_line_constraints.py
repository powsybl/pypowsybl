# Copyright (c) 2025, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pyoptinterface import ipopt, nl

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache

class DcLineConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for dc_line_num, dc_line_row in enumerate(network_cache.dc_lines.itertuples(index=False)):
            with nl.graph():
                dc_node1_id, dc_node2_id, r = dc_line_row.dc_node1_id, dc_line_row.dc_node2_id, dc_line_row.r
                dc_node1_num = network_cache.dc_nodes.index.get_loc(dc_node1_id)
                dc_node2_num = network_cache.dc_nodes.index.get_loc(dc_node2_id)
                dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]

                v1_var = variable_context.v_dc_vars[dc_node1_num]
                v2_var = variable_context.v_dc_vars[dc_node2_num]
                i1_var = variable_context.closed_dc_line_i1_vars[dc_line_index]
                i2_var = variable_context.closed_dc_line_i2_vars[dc_line_index]

                i1_eq = (v2_var - v1_var)/r - i1_var # By convention, the current injected in a node is positive
                i2_eq = (v1_var - v2_var)/r - i2_var

                model.add_nl_constraint(i1_eq == 0.0)
                model.add_nl_constraint(i2_eq == 0.0)