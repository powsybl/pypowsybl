# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

from pypowsybl._pypowsybl import run_voltage_initializer
from pypowsybl.network import Network
from .voltage_initializer_parameters import VoltageInitializerParameters
from .voltage_initializer_results import VoltageInitializerResults


def run(network: Network, params: VoltageInitializerParameters, debug: bool = False) \
        -> VoltageInitializerResults:
    """
    Run voltage initializer on the network with the given params.

    Args:
        network: Network on which voltage initializer will run
        params: The parameters used to customize the run
        debug: if true, the tmp directory of the voltage initializer run will not be erased.
    """
    if params is None:
        params = VoltageInitializerParameters()
    result_handle = run_voltage_initializer(debug, network._handle, params._handle)
    return VoltageInitializerResults(result_handle)
