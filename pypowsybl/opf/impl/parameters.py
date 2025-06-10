from enum import Enum


class OptimalPowerFlowMode(Enum):
    LOADFLOW = "LOADFLOW"
    REDISPATCHING = "REDISPATCHING"


class OptimalPowerFlowParameters:
    def __init__(self, reactive_bounds_reduction: float = 0.1, twt_split_shunt_admittance = False,
                 mode: OptimalPowerFlowMode = OptimalPowerFlowMode.LOADFLOW) -> None:
        self._reactive_bounds_reduction = reactive_bounds_reduction
        self._twt_split_shunt_admittance = twt_split_shunt_admittance
        self._mode = mode

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    @property
    def twt_split_shunt_admittance(self) -> bool:
        return self._twt_split_shunt_admittance

    @property
    def mode(self) -> OptimalPowerFlowMode:
        return self._mode
