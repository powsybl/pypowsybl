# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

from typing import Dict
from pypowsybl._pypowsybl import (
    VoltageInitializerStatus,
    voltage_initializer_get_status,
    voltage_initializer_get_indicators,
    voltage_initializer_apply_all_modifications,
    JavaHandle
)
from pypowsybl.network import Network


class VoltageInitializerResults:
    """
    Results of a voltage initializer run.
    """

    def __init__(self, result_handle: JavaHandle) -> None:
        self._handle = result_handle
        self._status: VoltageInitializerStatus = voltage_initializer_get_status(self._handle)
        self._indicators: Dict[str, str] = voltage_initializer_get_indicators(self._handle)

    def apply_all_modifications(self, network: Network) -> None:
        """
        Apply all the modifications voltage initializer found to the network.

        Args:
            network: the network on which the modifications are to be applied.
        """
        voltage_initializer_apply_all_modifications(self._handle, network._handle)

    @property
    def status(self) -> VoltageInitializerStatus:
        """
        If the optimisation failed, it can be useful to check the indicators.
        Returns:
            The status of the optimisation
        """
        return self._status

    @property
    def indicators(self) -> Dict[str, str]:
        """
        Returns:
            The indicators as a dict of the optimisation
        """
        return self._indicators
