#
# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import logging

import pytest

import pypowsybl as pp
from pypowsybl.opf.impl.opf import OptimalPowerFlowParameters
from pypowsybl.opf.impl.bounds import Bounds


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(1)


def test_reactive_range():
    b = Bounds(10, 20)
    assert b.min_value == 10
    assert b.max_value == 20
    assert str(b) == "[10, 20]"
    assert b.mirror().min_value == -20
    assert b.mirror().max_value == -10
    assert b.mirror().min_value_with_margin == -20.000001
    assert b.mirror().max_value_with_margin == -9.999999
    assert b.contains(10)
    assert b.contains(20)
    assert not b.contains(5)
    assert not b.contains(21)
    assert b.contains(9.99999999)
    assert not b.contains(9.999)
    assert b.contains(20.00000001)
    assert not b.contains(20.001)
    assert b.reduce(0.1).min_value == 11.0
    assert b.reduce(0.1).max_value == 18.0
    assert b.mirror().reduce(0.1).min_value == -18.0
    assert b.mirror().reduce(0.1).max_value == -11.0
    assert b.mirror().reduce(0.1).min_value_with_margin == -18.000001
    assert b.mirror().reduce(0.1).max_value_with_margin == -10.999999


def run_opf_then_lf(network: pp.network.Network, iteration_count: int = 1):
    lf_parameters = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.DC_VALUES,
                                           provider_parameters={'plausibleActivePowerLimit': '10000.0'})
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED

    opf_parameters = OptimalPowerFlowParameters()
    assert pp.opf.run_ac(network, opf_parameters)

    lf_parameters.voltage_init_mode = pp.loadflow.VoltageInitMode.PREVIOUS_VALUES
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED
    assert lf_result[0].iteration_count == iteration_count


def test_esg_tuto1():
    run_opf_then_lf(pp.network.create_eurostag_tutorial_example1_network())


def test_ieee9():
    run_opf_then_lf(pp.network.create_ieee9())


def test_ieee14():
    run_opf_then_lf(pp.network.create_ieee14())


def test_ieee14_open_side_1_branch():
    network = pp.network.create_ieee14()
    network.update_lines(id=['L3-4-1'], connected1=[False])
    run_opf_then_lf(network)
    row = network.get_branches(id=['L3-4-1'], attributes=['p1', 'q1', 'p2', 'q2' ]).head(1)
    assert row.p1.values[0] == 0
    assert row.q1.values[0] == 0
    assert row.p2.values[0] != 0
    assert row.q2.values[0] != 0


def test_ieee14_open_side_2_branch():
    network = pp.network.create_ieee14()
    network.update_lines(id=['L3-4-1'], connected2=[False])
    run_opf_then_lf(network)
    row = network.get_branches(id=['L3-4-1'], attributes=['p1', 'q1', 'p2', 'q2' ]).head(1)
    assert row.p1.values[0] != 0
    assert row.q1.values[0] != 0
    assert row.p2.values[0] == 0
    assert row.q2.values[0] == 0


def test_ieee30():
    run_opf_then_lf(pp.network.create_ieee30())


def test_ieee57():
    run_opf_then_lf(pp.network.create_ieee57())


def test_ieee118():
    run_opf_then_lf(pp.network.create_ieee118())


def test_ieee300():
    run_opf_then_lf(pp.network.create_ieee300())

