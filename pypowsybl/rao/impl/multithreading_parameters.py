from pypowsybl._pypowsybl import (
    RaoParameters
)

class MultithreadingParameters:
    def __init__(self, available_cpus: int = None,
                 rao_parameters: RaoParameters = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if available_cpus is not None:
            self.available_cpus = available_cpus

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.available_cpus = c_parameters.available_cpus

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"available_cpus={self.available_cpus!r}" \
               f")"