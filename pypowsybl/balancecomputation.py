from typing import (
    List as _List
)

import pypowsybl._pypowsybl as _pypowsybl
from pypowsybl.loadflow import Parameters as LoadFlowParameters
from pypowsybl.network import Network as _Network

class BalanceComputationParameters:
    def __init__(self, lfParameters : LoadFlowParameters = None, threshold : float = None, maxNumberIteration : int = None):
        self._init_with_default_values()
        self.load_flow_parameters = lfParameters

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.BalanceComputationParameters()
        self.threshold = default_parameters.threshold
        self.max_number_iterations = default_parameters.max_number_iterations

    def _to_c_parameters(self) -> _pypowsybl.BalanceComputationParameters:
        c_parameters = _pypowsybl.BalanceComputationParameters()
        c_parameters.threshold = self.threshold
        c_parameters.max_number_iterations = self.max_number_iterations
        return c_parameters

class BalanceComputationArea:
    def __init__(self):
        pass

class BalanceComputationResult:
    def __init__(self, res: _pypowsybl.BalanceComputationResult):
        self.res = res

    @property
    def status(self) -> _pypowsybl.BalanceComputationResultStatus:
        return self._res.status

    @property
    def iteration_count(self) -> int:
        return self._res.iteration_count

class BalanceComputation:
    def __init__(self):
        pass

    def run(self, networks : _List[_Network], bcParameters):
        listHandles = []
        for n in networks:
            listHandles.append(n._handle)
        return _pypowsybl.run_balance_computation(listHandles, bcParameters.load_flow_parameters._to_c_parameters(), bcParameters._to_c_parameters())

    def build_computation_area(self):
        pass
