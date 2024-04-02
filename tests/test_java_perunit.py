#
# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pytest

import pypowsybl as pp
import pandas as pd
from numpy import NaN


def test_per_unit_line():
    net = pp.network.create_four_substations_node_breaker_network()
    net.nominal_apparent_power = 100
    net.per_unit = True
    per_unit_lines = net.get_lines()
    line_s2s3 = per_unit_lines.loc['LINE_S2S3']
    assert 6e-6 == pytest.approx(line_s2s3.r, abs=1e-6)
    assert 1.2e-2 == pytest.approx(line_s2s3.x, abs=1e-2)
    assert 1.1 == pytest.approx(line_s2s3.p1, abs=1e-1)
    assert 1.9 == pytest.approx(line_s2s3.q1, abs=1e-1)
    assert 2.1 == pytest.approx(line_s2s3.i1, abs=1e-1)
    assert -1.1 == pytest.approx(line_s2s3.p2, abs=1e-1)
    assert -1.8 == pytest.approx(line_s2s3.q2, abs=1e-1)
    assert 2.1 == pytest.approx(line_s2s3.i2, abs=1e-1)
    net.update_lines(id='LINE_S2S3', r=9e-6, x=2e-2)
    per_unit_lines = net.get_lines()
    line_s2s3 = per_unit_lines.loc['LINE_S2S3']
    assert 9e-6 == pytest.approx(line_s2s3.r, abs=1e-6)
    assert 2e-2 == pytest.approx(line_s2s3.x, abs=1e-2)


def test_generator_per_unit():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.per_unit = True
    pp.loadflow.run_ac(n)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'energy_source', 'target_p', 'min_p', 'max_p', 'min_q', 'max_q', 'rated_s', 'reactive_limits_kind',
                 'target_v',
                 'target_q', 'voltage_regulator_on', 'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id',
                 'bus_id', 'connected'],
        data=[['GEN', '', 'OTHER', 6.07, -100, 49.99, -100, 100, None, 'MIN_MAX', 1.02, 3.01, True, 'GEN', -3.03,
               -1.12641, 3.16461, 'VLGEN', 'VLGEN_0', True],
              ['GEN2', '', 'OTHER', 6.07, -100, 49.99, -1.79769e+306, 1.79769e+306, None, 'MIN_MAX', 1.02, 3.01, True,
               'GEN2', -3.03, -1.13, 3.16, 'VLGEN', 'VLGEN_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_generators(), check_dtype=False, atol=1e-2)
    generators2 = pd.DataFrame(data=[[6.080, 3.02, 1.1, False, False]],
                               columns=['target_p', 'target_q', 'target_v', 'voltage_regulator_on', 'connected'],
                               index=['GEN'])
    n.update_generators(generators2)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'energy_source', 'target_p', 'min_p', 'max_p', 'min_q', 'max_q', 'rated_s', 'reactive_limits_kind',
                 'target_v',
                 'target_q', 'voltage_regulator_on', 'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id',
                 'bus_id', 'connected'],
        data=[['GEN', '', 'OTHER', 6.08, -100, 49.99, -100, 100, None, 'MIN_MAX', 1.1, 3.02, False, 'GEN', -3.03,
               -1.12641, NaN, 'VLGEN', '', False],
              ['GEN2', '', 'OTHER', 6.07, -100, 49.99, -1.79769e+306, 1.79769e+306, None, 'MIN_MAX', 1.02, 3.01, True,
               'GEN2', -3.03, -1.13, 3.16, 'VLGEN', 'VLGEN_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_generators(), check_dtype=False, atol=1e-2)
