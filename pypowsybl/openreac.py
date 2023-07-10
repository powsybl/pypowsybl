from typing import List, Union
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network as _Network


class OpenReacParameters:
    # referencing class to self return typing
    ...


class OpenReacParameters:
    """
    """

    def __init__(self) -> None:
        self._handle = _pp.create_open_reac_params()
        pass

    def add_variable_shunt_compensators(self, shunt_id: Union[str, List[str]]) -> OpenReacParameters:
        if type(shunt_id) == list:
            for id in shunt_id:
                _pp.open_reac_add_variable_shunt_compensators(self._handle, id)
        else:
            _pp.open_reac_add_variable_shunt_compensators(
                self._handle, shunt_id)
        return self

    def add_constant_q_generators(self, generator_id: Union[str, List[str]]) -> OpenReacParameters:
        if type(generator_id) == list:
            for id in generator_id:
                _pp.open_reac_add_constant_q_generators(self._handle, id)
        else:
            _pp.open_reac_add_constant_q_generators(
                self._handle, generator_id)
        return self

    def add_variable_two_windings_transformers(self, transformer_id: Union[str, List[str]]) -> OpenReacParameters:
        if type(transformer_id) == list:
            for id in transformer_id:
                _pp.open_reac_add_variable_two_windings_transformers(
                    self._handle, id)
        else:
            _pp.open_reac_add_variable_two_windings_transformers(
                self._handle, transformer_id)
        return self

    def add_specific_voltage_limits(self, voltage_id: Union[str, List[str]],
                                    lower_limit: Union[float, List[float]],
                                    upper_limit: Union[float, List[float]]) -> OpenReacParameters:
        if type(voltage_id) == list:
            nb_voltages_ids = len(voltage_id)
            if type(lower_limit) == list and type(upper_limit) == list and nb_voltages_ids == len(lower_limit) and nb_voltages_ids == len(upper_limit):
                for i in range(nb_voltages_ids):
                    _pp.open_reac_add_specific_voltage_limits(
                        voltage_id[i], lower_limit[i], self._handle, upper_limit[i])
            else:
                raise f"error of length or type of {voltage_id}, {lower_limit}, {upper_limit}"
        else:
            _pp.open_reac_add_specific_voltage_limits(
                voltage_id, lower_limit, self._handle, upper_limit)
        return self

    def add_algorithm_param(self, keys: Union[str, List[str]], values: Union[str, List[str]]) -> OpenReacParameters:
        if type(keys) == list:
            if type(values) == list and len(keys) == len(values):
                for i in range(len(values)):
                    _pp.open_reac_add_algorithm_param(
                        self._handle, keys[i], values[i])
            else:
                raise f"error of length or type of {keys}, {values}"
        else:
            _pp.open_reac_add_algorithm_param(self._handle, keys, values)
        return self

    def set_objective(self, objective: _pp.OpenReacObjective) -> OpenReacParameters:
        _pp.open_reac_set_objective(self._handle, objective)
        return self

    def set_objective_distance(self, distance: float) -> OpenReacParameters:
        _pp.open_reac_set_objective_distance(self._handle, distance)
        return self


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
