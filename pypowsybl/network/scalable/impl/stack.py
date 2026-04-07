from __future__ import annotations

from typing import List, Optional

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType


class StackScalable(Scalable):
    """
    Scalable that applies the asked modification on a list of underlying scalables, using them in order.
    The power change is applied to the first Scalable in the list until it is at its limit, then to the next one.
    """
    children: List[Scalable] = []

    def __init__(self, scalables: List[Scalable],
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None,):
        self.children = scalables
        super().__init__(type=JavaScalableType.STACK, min_value=min_value, max_value=max_value,
                         scalables=scalables)

    @classmethod
    def from_ids(cls, injection_ids: List[str], min_value: Optional[float] = None, max_value: Optional[float] = None) -> StackScalable:
        """
        Create a StackScalable from a list of injection ids.

        Args:
            injection_ids: The list of injection ids with which to create the underlying Scalable
            min_value (optional): The minimum active power value the modification can reach
            max_value (optional): The maximum active power value the modification can reach
        """
        return cls(scalables=[ElementScalable(injection_id=name) for name in injection_ids],
            min_value=min_value, max_value=max_value)

    @classmethod
    def from_scalables(cls, scalables: List[Scalable], min_value: Optional[float] = None, max_value: Optional[float] = None) -> StackScalable:
        """
        Create a StackScalable from a list of underlying Scalable.

        Args:
            scalables: The list of underlying Scalable to stack
            min_value (optional): The minimum active power value the modification can reach
            max_value (optional): The maximum active power value the modification can reach
        """
        return cls(scalables=scalables, min_value=min_value, max_value=max_value)

    def __repr__(self) -> str:
        desc: str =  f"{self.__class__.__name__}(" \
                     f"children={self.children},"
        desc += f", min_value={self.min_value}" if self.min_value != -float('inf') else ""
        desc += f", max_value={self.max_value}" if self.max_value != float('inf') else ""
        return desc + f")"