# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl import _pypowsybl
from .parameters import Parameters


def parameters_from_c(c_parameters: _pypowsybl.LoadFlowParameters) -> Parameters:
    """
    Converts C struct to python parameters (bypassing python constructor)
    """
    res = Parameters.__new__(Parameters)
    res._init_from_c(c_parameters) # pylint: disable=protected-access
    return res
