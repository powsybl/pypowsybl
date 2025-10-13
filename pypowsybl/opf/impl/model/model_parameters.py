from pypowsybl.opf.impl.model.bounds import Bounds


class ModelParameters:
    def __init__(self,
                 reactive_bounds_reduction: float,
                 twt_split_shunt_admittance: bool,
                 default_voltage_bounds: Bounds) -> None:
        self._reactive_bounds_reduction = reactive_bounds_reduction
        self._twt_split_shunt_admittance = twt_split_shunt_admittance
        self._default_voltage_bounds = default_voltage_bounds

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    @property
    def twt_split_shunt_admittance(self) -> bool:
        return self._twt_split_shunt_admittance

    @property
    def default_voltage_bounds(self) -> Bounds:
        return self._default_voltage_bounds