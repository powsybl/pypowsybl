#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from typing import List as _List
from datetime import datetime
import pypowsybl._pypowsybl as _pypowsybl
from _pypowsybl.network import Network as _Network

#GLSKImporter helper class
class GLSKImporter:
    def __init__(self, file: str):
        self.handle = _pypowsybl.create_glsk_importer(file)

    def get_gsk_time_interval_start(self) -> datetime:
        return datetime.fromtimestamp(_pypowsybl.get_glsk_factors_start_timestamp(self.handle))

    def get_gsk_time_interval_end(self) -> datetime:
        return datetime.fromtimestamp(_pypowsybl.get_glsk_factors_end_timestamp(self.handle))

    def get_countries(self) -> _List[str]:
        return _pypowsybl.get_glsk_countries(self.handle)

    def get_points_for_country(self, network: _Network, country: str, instant: datetime) -> _List[str]:
        return _pypowsybl.get_glsk_injection_keys(network._handle, self.handle, country, int(instant.timestamp()))

    def get_glsk_factors(self, network: _Network, country: str, instant: datetime) -> _List[float]:
        return _pypowsybl.get_glsk_factors(network._handle, self.handle, country, int(instant.timestamp()))
