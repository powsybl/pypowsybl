#
# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
"""The AC/DC optimal power flow against a load flow on the same network.

Under converter control modes the DC part of the problem has as many equations as unknowns, so the
optimiser has no freedom there and must reproduce the load flow's DC state exactly. That makes these
the tests that catch a wrong DC equation or a wrong DC start value - a start value cannot move the
answer of a determinate system, but it can stop the solver reaching it.

Only DC quantities are compared. The AC side is not determinate, so the optimiser may legitimately
settle on a different valid AC operating point.

Two ways this can legitimately stop holding, so read a failure here before assuming a regression.
Declaring a DC limit that the operating point violates makes the solve infeasible rather than
inexact, because a determinate system has nowhere else to go - that is the correct signal, not a
broken test. And replacing the control-mode equalities with bands gives the DC block real freedom,
at which point a load flow is no longer the right oracle for it.
"""
import platform

import pytest

import pypowsybl as pp
import pypowsybl.loadflow as lf
import pypowsybl.opf as opf

if platform.system() == 'Darwin' and platform.machine() == 'x86_64':
    pytest.skip("No version compatible with x86_64 macOS.", allow_module_level=True)

# Without this OpenLoadFlow refuses detailed-DC networks, with a message easy to misread as a lack
# of support.
LOADFLOW_PARAMETERS = lf.Parameters(provider_parameters={"acDcNetwork": "true"})

DETAILED_DC_NETWORKS = ['create_ac_dc_bipolar_network',
                        'create_ac_dc_bipolar_network_with_metallic_return',
                        'create_ac_dc_monopolar_network']


def assert_opf_reproduces_the_load_flow_dc_state(network):
    for result in lf.run_ac(network, parameters=LOADFLOW_PARAMETERS, provider='OpenLoadFlow'):
        assert result.status.name == 'CONVERGED'
    network.per_unit = False
    load_flow_v = network.get_dc_nodes()['v'].copy()
    load_flow_i = network.get_dc_lines()['i1'].copy()

    assert opf.run_ac(network, opf.OptimalPowerFlowParameters(mode=opf.OptimalPowerFlowMode.ACDC))

    network.per_unit = False
    dc_nodes, dc_lines = network.get_dc_nodes(), network.get_dc_lines()
    assert (dc_nodes['v'] - load_flow_v).abs().max() == pytest.approx(0.0, abs=1e-6)
    assert (dc_lines['i1'] - load_flow_i).abs().max() == pytest.approx(0.0, abs=1e-6)

    # The written-back currents must also agree with the written-back voltages.
    for dc_line_id, line in dc_lines.iterrows():
        expected_i1_a = 1000.0 * (dc_nodes.loc[line['dc_node1_id'], 'v']
                                  - dc_nodes.loc[line['dc_node2_id'], 'v']) / line['r']
        assert line['i1'] == pytest.approx(expected_i1_a, abs=1e-6)


@pytest.mark.parametrize('factory_name', DETAILED_DC_NETWORKS)
def test_opf_reproduces_the_load_flow_dc_state(factory_name):
    assert_opf_reproduces_the_load_flow_dc_state(getattr(pp.network, factory_name)())


def test_opf_reproduces_the_load_flow_dc_state_on_a_symmetrical_monopole():
    """The symmetrical monopole, whose DC level is held only by two very large grounding resistors.

    It is the case most sensitive to DC start values, and the one a wrong start-value anchor breaks.
    """
    network = pp.network.create_dc_detailed_vsc_symmetrical_monopole_network()

    # OpenLoadFlow refuses a DC component with no ground, so give it one behind resistors large
    # enough to carry no meaningful current.
    subnetwork = network.get_sub_network('VscSymmetricalMonopole')
    subnetwork.create_dc_nodes(id='dnGround', nominal_v=250.0)
    subnetwork.create_dc_lines(id='dlGroundNeg', dc_node1_id='dcNodeGbNeg',
                               dc_node2_id='dnGround', r=1e10)
    subnetwork.create_dc_lines(id='dlGroundPos', dc_node1_id='dcNodeGbPos',
                               dc_node2_id='dnGround', r=1e10)
    subnetwork.create_dc_grounds(id='dcGround', r=0.0, dc_node_id='dnGround')

    assert_opf_reproduces_the_load_flow_dc_state(network)
