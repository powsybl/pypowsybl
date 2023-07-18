from typing import Dict, List, Tuple
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network as _Network


class VoltageInitializerParameters:
    """
    """

    def __init__(self) -> None:
        self._handle = _pp.create_voltage_initializer_params()

    def add_variable_shunt_compensators(self, shunt_id_list: List[str]) -> None:
        '''
        Indicate to VoltageInitializer that the given generators have a fixed reactance.

        Args:
            shunt_id_list: List of shunt ids.
        '''
        for id in shunt_id_list:
            _pp.voltage_initializer_add_variable_shunt_compensators(
                self._handle, id)

    def add_constant_q_generators(self, generator_id_list: List[str]) -> None:
        '''
        Indicate to VoltageInitializer that the given generators have a fixed reactance.

        Args:
            generator_id_list: List of generator ids.
        '''
        for id in generator_id_list:
            _pp.voltage_initializer_add_constant_q_generators(self._handle, id)

    def add_variable_two_windings_transformers(self, transformer_id_list: List[str]) -> None:
        '''
        Indicate to VoltageInitializer that the given 2wt have a variable ratio.

        Args:
            transformer_id_list: List of transformer ids.
        '''
        for id in transformer_id_list:
            _pp.voltage_initializer_add_variable_two_windings_transformers(
                self._handle, id)

    def add_specific_voltage_limits(self, limits: Dict[str, Tuple[float, float]]) -> None:
        '''
        Indicate to VoltageInitializer to override the network voltages limits. Use this if VoltageInitializer cannot converge because of infeasibility.

        Args:
            limits: A dictionary keys are voltage ids, values are (lower limit, upper limit)
        '''
        for key in limits:
            _pp.voltage_initializer_add_specific_voltage_limits(
                key, limits[key][0], self._handle, limits[key][1])

    def add_algorithm_param(self, parameters_dict: Dict[str, str]) -> None:
        '''
        Add list of entries to VoltageInitializer. Danger zone as it tweaks the model directly.

        Args:
            parameters_dict: algorithm params are stored as (key, values) like a dict
        '''
        for key in parameters_dict:
            _pp.voltage_initializer_add_algorithm_param(
                self._handle, key, parameters_dict[key])

    def set_objective(self, objective: _pp.VoltageInitializerObjective) -> None:
        '''
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call :func:`~VoltageInitializerParameters.set_objective_distance`.

        Args:
            objective: objective function to set for VoltageInitializer.
        '''
        _pp.voltage_initializer_set_objective(self._handle, objective)

    def set_objective_distance(self, distance: float) -> None:
        '''
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call this function.

        Args:
            distance: is in %.
                        A 0% objective means the model will target lower voltage limit.
                        A 100% objective means the model will target upper voltage limit.
        '''
        _pp.voltage_initializer_set_objective_distance(self._handle, distance)


class VoltageInitializerResults:
    """
    Stores the result of an VoltageInitializer run.
    """

    def __init__(self, result_handle: _pp.JavaHandle) -> None:
        self._handle = result_handle
        self._status: _pp.VoltageInitializerStatus = _pp.voltage_initializer_get_status(
            self._handle)
        self._indicators: Dict[str, str] = _pp.voltage_initializer_get_indicators(
            self._handle)

    def apply_all_modification(self, network: _Network) -> None:
        '''
        Apply all the modifications VoltageInitializer found.

        Args:
            network: the network on which the modifications are to be applied.
        '''
        _pp.voltage_initializer_apply_all_modifications(
            self._handle, network._handle)

    def status(self) -> _pp.VoltageInitializerStatus:
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


def run(network: _Network, params: VoltageInitializerParameters, debug: bool) -> VoltageInitializerResults:
    """
    Run VoltageInitializer on the network with the given params.

    Args:
        network: Network on which VoltageInitializer will run
        params: The parameters use to customize the run
        debug: if true, the tmp directory of the VoltageInitializer run will not be erased.
    """
    result_handle = _pp.run_voltage_initializer(
        debug, network._handle, params._handle)
    return VoltageInitializerResults(result_handle)
