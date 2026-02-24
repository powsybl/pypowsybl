from __future__ import annotations

from typing import Optional

from pypowsybl.network.scalable.impl.scalable import Scalable, JavaScalableType


class ElementScalable(Scalable):
    """Scalable based on a single injection."""
    element_id: str = ""

    def __init__(self, injection_id: str,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None):
        self.element_id = injection_id
        super().__init__(type=JavaScalableType.ELEMENT, min_value=min_value,
                         max_value=max_value, injection_id=injection_id)
