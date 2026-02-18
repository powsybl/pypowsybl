# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters
)

class NotOptimizedCnecsParameters:
    def __init__(self, do_not_optimize_curative_cnecs_for_tsos_without_cras: Optional[bool] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if do_not_optimize_curative_cnecs_for_tsos_without_cras is not None:
            self.do_not_optimize_curative_cnecs_for_tsos_without_cras = do_not_optimize_curative_cnecs_for_tsos_without_cras

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.do_not_optimize_curative_cnecs_for_tsos_without_cras = c_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"do_not_optimize_curative_cnecs_for_tsos_without_cras={self.do_not_optimize_curative_cnecs_for_tsos_without_cras!r}" \
               f")"
