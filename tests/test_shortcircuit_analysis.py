#
# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl as pp
import pypowsybl.network as pn
import pytest
import pathlib
import pandas as pd

TEST_DIR = pathlib.Path(__file__).parent


@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)


def _create_network_with_sc_extensions():
    # reads a network with short'circuit extensions
    n = pn.create_four_substations_node_breaker_network()

    # create some short-circuit extensions
    n.create_extensions('identifiableShortCircuit', id='S1VL1', ip_min=3.2, ip_max=5.1)
    n.create_extensions('generatorShortCircuit', id='GH1', direct_sub_trans_x=9.2, direct_trans_x=2.1,
                        step_up_transformer_x=5)
    return n


def test_default_provider():
    assert '' == pp.shortcircuit.get_default_provider()
    pp.shortcircuit.set_default_provider("provider_test")
    assert 'provider_test' == pp.shortcircuit.get_default_provider()


def test_create_faults():
    sc = pp.shortcircuit.create_analysis()
    n = pp.network.create_four_substations_node_breaker_network()
    buses = n.get_buses()
    branches = n.get_branches()
    sc.set_faults(pd.DataFrame.from_records(index='id', data=[
        {'id': 'F1', 'element_id': buses.index[0], 'r': 1, 'x': 2, 'fault_type': 'BUS_FAULT'},
        {'id': 'F2', 'element_id': branches.index[0], 'r': 1, 'x': 2, 'fault_type': 'BRANCH_FAULT'},
    ]))
    sc.set_bus_fault(bus_id='F3', element_id=buses.index[1], r=1, x=2)
    sc.set_branch_fault(branch_id='F4', element_id=buses.index[1], r=1, x=2, proportional_location=3)


def test_run_analysis():
    # reads a network with short'circuit extensions
    n = _create_network_with_sc_extensions()

    # sets some short-circuit parameters
    pars = pp.shortcircuit.Parameters(with_feeder_result=False, with_limit_violations=False,
                                      with_voltage_result=False, min_voltage_drop_proportional_threshold=0,
                                      study_type=pp.shortcircuit.ShortCircuitStudyType.TRANSIENT,
                                      with_fortescue_result=True)
    assert pars is not None
    assert not pars.with_feeder_result
    assert not pars.with_limit_violations
    assert not pars.with_voltage_result
    assert pars.min_voltage_drop_proportional_threshold == 0
    assert pars.study_type == pp.shortcircuit.ShortCircuitStudyType.TRANSIENT
    assert pars.with_fortescue_result

    # create a short-circuit analysis context
    sc = pp.shortcircuit.create_analysis()
    assert sc is not None

    # define a couple of bus faults on the first two buses
    buses = n.get_buses()

    sc.set_faults(pd.DataFrame.from_records(index='id', data=[
        {'id': 'F1', 'element_id': buses.index[0], 'r': 1, 'x': 2, 'fault_type': 'BUS_FAULT'},
        {'id': 'F2', 'element_id': buses.index[1], 'r': 1, 'x': 2, 'fault_type': 'BUS_FAULT'},
    ]))

    # run the short-circuit analysis using a nonexistent provider
    with pytest.raises(Exception, match='No short-circuit analysis provider for name \'provider-unknown\''):
        results = sc.run(n, pars, 'provider-unknown')
