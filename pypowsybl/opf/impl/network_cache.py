import pandas as pd
from pandas import DataFrame

from pypowsybl.network import Network


class NetworkCache:
    def __init__(self, network: Network) -> None:
        self._network = network
        self._network.per_unit = True
        self._voltage_levels = self._build_voltage_levels(network)
        self._buses = self._build_buses(network, self._voltage_levels)
        self._generators = self._build_generators(network)
        self._loads = self._build_loads(network)
        self._shunts = self._build_shunt_compensators(network)
        self._lines = self._build_lines(network)
        self._transformers = self._build_2_windings_transformers(network)
        self._branches = self._build_branches(network)
        self._slack_terminal = self._network.get_extensions('slackTerminal')

    @staticmethod
    def _build_branches(network: Network):
        return network.get_branches(attributes=['bus1_id', 'bus2_id'])

    @staticmethod
    def _build_2_windings_transformers(network: Network):
        return network.get_2_windings_transformers(
            attributes=['bus1_id', 'bus2_id', 'rho', 'alpha', 'r_tap', 'x_tap', 'g_tap', 'b_tap'])

    @staticmethod
    def _build_lines(network: Network):
        return network.get_lines(attributes=['bus1_id', 'bus2_id', 'r', 'x', 'g1', 'g2', 'b1', 'b2'])

    @staticmethod
    def _build_shunt_compensators(network: Network):
        return network.get_shunt_compensators(attributes=['bus_id', 'g', 'b'])

    @staticmethod
    def _build_loads(network: Network):
        return network.get_loads(attributes=['bus_id', 'p0', 'q0'])

    @staticmethod
    def _build_generators(network: Network):
        return network.get_generators(
            attributes=['bus_id', 'min_p', 'max_p', 'min_q_at_target_p', 'max_q_at_target_p', 'target_p'])

    @staticmethod
    def _build_buses(network: Network, voltage_levels: DataFrame):
        return pd.merge(network.get_buses(attributes=['voltage_level_id']), voltage_levels,
                        left_on='voltage_level_id', right_index=True, how='left')

    @staticmethod
    def _build_voltage_levels(network: Network) -> DataFrame:
        return network.get_voltage_levels(attributes=['low_voltage_limit', 'high_voltage_limit'])

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
