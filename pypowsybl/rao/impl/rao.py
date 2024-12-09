# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
from os import PathLike
from typing import Union

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from .rao_result import RaoResult
from .parameters import Parameters as RaoParameters
from pypowsybl.utils import path_to_str


class Rao:
    """
    Allows to run a remedial action optimisation on a network
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    def set_crac_file_source(self, network: Network, crac_file: Union[str, PathLike]) -> None:
        crac = io.BytesIO(open(path_to_str(crac_file), "rb").read())
        self.set_crac_buffer_source(network, crac)

    def set_crac_buffer_source(self, network: Network, crac_source: io.BytesIO) -> None:
        _pypowsybl.set_crac_source(network._handle, self._handle, crac_source.getbuffer())

    def set_glsk_file_source(self, network: Network, glsk_file: Union[str, PathLike]) -> None:
        glsk = io.BytesIO(open(path_to_str(glsk_file), "rb").read())
        self.set_glsk_buffer_source(network, glsk)

    def set_glsk_buffer_source(self, network: Network, glsk_source: io.BytesIO) -> None:
        _pypowsybl.set_glsk_source(network._handle, self._handle, glsk_source.getbuffer())

    def run(self, network: Network, parameters: RaoParameters = None) -> RaoResult:
        """
        Run a rao from a set of input buffers
        """
        if parameters is None:
            parameters = RaoParameters()

        _pypowsybl.run_rao(network=network._handle, rao_context=self._handle, parameters=parameters._handle)
        result_handle = _pypowsybl.get_rao_result(self._handle)
        crac_handle = _pypowsybl.get_crac(self._handle)
        return RaoResult(result_handle, crac_handle)