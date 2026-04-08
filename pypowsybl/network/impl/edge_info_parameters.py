#  Copyright (c) 2026, RTE (https://www.rte-france.com)
#  This Source Code Form is subject to the terms of the Mozilla Public
#  License, v. 2.0. If a copy of the MPL was not distributed with this
#  file, You can obtain one at http://mozilla.org/MPL/2.0/.
#  SPDX-License-Identifier: MPL-2.0
from pypowsybl._pypowsybl import EdgeInfoType


class EdgeInfoParameters:
    """
    This class represents the type of info stored in each EdgeInfo."""

    def __init__(self, info_side_external: EdgeInfoType = EdgeInfoType.ACTIVE_POWER,
                 info_middle_side1: EdgeInfoType = EdgeInfoType.EMPTY,
                 info_middle_side2: EdgeInfoType = EdgeInfoType.EMPTY,
                 info_side_internal: EdgeInfoType = EdgeInfoType.EMPTY):
        self._info_side_external = info_side_external
        self._info_middle_side1 = info_middle_side1
        self._info_middle_side2 = info_middle_side2
        self._info_side_internal = info_side_internal

    @property
    def info_side_external(self) -> EdgeInfoType:
        """info_side_external"""
        return self._info_side_external

    @property
    def info_middle_side1(self) -> EdgeInfoType:
        """info_middle_side1"""
        return self._info_middle_side1

    @property
    def info_middle_side2(self) -> EdgeInfoType:
        """info_middle_side2"""
        return self._info_middle_side2

    @property
    def info_side_internal(self) -> EdgeInfoType:
        """info_side_internal"""
        return self._info_side_internal

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"info_side_external={self._info_side_external}" \
               f", info_middle_side1={self._info_middle_side1}" \
               f", info_middle_side2={self._info_middle_side2}" \
               f", info_side_internal={self._info_side_internal}" \
               f")"
