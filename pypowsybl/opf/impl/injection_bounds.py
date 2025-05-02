class InjectionBounds:
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

    def reduce(self, reduction: float) -> 'InjectionBounds':
        min_sign = 1 if self._min_value >= 0 else -1
        max_sign = 1 if self._max_value >= 0 else -1
        return InjectionBounds(self._min_value * (1 + min_sign * reduction), self._max_value * (1 - max_sign * reduction))

    def mirror(self) -> 'InjectionBounds':
        return InjectionBounds(-self._max_value, -self._min_value)

    def __repr__(self) -> str:
        return f"[{self._min_value}, {self._max_value}]"
