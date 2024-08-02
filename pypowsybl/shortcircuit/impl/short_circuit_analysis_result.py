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

    def __init__(self, handle: _pypowsybl.JavaHandle, with_fortescue_result: bool):
        self._handle = handle
        self._with_fortescue_result = with_fortescue_result

    @property
    def fault_results(self) -> pd.DataFrame:
        """
        contains the results, for each fault, in a dataframe representation. The rows are fault ids and the columns are:
        - status: the status of the computation, can be SUCCESS, NO_SHORT_CIRCUIT_DATA (in case the reactances of
        generators are missing), SOLVER_FAILURE or FAILURE
        - short_circuit_power: the value of the short-circuit power (in MVA)
        - time_constant: the duration before reaching the permanent short-circuit current
        - current: the current at the fault, either only the three-phase magnitude or detailed with magnitudes and
        angles on each phase (in A)
        - voltage: the voltage at the fault, either only the three-phase magnitude or detailed with magnitudes and
        angles on each phase (in kV)

        """
        return create_data_frame_from_series_array(_pypowsybl.get_fault_results(self._handle, self._with_fortescue_result))

    @property
    def feeder_results(self) -> pd.DataFrame:
        """
        contains the contributions of each feeder to the short-circuit current, in a dataframe representation. The rows
        are the ids of the contributing feeder IDs, sorted by fault and the columns are the current, either in
        three-phase magnitude or detailed with magnitude and angle for each phase. The current magnitudes are in
        A. If the feeder is a branch or a three-winding transformer, the side to which the result applies.
        The dataframe should be empty if the with_feeder_result parameter is set to false.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_feeder_results(self._handle, self._with_fortescue_result))

    @property
    def limit_violations(self) -> pd.DataFrame:
        """
        contains a list of all the violations after the fault, in a dataframe representation. The rows are the fault ids
        and the id of the equipment where the violation happens. The columns are:
        - subject_name: the name of the equipment where the violation occurs
        - limit_type: the type of limit violation, can be LOW_SHORT_CIRCUIT_CURRENT or HIGH_SHORT_CIRCUIT_CURRENT
        - limit_name
        - limit: the value of the limit that is violated (maximum or minimum admissible short-circuit current) in A
        - acceptable_duration
        - limit_reduction
        - value: the calculated short-circuit current that is too high or too low (in A)
        - side: in case of a limit on a branch, the side where the violation has been detected
        It should be empty when the parameter with_limit_violations is set to false
        """
        return create_data_frame_from_series_array(_pypowsybl.get_short_circuit_limit_violations(self._handle))

    @property
    def voltage_bus_results(self) -> pd.DataFrame:
        """
        contains a list of all the short-circuit voltage bus results, in a dataframe representation.
        The rows are for each fault the IDs of the buses sorted by voltage level ID and the columns are:
        - initial_voltage_magnitude: the initial voltage at the bus in kV
        - voltage_drop_proportional: the voltage drop in percent
        - voltage: the calculated voltage in kV
        It should be empty when the parameter with_voltage_result is set to false.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_short_circuit_bus_results(self._handle, self._with_fortescue_result))
