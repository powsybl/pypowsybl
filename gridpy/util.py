#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from typing import List
from typing import Callable


class ObjectHandle:
    def __init__(self, ptr):
        self.ptr = ptr

    def __del__(self):
        _gridpy.destroy_object_handle(self.ptr)


class ContingencyContainer(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    def add_single_element_contingency(self, element_id: str, contingency_id: str = None):
        _gridpy.add_contingency(self.ptr, contingency_id if contingency_id else element_id, [element_id])

    def add_multiple_elements_contingency(self, elements_ids: List[str], contingency_id: str):
        _gridpy.add_contingency(self.ptr, contingency_id, elements_ids)

    def add_single_element_contingencies(self, elements_ids: List[str], contingency_id_provider: Callable[[str], str] = None):
        for element_id in elements_ids:
            contingency_id = contingency_id_provider(element_id) if contingency_id_provider else element_id
            _gridpy.add_contingency(self.ptr, contingency_id, [element_id])
