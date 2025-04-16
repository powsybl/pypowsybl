# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

from pypowsybl import _pypowsybl


class SlackBusResult:
    """
    Result for one slack bus of a synchronous component.
    """

    def __init__(self, res: _pypowsybl.SlackBusResult):
        self._res = res

    @property
    def id(self) -> str:
        """Slack bus ID."""
        return self._res.id

    @property
    def active_power_mismatch(self) -> float:
        """Slack bus active power mismatch (MW)."""
        return self._res.active_power_mismatch

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"id={self.id!r}" \
               f", active_power_mismatch={self.active_power_mismatch!r}" \
               f")"
