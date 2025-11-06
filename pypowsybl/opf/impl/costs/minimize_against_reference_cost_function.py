#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import cast

import pyoptinterface as poi
from pandas import DataFrame
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.util import ConverterStationRow


class MinimizeAgainstReferenceCostFunction(CostFunction):
    def __init__(self) -> None:
        super().__init__('Minimize against reference')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()

        cost += self._create_generators_cost(network_cache, variable_context)
        cost += self._create_batteries_cost(network_cache, variable_context)
        cost += self._create_vsc_converter_stations_cost(network_cache, variable_context)
        return cost

    @staticmethod
    def _create_generators_cost(network_cache: NetworkCache, variable_context: VariableContext) -> poi.ExprBuilder:
        res = poi.ExprBuilder()
        for gen_num, gen_row in enumerate(network_cache.generators.itertuples(index=False)):
            if gen_row.bus_id:
                gen_p_expr = poi.ExprBuilder()
                gen_p_expr += variable_context.gen_p_vars[gen_num]
                gen_p_expr += gen_row.target_p
                res += gen_p_expr * gen_p_expr
                if gen_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(gen_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    res += (v_var - gen_row.target_v) * (v_var - gen_row.target_v)
        return res

    @staticmethod
    def _create_batteries_cost(network_cache: NetworkCache, variable_context: VariableContext) -> poi.ExprBuilder:
        res = poi.ExprBuilder()
        for bat_num, bat_row in enumerate(network_cache.batteries.itertuples(index=False)):
            if bat_row.bus_id:
                bat_p_expr = poi.ExprBuilder()
                bat_p_expr += variable_context.bat_p_vars[bat_num]
                bat_p_expr += bat_row.target_p
                res += bat_p_expr * bat_p_expr
                if bat_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(bat_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    res += (v_var - bat_row.target_v) * (v_var - bat_row.target_v)
        return res

    @staticmethod
    def _create_vsc_converter_stations_cost(network_cache: NetworkCache,
                                            variable_context: VariableContext) -> poi.ExprBuilder:
        res = poi.ExprBuilder()
        for vsc_cs_num, vsc_cs_row in enumerate(cast(list[ConverterStationRow],
                                                     network_cache.vsc_converter_stations.itertuples())):
            if vsc_cs_row.bus_id:
                if NetworkCache.is_rectifier(vsc_cs_row.Index, vsc_cs_row):
                    vsc_cs_p_expr = poi.ExprBuilder()
                    vsc_cs_p_expr += variable_context.vsc_cs_p_vars[vsc_cs_num]
                    vsc_cs_p_expr -= vsc_cs_row.target_p
                    res += vsc_cs_p_expr * vsc_cs_p_expr
                if vsc_cs_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(vsc_cs_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    res += (v_var - vsc_cs_row.target_v) * (v_var - vsc_cs_row.target_v)
        for conv_num, conv_row in enumerate(network_cache.voltage_source_converters.itertuples()):
            if conv_row.bus_id:
                if conv_row.control_mode == "P_PCC":
                    conv_p_expr = poi.ExprBuilder()
                    conv_p_expr += variable_context.conv_p_vars[conv_num]
                    conv_p_expr -= conv_row.target_p
                    cost += conv_p_expr * conv_p_expr
                if conv_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(conv_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += (v_var - conv_row.target_v_ac) * (v_var - conv_row.target_v_ac)

        return res