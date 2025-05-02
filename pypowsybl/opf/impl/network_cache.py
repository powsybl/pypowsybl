import pandas as pd
from pandas import DataFrame

from pypowsybl.network import Network


class NetworkCache:
    def __init__(self, network: Network) -> None:
        self._network = network
        self._network.per_unit = True
        self._voltage_levels = self._network.get_voltage_levels(attributes=['low_voltage_limit', 'high_voltage_limit'])
        self._buses = pd.merge(self._network.get_buses(attributes=['voltage_level_id']), self._voltage_levels, left_on='voltage_level_id', right_index=True, how='left')
        self._generators = self._network.get_generators(attributes=['bus_id', 'min_p', 'max_p', 'min_q_at_target_p', 'max_q_at_target_p'])
        self._loads = self._network.get_loads(attributes=['bus_id', 'p0', 'q0'])
        self._shunts = self._network.get_shunt_compensators(attributes=['bus_id', 'g', 'b'])
        self._lines = self._network.get_lines(attributes=['bus1_id', 'bus2_id', 'r', 'x', 'g1', 'g2', 'b1', 'b2'])
        self._transformers = self._network.get_2_windings_transformers(attributes=['bus1_id', 'bus2_id', 'r', 'x', 'g', 'b', 'rho', 'alpha'])
        self._branches = self._network.get_branches(attributes=['bus1_id', 'bus2_id'])
        self._slack_terminal = self._network.get_extensions('slackTerminal')

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
