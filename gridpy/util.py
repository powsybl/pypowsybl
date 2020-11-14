#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy


class ObjectHandle:
    def __init__(self, ptr):
        self.ptr = ptr

    def __del__(self):
        _gridpy.destroy_object_handle(self.ptr)
