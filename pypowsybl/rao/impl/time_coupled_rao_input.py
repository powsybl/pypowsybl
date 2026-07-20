# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from datetime import datetime as Datetime
from typing import (
    Any, List, Tuple
)

from pypowsybl.network import Network
from .crac import Crac


class TimeCoupledRaoInput:
    """
    Input wrapper class for time coupled rao input
    """

    def __init__(self) -> None:
        self._data: List[Tuple[Datetime, Network, Crac]] = []

    def add_temporal_data(self, timestamp: Datetime, network: Network, crac: Crac) -> Any :
        return self._data.append((timestamp, network, crac))

    def get_temporal_data(self) -> List[Tuple[Datetime, Network, Crac]]:
        return self._data
