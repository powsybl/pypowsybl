import pytest
import pypowsybl as pp

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
):
    dc_node1_id = f"dn1{suffix}"
    dc_node2_id = f"dn2{suffix}"

    network.create_dc_nodes(id=dc_node1_id, nominal_v=nominal_v)
    network.create_dc_nodes(id=dc_node2_id, nominal_v=nominal_v)
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


def test_validation_rejects_dc_component_without_vdc_converter():
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

    with pytest.raises(ValueError, match="no VSC in V_DC mode"):
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
    
def test_validation_accepts_one_vdc_and_one_pcc_vsc_on_same_dc_component_then_rejects_two_pcc():
    valid_network = create_two_vsc_same_dc_component_network("V_DC")

    validate_acdc_network(valid_network)

    invalid_network = create_two_vsc_same_dc_component_network("P_PCC")

    with pytest.raises(ValueError, match="no VSC in V_DC mode"):
        validate_acdc_network(invalid_network)