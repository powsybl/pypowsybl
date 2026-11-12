# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging

from pypowsybl.opf.impl.model.dc_voltage_starts import compute_dc_node_voltage_starts
from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

# TODO add voltage limits in DC node core modelization
MIN_DC_VOLTAGE = -2.0
MAX_DC_VOLTAGE = 2.0


class DcNodeVoltageBounds(VariableBounds):

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        starts = compute_dc_node_voltage_starts(network_cache)
        unplaced_dc_node_ids = []

        for dc_node_num, row in enumerate(network_cache.dc_nodes.itertuples()):
            logger.log(TRACE_LEVEL,
                       f"Add voltage magnitude bounds [{MIN_DC_VOLTAGE}, {MAX_DC_VOLTAGE}] "
                       f"to dc_node '{row.Index}' (num={dc_node_num})")

            model.set_variable_bounds(variable_context.v_dc_vars[dc_node_num],
                                      MIN_DC_VOLTAGE, MAX_DC_VOLTAGE)

            start_v = starts.get(str(row.Index))
            if start_v is None:
                unplaced_dc_node_ids.append(str(row.Index))
                continue

            logger.log(TRACE_LEVEL,
                       f"Set start value {start_v} for voltage variable of dc node '{row.Index}' "
                       f"(num={dc_node_num})")
            model.set_variable_start(variable_context.v_dc_vars[dc_node_num], start_v)

        if unplaced_dc_node_ids:
            logger.warning(
                f"{len(unplaced_dc_node_ids)} DC nodes have no path to a converter voltage setpoint "
                f"or a ground, so they start from the solver default: {sorted(unplaced_dc_node_ids)}")
