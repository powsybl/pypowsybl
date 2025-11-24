# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.utils import path_to_str
from os import PathLike

from typing import (
    Union,
    TypeVar
)

Self = TypeVar("Self", bound="Crac")

class Crac:
    """
    RAO Crac
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    @classmethod
    def from_file_source(cls, network: Network, crac_file: Union[str, PathLike]) -> Self :
        return Crac.from_buffer_source(network, io.BytesIO(open(path_to_str(crac_file), "rb").read()))

    @classmethod
    def from_buffer_source(cls, network: Network, crac_source: io.BytesIO) -> Self :
        return cls(_pypowsybl.load_crac_source(network._handle, crac_source.getbuffer()))