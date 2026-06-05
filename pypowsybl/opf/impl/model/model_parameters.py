#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from enum import Enum

from pypowsybl.opf.impl.model.bounds import Bounds


class SolverType(Enum):
    IPOPT = "IPOPT"
    KNITRO = "KNITRO"


class ModelParameters:
    def __init__(self,
                 reactive_bounds_reduction: float,
                 twt_split_shunt_admittance: bool,
                 default_voltage_bounds: Bounds,
                 solver_type: SolverType,
                 solver_options: dict[str, object]) -> None:
        self._reactive_bounds_reduction = reactive_bounds_reduction
        self._twt_split_shunt_admittance = twt_split_shunt_admittance
        self._default_voltage_bounds = default_voltage_bounds
        self._solver_type = solver_type
        self._solver_options = solver_options

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction

    @property
    def twt_split_shunt_admittance(self) -> bool:
        return self._twt_split_shunt_admittance

    @property
    def default_voltage_bounds(self) -> Bounds:
        return self._default_voltage_bounds

    @property
    def solver_type(self) -> SolverType:
        return self._solver_type

    @property
    def solver_options(self) -> dict[str, object]:
        return self._solver_options
