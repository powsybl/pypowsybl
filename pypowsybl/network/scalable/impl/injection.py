#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations

from typing import Optional

from .scalable import Scalable, JavaScalableType


class InjectionScalable(Scalable):
    """
    Scalable based on a single injection.

    Args:
        injection_id: The id of the injection the scalable will modify
        min_value (optional): The minimum active power value the modification can reach
        max_value (optional): The maximum active power value the modification can reach
    """
    injection_id: str = ""

    def __init__(self, injection_id: str,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None):

        self.injection_id = injection_id
        super().__init__(type=JavaScalableType.ELEMENT, min_value=min_value,
                         max_value=max_value, injection_id=injection_id)


    def __repr__(self) -> str:
        desc: str =  f"{self.__class__.__name__}(" \
                     f"injection_id={self.injection_id}"
        desc += f", min_value={self.min_value}" if self.min_value != -float('inf') else ""
        desc += f", max_value={self.max_value}" if self.max_value != float('inf') else ""
        return desc + f")"