#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl._pypowsybl as _pypowsybl
from datetime import datetime

class GLSKImporter:
    def __init__(self, file):
        self.handle = _pypowsybl.create_glsk_importer(file)

    def get_gsk_time_interval_start(self):
        return datetime.fromtimestamp(_pypowsybl.get_glsk_factors_start_timestamp(self.handle))

    def get_gsk_time_interval_end(self):
        return datetime.fromtimestamp(_pypowsybl.get_glsk_factors_end_timestamp(self.handle))

    def get_countries(self):
        return _pypowsybl.get_glsk_countries(self.handle)

    def get_points_for_country(self, country, instant):
        return _pypowsybl.get_glsk_injection_keys(self.handle, country, int(instant.timestamp()))

    def get_glsk_factors(self, country, instant):
        return _pypowsybl.get_glsk_factors(self.handle, country, int(instant.timestamp()))
