from __future__ import annotations

from typing import List, Optional

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType


class StackScalable(Scalable):
    """
    Scalable that applies the asked modification on a list of underlying scalables, using them in order.
    The power change is applied to the first Scalable in the list until it is at its limit, then to the next one.

    Args:
        scalables: The list of underlying Scalable to stack
        injection_ids (in place of scalables): The list of injection ids with which to create the underlying Scalable
        min_value (optional): The minimum active power value the modification can reach
        max_value (optional): The maximum active power value the modification can reach
    """
    children: List[Scalable] = []

    def __init__(self, injection_ids: Optional[List[str]] = None,
                 scalables: Optional[List[Scalable]] = None,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None,):
        if injection_ids is not None and scalables is not None:
            raise RuntimeError('Parameters "injection_ids" and "scalables" are mutually exclusive.')
        if injection_ids is None and scalables is None:
            raise RuntimeError('One of "injection_ids" and "scalables" parameters must be defined.')
        if injection_ids is not None:
            scalables = [ElementScalable(injection_id=name) for name in injection_ids]
        self.children = scalables
        super().__init__(type=JavaScalableType.STACK, min_value=min_value, max_value=max_value,
                         scalables=scalables)

    def __repr__(self) -> str:
        desc: str =  f"{self.__class__.__name__}(" \
                     f"children={self.children},"
        desc += f", min_value={self.min_value}" if self.min_value != -float('inf') else ""
        desc += f", max_value={self.max_value}" if self.max_value != float('inf') else ""
        return desc + f")"