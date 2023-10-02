# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List
from pypowsybl import _pypowsybl as _pp
from .util import EventType


class EventMapping:
    """
    Class to map events
    """

    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_branch_disconnection(self, static_id: str, event_time: float, disconnect_origin: bool,
                                 disconnect_extremity: bool) -> None:
        """ Creates a branch disconnection event

        Args:
            static_id (str): network element to disconnect
            event_time (float): timestep at which the event happens
            disconnect_origin (bool) : the disconnection is made at the origin
            disconnect_extremity (bool) : the disconnection is made at the extremity
        """
        _pp.add_event_branch_disconnection(
            self._handle, static_id, event_time, disconnect_origin, disconnect_extremity)

    def add_injection_disconnection(self, static_id: str, event_time: float, state_event: bool) -> None:
        """ Creates an injection disconnection event

        Args:
            static_id (str): network element to disconnect
            event_time (float): timestep at which the event happens
            state_event (bool): TODO
        """
        _pp.add_event_injection_disconnection(
            self._handle, static_id, event_time, state_event)

    @staticmethod
    def get_possible_events() -> List[EventType]:
        return list(EventType)
