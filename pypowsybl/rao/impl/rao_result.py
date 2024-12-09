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

from typing import (
    Dict,
    Any
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