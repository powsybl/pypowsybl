import logging
from dataclasses import dataclass
from typing import Any

from pyoptinterface import ipopt

from pypowsybl.opf.impl.bounds import Bounds
from pypowsybl.opf.impl.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)


@dataclass
class VariableContext:
    ph_vars: Any
    v_vars: Any
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
    branch_num_2_index: list[int]

    @staticmethod
    def build(network_cache: NetworkCache, model: ipopt.Model) -> 'VariableContext':
        bus_count = len(network_cache.buses)
        v_vars = model.add_variables(range(bus_count), name="v")
        ph_vars = model.add_variables(range(bus_count), name="ph")

        gen_count = len(network_cache.generators)
        gen_p_vars = model.add_variables(range(gen_count), name="gen_p")
        gen_q_vars = model.add_variables(range(gen_count), name="gen_q")

        shunt_count = len(network_cache.shunts)
        shunt_p_vars = model.add_variables(range(shunt_count), name="shunt_p")
        shunt_q_vars = model.add_variables(range(shunt_count), name="shunt_q")

        svc_count = len(network_cache.static_var_compensators)
        svc_q_vars = model.add_variables(range(svc_count), name="svc_q")

        vsc_cs_count = len(network_cache.vsc_converter_stations)
        vsc_cs_p_vars = model.add_variables(range(vsc_cs_count), name="vsc_cs_p_vars")
        vsc_cs_q_vars = model.add_variables(range(vsc_cs_count), name="vsc_cs_q_vars")

        closed_branch_nums = []
        open_side1_branch_nums = []
        open_side2_branch_nums = []
        branch_count = len(network_cache.lines) + len(network_cache.transformers)
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

        return VariableContext(ph_vars, v_vars,
                               gen_p_vars, gen_q_vars,
                               shunt_p_vars, shunt_q_vars,
                               svc_q_vars,
                               vsc_cs_p_vars, vsc_cs_q_vars,
                               closed_branch_p1_vars, closed_branch_q1_vars,
                               closed_branch_p2_vars, closed_branch_q2_vars,
                               open_side1_branch_p2_vars, open_side1_branch_q2_vars,
                               open_side2_branch_p1_vars, open_side2_branch_q1_vars,
                               branch_num_2_index)

    def _update_generators(self, network_cache: NetworkCache, model: ipopt.Model):
        gen_ids = []
        gen_target_p = []
        gen_target_q = []
        gen_target_v = []
        gen_voltage_regulator_on = []
        for gen_num, (gen_id, row) in enumerate(network_cache.generators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                gen_ids.append(gen_id)

                p = model.get_value(self.gen_p_vars[gen_num])
                target_p = -p
                gen_target_p.append(target_p)

                q = model.get_value(self.gen_q_vars[gen_num])
                target_q = -q
                gen_target_q.append(target_q)

                bus_num = network_cache.buses.index.get_loc(bus_id)
                target_v = model.get_value(self.v_vars[bus_num])
                gen_target_v.append(target_v)

                q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                voltage_regulator_on = q_bounds.contains(q)
                gen_voltage_regulator_on.append(voltage_regulator_on)

                logger.log(TRACE_LEVEL, f"Update generator '{gen_id}' (num={gen_num}): target_p={target_p}, target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}")

        network_cache.network.update_generators(id=gen_ids, target_p=gen_target_p, target_q=gen_target_q, target_v=gen_target_v,
                                        voltage_regulator_on=gen_voltage_regulator_on)

    def _update_vsc_converter_stations(self, network_cache: NetworkCache, model: ipopt.Model):
        vsc_cs_ids = []
        vsc_cs_target_q = []
        vsc_cs_target_v = []
        vsc_cs_voltage_regulator_on = []
        hvdc_line_ids = []
        hvdc_line_target_p = []
        hvdc_line_converters_mode = []
        for vsc_cs_num, (vsc_cs_id, row) in enumerate(network_cache.vsc_converter_stations.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                vsc_cs_ids.append(vsc_cs_id)
                hvdc_line_ids.append(row.hvdc_line_id)

                p = model.get_value(self.vsc_cs_p_vars[vsc_cs_num])
                target_p = abs(p)
                hvdc_line_target_p.append(target_p)
                converters_mode = 'SIDE_1_RECTIFIER_SIDE_2_INVERTER' if p >= 0 else 'SIDE_1_INVERTER_SIDE_2_RECTIFIER'
                hvdc_line_converters_mode.append(converters_mode)

                q = model.get_value(self.vsc_cs_q_vars[vsc_cs_num])
                target_q = -q
                vsc_cs_target_q.append(target_q)

                bus_num = network_cache.buses.index.get_loc(bus_id)
                target_v = model.get_value(self.v_vars[bus_num])
                vsc_cs_target_v.append(target_v)

                q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                voltage_regulator_on = q_bounds.contains(q)
                vsc_cs_voltage_regulator_on.append(voltage_regulator_on)

                logger.log(TRACE_LEVEL, f"Update VSC converter station '{vsc_cs_id}' (num={vsc_cs_num}): target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}")
                logger.log(TRACE_LEVEL, f"Update HVDC line '{row.hvdc_line_id}': target_p={target_p}, converters_mode={converters_mode}")

        network_cache.network.update_vsc_converter_stations(id=vsc_cs_ids, target_q=vsc_cs_target_q, target_v=vsc_cs_target_v,
                                                            voltage_regulator_on=vsc_cs_voltage_regulator_on)
        network_cache.network.update_hvdc_lines(id=hvdc_line_ids, target_p=hvdc_line_target_p,
                                                converters_mode=hvdc_line_converters_mode)

    def _update_static_var_compensators(self, network_cache: NetworkCache, model: ipopt.Model):
        svc_ids = []
        svc_target_q = []
        svc_target_v = []
        svc_regulation_mode = []
        for gen_num, (svc_id, row) in enumerate(network_cache.static_var_compensators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                svc_ids.append(svc_id)

                q = model.get_value(self.svc_q_vars[gen_num])
                target_q = -q
                svc_target_q.append(target_q)

                bus_num = network_cache.buses.index.get_loc(bus_id)
                target_v = model.get_value(self.v_vars[bus_num])
                svc_target_v.append(target_v)

                q_bounds = Bounds.get_svc_reactive_power_bounds(row).mirror()
                regulation_mode = 'VOLTAGE' if q_bounds.contains(q) else 'REACTIVE_POWER'
                svc_regulation_mode.append(regulation_mode)

                logger.log(TRACE_LEVEL, f"Update SVC '{svc_id}' (num={gen_num}): target_q={target_q}, target_v={target_v}, regulation_mode={regulation_mode}")

        network_cache.network.update_static_var_compensators(id=svc_ids, target_q=svc_target_q, target_v=svc_target_v,
                                                             regulation_mode=svc_regulation_mode)

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

    def update_network(self, network_cache: NetworkCache, model: ipopt.Model) -> None:
        self._update_generators(network_cache, model)
        self._update_vsc_converter_stations(network_cache, model)
        self._update_static_var_compensators(network_cache, model)
        self._update_buses(network_cache, model)
