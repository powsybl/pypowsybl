from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters
)

class CostlyMinMarginParameters:
    def __init__(self, shifted_violation_penalty: Optional[float] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if shifted_violation_penalty is not None:
            self.shifted_violation_penalty = shifted_violation_penalty

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.shifted_violation_penalty = c_parameters.shifted_violation_penalty

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"shifted_violation_penalty={self.shifted_violation_penalty!r}" \
               f")"