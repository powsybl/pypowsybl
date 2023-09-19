# Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from datetime import datetime
from typing import List
from pypowsybl.network import Network
from pypowsybl import _pypowsybl


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

    def get_countries(self) -> List[str]:
        return _pypowsybl.get_glsk_countries(self._handle)

    def get_points_for_country(self, network: Network, country: str, instant: datetime) -> List[str]:
        return _pypowsybl.get_glsk_injection_keys(network._handle, self._handle, country, int(instant.timestamp()))

    def get_glsk_factors(self, network: Network, country: str, instant: datetime) -> List[float]:
        return _pypowsybl.get_glsk_factors(network._handle, self._handle, country, int(instant.timestamp()))
