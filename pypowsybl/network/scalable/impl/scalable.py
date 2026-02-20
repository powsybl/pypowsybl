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
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None):
        self.type = type
        if min_value is not None:
            self.min_value = min_value
        if max_value is not None:
            self.max_value = max_value

    def scale(self, network: Network, asked: float, parameters: ScalingParameters = ScalingParameters()) -> float:
        c_param = parameters._to_c_parameters()
        return _pp.scale(network._handle, self._handle, c_param, asked)

class ElementScalable(Scalable):
    """Scalable based on a single injection."""
    element_id: str = ""

    def __init__(self, injection_id: str,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None, ):
        self.element_id = injection_id
        super().__init__(type=JavaScalableType.ELEMENT,
                         min_value=min_value, max_value=max_value)
        self._handle = _pp.create_scalable(type=self.type.value, injection_id=self.element_id, min_value=self.min_value,
                                           max_value=self.max_value, children=[])

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
        children_handles = [child._handle for child in scalables] if scalables is not None else []
        super().__init__(type=JavaScalableType.STACK, min_value=min_value, max_value=max_value)
        self._handle = _pp.create_scalable(type=self.type.value, injection_id="", min_value=self.min_value,
                                           max_value=self.max_value, children=children_handles)

class ProportionalScalable(Scalable):
    """Scalable based on a proportional repartition between underlying network elements."""
    children: List[Scalable] = []

    def __init__(self, injection_ids: Optional[List[str]] = None,
                 scalables: Optional[List[Scalable]] = None,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None,):
        if injection_ids is not None and scalables is not None:
            raise RuntimeError('Parameters "injection_ids" and "scalables" are mutually exclusive.')
        if injection_ids is None and scalables is None:
            raise RuntimeError('One of "injection_ids" and "scalables" parameters must be defined.')
        self.children = scalables if scalables is not None else [ElementScalable(injection_id) for injection_id in injection_ids]
        children_handles = [child._handle for child in scalables] if scalables is not None else []
        super().__init__(type=JavaScalableType.STACK, min_value=min_value, max_value=max_value)
        self._handle = _pp.create_scalable(type=self.type.value, injection_id="", min_value=self.min_value,
                                           max_value=self.max_value, children=children_handles)

class UpDownScalable(Scalable):
    """Scalable combining one scalable used to up power and one to bring it down."""
    up_scalable: Scalable
    down_scalable: Scalable

    def __init__(self, up_injection_id: Optional[str], down_injection_id: Optional[str],
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
        super().__init__(type=JavaScalableType.UP_DOWN, min_value=min_value, max_value=max_value)
        self._handle = _pp.create_scalable(type=self.type.value, injection_id="", min_value=self.min_value,
                                           max_value=self.max_value, children=[self.up_scalable._handle, self.down_scalable._handle])
