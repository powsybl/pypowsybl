#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from gridpy.network import Network
from gridpy.util import ObjectHandle


class LoadFlowResult(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    def is_ok(self) -> bool:
        return _gridpy.is_load_flow_result_ok(self.ptr)


def run(network: Network):
    return LoadFlowResult(_gridpy.run_load_flow(network.ptr))
