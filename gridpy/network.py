#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from gridpy.util import ObjectHandle


class Network(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    def get_buses(self):
        return _gridpy.get_buses(self.ptr)


def create_empty(id: str = "Default") -> Network:
    return Network(_gridpy.create_empty_network(id))


def create_ieee14() -> Network:
    return Network(_gridpy.create_ieee14_network())


def load(file: str) -> Network:
    return Network(_gridpy.load_network(file))
