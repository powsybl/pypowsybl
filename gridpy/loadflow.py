#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from _gridpy import LoadFlowParameters
from gridpy.network import Network


def run_ac(network: Network, parameters: LoadFlowParameters = LoadFlowParameters()):
    return _gridpy.run_load_flow(network.ptr, False, parameters)


def run_dc(network: Network, parameters: LoadFlowParameters = LoadFlowParameters()):
    return _gridpy.run_load_flow(network.ptr, True, parameters)
