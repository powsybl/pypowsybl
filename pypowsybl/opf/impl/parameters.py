class OptimalPowerFlowParameters:
    def __init__(self) -> None:
        self._reactive_bounds_reduction = 0.1
        self._twt_split_shunt_admittance = True

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    @property
    def twt_split_shunt_admittance(self) -> bool:
        return self._twt_split_shunt_admittance
