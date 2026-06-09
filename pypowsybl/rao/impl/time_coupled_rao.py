# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from .rao_result import RaoResult
from .crac import Crac
from .parameters import Parameters as RaoParameters
from .time_coupled_constraints import TimeCoupledConstraints
from .time_coupled_rao_input import TimeCoupledRaoInput


class TimeCoupledRao:
    """
    Allows to run a time coupled remedial action optimisation
    """
    def run(self, time_coupled_inputs: TimeCoupledRaoInput, time_coupled_constraints: TimeCoupledConstraints, parameters: Optional[RaoParameters] = None) -> RaoResult:
        """
        Run a rao from a set of input buffers
        """
        if parameters is None:
            parameters = RaoParameters()

        rao_result = _pypowsybl.run_marmot(network=network._handle, crac=crac._handle, rao_context=self._handle, parameters=parameters._to_c_parameters(), rao_provider=rao_provider)
        return RaoResult(rao_result, crac._handle)
