import inspect
import math
import platform

import pytest

import pypowsybl as pp
import pypowsybl.opf as opf
from pypowsybl.opf.impl.bounds.slack_bus_angle_bounds import SlackBusAngleBounds
from pypowsybl.opf.impl.bounds.transformer_3w_middle_voltage_bounds import Transformer3wMiddleVoltageBounds
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.dc_voltage_starts import compute_dc_node_voltage_starts
from pypowsybl.opf.impl.model.model import create_model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters, SolverType
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext

if platform.system() == 'Darwin' and platform.machine() == 'x86_64':
    pytest.skip("No version compatible with x86_64 macOS.", allow_module_level=True)

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


# --- bus_id crash on run_ac's default entry point -------------------------------------------------

def test_voltage_source_converters_have_no_bus_id_column():
    """trap: vsc_converter_stations has a bus_id column, voltage_source_converters doesn't."""
    network = pp.network.create_ac_dc_bipolar_network()
    assert 'bus_id' in network.get_vsc_converter_stations().columns
    assert 'bus_id' not in network.get_voltage_source_converters().columns
    assert 'bus1_id' in network.get_voltage_source_converters().columns


def test_run_ac_default_mode_on_detailed_dc_network():
    """red->green: run_ac(network) must not crash on a new-model AcDcConverter."""
    assert pp.opf.run_ac(pp.network.create_ac_dc_bipolar_network())


def test_run_ac_default_mode_with_voltage_regulator_on():
    """red->green: same bug, second bus_id read (inside voltage_regulator_on).
    No other test here reaches that branch, so a guard-only fix would pass everything but this."""
    network = pp.network.create_ac_dc_bipolar_network()
    network.update_voltage_source_converters(id='conv23', target_v_ac=400.0)
    network.update_voltage_source_converters(id='conv23', voltage_regulator_on=True)
    assert pp.opf.run_ac(network)


# --- DC line current -------------------------------------------------------------------------------

def test_dc_line_current_read_back_stays_mirrored():
    """trap: a DC line's i2 is always -i1, both before and after collapse-dc-line-current -- this
    is the fact that makes carrying only one solver variable safe. Ohm's law forces i2 = -i1
    structurally, so the network's own i1/i2 columns stay mirrored regardless of how many solver
    variables produced them."""
    network = pp.network.create_ac_dc_bipolar_network()
    parameters = opf.OptimalPowerFlowParameters(mode=opf.OptimalPowerFlowMode.ACDC)
    assert opf.run_ac(network, parameters)

    dc_lines = network.get_dc_lines()[['i1', 'i2']]
    assert len(dc_lines) > 0
    for i1, i2 in zip(dc_lines['i1'], dc_lines['i2']):
        assert i2 == pytest.approx(-i1, abs=1e-6)


def test_dc_line_carries_one_current_variable_not_two():
    """red->green: a DC line used to carry two solver variables (closed_dc_line_i1_vars,
    closed_dc_line_i2_vars) for the same physical current, constrained i2 = -i1 by two copies of
    Ohm's law. Collapsed to one (closed_dc_line_i_vars); network_cache.update_dc_lines mirrors it
    back into the network's own two columns."""
    network = pp.network.create_ac_dc_bipolar_network()
    cache = NetworkCache(network)
    model = create_model(SolverType.IPOPT, {})
    variable_context = VariableContext.build(cache, model)

    assert len(variable_context.closed_dc_line_i_vars) == len(cache.dc_lines)
    assert not hasattr(variable_context, 'closed_dc_line_i1_vars')
    assert not hasattr(variable_context, 'closed_dc_line_i2_vars')


# --- DC switches ---------------------------------------------------------------------------------
# Each test adds a second dn3p -> dn4p path in parallel with the existing one and of the same total
# resistance, so a closed switch must split the pole current about evenly and an open one must not.
# Parallel rather than carved out of a line because DC lines cannot be removed.
PARALLEL_PATH_R_OHM = 0.2
POLE_CURRENT_A = -126.5905


def create_bipolar_network_with_parallel_switch_path(switch_open, switch_r_ohm=0.0):
    n = pp.network.create_ac_dc_bipolar_network()
    n.create_dc_nodes(id='dnXp', nominal_v=400.0)
    n.create_dc_switches(id='dsXp', dc_node1_id='dn3p', dc_node2_id='dnXp',
                         kind='BREAKER', open=switch_open, r=switch_r_ohm)
    n.create_dc_lines(id='dlX4p', dc_node1_id='dnXp', dc_node2_id='dn4p',
                      r=PARALLEL_PATH_R_OHM - switch_r_ohm)
    return n


def test_closed_dc_switch_conducts_and_shorts_its_two_dc_nodes():
    n = create_bipolar_network_with_parallel_switch_path(switch_open=False)

    assert run_acdc(n)

    voltages = n.get_dc_nodes()['v']
    assert voltages['dn3p'] == pytest.approx(voltages['dnXp'], abs=1e-6)

    currents = n.get_dc_lines()['i1']
    total = currents['dlX4p'] + currents['dl3Gp']
    assert total == pytest.approx(POLE_CURRENT_A, abs=0.01)
    # Not exactly half each: the original path passes dnGp, where the metallic return draws 0.2 A.
    assert currents['dlX4p'] == pytest.approx(total / 2, abs=0.2)
    assert currents['dl3Gp'] == pytest.approx(total / 2, abs=0.2)


def test_resistive_closed_dc_switch_drops_r_times_current():
    switch_r_ohm = 0.05
    n = create_bipolar_network_with_parallel_switch_path(switch_open=False,
                                                         switch_r_ohm=switch_r_ohm)

    assert run_acdc(n)

    voltages = n.get_dc_nodes()['v']
    # The added line is the only other element at dnXp, so it carries the switch current exactly.
    switch_current_a = n.get_dc_lines()['i1']['dlX4p']
    assert voltages['dn3p'] - voltages['dnXp'] == pytest.approx(
        switch_r_ohm * switch_current_a / 1000.0, abs=1e-6)


def test_open_dc_switch_conducts_nothing():
    reference = pp.network.create_ac_dc_bipolar_network()
    assert run_acdc(reference)
    reference_currents = reference.get_dc_lines()['i1']

    n = create_bipolar_network_with_parallel_switch_path(switch_open=True)
    assert run_acdc(n)

    currents = n.get_dc_lines()['i1']
    assert currents['dlX4p'] == pytest.approx(0.0, abs=1e-3)
    for dc_line_id in reference_currents.index:
        assert currents[dc_line_id] == pytest.approx(reference_currents[dc_line_id], abs=1e-3)


# --- DC node voltage start values ----------------------------------------------------------------

def test_dc_voltage_starts_place_every_node_from_the_declared_voltages():
    """A converter's target_v_dc anchors the walk; a ground places what the walk cannot reach.

    create_ac_dc_monopolar_network exercises both: conv45 declares 1.0 pu, and dn3n is related to
    nothing because the other converter is in P_PCC mode and declares no target.
    """
    starts = compute_dc_node_voltage_starts(NetworkCache(pp.network.create_ac_dc_monopolar_network()))

    assert starts == pytest.approx({'dn4p': 0.5, 'dn4n': -0.5, 'dn3p': 0.5, 'dn3n': 0.0})


def test_transformer_3w_middle_start_lands_on_voltage_not_angle():
    """Transformer3wMiddleVoltageBounds used to call set_variable_start on t3_middle_ph_vars
    (the angle) instead of t3_middle_v_vars (the voltage magnitude) two lines above, where the
    bounds are set. That left the magnitude variable to default to a start of 0.0 and forced the
    angle to 1.0 rad (~57 degrees) instead of the 0.0 every other free angle defaults to.

    No public getter reads a start value before solving, so this reaches the underlying
    pyoptinterface model directly (get_variable_start), the same way set_variable_start does.
    """
    network = pp.network.create_micro_grid_be_network()
    cache = NetworkCache(network)
    model_parameters = ModelParameters(0.1, False, Bounds(0.8, 1.1), SolverType.IPOPT, {})
    model = create_model(SolverType.IPOPT, {})
    variable_context = VariableContext.build(cache, model)

    Transformer3wMiddleVoltageBounds().add(model_parameters, cache, variable_context, model)

    t3_index = variable_context.t3_num_2_index[0]
    v_start = model._model.get_variable_start(variable_context.t3_middle_v_vars[t3_index])
    ph_start = model._model.get_variable_start(variable_context.t3_middle_ph_vars[t3_index])

    assert v_start == 1.0
    assert ph_start == 0.0


def test_run_ac_has_no_shared_mutable_default_parameters():
    """run_ac() used to default to a single OptimalPowerFlowParameters() instance built once
    at import time. Every OptimalPowerFlowParameters.with_*() setter mutates self in place and
    returns it (no copy anywhere), so anyone who got hold of that default and configured it would
    have silently reconfigured every later caller relying on the default in the same process.
    """
    assert inspect.signature(opf.run_ac).parameters['parameters'].default is None


def test_network_cache_builds_on_a_pure_dc_network_with_no_ac_buses():
    """NetworkCache used to raise on any network with zero AC buses: an empty get_buses()
    carries voltage_level_id as float64 while get_voltage_levels() carries it as object,
    and the merge between the two rejected the mismatch.
    """
    network = pp.network.create_dc_detailed_dc_switch_2_nodes()
    cache = NetworkCache(network)
    assert len(cache.buses) == 0


def test_get_voltage_bounds_uses_declared_limits_when_present():
    """red->green: a bus whose voltage level declares real limits gets those limits, not the
    generic default. Bounds.get_voltage_bounds used to return default_voltage_bounds
    unconditionally, discarding whatever the network declared - this failed before the fix
    (returned [0.8, 1.1]) and passes after.
    """
    default = Bounds(0.8, 1.1)
    declared = Bounds.get_voltage_bounds(0.95, 1.05, default)
    assert declared.min_value == 0.95
    assert declared.max_value == 1.05


def test_get_voltage_bounds_falls_back_per_side_independently():
    """red->green: a voltage level declaring only one side keeps that side; the other still falls
    back to the default. A voltage level may legitimately declare a floor with no ceiling (or the
    reverse) - an all-or-nothing fallback would discard the declared side too.
    """
    default = Bounds(0.8, 1.1)
    only_low_declared = Bounds.get_voltage_bounds(0.95, float('nan'), default)
    assert only_low_declared.min_value == 0.95
    assert only_low_declared.max_value == 1.1

    only_high_declared = Bounds.get_voltage_bounds(None, 1.05, default)
    assert only_high_declared.min_value == 0.8
    assert only_high_declared.max_value == 1.05


def test_voltage_level_undeclared_limit_is_nan_not_none():
    """trap: an undeclared voltage-level limit surfaces as float NaN, not None, once read through
    the per-unit dataframe (create_ieee14 declares no voltage-level limits at all). True before and
    after declared-ac-voltage-limits - it is why get_voltage_bounds's fallback must check for NaN,
    not just `is None`.
    """
    network = pp.network.create_ieee14()
    network.per_unit = True
    voltage_levels = network.get_voltage_levels(attributes=['low_voltage_limit', 'high_voltage_limit'])
    value = voltage_levels['low_voltage_limit'].iloc[0]
    assert value is not None
    assert value != value  # NaN is the only value that does not equal itself


def test_dc_current_bound_does_not_cut_off_an_ordinary_operating_point():
    """2.0 pu was only 500 A on this network's base, below what an ordinary HVDC cable carries."""
    n = pp.network.create_ac_dc_bipolar_network()
    n.update_voltage_source_converters(id='conv23', target_p=-250.0)

    assert run_acdc(n)

    base_current_a = 100e3 / 400.0
    assert n.get_dc_lines()['i1'].abs().max() > 2.0 * base_current_a


# --- slack bus angle reference (one per synchronous component) --------------------------------------

def _add_slack_bus_angle_bounds(network):
    """Build just enough of the model to inspect SlackBusAngleBounds's own bounds, no solve."""
    cache = NetworkCache(network)
    model = create_model(SolverType.IPOPT, {})
    variable_context = VariableContext.build(cache, model)
    model_parameters = ModelParameters(0.1, False, Bounds(0.8, 1.1), SolverType.IPOPT, {})
    SlackBusAngleBounds().add(model_parameters, cache, variable_context, model)
    return cache, model, variable_context


def test_ac_islands_joined_only_by_dc_are_different_synchronous_components():
    """trap: the data SlackBusAngleBounds needs was already sitting in NetworkCache.buses, unused -
    every AC bus carries its own synchronous_component number, and two AC islands joined only by a
    DC link (no shared AC path) get two different ones. True before and after the fix; it is what
    makes per-component grouping possible at all."""
    network = create_back_to_back_dc_network()
    cache = NetworkCache(network)
    assert cache.buses['synchronous_component'].nunique() == 2


def test_slack_bus_angle_bounds_pins_one_bus_per_synchronous_component():
    """red->green: SlackBusAngleBounds used to pin exactly one bus, globally (the first declared
    slack terminal, or else the first bus of the whole network) - every synchronous component after
    the first got no angle reference at all. Fails before the fix (only one component ends up with
    a (0.0, 0.0)-bounded bus); passes after, one per component."""
    network = create_back_to_back_dc_network()
    cache, model, variable_context = _add_slack_bus_angle_bounds(network)

    for component, buses_in_component in cache.buses.groupby('synchronous_component'):
        pinned = [
            (model._model.get_variable_lb(variable_context.ph_vars[cache.buses.index.get_loc(bus_id)]),
             model._model.get_variable_ub(variable_context.ph_vars[cache.buses.index.get_loc(bus_id)]))
            == (0.0, 0.0)
            for bus_id in buses_in_component.index
        ]
        assert any(pinned), f"synchronous component {component} has no angle reference"


def test_slack_bus_angle_bounds_matches_prior_behavior_on_a_single_component_network():
    """Regression guard: on a network with one synchronous component and no declared slack terminal,
    the new per-component grouping must still pin exactly the bus the old global rule would have
    picked - the first bus of the network, in table order."""
    network = pp.network.create_ac_dc_bipolar_network()
    cache, model, variable_context = _add_slack_bus_angle_bounds(network)

    assert cache.buses['synchronous_component'].nunique() == 1
    old_rule_bus_id = cache.buses.index[0]
    pinned_bus_nums = [
        i for i, bus_id in enumerate(cache.buses.index)
        if (model._model.get_variable_lb(variable_context.ph_vars[i]),
            model._model.get_variable_ub(variable_context.ph_vars[i])) == (0.0, 0.0)
    ]
    assert pinned_bus_nums == [cache.buses.index.get_loc(old_rule_bus_id)]


def test_slack_bus_angle_bounds_does_not_crash_on_zero_ac_buses():
    """red->green, found while fixing angle-reference-per-component: SlackBusAngleBounds's old
    fallback, network_cache.buses.iloc[0].name, raised IndexError on a network with zero AC buses -
    reachable under LOADFLOW/REDISPATCHING mode, which do not gate on validate_acdc_network the way
    ACDC mode does. groupby over an empty frame simply iterates zero times instead."""
    network = pp.network.create_dc_detailed_dc_switch_2_nodes()
    cache, model, variable_context = _add_slack_bus_angle_bounds(network)
    assert len(cache.buses) == 0


def _build_two_converter_dc_network(convA_mode="V_DC", convB_mode="V_DC"):
    n = pp.network.create_empty()
    n.create_substations(id='sA')
    n.create_substations(id='sB')
    n.create_voltage_levels(id='vlA', substation_id='sA', topology_kind='BUS_BREAKER', nominal_v=400.0)
    n.create_voltage_levels(id='vlB', substation_id='sB', topology_kind='BUS_BREAKER', nominal_v=400.0)
    n.create_buses(id='bA', voltage_level_id='vlA')
    n.create_buses(id='bB', voltage_level_id='vlB')
    n.create_generators(id='gA', voltage_level_id='vlA', bus_id='bA', target_p=0.0, min_p=-500, max_p=500,
                         target_v=400.0, voltage_regulator_on=True)
    n.create_generators(id='gB', voltage_level_id='vlB', bus_id='bB', target_p=0.0, min_p=-500, max_p=500,
                         target_v=400.0, voltage_regulator_on=True)
    for node in ['dnAp', 'dnAn', 'dnBp', 'dnBn']:
        n.create_dc_nodes(id=node, nominal_v=400.0)
    n.create_dc_lines(id='dlP', dc_node1_id='dnAp', dc_node2_id='dnBp', r=0.5)
    n.create_dc_lines(id='dlN', dc_node1_id='dnAn', dc_node2_id='dnBn', r=0.5)
    n.create_dc_grounds(id='dg', r=0.0, dc_node_id='dnAn')
    for suffix, mode in [('A', convA_mode), ('B', convB_mode)]:
        kwargs = dict(id=f'conv{suffix}', voltage_level_id=f'vl{suffix}', bus1_id=f'b{suffix}',
                      dc_node1_id=f'dn{suffix}p', dc_node2_id=f'dn{suffix}n', voltage_regulator_on=False,
                      control_mode=mode, target_q=0.0, idle_loss=0.0, switching_loss=0.0, resistive_loss=0.0,
                      dc_connected1=True, dc_connected2=True)
        if mode == 'V_DC':
            kwargs['target_v_dc'] = 400.0
        else:
            kwargs['target_p'] = 30.0
        n.create_voltage_source_converters(**kwargs)
    return n


def test_disconnected_dc_line_is_excluded_from_the_solve():
    """dnBp's only path to the rest of the network is dlP; excluding it forces converter B's
    current to 0 there, which its fixed P_PCC target of 30 MW cannot satisfy - infeasible, not a
    silently unchanged answer."""
    network = _build_two_converter_dc_network(convA_mode="V_DC", convB_mode="P_PCC")
    assert opf.run_ac(network)

    network.update_dc_lines(id='dlP', connected1=False)
    assert not opf.run_ac(network)


def test_disconnected_dc_ground_no_longer_pins_voltage():
    """A disconnected DcGround no longer anchors its node's absolute voltage to 0. The validator
    already rejects a component left with no connected ground at all,
    so this patches it out to isolate the constraint-level behaviour it would otherwise mask."""
    import pypowsybl.opf.impl.opf as opf_impl
    network = _build_two_converter_dc_network(convA_mode="V_DC", convB_mode="P_PCC")
    opf.run_ac(network)
    assert network.get_dc_nodes().loc['dnAn', 'v'] == pytest.approx(0.0, abs=ATOL_KV)

    network.update_dc_grounds(id='dg', connected=False)
    original_validate = opf_impl.validate_acdc_network
    opf_impl.validate_acdc_network = lambda n: None
    try:
        assert opf.run_ac(network)
    finally:
        opf_impl.validate_acdc_network = original_validate
    assert network.get_dc_nodes().loc['dnAn', 'v'] != pytest.approx(0.0, abs=ATOL_KV)


def test_acdc_mode_loss_objective_handles_a_disconnected_dc_line():
    """The DC-losses objective must skip an open line's current variable, not index it by raw
    position among all DC lines."""
    network = _build_two_converter_dc_network()
    network.update_dc_lines(id='dlP', connected1=False)
    assert opf.run_ac(network, opf.OptimalPowerFlowParameters(mode=opf.OptimalPowerFlowMode.ACDC))
