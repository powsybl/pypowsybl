# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
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


class MinimizeDcLossesFunction(CostFunction):
    def __init__(self) -> None:
        super().__init__('Minimal Dc Losses')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        # By Tellegen's theorem, the AC power drawn by all converters equals what the DC system dissipates at any
        # feasible point: converter losses plus r*i^2 in every DC line and switch (grounds sit at 0 V).
        cost = poi.ExprBuilder()
        for conv_p_var in variable_context.conv_p_vars:
            cost += conv_p_var
        return cost
