# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
from os import PathLike
from typing import (
    Union,
    Any
)

from pypowsybl import _pypowsybl
from pypowsybl.utils import path_to_str


class TimeCoupledConstraints:
    """
    Contraints data for time coupled RAO
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    @classmethod
    def from_file_source(cls, constraints_file: Union[str, PathLike]) -> Any :
        return TimeCoupledConstraints.from_buffer_source(io.BytesIO(open(path_to_str(constraints_file), "rb").read()))

    @classmethod
    def from_buffer_source(cls, constraints_source: io.BytesIO) -> Any :
        return cls(_pypowsybl.load_time_coupled_constraints(constraints_source.getbuffer()))
