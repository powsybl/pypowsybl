from typing import List
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network as _Network

class OpenReacParameters:
    """
    """

    def __init__(self) -> None:
        self._handle = _pp.create_open_reac_params()
        pass

    def add_variable_shunt_compensators(self, shunt_id_list: List[str]) -> None:
        for id in shunt_id_list:
            _pp.open_reac_add_variable_shunt_compensators(self._handle, id)

    def add_constant_q_generators(self, generator_id: List[str]) -> None:
        for id in generator_id:
            _pp.open_reac_add_constant_q_generators(self._handle, id)

    def add_variable_two_windings_transformers(self, transformer_id: List[str]) -> None:
        for id in transformer_id:
            _pp.open_reac_add_variable_two_windings_transformers(
                self._handle, id)

    def add_specific_voltage_limits(self, voltage_id: List[str],
                                    lower_limit: List[float],
                                    upper_limit: List[float]) -> None:
        nb_voltages_ids = len(voltage_id)
        if nb_voltages_ids == len(lower_limit) and nb_voltages_ids == len(upper_limit):
            for i in range(nb_voltages_ids):
                _pp.open_reac_add_specific_voltage_limits(
                    voltage_id[i], lower_limit[i], self._handle, upper_limit[i])
        else:
            raise TypeError(f"error of length of {voltage_id}, {lower_limit}, {upper_limit}")

    def add_algorithm_param(self, keys: List[str], values: List[str]) -> None:
        if len(keys) == len(values):
            for i in range(len(values)):
                _pp.open_reac_add_algorithm_param(
                    self._handle, keys[i], values[i])
        else:
            raise TypeError(f"error of length of {keys}, {values}")

    def set_objective(self, objective: _pp.OpenReacObjective) -> None:
        _pp.open_reac_set_objective(self._handle, objective)

    def set_objective_distance(self, distance: float) -> None:
        _pp.open_reac_set_objective_distance(self._handle, distance)


class OpenReacResults:
    """
    """

    def __init__(self, result_handle: _pp.JavaHandle) -> None:
        self._handle = result_handle

    def apply_all_modification(self, network: _Network) -> None:
        _pp.open_reac_apply_all_modifications(self._handle, network._handle)


def run_open_reac(network: _Network, params: OpenReacParameters, debug: bool) -> OpenReacResults:
    """
    """
    result_handle = _pp.run_open_reac(debug, network._handle, params._handle)
    return OpenReacResults(result_handle)
