
class OptimalPowerFlowParameters:
    def __init__(self) -> None:
        self._reactive_bounds_reduction = 0.1

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction
