import pandapower as pdp
import pypowsybl as pp


def test_pandapower_case14():
    pdp_n = pdp.networks.case14()
    pdp.runpp(pdp_n, numba=False)
    n = pp.network.convert_from_pandapower(pdp_n)
    assert len(n.get_buses()) == 14
    results = pp.loadflow.run_ac(n)
    assert pp.loadflow.ComponentStatus.CONVERGED == results[0].status
    pdp_v = list(pdp_n.res_bus['vm_pu'])
    n.per_unit = True
    buses = n.get_buses()
    v = list(buses['v_mag'])
    assert pdp_v == v
