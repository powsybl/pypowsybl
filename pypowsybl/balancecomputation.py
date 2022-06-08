import pypowsybl._pypowsybl as _pypowsybl
from pypowsybl.loadflow import Parameters as LoadFlowParameters

class BalanceComputationParameters:
    def __init__(self, lfParameters = None : LoadFlowParameters, threshold = None, maxNumberIteration = None):
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

class BalanceComputation:
    def __init__(self):
        pass

    def run(self):
        pass

    def build_computation_area(self):
        pass
