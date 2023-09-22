# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List

from pypowsybl import _pypowsybl as _pp


class CurveMapping:
    """
    Class to map Curves
    """

    def __init__(self) -> None:
        self._handle = _pp.create_timeseries_mapping()

    def add_curve(self, dynamic_id: str, variable: str) -> None:
        """
        adds one curve mapping

        :param dynamic_id: id of the network's element
        :param variable: variable name to record
        """
        _pp.add_curve(self._handle, dynamic_id, variable)

    def add_curves(self, dynamic_id: str, variables: List[str]) -> None:
        """
        adds curves mapping in batch on a single network element

        :param dynamic_id: id of the network's element
        :param variables: list of variables names to record
        """
        for var in variables:
            self.add_curve(dynamic_id, var)
