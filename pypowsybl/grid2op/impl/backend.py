# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
from typing import List

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl._pypowsybl import Grid2opDoubleValueType

import numpy as np

class Backend:
    def __init__(self, network: Network):
        self._network = network

    def __enter__(self):
        self._handle = _pypowsybl.create_grid2op_backend(self._network._handle)
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        _pypowsybl.free_grid2op_backend(self._handle)
        return False

    def get_generator_name(self) -> List[str]:
        return _pypowsybl.get_grid2op_generator_name(self._handle)

    def get_double_value(self, value_type: Grid2opDoubleValueType) -> np.ndarray:
        return _pypowsybl.get_grid2op_double_value(self._handle, value_type)
