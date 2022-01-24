import pandas as pd
import pytest

import pypowsybl.network as pn
import util
import pathlib
from numpy import NaN


@pytest.fixture
def this_dir():
    return pathlib.Path(__file__).parent


def test_substation_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_substations(pd.DataFrame.from_records(index='id', data=[{
        'id': 'S3',
        'country': 'DE'
    }]))
    s3 = n.get_substations().loc['S3']
    assert s3.country == 'DE'


def test_generators_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_generators(pd.DataFrame.from_records(
        data=[('GEN3', 4999, -9999.99, 'VLHV1', True, 100, 150, 300, 'NHV1')],
        columns=['id', 'max_p', 'min_p', 'voltage_level_id',
                 'voltage_regulator_on', 'target_p', 'target_q', 'target_v', 'bus_id'],
        index='id'))
    gen3 = n.get_generators().loc['GEN3']
    assert gen3.target_p == 100.0
    assert gen3.target_q == 150.0
    assert gen3.voltage_level_id == 'VLHV1'
    assert gen3.bus_id == 'VLHV1_0'


def test_bus_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_buses(pd.DataFrame(index=pd.Series(name='id', data=['BUS_TEST']),
                                columns=['voltage_level_id'],
                                data=[['VLHV2']]))
    n.create_lines(pd.DataFrame(index=pd.Series(name='id', data=['NHV1_NHV2_3']),
                                columns=['r', 'x', 'g1', 'g2', 'b1', 'b2', 'voltage_level1_id', 'voltage_level2_id',
                                         'bus1_id', 'bus2_id'],
                                data=[[2, 2, 1, 1, 1, 1, 'VLHV1', 'VLHV2', 'NHV1', 'BUS_TEST']]))

    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLHV2_1', 'VLLOAD_0']),
        columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                 'voltage_level_id'],
        data=[['', NaN, NaN, 0, 0, 'VLGEN'],
              ['', 380, NaN, 0, 0, 'VLHV1'],
              ['', 380, NaN, 0, 0, 'VLHV2'],
              ['', NaN, NaN, 0, 0, 'VLHV2'],
              ['', NaN, NaN, 0, 0, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected, n.get_buses(), check_dtype=False)


def test_loads_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_loads(pd.DataFrame.from_records(
        data=[['LOAD2', 'VLHV2', 'NHV2', 'UNDEFINED', 500, 100]],
        columns=['id', 'voltage_level_id', 'bus_id', 'type', 'p0', 'q0'],
        index='id'))
    load2 = n.get_loads().loc['LOAD2']
    assert load2.p0 == 500
    assert load2.q0 == 100
    assert load2.type == 'UNDEFINED'
    assert load2.voltage_level_id == 'VLHV2'
    assert load2.bus_id == 'VLHV2_0'


def test_batteries_creation():
    n = util.create_battery_network()
    df = pd.DataFrame.from_records(
        data=[('BAT3', 'VLBAT', 'NBAT', 9999, -9999, 100, -150)],
        columns=['id', 'voltage_level_id', 'bus_id', 'max_p', 'min_p', 'p0', 'q0'],
        index='id')
    n.create_batteries(df)
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['BAT', 'BAT2', 'BAT3']),
        columns=['name', 'max_p', 'min_p', 'p0', 'q0', 'p', 'q', 'i',
                 'voltage_level_id', 'bus_id', 'connected'],
        data=[['', 9999.99, -9999.99, 9999.99, 9999.99, -605, -225, NaN, 'VLBAT', 'VLBAT_0', True],
              ['', 200, -200, 101, 201, -605, -225, NaN, 'VLBAT', 'VLBAT_0', True],
              ['', 9999, -9999, 100, -150, NaN, NaN, NaN, 'VLBAT', 'VLBAT_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_batteries(), check_dtype=False)


def test_vsc_data_frame():
    n = pn.create_four_substations_node_breaker_network()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'VSC3',
               'name': '',
               'voltage_level_id': 'S3VL1',
               'node': 1,
               'target_q': 200,
               'voltage_regulator_on': True,
               'loss_factor': 1.0,
               'target_v': 400}])
    n.create_vsc_converter_stations(df)
    vsc3 = n.get_vsc_converter_stations().loc['VSC3']
    assert vsc3.voltage_level_id == 'S3VL1'
    assert vsc3.bus_id == 'S3VL1_0'
    assert vsc3.target_q == 200
    assert vsc3.voltage_regulator_on == True
    assert vsc3.loss_factor == 1
    assert vsc3.target_v == 400


def test_svc_creation():
    n = pn.create_four_substations_node_breaker_network()
    df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': 'SVC2',
               'name': '',
               'voltage_level_id': 'S2VL1',
               'node': 1,
               'target_q': 200,
               'regulation_mode': 'REACTIVE_POWER',
               'target_v': 400,
               'b_min': 0,
               'b_max': 2}])
    n.create_static_var_compensators(df)
    svc2 = n.get_static_var_compensators().loc['SVC2']
    assert svc2.voltage_level_id == 'S2VL1'
    assert svc2.target_q == 200
    assert svc2.regulation_mode == 'REACTIVE_POWER'
    assert svc2.target_v == 400
    assert svc2.b_min == 0
    assert svc2.b_max == 2


def test_switches_creation(this_dir):
    n = pn.load(str(this_dir.joinpath('node-breaker.xiidm')))
    n.create_switches(pd.DataFrame(index=['TEST_BREAKER', 'TEST_DISCONNECTOR'],
                                   columns=['name', 'kind', 'node1', 'node2', 'open', 'retained',
                                            'voltage_level_id'],
                                   data=[['TEST_BREAKER', 'BREAKER', 1, 2, True, True, 'VLGEN'],
                                         ['TEST_DISCONNECTOR', 'DISCONNECTOR', 3, 4, True, True, 'VLGEN']]))
    switches = n.get_switches()
    breaker = switches.loc['TEST_BREAKER']
    disconnector = switches.loc['TEST_DISCONNECTOR']
    assert breaker['kind'] == 'BREAKER'
    assert breaker['open'] is True
    assert breaker['voltage_level_id'] == 'VLGEN'
    assert disconnector['kind'] == 'DISCONNECTOR'
    assert disconnector['open'] is True
    assert disconnector['voltage_level_id'] == 'VLGEN'


def test_2_windings_transformers_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_2_windings_transformers(pd.DataFrame.from_records(index='id', data=[{
        'id': 'TWT_TEST',
        'r': 0.1,
        'x': 10,
        'g': 1,
        'b': 0.1,
        'rated_u1': 400,
        'rated_u2': 158,
        'voltage_level1_id': 'VLHV1',
        'voltage_level2_id': 'VLGEN',
        'bus1_id': 'NHV1',
        'bus2_id': 'NGEN'
    }]))

    twt = n.get_2_windings_transformers().loc['TWT_TEST']
    assert twt.r == 0.1
    assert twt.x == 10
    assert twt.g == 1
    assert twt.b == 0.1
    assert twt.rated_u1 == 400
    assert twt.rated_u2 == 158
    assert twt.voltage_level1_id == 'VLHV1'
    assert twt.voltage_level2_id == 'VLGEN'


def test_voltage_levels_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    df = pd.DataFrame.from_records(index='id', data=[{
        'id': 'VLTEST',
        'substation_id': 'P1',
        'high_voltage_limit': 250,
        'low_voltage_limit': 200,
        'nominal_v': 225,
        'topology_kind': 'BUS_BREAKER'
    }])
    n.create_voltage_levels(df)
    vl = n.get_voltage_levels().loc['VLTEST']
    assert vl.substation_id == 'P1'
    assert vl.high_voltage_limit == 250
    assert vl.low_voltage_limit == 200
    assert vl.nominal_v == 225


def test_ratio_tap_changers_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_ratio_tap_changers(pd.DataFrame(index=pd.Series(name='id', data=['NGEN_NHV1']),
                                             columns=['target_deadband', 'target_v', 'on_load', 'low_tap', 'tap'],
                                             data=[[2, 200, False, 0, 1]]),
                                pd.DataFrame(index=pd.Series(name='id', data=['NGEN_NHV1', 'NGEN_NHV1']),
                                             columns=['b', 'g', 'r', 'x', 'rho'],
                                             data=[[2, 2, 1, 1, 0.5], [2, 2, 1, 1, 0.5]]))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['NGEN_NHV1', 'NHV2_NLOAD']),
                            columns=['tap', 'low_tap', 'high_tap', 'step_count', 'on_load', 'regulating',
                                     'target_v', 'target_deadband', 'regulating_bus_id', 'rho',
                                     'alpha'],
                            data=[[1, 0, 1, 2, False, False, 200, 2, '', 8.33, NaN],
                                  [0, 0, 2, 3, True, False, 180.0, 0.0, 'VLLOAD_0', 0.34, NaN]])
    pd.testing.assert_frame_equal(expected, n.get_ratio_tap_changers(), check_dtype=False, atol=10 ** -2)
    expected = pd.DataFrame(
        index=pd.MultiIndex.from_tuples(
            [('NGEN_NHV1', 0), ('NGEN_NHV1', 1), ('NHV2_NLOAD', 0),
             ('NHV2_NLOAD', 1), ('NHV2_NLOAD', 2)],
            names=['id', 'position']),
        columns=['rho', 'r', 'x', 'g', 'b'],
        data=[[0.5, 1, 1, 2, 2],
              [0.5, 1, 1, 2, 2],
              [0.85, 0, 0, 0, 0],
              [1.00, 0, 0, 0, 0],
              [1.15, 0, 0, 0, 0]])
    pd.testing.assert_frame_equal(expected, n.get_ratio_tap_changer_steps(), check_dtype=False, atol=10 ** -2)


def test_phase_tap_changers_creation():
    n = pn.create_four_substations_node_breaker_network()
    n.create_2_windings_transformers(pd.DataFrame(index=['TWT_TEST'],
                                                  columns=['r', 'x', 'g', 'b', 'rated_u1', 'rated_u2',
                                                           'voltage_level1_id', 'voltage_level2_id', 'node1',
                                                           'node2'],
                                                  data=[[0.1, 10, 1, 0.1, 400, 158, 'S1VL1', 'S1VL2', 1, 2]]))
    n.create_phase_tap_changers(pd.DataFrame(index=pd.Series(name='id', data=['TWT_TEST']),
                                             columns=['target_deadband', 'regulation_mode', 'low_tap', 'tap'],
                                             data=[[2, 'CURRENT_LIMITER', 0, 1]]),
                                pd.DataFrame(index=pd.Series(name='id', data=['TWT_TEST', 'TWT_TEST']),
                                             columns=['b', 'g', 'r', 'x', 'rho', 'alpha'],
                                             data=[[2, 2, 1, 1, 0.5, 0.1], [2, 2, 1, 1, 0.5, 0.1]]))
    created = pd.DataFrame(index=pd.Series(name='id', data=['TWT', 'TWT_TEST']),
                           columns=['tap', 'low_tap', 'high_tap', 'step_count', 'regulating', 'regulation_mode',
                                    'regulation_value', 'target_deadband', 'regulating_bus_id'],
                           data=[[10, 0, 32, 33, True, 'CURRENT_LIMITER', 1000, 100, 'S1VL1_0'],
                                 [1, 0, 1, 2, False, 'CURRENT_LIMITER', NaN, 2, '']])
    pd.testing.assert_frame_equal(created, n.get_phase_tap_changers(), check_dtype=False)


def test_lines_creation():
    n = pn.create_four_substations_node_breaker_network()
    n.create_lines(pd.DataFrame(index=pd.Series(name='id', data=['LINE_TEST']),
                                columns=['r', 'x', 'g1', 'g2', 'b1', 'b2', 'voltage_level1_id', 'voltage_level2_id',
                                         'node1', 'node2'],
                                data=[[2, 2, 1, 1, 1, 1, 'S2VL1', 'S4VL1', 1, 1]]))
    new_line = n.get_lines().loc['LINE_TEST']
    assert new_line.r == 2
    assert new_line.x == 2
    assert new_line.g1 == 1
    assert new_line.g2 == 1
    assert new_line.b1 == 1
    assert new_line.b2 == 1
    assert new_line.voltage_level1_id == 'S2VL1'
    assert new_line.voltage_level2_id == 'S4VL1'


def test_dangling_lines():
    n = util.create_dangling_lines_network()
    df = pd.DataFrame.from_records(index='id', data=[{
        'id': 'DL_TEST',
        'name': '',
        'voltage_level_id': 'VL',
        'bus_id': 'BUS',
        'p0': 100,
        'q0': 101,
        'r': 2,
        'x': 2,
        'g': 1,
        'b': 1
    }])
    n.create_dangling_lines(df)
    new_dl = n.get_dangling_lines().loc['DL_TEST']
    assert new_dl.voltage_level_id == 'VL'
    assert new_dl.r == 2
    assert new_dl.x == 2
    assert new_dl.g == 1
    assert new_dl.b == 1
    assert new_dl.p0 == 100
    assert new_dl.q0 == 101


def test_shunt():
    n = pn.create_four_substations_node_breaker_network()
    n.create_shunt_compensators(pd.DataFrame(index=pd.Series(name='id', data=['SHUNT_TEST']),
                                             columns=['name', 'g', 'b', 'model_type', 'max_section_count',
                                                      'section_count', 'voltage_regulation_on', 'target_v',
                                                      'target_deadband', 'voltage_level_id', 'node'],
                                             data=[['SHUNT_TEST', 0.0, -0.0, 'LINEAR', 1, 0, True, 400, 2,
                                                    'S1VL2', 2]]),
                                pd.DataFrame(index=['SHUNT_TEST'],
                                             columns=['g_per_section', 'b_per_section', 'max_section_count'],
                                             data=[[0.14, -0.01, 2]]))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['SHUNT', 'SHUNT_TEST']),
                            columns=['name', 'g', 'b', 'model_type', 'max_section_count', 'section_count',
                                     'voltage_regulation_on', 'target_v',
                                     'target_deadband', 'regulating_bus_id', 'p', 'q', 'i',
                                     'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 0.0, -0.0, 'LINEAR', 1, 0, True, 50, 3,
                                   '', NaN, 1900, NaN, 'S1VL2', '', False],
                                  ['SHUNT_TEST', 0.0, -0.0, 'LINEAR', 2, 0, False, 400, 2,
                                   'S1VL2_0', NaN, NaN, NaN, 'S1VL2', 'S1VL2_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_shunt_compensators(), check_dtype=False)


def test_busbar_sections():
    n = pn.create_four_substations_node_breaker_network()
    n.create_busbar(pd.DataFrame(index=pd.Series(name='id',
                                                 data=['S_TEST']),
                                 columns=['name', 'voltage_level_id', 'node'],
                                 data=[['S_TEST', 'S1VL1', 1]]))
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['S1VL1_BBS', 'S1VL2_BBS1', 'S1VL2_BBS2', 'S2VL1_BBS', 'S3VL1_BBS',
                                                  'S4VL1_BBS', 'S_TEST']),
                            columns=['name', 'fictitious', 'v', 'angle', 'voltage_level_id', 'connected'],
                            data=[['S1VL1_BBS', False, 224.6139, 2.2822, 'S1VL1', True],
                                  ['S1VL2_BBS1', False, 400.0000, 0.0000, 'S1VL2', True],
                                  ['S1VL2_BBS2', False, 400.0000, 0.0000, 'S1VL2', True],
                                  ['S2VL1_BBS', False, 408.8470, 0.7347, 'S2VL1', True],
                                  ['S3VL1_BBS', False, 400.0000, 0.0000, 'S3VL1', True],
                                  ['S4VL1_BBS', False, 400.0000, -1.1259, 'S4VL1', True],
                                  ['S_TEST', False, NaN, NaN, 'S1VL1', True]])
    pd.testing.assert_frame_equal(expected, n.get_busbar_sections(), check_dtype=False)
