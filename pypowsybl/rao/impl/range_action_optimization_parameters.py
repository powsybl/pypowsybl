from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters,
    Solver,
    PstModel,
    RaRangeShrinking
)

class RangeActionOptimizationParameters:
    def __init__(self, max_mip_iterations: Optional[int] = None,
                 pst_ra_min_impact_threshold: Optional[float] = None,
                 hvdc_ra_min_impact_threshold: Optional[float] = None,
                 injection_ra_min_impact_threshold: Optional[float] = None,
                 pst_sensitivity_threshold: Optional[float] = None,
                 hvdc_sensitivity_threshold: Optional[float] = None,
                 injection_ra_sensitivity_threshold: Optional[float] = None,
                 pst_model: Optional[PstModel] = None,
                 ra_range_shrinking: Optional[RaRangeShrinking] = None,
                 solver: Optional[Solver] = None,
                 relative_mip_gap: Optional[float] = None,
                 solver_specific_parameters:  Optional[str] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if pst_ra_min_impact_threshold is not None:
            self.pst_ra_min_impact_threshold = pst_ra_min_impact_threshold
        if hvdc_ra_min_impact_threshold is not None:
            self.hvdc_ra_min_impact_threshold = hvdc_ra_min_impact_threshold
        if injection_ra_min_impact_threshold is not None:
            self.injection_ra_min_impact_threshold = injection_ra_min_impact_threshold
        if max_mip_iterations is not None:
            self.max_mip_iterations = max_mip_iterations
        if pst_sensitivity_threshold is not None:
            self.pst_sensitivity_threshold = pst_sensitivity_threshold
        if pst_model is not None:
            self.pst_model = pst_model
        if hvdc_sensitivity_threshold is not None:
            self.hvdc_sensitivity_threshold = hvdc_sensitivity_threshold
        if injection_ra_sensitivity_threshold is not None:
            self.injection_ra_sensitivity_threshold = injection_ra_sensitivity_threshold
        if ra_range_shrinking is not None:
            self.ra_range_shrinking = ra_range_shrinking
        if solver is not None:
            self.solver = solver
        if relative_mip_gap is not None:
            self.relative_mip_gap = relative_mip_gap
        if solver_specific_parameters is not None:
            self.solver_specific_parameters = solver_specific_parameters

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.pst_ra_min_impact_threshold = c_parameters.pst_ra_min_impact_threshold
        self.hvdc_ra_min_impact_threshold = c_parameters.hvdc_ra_min_impact_threshold
        self.injection_ra_min_impact_threshold = c_parameters.injection_ra_min_impact_threshold
        self.max_mip_iterations = c_parameters.max_mip_iterations
        self.pst_sensitivity_threshold = c_parameters.pst_sensitivity_threshold
        self.pst_model = c_parameters.pst_model
        self.hvdc_sensitivity_threshold = c_parameters.hvdc_sensitivity_threshold
        self.injection_ra_sensitivity_threshold = c_parameters.injection_ra_sensitivity_threshold
        self.ra_range_shrinking = c_parameters.ra_range_shrinking
        self.solver = c_parameters.solver
        self.relative_mip_gap = c_parameters.relative_mip_gap
        self.solver_specific_parameters = c_parameters.solver_specific_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"max_mip_iterations={self.max_mip_iterations!r}" \
               f", pst_penalty_cost={self.pst_ra_min_impact_threshold!r}" \
               f", pst_sensitivity_threshold={self.pst_sensitivity_threshold!r}" \
               f", pst_model={self.pst_model.name}" \
               f", hvdc_penalty_cost={self.hvdc_ra_min_impact_threshold!r}" \
               f", hvdc_sensitivity_threshold={self.hvdc_sensitivity_threshold!r}" \
               f", injection_ra_penalty_cost={self.injection_ra_min_impact_threshold!r}" \
               f", injection_ra_sensitivity_threshold={self.injection_ra_sensitivity_threshold!r}" \
               f", ra_range_shrinking={self.ra_range_shrinking.name}" \
               f", solver={self.solver!r}" \
               f", relative_mip_gap={self.relative_mip_gap!r}" \
               f", solver_specific_parameters={self.solver_specific_parameters!r}" \
               f")"