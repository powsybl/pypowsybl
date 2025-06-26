# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
import json

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import RaoComputationStatus
from pypowsybl.utils import create_data_frame_from_series_array
from pandas import DataFrame

from typing import (
    Dict,
    Any,
    List
)


class RaoResult:
    """
    The result of a rao
    """

    def __init__(self, handle_result: _pypowsybl.JavaHandle, handle_crac: _pypowsybl.JavaHandle):
        self._handle_result = handle_result
        self._handle_crac = handle_crac
        self._status = _pypowsybl.get_rao_result_status(self._handle_result)

    def status(self) -> RaoComputationStatus:
        return self._status

    def get_flow_cnec_results(self) -> DataFrame:
        serie_flow = _pypowsybl.get_flow_cnec_results(self._handle_crac, self._handle_result)
        return create_data_frame_from_series_array(serie_flow)

    def get_angle_cnec_results(self) -> DataFrame:
        serie_flow = _pypowsybl.get_angle_cnec_results(self._handle_crac, self._handle_result)
        return create_data_frame_from_series_array(serie_flow)

    def get_voltage_cnec_results(self) -> DataFrame:
        serie_flow = _pypowsybl.get_voltage_cnec_results(self._handle_crac, self._handle_result)
        return create_data_frame_from_series_array(serie_flow)

    def get_ra_results(self) -> DataFrame:
        serie_flow = _pypowsybl.get_ra_results(self._handle_crac, self._handle_result)
        return create_data_frame_from_series_array(serie_flow)

    def get_cost_results(self) -> DataFrame:
        serie_flow = _pypowsybl.get_cost_results(self._handle_crac, self._handle_result)
        return create_data_frame_from_series_array(serie_flow)

    def get_virtual_cost_names(self) -> List[str]:
        return _pypowsybl.get_virtual_cost_names(self._handle_result)

    def get_virtual_cost_results(self, virtual_cost_name: str) -> DataFrame:
        serie_flow = _pypowsybl.get_virtual_cost_results(self._handle_crac, self._handle_result, virtual_cost_name)
        return create_data_frame_from_series_array(serie_flow)

    def serialize(self, output_file: str) -> None:
        """
        Serialize result to file
        """
        with open(output_file, "wb") as f:
            f.write(self.serialize_to_binary_buffer().getbuffer())

    def serialize_to_binary_buffer(self) -> io.BytesIO:
        """
        Serialize result to BytesIO
        """
        return io.BytesIO(_pypowsybl.serialize_rao_results_to_buffer(self._handle_result, self._handle_crac))

    def to_json(self) -> Dict[str, Any]:
        return json.load(self.serialize_to_binary_buffer())