from __future__ import annotations

from typing import Optional

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType


class UpDownScalable(Scalable):
    """Scalable combining one scalable used to up power and one to bring it down."""
    up_scalable: Scalable
    down_scalable: Scalable

    def __init__(self, up_injection_id: Optional[str] = None, down_injection_id: Optional[str] = None,
                 up_scalable: Optional[Scalable] = None, down_scalable: Optional[Scalable] = None,
                 min_value: Optional[float] = None, max_value: Optional[float] = None):
        if up_injection_id is None and up_scalable is None:
            raise RuntimeError('One of "up_injection_id" and "up_scalable" parameters must be defined.')
        if down_injection_id is None and down_scalable is None:
            raise RuntimeError('One of "down_injection_id" and "down_scalable" parameters must be defined.')
        if up_injection_id is not None and up_scalable is not None:
            raise RuntimeError('Parameters "up_injection_id" and "up_scalable" are mutually exclusive.')
        if down_injection_id is not None and down_scalable is not None:
            raise RuntimeError('Parameters "down_injection_id" and "down_scalable" are mutually exclusive.')
        self.up_scalable = up_scalable if up_scalable is not None else ElementScalable(up_injection_id)
        self.down_scalable = down_scalable if down_scalable is not None else ElementScalable(down_injection_id)
        super().__init__(type=JavaScalableType.UP_DOWN, min_value=min_value, max_value=max_value,
                         scalables=[self.up_scalable, self.down_scalable])

    def __repr__(self) -> str:
        desc: str =  f"{self.__class__.__name__}(" \
                     f"up_scalable={self.up_scalable}," \
                     f"down_scalable={self.down_scalable}"
        desc += f", min_value={self.min_value}" if self.min_value != -float('inf') else ""
        desc += f", max_value={self.max_value}" if self.max_value != float('inf') else ""
        return desc + f")"