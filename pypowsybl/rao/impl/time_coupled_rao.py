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
from pandas import DataFrame


class TimeCoupledRao:
    """
    Allows to run a time coupled remedial action optimisation
    """
    def run(self, time_coupled_inputs: TimeCoupledRaoInput, time_coupled_constraints: TimeCoupledConstraints, parameters: Optional[RaoParameters] = None) -> DataFrame:
        """
        Run a rao from a set of input buffers
        """
        if parameters is None:
            parameters = RaoParameters()

        timestamps, networks, cracs = zip(*time_coupled_inputs.get_data())
        str_timestamps = [t.strftime('%Y-%m-%dT%H:%M:%S+01:00') for t in timestamps]
        rao_result = _pypowsybl.run_marmot(timestamps=str_timestamps, networks=[n._handle for n in networks], cracs=[c._handle for c in cracs],
          parameters=parameters._to_c_parameters(), constraints=time_coupled_constraints._handle)

        results = dict()
        for timestamp, crac in zip(str_timestamps, cracs):
            results[timestamp] = RaoResult(rao_result, crac._handle)
        return DataFrame.from_dict(results, orient = 'index')
