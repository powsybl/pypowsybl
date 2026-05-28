import logging
import pytest
import pypowsybl as pp

from pypowsybl.opf.impl.acdc_network_validator import AcdcNetworkValidator
from pypowsybl.opf.impl.model.network_cache import NetworkCache


def validate_network(network):
    AcdcNetworkValidator(NetworkCache(network)).validate()


def _build_minimal_ac_bus(network, suffix=""):
    voltage_level_id = f"vl{suffix}"
    bus_id = f"b{suffix}"
    generator_id = f"g{suffix}"

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
        id=generator_id,
        voltage_level_id=voltage_level_id,
        bus_id=bus_id,
        target_p=0.0,
        min_p=-1000.0,
        max_p=1000.0,
        target_v=400.0,
        voltage_regulator_on=True,
    )

    return voltage_level_id, bus_id

def test_validation_accepts_vsc_with_same_terminal_nominal_voltage():
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1", nominal_v=400.0)
    n.create_dc_nodes(id="dn2", nominal_v=400.0)

    vl, bus = _build_minimal_ac_bus(n)

    n.create_voltage_source_converters(
        id="conv",
        voltage_level_id=vl,
        bus1_id=bus,
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

    validate_network(n)


def test_validation_rejects_vsc_with_mismatched_terminal_nominal_voltage():
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1", nominal_v=1.0)
    n.create_dc_nodes(id="dn2", nominal_v=500.0)

    vl, bus = _build_minimal_ac_bus(n)

    n.create_voltage_source_converters(
        id="conv",
        voltage_level_id=vl,
        bus1_id=bus,
        dc_node1_id="dn1",
        dc_node2_id="dn2",
        voltage_regulator_on=False,
        control_mode="V_DC",
        target_v_dc=500.0,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    with pytest.raises(ValueError, match="Invalid detailed-DC network for ACDC OPF"):
        validate_network(n)


def test_validation_warns_dc_line_component_with_only_p_pcc_vsc(caplog):
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1", nominal_v=400.0)
    n.create_dc_nodes(id="dn2", nominal_v=400.0)
    n.create_dc_lines(id="dl", dc_node1_id="dn1", dc_node2_id="dn2", r=0.1)

    vl, bus = _build_minimal_ac_bus(n)

    n.create_voltage_source_converters(
        id="conv",
        voltage_level_id=vl,
        bus1_id=bus,
        dc_node1_id="dn1",
        dc_node2_id="dn2",
        voltage_regulator_on=False,
        control_mode="P_PCC",
        target_p=-30.0,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    caplog.set_level(logging.WARNING)

    validate_network(n)

    # assert "has no VSC in V_DC mode attached" in caplog.text
    # assert "DC component nodes:" in caplog.text
    # assert "dn1" in caplog.text
    # assert "dn2" in caplog.text
    # assert "dl" in caplog.text



def test_validation_warns_dc_line_component_without_vsc(caplog):
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1", nominal_v=400.0)
    n.create_dc_nodes(id="dn2", nominal_v=400.0)
    n.create_dc_lines(id="dl", dc_node1_id="dn1", dc_node2_id="dn2", r=0.1)

    # NetworkCache expects at least a minimal AC network.
    # This test still has no VSC attached to the DC-line component.
    _build_minimal_ac_bus(n)

    caplog.set_level(logging.WARNING)

    validate_network(n)

    assert "has no VSC in V_DC mode attached" in caplog.text
    assert "DC component nodes:" in caplog.text
    assert "dn1" in caplog.text
    assert "dn2" in caplog.text
    assert "dl" in caplog.text

def test_validation_accepts_dc_line_component_with_many_vscs_if_one_is_vdc(caplog):
    n = pp.network.create_empty()

    n.create_dc_nodes(id="dn1", nominal_v=400.0)
    n.create_dc_nodes(id="dn2", nominal_v=400.0)
    n.create_dc_nodes(id="dn3", nominal_v=400.0)

    n.create_dc_lines(id="dl12", dc_node1_id="dn1", dc_node2_id="dn2", r=0.1)
    n.create_dc_lines(id="dl23", dc_node1_id="dn2", dc_node2_id="dn3", r=0.1)

    vl1, bus1 = _build_minimal_ac_bus(n, suffix="1")
    vl2, bus2 = _build_minimal_ac_bus(n, suffix="2")

    n.create_voltage_source_converters(
        id="conv_p",
        voltage_level_id=vl1,
        bus1_id=bus1,
        dc_node1_id="dn1",
        dc_node2_id="dn2",
        voltage_regulator_on=False,
        control_mode="P_PCC",
        target_p=-30.0,
        target_q=0.0,
        idle_loss=0.0,
        switching_loss=0.0,
        resistive_loss=0.0,
        dc_connected1=True,
        dc_connected2=True,
    )

    n.create_voltage_source_converters(
        id="conv_v",
        voltage_level_id=vl2,
        bus1_id=bus2,
        dc_node1_id="dn2",
        dc_node2_id="dn3",
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

    caplog.set_level(logging.WARNING)

    validate_network(n)

    assert "has no VSC in V_DC mode attached" not in caplog.text


def create_three_terminal_mtdc3T_validation_network(vdc_converter_id=None):
    """
    Three-terminal Y-shaped detailed-DC validation network.

    VSCs:
        conv1: dc_node1=dn1 grounded/reference, dc_node2=dn2 top
        conv2: dc_node1=dn3 grounded/reference, dc_node2=dn4 top
        conv3: dc_node1=dn5 grounded/reference, dc_node2=dn6 top

    DC grid:
        dn2 -- R=2Ω -- dn7
        dn4 -- R=4Ω -- dn7
        dn6 -- R=6Ω -- dn7

    If vdc_converter_id is None, all converters are P_PCC.
    If vdc_converter_id is "conv1", "conv2", or "conv3", that converter is V_DC.
    """
    n = pp.network.create_empty()

    for node_id in ["dn1", "dn2", "dn3", "dn4", "dn5", "dn6", "dn7"]:
        n.create_dc_nodes(id=node_id, nominal_v=400.0)

    n.create_dc_grounds(id="dc_ground_1", dc_node_id="dn1", r=0.0)
    n.create_dc_grounds(id="dc_ground_3", dc_node_id="dn3", r=0.0)
    n.create_dc_grounds(id="dc_ground_5", dc_node_id="dn5", r=0.0)

    n.create_dc_lines(id="dl27", dc_node1_id="dn2", dc_node2_id="dn7", r=2.0)
    n.create_dc_lines(id="dl47", dc_node1_id="dn4", dc_node2_id="dn7", r=4.0)
    n.create_dc_lines(id="dl67", dc_node1_id="dn6", dc_node2_id="dn7", r=6.0)

    vl1, bus1 = _build_minimal_ac_bus(n, suffix="1")
    vl2, bus2 = _build_minimal_ac_bus(n, suffix="2")
    vl3, bus3 = _build_minimal_ac_bus(n, suffix="3")

    converter_specs = [
        ("conv1", vl1, bus1, "dn1", "dn2", -10.0),
        ("conv2", vl2, bus2, "dn3", "dn4", -20.0),
        ("conv3", vl3, bus3, "dn5", "dn6", -30.0),
    ]

    for conv_id, vl, bus, dc1, dc2, target_p in converter_specs:
        is_vdc = conv_id == vdc_converter_id

        kwargs = dict(
            id=conv_id,
            voltage_level_id=vl,
            bus1_id=bus,
            dc_node1_id=dc1,
            dc_node2_id=dc2,
            voltage_regulator_on=False,
            control_mode="V_DC" if is_vdc else "P_PCC",
            target_q=0.0,
            idle_loss=0.0,
            switching_loss=0.0,
            resistive_loss=0.0,
            dc_connected1=True,
            dc_connected2=True,
        )

        if is_vdc:
            kwargs["target_v_dc"] = 400.0
        else:
            kwargs["target_p"] = target_p

        n.create_voltage_source_converters(**kwargs)

    return n

def test_validation_warns_mtdc3T_when_all_vscs_are_p_pcc(caplog):
    n = create_three_terminal_mtdc3T_validation_network(vdc_converter_id=None)

    caplog.set_level(logging.WARNING)

    validate_network(n)

    assert "has no VSC in V_DC mode attached" in caplog.text
    assert "dn2" in caplog.text
    assert "dn4" in caplog.text
    assert "dn6" in caplog.text
    assert "dn7" in caplog.text
    assert "dl27" in caplog.text
    assert "dl47" in caplog.text
    assert "dl67" in caplog.text

def test_validation_accepts_three_terminal_mtdc3T_with_one_vdc_vsc(caplog):
    n = create_three_terminal_mtdc3T_validation_network(vdc_converter_id="conv1")

    caplog.set_level(logging.WARNING)

    validate_network(n)

    assert "has no VSC in V_DC mode attached" not in caplog.text
