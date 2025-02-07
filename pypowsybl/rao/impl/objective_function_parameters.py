from pypowsybl._pypowsybl import (
    RaoParameters,
    ObjectiveFunctionType,
    PreventiveStopCriterion,
    CurativeStopCriterion
)

class ObjectiveFunctionParameters:
    def __init__(self, objective_function_type: ObjectiveFunctionType = None,
                 preventive_stop_criterion: PreventiveStopCriterion = None,
                 curative_stop_criterion: CurativeStopCriterion = None,
                 curative_min_obj_improvement: float = None,
                 forbid_cost_increase: bool = None,
                 optimize_curative_if_preventive_unsecure: bool = None,
                 rao_parameters: RaoParameters = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if objective_function_type is not None:
            self.objective_function_type = objective_function_type
        if preventive_stop_criterion is not None:
            self.preventive_stop_criterion = preventive_stop_criterion
        if curative_stop_criterion is not None:
            self.curative_stop_criterion = curative_stop_criterion
        if curative_min_obj_improvement is not None:
            self.curative_min_obj_improvement = curative_min_obj_improvement
        if forbid_cost_increase is not None:
            self.forbid_cost_increase = forbid_cost_increase
        if optimize_curative_if_preventive_unsecure is not None:
            self.optimize_curative_if_preventive_unsecure = optimize_curative_if_preventive_unsecure

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.objective_function_type = c_parameters.objective_function_type
        self.preventive_stop_criterion = c_parameters.preventive_stop_criterion
        self.curative_stop_criterion = c_parameters.curative_stop_criterion
        self.curative_min_obj_improvement = c_parameters.curative_min_obj_improvement
        self.forbid_cost_increase = c_parameters.forbid_cost_increase
        self.optimize_curative_if_preventive_unsecure = c_parameters.optimize_curative_if_preventive_unsecure

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"objective_function_type={self.objective_function_type.name}" \
               f", preventive_stop_criterion={self.preventive_stop_criterion.name}" \
               f", curative_stop_criterion={self.curative_stop_criterion.name}" \
               f", curative_min_obj_improvement={self.curative_min_obj_improvement!r}" \
               f", forbid_cost_increase={self.forbid_cost_increase!r}" \
               f", optimize_curative_if_preventive_unsecure={self.optimize_curative_if_preventive_unsecure!r}" \
               f")"