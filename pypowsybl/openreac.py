from typing import Dict, List, Tuple
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network as _Network

class OpenReacParameters:
    """
    """

    def __init__(self) -> None:
        self._handle = _pp.create_open_reac_params()

    def add_variable_shunt_compensators(self, shunt_id_list: List[str]) -> None:
        '''
        Indicate to OpenReac that the given generators have a fixed reactance.
        Args:
            shunt_id_list: List of shunt ids.
        '''
        for id in shunt_id_list:
            _pp.open_reac_add_variable_shunt_compensators(self._handle, id)

    def add_constant_q_generators(self, generator_id_list: List[str]) -> None:
        '''
        Indicate to OpenReac that the given generators have a fixed reactance.
        Args:
            generator_id_list: List of generator ids.
        '''
        for id in generator_id_list:
            _pp.open_reac_add_constant_q_generators(self._handle, id)

    def add_variable_two_windings_transformers(self, transformer_id_list: List[str]) -> None:
        '''
        Indicate to OpenReac that the given 2wt have a variable ratio.
        Args:
            transformer_id_list: List of transformer ids.
        '''
        for id in transformer_id_list:
            _pp.open_reac_add_variable_two_windings_transformers(
                self._handle, id)

    def add_specific_voltage_limits(self, limits: Dict[str, Tuple[float, float]]) -> None:
        '''
        Indicate to OpenReac to override the network voltages limits. Use this if OpenReac cannot converge because of infeasibility.
        Args:
            limits: A dictionary keys are voltage ids, values are (lower limit, upper limit)
        '''
        for key in limits:
            _pp.open_reac_add_specific_voltage_limits(
                key, limits[key][0], self._handle, limits[key][1])

    def add_algorithm_param(self, parameters_dict: Dict[str, str]]) -> None:
        '''
        Add list of entries to OpenReac. Danger zone as it tweaks the model directly.
        Args:
            parameters_dict: algorithm params are stored as (key, values) like a dict
        '''
        for key in parameters_dict:
            _pp.open_reac_add_algorithm_param(
    self._handle, key, parameters_dict[key])

    def set_objective(self, objective: _pp.OpenReacObjective) -> None:
        '''
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call set_objective_distance.
        Args:
            objective: objective function to set for OpenReac.
        '''
        _pp.open_reac_set_objective(self._handle, objective)

    def set_objective_distance(self, distance: float) -> None:
        '''
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call this function.
        Args:
            distance: is in %.
                      A 0% objective means the model will target lower voltage limit.
                      A 100% objective means the model will target upper voltage limit.
        '''
        _pp.open_reac_set_objective_distance(self._handle, distance)


class OpenReacResults:
    """
    Stores the result of an OpenReac run.
    """

    def __init__(self, result_handle: _pp.JavaHandle) -> None:
        self._handle = result_handle
        self._status: _pp.OpenReacStatus=_pp.open_reac_get_status(self._handle)
        self._indicators: Dict[str, str] = _pp.open_reac_get_indicators(self._handle)

    def apply_all_modification(self, network: _Network) -> None:
        '''
        Apply all the modifications OpenReac found.
        Args:
            network: the network on which the modifications are to be applied.
        '''
        _pp.open_reac_apply_all_modifications(self._handle, network._handle)

    def status(self) -> _pp.OpenReacStatus:
        '''
        If the optimisation failed, it can be useful to check the indicators.
        Returns:
            The status of the optimisation
        '''
        return self._status

    def indicators(self) -> Dict[str, str]:
        '''
        Returns:
            The indicators as a dict of the optimisation
        '''
        return self._indicators


def run(network: _Network, params: OpenReacParameters, debug: bool) -> OpenReacResults:
    """
    Run OpenReac on the network with the given params.
    Args:
        network: Network on which OpenReac will run
        params: The parameters use to customize the run
        debug: if true, the tmp directory of the OpenReac run will not be erased.
    """
    result_handle = _pp.run_open_reac(debug, network._handle, params._handle)
    return OpenReacResults(result_handle)
