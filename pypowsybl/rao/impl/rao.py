# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
from os import PathLike
from typing import Union, Optional

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from .rao_result import RaoResult
from .crac import Crac
from .glsk import Glsk
from .parameters import Parameters as RaoParameters
from pypowsybl.utils import path_to_str
from pypowsybl.loadflow import Parameters as LfParameters


class Rao:
    """
    Allows to run a remedial action optimisation on a network
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    def set_crac(self, crac: Crac) -> None:
        """
        Set crac to be used for the rao run
        """
        self._crac = crac

    def set_loop_flow_glsk(self, glsk: Glsk) -> None:
        """
        Set loop flow glsk to be used for the rao run
        """
        _pypowsybl.set_loopflow_glsk(self._handle, glsk._handle)

    def set_monitoring_glsk(self, glsk: Glsk) -> None:
        """
        Set the glsk to be used for the rao monitoring
        """
        _pypowsybl.set_monitoring_glsk(self._handle, glsk._handle)

    def run(self, network: Network, parameters: Optional[RaoParameters] = None, rao_provider: str = "SearchTreeRao", crac: Optional[Crac] = None, loop_flow_glsk: Optional[Glsk] = None) -> RaoResult:
        """
        Run a rao from a set of input buffers
        """
        if parameters is None:
            parameters = RaoParameters()

        if crac is not None:
            self._crac = crac
        if loop_flow_glsk is not None:
            self.set_loop_flow_glsk(loop_flow_glsk)

        rao_result = _pypowsybl.run_rao(network=network._handle, crac=self._crac._handle, rao_context=self._handle, parameters=parameters._to_c_parameters(), rao_provider=rao_provider)
        return RaoResult(rao_result, self._crac._handle)

    def run_voltage_monitoring(self, network: Network, rao_result: RaoResult, load_flow_parameters: Optional[LfParameters] = None, provider_str: str = '', crac: Optional[Crac] = None, monitoring_glsk: Optional[Glsk] = None) -> RaoResult:
        """
        """
        p = load_flow_parameters._to_c_parameters() if load_flow_parameters is not None else _pypowsybl.LoadFlowParameters()

        if crac is not None:
            self._crac = crac
        if monitoring_glsk is not None:
            self.set_monitoring_glsk(monitoring_glsk)

        result_handle = _pypowsybl.run_voltage_monitoring(network._handle, rao_result._handle_result, self._crac._handle, self._handle, p, provider_str)
        return RaoResult(result_handle, self._crac._handle)

    def run_angle_monitoring(self, network: Network,  rao_result: RaoResult, load_flow_parameters: Optional[LfParameters] = None, provider_str: str = '', crac: Optional[Crac] = None, monitoring_glsk: Optional[Glsk] = None) -> RaoResult:
        """
        """
        p = load_flow_parameters._to_c_parameters() if load_flow_parameters is not None else _pypowsybl.LoadFlowParameters()

        if crac is not None:
            self._crac = crac
        if monitoring_glsk is not None:
            self.set_monitoring_glsk(monitoring_glsk)

        result_handle = _pypowsybl.run_angle_monitoring(network._handle, rao_result._handle_result, self._crac._handle, self._handle, p, provider_str)
        return RaoResult(result_handle, self._crac._handle)