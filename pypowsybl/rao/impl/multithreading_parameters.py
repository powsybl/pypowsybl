from pypowsybl._pypowsybl import (
    RaoParameters
)

class MultithreadingParameters:
    def __init__(self, contingency_scenarios_in_parallel: int = None,
                 preventive_leaves_in_parallel: int = None,
                 auto_leaves_in_parallel: int = None,
                 curative_leaves_in_parallel: int = None,
                 rao_parameters: RaoParameters = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if contingency_scenarios_in_parallel is not None:
            self.contingency_scenarios_in_parallel = contingency_scenarios_in_parallel
        if preventive_leaves_in_parallel is not None:
            self.preventive_leaves_in_parallel = preventive_leaves_in_parallel
        if auto_leaves_in_parallel is not None:
            self.auto_leaves_in_parallel = auto_leaves_in_parallel
        if curative_leaves_in_parallel is not None:
            self.curative_leaves_in_parallel = curative_leaves_in_parallel

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.contingency_scenarios_in_parallel = c_parameters.contingency_scenarios_in_parallel
        self.preventive_leaves_in_parallel = c_parameters.preventive_leaves_in_parallel
        self.auto_leaves_in_parallel = c_parameters.auto_leaves_in_parallel
        self.curative_leaves_in_parallel = c_parameters.curative_leaves_in_parallel

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"contingency_scenarios_in_parallel={self.contingency_scenarios_in_parallel!r}" \
               f", preventive_leaves_in_parallel={self.preventive_leaves_in_parallel!r}" \
               f", auto_leaves_in_parallel={self.auto_leaves_in_parallel!r}" \
               f", curative_leaves_in_parallel={self.curative_leaves_in_parallel!r}" \
               f")"