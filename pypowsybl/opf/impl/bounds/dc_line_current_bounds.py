# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

# TODO add current limits in DC line core modelization.
# I_base = S_base / V_base = 100 MVA / 500 kV = 0.2 kA. Until limits are implemented, 20 pu allows for 4 kA.
MIN_DC_CURRENT = -20.0
MAX_DC_CURRENT = 20.0
class DcLineCurrentBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for dc_line_num, row in enumerate(network_cache.dc_lines.itertuples()):

            dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]
            if dc_line_index == -1:
                continue

            logger.log(TRACE_LEVEL, f"Add current bounds: [{MIN_DC_CURRENT}, {MAX_DC_CURRENT}] to dc line '{row.Index}' (num={dc_line_num})'")

            model.set_variable_bounds(variable_context.closed_dc_line_i_vars[dc_line_index], MIN_DC_CURRENT, MAX_DC_CURRENT)
