#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations

from enum import Enum
from typing import Optional, List

import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import DistributionMode
from pypowsybl.network.impl.network import Network
from .scaling_parameters import ScalingParameters

DistributionMode.__module__ = __name__

class JavaScalableType(Enum):
    ELEMENT = 0
    STACK = 1
    PROPORTIONAL = 2
    UP_DOWN = 3

class Scalable:
    _handle: _pp.JavaHandle
    type: JavaScalableType
    min_value: float = - float('inf')
    max_value: float = float('inf')

    def __init__(self, type: JavaScalableType,
                 injection_id: Optional[str] = None,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None,
                 scalables: Optional[List[Scalable]] = None,
                 percentages: Optional[List[float]] = None):
        self.type = type
        if min_value is not None:
            self.min_value = min_value
        if max_value is not None:
            self.max_value = max_value
        if injection_id is None:
            injection_id = ""
        if scalables is None:
            scalables = []
        if percentages is None:
            percentages = []
        children_handles = [child._handle for child in scalables] if scalables is not None else []
        self._handle = _pp.create_scalable(type=self.type.value, injection_id=injection_id, min_value=self.min_value,
                                           max_value=self.max_value, children=children_handles, percentages=percentages)

    def scale(self, network: Network, asked: float, parameters: ScalingParameters = ScalingParameters()) -> float:
        c_param = parameters._to_c_parameters()
        return _pp.scale(network._handle, self._handle, c_param, asked)


