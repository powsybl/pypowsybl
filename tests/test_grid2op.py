# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
import pathlib
import pickle
import tempfile

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
        assert backend.network is n

        npt.assert_array_equal(np.array(['VLGEN', 'VLHV1', 'VLHV2', 'VLLOAD']), backend.get_string_value(grid2op.StringValueType.VOLTAGE_LEVEL_NAME))
        npt.assert_array_equal(np.array(['LOAD']), backend.get_string_value(grid2op.StringValueType.LOAD_NAME))
        npt.assert_array_equal(np.array(['GEN', 'GEN2']), backend.get_string_value(grid2op.StringValueType.GENERATOR_NAME))
        npt.assert_array_equal(np.array([]), backend.get_string_value(grid2op.StringValueType.SHUNT_NAME))
        npt.assert_array_equal(np.array(['NHV1_NHV2_1', 'NHV1_NHV2_2', 'NGEN_NHV1', 'NHV2_NLOAD']), backend.get_string_value(grid2op.StringValueType.BRANCH_NAME))

        npt.assert_array_equal(np.array([3]), backend.get_integer_value(grid2op.IntegerValueType.LOAD_VOLTAGE_LEVEL_NUM))
        npt.assert_array_equal(np.array([0, 0]), backend.get_integer_value(grid2op.IntegerValueType.GENERATOR_VOLTAGE_LEVEL_NUM))
        npt.assert_array_equal(np.array([]), backend.get_integer_value(grid2op.IntegerValueType.SHUNT_VOLTAGE_LEVEL_NUM))
        npt.assert_array_equal(np.array([1, 1, 0, 2]), backend.get_integer_value(grid2op.IntegerValueType.BRANCH_VOLTAGE_LEVEL_NUM_1))
        npt.assert_array_equal(np.array([2, 2, 1, 3]), backend.get_integer_value(grid2op.IntegerValueType.BRANCH_VOLTAGE_LEVEL_NUM_2))

        npt.assert_allclose(np.array([600.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([200.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([147.578618]), backend.get_double_value(grid2op.DoubleValueType.LOAD_V), rtol=TOLERANCE, atol=TOLERANCE)

        npt.assert_allclose(np.array([302.78, 302.78]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([112.641, 112.641]), backend.get_double_value(grid2op.DoubleValueType.GENERATOR_Q), rtol=TOLERANCE, atol=TOLERANCE)
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
        backend.run_pf()
        npt.assert_allclose(np.array([317.697, 317.697, 636.14, 630.954]), backend.get_double_value(grid2op.DoubleValueType.BRANCH_P1), rtol=TOLERANCE, atol=TOLERANCE)

        backend.update_double_value(grid2op.UpdateDoubleValueType.UPDATE_LOAD_P, np.array([640]), np.array([False]))
        npt.assert_allclose(np.array([630.0]), backend.get_double_value(grid2op.DoubleValueType.LOAD_P), rtol=TOLERANCE, atol=TOLERANCE)

        backend.update_integer_value(grid2op.UpdateIntegerValueType.UPDATE_LOAD_BUS, np.array([-1]), np.array([True]))
        npt.assert_array_equal(np.array([1, 1, 1, 1, 1, 1, 1, 1, 1, -1, 1]), backend.get_integer_value(grid2op.IntegerValueType.TOPO_VECT))

        backend.update_integer_value(grid2op.UpdateIntegerValueType.UPDATE_GENERATOR_BUS, np.array([2, -1]), np.array([True, True]))
        npt.assert_array_equal(np.array([2, -1, 1, 1, 1, 1, 1, 1, 1, -1, 1]), backend.get_integer_value(grid2op.IntegerValueType.TOPO_VECT))

        npt.assert_allclose(np.array([500.0, 1100.0, 999999.0, 999999.0]),
                            backend.get_double_value(grid2op.DoubleValueType.BRANCH_PERMANENT_LIMIT_A), rtol=TOLERANCE, atol=TOLERANCE)


def test_backend_ieee14():
    n = pp.network.create_ieee14()
    pp.loadflow.run_ac(n)
    with grid2op.Backend(n) as backend:
        npt.assert_array_equal(np.array(['B9-SH']), backend.get_string_value(grid2op.StringValueType.SHUNT_NAME))
        npt.assert_allclose(np.array([0.0]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_P), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([-21.184]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([12.671]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_V), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([141.075, 136.35, 137.385, 137.634, 12.84, 12.671,  12.611,  12.682,  12.662, 12.604, 12.426]), backend.get_double_value(grid2op.DoubleValueType.LOAD_V), rtol=TOLERANCE, atol=TOLERANCE)

        backend.update_integer_value(grid2op.UpdateIntegerValueType.UPDATE_SHUNT_BUS, np.array([-1]), np.array([True]))
        npt.assert_allclose(np.array([-1]), backend.get_integer_value(grid2op.IntegerValueType.SHUNT_LOCAL_BUS))
        backend.run_pf()
        npt.assert_allclose(np.array([0.0]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_Q), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([0.0]), backend.get_double_value(grid2op.DoubleValueType.SHUNT_V), rtol=TOLERANCE, atol=TOLERANCE)
        npt.assert_allclose(np.array([141.075, 136.35, 136.9, 137.313, 12.84, 12.398, 12.385, 12.567, 12.641, 12.564, 12.252]), backend.get_double_value(grid2op.DoubleValueType.LOAD_V), rtol=TOLERANCE, atol=TOLERANCE)


def test_backend_copy():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    with grid2op.Backend(n) as backend:
        backend.update_double_value(grid2op.UpdateDoubleValueType.UPDATE_LOAD_P, np.array([630]), np.array([True]))
        pp.loadflow.run_ac(n)

        with tempfile.TemporaryDirectory() as tmp_dir_name:
            tmp_dir_path = pathlib.Path(tmp_dir_name)
            data_file = tmp_dir_path.joinpath('data.pkl')
            with open(data_file, 'wb') as f:
                pickle.dump(backend, f)
            with open(data_file, 'rb') as f:
                with pickle.load(f) as backend2:
                    npt.assert_allclose(np.array([630.0]), backend2.get_double_value(grid2op.DoubleValueType.LOAD_P), rtol=TOLERANCE, atol=TOLERANCE)


def test_backend_disconnection_issue():
    n = pp.network.create_ieee14()
    pp.loadflow.run_ac(n)
    with grid2op.Backend(n, check_isolated_and_disconnected_injections=False) as backend:
        npt.assert_array_equal(np.array([1] * 56), # all is connected to bus 1
                               backend.get_integer_value(grid2op.IntegerValueType.TOPO_VECT))
        npt.assert_array_equal(np.array(['L1-2-1', 'L1-5-1', 'L2-3-1', 'L2-4-1', 'L2-5-1', 'L3-4-1', 'L4-5-1', 'L6-11-1', 'L6-12-1', 'L6-13-1', 'L7-8-1', 'L7-9-1', 'L9-10-1', 'L9-14-1', 'L10-11-1', 'L12-13-1', 'L13-14-1', 'T4-7-1', 'T4-9-1', 'T5-6-1']),
                               backend.get_string_value(grid2op.StringValueType.BRANCH_NAME))
        # disconnect L7-8-1
        backend.update_integer_value(grid2op.UpdateIntegerValueType.UPDATE_BRANCH_BUS1, np.array([1] * 10 + [-1] + [1] * 9), np.array([False] * 10 + [True] + [False] * 9))
        backend.update_integer_value(grid2op.UpdateIntegerValueType.UPDATE_BRANCH_BUS2, np.array([1] * 10 + [-1] + [1] * 9), np.array([False] * 10 + [True] + [False] * 9))
        backend.run_pf()
        # we can see than L7-8-1 is disconnected at both side but also generator at bus 8
        npt.assert_array_equal(np.array([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, 1, 1, -1,
                                         -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                         1, 1, 1, 1, 1]),
                               backend.get_integer_value(grid2op.IntegerValueType.TOPO_VECT))
