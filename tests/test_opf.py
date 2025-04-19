#
# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import logging

import pytest

import pypowsybl as pp


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(logging.DEBUG)


def run_opf_then_lf(network: pp.network.Network, iteration_count: int):
    assert pp.opf.run_ac(network)

    lf_parameters = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.PREVIOUS_VALUES)
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED
    assert lf_result[0].iteration_count == iteration_count


def test_esg_tuto1():
    run_opf_then_lf(pp.network.create_eurostag_tutorial_example1_network(), 1)


def test_ieee9():
    run_opf_then_lf(pp.network.create_ieee9(), 1)


def test_ieee14():
    run_opf_then_lf(pp.network.create_ieee14(), 2)


def test_ieee30():
    run_opf_then_lf(pp.network.create_ieee30(), 1)


def test_ieee57():
    run_opf_then_lf(pp.network.create_ieee57(), 1)


def test_ieee118():
    run_opf_then_lf(pp.network.create_ieee118(), 1)


def test_ieee300():
    run_opf_then_lf(pp.network.create_ieee300(), 1)
