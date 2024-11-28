# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

import numpy as np
import numpy.testing as npt
import pytest

import pypowsybl as pp
from pypowsybl import grid2op

TOLERANCE = 1e-6

@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)

def test_backend():
    n = pp.network.create_eurostag_tutorial_example1_network()
    with grid2op.Backend(n) as backend:
        assert ['GEN', 'GEN2'] == backend.get_generator_name()
        npt.assert_allclose(np.array([607.0, 0.0]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([301.0, 0.0]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([24.5, 0.0]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_V), rtol=TOLERANCE, atol=TOLERANCE)
