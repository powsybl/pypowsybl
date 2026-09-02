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


def test_validation_rejects_an_ungrounded_dc_component_even_with_a_vdc_converter():
    """target_v_dc constrains a voltage difference between two converter terminals, not an
    absolute level, so a V_DC converter alone cannot ground a DC component."""
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
    """Grounding does not depend on converter control mode."""
    validate_acdc_network(create_two_vsc_same_dc_component_network("V_DC"))


def test_validation_rejects_a_dc_component_with_no_v_dc_converter():
    """Two P_PCC converters on the same grounded component leave nothing to absorb line losses
    that cannot be known before solving."""
    with pytest.raises(ValueError, match="none in V_DC mode"):
        validate_acdc_network(create_two_vsc_same_dc_component_network("P_PCC"))


def test_validation_rejects_a_dc_component_whose_only_converter_is_in_droop_mode():
    """P_PCC_DROOP is not implemented by this OPF yet - it pins neither power nor voltage - so it must
    not count as a valid power-balancing converter for now."""
    with pytest.raises(ValueError, match="none in V_DC mode"):
        validate_acdc_network(create_two_vsc_same_dc_component_network("P_PCC_DROOP"))


def test_validation_accepts_an_isolated_dc_component_with_no_converter_at_all():
    """A DC component with no converter has no power to balance, so it is exempt from this
    check."""
    network = create_two_vsc_same_dc_component_network("V_DC")
    network.create_dc_nodes(id="dn_orphan1", nominal_v=400.0)
    network.create_dc_nodes(id="dn_orphan2", nominal_v=400.0)
    network.create_dc_lines(id="dl_orphan", dc_node1_id="dn_orphan1", dc_node2_id="dn_orphan2", r=2.0)
    network.create_dc_grounds(id="dg_orphan", r=0.0, dc_node_id="dn_orphan2")

    validate_acdc_network(network)

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
    """validate_acdc_network returns immediately on a network with no DC nodes, so it is always
    safe to call regardless of mode."""
    validate_acdc_network(pp.network.create_ieee14())


def test_run_ac_default_mode_rejects_mixed_nominal_voltages():
    """run_ac's default mode (LOADFLOW) must validate the network, not only ACDC mode."""
    network = pp.network.create_dc_detailed_vsc_asymmetrical_monopole_network()

    with pytest.raises(ValueError, match="has several nominal voltages"):
        opf.run_ac(network)


def test_run_ac_default_mode_rejects_an_ungrounded_dc_component():
    """run_ac's default mode (LOADFLOW) must validate the network, not only ACDC mode."""
    network = pp.network.create_empty()
    voltage_level_id, bus_id = build_ac_bus(network, "A")
    add_vsc_island_on_ac_bus(network, "A", voltage_level_id, bus_id, nominal_v=400.0,
                              control_mode="V_DC", grounded=False)

    with pytest.raises(ValueError, match="no connected ground"):
        opf.run_ac(network)
