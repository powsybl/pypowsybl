# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl._pypowsybl import (
    RaoParameters
)

from typing import Optional


class TopoOptimizationParameters:
    def __init__(self, relative_min_impact_threshold: Optional[float] = None,
                 absolute_min_impact_threshold: Optional[float] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if relative_min_impact_threshold is not None:
            self.relative_min_impact_threshold = relative_min_impact_threshold
        if absolute_min_impact_threshold is not None:
            self.absolute_min_impact_threshold = absolute_min_impact_threshold

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.relative_min_impact_threshold = c_parameters.relative_min_impact_threshold
        self.absolute_min_impact_threshold = c_parameters.absolute_min_impact_threshold

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", relative_min_impact_threshold={self.relative_min_impact_threshold!r}" \
               f", absolute_min_impact_threshold={self.absolute_min_impact_threshold!r}" \
               f")"
