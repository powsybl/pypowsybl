# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pandas as pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import DynamicSimulationStatus
from pypowsybl.utils import create_data_frame_from_series_array


class SimulationResult:
    """Can only be instantiated by :func:`~Simulation.run`"""

    def __init__(self, handle: _pp.JavaHandle) -> None:
        self._handle = handle
        self._status = _pp.get_dynamic_simulation_results_status(self._handle)
        self._status_text = _pp.get_dynamic_simulation_results_status_text(self._handle)
        self._curves = self._get_all_curves()
        self._fsv = create_data_frame_from_series_array(_pp.get_final_state_values(self._handle))
        self._timeline = create_data_frame_from_series_array(_pp.get_timeline(self._handle))

    def status(self) -> DynamicSimulationStatus:
        """Status of the simulation (SUCCESS or FAILURE)"""
        return self._status

    def status_text(self) -> str:
        """Status text of the simulation (failure description or empty if success)"""
        return self._status_text

    def curves(self) -> pd.DataFrame:
        """Dataframe of the curves results, columns are the curves names and rows are timestep"""
        return self._curves

    def _get_curve(self, curve_name: str) -> pd.DataFrame:
        series_array = _pp.get_dynamic_curve(self._handle, curve_name)
        return create_data_frame_from_series_array(series_array)

    def _get_all_curves(self) -> pd.DataFrame:
        curve_name_lst = _pp.get_all_dynamic_curves_ids(self._handle)
        df_curves = [self._get_curve(curve_name)
                     for curve_name in curve_name_lst]
        return pd.concat(df_curves, axis=1).ffill() if df_curves else pd.DataFrame()

    def final_state_values(self) -> pd.DataFrame:
        """Dataframe of the final state values results, first column is the fsv names, second one the final state values"""
        return self._fsv

    def timeline(self) -> pd.DataFrame:
        """Dataframe of the simulation timeline, first column is the event time, second one the model name and the third one the event message"""
        return self._timeline
