from pypowsybl.opf.impl.cost_function import ReferenceCostFunction, CostFunction


class OptimalPowerFlowParameters:
    def __init__(self) -> None:
        self._reactive_bounds_reduction = 0.1
        self._cost_function = ReferenceCostFunction()

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    @property
    def cost_function(self) -> CostFunction:
        return self._cost_function
