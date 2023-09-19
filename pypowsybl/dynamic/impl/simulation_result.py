# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pandas as pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl.utils import create_data_frame_from_series_array


class SimulationResult:
    """Can only be instantiated by :func:`~Simulation.run`"""

    def __init__(self, handle: _pp.JavaHandle) -> None:
        self._handle = handle
        self._status = _pp.get_dynamic_simulation_results_status(self._handle)
        self._curves = self._get_all_curves()

    def status(self) -> str:
        """
        status of the simulation

        :returns 'Ok' or 'Not OK'
        """
        return self._status

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
        return pd.concat(df_curves, axis=1)
