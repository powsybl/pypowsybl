#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from typing import (
    List as _List,
    Union as _Union
)
from os import PathLike as _PathLike
from datetime import datetime
import pypowsybl._pypowsybl as _pypowsybl
from pypowsybl.network import Network as _Network

class GLSKDocument:
    def __init__(self, handle):
        self._handle = handle

    def get_gsk_time_interval_start(self) -> datetime:
        return datetime.fromtimestamp(_pypowsybl.get_glsk_factors_start_timestamp(self._handle))

    def get_gsk_time_interval_end(self) -> datetime:
        return datetime.fromtimestamp(_pypowsybl.get_glsk_factors_end_timestamp(self._handle))

    def get_countries(self) -> _List[str]:
        return _pypowsybl.get_glsk_countries(self._handle)

    def get_points_for_country(self, network: _Network, country: str, instant: datetime) -> _List[str]:
        return _pypowsybl.get_glsk_injection_keys(network._handle, self._handle, country, int(instant.timestamp()))

    def get_glsk_factors(self, network: _Network, country: str, instant: datetime) -> _List[float]:
        return _pypowsybl.get_glsk_factors(network._handle, self._handle, country, int(instant.timestamp()))

def load(file: _Union[str, _PathLike]) -> GLSKDocument:
    return GLSKDocument(_pypowsybl.create_glsk_importer(file))