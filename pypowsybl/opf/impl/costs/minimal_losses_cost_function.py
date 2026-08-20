#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MinimalLossesCostFunction(CostFunction):
    def __init__(self) -> None:
        super().__init__('Minimal losses power')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for branch_index in range(len(variable_context.closed_branch_p1_vars)):
            cost += variable_context.closed_branch_p1_vars[branch_index] - variable_context.closed_branch_p2_vars[branch_index]
        return cost
