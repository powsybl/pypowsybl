from typing import Dict, List, Tuple
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network as _Network

class OpenReacParameters:
    """
    """

    def __init__(self) -> None:
        self._handle = _pp.create_open_reac_params()

    def add_variable_shunt_compensators(self, shunt_id_list: List[str]) -> None:
        for id in shunt_id_list:
            _pp.open_reac_add_variable_shunt_compensators(self._handle, id)

    def add_constant_q_generators(self, generator_id: List[str]) -> None:
        '''
        Indicate to OpenReac that the given generators have a fixed reactance.
        '''
        for id in generator_id:
            _pp.open_reac_add_constant_q_generators(self._handle, id)

    def add_variable_two_windings_transformers(self, transformer_id: List[str]) -> None:
        '''
        Indicate to OpenReac that the given 2wt have a variable ratio.
        '''
        for id in transformer_id:
            _pp.open_reac_add_variable_two_windings_transformers(
                self._handle, id)

    def add_specific_voltage_limits(self, limits: Dict[str, Tuple[float, float]]) -> None:
        '''
        Indicate to OpenReac to override the network voltages limits. Use this if OpenReac cannot converge because of infeasibility.
        keys are voltage ids, values are (lower limit, upper limit)
        '''
        for key in limits:
            _pp.open_reac_add_specific_voltage_limits(
                key, limits[key][0], self._handle, limits[key][1])

    def add_algorithm_param(self, entries: List[Tuple[str, str]]) -> None:
        for i in range(len(entries)):
            _pp.open_reac_add_algorithm_param(self._handle, entries[i][0], entries[i][1])

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

    def get_status(self) -> _pp.OpenReacStatus:
        return _pp.open_reac_get_status(self._handle)

    def get_indicators(self) -> Dict[str, str]:
        return _pp.open_reac_get_indicators(self._handle)


def run_open_reac(network: _Network, params: OpenReacParameters, debug: bool) -> OpenReacResults:
    """
    """
    result_handle = _pp.run_open_reac(debug, network._handle, params._handle)
    return OpenReacResults(result_handle)
