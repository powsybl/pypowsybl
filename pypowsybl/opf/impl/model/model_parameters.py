class ModelParameters:
    def __init__(self, reactive_bounds_reduction: float, twt_split_shunt_admittance: bool) -> None:
        self._reactive_bounds_reduction = reactive_bounds_reduction
        self._twt_split_shunt_admittance = twt_split_shunt_admittance

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    @property
    def twt_split_shunt_admittance(self) -> bool:
        return self._twt_split_shunt_admittance
