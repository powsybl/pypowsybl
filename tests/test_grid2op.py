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

TOLERANCE = 1e-3

@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)

def test_backend():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    with grid2op.Backend(n) as backend:
        assert ['VLGEN', 'VLHV1', 'VLHV2', 'VLLOAD'] == backend.get_string_value(grid2op.StringValueType.VOLTAGE_LEVEL_NAME)
        assert ['LOAD'] == backend.get_string_value(grid2op.StringValueType.LOAD_NAME)
        assert ['GEN', 'GEN2'] == backend.get_string_value(grid2op.StringValueType.GENERATOR_NAME)
        assert [] == backend.get_string_value(grid2op.StringValueType.SHUNT_NAME)
        assert ['NHV1_NHV2_1', 'NHV1_NHV2_2', 'NGEN_NHV1', 'NHV2_NLOAD'] == backend.get_string_value(grid2op.StringValueType.BRANCH_NAME)

        npt.assert_array_equal(np.array([3]), backend.get_integer_value(grid2op.IntegerValueType.LOAD_VOLTAGE_LEVEL_NUM))
        npt.assert_array_equal(np.array([0, 0]), backend.get_integer_value(grid2op.IntegerValueType.GENERATOR_VOLTAGE_LEVEL_NUM))
        npt.assert_array_equal(np.array([]), backend.get_integer_value(grid2op.IntegerValueType.SHUNT_VOLTAGE_LEVEL_NUM))
        npt.assert_array_equal(np.array([1, 1, 0, 2]), backend.get_integer_value(grid2op.IntegerValueType.BRANCH_VOLTAGE_LEVEL_NUM_1))
        npt.assert_array_equal(np.array([2, 2, 1, 3]), backend.get_integer_value(grid2op.IntegerValueType.BRANCH_VOLTAGE_LEVEL_NUM_2))

        npt.assert_allclose(np.array([600.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([200.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([147.578618]), backend.get_double_value(grid2op.DoubleValueType.LOAD_V), rtol=TOLERANCE, atol=TOLERANCE)

        npt.assert_allclose(np.array([-302.78, -302.78]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([-112.641, -112.641]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([24.5, 24.5]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_V), rtol=TOLERANCE, atol=TOLERANCE)

        npt.assert_allclose(np.array([]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_V), rtol=TOLERANCE, atol=TOLERANCE)

        npt.assert_allclose(np.array([302.444, 302.444, 605.561, 600.867]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_P1), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([-300.433, -300.433, -604.893, -600.]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_P2), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([98.74, 98.74, 225.282, 274.376]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_Q1), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([-137.188, -137.188, -197.48, -200.0]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_Q2), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([402.142, 402.142, 24.5, 389.952]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_V1), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([389.952, 389.952, 402.142, 147.578]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_V2), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([456.768, 456.768, 15225.756, 977.985]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_I1), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([488.992, 488.992, 913.545, 2474.263]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_I2), rtol=TOLERANCE, atol=TOLERANCE)

        npt.assert_array_equal(np.array([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]), backend.get_integer_value(grid2op.IntegerValueType.TOPO_VECT))

        backend.update_double_value(grid2op.UpdateDoubleValueType.UPDATE_LOAD_P, np.array([630]), np.array([True]))
        npt.assert_allclose(np.array([630.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_P), rtol=TOLERANCE, atol=TOLERANCE)

        backend.update_double_value(grid2op.UpdateDoubleValueType.UPDATE_LOAD_P, np.array([640]), np.array([False]))
        npt.assert_allclose(np.array([630.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_P), rtol=TOLERANCE, atol=TOLERANCE)
