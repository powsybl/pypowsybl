# Copyright (c) 2025, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class DcNodeVoltageBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        dc_node_count = len(network_cache.dc_nodes)
        grounded_node_ids = {row.dc_node_id for row in network_cache.dc_grounds.itertuples(index=False)}
        for dc_node_num, row in enumerate(network_cache.dc_nodes.itertuples()):
            #TODO add voltage limits in DC node core modelization
            low_voltage_limit = -2.0
            high_voltage_limit = 2.0
            v_bounds = Bounds(low_voltage_limit, high_voltage_limit)
            logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to dc_node '{row.Index}' (num={dc_node_num})'")
            model.set_variable_bounds(variable_context.v_dc_vars[dc_node_num],
                                      *Bounds.fix(row.Index, v_bounds.min_value, v_bounds.max_value))
            # "1e-4 * (dc_node_num - (dc_node_count-1)/2)" : centered, deterministic and guaranteed functional initialization for nonlinear VSC coupling constraint (conv_i * |v1-v2|) to avoir v1=V2 at start which stops solver at first iteration
            start_v_ungrounded_dc_node= 1.0 + 1e-4 * (dc_node_num - (dc_node_count-1)/2)
            # initializating grounded nodes at 0 makes ipopt solver converge in fewer iterations than initializing them all using start_v_ungrounded_dc_node.
            start_v = 0.0 if row.Index in grounded_node_ids else start_v_ungrounded_dc_node
            model.set_variable_start(variable_context.v_dc_vars[dc_node_num], start_v)

            