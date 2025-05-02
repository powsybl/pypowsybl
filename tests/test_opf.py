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
from pypowsybl.opf.impl.injection_bounds import InjectionBounds


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(1)


def test_reactive_range():
    rb = InjectionBounds(10, 20)
    assert rb.min_value == 10
    assert rb.max_value == 20
    assert str(rb) == "[10, 20]"
    assert rb.mirror().min_value == -20
    assert rb.mirror().max_value == -10
    assert rb.mirror().min_value_with_margin == -20.000001
    assert rb.mirror().max_value_with_margin == -9.999999
    assert rb.contains(10)
    assert rb.contains(20)
    assert not rb.contains(5)
    assert not rb.contains(21)
    assert rb.contains(9.99999999)
    assert not rb.contains(9.999)
    assert rb.contains(20.00000001)
    assert not rb.contains(20.001)
    assert rb.reduce(0.1).min_value == 11.0
    assert rb.reduce(0.1).max_value == 18.0
    assert rb.mirror().reduce(0.1).min_value == -18.0
    assert rb.mirror().reduce(0.1).max_value == -11.0
    assert rb.mirror().reduce(0.1).min_value_with_margin == -18.000001
    assert rb.mirror().reduce(0.1).max_value_with_margin == -10.999999


def run_opf_then_lf(network: pp.network.Network, iteration_count: int):
    opf_parameters = OptimalPowerFlowParameters()
    assert pp.opf.run_ac(network, opf_parameters)

    lf_parameters = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.PREVIOUS_VALUES,
                                           provider_parameters={'plausibleActivePowerLimit': '10000.0'})
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED
    assert lf_result[0].iteration_count == iteration_count


def test_esg_tuto1():
    run_opf_then_lf(pp.network.create_eurostag_tutorial_example1_network(), 1)


def test_ieee9():
    run_opf_then_lf(pp.network.create_ieee9(), 1)


def test_ieee14():
    run_opf_then_lf(pp.network.create_ieee14(), 1)


def test_ieee14_open_side_1_branch():
    network = pp.network.create_ieee14()
    network.update_lines(id=['L1-2-1'], connected1=[False])
    run_opf_then_lf(network, 1)


def test_ieee14_open_side_2_branch():
    network = pp.network.create_ieee14()
    network.update_lines(id=['L1-2-1'], connected2=[False])
    run_opf_then_lf(network, 1)


def test_ieee30():
    run_opf_then_lf(pp.network.create_ieee30(), 1)


def test_ieee57():
    run_opf_then_lf(pp.network.create_ieee57(), 1)


def test_ieee118():
    run_opf_then_lf(pp.network.create_ieee118(), 1)


def test_ieee300():
    run_opf_then_lf(pp.network.create_ieee300(), 1)
