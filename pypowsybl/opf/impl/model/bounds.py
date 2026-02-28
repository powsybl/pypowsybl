import logging
from typing import Any

logger = logging.getLogger(__name__)


class Bounds:
    def __init__(self, min_value: float, max_value: float, margin: float = 1e-6):
        self._min_value = min_value
        self._max_value = max_value
        self._margin = margin

    @property
    def min_value(self) -> float:
        return self._min_value

    @property
    def min_value_with_margin(self) -> float:
        return self._min_value - self._margin

    @property
    def max_value_with_margin(self) -> float:
        return self._max_value + self._margin

    @property
    def max_value(self) -> float:
        return self._max_value

    def contains(self, value: float) -> bool:
        return self.min_value_with_margin <= value <= self.max_value_with_margin

    def reduce(self, reduction: float) -> 'Bounds':
        min_sign = 1 if self._min_value >= 0 else -1
        max_sign = 1 if self._max_value >= 0 else -1
        return Bounds(self._min_value * (1 + min_sign * reduction), self._max_value * (1 - max_sign * reduction))

    def mirror(self) -> 'Bounds':
        return Bounds(-self._max_value, -self._min_value)

    def __repr__(self) -> str:
        return f"[{self._min_value}, {self._max_value}]"

    @staticmethod
    def get_voltage_bounds(_low_voltage_limit: float | None, _high_voltage_limit: float | None, default_voltage_bounds: 'Bounds'):
        return default_voltage_bounds  # FIXME get from voltage level dataframe

    @staticmethod
    def get_reactive_power_bounds(row: Any) -> 'Bounds':
        return Bounds(row.min_q_at_target_p, row.max_q_at_target_p)

    @staticmethod
    def fix(id:str, lb: float, ub: float) -> tuple[float, float]:
        if lb > ub:
            logger.warning(f"{id}, lower bound {lb} is greater than upper bound {ub}")
            return ub, lb
        return lb, ub
