# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pandas as pd
from pypowsybl import _pypowsybl
from pypowsybl.utils import create_data_frame_from_series_array


class ShortCircuitAnalysisResult:
    """
    The result of a short-circuit analysis.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    @property
    def fault_results(self) -> pd.DataFrame:
        """
        contains the results, for each fault, in a dataframe representation.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_fault_results(self._handle))

    @property
    def feeder_results(self) -> pd.DataFrame:
        """
        contains the contributions of each feeder to the short circuit current, in a dataframe representation.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_feeder_results(self._handle))

    @property
    def limit_violations(self) -> pd.DataFrame:
        """
        contains a list of all the violations after the fault, in a dataframe representation.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_short_circuit_limit_violations(self._handle))

    @property
    def voltage_bus_results(self) -> pd.DataFrame:
        """
        contains a list of all the short circuit voltage bus results, in a dataframe representation.
        It should be empty when the parameter with_voltage_result is set to false
        """
        return create_data_frame_from_series_array(_pypowsybl.get_short_circuit_bus_results(self._handle))
