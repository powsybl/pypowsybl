# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters,
    ExecutionCondition
)

class SecondPreventiveRaoParameters:
    def __init__(self, execution_condition: Optional[ExecutionCondition] = None,
                 hint_from_first_preventive_rao: Optional[bool] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if execution_condition is not None:
            self.execution_condition = execution_condition
        if hint_from_first_preventive_rao is not None:
            self.hint_from_first_preventive_rao = hint_from_first_preventive_rao

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.execution_condition = c_parameters.execution_condition
        self.hint_from_first_preventive_rao = c_parameters.hint_from_first_preventive_rao

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"execution_condition={self.execution_condition.name}" \
               f", hint_from_first_preventive_rao={self.hint_from_first_preventive_rao!r}" \
               f")"
