from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters,
    ObjectiveFunctionType,
    Unit
)

class ObjectiveFunctionParameters:
    def __init__(self, objective_function_type: Optional[ObjectiveFunctionType] = None,
                 unit: Optional[Unit] = None,
                 curative_min_obj_improvement: Optional[float] = None,
                 enforce_curative_security: Optional[bool] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if objective_function_type is not None:
            self.objective_function_type = objective_function_type
        if unit is not None:
            self.unit = unit
        if curative_min_obj_improvement is not None:
            self.curative_min_obj_improvement = curative_min_obj_improvement
        if enforce_curative_security is not None:
            self.enforce_curative_security = enforce_curative_security

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.objective_function_type = c_parameters.objective_function_type
        self.unit = c_parameters.unit
        self.curative_min_obj_improvement = c_parameters.curative_min_obj_improvement
        self.enforce_curative_security = c_parameters.enforce_curative_security

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"objective_function_type={self.objective_function_type.name}" \
               f", unit={self.unit.name}" \
               f", curative_min_obj_improvement={self.curative_min_obj_improvement!r}" \
               f", enforce_curative_security={self.enforce_curative_security!r}" \
               f")"