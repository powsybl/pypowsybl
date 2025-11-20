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

class DcGroundConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for row in network_cache.dc_grounds.itertuples(index=False):
            with nl.graph():
                dc_node_num = network_cache.dc_nodes.index.get_loc(row.dc_node_id)
                v_var = variable_context.v_dc_vars[dc_node_num]
                model.add_nl_constraint(v_var == 0.0)