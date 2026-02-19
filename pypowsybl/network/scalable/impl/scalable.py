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
from pypowsybl.network.impl.network import Network
from .scaling_parameters import ScalingParameters


class ScalableType(Enum):
    ELEMENT = 1
    STACK = 2
    PROPORTIONAL = 3

class Scalable:
    _handle: _pp.JavaHandle
    type: ScalableType
    min_value: Optional[float]
    max_value: Optional[float]
    element_id: Optional[str] = None
    children: Optional[List[Scalable]] = None


    def __init__(self, type: ScalableType,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None,
                 element_id: Optional[str] = None,
                 children: Optional[List[Scalable]] = None):
        self.type = type
        self.min_value = min_value
        self.max_value = max_value
        self.max_value = max_value
        self.element_id  = element_id
        self.children = children
        children_handles = [child._handle for child in children] if children is not None else []
        _pp.create_scalable(self.type, injection_id=self.element_id, min_value=self.min_value,
                            max_value=self.max_value, children=children_handles)

    @classmethod
    def from_id(cls, injection_id: str, min_value: Optional[float] = None, max_value: Optional[float] = None):
        return cls(type=ScalableType.ELEMENT, element_id=injection_id, min_value=min_value, max_value=max_value)


    @classmethod
    def stack(cls, injection_ids: Optional[List[str]] = None, scalables: Optional[List[Scalable]] = None,
              min_value: Optional[float] = None, max_value: Optional[float] = None):
        if injection_ids is not None and scalables is not None:
            raise RuntimeError('Parameters "injection_ids" and "scalables" are mutually exclusive.')
        if injection_ids is None and scalables is None:
            raise RuntimeError('One of "injection_ids" and "scalables" parameters must be defined.')
        if injection_ids is not None:
            scalables = [Scalable.from_id(injection_id=name) for name in injection_ids]

        return cls(type=ScalableType.STACK, children=scalables, min_value=min_value, max_value=max_value)


    def scale(self, network: Network, asked: float, parameters: ScalingParameters = None) -> float:
        c_scalable = _pp.convert_to_c_scalable(self)
        return _pp.scale(network, c_scalable, parameters, asked)
