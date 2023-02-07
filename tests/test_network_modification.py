import pytest

import pypowsybl as pp
import pandas as pd
import pathlib

TEST_DIR = pathlib.Path(__file__).parent


def test_voltage_level_topology_creation():
    network = pp.network.create_four_substations_node_breaker_network()
    network.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='NODE_BREAKER', nominal_v=225)
    df = pd.DataFrame.from_records(index="id", data=[
        {'id': 'VL1', 'busbar_count': 3, 'switch_kinds': 'BREAKER, DISCONNECTOR'}
    ])
    pp.network.create_voltage_level_topology(network, df)
    busbar_sections = network.get_busbar_sections()
    assert busbar_sections[busbar_sections['voltage_level_id'] == 'VL1'].shape[0] == 9
    switches = network.get_node_breaker_topology('VL1').switches
    assert switches[switches['kind'] == 'DISCONNECTOR'].shape[0] == 9
    assert switches[switches['kind'] == 'BREAKER'].shape[0] == 3


def test_voltage_level_topology_creation_with_no_switch_kind():
    network = pp.network.create_four_substations_node_breaker_network()
    network.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='NODE_BREAKER', nominal_v=225)
    pp.network.create_voltage_level_topology(network=network, switch_kinds='', raise_exception=True, id='VL1',
                                             busbar_count=1)
    busbar_sections = network.get_busbar_sections()
    assert 'VL1_1_1' in busbar_sections.index
    switches = network.get_node_breaker_topology('VL1').switches
    assert switches.empty


def test_voltage_level_topology_creation_from_kwargs():
    network = pp.network.create_four_substations_node_breaker_network()
    network.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='NODE_BREAKER', nominal_v=225)
    switch = 'BREAKER, DISCONNECTOR'
    pp.network.create_voltage_level_topology(network=network, switch_kinds=switch, raise_exception=True, id='VL1',
                                             busbar_count=1, )
    busbar_sections = network.get_busbar_sections()
    assert busbar_sections[busbar_sections['voltage_level_id'] == 'VL1'].shape[0] == 3
    switches = network.get_node_breaker_topology('VL1').switches
    assert switches[switches['kind'] == 'DISCONNECTOR'].shape[0] == 3
    assert switches[switches['kind'] == 'BREAKER'].shape[0] == 1


def test_voltage_level_topology_creation_with_switch_kind_as_list():
    network = pp.network.create_four_substations_node_breaker_network()
    network.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='NODE_BREAKER', nominal_v=225)
    df = pd.DataFrame.from_records(index="id", data=[
        {'id': 'VL1', 'busbar_count': 3, 'switch_kinds': ['BREAKER', 'DISCONNECTOR']}
    ])
    pp.network.create_voltage_level_topology(network, df)
    busbar_sections = network.get_busbar_sections()
    assert busbar_sections[busbar_sections['voltage_level_id'] == 'VL1'].shape[0] == 9
    switches = network.get_node_breaker_topology('VL1').switches
    assert switches[switches['kind'] == 'DISCONNECTOR'].shape[0] == 9
    assert switches[switches['kind'] == 'BREAKER'].shape[0] == 3


def test_multiple_voltage_levels_creation():
    network = pp.network.create_four_substations_node_breaker_network()
    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 400},
        {'substation_id': 'S1', 'id': 'VL2', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 225},
    ])
    network.create_voltage_levels(voltage_levels)
    df = pd.DataFrame.from_records(index="id", data=[
        {'id': 'VL1', 'busbar_count': 3, 'switch_kinds': ['BREAKER', 'DISCONNECTOR']},
        {'id': 'VL2', 'busbar_count': 2, 'switch_kinds': ['BREAKER']}
    ])
    pp.network.create_voltage_level_topology(network, df)
    busbar_sections = network.get_busbar_sections()
    assert busbar_sections[busbar_sections['voltage_level_id'] == 'VL1'].shape[0] == 9
    switches = network.get_node_breaker_topology('VL1').switches
    assert switches[switches['kind'] == 'DISCONNECTOR'].shape[0] == 9
    assert switches[switches['kind'] == 'BREAKER'].shape[0] == 3

    assert busbar_sections[busbar_sections['voltage_level_id'] == 'VL2'].shape[0] == 4
    switches = network.get_node_breaker_topology('VL2').switches
    assert switches[switches['kind'] == 'BREAKER'].shape[0] == 2
    assert switches[switches['kind'] == 'DISCONNECTOR'].shape[0] == 4


def test_no_switch_kind():
    network = pp.network.create_four_substations_node_breaker_network()
    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 400}
    ])
    network.create_voltage_levels(voltage_levels)
    df = pd.DataFrame.from_records(index="id", data=[
        {'id': 'VL1', 'busbar_count': 3, 'switch_kinds': []}
    ])
    pp.network.create_voltage_level_topology(network, df)


def test_no_extensions_created_if_none_in_the_voltage_level():
    n = pp.network.create_four_substations_node_breaker_network()
    df = pd.DataFrame(index=["new_load"], columns=["id", "p0", "q0", "busbar_section_id", "position_order"],
                      data=[["new_load", 10.0, 3.0, "S1VL1_BBS", 0]])
    pp.network.create_load_bay(network=n, df=df, raise_exception=True)
    load = n.get_loads().loc["new_load"]
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position')
    assert position.size == 0


def test_extensions_created_if_first_equipment_in_the_voltage_level():
    n = pp.network.create_empty()
    stations = pd.DataFrame.from_records(index='id', data=[
        {'id': 'S1'}
    ])
    n.create_substations(stations)
    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 225}
    ])
    n.create_voltage_levels(voltage_levels)
    busbars = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'BBS1', 'node': 0},
        {'voltage_level_id': 'VL1', 'id': 'BBS2', 'node': 1}
    ])
    n.create_busbar_sections(busbars)
    df = pd.DataFrame(index=["new_load"], columns=["id", "p0", "q0", "busbar_section_id", "position_order"],
                      data=[["new_load", 10.0, 3.0, "BBS1", 0]])
    pp.network.create_load_bay(network=n, df=df, raise_exception=True)
    load = n.get_loads().loc["new_load"]
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position').loc['new_load']
    assert position.order == 0
    assert position.feeder_name == 'new_load'
    assert position.direction == 'BOTTOM'


def test_get_order_positions_connectables():
    n = pp.network.load(str(TEST_DIR.joinpath('node-breaker-with-extensions.xiidm')))
    df = pp.network.get_connectables_order_positions(n, 'vl1')
    assert df.shape[0] == 13
    assert df.loc['line1']['order_position'] == 70
    assert df.loc['trf2']['order_position'] == 110


def test_get_unused_order_positions():
    n = pp.network.load(str(TEST_DIR.joinpath('node-breaker-with-extensions.xiidm')))
    positions_after = pp.network.get_unused_order_positions_after(n, 'bbs4')
    assert positions_after.left == 121
    assert positions_after.right == 2147483647
    positions_before = pp.network.get_unused_order_positions_before(n, 'bbs1')
    assert positions_before.right == 0

    positions_before_no_space = pp.network.get_unused_order_positions_before(n, 'bbs4')
    assert positions_before_no_space is None
    positions_after_no_space = pp.network.get_unused_order_positions_after(n, 'bbs1')
    assert positions_after_no_space is None


def test_add_load_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame(index=["new_load"], columns=["id", "p0", "q0", "busbar_section_id", "position_order"],
                      data=[["new_load", 10.0, 3.0, "S1VL1_BBS", 0]])
    pp.network.create_load_bay(network=n, df=df, raise_exception=True)
    load = n.get_loads().loc["new_load"]
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position').loc["new_load"]
    assert position.order == 0
    assert position.feeder_name == 'new_load'
    assert position.direction == 'BOTTOM'


def test_add_load_bay_from_kwargs():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    pp.network.create_load_bay(network=n, id="new_load", p0=10.0, q0=3.0, busbar_section_id="S1VL1_BBS",
                               position_order=15)
    load = n.get_loads().loc["new_load"]
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position').loc["new_load"]
    assert position.order == 15
    assert position.feeder_name == 'new_load'
    assert position.direction == 'BOTTOM'


def test_add_generator_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    pp.network.create_generator_bay(n, pd.DataFrame.from_records(
        data=[('new_gen', 4999, -9999.99, True, 100, 150, 300, 'S1VL1_BBS', 15, 'TOP')],
        columns=['id', 'max_p', 'min_p', 'voltage_regulator_on', 'target_p', 'target_q', 'target_v',
                 'busbar_section_id', 'position_order', 'direction'],
        index='id'))
    generator = n.get_generators().loc['new_gen']
    assert generator.target_p == 100.0
    assert generator.target_q == 150.0
    assert generator.voltage_level_id == 'S1VL1'
    position = n.get_extensions('position').loc["new_gen"]
    assert position.order == 15
    assert position.feeder_name == 'new_gen'
    assert position.direction == 'TOP'


def test_add_battery_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        columns=['id', 'busbar_section_id', 'max_p', 'min_p', 'target_p', 'target_q', 'position_order'],
        data=[('new_battery', 'S1VL1_BBS', 100, 10, 90, 20, 15)],
        index='id')
    pp.network.create_battery_bay(n, df)
    battery = n.get_batteries().loc['new_battery']
    assert battery.voltage_level_id == 'S1VL1'
    assert battery.max_p == 100
    assert battery.min_p == 10
    assert battery.target_p == 90
    assert battery.target_q == 20
    position = n.get_extensions('position').loc["new_battery"]
    assert position.order == 15
    assert position.feeder_name == 'new_battery'
    assert position.direction == 'BOTTOM'


def test_add_dangling_line_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(index='id', data=[{
        'id': 'new_dangling_line',
        'name': 'dangling_line',
        'p0': 100,
        'q0': 101,
        'r': 2,
        'x': 2,
        'g': 1,
        'b': 1,
        'position_order': 15,
        'busbar_section_id': 'S1VL1_BBS'
    }])
    pp.network.create_dangling_line_bay(n, df)
    dangling_line = n.get_dangling_lines().loc['new_dangling_line']
    assert dangling_line.voltage_level_id == 'S1VL1'
    assert dangling_line.r == 2
    assert dangling_line.x == 2
    assert dangling_line.g == 1
    assert dangling_line.b == 1
    assert dangling_line.p0 == 100
    assert dangling_line.q0 == 101
    position = n.get_extensions('position').loc["new_dangling_line"]
    assert position.order == 15
    assert position.feeder_name == 'new_dangling_line'
    assert position.direction == 'BOTTOM'


def test_add_linear_shunt_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'busbar_section_id', 'position_order'],
        data=[('shunt_test', '', 'LINEAR', 1, 400, 2, 'S1VL1_BBS', 25)])
    model_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'g_per_section', 'b_per_section', 'max_section_count'],
        data=[('shunt_test', 0.14, -0.01, 2)])
    pp.network.create_shunt_compensator_bay(n, shunt_df=shunt_df, linear_model_df=model_df, raise_exception=True)

    shunt = n.get_shunt_compensators().loc['shunt_test']
    assert shunt.max_section_count == 2
    assert shunt.section_count == 1
    assert shunt.target_v == 400
    assert shunt.target_deadband == 2
    assert not shunt.voltage_regulation_on
    assert shunt.model_type == 'LINEAR'
    assert shunt.g == 0.14
    assert shunt.b == -0.01

    model = n.get_linear_shunt_compensator_sections().loc['shunt_test']
    assert model.g_per_section == 0.14
    assert model.b_per_section == -0.01
    assert model.max_section_count == 2

    position = n.get_extensions('position').loc['shunt_test']
    assert position.order == 25
    assert position.feeder_name == 'shunt_test'
    assert position.direction == 'BOTTOM'


def test_add_non_linear_shunt_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'busbar_section_id', 'position_order'],
        data=[('shunt1', '', 'NON_LINEAR', 1, 400, 2, 'S1VL1_BBS', 25),
              ('shunt2', '', 'NON_LINEAR', 1, 400, 2, 'S1VL1_BBS', 55)])
    model_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'g', 'b'],
        data=[('shunt1', 1, 2),
              ('shunt1', 3, 4),
              ('shunt2', 5, 6),
              ('shunt2', 7, 8)])
    pp.network.create_shunt_compensator_bay(n, shunt_df=shunt_df, non_linear_model_df=model_df)

    shunt = n.get_shunt_compensators().loc['shunt1']
    assert shunt.max_section_count == 2
    assert shunt.section_count == 1
    assert shunt.target_v == 400
    assert shunt.target_deadband == 2
    assert not shunt.voltage_regulation_on
    assert shunt.model_type == 'NON_LINEAR'
    assert shunt.g == 1
    assert shunt.b == 2

    model1 = n.get_non_linear_shunt_compensator_sections().loc['shunt1']
    section1 = model1.loc[0]
    section2 = model1.loc[1]
    assert section1.g == 1
    assert section1.b == 2
    assert section2.g == 3
    assert section2.b == 4

    model2 = n.get_non_linear_shunt_compensator_sections().loc['shunt2']
    section1 = model2.loc[0]
    section2 = model2.loc[1]
    assert section1.g == 5
    assert section1.b == 6
    assert section2.g == 7
    assert section2.b == 8

    position1 = n.get_extensions('position').loc['shunt1']
    assert position1.order == 25
    assert position1.feeder_name == 'shunt1'
    assert position1.direction == 'BOTTOM'

    position2 = n.get_extensions('position').loc['shunt2']
    assert position2.order == 55
    assert position2.feeder_name == 'shunt2'
    assert position2.direction == 'BOTTOM'


def test_add_svc_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'svc_test',
               'name': '',
               'busbar_section_id': 'S1VL1_BBS',
               'position_order': 15,
               'target_q': 200,
               'regulation_mode': 'REACTIVE_POWER',
               'target_v': 400,
               'b_min': 0,
               'b_max': 2}])
    pp.network.create_static_var_compensator_bay(n, df)
    svc = n.get_static_var_compensators().loc['svc_test']
    assert svc.voltage_level_id == 'S1VL1'
    assert svc.target_q == 200
    assert svc.regulation_mode == 'REACTIVE_POWER'
    assert svc.target_v == 400
    assert svc.b_min == 0
    assert svc.b_max == 2
    position = n.get_extensions('position').loc['svc_test']
    assert position.order == 15
    assert position.feeder_name == 'svc_test'
    assert position.direction == 'BOTTOM'


def test_add_lcc_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'lcc_test',
               'name': '',
               'busbar_section_id': 'S1VL1_BBS',
               'position_order': 15,
               'loss_factor': 0.1,
               'power_factor': 0.2}])
    pp.network.create_lcc_converter_station_bay(n, df)
    lcc = n.get_lcc_converter_stations().loc['lcc_test']
    assert lcc.voltage_level_id == 'S1VL1'
    assert lcc.loss_factor == pytest.approx(0.1, abs=1e-6)
    assert lcc.power_factor == pytest.approx(0.2, abs=1e-6)
    position = n.get_extensions('position').loc['lcc_test']
    assert position.order == 15
    assert position.feeder_name == 'lcc_test'
    assert position.direction == 'BOTTOM'


def test_add_vsc_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'vsc_test',
               'name': '',
               'busbar_section_id': 'S1VL1_BBS',
               'position_order': 15,
               'target_q': 200,
               'voltage_regulator_on': True,
               'loss_factor': 1.0,
               'target_v': 400}])
    pp.network.create_vsc_converter_station_bay(n, df)
    vsc = n.get_vsc_converter_stations().loc['vsc_test']
    assert vsc.voltage_level_id == 'S1VL1'
    assert vsc.target_q == 200
    assert vsc.voltage_regulator_on == True
    assert vsc.loss_factor == 1
    assert vsc.target_v == 400
    position = n.get_extensions('position').loc['vsc_test']
    assert position.order == 15
    assert position.feeder_name == 'vsc_test'
    assert position.direction == 'BOTTOM'
