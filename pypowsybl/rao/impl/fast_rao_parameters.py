# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters
)

class FastRaoParameters:
    def __init__(self, number_of_cnecs_to_add: Optional[int] = None,
                 add_unsecure_cnecs: Optional[bool] = None,
                 margin_limit: Optional[float] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()
        if number_of_cnecs_to_add is not None:
            self.number_of_cnecs_to_add = number_of_cnecs_to_add
        if add_unsecure_cnecs is not None:
            self.add_unsecure_cnecs = add_unsecure_cnecs
        if margin_limit is not None:
            self.margin_limit = margin_limit

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.number_of_cnecs_to_add = c_parameters.number_of_cnecs_to_add
        self.add_unsecure_cnecs = c_parameters.add_unsecure_cnecs
        self.margin_limit = c_parameters.margin_limit

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"number_of_cnecs_to_add={self.number_of_cnecs_to_add!r}" \
               f"add_unsecure_cnecs={self.add_unsecure_cnecs!r}" \
               f"margin_limit={self.margin_limit!r}" \
               f")"
