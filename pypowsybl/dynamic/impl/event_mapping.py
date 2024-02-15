# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List
from pypowsybl import _pypowsybl as _pp
from .util import EventType
from pypowsybl._pypowsybl import BranchSide


class EventMapping:
    """
    Class to map events
    """

    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_disconnection(self, static_id: str, event_time: float, disconnect_only: BranchSide) -> None:
        """ Creates a equipment disconnection event

        Args:
            static_id (str): network element to disconnect
            event_time (float): timestep at which the event happens
            disconnect_only (BranchSide): the disconnection is made on the provided side only
        """
        _pp.add_event_disconnection(
            self._handle, static_id, event_time, disconnect_only)

    @staticmethod
    def get_possible_events() -> List[EventType]:
        return list(EventType)
