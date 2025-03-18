from pypowsybl._pypowsybl import (
    RaoParameters
)
from pypowsybl.sensitivity import Parameters as SensitivityParameters
from pypowsybl.loadflow.impl.util import parameters_from_c


class LoadFlowAndSensitivityParameters:
    def __init__(self, load_flow_provider: str = None,
                 sensitivity_provider: str = None,
                 sensitivity_parameters: SensitivityParameters = None,
                 sensitivity_failure_overcost: float = None,
                 rao_parameters: RaoParameters = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if load_flow_provider is not None:
            self.load_flow_provider = load_flow_provider
        if sensitivity_provider is not None:
            self.sensitivity_provider = sensitivity_provider
        if sensitivity_failure_overcost is not None:
            self.sensitivity_failure_overcost = sensitivity_failure_overcost
        if sensitivity_parameters is not None:
            self.sensitivity_parameters = sensitivity_parameters

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.load_flow_provider = c_parameters.load_flow_provider
        self.sensitivity_provider = c_parameters.sensitivity_provider
        self.sensitivity_failure_overcost = c_parameters.sensitivity_failure_overcost
        sensitivity_provider_params = dict(zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))
        self.sensitivity_parameters = SensitivityParameters(parameters_from_c(c_parameters.sensitivity_parameters.loadflow_parameters),
                                                            sensitivity_provider_params)

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"load_flow_provider={self.load_flow_provider!r}" \
               f", sensitivity_provider={self.sensitivity_provider!r}" \
               f", sensitivity_failure_overcost={self.sensitivity_failure_overcost!r}" \
               f")"