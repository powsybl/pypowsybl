# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import LoadFlowComponentStatus as ComponentStatus

ComponentStatus.__name__ = 'ComponentStatus'
ComponentStatus.__module__ = __name__


# Pure python wrapper for C ext object
# although it adds some boiler plate code, it integrates better with tools such as sphinx
class ComponentResult:
    """
    Loadflow result for one connected component of the network.
    """

    def __init__(self, res: _pypowsybl.LoadFlowComponentResult):
        self._res = res

    @property
    def status(self) -> ComponentStatus:
        """Status of the loadflow for this component."""
        return self._res.status

    @property
    def connected_component_num(self) -> int:
        """Number of the connected component."""
        return self._res.connected_component_num

    @property
    def synchronous_component_num(self) -> int:
        """Number of the synchronous component."""
        return self._res.synchronous_component_num

    @property
    def iteration_count(self) -> int:
        """The number of iterations performed by the loadflow."""
        return self._res.iteration_count

    @property
    def slack_bus_id(self) -> str:
        """ID of the slack bus used for this component."""
        return self._res.slack_bus_id

    @property
    def slack_bus_active_power_mismatch(self) -> float:
        """Remaining active power slack at the end of the loadflow"""
        return self._res.slack_bus_active_power_mismatch

    @property
    def distributed_active_power(self) -> float:
        """Active power distributed from slack bus to other buses during the loadflow"""
        return self._res.distributed_active_power

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"connected_component_num={self.connected_component_num!r}" \
               f", synchronous_component_num={self.synchronous_component_num!r}" \
               f", status={self.status.name}" \
               f", iteration_count={self.iteration_count!r}" \
               f", slack_bus_id={self.slack_bus_id!r}" \
               f", slack_bus_active_power_mismatch={self.slack_bus_active_power_mismatch!r}" \
               f", distributed_active_power={self.distributed_active_power!r}" \
               f")"
