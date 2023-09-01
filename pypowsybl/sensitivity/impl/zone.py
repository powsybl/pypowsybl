# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict, List
from pypowsybl._pypowsybl import PyPowsyblError


class Zone:
    def __init__(self, id: str, shift_keys_by_injections_ids: Dict[str, float] = None):
        self._id = id
        self._shift_keys_by_injections_ids = {} if shift_keys_by_injections_ids is None else shift_keys_by_injections_ids

    @property
    def id(self) -> str:
        return self._id

    @property
    def shift_keys_by_injections_ids(self) -> Dict[str, float]:
        return self._shift_keys_by_injections_ids

    @property
    def injections_ids(self) -> List[str]:
        return list(self._shift_keys_by_injections_ids.keys())

    def get_shift_key(self, injection_id: str) -> float:
        shift_key = self._shift_keys_by_injections_ids.get(injection_id)
        if shift_key is None:
            raise PyPowsyblError(f'Injection {injection_id} not found')
        return shift_key

    def add_injection(self, id: str, key: float = 1) -> None:
        self._shift_keys_by_injections_ids[id] = key

    def remove_injection(self, id: str) -> None:
        del self._shift_keys_by_injections_ids[id]

    def move_injection_to(self, other_zone: 'Zone', id: str) -> None:
        shift_key = self.get_shift_key(id)
        other_zone.add_injection(id, shift_key)
        self.remove_injection(id)
