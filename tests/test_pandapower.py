import pandapower as pdp
from pytest import approx

import pypowsybl as pp

ABS = 0.001


def run_and_compare(pdp_n, slack_id: str, expected_bus_count: int):
    pdp.runpp(pdp_n, numba=False, enforce_q_lims=False, distributed_slack=False)
    n = pp.network.convert_from_pandapower(pdp_n)
    assert len(n.get_buses()) == expected_bus_count
    param = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.UNIFORM_VALUES,
                                   transformer_voltage_control_on=False,
                                   use_reactive_limits=False,
                                   shunt_compensator_voltage_control_on=False,
                                   phase_shifter_regulation_on=False,
                                   distributed_slack=False,
                                   provider_parameters={"slackBusSelectionMode": "NAME",
                                                        "slackBusesIds": slack_id})
    results = pp.loadflow.run_ac(n, param)
    assert pp.loadflow.ComponentStatus.CONVERGED == results[0].status
    pdp_v = list(pdp_n.res_bus['vm_pu'])
    n.per_unit = True
    buses = n.get_buses()
    v = list(buses['v_mag'])
    assert pdp_v == approx(v, abs=ABS)


def test_pandapower_case5():
    run_and_compare(pdp.networks.case5(), 'vl_3_0', 5)


def test_pandapower_case14():
    run_and_compare(pdp.networks.case14(), 'vl_3_0', 14)
