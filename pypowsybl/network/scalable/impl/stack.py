from __future__ import annotations

from typing import List, Optional

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType


class StackScalable(Scalable):
    """Scalable based on a stack of scalables."""
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
