from enum import Enum

from pypowsybl.opf.impl.model.model_parameters import SolverType


class OptimalPowerFlowMode(Enum):
    LOADFLOW = "LOADFLOW"
    REDISPATCHING = "REDISPATCHING"


class OptimalPowerFlowParameters:
    def __init__(self,
                 reactive_bounds_reduction: float = 0.1,
                 twt_split_shunt_admittance = False,
                 mode: OptimalPowerFlowMode = OptimalPowerFlowMode.LOADFLOW,
                 default_voltage_bounds: tuple[float, float] = (0.8, 1.1),
                 solver_type: SolverType = SolverType.IPOPT) -> None:
        self._reactive_bounds_reduction = reactive_bounds_reduction
        self._twt_split_shunt_admittance = twt_split_shunt_admittance
        self._mode = mode
        self._default_voltage_bounds = default_voltage_bounds
        self._solver_type = solver_type
        self._solver_options: dict[str, object] = {}

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    def with_reactive_bounds_reduction(self, reactive_bounds_reduction: float) -> "OptimalPowerFlowParameters":
        self._reactive_bounds_reduction = reactive_bounds_reduction
        return self

    @property
    def twt_split_shunt_admittance(self) -> bool:
        return self._twt_split_shunt_admittance

    def with_twt_split_shunt_admittance(self, twt_split_shunt_admittance: bool) -> "OptimalPowerFlowParameters":
        self._twt_split_shunt_admittance = twt_split_shunt_admittance
        return self

    @property
    def mode(self) -> OptimalPowerFlowMode:
        return self._mode

    def with_mode(self, mode: OptimalPowerFlowMode) -> "OptimalPowerFlowParameters":
        self._mode = mode
        return self

    @property
    def default_voltage_bounds(self) -> tuple[float, float]:
        return self._default_voltage_bounds

    def with_default_voltage_bounds(self, bounds: tuple[float, float]) -> "OptimalPowerFlowParameters":
        self._default_voltage_bounds = bounds
        return self

    @property
    def solver_type(self) -> SolverType:
        return self._solver_type

    def with_solver_type(self, solver_type: SolverType) -> "OptimalPowerFlowParameters":
        self._solver_type = solver_type
        return self

    @property
    def solver_options(self) -> dict[str, object]:
        return self._solver_options

    def with_solver_option(self, name: str, value: object) -> "OptimalPowerFlowParameters":
        self._solver_options[name] = value
        return self

    def with_solver_options(self, options: dict[str, object]) -> "OptimalPowerFlowParameters":
        self._solver_options.update(options)
        return self
