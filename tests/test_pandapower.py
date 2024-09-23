import pandapower as pdp
import pytest

import pypowsybl as pp
import logging

EPS_V = 0.001


@pytest.fixture(autouse=True)
def setup():
    logging.basicConfig()
    logging.getLogger('powsybl').setLevel(logging.DEBUG)


def run_and_compare(pdp_n, expected_bus_count: int):
    pdp.runpp(pdp_n, numba=True, enforce_q_lims=False, distributed_slack=False, trafo_model="pi")
    n = pp.network.convert_from_pandapower(pdp_n)
    assert len(n.get_buses()) == expected_bus_count
    param = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.UNIFORM_VALUES,
                                   transformer_voltage_control_on=False,
                                   use_reactive_limits=False,
                                   shunt_compensator_voltage_control_on=False,
                                   phase_shifter_regulation_on=False,
                                   distributed_slack=False)
    results = pp.loadflow.run_ac(n, param)
    assert pp.loadflow.ComponentStatus.CONVERGED == results[0].status
    pdp_v = list(pdp_n.res_bus['vm_pu'] * pdp_n.bus['vn_kv'])
    buses = n.get_bus_breaker_view_buses()
    v = list(buses['v_mag'])
    for index, (pdp_v_val, v_val) in enumerate(zip(pdp_v, v)):
        print(str(index))
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
