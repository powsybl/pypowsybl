# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
import json

from pypowsybl import _pypowsybl
from pypowsybl.utils import path_to_str
from typing import Union, Dict, Any
from os import PathLike

class Parameters:
    def __init__(self) -> None:
        self._init_with_default_values()

    def _init_with_default_values(self) -> None:
        self._handle = _pypowsybl.create_default_rao_parameters()

    def load_from_file_source(self, parameters_file: Union[str, PathLike]) -> None:
        parameters = io.BytesIO(open(path_to_str(parameters_file), "rb").read())
        self.load_from_buffer_source(parameters)

    def load_from_buffer_source(self, parameters_source: io.BytesIO) -> None:
        self._handle = _pypowsybl.load_rao_parameters(parameters_source.getbuffer())

    def serialize(self, output_file: str) -> None:
        with open(output_file, "wb") as f:
            f.write(self.serialize_to_binary_buffer().getbuffer())

    def serialize_to_binary_buffer(self) -> io.BytesIO:
        return io.BytesIO(_pypowsybl.serialize_rao_parameters(self._handle))

    def to_json(self) -> Dict[str, Any]:
        return json.load(self.serialize_to_binary_buffer())