# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters,
    Solver,
    PstModel,
    RaRangeShrinking
)

class RangeActionOptimizationParameters:
    def __init__(self, pst_ra_min_impact_threshold: Optional[float] = None,
                 hvdc_ra_min_impact_threshold: Optional[float] = None,
                 injection_ra_min_impact_threshold: Optional[float] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if pst_ra_min_impact_threshold is not None:
            self.pst_ra_min_impact_threshold = pst_ra_min_impact_threshold
        if hvdc_ra_min_impact_threshold is not None:
            self.hvdc_ra_min_impact_threshold = hvdc_ra_min_impact_threshold
        if injection_ra_min_impact_threshold is not None:
            self.injection_ra_min_impact_threshold = injection_ra_min_impact_threshold

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.pst_ra_min_impact_threshold = c_parameters.pst_ra_min_impact_threshold
        self.hvdc_ra_min_impact_threshold = c_parameters.hvdc_ra_min_impact_threshold
        self.injection_ra_min_impact_threshold = c_parameters.injection_ra_min_impact_threshold

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", pst_penalty_cost={self.pst_ra_min_impact_threshold!r}" \
               f", hvdc_penalty_cost={self.hvdc_ra_min_impact_threshold!r}" \
               f", injection_ra_penalty_cost={self.injection_ra_min_impact_threshold!r}" \
               f")"
