import math

import pandas as pd
from pandas import DataFrame

from pypowsybl.network import Network
from pypowsybl.opf.impl.model.bounds import Bounds


class NetworkCache:
    def __init__(self, network: Network) -> None:
        self._network = network
        self._network.per_unit = True
        self._voltage_levels = self._build_voltage_levels(network)
        self._buses = self._build_buses(network, self._voltage_levels)
        self._generators = self._build_generators(network, self.buses)
        self._loads = self._build_loads(network, self.buses)
        self._shunts = self._build_shunt_compensators(network, self.buses)
        self._static_var_compensators = self._build_static_var_compensators(network, self.buses)
        self._vsc_converter_stations = self._build_vsc_converter_stations(network, self.buses)
        self._lcc_converter_stations = self._build_lcc_converter_stations(network, self.buses)
        self._hvdc_lines = self._build_hvdc_lines(network, self._vsc_converter_stations, self._lcc_converter_stations, self.buses)
        self._lines = self._build_lines(network, self.buses)
        self._transformers_2w = self._build_2w_transformers(network, self.buses)
        self._transformers_3w = self._build_3w_transformers(network, self.buses)
        self._branches = self._build_branches(network, self.buses)
        self._dangling_lines = self._build_dangling_lines(network, self.buses)
        self._batteries = self._build_batteries(network, self.buses)
        # self._current_limits1, self._current_limits2 = self._build_current_limits(network)
        self._slack_terminal = self._build_stack_terminal(network, self.buses)
        self._dc_buses = self._build_dc_buses(network)
        self._dc_nodes = self._build_dc_nodes(network, self.dc_buses)
        self._dc_lines = self._build_dc_lines(network)
        self._voltage_source_converters = self._build_voltage_source_converters(network)
        self._dc_grounds = self._build_dc_grounds(network)

    @staticmethod
    def _filter_injections(injections: DataFrame, buses: DataFrame) -> DataFrame:
        if len(injections) == 0:
            return injections
        injections_and_buses = pd.merge(injections, buses, right_index=True, left_on='bus_id', how='left')
        return injections_and_buses[
            (injections_and_buses['connected_component'] == 0) & (injections_and_buses['synchronous_component'] == 0)]

    @staticmethod
    def _filter_branches(branches: DataFrame, buses: DataFrame) -> DataFrame:
        if len(branches) == 0:
            return branches
        branches_and_buses = pd.merge(branches, buses, left_on='bus1_id', right_index=True, how='left')
        branches_and_buses = pd.merge(branches_and_buses, buses, left_on='bus2_id', right_index=True,
                                      suffixes=('', '_2'), how='left')
        return branches_and_buses[
            (branches_and_buses['connected_component'] == 0) & (branches_and_buses['synchronous_component'] == 0) | (
                    branches_and_buses['connected_component_2'] == 0) & (
                    branches_and_buses['synchronous_component_2'] == 0)]

    @staticmethod
    def _filter_3w_transformers(transformers_3w: DataFrame, buses: DataFrame) -> DataFrame:
        if len(transformers_3w) == 0:
            return transformers_3w
        transfos_and_buses = pd.merge(transformers_3w, buses, left_on='bus1_id', right_index=True, how='left')
        transfos_and_buses = pd.merge(transfos_and_buses, buses, left_on='bus2_id', right_index=True, suffixes=('', '_2'), how='left')
        transfos_and_buses = pd.merge(transfos_and_buses, buses, left_on='bus3_id', right_index=True, suffixes=('', '_3'), how='left')
        return transfos_and_buses[
            (transfos_and_buses['connected_component'] == 0) & (transfos_and_buses['synchronous_component'] == 0)
            | (transfos_and_buses['connected_component_2'] == 0) & (transfos_and_buses['synchronous_component_2'] == 0)
            | (transfos_and_buses['connected_component_3'] == 0) & (transfos_and_buses['synchronous_component_3'] == 0)]

    @staticmethod
    def _build_branches(network: Network, buses: DataFrame):
        branches = network.get_branches(attributes=['bus1_id', 'bus2_id'])
        return NetworkCache._filter_branches(branches, buses)

    @staticmethod
    def _build_2w_transformers(network: Network, buses: DataFrame):
        transfos = network.get_2_windings_transformers(
            attributes=['bus1_id', 'bus2_id', 'rho', 'alpha', 'r_at_current_tap', 'x_at_current_tap', 'g_at_current_tap', 'b_at_current_tap'])
        return NetworkCache._filter_branches(transfos, buses)

    @staticmethod
    def _build_3w_transformers(network: Network, buses: DataFrame):
        transfos = network.get_3_windings_transformers(
            attributes=['bus1_id', 'bus2_id', 'bus3_id',
                        'rated_u0',
                        'rho1', 'rho2', 'rho3',
                        'alpha1', 'alpha2', 'alpha3',
                        'r1_at_current_tap', 'r2_at_current_tap', 'r3_at_current_tap', 'x1_at_current_tap', 'x2_at_current_tap', 'x3_at_current_tap',
                        'g1_at_current_tap', 'g2_at_current_tap', 'g3_at_current_tap', 'b1_at_current_tap', 'b2_at_current_tap', 'b3_at_current_tap'])
        return NetworkCache._filter_3w_transformers(transfos, buses)

    @staticmethod
    def _build_lines(network: Network, buses: DataFrame):
        lines = network.get_lines(attributes=['bus1_id', 'bus2_id', 'r', 'x', 'g1', 'g2', 'b1', 'b2'])
        return NetworkCache._filter_branches(lines, buses)

    @staticmethod
    def _build_shunt_compensators(network: Network, buses: DataFrame):
        shunts = network.get_shunt_compensators(attributes=['bus_id', 'g', 'b'])
        return NetworkCache._filter_injections(shunts, buses)

    @staticmethod
    def _build_static_var_compensators(network: Network, buses: DataFrame):
        svcs = network.get_static_var_compensators(attributes=['bus_id', 'b_min', 'b_max', 'regulated_bus_id'])
        return NetworkCache._filter_injections(svcs, buses)

    @staticmethod
    def _filter_hvdc_lines(hvdc_lines: DataFrame, vsc_converter_stations: DataFrame,
                           lcc_converter_stations: DataFrame, buses: DataFrame) -> DataFrame:
        if len(hvdc_lines) == 0:
            return hvdc_lines
        converter_stations = pd.concat([vsc_converter_stations[['bus_id']], lcc_converter_stations[['bus_id']]])
        hvdc_lines_and_buses = pd.merge(hvdc_lines, converter_stations, left_on='converter_station1_id',
                                        right_index=True, how='left')
        hvdc_lines_and_buses = pd.merge(hvdc_lines_and_buses, converter_stations, left_on='converter_station2_id',
                                        right_index=True, suffixes=('', '_2'), how='left')
        hvdc_lines_and_buses = pd.merge(hvdc_lines_and_buses, buses, left_on='bus_id', right_index=True, how='left')
        hvdc_lines_and_buses = pd.merge(hvdc_lines_and_buses, buses, left_on='bus_id_2', right_index=True,
                                        suffixes=('', '_2'), how='left')
        return hvdc_lines_and_buses[
            (hvdc_lines_and_buses['connected_component'] == 0) & (hvdc_lines_and_buses['synchronous_component'] == 0) & (
                    hvdc_lines_and_buses['connected_component_2'] == 0) & (
                    hvdc_lines_and_buses['synchronous_component_2'] == 0)]

    @staticmethod
    def _build_hvdc_lines(network: Network, vsc_converter_stations: DataFrame, lcc_converter_stations: DataFrame,
                          buses: DataFrame):
        hvdc_lines = network.get_hvdc_lines(attributes=['converter_station1_id', 'converter_station2_id', 'converters_mode', 'nominal_v', 'r'])
        return NetworkCache._filter_hvdc_lines(hvdc_lines, vsc_converter_stations, lcc_converter_stations, buses)

    @staticmethod
    def _build_vsc_converter_stations(network: Network, buses: DataFrame):
        stations = network.get_vsc_converter_stations(attributes=['bus_id', 'loss_factor', 'min_q_at_target_p', 'max_q_at_target_p',
                                                                  'target_v', 'target_q', 'voltage_regulator_on', 'hvdc_line_id',
                                                                  'regulated_bus_id'])
        if len(stations) > 0:
            hvdc_lines = network.get_hvdc_lines(attributes=['converter_station1_id', 'converter_station2_id', 'converters_mode', 'target_p', 'max_p'])
            stations = pd.merge(stations, hvdc_lines, left_on='hvdc_line_id', right_index=True, how='left')
        return NetworkCache._filter_injections(stations, buses)

    @staticmethod
    def _build_lcc_converter_stations(network: Network, buses: DataFrame):
        stations = network.get_lcc_converter_stations(attributes=['bus_id', 'loss_factor', 'power_factor', 'hvdc_line_id'])
        if len(stations) > 0:
            hvdc_lines = network.get_hvdc_lines(attributes=['converters_mode', 'target_p', 'max_p'])
            stations = pd.merge(stations, hvdc_lines, left_on='hvdc_line_id', right_index=True, how='left')
        return NetworkCache._filter_injections(stations, buses)

    @staticmethod
    def _build_loads(network: Network, buses: DataFrame):
        loads = network.get_loads(attributes=['bus_id', 'p0', 'q0'])
        return NetworkCache._filter_injections(loads, buses)

    @staticmethod
    def _build_generators(network: Network, buses: DataFrame):
        generators = network.get_generators(
            attributes=['bus_id', 'min_p', 'max_p', 'min_q_at_target_p', 'max_q_at_target_p', 'target_p', 'target_q',
                        'target_v', 'voltage_regulator_on', 'regulated_bus_id'])
        return NetworkCache._filter_injections(generators, buses)

    @staticmethod
    def _build_buses(network: Network, voltage_levels: DataFrame):
        buses = network.get_buses(attributes=['voltage_level_id', 'connected_component', 'synchronous_component'])
        # buses = buses[(buses['synchronous_component'] == 0) & (buses['connected_component'] == 0)]
        buses = buses[buses['connected_component'] == 0]
        return pd.merge(buses, voltage_levels, left_on='voltage_level_id', right_index=True, how='left')

    @staticmethod
    def _build_voltage_levels(network: Network) -> DataFrame:
        return network.get_voltage_levels(attributes=['low_voltage_limit', 'high_voltage_limit', 'nominal_v'])

    @staticmethod
    def _build_dangling_lines(network: Network, buses: DataFrame):
        dangling_lines = network.get_dangling_lines(attributes=['bus_id', 'r', 'x', 'g', 'b', 'p0', 'q0', 'paired'])
        return NetworkCache._filter_injections(dangling_lines, buses)

    @staticmethod
    def _build_batteries(network: Network, buses: DataFrame):
        batteries = network.get_batteries(attributes=['bus_id', 'min_p', 'max_p', 'min_q_at_target_p', 'max_q_at_target_p', 'target_p', 'target_q'])
        if len(batteries):
            voltage_regulation = network.get_extensions('voltageRegulation')
            batteries = pd.merge(batteries, voltage_regulation, left_index=True, right_on='id', how='left')
            batteries['voltage_regulator_on'] = batteries['voltage_regulator_on'].fillna(False)
            batteries = NetworkCache._filter_injections(batteries, buses)
            # FIXME to remove when extensions will be per united
            batteries['target_v'] /= batteries['nominal_v']
        return batteries

    @staticmethod
    def _build_current_limits(network: Network) -> tuple[DataFrame, DataFrame]:
        limits = network.get_operational_limits(attributes=['name', 'value']).reset_index(
            level=['side', 'type', 'acceptable_duration', 'group_name'])
        limits = limits[(limits['type'] == 'CURRENT') & (limits['name'] == 'permanent_limit')]
        return limits[limits['side'] == 'ONE'][['value']], limits[limits['side'] == 'TWO'][['value']]

    @staticmethod
    def _build_stack_terminal(network, buses: DataFrame):
        slack_terminal = network.get_extensions('slackTerminal')
        return NetworkCache._filter_injections(slack_terminal, buses)

    @staticmethod
    def _build_dc_buses(network: Network):
        dc_buses = network.get_dc_buses(attributes=['connected_component', 'dc_component'])
        return dc_buses[dc_buses['connected_component'] == 0]

    @staticmethod
    def _build_dc_nodes(network: Network, dc_buses: DataFrame):
        dc_nodes = network.get_dc_nodes(attributes=['dc_bus_id', 'nominal_v'])
        return pd.merge(dc_nodes, dc_buses, left_on='bus_id', right_index=True, how='left')

    @staticmethod
    def _build_dc_lines(network: Network):
        return network.get_dc_lines(attributes=['dc_node1_id', 'dc_node2_id', 'r'])

    @staticmethod
    def _build_dc_nodes(network: Network, dc_buses: DataFrame):
        return network.get_dc_nodes(attributes=['dc_bus_id', 'nominal_v'])

    @staticmethod
    def _build_voltage_source_converters(network: Network):
        return network.get_voltage_source_converters(attributes=['voltage_level_id', 'bus1_id', 'bus2_id', 'dc_node1_id', 'dc_node2_id',
                                     'dc_connected1', 'dc_connected2', 'pcc_terminal_id', 'voltage_regulator_on',
                                     'control_mode', 'target_v_dc', 'target_v_ac', 'target_p', 'target_q', 'idle_loss',
                                     'switching_loss', 'resistive_loss'])

    @staticmethod
    def _build_dc_grounds(network: Network):
        return network.get_dc_grounds(attributes=['dc_node_id', 'r'])

    @property
    def network(self) -> Network:
        return self._network

    @property
    def buses(self) -> DataFrame:
        return self._buses

    @property
    def generators(self) -> DataFrame:
        return self._generators

    @property
    def loads(self) -> DataFrame:
        return self._loads

    @property
    def shunts(self) -> DataFrame:
        return self._shunts

    @property
    def static_var_compensators(self) -> DataFrame:
        return self._static_var_compensators

    @property
    def vsc_converter_stations(self) -> DataFrame:
        return self._vsc_converter_stations

    @property
    def lcc_converter_stations(self) -> DataFrame:
        return self._lcc_converter_stations

    @property
    def lines(self) -> DataFrame:
        return self._lines

    @property
    def transformers_2w(self) -> DataFrame:
        return self._transformers_2w

    @property
    def transformers_3w(self) -> DataFrame:
        return self._transformers_3w

    @property
    def branches(self) -> DataFrame:
        return self._branches

    @property
    def hvdc_lines(self) -> DataFrame:
        return self._hvdc_lines

    @property
    def dangling_lines(self) -> DataFrame:
        return self._dangling_lines

    @property
    def batteries(self) -> DataFrame:
        return self._batteries

    @property
    def current_limits1(self) -> DataFrame:
        return self._current_limits1

    @property
    def current_limits2(self) -> DataFrame:
        return self._current_limits2

    @property
    def slack_terminal(self) -> DataFrame:
        return self._slack_terminal

    @property
    def dc_buses(self) -> DataFrame:
        return self._dc_buses

    @property
    def dc_lines(self) -> DataFrame:
        return self._dc_lines

    @property
    def dc_nodes(self) -> DataFrame:
        return self._dc_nodes

    @property
    def voltage_source_converters(self) -> DataFrame:
        return self._voltage_source_converters

    @property
    def dc_grounds(self) -> DataFrame:
        return self._dc_grounds

    @staticmethod
    def is_rectifier(vsc_cs_id: str, hvdc_line_row) -> bool:
        return ((vsc_cs_id == hvdc_line_row.converter_station1_id and hvdc_line_row.converters_mode == 'SIDE_1_RECTIFIER_SIDE_2_INVERTER')
                or (vsc_cs_id == hvdc_line_row.converter_station2_id and hvdc_line_row.converters_mode == 'SIDE_1_INVERTER_SIDE_2_RECTIFIER'))

    def is_too_small_reactive_power_bounds(self, q_bounds: Bounds):
        return abs(q_bounds.max_value - q_bounds.min_value) < 1.0 / self._network.nominal_apparent_power

    def update_generators(self,
                          connected_gen_ids: list[str],
                          connected_gen_target_p: list[float],
                          connected_gen_target_q: list[float],
                          connected_gen_target_v: list[float],
                          connected_gen_voltage_regulator_on: list[bool],
                          connected_gen_p: list[float],
                          connected_gen_q: list[float],
                          disconnected_gen_ids: list[str] = None):

        if connected_gen_ids:
            self._network.update_generators(id=connected_gen_ids,
                                            target_p=connected_gen_target_p,
                                            target_q=connected_gen_target_q,
                                            target_v=connected_gen_target_v,
                                            voltage_regulator_on=connected_gen_voltage_regulator_on,
                                            p=connected_gen_p,
                                            q=connected_gen_q)

        if disconnected_gen_ids:
            self._network.update_generators(id=disconnected_gen_ids,
                                            p=[0.0] * len(disconnected_gen_ids),
                                            q=[0.0] * len(disconnected_gen_ids))

        self._generators = self._build_generators(self._network, self._buses)

    def update_batteries(self,
                         connected_bat_ids: list[str],
                         connected_bat_target_p: list[float],
                         connected_bat_target_q: list[float],
                         connected_bat_target_v: list[float],
                         connected_bat_voltage_regulator_on: list[bool],
                         connected_bat_p: list[float],
                         connected_bat_q: list[float],
                         disconnected_bat_ids: list[str] = None):
        if connected_bat_ids:
            self._network.update_batteries(id=connected_bat_ids,
                                           target_p=connected_bat_target_p,
                                           target_q=connected_bat_target_q,
                                           p=connected_bat_p,
                                           q=connected_bat_q)
            self._network.update_extensions("voltageRegulation",
                                            id=connected_bat_ids,
                                            voltage_regulator_on=connected_bat_voltage_regulator_on,
                                            target_v=connected_bat_target_v)

        if disconnected_bat_ids:
            self._network.update_batteries(id=disconnected_bat_ids,
                                           p=[0.0] * len(disconnected_bat_ids),
                                           q=[0.0] * len(disconnected_bat_ids))

        self._batteries = self._build_batteries(self._network, self._buses)

    def update_vsc_converter_stations(self,
                                      connected_vsc_cs_ids: list[str],
                                      connected_vsc_cs_target_q: list[float],
                                      connected_vsc_cs_target_v: list[float],
                                      connected_vsc_cs_voltage_regulator_on: list[bool],
                                      connected_vsc_cs_p: list[float],
                                      connected_vsc_cs_q: list[float],
                                      disconnected_vsc_cs_ids: list[str]):

        if connected_vsc_cs_ids:
            self._network.update_vsc_converter_stations(id=connected_vsc_cs_ids,
                                                        target_q=connected_vsc_cs_target_q,
                                                        target_v=connected_vsc_cs_target_v,
                                                        voltage_regulator_on=connected_vsc_cs_voltage_regulator_on,
                                                        p=connected_vsc_cs_p,
                                                        q=connected_vsc_cs_q)

        if disconnected_vsc_cs_ids:
            self._network.update_vsc_converter_stations(id=disconnected_vsc_cs_ids,
                                                        p=[0.0] * len(disconnected_vsc_cs_ids),
                                                        q=[0.0] * len(disconnected_vsc_cs_ids))

        self._vsc_converter_stations = self._build_vsc_converter_stations(self._network, self._buses)

    def update_hvdc_lines(self, hvdc_line_ids: list[str], hvdc_line_target_p: list[float]):
        self._network.update_hvdc_lines(id=hvdc_line_ids, target_p=hvdc_line_target_p)
        self._hvdc_lines = self._build_hvdc_lines(self._network, self._vsc_converter_stations, self._lcc_converter_stations, self._buses)

    def update_static_var_compensators(self,
                                       connected_svc_ids: list[str],
                                       connected_svc_target_q: list[float],
                                       connected_svc_target_v: list[float],
                                       connected_svc_regulation_mode: list[str],
                                       connected_svc_p: list[float],
                                       connected_svc_q: list[float],
                                       disconnected_svc_ids: list[str]):
        if connected_svc_ids:
            self._network.update_static_var_compensators(id=connected_svc_ids,
                                                         target_q=connected_svc_target_q,
                                                         target_v=connected_svc_target_v,
                                                         regulation_mode=connected_svc_regulation_mode,
                                                         p=connected_svc_p,
                                                         q=connected_svc_q)

        if disconnected_svc_ids:
            self._network.update_static_var_compensators(id=disconnected_svc_ids,
                                                         p=[0.0] * len(disconnected_svc_ids),
                                                         q=[0.0] * len(disconnected_svc_ids))

        self._static_var_compensators = self._build_static_var_compensators(self._network, self._buses)

    def update_shunt_compensators(self,
                                  connected_shunt_ids: list[str],
                                  connected_shunt_p: list[float],
                                  connected_shunt_q: list[float],
                                  disconnected_shunt_ids: list[str]):
        if connected_shunt_ids:
            self._network.update_shunt_compensators(id=connected_shunt_ids,
                                                    p=connected_shunt_p,
                                                    q=connected_shunt_q)

        if disconnected_shunt_ids:
            self._network.update_shunt_compensators(id=disconnected_shunt_ids,
                                                    p=[0.0] * len(disconnected_shunt_ids),
                                                    q=[0.0] * len(disconnected_shunt_ids))

        self._shunts = self._build_shunt_compensators(self._network, self._buses)

    def update_branches(self,
                        branch_ids: list[str],
                        branch_p1: list[float],
                        branch_p2: list[float],
                        branch_q1: list[float],
                        branch_q2: list[float]):
        self._network.update_branches(id=branch_ids,
                                      p1=branch_p1,
                                      p2=branch_p2,
                                      q1=branch_q1,
                                      q2=branch_q2)

        self._branches = self._build_branches(self._network, self._buses)

    def update_transformers_3w(self,
                               t3_ids: list[str],
                               t3_p1: list[float],
                               t3_p2: list[float],
                               t3_p3: list[float],
                               t3_q1: list[float],
                               t3_q2: list[float],
                               t3_q3: list[float],
                               t3_v: list[float],
                               t3_angle: list[float]):
        self._network.update_3_windings_transformers(id=t3_ids,
                                                     p1=t3_p1,
                                                     p2=t3_p2,
                                                     p3=t3_p3,
                                                     q1=t3_q1,
                                                     q2=t3_q2,
                                                     q3=t3_q3)
        self._network.add_elements_properties(id=t3_ids,
                                              v=t3_v,
                                              angle=t3_angle)

        self._transformers_3w = self._build_3w_transformers(self._network, self._buses)

    def update_buses(self,
                     bus_ids: list[str],
                     bus_v_mag: list[float],
                     bus_v_angle: list[float]):
        self._network.update_buses(id=bus_ids, v_mag=bus_v_mag, v_angle=bus_v_angle)
        self._buses = self._build_buses(self._network, self._voltage_levels)

    def update_dangling_lines(self,
                              connected_dl_ids: list[str],
                              connected_dl_v: list[float],
                              connected_dl_angle: list[float],
                              connected_dl_p: list[float],
                              connected_dl_q: list[float],
                              disconnected_dl_ids: list[str]):
        if connected_dl_ids:
            self._network.update_dangling_lines(id=connected_dl_ids,
                                                        p=connected_dl_p,
                                                        q=connected_dl_q)
            self._network.add_elements_properties(id=connected_dl_ids,
                                                          v=connected_dl_v,
                                                          angle=connected_dl_angle)

        if disconnected_dl_ids:
            self._network.update_dangling_lines(id=disconnected_dl_ids,
                                                        p=[0.0] * len(disconnected_dl_ids),
                                                        q=[0.0] * len(disconnected_dl_ids))
            self._network.add_elements_properties(id=disconnected_dl_ids,
                                                          v=[math.nan] * len(disconnected_dl_ids),
                                                          angle=[math.nan] * len(disconnected_dl_ids))

        self._dangling_lines = self._build_dangling_lines(self._network, self._buses)

    def update_dc_nodes(self,
                     dc_node_ids: list[str],
                     dc_node_v: list[float]):
        self._network.update_dc_nodes(id=dc_node_ids, v=dc_node_v)
        self._dc_nodes = self._build_dc_nodes(self._network)

    def update_dc_lines(self,
                        dc_line_ids: list[str],
                        dc_line_i1: list[float],
                        dc_line_i2: list[float]):
        self._network.update_dc_lines(id=dc_line_ids,
                                      i1=dc_line_i1,
                                      i2=dc_line_i2)
        self._dc_lines = self._build_dc_lines(self._network)

    def update_voltage_source_converters(self,
                                         conv_ids: list[str],
                                         conv_p: list[float],
                                         conv_q: list[float],
                                         conv_p_dc1: list[float],
                                         conv_p_dc2: list[float],
                                         conv_target_p: list[float],
                                         conv_target_q:list[float],
                                         conv_target_v_dc: list[float],
                                         conv_target_v_ac: list[float]):
        self._network.update_voltage_source_converters(id=conv_ids,
                                                       p_ac=conv_p,
                                                       q_ac=conv_q,
                                                       p_dc1=conv_p_dc1,
                                                       p_dc2=conv_p_dc2,
                                                       target_p=conv_target_p,
                                                       target_q=conv_target_q,
                                                       target_v_dc=conv_target_v_dc,
                                                       target_v_ac=conv_target_v_ac)
        self._voltage_source_converters = self._build_voltage_source_converters(self._network)
