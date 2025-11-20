#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging
import math
from dataclasses import dataclass
from typing import Any, cast

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL, HvdcRow

logger = logging.getLogger(__name__)


@dataclass
class VariableContext:
    v_vars: Any
    ph_vars: Any
    gen_p_vars: Any
    gen_q_vars: Any
    bat_p_vars: Any
    bat_q_vars: Any
    shunt_p_vars: Any
    shunt_q_vars: Any
    svc_q_vars: Any
    vsc_cs_p_vars: Any
    vsc_cs_q_vars: Any
    closed_branch_p1_vars: Any
    closed_branch_q1_vars: Any
    closed_branch_p2_vars: Any
    closed_branch_q2_vars: Any
    open_side1_branch_p2_vars: Any
    open_side1_branch_q2_vars: Any
    open_side2_branch_p1_vars: Any
    open_side2_branch_q1_vars: Any
    bl_v_vars: Any
    bl_ph_vars: Any
    bl_branch_p1_vars: Any
    bl_branch_p2_vars: Any
    bl_branch_q1_vars: Any
    bl_branch_q2_vars: Any
    t3_middle_v_vars: Any
    t3_middle_ph_vars: Any
    t3_closed_branch_p1_vars: Any
    t3_closed_branch_p2_vars: Any
    t3_closed_branch_q1_vars: Any
    t3_closed_branch_q2_vars: Any
    t3_open_side1_branch_p2_vars: Any
    t3_open_side1_branch_q2_vars: Any
    v_dc_vars: Any
    closed_dc_line_i1_vars: Any
    closed_dc_line_i2_vars: Any
    conv_p_vars: Any
    conv_q_vars: Any
    conv_i_vars: Any
    branch_num_2_index: list[int]
    gen_p_num_2_index: list[int]
    gen_q_num_2_index: list[int]
    bat_p_num_2_index: list[int]
    bat_q_num_2_index: list[int]
    shunt_num_2_index: list[int]
    svc_num_2_index: list[int]
    vsc_cs_num_2_index: list[int]
    bl_num_2_index: list[int]
    t3_num_2_index: list[int]
    t3_leg1_num_2_index: list[int]
    t3_leg2_num_2_index: list[int]
    t3_leg3_num_2_index: list[int]
    dc_line_num_2_index: list[int]
    conv_num_2_index: list[int]


    @staticmethod
    def build(network_cache: NetworkCache, model: Model) -> 'VariableContext':
        bus_count = len(network_cache.buses)
        v_vars = model.add_m_variables(bus_count, name="v")
        ph_vars = model.add_m_variables(bus_count, name="ph")
        dc_node_count = len(network_cache.dc_nodes)
        v_dc_vars = model.add_m_variables(dc_node_count, name="v_dc")

        gen_count = len(network_cache.generators)
        gen_p_nums: list[int] = []
        gen_q_nums: list[int] = []
        gen_p_num_2_index = [-1] * gen_count
        gen_q_num_2_index = [-1] * gen_count
        too_small_q_bounds_generator_ids = []
        for gen_num, row in enumerate(network_cache.generators.itertuples()):
            if row.bus_id:
                gen_p_num_2_index[gen_num] = len(gen_p_nums)
                gen_p_nums.append(gen_num)

                q_bounds = Bounds.get_reactive_power_bounds(row).mirror()
                if network_cache.is_too_small_reactive_power_bounds(q_bounds):
                    too_small_q_bounds_generator_ids.append(row.Index)
                    logger.log(TRACE_LEVEL, f"Too small reactive power bounds {q_bounds} for generator '{row.Index}' (num={gen_num})")
                else:
                    gen_q_num_2_index[gen_num] = len(gen_q_nums)
                    gen_q_nums.append(gen_num)

        gen_p_vars = model.add_m_variables(len(gen_p_nums), name="gen_p")
        gen_q_vars = model.add_m_variables(len(gen_q_nums), name="gen_q")

        bat_count = len(network_cache.batteries)
        bat_p_nums: list[int] = []
        bat_q_nums: list[int] = []
        bat_p_num_2_index = [-1] * bat_count
        bat_q_num_2_index = [-1] * bat_count
        for bat_num, row in enumerate(network_cache.batteries.itertuples()):
            if row.bus_id:
                bat_p_num_2_index[bat_num] = len(bat_p_nums)
                bat_p_nums.append(bat_num)

                q_bounds = Bounds.get_reactive_power_bounds(row).mirror()
                if network_cache.is_too_small_reactive_power_bounds(q_bounds):
                    too_small_q_bounds_generator_ids.append(row.Index)
                    logger.log(TRACE_LEVEL, f"Too small reactive power bounds {q_bounds} for battery '{row.Index}' (num={bat_num})")
                else:
                    bat_q_num_2_index[bat_num] = len(bat_q_nums)
                    bat_q_nums.append(bat_num)

        bat_p_vars = model.add_m_variables(len(bat_p_nums), name="bat_p")
        bat_q_vars = model.add_m_variables(len(bat_q_nums), name="bat_q")

        if too_small_q_bounds_generator_ids:
            logger.warning(f"{len(too_small_q_bounds_generator_ids)} generators|batteries have too small reactive power bounds")

        shunt_nums: list[int] = []
        shunt_count = len(network_cache.shunts)
        shunt_num_2_index = [-1] * shunt_count
        for shunt_num, row in enumerate(network_cache.shunts.itertuples()):
            if row.bus_id:
                shunt_num_2_index[shunt_num] = len(shunt_nums)
                shunt_nums.append(shunt_num)
        shunt_p_vars = model.add_m_variables(len(shunt_nums), name="shunt_p")
        shunt_q_vars = model.add_m_variables(len(shunt_nums), name="shunt_q")

        svc_nums: list[int] = []
        svc_count = len(network_cache.static_var_compensators)
        svc_num_2_index = [-1] * svc_count
        for svc_num, row in enumerate(network_cache.static_var_compensators.itertuples()):
            if row.bus_id:
                svc_num_2_index[svc_num] = len(svc_nums)
                svc_nums.append(svc_num)
        svc_q_vars = model.add_m_variables(len(svc_nums), name="svc_q")

        vsc_cs_nums: list[int] = []
        vsc_cs_count = len(network_cache.vsc_converter_stations)
        vsc_cs_num_2_index = [-1] * vsc_cs_count
        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if row.bus_id:
                vsc_cs_num_2_index[vsc_cs_num] = len(vsc_cs_nums)
                vsc_cs_nums.append(vsc_cs_num)
        vsc_cs_p_vars = model.add_m_variables(len(vsc_cs_nums), name="vsc_cs_p_vars")
        vsc_cs_q_vars = model.add_m_variables(len(vsc_cs_nums), name="vsc_cs_q_vars")

        closed_branch_nums: list[int] = []
        open_side1_branch_nums: list[int] = []
        open_side2_branch_nums: list[int] = []
        branch_count = len(network_cache.lines) + len(network_cache.transformers_2w)
        branch_num_2_index = [-1] * branch_count
        for branch_num, row in enumerate(network_cache.branches.itertuples(index=False)):
            if row.bus1_id and row.bus2_id:
                branch_num_2_index[branch_num] = len(closed_branch_nums)
                closed_branch_nums.append(branch_num)
            elif row.bus2_id:
                branch_num_2_index[branch_num] = len(open_side1_branch_nums)
                open_side1_branch_nums.append(branch_num)
            elif row.bus1_id:
                branch_num_2_index[branch_num] = len(open_side2_branch_nums)
                open_side2_branch_nums.append(branch_num)
        closed_branch_p1_vars = model.add_m_variables(len(closed_branch_nums), name='closed_branch_p1')
        closed_branch_q1_vars = model.add_m_variables(len(closed_branch_nums), name='closed_branch_q1')
        closed_branch_p2_vars = model.add_m_variables(len(closed_branch_nums), name='closed_branch_p2')
        closed_branch_q2_vars = model.add_m_variables(len(closed_branch_nums), name='closed_branch_q2')
        open_side1_branch_p2_vars = model.add_m_variables(len(open_side1_branch_nums), name='open_side1_branch_p2')
        open_side1_branch_q2_vars = model.add_m_variables(len(open_side1_branch_nums), name='open_side1_branch_q2')
        open_side2_branch_p1_vars = model.add_m_variables(len(open_side2_branch_nums), name='open_side2_branch_p1')
        open_side2_branch_q1_vars = model.add_m_variables(len(open_side2_branch_nums), name='open_side2_branch_q1')

        bl_count = len(network_cache.boundary_lines)
        bl_nums: list[int] = []
        bl_num_2_index = [-1] * bl_count
        for bl_num, row in enumerate(network_cache.boundary_lines.itertuples()):
            if row.bus_id:
                bl_num_2_index[bl_num] = len(bl_nums)
                bl_nums.append(bl_num)

        bl_v_vars = model.add_m_variables(len(bl_nums), name="bl_v")
        bl_ph_vars = model.add_m_variables(len(bl_nums), name="bl_ph")
        bl_branch_p1_vars = model.add_m_variables(len(bl_nums), name="bl_branch_p1")
        bl_branch_p2_vars = model.add_m_variables(len(bl_nums), name="bl_branch_p2")
        bl_branch_q1_vars = model.add_m_variables(len(bl_nums), name="bl_branch_q1")
        bl_branch_q2_vars = model.add_m_variables(len(bl_nums), name="bl_branch_q2")

        t3_count = len(network_cache.transformers_3w)
        t3_nums: list[int] = []
        t3_num_2_index = [-1] * t3_count
        t3_closed_branch_leg_nums: list[int] = []
        t3_open_side2_leg_nums: list[int] = []
        t3_leg1_num_2_index = [-1] * t3_count
        t3_leg2_num_2_index = [-1] * t3_count
        t3_leg3_num_2_index = [-1] * t3_count
        for t3_num, t3_row in enumerate(network_cache.transformers_3w.itertuples()):
            if t3_row.bus1_id or t3_row.bus2_id or t3_row.bus3_id:
                t3_num_2_index[t3_num] = len(t3_nums)
                t3_nums.append(t3_num)

                if t3_row.bus1_id:
                    t3_leg1_num_2_index[t3_num] = len(t3_closed_branch_leg_nums)
                    t3_closed_branch_leg_nums.append(t3_num)
                else:
                    t3_leg1_num_2_index[t3_num] = len(t3_open_side2_leg_nums)
                    t3_open_side2_leg_nums.append(t3_num)

                if t3_row.bus2_id:
                    t3_leg2_num_2_index[t3_num] = len(t3_closed_branch_leg_nums)
                    t3_closed_branch_leg_nums.append(t3_num)
                else:
                    t3_leg2_num_2_index[t3_num] = len(t3_open_side2_leg_nums)
                    t3_open_side2_leg_nums.append(t3_num)

                if t3_row.bus3_id:
                    t3_leg3_num_2_index[t3_num] = len(t3_closed_branch_leg_nums)
                    t3_closed_branch_leg_nums.append(t3_num)
                else:
                    t3_leg3_num_2_index[t3_num] = len(t3_open_side2_leg_nums)
                    t3_open_side2_leg_nums.append(t3_num)

        t3_middle_v_vars = model.add_m_variables(len(t3_nums), name="t3_v")
        t3_middle_ph_vars = model.add_m_variables(len(t3_nums), name="t3_ph")
        t3_closed_branch_p1_vars = model.add_m_variables(len(t3_closed_branch_leg_nums), name="t3_closed_branch_p1")
        t3_closed_branch_p2_vars = model.add_m_variables(len(t3_closed_branch_leg_nums), name="t3_closed_branch_p2")
        t3_closed_branch_q1_vars = model.add_m_variables(len(t3_closed_branch_leg_nums), name="t3_closed_branch_q1")
        t3_closed_branch_q2_vars = model.add_m_variables(len(t3_closed_branch_leg_nums), name="t3_closed_branch_q2")
        t3_open_side1_p2_vars = model.add_m_variables(len(t3_open_side2_leg_nums), name="t3_open_side1_branch_p2")
        t3_open_side1_q2_vars = model.add_m_variables(len(t3_open_side2_leg_nums), name="t3_open_side1_branch_q2")

        closed_dc_line_nums = []
        dc_line_count = len(network_cache.dc_lines)
        dc_line_num_2_index = [-1] * dc_line_count
        for dc_line_num, row in enumerate(network_cache.dc_lines.itertuples(index=False)):
            if row.dc_node1_id and row.dc_node2_id:
                dc_line_num_2_index[dc_line_num] = len(closed_dc_line_nums)
                closed_dc_line_nums.append(dc_line_num)

        closed_dc_line_i1_vars = model.add_m_variables(len(closed_dc_line_nums), name='closed_dc_line_i1')
        closed_dc_line_i2_vars = model.add_m_variables(len(closed_dc_line_nums), name='closed_dc_line_i2')

        converter_nums = []
        converter_count = len(network_cache.voltage_source_converters)
        conv_num_2_index = [-1] * converter_count
        for converter_num, row in enumerate(network_cache.voltage_source_converters.itertuples()):
            conv_num_2_index[converter_num] = len(converter_nums)
            converter_nums.append(converter_num)
        conv_p_vars = model.add_m_variables(len(converter_nums), name="conv_p_vars")
        conv_q_vars = model.add_m_variables(len(converter_nums), name="conv_q_vars")
        # for one converter, conv_i is the DC current flowing from dc_node1 to dc_node2
        conv_i_vars = model.add_m_variables(len(converter_nums), name="conv_i_vars")

        return VariableContext(v_vars, ph_vars,
                               gen_p_vars, gen_q_vars,
                               bat_p_vars, bat_q_vars,
                               shunt_p_vars, shunt_q_vars,
                               svc_q_vars,
                               vsc_cs_p_vars, vsc_cs_q_vars,
                               closed_branch_p1_vars, closed_branch_q1_vars,
                               closed_branch_p2_vars, closed_branch_q2_vars,
                               open_side1_branch_p2_vars, open_side1_branch_q2_vars,
                               open_side2_branch_p1_vars, open_side2_branch_q1_vars,
                               bl_v_vars, bl_ph_vars,
                               bl_branch_p1_vars, bl_branch_p2_vars,
                               bl_branch_q1_vars, bl_branch_q2_vars,
                               t3_middle_v_vars, t3_middle_ph_vars,
                               t3_closed_branch_p1_vars, t3_closed_branch_p2_vars,
                               t3_closed_branch_q1_vars, t3_closed_branch_q2_vars,
                               t3_open_side1_p2_vars, t3_open_side1_q2_vars,
                               v_dc_vars,
                               closed_dc_line_i1_vars, closed_dc_line_i2_vars,
                               conv_p_vars, conv_q_vars, conv_i_vars,
                               branch_num_2_index,
                               gen_p_num_2_index, gen_q_num_2_index,
                               bat_p_num_2_index, bat_q_num_2_index,
                               shunt_num_2_index,
                               svc_num_2_index,
                               vsc_cs_num_2_index,
                               bl_num_2_index,
                               t3_num_2_index, t3_leg1_num_2_index, t3_leg2_num_2_index, t3_leg3_num_2_index,
                               dc_line_num_2_index, conv_num_2_index)

    def _update_generators(self, network_cache: NetworkCache, model: Model) -> None:
        connected_gen_ids: list[str] = []
        connected_gen_target_p = []
        connected_gen_target_q = []
        connected_gen_target_v = []
        connected_gen_voltage_regulator_on = []
        connected_gen_p = []
        connected_gen_q = []
        disconnected_gen_ids: list[str] = []
        for gen_num, (gen_id, row) in enumerate(network_cache.generators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                connected_gen_ids.append(gen_id.__str__())

                gen_p_index = self.gen_p_num_2_index[gen_num]
                gen_q_index = self.gen_q_num_2_index[gen_num]

                p = model.get_value(self.gen_p_vars[gen_p_index])
                target_p = -p
                connected_gen_target_p.append(target_p)
                connected_gen_p.append(p)

                if gen_q_index == -1:
                    target_q = row.target_q
                    q = -row.target_q
                    target_v = row.target_v
                    voltage_regulator_on = False
                else:
                    q = model.get_value(self.gen_q_vars[gen_q_index])
                    target_q = -q

                    regulated_bus_num = network_cache.buses.index.get_loc(row.regulated_bus_id)
                    v = model.get_value(self.v_vars[regulated_bus_num])
                    target_v = v

                    q_bounds = Bounds.get_reactive_power_bounds(row).mirror()
                    voltage_regulator_on = q_bounds.contains(q)

                connected_gen_target_q.append(target_q)
                connected_gen_target_v.append(target_v)
                connected_gen_voltage_regulator_on.append(voltage_regulator_on)
                connected_gen_q.append(q)

                logger.log(TRACE_LEVEL, f"Update generator '{gen_id}' (num={gen_num}): target_p={target_p}, target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}, p={p}, q={q}")
            else:
                disconnected_gen_ids.append(gen_id.__str__())
                logger.log(TRACE_LEVEL, f"Update disconnected generator '{gen_id}' (num={gen_num})")

        network_cache.update_generators(connected_gen_ids,
                                        connected_gen_target_p,
                                        connected_gen_target_q,
                                        connected_gen_target_v,
                                        connected_gen_voltage_regulator_on,
                                        connected_gen_p,
                                        connected_gen_q,
                                        disconnected_gen_ids)

    def _update_batteries(self, network_cache: NetworkCache, model: Model) -> None:
        connected_bat_ids = []
        connected_bat_target_p = []
        connected_bat_target_q = []
        connected_bat_target_v = []
        connected_bat_voltage_regulator_on = []
        connected_bat_p = []
        connected_bat_q = []
        disconnected_bat_ids = []
        for bat_num, (bat_id, row) in enumerate(network_cache.batteries.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                connected_bat_ids.append(bat_id.__str__())

                bat_p_index = self.bat_p_num_2_index[bat_num]
                bat_q_index = self.bat_q_num_2_index[bat_num]

                p = model.get_value(self.bat_p_vars[bat_p_index])
                target_p = -p
                connected_bat_target_p.append(target_p)
                connected_bat_p.append(p)

                if bat_q_index == -1:
                    target_q = row.target_q
                    q = -row.target_q
                    target_v = row.target_v
                    voltage_regulator_on = False
                else:
                    q = model.get_value(self.bat_q_vars[bat_q_index])
                    target_q = -q

                    bus_num = network_cache.buses.index.get_loc(row.bus_id)
                    v = model.get_value(self.v_vars[bus_num]) * row.nominal_v  # FIXME
                    target_v = v

                    q_bounds = Bounds.get_reactive_power_bounds(row).mirror()
                    voltage_regulator_on = q_bounds.contains(q)

                connected_bat_target_q.append(target_q)
                connected_bat_target_v.append(target_v)
                connected_bat_voltage_regulator_on.append(voltage_regulator_on)
                connected_bat_q.append(q)

                logger.log(TRACE_LEVEL, f"Update battery '{bat_id}' (num={bat_num}): target_p={target_p}, target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}, p={p}, q={q}")
            else:
                disconnected_bat_ids.append(bat_id.__str__())
                logger.log(TRACE_LEVEL, f"Update disconnected battery '{bat_id}' (num={bat_num})")

        network_cache.update_batteries(connected_bat_ids,
                                       connected_bat_target_p,
                                       connected_bat_target_q,
                                       connected_bat_target_v,
                                       connected_bat_voltage_regulator_on,
                                       connected_bat_p,
                                       connected_bat_q,
                                       disconnected_bat_ids)

    def _update_vsc_converter_stations(self, network_cache: NetworkCache, model: Model) -> None:
        connected_vsc_cs_ids = []
        connected_vsc_cs_target_q = []
        connected_vsc_cs_target_v = []
        connected_vsc_cs_voltage_regulator_on = []
        connected_vsc_cs_p = []
        connected_vsc_cs_q = []
        disconnected_vsc_cs_ids = []
        for vsc_cs_num, (vsc_cs_id, row) in enumerate(network_cache.vsc_converter_stations.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                connected_vsc_cs_ids.append(vsc_cs_id.__str__())
                vsc_cs_index = self.vsc_cs_num_2_index[vsc_cs_num]

                p = model.get_value(self.vsc_cs_p_vars[vsc_cs_index])
                connected_vsc_cs_p.append(p)

                q = model.get_value(self.vsc_cs_q_vars[vsc_cs_index])
                target_q = -q
                connected_vsc_cs_target_q.append(target_q)
                connected_vsc_cs_q.append(q)

                regulated_bus_num = network_cache.buses.index.get_loc(row.regulated_bus_id)
                v = model.get_value(self.v_vars[regulated_bus_num])
                target_v = v
                connected_vsc_cs_target_v.append(target_v)

                q_bounds = Bounds.get_reactive_power_bounds(row).mirror()
                voltage_regulator_on = q_bounds.contains(q)
                connected_vsc_cs_voltage_regulator_on.append(voltage_regulator_on)

                logger.log(TRACE_LEVEL, f"Update VSC converter station '{vsc_cs_id}' (num={vsc_cs_num}): target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}, p={p}, q={q}")
            else:
                disconnected_vsc_cs_ids.append(vsc_cs_id.__str__())
                logger.log(TRACE_LEVEL, f"Update disconnected VSC converter station '{vsc_cs_id}' (num={vsc_cs_num})")

        network_cache.update_vsc_converter_stations(connected_vsc_cs_ids,
                                                    connected_vsc_cs_target_q,
                                                    connected_vsc_cs_target_v,
                                                    connected_vsc_cs_voltage_regulator_on,
                                                    connected_vsc_cs_p,
                                                    connected_vsc_cs_q,
                                                    disconnected_vsc_cs_ids)

    def _update_hvdc_lines(self, network_cache: NetworkCache, model: Model) -> None:
        hvdc_line_ids = []
        hvdc_line_target_p = []
        for hvdc_line_num, hvdc_line_row in enumerate(cast(list[HvdcRow], network_cache.hvdc_lines.itertuples(index=True))):
            hvdc_line_ids.append(hvdc_line_row.Index)
            vsc_cs1_num = network_cache.vsc_converter_stations.index.get_loc(hvdc_line_row.converter_station1_id)
            vsc_cs2_num = network_cache.vsc_converter_stations.index.get_loc(hvdc_line_row.converter_station2_id)
            p1 = model.get_value(self.vsc_cs_p_vars[vsc_cs1_num])
            p2 = model.get_value(self.vsc_cs_p_vars[vsc_cs2_num])
            target_p = abs(p1) if NetworkCache.is_rectifier(hvdc_line_row.converter_station1_id, hvdc_line_row) else abs(p2)
            hvdc_line_target_p.append(target_p)

            logger.log(TRACE_LEVEL, f"Update HVDC line '{hvdc_line_row.Index}': target_p={target_p}")

        network_cache.update_hvdc_lines(hvdc_line_ids, hvdc_line_target_p)

    def _update_static_var_compensators(self, network_cache: NetworkCache, model: Model) -> None:
        connected_svc_ids = []
        connected_svc_target_q = []
        connected_svc_target_v = []
        connected_svc_regulation_mode = []
        connected_svc_p = []
        connected_svc_q = []
        disconnected_svc_ids = []
        for svc_num, (svc_id, row) in enumerate(network_cache.static_var_compensators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                connected_svc_ids.append(svc_id.__str__())

                connected_svc_p.append(0.0)

                svc_index = self.svc_num_2_index[svc_num]
                q = model.get_value(self.svc_q_vars[svc_index])
                target_q = -q
                connected_svc_target_q.append(target_q)
                connected_svc_q.append(q)

                regulated_bus_num = network_cache.buses.index.get_loc(row.regulated_bus_id)
                v = model.get_value(self.v_vars[regulated_bus_num])
                target_v = v
                connected_svc_target_v.append(target_v)

                q_bounds = Bounds(row.b_min * v * v, row.b_max * v * v)
                regulation_mode = 'VOLTAGE' if q_bounds.contains(q) else 'REACTIVE_POWER'
                connected_svc_regulation_mode.append(regulation_mode)

                logger.log(TRACE_LEVEL, f"Update SVC '{svc_id}' (num={svc_num}): target_q={target_q}, target_v={target_v}, regulation_mode={regulation_mode}")
            else:
                disconnected_svc_ids.append(svc_id.__str__())
                logger.log(TRACE_LEVEL, f"Update disconnected SVC '{svc_id}' (num={svc_num})")

        network_cache.update_static_var_compensators(connected_svc_ids,
                                                     connected_svc_target_q,
                                                     connected_svc_target_v,
                                                     connected_svc_regulation_mode,
                                                     connected_svc_p,
                                                     connected_svc_q,
                                                     disconnected_svc_ids)

    def _update_shunt_compensators(self, network_cache: NetworkCache, model: Model) -> None:
        connected_shunt_ids = []
        connected_shunt_p = []
        connected_shunt_q = []
        disconnected_shunt_ids = []
        for shunt_num, (shunt_id, row) in enumerate(network_cache.shunts.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                shunt_index = self.shunt_num_2_index[shunt_num]
                p = model.get_value(self.shunt_p_vars[shunt_index])
                q = model.get_value(self.shunt_q_vars[shunt_index])
                connected_shunt_ids.append(shunt_id.__str__())
                connected_shunt_p.append(p)
                connected_shunt_q.append(q)

                logger.log(TRACE_LEVEL, f"Update shunt '{shunt_id}' (num={shunt_num}): p={p} q={q}")
            else:
                disconnected_shunt_ids.append(shunt_id.__str__())
                logger.log(TRACE_LEVEL, f"Update disconnected shunt '{shunt_id}' (num={shunt_num})")

        network_cache.update_shunt_compensators(connected_shunt_ids, connected_shunt_p, connected_shunt_q, disconnected_shunt_ids)

    def _update_branches(self, network_cache: NetworkCache, model: Model) -> None:
        branch_ids = []
        branch_p1 = []
        branch_p2 = []
        branch_q1 = []
        branch_q2 = []
        for branch_num, (branch_id, row) in enumerate(network_cache.branches.iterrows()):
            branch_index = self.branch_num_2_index[branch_num]
            branch_ids.append(branch_id.__str__())
            if row.bus1_id and row.bus2_id:
                p1 = model.get_value(self.closed_branch_p1_vars[branch_index])
                p2 = model.get_value(self.closed_branch_p2_vars[branch_index])
                q1 = model.get_value(self.closed_branch_q1_vars[branch_index])
                q2 = model.get_value(self.closed_branch_q2_vars[branch_index])
            elif row.bus2_id:
                p1 = 0.0
                p2 = model.get_value(self.open_side1_branch_p2_vars[branch_index])
                q1 = 0.0
                q2 = model.get_value(self.open_side1_branch_q2_vars[branch_index])
            elif row.bus1_id:
                p1 = model.get_value(self.open_side2_branch_p1_vars[branch_index])
                p2 = 0.0
                q1 = model.get_value(self.open_side2_branch_q1_vars[branch_index])
                q2 = 0.0
            else:
                p1 = 0.0
                p2 = 0.0
                q1 = 0.0
                q2 = 0.0

            branch_p1.append(p1)
            branch_p2.append(p2)
            branch_q1.append(q1)
            branch_q2.append(q2)

            logger.log(TRACE_LEVEL, f"Update branch '{branch_id}': p1={p1} p2={p2} q1={q1} q2={q2}")

        network_cache.update_branches(branch_ids, branch_p1, branch_p2, branch_q1, branch_q2)

    def _update_transformers_3w(self, network_cache: NetworkCache, model: Model) -> None:
        t3_ids = []
        t3_p1 = []
        t3_p2 = []
        t3_p3 = []
        t3_q1 = []
        t3_q2 = []
        t3_q3 = []
        t3_v = []
        t3_angle = []
        for t3_num, (t3_id, t3_row) in enumerate(network_cache.transformers_3w.iterrows()):
            t3_ids.append(t3_id.__str__())
            t3_index = self.t3_num_2_index[t3_num]
            if t3_row.bus1_id or t3_row.bus2_id or t3_row.bus3_id:
                leg1_index = self.t3_leg1_num_2_index[t3_num]
                leg2_index = self.t3_leg2_num_2_index[t3_num]
                leg3_index = self.t3_leg3_num_2_index[t3_num]

                if t3_row.bus1_id:
                    p1 = model.get_value(self.t3_closed_branch_p1_vars[leg1_index])
                    q1 = model.get_value(self.t3_closed_branch_q1_vars[leg1_index])
                else:
                    p1 = 0
                    q1 = 0

                if t3_row.bus2_id:
                    p2 = model.get_value(self.t3_closed_branch_p1_vars[leg2_index])
                    q2 = model.get_value(self.t3_closed_branch_q1_vars[leg2_index])
                else:
                    p2 = 0
                    q2 = 0

                if t3_row.bus3_id:
                    p3 = model.get_value(self.t3_closed_branch_p1_vars[leg3_index])
                    q3 = model.get_value(self.t3_closed_branch_q1_vars[leg3_index])
                else:
                    q3 = 0
                    p3 = 0

                v = model.get_value(self.t3_middle_v_vars[t3_index])
                angle = model.get_value(self.t3_middle_ph_vars[t3_index])
            else:
                p1 = 0.0
                p2 = 0.0
                p3 = 0.0
                q1 = 0.0
                q2 = 0.0
                q3 = 0.0
                v = math.nan
                angle = math.nan

            t3_p1.append(p1)
            t3_p2.append(p2)
            t3_p3.append(p3)
            t3_q1.append(q1)
            t3_q2.append(q2)
            t3_q3.append(q3)
            t3_v.append(v * t3_row.rated_u0)
            t3_angle.append(math.degrees(angle))

            logger.log(TRACE_LEVEL, f"Update 3 windings transformer '{t3_id}': p1={p1} p2={p2} p3={p3} q1={q1} q2={q2} q3={q3} v={v} angle={angle}")

        network_cache.update_transformers_3w(t3_ids, t3_p1, t3_p2, t3_p3, t3_q1, t3_q2, t3_q3, t3_v, t3_angle)

    def _update_buses(self, network_cache: NetworkCache, model: Model) -> None:
        bus_ids = []
        bus_v_mag = []
        bus_v_angle = []
        for bus_num, (bus_id, row) in enumerate(network_cache.buses.iterrows()):
            bus_ids.append(bus_id.__str__())
            v = model.get_value(self.v_vars[bus_num])
            bus_v_mag.append(v)
            angle = model.get_value(self.ph_vars[bus_num])
            bus_v_angle.append(angle)

            logger.log(TRACE_LEVEL, f"Update bus '{bus_id}' (num={bus_num}): v={v}, angle={angle}")

        network_cache.update_buses(bus_ids, bus_v_mag, bus_v_angle)

    def _update_boundary_lines(self, network_cache: NetworkCache, model: Model)-> None:
        connected_bl_ids = []
        connected_bl_v = []
        connected_bl_angle = []
        connected_bl_p = []
        connected_bl_q = []
        disconnected_bl_ids = []
        for bl_num, (bl_id, row) in enumerate(network_cache.boundary_lines.iterrows()):
            bl_index = self.bl_num_2_index[bl_num]

            if row.bus_id:
                v = model.get_value(self.bl_v_vars[bl_index])
                angle = model.get_value(self.bl_ph_vars[bl_index])
                connected_bl_ids.append(bl_id.__str__())
                connected_bl_v.append(v * row.nominal_v)
                connected_bl_angle.append(math.degrees(angle))
                p = model.get_value(self.bl_branch_p1_vars[bl_index])
                q = model.get_value(self.bl_branch_q1_vars[bl_index])
                connected_bl_p.append(p)
                connected_bl_q.append(q)
                logger.log(TRACE_LEVEL, f"Update boundary line '{bl_id}' (num={bl_num}): v={v}, angle={angle}, p={p}, q={q}")
            else:
                disconnected_bl_ids.append(bl_id.__str__())
                logger.log(TRACE_LEVEL, f"Update disconnected boundary line '{bl_id}' (num={bl_num})")

        network_cache.update_boundary_lines(connected_bl_ids,
                                            connected_bl_v,
                                            connected_bl_angle,
                                            connected_bl_p,
                                            connected_bl_q,
                                            disconnected_bl_ids)

    def _update_dc_nodes(self, network_cache: NetworkCache, model: Model):
        dc_node_ids = []
        dc_node_v = []
        for dc_node_num, (dc_node_id, row) in enumerate(network_cache.dc_nodes.iterrows()):
            dc_node_ids.append(dc_node_id)
            v = model.get_value(self.v_dc_vars[dc_node_num])
            dc_node_v.append(v)

            logger.log(TRACE_LEVEL, f"Update dc_node '{dc_node_id}' (num={dc_node_num}): v={v}")

<<<<<<< HEAD
<<<<<<< HEAD
        network_cache.update_dc_nodes(dc_node_ids, dc_node_v)
=======
        network_cache.network.update_dc_nodes(id=dc_node_ids, v=dc_node_v)
>>>>>>> 2f9806ea (Add DC losses function, fix network update)
=======
        network_cache.update_dc_nodes(dc_node_ids, dc_node_v)
>>>>>>> 9d7e3835 (Update DC opf with changes in DC dataframes)

    def _update_dc_lines(self, network_cache: NetworkCache, model: Model):
        dc_line_ids = []
        dc_line_i1 = []
        dc_line_i2 = []
        for dc_line_num, (dc_line_id, row) in enumerate(network_cache.dc_lines.iterrows()):
            dc_line_index = self.dc_line_num_2_index[dc_line_num]
            dc_line_ids.append(dc_line_id)
            i1 = model.get_value(self.closed_dc_line_i1_vars[dc_line_index])
            i2 = model.get_value(self.closed_dc_line_i2_vars[dc_line_index])

            dc_line_i1.append(i1)
            dc_line_i2.append(i2)

            logger.log(TRACE_LEVEL, f"Update dc_line '{dc_line_id}': i1={i1} i2={i2}")

        network_cache.update_dc_lines(dc_line_ids, dc_line_i1, dc_line_i2)

    def _update_voltage_source_converters(self, network_cache: NetworkCache, model: Model):
        conv_ids = []
        conv_p = []
        conv_q = []
        conv_target_p = []
        conv_target_q = []
        conv_target_v_dc = []
        conv_target_v_ac = []
        conv_p_dc1 = []
        conv_p_dc2 = []
        for conv_num, (conv_id, row) in enumerate(network_cache.voltage_source_converters.iterrows()):
            bus1_id = row.bus1_id
            dc_node1_id = row.dc_node1_id
            dc_node2_id = row.dc_node2_id
            v1 = 0
            v2 = 0

            if row.dc_connected1:
                dc_node1_num = network_cache.dc_nodes.index.get_loc(dc_node1_id)
                v1 = model.get_value(self.v_dc_vars[dc_node1_num])
            if row.dc_connected2:
                dc_node2_num = network_cache.dc_nodes.index.get_loc(dc_node2_id)
                v2 = model.get_value(self.v_dc_vars[dc_node2_num])
            v_dc = v1 - v2
            conv_target_v_dc.append(v_dc)
<<<<<<< HEAD
<<<<<<< HEAD
            if bus1_id:
                bus_num = network_cache.buses.index.get_loc(bus1_id)
=======
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
>>>>>>> 2f9806ea (Add DC losses function, fix network update)
=======
            if bus1_id:
                bus_num = network_cache.buses.index.get_loc(bus1_id)
>>>>>>> 9d7e3835 (Update DC opf with changes in DC dataframes)

                conv_ids.append(conv_id)
                conv_index = self.conv_num_2_index[conv_num]

<<<<<<< HEAD
<<<<<<< HEAD
                p_ac = model.get_value(self.conv_p_vars[conv_index])
                conv_p.append(p_ac)
                conv_target_p.append(p_ac)

                q_ac = model.get_value(self.conv_q_vars[conv_index])
                conv_q.append(q_ac)
                conv_target_q.append(q_ac)
=======
                p = model.get_value(self.conv_p_vars[conv_index])
                conv_p.append(p)
                conv_target_p.append(p)

                q = model.get_value(self.conv_q_vars[conv_index])
                conv_q.append(q)
                conv_target_q.append(q)
>>>>>>> 2f9806ea (Add DC losses function, fix network update)
=======
                p_ac = model.get_value(self.conv_p_vars[conv_index])
                conv_p.append(p_ac)
                conv_target_p.append(p_ac)

                q_ac = model.get_value(self.conv_q_vars[conv_index])
                conv_q.append(q_ac)
                conv_target_q.append(q_ac)
>>>>>>> 9d7e3835 (Update DC opf with changes in DC dataframes)

                i = model.get_value(self.conv_i_vars[conv_index])
                p_dc1 = i * v1
                p_dc2 = i * v2
                conv_p_dc1.append(p_dc1)
                conv_p_dc2.append(p_dc2)

                v_ac = model.get_value(self.v_vars[bus_num])
                conv_target_v_ac.append(v_ac)

<<<<<<< HEAD
<<<<<<< HEAD
                logger.log(TRACE_LEVEL,
                           f"Update voltage source converter '{conv_id}' (num={conv_num}): p_ac={p_ac}, q_ac={q_ac}, "
                           f"p_dc1={p_dc1}, p_dc2={p_dc2}, target_p={p_ac}, target_q={q_ac}, target_v_dc={v_dc}, target_v_ac={v_ac}")
=======
                logger.log(TRACE_LEVEL, f"Update VSC converter station '{conv_id}' (num={conv_num}): p={p}, q={q}, "
                                        f"p_dc1={p_dc1}, p_dc2={p_dc2}, target_p={p}, target_q={q}, target_v_dc={v_dc}, target_v_ac={v_ac}")

        network_cache.network.update_voltage_source_converters(id=conv_ids, p=conv_p, q=conv_q, p_dc1=conv_p_dc1,
                                                               p_dc2=conv_p_dc2, target_p=conv_target_p, target_q=conv_target_q,
                                                               target_v_dc=conv_target_v_dc, target_v_ac=conv_target_v_ac)
>>>>>>> 2f9806ea (Add DC losses function, fix network update)
=======
                logger.log(TRACE_LEVEL,
                           f"Update voltage source converter '{conv_id}' (num={conv_num}): p_ac={p_ac}, q_ac={q_ac}, "
                           f"p_dc1={p_dc1}, p_dc2={p_dc2}, target_p={p_ac}, target_q={q_ac}, target_v_dc={v_dc}, target_v_ac={v_ac}")

        network_cache.update_voltage_source_converters(conv_ids, conv_p, conv_q, conv_p_dc1, conv_p_dc2, conv_target_p,
                                                       conv_target_q, conv_target_v_dc, conv_target_v_ac)
>>>>>>> 9d7e3835 (Update DC opf with changes in DC dataframes)

        network_cache.update_voltage_source_converters(conv_ids, conv_p, conv_q, conv_p_dc1, conv_p_dc2, conv_target_p,
                                                       conv_target_q, conv_target_v_dc, conv_target_v_ac)

    def update_network(self, network_cache: NetworkCache, model: Model) -> None:
        self._update_generators(network_cache, model)
        self._update_batteries(network_cache, model)
        self._update_vsc_converter_stations(network_cache, model)
        self._update_hvdc_lines(network_cache, model)
        self._update_static_var_compensators(network_cache, model)
        self._update_shunt_compensators(network_cache, model)
        self._update_branches(network_cache, model)
        self._update_transformers_3w(network_cache, model)
        self._update_buses(network_cache, model)
        self._update_boundary_lines(network_cache, model)
        self._update_dc_nodes(network_cache, model)
        self._update_dc_lines(network_cache, model)
        self._update_voltage_source_converters(network_cache, model)

