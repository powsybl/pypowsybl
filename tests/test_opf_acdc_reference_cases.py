import math
import pytest
import pypowsybl as pp
import pypowsybl.opf as opf

V_DC_REF_KV = 400.0
P_REF_B_MW = -30.0
P_LOAD_B_MW = 50.0
Q_LOAD_B_MVAR = 10.0
R_DC_OHM = 0.1

ATOL_KV = 1e-2
ATOL_MW = 1e-3


def run_acdc(network):
    """Run an AC/DC optimal power flow on the given network.

    Args:
        network: A PyPowsybl network containing AC and DC elements.

    Returns:
        bool: True if the optimal power flow converged, otherwise False.
    """
    parameters = opf.OptimalPowerFlowParameters(
        mode=opf.OptimalPowerFlowMode.ACDC,
        solver_type=opf.SolverType.IPOPT,
    )
    return opf.run_ac(network, parameters)


def build_ac_island(network, suffix, with_load=False):
    """Create a simple AC island with one generator and optional load.

    Args:
        network: The PyPowsybl network object to modify.
        suffix: Suffix used to generate unique IDs for voltage level, bus, and elements.
        with_load (bool, optional): Whether to add a load to the AC bus. Defaults to False.

    Returns:
        tuple[str, str]: Voltage level ID and bus ID created for the AC island.
    """
    voltage_level_id = f"vl{suffix}"
    bus_id = f"b{suffix}"

    network.create_voltage_levels(
        id=voltage_level_id,
        topology_kind="BUS_BREAKER",
        nominal_v=400.0,
    )
    network.create_buses(
        id=bus_id,
        voltage_level_id=voltage_level_id,
    )
    network.create_generators(
        id=f"g{suffix}",
        voltage_level_id=voltage_level_id,
        bus_id=bus_id,
        target_p=0.0,
        min_p=-500.0,
        max_p=500.0,
        target_v=400.0,
        voltage_regulator_on=True,
    )

    if with_load:
        network.create_loads(
            id=f"ld{suffix}",
            voltage_level_id=voltage_level_id,
            bus_id=bus_id,
            p0=P_LOAD_B_MW,
            q0=Q_LOAD_B_MVAR,
        )

    return voltage_level_id, bus_id


def create_back_to_back_dc_network():
    """Construct a test network with VSCs connected back-to-back without a DC line on the DC side, and to two AC islands on the AC side.

    Returns:
        network: A PyPowsybl network containing two voltage source converters connected by DC nodes.
    """
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1", nominal_v=400.0)
    n.create_dc_nodes(id="dn2", nominal_v=400.0)
    n.create_dc_grounds(id="dg", r=0.0, dc_node_id="dn2")

    vl_a, bus_a = build_ac_island(n, "A", with_load=False)
    vl_b, bus_b = build_ac_island(n, "B", with_load=True)

    n.create_voltage_source_converters(
        id="convA",
        voltage_level_id=vl_a,
        bus1_id=bus_a,
        dc_node1_id="dn1",
        dc_node2_id="dn2",
        voltage_regulator_on=False,
        control_mode="V_DC",
        target_v_dc=V_DC_REF_KV,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )
    n.create_voltage_source_converters(
        id="convB",
        voltage_level_id=vl_b,
        bus1_id=bus_b,
        dc_node1_id="dn1",
        dc_node2_id="dn2",
        voltage_regulator_on=False,
        control_mode="P_PCC",
        target_p=P_REF_B_MW,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    return n


def create_asymmetric_dc_line_network():
    """Construct a test network with VSCs connected over a DC line on the DC side, and to two AC islands on the AC side.

    Returns:
        network: A PyPowsybl network with two DC node pairs, a DC line, and two AC islands.
    """
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1A", nominal_v=400.0)
    n.create_dc_nodes(id="dn2A", nominal_v=400.0)
    n.create_dc_nodes(id="dn1B", nominal_v=400.0)
    n.create_dc_nodes(id="dn2B", nominal_v=400.0)

    n.create_dc_grounds(id="dgA", r=0.0, dc_node_id="dn2A")
    n.create_dc_grounds(id="dgB", r=0.0, dc_node_id="dn2B")
    n.create_dc_lines(id="dl_AB", dc_node1_id="dn1A", dc_node2_id="dn1B", r=R_DC_OHM)

    vl_a, bus_a = build_ac_island(n, "A", with_load=False)
    vl_b, bus_b = build_ac_island(n, "B", with_load=True)

    n.create_voltage_source_converters(
        id="convA",
        voltage_level_id=vl_a,
        bus1_id=bus_a,
        dc_node1_id="dn1A",
        dc_node2_id="dn2A",
        voltage_regulator_on=False,
        control_mode="V_DC",
        target_v_dc=V_DC_REF_KV,
        target_q=0.0,
        idle_loss=0.5,
        switching_loss=0.1,
        resistive_loss=0.2,
        dc_connected1=True,
        dc_connected2=True,
    )
    n.create_voltage_source_converters(
        id="convB",
        voltage_level_id=vl_b,
        bus1_id=bus_b,
        dc_node1_id="dn1B",
        dc_node2_id="dn2B",
        voltage_regulator_on=False,
        control_mode="P_PCC",
        target_p=P_REF_B_MW,
        target_q=0.0,
        idle_loss=0.5,
        switching_loss=0.1,
        resistive_loss=0.2,
        dc_connected1=True,
        dc_connected2=True,
    )
    

    return n



def test_back_to_back_dc_analytical():
    """Verify analytical results for a back-to-back DC network.

    The test checks DC node voltages, converter AC power, and generator targets against expected values.
    """
    n = create_back_to_back_dc_network()

    assert run_acdc(n), "OPF did not converge on back-to-back DC network"

    dc_nodes = n.get_dc_nodes()
    vscs = n.get_voltage_source_converters()
    gens = n.get_generators()

    v1 = float(dc_nodes.loc["dn1", "v"])
    v2 = float(dc_nodes.loc["dn2", "v"])

    p_ac_a = float(vscs.loc["convA", "p_ac"])
    p_ac_b = float(vscs.loc["convB", "p_ac"])

    g_a_target_p = float(gens.loc["gA", "target_p"])
    g_b_target_p = float(gens.loc["gB", "target_p"])
    g_a_terminal_p = float(gens.loc["gA", "p"])
    g_b_terminal_p = float(gens.loc["gB", "p"])

    expected_gen_a = abs(P_REF_B_MW)
    expected_gen_b = P_LOAD_B_MW + P_REF_B_MW

    assert abs((v1 - v2) - V_DC_REF_KV) < ATOL_KV
    assert abs(v2) < ATOL_KV

    assert abs(p_ac_b - P_REF_B_MW) < ATOL_MW
    assert p_ac_b < 0.0

    assert abs(p_ac_a - expected_gen_a) < ATOL_MW
    assert p_ac_a > 0.0

    assert abs(g_a_target_p - expected_gen_a) < ATOL_MW
    assert abs(g_a_terminal_p + expected_gen_a) < ATOL_MW

    assert abs(g_b_target_p - expected_gen_b) < ATOL_MW
    assert abs(g_b_terminal_p + expected_gen_b) < ATOL_MW


def test_asymmetric_dc_line_analytical_closed_form_full_test():
    n = create_asymmetric_dc_line_network()

    assert run_acdc(n), "OPF did not converge on asymmetric DC line network"

    dc_nodes = n.get_dc_nodes()
    vscs = n.get_voltage_source_converters()
    gens = n.get_generators()

    v1a = float(dc_nodes.loc["dn1A", "v"])
    v2a = float(dc_nodes.loc["dn2A", "v"])
    v1b = float(dc_nodes.loc["dn1B", "v"])
    v2b = float(dc_nodes.loc["dn2B", "v"])

    p_ac_a = float(vscs.loc["convA", "p_ac"])
    p_ac_b = float(vscs.loc["convB", "p_ac"])

    g_a_target_p = float(gens.loc["gA", "target_p"])
    g_b_target_p = float(gens.loc["gB", "target_p"])
    g_a_terminal_p = float(gens.loc["gA", "p"])
    g_b_terminal_p = float(gens.loc["gB", "p"])

    idle_loss = 0.5
    switching_loss = 0.1
    resistive_loss = 0.2

    # Current is solved in kA.
    #
    # convB equation:
    # P_ac_B + P_dc_B = loss_B
    #
    # with:
    # P_ac_B = -30 MW
    # P_dc_B = V_B * i_kA
    # V_B = 400 - R_line * i_kA
    # loss_B = idle + switching * (1000 * i_kA) + resistive * i_kA²
    #
    # This gives:
    # (R_line + resistive) * i² + (1000 * switching - V_DC_REF) * i + (idle + abs(P_REF_B)) = 0
    a = R_DC_OHM + resistive_loss
    b = 1000.0 * switching_loss - V_DC_REF_KV
    c = idle_loss + abs(P_REF_B_MW)

    discriminant = b * b - 4.0 * a * c
    expected_i_ka = (-b - math.sqrt(discriminant)) / (2.0 * a)

    expected_v1b = V_DC_REF_KV - R_DC_OHM * expected_i_ka

    expected_converter_loss = (
        idle_loss
        + switching_loss * 1000.0 * expected_i_ka
        + resistive_loss * expected_i_ka * expected_i_ka
    )

    expected_p_dc_a = V_DC_REF_KV * expected_i_ka
    expected_p_ac_a = expected_p_dc_a + expected_converter_loss

    expected_gen_a = expected_p_ac_a
    expected_gen_b = P_LOAD_B_MW + P_REF_B_MW

    assert abs((v1a - v2a) - V_DC_REF_KV) < ATOL_KV
    assert abs(v2a) < ATOL_KV
    assert abs(v2b) < ATOL_KV

    assert v1a > v1b
    assert abs(v1b - expected_v1b) < ATOL_KV

    assert abs(p_ac_b - P_REF_B_MW) < ATOL_MW
    assert p_ac_b < 0.0

    assert abs(g_b_target_p - expected_gen_b) < ATOL_MW
    assert abs(g_b_terminal_p + expected_gen_b) < ATOL_MW

    assert abs(g_a_target_p - expected_gen_a) < ATOL_MW
    assert abs(g_a_terminal_p + expected_gen_a) < ATOL_MW

    assert abs(p_ac_a - expected_p_ac_a) < ATOL_MW

def test_official_asymmetrical_monopole_run_ac_is_rejected_for_mixed_nominal_voltages():
    """Ensure mixed nominal DC voltages are rejected for ACDC optimal power flow.

    The test asserts that run_ac raises a ValueError when DC nodes in the network have differing nominal voltages.
    """
    n = pp.network.create_dc_detailed_vsc_asymmetrical_monopole_network()
    params = opf.OptimalPowerFlowParameters(mode=opf.OptimalPowerFlowMode.ACDC)

    with pytest.raises(ValueError, match="has several nominal voltages"):
        opf.run_ac(n, params)