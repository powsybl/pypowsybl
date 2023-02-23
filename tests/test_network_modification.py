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


def test_no_extensions_created_if_none_in_the_voltage_level():
    n = pp.network.create_four_substations_node_breaker_network()
    df = pd.DataFrame(index=["new_load"], columns=["id", "p0", "q0", "bus_or_busbar_section_id", "position_order"],
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
    df = pd.DataFrame(index=["new_load"], columns=["id", "p0", "q0", "bus_or_busbar_section_id", "position_order"],
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


def test_add_load_bay_node_breaker():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame(index=["new_load"], columns=["id", "p0", "q0", "bus_or_busbar_section_id", "position_order"],
                      data=[["new_load", 10.0, 3.0, "S1VL1_BBS", 0]])
    pp.network.create_load_bay(network=n, df=df, raise_exception=True)
    load = n.get_loads().loc["new_load"]
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position').loc["new_load"]
    assert position.order == 0
    assert position.feeder_name == 'new_load'
    assert position.direction == 'BOTTOM'


def test_add_load_bay_from_kwargs_node_breaker():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    pp.network.create_load_bay(network=n, id="new_load", p0=10.0, q0=3.0, bus_or_busbar_section_id="S1VL1_BBS",
                               position_order=15)
    load = n.get_loads().loc["new_load"]
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position').loc["new_load"]
    assert position.order == 15
    assert position.feeder_name == 'new_load'
    assert position.direction == 'BOTTOM'


def test_add_load_bay_from_kwargs_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.network.create_load_bay(network=n, id="new_load", p0=10.0, q0=3.0, bus_or_busbar_section_id='NGEN')
    load = n.get_loads().loc['new_load']
    assert load.p0 == 10.0
    assert load.q0 == 3.0
    position = n.get_extensions('position')
    assert position.size == 0


def test_add_generator_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    pp.network.create_generator_bay(n, pd.DataFrame.from_records(
        data=[('new_gen', 4999, -9999.99, True, 100, 150, 300, 'S1VL1_BBS', 15, 'TOP')],
        columns=['id', 'max_p', 'min_p', 'voltage_regulator_on', 'target_p', 'target_q', 'target_v',
                 'bus_or_busbar_section_id', 'position_order', 'direction'],
        index='id'))
    generator = n.get_generators().loc['new_gen']
    assert generator.target_p == 100.0
    assert generator.target_q == 150.0
    assert generator.voltage_level_id == 'S1VL1'
    position = n.get_extensions('position').loc["new_gen"]
    assert position.order == 15
    assert position.feeder_name == 'new_gen'
    assert position.direction == 'TOP'


def test_add_generator_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.network.create_generator_bay(n, pd.DataFrame.from_records(
        data=[('new_gen', 4999, -9999.99, True, 100, 150, 300, 'NGEN')],
        columns=['id', 'max_p', 'min_p', 'voltage_regulator_on', 'target_p', 'target_q', 'target_v',
                 'bus_or_busbar_section_id'],
        index='id'))
    generator = n.get_generators().loc['new_gen']
    assert generator.target_p == 100.0
    assert generator.target_q == 150.0
    assert generator.voltage_level_id == 'VLGEN'
    position = n.get_extensions('position')
    assert position.size == 0


def test_add_battery_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        columns=['id', 'bus_or_busbar_section_id', 'max_p', 'min_p', 'target_p', 'target_q', 'position_order'],
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


def test_add_battery_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame.from_records(
        columns=['id', 'bus_or_busbar_section_id', 'max_p', 'min_p', 'target_p', 'target_q'],
        data=[('new_battery', 'NGEN', 100, 10, 90, 20)],
        index='id')
    pp.network.create_battery_bay(n, df)
    battery = n.get_batteries().loc['new_battery']
    assert battery.voltage_level_id == 'VLGEN'
    assert battery.max_p == 100
    assert battery.min_p == 10
    assert battery.target_p == 90
    assert battery.target_q == 20
    position = n.get_extensions('position')
    assert position.size == 0


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
        'bus_or_busbar_section_id': 'S1VL1_BBS'
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


def test_add_dangling_line_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame.from_records(index='id', data=[{
        'id': 'new_dangling_line',
        'name': 'dangling_line',
        'p0': 100,
        'q0': 101,
        'r': 2,
        'x': 2,
        'g': 1,
        'b': 1,
        'bus_or_busbar_section_id': 'NGEN'
    }])
    pp.network.create_dangling_line_bay(n, df)
    dangling_line = n.get_dangling_lines().loc['new_dangling_line']
    assert dangling_line.voltage_level_id == 'VLGEN'
    assert dangling_line.r == 2
    assert dangling_line.x == 2
    assert dangling_line.g == 1
    assert dangling_line.b == 1
    assert dangling_line.p0 == 100
    assert dangling_line.q0 == 101
    position = n.get_extensions('position')
    assert position.size == 0


def test_add_linear_shunt_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'bus_or_busbar_section_id', 'position_order'],
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


def test_add_linear_shunt_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'bus_or_busbar_section_id'],
        data=[('shunt_test', '', 'LINEAR', 1, 400, 2, 'NGEN')])
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

    position = n.get_extensions('position')
    assert position.size == 0


def test_add_non_linear_shunt_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'bus_or_busbar_section_id', 'position_order'],
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


def test_add_non_linear_shunt_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'bus_or_busbar_section_id'],
        data=[('shunt1', '', 'NON_LINEAR', 1, 400, 2, 'NGEN'),
              ('shunt2', '', 'NON_LINEAR', 1, 400, 2, 'NGEN')])
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

    position = n.get_extensions('position')
    assert position.size == 0


def test_add_svc_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'svc_test',
               'name': '',
               'bus_or_busbar_section_id': 'S1VL1_BBS',
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


def test_add_svc_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'svc_test',
               'name': '',
               'bus_or_busbar_section_id': 'NGEN',
               'target_q': 200,
               'regulation_mode': 'REACTIVE_POWER',
               'target_v': 400,
               'b_min': 0,
               'b_max': 2}])
    pp.network.create_static_var_compensator_bay(n, df)
    svc = n.get_static_var_compensators().loc['svc_test']
    assert svc.voltage_level_id == 'VLGEN'
    assert svc.target_q == 200
    assert svc.regulation_mode == 'REACTIVE_POWER'
    assert svc.target_v == 400
    assert svc.b_min == 0
    assert svc.b_max == 2
    position = n.get_extensions('position')
    assert position.size == 0


def test_add_lcc_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'lcc_test',
               'name': '',
               'bus_or_busbar_section_id': 'S1VL1_BBS',
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


def test_add_lcc_bay_node_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'lcc_test',
               'name': '',
               'bus_or_busbar_section_id': 'NGEN',
               'position_order': 15,
               'loss_factor': 0.1,
               'power_factor': 0.2}])
    pp.network.create_lcc_converter_station_bay(n, df)
    lcc = n.get_lcc_converter_stations().loc['lcc_test']
    assert lcc.voltage_level_id == 'VLGEN'
    assert lcc.loss_factor == pytest.approx(0.1, abs=1e-6)
    assert lcc.power_factor == pytest.approx(0.2, abs=1e-6)
    position = n.get_extensions('position')
    assert position.size == 0


def test_add_vsc_bay():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'vsc_test',
               'name': '',
               'bus_or_busbar_section_id': 'S1VL1_BBS',
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


def test_add_vsc_bay_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'vsc_test',
               'name': '',
               'bus_or_busbar_section_id': 'NGEN',
               'target_q': 200,
               'voltage_regulator_on': True,
               'loss_factor': 1.0,
               'target_v': 400}])
    pp.network.create_vsc_converter_station_bay(n, df)
    vsc = n.get_vsc_converter_stations().loc['vsc_test']
    assert vsc.voltage_level_id == 'VLGEN'
    assert vsc.target_q == 200
    assert vsc.voltage_regulator_on == True
    assert vsc.loss_factor == 1
    assert vsc.target_v == 400
    position = n.get_extensions('position')
    assert position.size == 0


def test_create_branch_feeder_bays_twt_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame(index=['new_twt'],
                      columns=['id', 'bus_or_busbar_section_id_1', 'bus_or_busbar_section_id_2', 'r', 'x', 'g', 'b',
                               'rated_u1', 'rated_u2', 'rated_s'],
                      data=[['new_twt', 'NGEN', 'NHV1', 5.0, 50.0, 2.0, 4.0, 225.0, 400.0, 1.0]])
    pp.network.create_2_windings_transformer_bays(n, df)
    retrieved_new_twt = n.get_2_windings_transformers().loc['new_twt']
    assert retrieved_new_twt["r"] == 5.0
    assert retrieved_new_twt["x"] == 50.0
    assert retrieved_new_twt["g"] == 2.0
    assert retrieved_new_twt["b"] == 4.0
    assert retrieved_new_twt["rated_u1"] == 225.0
    assert retrieved_new_twt["rated_u2"] == 400.0
    assert retrieved_new_twt["rated_s"] == 1.0
    assert n.get_extensions('position').size == 0


def test_create_branch_feeder_bays_line_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame(index=['new_line'],
                      columns=['id', 'bus_or_busbar_section_id_1', 'bus_or_busbar_section_id_2', 'r', 'x', 'g1', 'g2', 'b1', 'b2'],
                      data=[['new_line', 'NHV1', 'NHV2', 5.0, 50.0, 20.0, 30.0, 40.0, 50.0]])
    pp.network.create_line_bays(n, df)
    retrieved_newline = n.get_lines().loc['new_line']
    assert retrieved_newline["r"] == 5.0
    assert retrieved_newline["x"] == 50.0
    assert retrieved_newline["g1"] == 20.0
    assert retrieved_newline["g2"] == 30.0
    assert retrieved_newline["b1"] == 40.0
    assert retrieved_newline["b2"] == 50.0
    assert retrieved_newline["connected1"]
    assert retrieved_newline["connected2"]
    assert n.get_extensions('position').size == 0


def test_create_coupling_device():
    n = pp.network.create_empty()
    n.create_substations(id='S1')
    n.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='NODE_BREAKER',
                            nominal_v=225, low_voltage_limit=380, high_voltage_limit=420)
    busbars = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'BBS1', 'node': 0},
        {'voltage_level_id': 'VL1', 'id': 'BBS2', 'node': 1},
        {'voltage_level_id': 'VL1', 'id': 'BBS3', 'node': 2},
    ])
    n.create_busbar_sections(busbars)
    n.create_extensions('busbarSectionPosition', id=['BBS1', 'BBS2', 'BBS3'], busbar_index=[1, 2, 3],
                        section_index=[1, 1, 1])
    assert len(n.get_switches().index) == 0
    coupling_device = pd.DataFrame.from_records(index='busbar_section_id_1', data=[
        {'busbar_section_id_1': 'BBS1', 'busbar_section_id_2': 'BBS2'},
    ])
    pp.network.create_coupling_device(n, coupling_device)
    switches = n.get_switches()
    assert len(switches.index) == 7
    assert len(switches[switches["kind"] == "DISCONNECTOR"].index) == 6
    assert len(switches[switches["kind"] == "BREAKER"].index) == 1
    assert len(switches[switches["open"] == True].index) == 4
    assert len(switches[switches["open"] == False].index) == 3


def test_create_coupling_device_kwargs():
    n = pp.network.create_empty()
    n.create_substations(id='S1')
    n.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='NODE_BREAKER',
                                  nominal_v=225, low_voltage_limit=380, high_voltage_limit=420)
    busbars = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'BBS1', 'node': 0},
        {'voltage_level_id': 'VL1', 'id': 'BBS2', 'node': 1},
        {'voltage_level_id': 'VL1', 'id': 'BBS3', 'node': 2},
    ])
    n.create_busbar_sections(busbars)
    n.create_extensions('busbarSectionPosition', id=['BBS1', 'BBS2', 'BBS3'], busbar_index=[1, 2, 3],
                        section_index=[1, 1, 1])
    assert len(n.get_switches().index) == 0
    pp.network.create_coupling_device(n, busbar_section_id_1='BBS1', busbar_section_id_2='BBS2', switch_prefix_id='sw')
    switches = n.get_switches()
    assert len(switches.index) == 7
    assert len(switches[switches["kind"] == "DISCONNECTOR"].index) == 6
    assert len(switches[switches["kind"] == "BREAKER"].index) == 1
    assert len(switches[switches["open"] == True].index) == 4
    assert len(switches[switches["open"] == False].index) == 3


def test_remove_feeder_bay():
    n = pp.network.create_four_substations_node_breaker_network()
    df = pd.DataFrame(index=['new_line'],
                      columns=['id', 'bus_or_busbar_section_id_1', 'bus_or_busbar_section_id_2', 'position_order_1',
                               'position_order_2', 'direction_1', 'direction_2', 'r', 'x', 'g1', 'g2', 'b1', 'b2'],
                      data=[['new_line', 'S1VL2_BBS1', 'S2VL1_BBS', 115, 121, 'TOP', 'TOP', 5.0, 50.0, 20.0, 30.0, 40.0,
                             50.0]])
    pp.network.create_line_bays(n, df)
    assert 'new_line' in n.get_lines().index
    assert 'new_line1_BREAKER' in n.get_switches().index
    assert 'new_line1_DISCONNECTOR' in n.get_switches().index
    assert 'new_line2_BREAKER' in n.get_switches().index
    assert 'new_line2_DISCONNECTOR' in n.get_switches().index
    pp.network.remove_feeder_bays(n, 'new_line')
    assert 'new_line1_BREAKER' not in n.get_switches().index
    assert 'new_line1_DISCONNECTOR' not in n.get_switches().index
    assert 'new_line2_BREAKER' not in n.get_switches().index
    assert 'new_line2_DISCONNECTOR' not in n.get_switches().index


def test_create_branch_feeder_bays_line():
    n = pp.network.create_four_substations_node_breaker_network()
    df = pd.DataFrame(index=['new_line'],
                      columns=['id', 'bus_or_busbar_section_id_1', 'bus_or_busbar_section_id_2', 'position_order_1',
                               'position_order_2', 'direction_1', 'direction_2', 'r', 'x', 'g1', 'g2', 'b1', 'b2'],
                      data=[['new_line', 'S1VL2_BBS1', 'S2VL1_BBS', 115, 121, 'TOP', 'TOP', 5.0, 50.0, 20.0, 30.0, 40.0,
                             50.0]])
    pp.network.create_line_bays(n, df)
    retrieved_newline = n.get_lines().loc['new_line']
    assert retrieved_newline["r"] == 5.0
    assert retrieved_newline["x"] == 50.0
    assert retrieved_newline["g1"] == 20.0
    assert retrieved_newline["g2"] == 30.0
    assert retrieved_newline["b1"] == 40.0
    assert retrieved_newline["b2"] == 50.0
    assert retrieved_newline["connected1"]
    assert retrieved_newline["connected2"]


def test_create_multiple_branch_feeder_bays_line():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    df = pd.DataFrame.from_records(index='id', data=[
        {'id': 'new_line1', 'bus_or_busbar_section_id_1': 'S1VL2_BBS1', 'bus_or_busbar_section_id_2': 'S2VL1_BBS',
         'position_order_1': 105, 'position_order_2': 40, 'r': 5.0, 'x': 50.0, 'g1': 20.0, 'b1': 30.0, 'g2': 40.0,
         'b2': 50.0},
        {'id': 'new_line2', 'bus_or_busbar_section_id_1': 'S1VL2_BBS1', 'bus_or_busbar_section_id_2': 'S2VL1_BBS',
         'position_order_1': 106, 'position_order_2': 45, 'r': 5.0, 'x': 50.0, 'g1': 20.0, 'b1': 30.0, 'g2': 40.0,
         'b2': 50.0},
    ])
    pp.network.create_line_bays(n, df)
    new_line_1 = n.get_lines().loc['new_line1']
    assert new_line_1["connected1"]
    assert new_line_1["connected2"]
    position1 = n.get_extensions('position').loc["new_line1"]
    assert all([a == b for a, b in zip(position1.side.values, ['ONE', 'TWO'])])
    assert all([a == b for a, b in zip(position1.feeder_name.values, ['new_line1', 'new_line1'])])
    assert all([a == b for a, b in zip(position1.direction.values, ['TOP', 'TOP'])])
    assert all([a == b for a, b in zip(position1.order.values, [105, 40])])
    new_line_2 = n.get_lines().loc['new_line2']
    assert new_line_2["connected1"]
    assert new_line_2["connected2"]
    position2 = n.get_extensions('position').loc["new_line2"]
    assert all([a == b for a, b in zip(position2.side.values, ['ONE', 'TWO'])])
    assert all([a == b for a, b in zip(position2.feeder_name.values, ['new_line2', 'new_line2'])])
    assert all([a == b for a, b in zip(position2.direction.values, ['TOP', 'TOP'])])
    assert all([a == b for a, b in zip(position2.order.values, [106, 45])])


def test_create_line_on_line():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.create_substations(id='P3', country='BE')
    n.create_voltage_levels(id='VLTEST', substation_id='P3', nominal_v=380, topology_kind='BUS_BREAKER',
                            high_voltage_limit=400, low_voltage_limit=370)
    assert 'VLTEST' in n.get_voltage_levels().index
    assert n.get_voltage_levels().loc['VLTEST']['substation_id'] == 'P3'
    n.create_buses(id='VLTEST_0', voltage_level_id='VLTEST')

    assert 'VLTEST_0' in n.get_bus_breaker_topology('VLTEST').buses.index

    n.create_generators(id='GEN3', max_p=4999, min_p=-9999.99, voltage_level_id='VLTEST',
                        voltage_regulator_on=True, target_p=100, target_q=150,
                        target_v=300, bus_id='VLTEST_0')
    generators = n.get_generators(all_attributes=True)
    assert 'GEN3' in generators.index
    gen3 = generators.loc['GEN3']
    assert gen3['voltage_level_id'] == 'VLTEST'
    assert gen3['bus_breaker_bus_id'] == 'VLTEST_0'
    assert gen3['target_p'] == 100
    assert gen3['bus_id'] == 'VLTEST_0#0'

    pp.network.create_line_on_line(n, bbs_or_bus_id='VLTEST_0', new_line_id='test_line', new_line_r=5.0, new_line_x=50.0,
                                   new_line_b1=2.0, new_line_b2=3.0, new_line_g1=4.0, new_line_g2=5.0,
                                   line_id='NHV1_NHV2_1', position_percent=75.0)
    retrieved_newline = n.get_lines().loc['test_line']
    assert retrieved_newline["r"] == 5.0
    assert retrieved_newline["x"] == 50.0
    assert retrieved_newline["b1"] == 2.0
    assert retrieved_newline["b2"] == 3.0
    assert retrieved_newline["g1"] == 4.0
    assert retrieved_newline["g2"] == 5.0
    assert retrieved_newline["connected1"]
    assert retrieved_newline["connected2"]

    # Check splitted line percent
    retrieved_splittedline1 = n.get_lines().loc['NHV1_NHV2_1_1']
    assert retrieved_splittedline1["r"] == 2.25

    retrieved_splittedline2 = n.get_lines().loc['NHV1_NHV2_1_2']
    assert retrieved_splittedline2["r"] == 0.75
    generators = n.get_generators(all_attributes=True)
    assert 'GEN3' in generators.index
    assert generators.loc['GEN3']['bus_id'] == 'VLTEST_0#0'


def test_revert_create_line_on_line():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.create_substations(id='P3', country='BE')
    n.create_voltage_levels(id='VLTEST', substation_id='P3', nominal_v=380, topology_kind='BUS_BREAKER',
                            high_voltage_limit=400, low_voltage_limit=370)
    n.create_buses(id='VLTEST_0', voltage_level_id='VLTEST')

    n.create_generators(id='GEN3', max_p=4999, min_p=-9999.99, voltage_level_id='VLTEST',
                        voltage_regulator_on=True, target_p=100, target_q=150,
                        target_v=300, bus_id='VLTEST_0')
    assert len(n.get_lines()) == 2

    pp.network.create_line_on_line(n, bbs_or_bus_id='VLTEST_0', new_line_id='test_line', new_line_r=5.0, new_line_x=50.0,
                                   new_line_b1=2.0, new_line_b2=3.0, new_line_g1=4.0, new_line_g2=5.0,
                                   line_id='NHV1_NHV2_1', position_percent=75.0)
    assert len(n.get_lines()) == 4

    pp.network.revert_create_line_on_line(network=n, line_to_be_merged1_id='NHV1_NHV2_1_1',
                                          line_to_be_merged2_id='NHV1_NHV2_1_2', line_to_be_deleted='test_line',
                                          merged_line_id='NHV1_NHV2_1')
    assert len(n.get_lines()) == 2

    retrieved_line = n.get_lines().loc['NHV1_NHV2_1']
    assert retrieved_line["connected1"]
    assert retrieved_line["connected2"]


def test_create_branch_feeder_bays_twt():
    n = pp.network.create_four_substations_node_breaker_network()
    df = pd.DataFrame(index=['new_twt'],
                      columns=['id', 'bus_or_busbar_section_id_1', 'bus_or_busbar_section_id_2', 'position_order_1',
                               'position_order_2', 'direction_1', 'direction_2', 'r', 'x', 'g', 'b', 'rated_u1',
                               'rated_u2', 'rated_s'],
                      data=[['new_twt', 'S1VL1_BBS', 'S1VL2_BBS1', 115, 121, 'TOP', 'TOP', 5.0, 50.0, 2.0, 4.0, 225.0,
                             400.0, 1.0]])
    pp.network.create_2_windings_transformer_bays(n, df)
    retrieved_newtwt = n.get_2_windings_transformers().loc['new_twt']
    assert retrieved_newtwt["r"] == 5.0
    assert retrieved_newtwt["x"] == 50.0
    assert retrieved_newtwt["g"] == 2.0
    assert retrieved_newtwt["b"] == 4.0
    assert retrieved_newtwt["rated_u1"] == 225.0
    assert retrieved_newtwt["rated_u2"] == 400.0
    assert retrieved_newtwt["rated_s"] == 1.0


def test_create_multiple_branch_feeder_bays_twt():
    n = pp.network.create_four_substations_node_breaker_network_with_extensions()
    two_windings_transformers = pd.DataFrame.from_records(index='id', data=[
        {'id': 'new_twt1', 'bus_or_busbar_section_id_1': 'S1VL2_BBS1', 'bus_or_busbar_section_id_2': 'S1VL1_BBS',
         'position_order_1': 105, 'position_order_2': 50, 'r': 5.0, 'x': 50.0, 'g': 20.0, 'b': 30.0, 'rated_u1': 40.0,
         'rated_u2': 50.0, 'rated_s': 60.0},
        {'id': 'new_twt2', 'bus_or_busbar_section_id_1': 'S1VL2_BBS1', 'bus_or_busbar_section_id_2': 'S1VL1_BBS',
         'position_order_1': 106, 'position_order_2': 51, 'r': 5.0, 'x': 50.0, 'g': 20.0, 'b': 30.0, 'rated_u1': 40.0,
         'rated_u2': 50.0, 'rated_s': 60.0},
    ])
    pp.network.create_2_windings_transformer_bays(n, two_windings_transformers)
    new_twt_1 = n.get_2_windings_transformers().loc['new_twt1']
    assert new_twt_1["r"] == 5.0
    assert new_twt_1["x"] == 50.0
    assert new_twt_1["g"] == 20.0
    assert new_twt_1["b"] == 30.0
    assert new_twt_1["rated_u1"] == 40.0
    assert new_twt_1["rated_u2"] == 50.0
    assert new_twt_1["rated_s"] == 60.0
    position1 = n.get_extensions('position').loc["new_twt1"]
    assert all([a == b for a, b in zip(position1.side.values, ['ONE', 'TWO'])])
    assert all([a == b for a, b in zip(position1.feeder_name.values, ['new_twt1', 'new_twt1'])])
    assert all([a == b for a, b in zip(position1.direction.values, ['TOP', 'TOP'])])
    assert all([a == b for a, b in zip(position1.order.values, [105, 50])])
    new_twt_2 = n.get_2_windings_transformers().loc['new_twt2']
    assert new_twt_2["r"] == 5.0
    assert new_twt_2["x"] == 50.0
    assert new_twt_2["g"] == 20.0
    assert new_twt_2["b"] == 30.0
    assert new_twt_2["rated_u1"] == 40.0
    assert new_twt_2["rated_u2"] == 50.0
    assert new_twt_2["rated_s"] == 60.0
    position2 = n.get_extensions('position').loc["new_twt2"]
    assert all([a == b for a, b in zip(position2.side.values, ['ONE', 'TWO'])])
    assert all([a == b for a, b in zip(position2.feeder_name.values, ['new_twt2', 'new_twt2'])])
    assert all([a == b for a, b in zip(position2.direction.values, ['TOP', 'TOP'])])
    assert all([a == b for a, b in zip(position2.order.values, [106, 51])])


def test_connect_voltage_level_on_line():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.create_voltage_levels(id='N_VL', topology_kind='NODE_BREAKER', nominal_v=400)
    n.create_busbar_sections(id='BBS', voltage_level_id='N_VL', node=0)
    pp.network.connect_voltage_level_on_line(n, "BBS", "NHV1_NHV2_1", 75.0)

    retrieved_splittedline1 = n.get_lines(id=['NHV1_NHV2_1_1'])
    assert retrieved_splittedline1.loc['NHV1_NHV2_1_1', "voltage_level1_id"] == "VLHV1"
    assert retrieved_splittedline1.loc['NHV1_NHV2_1_1', "voltage_level2_id"] == "N_VL"
    assert retrieved_splittedline1.loc['NHV1_NHV2_1_1', "r"] == 2.25

    retrieved_splittedline2 = n.get_lines(id=['NHV1_NHV2_1_2'])
    assert retrieved_splittedline2.loc['NHV1_NHV2_1_2', "voltage_level1_id"] == "N_VL"
    assert retrieved_splittedline2.loc['NHV1_NHV2_1_2', "voltage_level2_id"] == "VLHV2"
    assert retrieved_splittedline2.loc['NHV1_NHV2_1_2', "r"] == 0.75


def test_revert_connect_voltage_level_on_line():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.create_voltage_levels(id='N_VL', topology_kind='NODE_BREAKER', nominal_v=400)
    n.create_busbar_sections(id='BBS', voltage_level_id='N_VL', node=0)

    assert len(n.get_lines()) == 2
    retrieved_line = n.get_lines(id=['NHV1_NHV2_1'])
    assert retrieved_line.loc['NHV1_NHV2_1', "voltage_level1_id"] == "VLHV1"
    assert retrieved_line.loc['NHV1_NHV2_1', "voltage_level2_id"] == "VLHV2"
    assert retrieved_line.loc['NHV1_NHV2_1', "r"] == 3.0

    pp.network.connect_voltage_level_on_line(n, "BBS", "NHV1_NHV2_1", 75.0)

    assert len(n.get_lines()) == 3

    pp.network.revert_connect_voltage_level_on_line(network=n, line1_id='NHV1_NHV2_1_1', line2_id='NHV1_NHV2_1_2',
                                                    line_id='NHV1_NHV2_1')

    assert len(n.get_lines()) == 2
    retrieved_line = n.get_lines(id=['NHV1_NHV2_1'])
    assert retrieved_line.loc['NHV1_NHV2_1', "voltage_level1_id"] == "VLHV1"
    assert retrieved_line.loc['NHV1_NHV2_1', "voltage_level2_id"] == "VLHV2"
    assert retrieved_line.loc['NHV1_NHV2_1', "r"] == 3.0


def test_replace_tee_point_by_voltage_level_on_line():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.create_substations(id='P3', country='BE')
    n.create_voltage_levels(id='VLTEST', substation_id='P3', nominal_v=380, topology_kind='BUS_BREAKER',
                            high_voltage_limit=400, low_voltage_limit=370)
    n.create_buses(id='VLTEST_0', voltage_level_id='VLTEST')

    n.create_generators(id='GEN3', max_p=4999, min_p=-9999.99, voltage_level_id='VLTEST',
                        voltage_regulator_on=True, target_p=100, target_q=150,
                        target_v=300, bus_id='VLTEST_0')
    generators = n.get_generators(all_attributes=True)
    gen3 = generators.loc['GEN3']

    pp.network.create_line_on_line(n, bbs_or_bus_id='VLTEST_0', new_line_id='test_line', new_line_r=5.0, new_line_x=50.0,
                                   new_line_b1=2.0, new_line_b2=3.0, new_line_g1=4.0, new_line_g2=5.0,
                                   line_id='NHV1_NHV2_1', position_percent=75.0)

    assert len(n.get_lines()) == 4

    pp.network.replace_tee_point_by_voltage_level_on_line(n, 'NHV1_NHV2_1_1', 'NHV1_NHV2_1_2', 'test_line', 'VLTEST_0',
                                                          'NewLine1', 'NewLine2')

    # Remove test_line and replace NHV1_NHV2_1_1 and NHV1_NHV2_1_2 by NewLine1 and NewLine2
    assert len(n.get_lines()) == 3

    retrieved_newline1 = n.get_lines().loc['NewLine1']
    assert retrieved_newline1["connected1"]
    assert retrieved_newline1["connected2"]

    retrieved_newline2 = n.get_lines().loc['NewLine1']
    assert retrieved_newline2["connected1"]
    assert retrieved_newline2["connected2"]

    assert 'test_line' not in n.get_lines().index
