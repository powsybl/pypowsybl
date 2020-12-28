#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from gridpy.network import Network


class SecurityAnalysis:
    def run(self, network: Network):
        return _gridpy.run_security_analysis(network.ptr)

    def add_contingency(self, id: str, *elements: str):
        pass


def create():
    return SecurityAnalysis()
