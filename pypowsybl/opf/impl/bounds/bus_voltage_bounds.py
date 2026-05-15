#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging
from typing import cast

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL, BusRow

logger = logging.getLogger(__name__)


class BusVoltageBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for bus_num, row in enumerate(cast(list[BusRow], network_cache.buses.itertuples())):
            v_bounds = Bounds.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit, parameters.default_voltage_bounds)
            logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to bus '{row.Index}' (num={bus_num})'")
            model.set_variable_bounds(variable_context.v_vars[bus_num],
                                      *Bounds.fix(row.Index, v_bounds.min_value, v_bounds.max_value))
            model.set_variable_start(variable_context.v_vars[bus_num], 1.0)
