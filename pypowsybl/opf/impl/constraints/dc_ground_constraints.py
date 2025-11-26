# Copyright (c) 2025, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pyoptinterface as poi
from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class DcGroundConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache, variable_context: VariableContext,
            model: ipopt.Model) -> None:
        for row in network_cache.dc_grounds.itertuples(index=False):
            dc_node_num = network_cache.dc_nodes.index.get_loc(row.dc_node_id)
            v_var = variable_context.v_dc_vars[dc_node_num]
            model.add_linear_constraint(v_var, poi.Eq, 0.0)
