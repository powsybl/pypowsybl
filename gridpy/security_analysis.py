#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from gridpy.network import Network
from gridpy.util import ObjectHandle


class SecurityAnalysis(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    def run(self, network: Network):
        return _gridpy.run_security_analysis(self.ptr, network.ptr)

    def add_contingency(self, id: str, element_id: str):
        _gridpy.add_contingency_to_security_analysis(self.ptr, id, element_id)


def create():
    return SecurityAnalysis(_gridpy.create_security_analysis())
