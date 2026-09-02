import pytest
import pypowsybl as pp
import pypowsybl.opf as opf

from pypowsybl.opf.impl.acdc_network_validator import validate_acdc_network


def build_ac_bus(network, suffix):
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

    return voltage_level_id, bus_id


def add_vsc_island_on_ac_bus(
    network,
    suffix,
    voltage_level_id,
    bus_id,
    nominal_v,
    control_mode="V_DC",
    grounded=True,
):
    dc_node1_id = f"dn1{suffix}"
    dc_node2_id = f"dn2{suffix}"

    network.create_dc_nodes(id=dc_node1_id, nominal_v=nominal_v)
    network.create_dc_nodes(id=dc_node2_id, nominal_v=nominal_v)
    if grounded:
        network.create_dc_grounds(id=f"dg{suffix}", r=0.0, dc_node_id=dc_node2_id)

    kwargs = dict(
        id=f"conv{suffix}",
        voltage_level_id=voltage_level_id,
        bus1_id=bus_id,
        dc_node1_id=dc_node1_id,
        dc_node2_id=dc_node2_id,
        voltage_regulator_on=False,
        control_mode=control_mode,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    if control_mode == "V_DC":
        kwargs["target_v_dc"] = nominal_v
    else:
        kwargs["target_p"] = -30.0

    network.create_voltage_source_converters(**kwargs)


def test_validation_rejects_different_nominal_voltages_in_same_dc_component():
    network = pp.network.create_empty()

    voltage_level_id, bus_id = build_ac_bus(network, "A")

    network.create_dc_nodes(id="dn1", nominal_v=400.0)
    network.create_dc_nodes(id="dn2", nominal_v=320.0)
    network.create_dc_grounds(id="dg", r=0.0, dc_node_id="dn2")

    network.create_voltage_source_converters(
        id="conv",
        voltage_level_id=voltage_level_id,
        bus1_id=bus_id,
        dc_node1_id="dn1",
        dc_node2_id="dn2",
        voltage_regulator_on=False,
        control_mode="V_DC",
        target_v_dc=400.0,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    with pytest.raises(ValueError, match="has several nominal voltages"):
        validate_acdc_network(network)


def test_validation_accepts_different_nominal_voltages_in_different_dc_components():
    network = pp.network.create_empty()

    voltage_level_id, bus_id = build_ac_bus(network, "A")

    add_vsc_island_on_ac_bus(
        network,
        "A",
        voltage_level_id,
        bus_id,
        nominal_v=400.0,
        control_mode="V_DC",
    )
    add_vsc_island_on_ac_bus(
        network,
        "B",
        voltage_level_id,
        bus_id,
        nominal_v=320.0,
        control_mode="V_DC",
    )

    validate_acdc_network(network)


def test_validation_no_longer_requires_a_vdc_converter_when_grounded():
    """A grounded DC component with no V_DC converter used to be rejected for the wrong reason (no
    V_DC converter); grounding, not a V_DC converter, is what anchors a component's absolute
    voltage, so this must now be accepted."""
    network = pp.network.create_empty()

    voltage_level_id, bus_id = build_ac_bus(network, "A")

    add_vsc_island_on_ac_bus(
        network,
        "A",
        voltage_level_id,
        bus_id,
        nominal_v=400.0,
        control_mode="P_PCC",
    )

    validate_acdc_network(network)


def test_validation_rejects_an_ungrounded_dc_component_even_with_a_vdc_converter():
    """red->green: a V_DC converter used to be treated as sufficient on its own. target_v_dc is a
    difference between two converter terminals, so it can never anchor a component's absolute
    level - only a connected DcGround can. Fails to raise before the fix; raises after."""
    network = pp.network.create_empty()

    voltage_level_id, bus_id = build_ac_bus(network, "A")

    add_vsc_island_on_ac_bus(
        network,
        "A",
        voltage_level_id,
        bus_id,
        nominal_v=400.0,
        control_mode="V_DC",
        grounded=False,
    )

    with pytest.raises(ValueError, match="no connected ground"):
        validate_acdc_network(network)


def test_validation_accepts_dc_component_with_one_vdc_converter():
    network = pp.network.create_empty()

    voltage_level_id, bus_id = build_ac_bus(network, "A")

    add_vsc_island_on_ac_bus(
        network,
        "A",
        voltage_level_id,
        bus_id,
        nominal_v=400.0,
        control_mode="V_DC",
    )

    validate_acdc_network(network)


def add_vsc_on_existing_dc_nodes(
    network,
    suffix,
    voltage_level_id,
    bus_id,
    dc_node1_id,
    dc_node2_id,
    nominal_v,
    control_mode,
):
    kwargs = dict(
        id=f"conv{suffix}",
        voltage_level_id=voltage_level_id,
        bus1_id=bus_id,
        dc_node1_id=dc_node1_id,
        dc_node2_id=dc_node2_id,
        voltage_regulator_on=False,
        control_mode=control_mode,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    if control_mode == "V_DC":
        kwargs["target_v_dc"] = nominal_v
    else:
        kwargs["target_p"] = -30.0

    network.create_voltage_source_converters(**kwargs)


def create_two_vsc_same_dc_component_network(conv_a_control_mode: str):
    network = pp.network.create_empty()

    voltage_level_id_a, bus_id_a = build_ac_bus(network, "A")
    voltage_level_id_b, bus_id_b = build_ac_bus(network, "B")

    network.create_dc_nodes(id="dn_a", nominal_v=400.0)
    network.create_dc_nodes(id="dn_b", nominal_v=400.0)
    network.create_dc_nodes(id="dn_g", nominal_v=400.0)

    network.create_dc_lines(
        id="dc_line",
        dc_node1_id="dn_a",
        dc_node2_id="dn_b",
        r=1.0,
    )

    network.create_dc_grounds(id="dg", r=0.0, dc_node_id="dn_g")

    add_vsc_on_existing_dc_nodes(
        network,
        "A",
        voltage_level_id_a,
        bus_id_a,
        dc_node1_id="dn_a",
        dc_node2_id="dn_g",
        nominal_v=400.0,
        control_mode=conv_a_control_mode,
    )

    add_vsc_on_existing_dc_nodes(
        network,
        "B",
        voltage_level_id_b,
        bus_id_b,
        dc_node1_id="dn_b",
        dc_node2_id="dn_g",
        nominal_v=400.0,
        control_mode="P_PCC",
    )

    return network
    
def test_validation_accepts_a_grounded_dc_component_regardless_of_converter_control_mode():
    """Both converters on the same, grounded DC component - control mode no longer decides
    validity, grounding does. One V_DC + one P_PCC, and two P_PCC, both accepted."""
    validate_acdc_network(create_two_vsc_same_dc_component_network("V_DC"))
    validate_acdc_network(create_two_vsc_same_dc_component_network("P_PCC"))

import pandas as pd
import pytest

from pypowsybl.opf.impl.acdc_network_validator import check_no_dangling_dc_lines


def test_check_no_dangling_dc_lines_rejects_line_missing_one_side():
    dc_lines = pd.DataFrame(
        {
            "dc_node1_id": ["dn1", "dn3"],
            "dc_node2_id": ["dn2", None],
            "r": [0.1, 0.1],
        },
        index=["valid_line", "dangling_line"],
    )

    with pytest.raises(ValueError, match="DC lines must be connected on both sides"):
        check_no_dangling_dc_lines(dc_lines)


def test_check_no_dangling_dc_lines_accepts_lines_connected_on_both_sides():
    dc_lines = pd.DataFrame(
        {
            "dc_node1_id": ["dn1", "dn3"],
            "dc_node2_id": ["dn2", "dn4"],
            "r": [0.1, 0.2],
        },
        index=["line1", "line2"],
    )

    check_no_dangling_dc_lines(dc_lines)


def test_check_no_dangling_dc_lines_rejects_disconnected_existing_dc_line():
    network = create_two_vsc_same_dc_component_network("V_DC")

    dc_lines = network.get_dc_lines()
    dc_lines.loc["dc_line", "dc_node2_id"] = None

    with pytest.raises(ValueError, match="DC lines must be connected on both sides"):
        check_no_dangling_dc_lines(dc_lines)


# --- validate_acdc_network runs regardless of mode -------------------------------------------------

def test_validate_acdc_network_is_a_no_op_on_a_network_with_no_dc_nodes():
    """trap: validate_acdc_network already returns immediately when the network has no DC nodes at
    all - true before and after this fix. That is what makes calling it on every run() safe: it is
    not the caller's job to decide whether the network needs validating, the function already knows.
    """
    validate_acdc_network(pp.network.create_ieee14())


def test_run_ac_default_mode_rejects_mixed_nominal_voltages():
    """red->green: run_ac's default mode (LOADFLOW) used to skip validate_acdc_network entirely,
    solving a network ACDC mode would have rejected outright. Fails to raise before the fix
    (silently attempts to solve instead); raises after.
    """
    network = pp.network.create_dc_detailed_vsc_asymmetrical_monopole_network()

    with pytest.raises(ValueError, match="has several nominal voltages"):
        opf.run_ac(network)


def test_run_ac_default_mode_rejects_an_ungrounded_dc_component():
    """This assertion (validate-on-network-not-mode: the default mode used to skip validation
    entirely) was originally written against the missing-V_DC-converter check;
    grounding-not-control-mode retired that check, so it is rewritten here against its
    replacement - an ungrounded DC component - to keep testing the same thing: the validator runs
    regardless of mode.
    """
    network = pp.network.create_empty()
    voltage_level_id, bus_id = build_ac_bus(network, "A")
    add_vsc_island_on_ac_bus(network, "A", voltage_level_id, bus_id, nominal_v=400.0,
                              control_mode="V_DC", grounded=False)

    with pytest.raises(ValueError, match="no connected ground"):
        opf.run_ac(network)
