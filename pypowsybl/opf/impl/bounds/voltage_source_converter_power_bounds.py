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

#TODO implement max P, Q in converter core modelization
MAX_CONVERTER_P = 100.0
MAX_CONVERTER_Q = 100.0


class VoltageSourceConverterPowerBounds(VariableBounds):

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for converter_num, row in enumerate(network_cache.voltage_source_converters.itertuples()):
            converter_index = variable_context.conv_num_2_index[converter_num]
            if converter_index == -1:
                continue

            logger.log(TRACE_LEVEL,
                       f"Add active power bounds [{-MAX_CONVERTER_P}, {MAX_CONVERTER_P}] to converter '{row.Index}' (num={converter_num})")

            model.set_variable_bounds(variable_context.conv_p_vars[converter_index], -MAX_CONVERTER_P, MAX_CONVERTER_P)

            logger.log(TRACE_LEVEL,
                       f"Add reactive power bounds [{-MAX_CONVERTER_Q}, {MAX_CONVERTER_Q}] to converter '{row.Index}' (num={converter_num})")
            model.set_variable_bounds(variable_context.conv_q_vars[converter_index], -MAX_CONVERTER_Q, MAX_CONVERTER_Q)
