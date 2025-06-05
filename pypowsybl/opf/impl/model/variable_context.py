import logging
import math
from dataclasses import dataclass
from typing import Any

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)


@dataclass
class VariableContext:
    v_vars: Any
    ph_vars: Any
    gen_p_vars: Any
    gen_q_vars: Any
    shunt_p_vars: Any
    shunt_q_vars: Any
    svc_q_vars: Any
    vsc_cs_p_vars: Any
    vsc_cs_q_vars: Any
    shunt_q_vars: Any
    closed_branch_p1_vars: Any
    closed_branch_q1_vars: Any
    closed_branch_p2_vars: Any
    closed_branch_q2_vars: Any
    open_side1_branch_p2_vars: Any
    open_side1_branch_q2_vars: Any
    open_side2_branch_p1_vars: Any
    open_side2_branch_q1_vars: Any
    dl_v_vars: Any
    dl_ph_vars: Any
    dl_branch_p1_vars: Any
    dl_branch_p2_vars: Any
    dl_branch_q1_vars: Any
    dl_branch_q2_vars: Any
    t3_middle_v_vars: Any
    t3_middle_ph_vars: Any
    t3_closed_branch_p1_vars: Any
    t3_closed_branch_p2_vars: Any
    t3_closed_branch_q1_vars: Any
    t3_closed_branch_q2_vars: Any
    t3_open_side1_branch_p2_vars: Any
    t3_open_side1_branch_q2_vars: Any
    branch_num_2_index: list[int]
    gen_p_num_2_index: list[int]
    gen_q_num_2_index: list[int]
    shunt_num_2_index: list[int]
    svc_num_2_index: list[int]
    vsc_cs_num_2_index: list[int]
    dl_num_2_index: list[int]
    t3_num_2_index: list[int]
    t3_leg1_num_2_index: list[int]
    t3_leg2_num_2_index: list[int]
    t3_leg3_num_2_index: list[int]

    @staticmethod
    def build(network_cache: NetworkCache, model: ipopt.Model) -> 'VariableContext':
        bus_count = len(network_cache.buses)
        v_vars = model.add_variables(range(bus_count), name="v")
        ph_vars = model.add_variables(range(bus_count), name="ph")

        gen_count = len(network_cache.generators)
        gen_p_nums = []
        gen_q_nums = []
        gen_p_num_2_index = [-1] * gen_count
        gen_q_num_2_index = [-1] * gen_count
        too_small_q_bounds_generator_ids = []
        for gen_num, row in enumerate(network_cache.generators.itertuples()):
            if row.bus_id:
                gen_p_num_2_index[gen_num] = len(gen_p_nums)
                gen_p_nums.append(gen_num)

                q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                if network_cache.is_too_small_reactive_power_bounds(q_bounds):
                    too_small_q_bounds_generator_ids.append(row.Index)
                    logger.log(TRACE_LEVEL, f"Too small reactive power bounds {q_bounds} for generator '{row.Index}' (num={gen_num})")
                else:
                    gen_q_num_2_index[gen_num] = len(gen_q_nums)
                    gen_q_nums.append(gen_num)

        if too_small_q_bounds_generator_ids:
            logger.warning(f"{len(too_small_q_bounds_generator_ids)} generators have too small reactive power bounds")

        gen_p_vars = model.add_variables(range(len(gen_p_nums)), name="gen_p")
        gen_q_vars = model.add_variables(range(len(gen_q_nums)), name="gen_q")

        shunt_nums = []
        shunt_count = len(network_cache.shunts)
        shunt_num_2_index = [-1] * shunt_count
        for shunt_num, row in enumerate(network_cache.shunts.itertuples()):
            if row.bus_id:
                shunt_num_2_index[shunt_num] = len(shunt_nums)
                shunt_nums.append(shunt_num)
        shunt_p_vars = model.add_variables(range(len(shunt_nums)), name="shunt_p")
        shunt_q_vars = model.add_variables(range(len(shunt_nums)), name="shunt_q")

        svc_nums = []
        svc_count = len(network_cache.static_var_compensators)
        svc_num_2_index = [-1] * svc_count
        for svc_num, row in enumerate(network_cache.static_var_compensators.itertuples()):
            if row.bus_id:
                svc_num_2_index[svc_num] = len(svc_nums)
                svc_nums.append(svc_num)
        svc_q_vars = model.add_variables(range(len(svc_nums)), name="svc_q")

        vsc_cs_nums = []
        vsc_cs_count = len(network_cache.vsc_converter_stations)
        vsc_cs_num_2_index = [-1] * vsc_cs_count
        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if row.bus_id:
                vsc_cs_num_2_index[vsc_cs_num] = len(vsc_cs_nums)
                vsc_cs_nums.append(vsc_cs_num)
        vsc_cs_p_vars = model.add_variables(range(len(vsc_cs_nums)), name="vsc_cs_p_vars")
        vsc_cs_q_vars = model.add_variables(range(len(vsc_cs_nums)), name="vsc_cs_q_vars")

        closed_branch_nums = []
        open_side1_branch_nums = []
        open_side2_branch_nums = []
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
        closed_branch_p1_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_p1')
        closed_branch_q1_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_q1')
        closed_branch_p2_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_p2')
        closed_branch_q2_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_q2')
        open_side1_branch_p2_vars = model.add_variables(range(len(open_side1_branch_nums)), name='open_side1_branch_p2')
        open_side1_branch_q2_vars = model.add_variables(range(len(open_side1_branch_nums)), name='open_side1_branch_q2')
        open_side2_branch_p1_vars = model.add_variables(range(len(open_side2_branch_nums)), name='open_side2_branch_p1')
        open_side2_branch_q1_vars = model.add_variables(range(len(open_side2_branch_nums)), name='open_side2_branch_q1')

        dl_count = len(network_cache.dangling_lines)
        dl_nums = []
        dl_num_2_index = [-1] * dl_count
        for dl_num, row in enumerate(network_cache.dangling_lines.itertuples()):
            if row.bus_id:
                dl_num_2_index[dl_num] = len(dl_nums)
                dl_nums.append(dl_num)

        dl_v_vars = model.add_variables(range(len(dl_nums)), name="dl_v")
        dl_ph_vars = model.add_variables(range(len(dl_nums)), name="dl_ph")
        dl_branch_p1_vars = model.add_variables(range(len(dl_nums)), name="dl_branch_p1")
        dl_branch_p2_vars = model.add_variables(range(len(dl_nums)), name="dl_branch_p2")
        dl_branch_q1_vars = model.add_variables(range(len(dl_nums)), name="dl_branch_q1")
        dl_branch_q2_vars = model.add_variables(range(len(dl_nums)), name="dl_branch_q2")

        t3_count = len(network_cache.transformers_3w)
        t3_nums = []
        t3_num_2_index = [-1] * t3_count
        t3_closed_branch_leg_nums = []
        t3_open_side2_leg_nums = []
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

        t3_middle_v_vars = model.add_variables(range(len(t3_nums)), name="t3_v")
        t3_middle_ph_vars = model.add_variables(range(len(t3_nums)), name="t3_ph")
        t3_closed_branch_p1_vars = model.add_variables(range(len(t3_closed_branch_leg_nums)), name="t3_closed_branch_p1")
        t3_closed_branch_p2_vars = model.add_variables(range(len(t3_closed_branch_leg_nums)), name="t3_closed_branch_p2")
        t3_closed_branch_q1_vars = model.add_variables(range(len(t3_closed_branch_leg_nums)), name="t3_closed_branch_q1")
        t3_closed_branch_q2_vars = model.add_variables(range(len(t3_closed_branch_leg_nums)), name="t3_closed_branch_q2")
        t3_open_side1_p2_vars = model.add_variables(range(len(t3_open_side2_leg_nums)), name="t3_open_side1_branch_p2")
        t3_open_side1_q2_vars = model.add_variables(range(len(t3_open_side2_leg_nums)), name="t3_open_side1_branch_q2")

        return VariableContext(v_vars, ph_vars,
                               gen_p_vars, gen_q_vars,
                               shunt_p_vars, shunt_q_vars,
                               svc_q_vars,
                               vsc_cs_p_vars, vsc_cs_q_vars,
                               closed_branch_p1_vars, closed_branch_q1_vars,
                               closed_branch_p2_vars, closed_branch_q2_vars,
                               open_side1_branch_p2_vars, open_side1_branch_q2_vars,
                               open_side2_branch_p1_vars, open_side2_branch_q1_vars,
                               dl_v_vars, dl_ph_vars,
                               dl_branch_p1_vars, dl_branch_p2_vars,
                               dl_branch_q1_vars, dl_branch_q2_vars,
                               t3_middle_v_vars, t3_middle_ph_vars,
                               t3_closed_branch_p1_vars, t3_closed_branch_p2_vars,
                               t3_closed_branch_q1_vars, t3_closed_branch_q2_vars,
                               t3_open_side1_p2_vars, t3_open_side1_q2_vars,
                               branch_num_2_index,
                               gen_p_num_2_index, gen_q_num_2_index,
                               shunt_num_2_index,
                               svc_num_2_index,
                               vsc_cs_num_2_index,
                               dl_num_2_index,
                               t3_num_2_index, t3_leg1_num_2_index, t3_leg2_num_2_index, t3_leg3_num_2_index)

    def _update_generators(self, network_cache: NetworkCache, model: ipopt.Model):
        gen_ids = []
        gen_target_p = []
        gen_target_q = []
        gen_target_v = []
        gen_voltage_regulator_on = []
        gen_p = []
        gen_q = []
        for gen_num, (gen_id, row) in enumerate(network_cache.generators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                gen_ids.append(gen_id)

                gen_p_index = self.gen_p_num_2_index[gen_num]
                gen_q_index = self.gen_q_num_2_index[gen_num]

                p = model.get_value(self.gen_p_vars[gen_p_index])
                target_p = -p
                gen_target_p.append(target_p)
                gen_p.append(p)

                if gen_q_index == -1:
                    gen_target_q.append(row.target_q)
                    gen_q.append(-row.target_q)
                    gen_target_v.append(row.target_v)
                    gen_voltage_regulator_on.append(False)
                else:
                    q = model.get_value(self.gen_q_vars[gen_q_index])
                    target_q = -q
                    gen_target_q.append(target_q)
                    gen_q.append(q)

                    bus_num = network_cache.buses.index.get_loc(bus_id)
                    target_v = model.get_value(self.v_vars[bus_num])
                    gen_target_v.append(target_v)

                    q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                    voltage_regulator_on = q_bounds.contains(q)
                    gen_voltage_regulator_on.append(voltage_regulator_on)

                logger.log(TRACE_LEVEL, f"Update generator '{gen_id}' (num={gen_num}): target_p={target_p}, target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}")

        network_cache.network.update_generators(id=gen_ids, target_p=gen_target_p, target_q=gen_target_q, target_v=gen_target_v,
                                        voltage_regulator_on=gen_voltage_regulator_on, p=gen_p, q=gen_q)

    def _update_vsc_converter_stations(self, network_cache: NetworkCache, model: ipopt.Model):
        vsc_cs_ids = []
        vsc_cs_target_q = []
        vsc_cs_target_v = []
        vsc_cs_voltage_regulator_on = []
        vsc_cs_p = []
        vsc_cs_q = []
        for vsc_cs_num, (vsc_cs_id, row) in enumerate(network_cache.vsc_converter_stations.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                vsc_cs_ids.append(vsc_cs_id)
                vsc_cs_index = self.vsc_cs_num_2_index[vsc_cs_num]

                p = model.get_value(self.vsc_cs_p_vars[vsc_cs_index])
                vsc_cs_p.append(p)

                q = model.get_value(self.vsc_cs_q_vars[vsc_cs_index])
                target_q = -q
                vsc_cs_target_q.append(target_q)
                vsc_cs_q.append(q)

                bus_num = network_cache.buses.index.get_loc(bus_id)
                target_v = model.get_value(self.v_vars[bus_num])
                vsc_cs_target_v.append(target_v)

                q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                voltage_regulator_on = q_bounds.contains(q)
                vsc_cs_voltage_regulator_on.append(voltage_regulator_on)

                logger.log(TRACE_LEVEL, f"Update VSC converter station '{vsc_cs_id}' (num={vsc_cs_num}): target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}")

        network_cache.network.update_vsc_converter_stations(id=vsc_cs_ids, target_q=vsc_cs_target_q, target_v=vsc_cs_target_v,
                                                            voltage_regulator_on=vsc_cs_voltage_regulator_on, p=vsc_cs_p, q=vsc_cs_q)

    def _update_hvdc_lines(self, network_cache: NetworkCache, model: ipopt.Model):
        hvdc_line_ids = []
        hvdc_line_target_p = []
        for hvdc_line_num, (hvdc_line_id, hvdc_line_row) in enumerate(network_cache.hvdc_lines.iterrows()):
            hvdc_line_ids.append(hvdc_line_id)
            vsc_cs1_num = network_cache.vsc_converter_stations.index.get_loc(hvdc_line_row.converter_station1_id)
            vsc_cs2_num = network_cache.vsc_converter_stations.index.get_loc(hvdc_line_row.converter_station2_id)
            p1 = model.get_value(self.vsc_cs_p_vars[vsc_cs1_num])
            p2 = model.get_value(self.vsc_cs_p_vars[vsc_cs2_num])
            target_p = abs(p1) if NetworkCache.is_rectifier(hvdc_line_row.converter_station1_id, hvdc_line_row) else abs(p2)
            hvdc_line_target_p.append(target_p)

            logger.log(TRACE_LEVEL, f"Update HVDC line '{hvdc_line_id}': target_p={target_p}")

        network_cache.network.update_hvdc_lines(id=hvdc_line_ids, target_p=hvdc_line_target_p)

    def _update_static_var_compensators(self, network_cache: NetworkCache, model: ipopt.Model):
        svc_ids = []
        svc_target_q = []
        svc_target_v = []
        svc_regulation_mode = []
        svc_p = []
        svc_q = []
        for svc_num, (svc_id, row) in enumerate(network_cache.static_var_compensators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                svc_ids.append(svc_id)

                svc_p.append(0.0)

                svc_index = self.svc_num_2_index[svc_num]
                q = model.get_value(self.svc_q_vars[svc_index])
                target_q = -q
                svc_target_q.append(target_q)
                svc_q.append(q)

                bus_num = network_cache.buses.index.get_loc(bus_id)
                v = model.get_value(self.v_vars[bus_num])
                target_v = v
                svc_target_v.append(target_v)

                q_bounds = Bounds(row.b_min * v * v, row.b_max * v * v)
                regulation_mode = 'VOLTAGE' if q_bounds.contains(q) else 'REACTIVE_POWER'
                svc_regulation_mode.append(regulation_mode)

                logger.log(TRACE_LEVEL, f"Update SVC '{svc_id}' (num={svc_num}): target_q={target_q}, target_v={target_v}, regulation_mode={regulation_mode}")

        network_cache.network.update_static_var_compensators(id=svc_ids, target_q=svc_target_q, target_v=svc_target_v,
                                                             regulation_mode=svc_regulation_mode, p=svc_p, q=svc_q)

    def _update_shunt_compensators(self, network_cache: NetworkCache, model: ipopt.Model):
        shunt_ids = []
        shunt_p = []
        shunt_q = []
        for shunt_num, (shunt_id, row) in enumerate(network_cache.shunts.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                shunt_index = self.shunt_num_2_index[shunt_num]
                p = model.get_value(self.shunt_p_vars[shunt_index])
                q = model.get_value(self.shunt_q_vars[shunt_index])
            else:
                p = 0.0
                q = 0.0
            shunt_ids.append(shunt_id)
            shunt_p.append(p)
            shunt_q.append(q)

            logger.log(TRACE_LEVEL,
                           f"Update shunt '{shunt_id}' (num={shunt_num}): p={p} q={q}")

        network_cache.network.update_shunt_compensators(id=shunt_ids, p=shunt_p, q=shunt_q)

    def _update_branches(self, network_cache: NetworkCache, model: ipopt.Model):
        branch_ids = []
        branch_p1 = []
        branch_p2 = []
        branch_q1 = []
        branch_q2 = []
        for branch_num, (branch_id, row) in enumerate(network_cache.branches.iterrows()):
            branch_index = self.branch_num_2_index[branch_num]
            branch_ids.append(branch_id)
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

        network_cache.network.update_branches(id=branch_ids, p1=branch_p1, p2=branch_p2, q1=branch_q1, q2=branch_q2)

    def _update_transformers_3w(self, network_cache: NetworkCache, model: ipopt.Model):
        t3_ids = []
        t3_p1 = []
        t3_p2 = []
        t3_p3 = []
        t3_q1 = []
        t3_q2 = []
        t3_q3 = []
        for t3_num, (t3_id, t3_row) in enumerate(network_cache.transformers_3w.iterrows()):
            t3_ids.append(t3_id)
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
            else:
                p1 = 0.0
                p2 = 0.0
                p3 = 0.0
                q1 = 0.0
                q2 = 0.0
                q3 = 0.0

            t3_p1.append(p1)
            t3_p2.append(p2)
            t3_p3.append(p3)
            t3_q1.append(q1)
            t3_q2.append(q2)
            t3_q3.append(q3)

            logger.log(TRACE_LEVEL, f"Update 3 windings transformer '{t3_id}': p1={p1} p2={p2} p3={p3} q1={q1} q2={q2} q3={q3}")

        network_cache.network.update_3_windings_transformers(id=t3_ids, p1=t3_p1, p2=t3_p2, p3=t3_p3, q1=t3_q1, q2=t3_q2, q3=t3_q3)

    def _update_buses(self, network_cache: NetworkCache, model: ipopt.Model):
        bus_ids = []
        bus_v_mag = []
        bus_v_angle = []
        for bus_num, (bus_id, row) in enumerate(network_cache.buses.iterrows()):
            bus_ids.append(bus_id)
            v = model.get_value(self.v_vars[bus_num])
            bus_v_mag.append(v)
            angle = model.get_value(self.ph_vars[bus_num])
            bus_v_angle.append(angle)

            logger.log(TRACE_LEVEL, f"Update bus '{bus_id}' (num={bus_num}): v={v}, angle={angle}")

        network_cache.network.update_buses(id=bus_ids, v_mag=bus_v_mag, v_angle=bus_v_angle)

    def _update_dangling_lines(self, network_cache: NetworkCache, model: ipopt.Model):
        dl_ids = []
        dl_v = []
        dl_angle = []
        dl_p = []
        dl_q = []
        for dl_num, (dl_id, row) in enumerate(network_cache.dangling_lines.iterrows()):
            dl_index = self.dl_num_2_index[dl_num]

            if row.bus_id:
                v = model.get_value(self.dl_v_vars[dl_index])
                angle = model.get_value(self.dl_ph_vars[dl_index])
                dl_ids.append(dl_id)
                dl_v.append(v * row.nominal_v)
                dl_angle.append(math.degrees(angle))
                p = model.get_value(self.dl_branch_p1_vars[dl_index])
                q = model.get_value(self.dl_branch_q1_vars[dl_index])
            else:
                v = math.nan
                angle = math.nan
                p = 0.0
                q = 0.0

            dl_p.append(p)
            dl_q.append(q)

            logger.log(TRACE_LEVEL, f"Update dangline line '{dl_id}' (num={dl_num}): v={v}, angle={angle}, p={p}, q={q}")

        network_cache.network.update_dangling_lines(id=dl_ids, p=dl_p, q=dl_q)
        network_cache.network.add_elements_properties(id=dl_ids, v=dl_v, angle=dl_angle)

    def update_network(self, network_cache: NetworkCache, model: ipopt.Model) -> None:
        self._update_generators(network_cache, model)
        self._update_vsc_converter_stations(network_cache, model)
        self._update_hvdc_lines(network_cache, model)
        self._update_static_var_compensators(network_cache, model)
        self._update_shunt_compensators(network_cache, model)
        self._update_branches(network_cache, model)
        self._update_transformers_3w(network_cache, model)
        self._update_buses(network_cache, model)
        self._update_dangling_lines(network_cache, model)
