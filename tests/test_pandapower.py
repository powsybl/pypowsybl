# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import math
import pathlib

import pytest

try:
    import pandapower as pdp
except ImportError:
    pdp = any

import pypowsybl as pp
import logging

EPS_V = 0.001

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'

@pytest.fixture(autouse=True)
def setup():
    logging.basicConfig()
    logging.getLogger('powsybl').setLevel(logging.DEBUG)


def run_and_compare(pdp_n, expected_bus_count: int):
    pdp.runpp(pdp_n, numba=True, enforce_q_lims=False, distributed_slack=True, trafo_model="pi")
    n = pp.network.convert_from_pandapower(pdp_n)
    assert len(n.get_buses()) == expected_bus_count
    param = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.UNIFORM_VALUES,
                                   transformer_voltage_control_on=False,
                                   use_reactive_limits=False,
                                   shunt_compensator_voltage_control_on=False,
                                   phase_shifter_regulation_on=False,
                                   distributed_slack=True)
    results = pp.loadflow.run_ac(n, param)
    assert pp.loadflow.ComponentStatus.CONVERGED == results[0].status
    pdp_v = sorted(list(pdp_n.res_bus['vm_pu'] * pdp_n.bus['vn_kv']))
    buses = n.get_bus_breaker_view_buses()
    v = sorted(list(buses['v_mag']))
    for index, (pdp_v_val, v_val) in enumerate(zip(pdp_v, v)):
        assert pdp_v_val == pytest.approx(v_val, abs=EPS_V, rel=EPS_V), f"Voltage mismatch at index {index}: {pdp_v_val} != {v_val}"


def test_pandapower_case5():
    run_and_compare(pdp.networks.case5(), 5)

def test_pandapower_case4gs():
    run_and_compare(pdp.networks.case4gs(),  4)

def test_pandapower_case6ww():
    run_and_compare(pdp.networks.case6ww(), 6)

def test_pandapower_case9():
    run_and_compare(pdp.networks.case9(), 9)

def test_pandapower_case14():
    run_and_compare(pdp.networks.case14(), 14)

def test_pandapower_case30():
    run_and_compare(pdp.networks.case30(), 30)

def test_pandapower_case_ieee30():
    run_and_compare(pdp.networks.case_ieee30(), 30)

def test_pandapower_case33bw():
    run_and_compare(pdp.networks.case33bw(), 33)

def test_pandapower_case39():
    run_and_compare(pdp.networks.case39(), 39)

def test_pandapower_case57():
    run_and_compare(pdp.networks.case57(), 57)

def test_pandapower_panda_four_load_branch():
    run_and_compare(pdp.networks.panda_four_load_branch(), 6)

def test_pandapower_four_loads_with_branches_out():
    run_and_compare(pdp.networks.four_loads_with_branches_out(), 10)

def test_educ_case14_storage():
    run_and_compare(pdp.from_json(DATA_DIR / 'educ_case14_storage.json'), 14)

def test_switch_conversion():
    # expected_bus_count is 2 because of the switch between bus 1 and bus 2 in the pdp case
    # causing them to be merged into a single bus in pypowsybl
    run_and_compare(pdp.from_json(DATA_DIR / 'switch_conversion_test_case.json'), 2)


def create_transformer_network(net_sn_mva: float, i0_percent: float, pfe_kw: float, parallel: int = 1):
    """
    Slack bus and line on the HV side, load on the LV side, so that the magnetizing branch of the
    transformer has an influence on the load flow result and is not fully absorbed by the slack bus.
    """
    n_pdp = pdp.create_empty_network(sn_mva=net_sn_mva)
    slack_bus = pdp.create_bus(n_pdp, vn_kv=110.0)
    hv_bus = pdp.create_bus(n_pdp, vn_kv=110.0)
    lv_bus = pdp.create_bus(n_pdp, vn_kv=20.0)
    pdp.create_ext_grid(n_pdp, slack_bus, vm_pu=1.0)
    pdp.create_line_from_parameters(n_pdp, slack_bus, hv_bus, length_km=20.0, r_ohm_per_km=0.1,
                                    x_ohm_per_km=0.35, c_nf_per_km=10.0, max_i_ka=1.0)
    pdp.create_transformer_from_parameters(n_pdp, hv_bus=hv_bus, lv_bus=lv_bus,
                                           sn_mva=25.0,
                                           vn_hv_kv=110.0, vn_lv_kv=20.0,
                                           vkr_percent=0.41, vk_percent=12.0,
                                           pfe_kw=pfe_kw, i0_percent=i0_percent,
                                           parallel=parallel)
    pdp.create_load(n_pdp, lv_bus, p_mw=10.0, q_mvar=3.0)
    return n_pdp


def expected_magnetizing_admittance(i0_percent: float, pfe_kw: float, parallel: int = 1):
    """
    Magnetizing branch of the pandapower transformer model, in siemens at the LV side:
    the iron losses give the conductance, and the no load current i0_percent, which is a
    percentage of the *transformer* rated current, gives the magnitude of the admittance.
    Both are quantities of the transformer itself and so do not depend on net.sn_mva.
    """
    pfe_mw = pfe_kw * 1e-3
    ym_mva = i0_percent / 100 * 25.0 # net.trafo.sn_mva is 25 MVA
    bm_mva = -math.sqrt(max(ym_mva ** 2 - pfe_mw ** 2, 0.0))  # clipped, as pandapower does
    zb_lv_factor = 20.0 ** 2 # trafo LV voltage is 20 kV
    return pfe_mw / zb_lv_factor * parallel, bm_mva / zb_lv_factor * parallel


@pytest.mark.parametrize('net_sn_mva, i0_percent, pfe_kw, parallel', [
    (1.0, 0.07, 14.0, 1),
    (100.0, 0.07, 14.0, 1),
    (100.0, 0.6, 14.0, 2),
    # iron losses larger than the no load apparent power: pandapower clips the susceptance to zero
    # instead of computing the square root of a negative number
    (1.0, 0.05, 40.0, 1),
], ids=['net_sn_mva_1', 'net_sn_mva_100', 'parallel', 'clipped_susceptance'])
def test_transformer_magnetizing_admittance(net_sn_mva, i0_percent, pfe_kw, parallel):
    # i0_percent and pfe_kw are given on the transformer rated apparent power, so the resulting
    # g and b must not depend on the apparent power base of the network
    n_pdp = create_transformer_network(net_sn_mva, i0_percent=i0_percent, pfe_kw=pfe_kw, parallel=parallel)
    n = pp.network.convert_from_pandapower(n_pdp)
    trafo = n.get_2_windings_transformers().loc['1_2_1']
    expected_g, expected_b = expected_magnetizing_admittance(i0_percent, pfe_kw, parallel)
    assert expected_g == pytest.approx(trafo['g'], rel=1e-10)
    assert expected_b == pytest.approx(trafo['b'], rel=1e-10)
    # series parameters are not impacted by the apparent power base either
    assert 0.0656 / parallel == pytest.approx(trafo['r'], rel=1e-10)
    assert 1.9188792 / parallel == pytest.approx(trafo['x'], rel=1e-6)
    run_and_compare(n_pdp, 3)


def test_injection_scaling():
    n_pdp = pdp.create_empty_network()
    b1 = pdp.create_bus(n_pdp, vn_kv=110.0)
    b2 = pdp.create_bus(n_pdp, vn_kv=110.0)
    b3 = pdp.create_bus(n_pdp, vn_kv=110.0)
    pdp.create_ext_grid(n_pdp, b1, vm_pu=1.0)
    pdp.create_line(n_pdp, b1, b2, length_km=10.0, std_type='N2XS(FL)2Y 1x300 RM/35 64/110 kV')
    pdp.create_line(n_pdp, b2, b3, length_km=10.0, std_type='N2XS(FL)2Y 1x300 RM/35 64/110 kV')
    pdp.create_load(n_pdp, b2, p_mw=30.0, q_mvar=10.0, scaling=0.8)
    pdp.create_sgen(n_pdp, b2, p_mw=10.0, q_mvar=2.0, scaling=0.5, min_q_mvar=-10.0, max_q_mvar=10.0)
    pdp.create_gen(n_pdp, b3, p_mw=20.0, vm_pu=1.0, scaling=0.25, min_q_mvar=-30.0, max_q_mvar=30.0)
    run_and_compare(n_pdp, 3)
