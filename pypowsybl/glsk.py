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
from pypowsybl import _pypowsybl
from pypowsybl.network import Network as _Network
from pypowsybl.network import _path_to_str


class GLSKDocument:
    """
    Result of GLSK file parsing, provides access to underlying data.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
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
    """
    Loads a GLSK file.

    Args:
        file: path to the GLSK file

    Returns:
        A GLSK document object.
    """
    file = _path_to_str(file)
    return GLSKDocument(_pypowsybl.create_glsk_document(file))
