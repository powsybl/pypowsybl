#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations

from typing import Optional

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType


class UpDownScalable(Scalable):
    """
    Scalable based on two others : one used when scaling the power up and one to scale the power down
    """
    up_scalable: Scalable
    down_scalable: Scalable

    def __init__(self, up_scalable: Scalable, down_scalable: Scalable,
                 min_value: Optional[float] = None, max_value: Optional[float] = None):
        self.up_scalable = up_scalable
        self.down_scalable = down_scalable
        super().__init__(type=JavaScalableType.UP_DOWN, min_value=min_value, max_value=max_value,
                         scalables=[self.up_scalable, self.down_scalable])

    @classmethod
    def from_ids(cls, up_injection_id: str, down_injection_id: str,
                 min_value: Optional[float] = None, max_value: Optional[float] = None) -> UpDownScalable:
        """
        Create an UpDownScalable from two injection ids.

        Args:
            up_injection_id: The id of the injection with which to create the up scalable
            down_injection_id: The id of the injection with which to create the down scalable
            min_value (optional): The minimum active power value the modification can reach
            max_value (optional): The maximum active power value the modification can reach
        """
        return cls(up_scalable=ElementScalable(up_injection_id), down_scalable=ElementScalable(down_injection_id),
                   min_value=min_value, max_value=max_value)

    @classmethod
    def from_scalables(cls, up_scalable: Scalable, down_scalable: Scalable,
                       min_value: Optional[float] = None, max_value: Optional[float] = None) -> UpDownScalable:
        """
        Create an UpDownScalable from two Scalable.

        Args:
            up_scalable: The Scalable used to up power
            down_scalable: The Scalable used to lower power
            min_value (optional): The minimum active power value the modification can reach
            max_value (optional): The maximum active power value the modification can reach
        """
        return cls(up_scalable=up_scalable, down_scalable=down_scalable,
                   min_value=min_value, max_value=max_value)

    def __repr__(self) -> str:
        desc: str =  f"{self.__class__.__name__}(" \
                     f"up_scalable={self.up_scalable}," \
                     f"down_scalable={self.down_scalable}"
        desc += f", min_value={self.min_value}" if self.min_value != -float('inf') else ""
        desc += f", max_value={self.max_value}" if self.max_value != float('inf') else ""
        return desc + f")"