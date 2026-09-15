#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext

logger = logging.getLogger(__name__)

class SlackBusAngleBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        # each synchronous component needs its own angle reference
        for component, buses_in_component in network_cache.buses.groupby('synchronous_component'):
            declared_in_component = network_cache.slack_terminal[network_cache.slack_terminal.bus_id.isin(buses_in_component.index)]
            if declared_in_component.empty:
                slack_bus_id = buses_in_component.index[0]
            else:
                slack_bus_id = declared_in_component.iloc[0].bus_id
            slack_bus_num = network_cache.buses.index.get_loc(slack_bus_id)
            model.set_variable_bounds(variable_context.ph_vars[slack_bus_num], 0.0, 0.0)
            logger.info(f"Angle reference for synchronous component {component} is at bus '{slack_bus_id}' (num={slack_bus_num})")
