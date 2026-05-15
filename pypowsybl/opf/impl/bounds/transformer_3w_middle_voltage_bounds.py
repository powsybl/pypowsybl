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
from pypowsybl.opf.impl.util import TRACE_LEVEL, Transformer3WRow

logger = logging.getLogger(__name__)


class Transformer3wMiddleVoltageBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for t3_num, t3_row in enumerate(cast(list[Transformer3WRow], network_cache.transformers_3w.itertuples())):
            if t3_row.bus1_id or t3_row.bus2_id or t3_row.bus3_id:
                v_bounds = Bounds.get_voltage_bounds(None, None, parameters.default_voltage_bounds)
                logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to 3 windings transformer middle '{t3_row.Index}' (num={t3_num})'")
                t3_index = variable_context.t3_num_2_index[t3_num]
                model.set_variable_bounds(variable_context.t3_middle_v_vars[t3_index],
                                          *Bounds.fix(t3_row.Index, v_bounds.min_value, v_bounds.max_value))
                model.set_variable_start(variable_context.t3_middle_ph_vars[t3_index], 1.0)
