import pandapower as pdp
import pytest
from pytest import approx

import pypowsybl as pp
import logging

ABS = 0.001


@pytest.fixture(autouse=True)
def setup():
    logging.basicConfig()
    logging.getLogger('powsybl').setLevel(logging.DEBUG)


def run_and_compare(pdp_n, expected_bus_count: int):
    pdp.runpp(pdp_n, numba=True, enforce_q_lims=False, distributed_slack=False)
    n = pp.network.convert_from_pandapower(pdp_n)
    n.per_unit = True
    assert len(n.get_buses()) == expected_bus_count
    param = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.UNIFORM_VALUES,
                                   transformer_voltage_control_on=False,
                                   use_reactive_limits=False,
                                   shunt_compensator_voltage_control_on=False,
                                   phase_shifter_regulation_on=False,
                                   distributed_slack=False)
    results = pp.loadflow.run_ac(n, param)
    assert pp.loadflow.ComponentStatus.CONVERGED == results[0].status
    pdp_v = list(pdp_n.res_bus['vm_pu'])
    buses = n.get_buses()
    v = list(buses['v_mag'])
    print()
    print(pdp_v)
    print(v)
    assert pdp_v == approx(v, abs=ABS)


def test_pandapower_case5():
    run_and_compare(pdp.networks.case5(), 5)

def test_pandapower_case4gs():
    run_and_compare(pdp.networks.case4gs(),  4)

def test_pandapower_case6ww():
    run_and_compare(pdp.networks.case6ww(), 6)

def test_pandapower_case9():
    run_and_compare(pdp.networks.case9(), 9)

def test_pandapower_case11_iwamoto():
    run_and_compare(pdp.networks.case11_iwamoto(), 11)

def test_pandapower_case14():
    run_and_compare(pdp.networks.case14(), 14)

def test_pandapower_case30():
    run_and_compare(pdp.networks.case30(), 30)

def test_pandapower_case_ieee30():
    run_and_compare(pdp.networks.case_ieee30(), 30)
