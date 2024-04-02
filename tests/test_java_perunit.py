#
# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pytest

import pypowsybl as pp


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
