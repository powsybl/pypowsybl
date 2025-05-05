import pandas as pd
from pandas import DataFrame

from pypowsybl.network import Network


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
        self._lines = self._build_lines(network, self.buses)
        self._transformers = self._build_2_windings_transformers(network, self.buses)
        self._branches = self._build_branches(network, self.buses)
        self._slack_terminal = self._network.get_extensions('slackTerminal')

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
            (branches_and_buses['connected_component'] == 0) & (branches_and_buses['synchronous_component'] == 0) & (
                    branches_and_buses['connected_component_2'] == 0) & (
                    branches_and_buses['synchronous_component_2'] == 0)]

    @staticmethod
    def _build_branches(network: Network, buses: DataFrame):
        branches = network.get_branches(attributes=['bus1_id', 'bus2_id'])
        return NetworkCache._filter_branches(branches, buses)

    @staticmethod
    def _build_2_windings_transformers(network: Network, buses: DataFrame):
        transfos = network.get_2_windings_transformers(
            attributes=['bus1_id', 'bus2_id', 'rho', 'alpha', 'r_tap', 'x_tap', 'g_tap', 'b_tap'])
        return NetworkCache._filter_branches(transfos, buses)

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
        svcs = network.get_static_var_compensators(attributes=['voltage_level_id', 'bus_id', 'b_min', 'b_max'])
        return NetworkCache._filter_injections(svcs, buses)

    @staticmethod
    def _build_loads(network: Network, buses: DataFrame):
        loads = network.get_loads(attributes=['bus_id', 'p0', 'q0'])
        return NetworkCache._filter_injections(loads, buses)

    @staticmethod
    def _build_generators(network: Network, buses: DataFrame):
        generators = network.get_generators(
            attributes=['bus_id', 'min_p', 'max_p', 'min_q_at_target_p', 'max_q_at_target_p', 'target_p'])
        return NetworkCache._filter_injections(generators, buses)

    @staticmethod
    def _build_buses(network: Network, voltage_levels: DataFrame):
        buses = network.get_buses(attributes=['voltage_level_id', 'connected_component', 'synchronous_component'])
        buses = buses[(buses['synchronous_component'] == 0) & (buses['connected_component'] == 0)]
        return pd.merge(buses, voltage_levels, left_on='voltage_level_id', right_index=True, how='left')

    @staticmethod
    def _build_voltage_levels(network: Network) -> DataFrame:
        return network.get_voltage_levels(attributes=['low_voltage_limit', 'high_voltage_limit', 'nominal_v'])

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
    def lines(self) -> DataFrame:
        return self._lines

    @property
    def transformers(self) -> DataFrame:
        return self._transformers

    @property
    def branches(self) -> DataFrame:
        return self._branches

    @property
    def slack_terminal(self) -> DataFrame:
        return self._slack_terminal
