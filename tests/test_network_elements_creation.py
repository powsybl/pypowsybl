#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import io

import numpy as np
import pandas as pd
import pytest

import pypowsybl
import pypowsybl.network
import pypowsybl.network as pn
import util
from util import dataframe_from_string
import pathlib
from numpy import NaN
from pypowsybl import PyPowsyblError


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


def test_substation_kwargs():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_substations(id='S3', country='DE')
    s3 = n.get_substations().loc['S3']
    assert s3.country == 'DE'


def test_substation_tso():
    # accepting lower case for backward compat
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_substations(id='S3', tso='TERNA')
    s3 = n.get_substations().loc['S3']
    assert s3.TSO == 'TERNA'

    n.create_substations(id='S4', TSO='TERNA')
    s4 = n.get_substations().loc['S4']
    assert s4.TSO == 'TERNA'


def test_substation_exceptions():
    n = pn.create_eurostag_tutorial_example1_network()
    with pytest.raises(ValueError) as exc:
        n.create_substations(country='FR')
    assert exc.match('No data provided for index: id')

    with pytest.raises(ValueError) as exc:
        n.create_substations(id='test', country='FR', invalid=2)
    assert exc.match('No column named invalid')

    with pytest.raises(PyPowsyblError) as exc:
        n.create_substations(id='test', country='ABC')
    assert exc.match('No enum constant com.powsybl.iidm.network.Country.ABC')

    with pytest.raises(PyPowsyblError) as exc:
        n.create_substations(id='GEN', country=2)
    assert exc.match('Data of column "country" has the wrong type, expected string')
    with pytest.raises(PyPowsyblError) as exc:
        n.create_generators(id='GEN', max_p='2')
    assert exc.match('Data of column "max_p" has the wrong type, expected float')
    with pytest.raises(PyPowsyblError) as exc:
        n.create_generators(id='GEN', voltage_regulator_on=31.3)
    assert exc.match('Data of column "voltage_regulator_on" has the wrong type, expected bool')


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


def test_generators_kwargs():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_generators(id='GEN3', max_p=200, min_p=50, voltage_level_id='VLHV1', bus_id='NHV1',
                        voltage_regulator_on=True, target_p=100, target_q=0, target_v=400)
    gen3 = n.get_generators().loc['GEN3']
    assert gen3.max_p == 200.0
    assert gen3.min_p == 50.0
    assert gen3.target_p == 100.0
    assert gen3.target_q == 0
    assert gen3.target_v == 400
    assert gen3.voltage_regulator_on
    assert gen3.voltage_level_id == 'VLHV1'
    assert gen3.bus_id == 'VLHV1_0'


def test_bus_creation():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_buses(pd.DataFrame(index=['BUS_TEST'],
                                columns=['voltage_level_id'],
                                data=[['VLHV2']]))
    n.create_lines(pd.DataFrame(index=['NHV1_NHV2_3'],
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
        columns=['id', 'voltage_level_id', 'bus_id', 'max_p', 'min_p', 'target_p', 'target_q'],
        data=[('BAT3', 'VLBAT', 'NBAT', 100, 10, 90, 20)],
        index='id')
    n.create_batteries(df)
    bat3 = n.get_batteries().loc['BAT3']
    assert bat3.voltage_level_id == 'VLBAT'
    assert bat3.max_p == 100
    assert bat3.min_p == 10
    assert bat3.target_p == 90
    assert bat3.target_q == 20


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


def test_lcc_creation():
    n = pn.create_four_substations_node_breaker_network()
    n.create_lcc_converter_stations(id='LCC', voltage_level_id='S3VL1', node=1, loss_factor=0.1, power_factor=0.2)
    lcc = n.get_lcc_converter_stations().loc['LCC']
    assert lcc.voltage_level_id == 'S3VL1'
    assert lcc.bus_id == 'S3VL1_0'
    assert lcc.loss_factor == pytest.approx(0.1, abs=1e-6)
    assert lcc.power_factor == pytest.approx(0.2, abs=1e-6)


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
    n.create_switches(pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'kind', 'node1', 'node2', 'open', 'retained',
                 'voltage_level_id'],
        data=[['TEST_BREAKER', '', 'BREAKER', 1, 2, True, True, 'VLGEN'],
              ['TEST_DISCONNECTOR', '', 'DISCONNECTOR', 3, 4, True, True, 'VLGEN']]))
    switches = n.get_switches()
    breaker = switches.loc['TEST_BREAKER']
    disconnector = switches.loc['TEST_DISCONNECTOR']
    assert breaker['kind'] == 'BREAKER'
    assert breaker['open'] == True
    assert breaker['voltage_level_id'] == 'VLGEN'
    assert disconnector['kind'] == 'DISCONNECTOR'
    assert disconnector['open'] == True
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
    rtc_df = dataframe_from_string("""
id         target_deadband  target_v  on_load  low_tap  tap  regulating  regulated_side
NGEN_NHV1                2       200    False        0    1        True             ONE
""")

    steps_df = dataframe_from_string("""
id         b  g  r  x  rho
NGEN_NHV1  2  2  1  1  0.5
NGEN_NHV1  2  2  1  1  0.5
""")

    n.create_ratio_tap_changers(rtc_df, steps_df)

    rtc = n.get_ratio_tap_changers(all_attributes=True).loc['NGEN_NHV1']
    assert rtc.target_deadband == 2
    assert rtc.target_v == 200
    assert not rtc.on_load
    assert rtc.low_tap == 0
    assert rtc.tap == 1
    assert rtc.regulating
    assert rtc.regulated_side == 'ONE'
    steps = n.get_ratio_tap_changer_steps().loc['NGEN_NHV1']
    step1, step2 = (steps.loc[0], steps.loc[1])
    assert step1.b == 2
    assert step1.g == 2
    assert step1.r == 1
    assert step1.x == 1
    assert step1.rho == 0.5


def test_phase_tap_changers_creation():
    n = pn.create_four_substations_node_breaker_network()
    n.create_2_windings_transformers(dataframe_from_string("""
id          r   x  g    b  rated_u1  rated_u2 voltage_level1_id voltage_level2_id  node1  node2                                                                                         
TWT_TEST  0.1  10  1  0.1       400       158             S1VL1             S1VL2      1      2
"""))
    ptc_df = dataframe_from_string("""
id        target_deadband  regulation_mode  target_value  low_tap  tap  regulating  regulated_side                                                  
TWT_TEST                2  CURRENT_LIMITER           300        0    1        True             TWO
""")
    steps_df = dataframe_from_string("""
id        b  g  r  x  rho  alpha                          
TWT_TEST  2  2  1  1  0.5    0.1
TWT_TEST  2  2  1  1  0.5    0.1
""")
    n.create_phase_tap_changers(ptc_df, steps_df)

    ptc = n.get_phase_tap_changers(all_attributes=True).loc['TWT_TEST']
    assert ptc.target_deadband == 2
    assert ptc.regulation_mode == 'CURRENT_LIMITER'
    assert ptc.regulation_value == 300
    assert ptc.regulating
    assert ptc.regulated_side == 'TWO'
    assert ptc.low_tap == 0
    assert ptc.tap == 1
    steps = n.get_phase_tap_changer_steps().loc['TWT_TEST']
    step1, step2 = (steps.loc[0], steps.loc[1])
    assert step1.b == 2
    assert step1.g == 2
    assert step1.r == 1
    assert step1.x == 1
    assert step1.rho == 0.5
    assert step1.alpha == 0.1


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


def test_linear_shunt():
    n = pn.create_four_substations_node_breaker_network()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'voltage_level_id', 'node'],
        data=[('SHUNT_TEST', '', 'LINEAR', 1, 400, 2, 'S1VL2', 2)])
    model_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'g_per_section', 'b_per_section', 'max_section_count'],
        data=[('SHUNT_TEST', 0.14, -0.01, 2)])
    n.create_shunt_compensators(shunt_df, model_df)

    shunt = n.get_shunt_compensators().loc['SHUNT_TEST']
    assert shunt.max_section_count == 2
    assert shunt.section_count == 1
    assert shunt.target_v == 400
    assert shunt.target_deadband == 2
    assert not shunt.voltage_regulation_on
    assert shunt.model_type == 'LINEAR'
    assert shunt.g == 0.14
    assert shunt.b == -0.01

    model = n.get_linear_shunt_compensator_sections().loc['SHUNT_TEST']
    assert model.g_per_section == 0.14
    assert model.b_per_section == -0.01
    assert model.max_section_count == 2


def test_non_linear_shunt():
    n = pn.create_four_substations_node_breaker_network()
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'voltage_level_id', 'node'],
        data=[('SHUNT1', '', 'NON_LINEAR', 1, 400, 2, 'S1VL2', 2),
              ('SHUNT2', '', 'NON_LINEAR', 1, 400, 2, 'S1VL2', 10)])
    model_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'g', 'b'],
        data=[('SHUNT1', 1, 2),
              ('SHUNT1', 3, 4),
              ('SHUNT2', 5, 6),
              ('SHUNT2', 7, 8)])
    n.create_shunt_compensators(shunt_df, non_linear_model_df=model_df)

    shunt = n.get_shunt_compensators().loc['SHUNT1']
    assert shunt.max_section_count == 2
    assert shunt.section_count == 1
    assert shunt.target_v == 400
    assert shunt.target_deadband == 2
    assert not shunt.voltage_regulation_on
    assert shunt.model_type == 'NON_LINEAR'
    assert shunt.g == 1
    assert shunt.b == 2

    model1 = n.get_non_linear_shunt_compensator_sections().loc['SHUNT1']
    section1 = model1.loc[0]
    section2 = model1.loc[1]
    assert section1.g == 1
    assert section1.b == 2
    assert section2.g == 3
    assert section2.b == 4

    model2 = n.get_non_linear_shunt_compensator_sections().loc['SHUNT2']
    section1 = model2.loc[0]
    section2 = model2.loc[1]
    assert section1.g == 5
    assert section1.b == 6
    assert section2.g == 7
    assert section2.b == 8


def test_busbar_sections():
    n = pn.create_four_substations_node_breaker_network()
    n.create_busbar_sections(pd.DataFrame(index=pd.Series(name='id',
                                                          data=['S_TEST']),
                                          columns=['name', 'voltage_level_id', 'node'],
                                          data=[['S_TEST', 'S1VL1', 1]]))
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['S1VL1_BBS', 'S1VL2_BBS1', 'S1VL2_BBS2', 'S2VL1_BBS', 'S3VL1_BBS',
                                                  'S4VL1_BBS', 'S_TEST']),
                            columns=['name', 'v', 'angle', 'voltage_level_id', 'bus_id', 'connected', 'fictitious'],
                            data=[['S1VL1_BBS', 224.6139, 2.2822, 'S1VL1', 'S1VL1_0', True, False],
                                  ['S1VL2_BBS1', 400.0000, 0.0000, 'S1VL2', 'S1VL2_0', True, False],
                                  ['S1VL2_BBS2', 400.0000, 0.0000, 'S1VL2', 'S1VL2_0', True, False],
                                  ['S2VL1_BBS', 408.8470, 0.7347, 'S2VL1', 'S2VL1_0', True, False],
                                  ['S3VL1_BBS', 400.0000, 0.0000, 'S3VL1', 'S3VL1_0', True, False],
                                  ['S4VL1_BBS', 400.0000, -1.1259, 'S4VL1', 'S4VL1_0', True, False],
                                  ['S_TEST', NaN, NaN, 'S1VL1', 'S1VL1_0', True, False]])
    pd.testing.assert_frame_equal(expected, n.get_busbar_sections(all_attributes=True), check_dtype=False)


def test_hvdc_creation():
    n = pn.create_four_substations_node_breaker_network()
    df = pd.DataFrame.from_records(index='id', data=[{
        'id': 'VSC_TEST',
        'converter_station1_id': 'VSC1',
        'converter_station2_id': 'VSC2',
        'r': 0.1,
        'nominal_v': 320,
        'target_p': 100,
        'max_p': 200,
        'converters_mode': 'SIDE_1_RECTIFIER_SIDE_2_INVERTER'
    }])
    n.create_hvdc_lines(df)
    hvdc = n.get_hvdc_lines().loc['VSC_TEST']
    assert hvdc.converter_station1_id == 'VSC1'
    assert hvdc.converter_station2_id == 'VSC2'
    assert hvdc.r == 0.1
    assert hvdc.nominal_v == 320
    assert hvdc.target_p == 100
    assert hvdc.max_p == 200
    assert hvdc.converters_mode == 'SIDE_1_RECTIFIER_SIDE_2_INVERTER'
    # test inline arguments
    n = pn.create_four_substations_node_breaker_network()
    n.create_hvdc_lines(id='VSC_TEST', converter_station1_id='VSC1',
                        converter_station2_id='VSC2', r=0.1, nominal_v=320, target_p=100,
                        max_p=200, converters_mode='SIDE_1_RECTIFIER_SIDE_2_INVERTER')
    hvdc = n.get_hvdc_lines().loc['VSC_TEST']
    assert hvdc.converter_station1_id == 'VSC1'
    assert hvdc.converter_station2_id == 'VSC2'
    assert hvdc.r == 0.1
    assert hvdc.nominal_v == 320
    assert hvdc.target_p == 100
    assert hvdc.max_p == 200
    assert hvdc.converters_mode == 'SIDE_1_RECTIFIER_SIDE_2_INVERTER'


def test_create_network_and_run_loadflow():
    n = pn.create_empty()
    stations = pd.DataFrame.from_records(index='id', data=[
        {'id': 'S1', 'country': 'BE'},
        {'id': 'S2', 'country': 'DE'}
    ])
    n.create_substations(stations)

    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'BUS_BREAKER', 'nominal_v': 400},
        {'substation_id': 'S2', 'id': 'VL2', 'topology_kind': 'BUS_BREAKER', 'nominal_v': 400},
    ])
    n.create_voltage_levels(voltage_levels)

    buses = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'B1'},
        {'voltage_level_id': 'VL2', 'id': 'B2'},
    ])
    n.create_buses(buses)

    lines = pd.DataFrame.from_records(index='id', data=[
        {'id': 'LINE', 'voltage_level1_id': 'VL1', 'voltage_level2_id': 'VL2', 'bus1_id': 'B1', 'bus2_id': 'B2',
         'r': 0.1, 'x': 1.0, 'g1': 0, 'b1': 1e-6, 'g2': 0, 'b2': 1e-6}
    ])
    n.create_lines(lines)

    loads = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL2', 'id': 'LOAD', 'bus_id': 'B2', 'p0': 100, 'q0': 10}
    ])
    n.create_loads(loads)

    generators = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'GEN', 'bus_id': 'B1', 'target_p': 100, 'min_p': 0, 'max_p': 200,
         'target_v': 400, 'voltage_regulator_on': True, 'regulating_element_id': 'LOAD'}
    ])
    n.create_generators(generators)

    import pypowsybl.loadflow as lf
    lf.run_ac(n)

    line = n.get_lines().loc['LINE']
    assert line.p2 == pytest.approx(-100, abs=1e-2)
    assert line.q2 == pytest.approx(-10, abs=1e-2)
    assert line.p1 == pytest.approx(100, abs=1e-1)
    assert line.q1 == pytest.approx(9.7, abs=1e-1)

    generator = n.get_generators().loc['GEN']
    assert generator.regulated_element_id == 'LOAD'


def test_create_node_breaker_network_and_run_loadflow():
    n = pn.create_empty()
    stations = pd.DataFrame.from_records(index='id', data=[
        {'id': 'S1', 'country': 'BE'},
        {'id': 'S2', 'country': 'DE'}
    ])
    n.create_substations(stations)

    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 400},
        {'substation_id': 'S2', 'id': 'VL2', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 400},
    ])
    n.create_voltage_levels(voltage_levels)

    busbars = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'BB1', 'node': 0},
        {'voltage_level_id': 'VL2', 'id': 'BB2', 'node': 0},
    ])
    n.create_busbar_sections(busbars)

    n.create_switches(pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'DISC-BB1', 'kind': 'DISCONNECTOR', 'node1': 0, 'node2': 1},
        {'voltage_level_id': 'VL1', 'id': 'BREAKER-BB1-LINE', 'kind': 'BREAKER', 'node1': 1, 'node2': 2},
        {'voltage_level_id': 'VL1', 'id': 'BREAKER-BB1-GEN', 'kind': 'BREAKER', 'node1': 0, 'node2': 3},
        {'voltage_level_id': 'VL2', 'id': 'DISC-BB2', 'kind': 'DISCONNECTOR', 'node1': 0, 'node2': 1},
        {'voltage_level_id': 'VL2', 'id': 'BREAKER-BB2-LINE', 'kind': 'BREAKER', 'node1': 1, 'node2': 2},
        {'voltage_level_id': 'VL2', 'id': 'BREAKER-BB2-LOAD', 'kind': 'BREAKER', 'node1': 0, 'node2': 3},
    ]))

    lines = pd.DataFrame.from_records(index='id', data=[
        {'id': 'LINE', 'voltage_level1_id': 'VL1', 'voltage_level2_id': 'VL2', 'node1': 2, 'node2': 2,
         'r': 0.1, 'x': 1.0, 'g1': 0, 'b1': 1e-6, 'g2': 0, 'b2': 1e-6}
    ])
    n.create_lines(lines)

    loads = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL2', 'id': 'LOAD', 'node': 3, 'p0': 100, 'q0': 10}
    ])
    n.create_loads(loads)

    generators = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'GEN', 'node': 3, 'target_p': 100, 'min_p': 0, 'max_p': 200,
         'target_v': 400, 'voltage_regulator_on': True, 'regulating_element_id': 'LOAD'}
    ])
    n.create_generators(generators)

    import pypowsybl.loadflow as lf
    lf.run_ac(n)

    line = n.get_lines().loc['LINE']
    assert line.p2 == pytest.approx(-100, abs=1e-2)
    assert line.q2 == pytest.approx(-10, abs=1e-2)
    assert line.p1 == pytest.approx(100, abs=1e-1)
    assert line.q1 == pytest.approx(9.7, abs=1e-1)

    generator = n.get_generators().loc['GEN']
    assert generator.regulated_element_id == 'LOAD'


def test_create_limits():
    net = pn.create_eurostag_tutorial_example1_network()
    net.create_operational_limits(pd.DataFrame.from_records(index='element_id', data=[
        {'element_id': 'NHV1_NHV2_1', 'name': 'permanent_limit', 'element_type': 'LINE', 'side': 'ONE',
         'type': 'APPARENT_POWER', 'value': 600,
         'acceptable_duration': np.Inf, 'is_fictitious': False},
        {'element_id': 'NHV1_NHV2_1', 'name': '1\'', 'element_type': 'LINE', 'side': 'ONE',
         'type': 'APPARENT_POWER', 'value': 1000,
         'acceptable_duration': 60, 'is_fictitious': False},
        {'element_id': 'NHV1_NHV2_1', 'name': 'permanent_limit', 'element_type': 'LINE', 'side': 'ONE',
         'type': 'ACTIVE_POWER', 'value': 400,
         'acceptable_duration': np.Inf, 'is_fictitious': False},
        {'element_id': 'NHV1_NHV2_1', 'name': '1\'', 'element_type': 'LINE', 'side': 'ONE',
         'type': 'ACTIVE_POWER', 'value': 700,
         'acceptable_duration': 60, 'is_fictitious': False}
    ]))
    expected = pd.DataFrame.from_records(
        index='element_id',
        columns=['element_id', 'element_type', 'side', 'name', 'type', 'value', 'acceptable_duration', 'fictitious'],
        data=[['NHV1_NHV2_1', 'LINE', 'ONE', 'permanent_limit', 'CURRENT', 500, -1, False],
              ['NHV1_NHV2_1', 'LINE', 'TWO', 'permanent_limit', 'CURRENT', 1100, -1, False],
              ['NHV1_NHV2_1', 'LINE', 'ONE', 'permanent_limit', 'ACTIVE_POWER', 400, -1, False],
              ['NHV1_NHV2_1', 'LINE', 'ONE', 'permanent_limit', 'APPARENT_POWER', 600, -1, False]])
    limits = net.get_operational_limits(all_attributes=True).loc['NHV1_NHV2_1']
    permanent_limits = limits[limits['name'] == 'permanent_limit']
    pd.testing.assert_frame_equal(expected, permanent_limits, check_dtype=False)

    expected = pd.DataFrame.from_records(
        index='element_id',
        columns=['element_id', 'element_type', 'side', 'name', 'type', 'value', 'acceptable_duration', 'fictitious'],
        data=[['NHV1_NHV2_1', 'LINE', 'TWO', '1\'', 'CURRENT', 1500, 60, False],
              ['NHV1_NHV2_1', 'LINE', 'ONE', '1\'', 'ACTIVE_POWER', 700, 60, False],
              ['NHV1_NHV2_1', 'LINE', 'ONE', '1\'', 'APPARENT_POWER', 1000, 60, False]])
    one_minute_limits = limits[limits['name'] == '1\'']
    pd.testing.assert_frame_equal(expected, one_minute_limits, check_dtype=False)


def test_create_minmax_reactive_limits():
    network = pn.create_four_substations_node_breaker_network()
    network.create_minmax_reactive_limits(pd.DataFrame.from_records(index='id', data=[
        {'id': 'GH1', 'min_q': -201.0, 'max_q': 201.0},
        {'id': 'GH2', 'min_q': -205.0, 'max_q': 205.0},
        {'id': 'VSC1', 'min_q': -355.0, 'max_q': 405.0},
        {'id': 'VSC2', 'min_q': -405.0, 'max_q': 505.0},
    ]))
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'min_q', 'max_q'],
        data=[['GH1', -201.0, 201.0],
              ['GH2', -205.0, 205.0]])
    pd.testing.assert_frame_equal(expected, network.get_generators(id=['GH1', 'GH2'], attributes=['min_q', 'max_q']),
                                  check_dtype=False)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'min_q', 'max_q'],
        data=[['VSC1', -355.0, 405.0],
              ['VSC2', -405.0, 505.0]])
    pd.testing.assert_frame_equal(expected, network.get_vsc_converter_stations(id=['VSC1', 'VSC2'],
                                                                               attributes=['min_q', 'max_q']),
                                  check_dtype=False)
    network = util.create_battery_network()
    network.create_minmax_reactive_limits(pd.DataFrame.from_records(index='id', data=[
        {'id': 'BAT', 'min_q': -201.0, 'max_q': 201.0}
    ]))
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'min_q', 'max_q'],
        data=[['BAT', -201.0, 201.0]])
    pd.testing.assert_frame_equal(expected, network.get_batteries(id='BAT', attributes=['min_q', 'max_q']),
                                  check_dtype=False)


def test_create_curve_reactive_limits():
    # Generators
    network = pn.create_eurostag_tutorial_example1_network()
    network.create_curve_reactive_limits(dataframe_from_string("""
id  p    min_q    max_q
GEN 0    -556.8   557.4
GEN 200  -553.514 536.4
    """))
    reactive_capability_curve_points = network.get_reactive_capability_curve_points()
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GEN', 0)],
                                   pd.Series(data={'p': 0, 'min_q': -556.8, 'max_q': 557.4},
                                             name=('GEN', 0)), check_dtype=False)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GEN', 1)],
                                   pd.Series(data={'p': 200, 'min_q': -553.514, 'max_q': 536.4},
                                             name=('GEN', 1)), check_dtype=False)

    # Batteries
    network = util.create_battery_network()
    network.create_curve_reactive_limits(dataframe_from_string("""
id  p    min_q    max_q
BAT 50    -50     100
BAT 60  -100      50
    """))
    expected = dataframe_from_string("""
num  p  min_q max_q
0   50    -50   100
1   60   -100    50
    """, index='num')
    battery_curve_points = network.get_reactive_capability_curve_points().loc['BAT']
    assert battery_curve_points.loc[0]['p'] == 50
    assert battery_curve_points.loc[0]['min_q'] == -50
    assert battery_curve_points.loc[0]['max_q'] == 100
    assert battery_curve_points.loc[1]['p'] == 60
    assert battery_curve_points.loc[1]['min_q'] == -100
    assert battery_curve_points.loc[1]['max_q'] == 50

    # VSCs
    network = pn.create_four_substations_node_breaker_network()
    network.create_curve_reactive_limits(dataframe_from_string("""
  id   p  min_q    max_q
VSC1  50    -50      100
VSC1  60   -100       50
"""))
    vsc_curve_points = network.get_reactive_capability_curve_points().loc['VSC1']
    assert vsc_curve_points.loc[0]['p'] == 50
    assert vsc_curve_points.loc[0]['min_q'] == -50
    assert vsc_curve_points.loc[0]['max_q'] == 100
    assert vsc_curve_points.loc[1]['p'] == 60
    assert vsc_curve_points.loc[1]['min_q'] == -100
    assert vsc_curve_points.loc[1]['max_q'] == 50


def test_delete_elements_eurostag():
    net = pypowsybl.network.create_eurostag_tutorial_example1_network()
    net.remove_elements(['GEN', 'GEN2'])
    assert net.get_generators().empty
    net.remove_elements(['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    assert net.get_lines().empty
    net.remove_elements(['NHV2_NLOAD', 'NGEN_NHV1'])
    assert net.get_2_windings_transformers().empty
    net.remove_elements('LOAD')
    assert net.get_loads().empty
    net = pypowsybl.network.create_eurostag_tutorial_example1_network()
    net.remove_elements(['GEN', 'GEN2', 'NHV1_NHV2_1', 'NHV1_NHV2_2', 'NHV2_NLOAD', 'NGEN_NHV1', 'LOAD'])
    assert net.get_generators().empty
    assert net.get_lines().empty
    assert net.get_2_windings_transformers().empty
    assert net.get_loads().empty


def test_delete_elements_four_substations():
    net = pypowsybl.network.create_four_substations_node_breaker_network()
    net.remove_elements(['TWT', 'HVDC1', 'HVDC2', 'S1'])
    assert 'HVDC1' not in net.get_hvdc_lines().index
    assert 'HVDC2' not in net.get_hvdc_lines().index
    assert 'TWT' not in net.get_2_windings_transformers().index
    assert 'S1' not in net.get_substations().index
    assert 'S1VL1' not in net.get_voltage_levels().index
    assert 'S2VL1' in net.get_voltage_levels().index
    with pytest.raises(pypowsybl.PyPowsyblError) as err:
        net.remove_elements('S2VL1')
    assert 'The voltage level \'S2VL1\' cannot be removed because of a remaining LINE' in str(err.value)
    net.remove_elements(['LINE_S2S3', 'S2VL1'])
    assert 'S2VL1' not in net.get_voltage_levels().index


def test_remove_elements_switches():
    net = pypowsybl.network.create_four_substations_node_breaker_network()
    net.remove_elements(['S1VL1_BBS_LD1_DISCONNECTOR', 'S1VL1_LD1_BREAKER', 'TWT', 'HVDC1'])
    # TODO: restore it when bug fixed in powsybl-core
    # net.remove_elements(['S1VL1', 'S1'])
    assert 'S1VL1_BBS_LD1_DISCONNECTOR' not in net.get_switches().index
    assert 'S1VL1_LD1_BREAKER' not in net.get_switches().index
    assert 'HVDC1' not in net.get_hvdc_lines().index
    assert 'TWT' not in net.get_2_windings_transformers().index
    # assert 'S1' not in net.get_substations().index
    # assert 'S1VL1' not in net.get_voltage_levels().index


def test_creating_vl_without_substation():
    net = pypowsybl.network.create_four_substations_node_breaker_network()
    df = pd.DataFrame.from_records(index='id', data=[{
        'id': 'VLTEST',
        'high_voltage_limit': 250,
        'low_voltage_limit': 200,
        'nominal_v': 225,
        'topology_kind': 'BUS_BREAKER'
    }])
    net.create_voltage_levels(df)
    assert 'VLTEST' in net.get_voltage_levels().index
    net.create_voltage_levels(id='VLTEST2', high_voltage_limit=250,
                              low_voltage_limit=200,
                              nominal_v=225,
                              topology_kind='BUS_BREAKER')
    assert 'VLTEST2' in net.get_voltage_levels().index
    net.remove_elements(['VLTEST', 'VLTEST2'])
    assert 'VLTEST2' not in net.get_voltage_levels().index and 'VLTEST' not in net.get_voltage_levels().index
    net.create_voltage_levels(id=['VLTEST', 'VLTEST2'], high_voltage_limit=[250, 250],
                              low_voltage_limit=[200, 200],
                              nominal_v=[225, 225],
                              topology_kind=['BUS_BREAKER', 'BUS_BREAKER'])
    assert 'VLTEST2' in net.get_voltage_levels().index and 'VLTEST' in net.get_voltage_levels().index


def check_unknown_voltage_level_error_message(fn):
    with pytest.raises(PyPowsyblError) as exc:
        fn(voltage_level_id='UNKNOWN', id='S')
    assert exc.match('Voltage level UNKNOWN does not exist')


def test_error_messages():
    network = pn.create_eurostag_tutorial_example1_network()
    with pytest.raises(PyPowsyblError) as exc:
        network.create_voltage_levels(id='VL', substation_id='UNKNOWN', nominal_v=400)
    assert exc.match('Substation UNKNOWN does not exist')

    check_unknown_voltage_level_error_message(network.create_loads)
    check_unknown_voltage_level_error_message(network.create_generators)
    check_unknown_voltage_level_error_message(network.create_switches)
    check_unknown_voltage_level_error_message(network.create_static_var_compensators)
    check_unknown_voltage_level_error_message(network.create_dangling_lines)
    check_unknown_voltage_level_error_message(network.create_lcc_converter_stations)
    check_unknown_voltage_level_error_message(network.create_vsc_converter_stations)


def test_tie_line_creation():
    network = pn.create_empty()
    network.create_substations(id=['S1', 'S2'], tso=['TERNA', 'RTE'])
    network.create_voltage_levels(id=['VLTEST', 'VLTEST2'], high_voltage_limit=[250, 250],
                                  low_voltage_limit=[200, 200],
                                  nominal_v=[225, 225],
                                  topology_kind=['BUS_BREAKER', 'BUS_BREAKER'])
    network.create_buses(id=['BUS_TEST', 'BUS_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'])
    network.create_dangling_lines(id=['DL_TEST', 'DL_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'],
                                  bus_id=['BUS_TEST', 'BUS_TEST2'],
                                  p0=[100, 100], q0=[101, 101], r=[2, 2], x=[2, 2], g=[1, 1], b=[1, 1],
                                  ucte_xnode_code=['XNODE', 'XNODE'])
    df = pd.DataFrame.from_records(
        columns=['id', 'dangling_line1_id', 'dangling_line2_id'],
        data=[('TIE_LINE_TEST', 'DL_TEST', 'DL_TEST2')],
        index='id')
    network.create_tie_lines(df)
    assert 'TIE_LINE_TEST' in network.get_tie_lines().index


def test_tie_line_creation_fail_if_xnodes_are_different():
    network = pn.create_empty()
    network.create_substations(id=['S1', 'S2'], tso=['TERNA', 'RTE'])
    network.create_voltage_levels(id=['VLTEST', 'VLTEST2'], high_voltage_limit=[250, 250],
                                  low_voltage_limit=[200, 200],
                                  nominal_v=[225, 225],
                                  topology_kind=['BUS_BREAKER', 'BUS_BREAKER'])
    network.create_buses(id=['BUS_TEST', 'BUS_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'])
    network.create_dangling_lines(id=['DL_TEST', 'DL_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'],
                                  bus_id=['BUS_TEST', 'BUS_TEST2'],
                                  p0=[100, 100], q0=[101, 101], r=[2, 2], x=[2, 2], g=[1, 1], b=[1, 1],
                                  pairing_key=['XNODE1', 'XNODE'])
    df = pd.DataFrame.from_records(
        columns=['id', 'dangling_line1_id', 'dangling_line2_id'],
        data=[('TIE_LINE_TEST', 'DL_TEST', 'DL_TEST2')],
        index='id')
    with pytest.raises(PyPowsyblError) as exc:
        network.create_tie_lines(df)
    assert exc.match("AC tie Line 'TIE_LINE_TEST': pairingKey is not consistent")


def test_tie_line_kwargs():
    network = pn.create_empty()
    network.create_substations(id=['S1', 'S2'], tso=['TERNA', 'RTE'])
    network.create_voltage_levels(id=['VLTEST', 'VLTEST2'], high_voltage_limit=[250, 250],
                                  low_voltage_limit=[200, 200],
                                  nominal_v=[225, 225],
                                  topology_kind=['BUS_BREAKER', 'BUS_BREAKER'])
    network.create_buses(id=['BUS_TEST', 'BUS_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'])
    network.create_dangling_lines(id=['DL_TEST', 'DL_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'],
                                  bus_id=['BUS_TEST', 'BUS_TEST2'],
                                  p0=[100, 100], q0=[101, 101], r=[2, 2], x=[2, 2], g=[1, 1], b=[1, 1])
    network.create_tie_lines(id='TIE_LINE_TEST', dangling_line1_id='DL_TEST', dangling_line2_id='DL_TEST2')
    assert 'TIE_LINE_TEST' in network.get_tie_lines().index

    network.update_tie_lines(id='TIE_LINE_TEST', fictitious=True)
    assert network.get_tie_lines(True).loc['TIE_LINE_TEST'].fictitious

    network.remove_elements('TIE_LINE_TEST')
    assert network.get_tie_lines().empty


def test_deprecated_ucte_xnode_code_kwargs():
    network = pn.create_empty()
    network.create_substations(id=['S1', 'S2'], tso=['TERNA', 'RTE'])
    network.create_voltage_levels(id=['VLTEST', 'VLTEST2'], high_voltage_limit=[250, 250],
                                  low_voltage_limit=[200, 200],
                                  nominal_v=[225, 225],
                                  topology_kind=['BUS_BREAKER', 'BUS_BREAKER'])
    network.create_buses(id=['BUS_TEST', 'BUS_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'])
    with pytest.deprecated_call():
        network.create_dangling_lines(id=['DL_TEST', 'DL_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'],
                                      bus_id=['BUS_TEST', 'BUS_TEST2'],
                                      p0=[100, 100], q0=[101, 101], r=[2, 2], x=[2, 2], g=[1, 1], b=[1, 1],
                                      ucte_xnode_code=['XNODE1', 'XNODE'])
    assert 'DL_TEST' in network.get_dangling_lines().index
    assert 'DL_TEST2' in network.get_dangling_lines().index
    assert 'ucte_xnode_code' in network.get_dangling_lines().columns


def test_deprecated_ucte_xnode_code_dataframe():
    network = pn.create_empty()
    network.create_substations(id=['S1', 'S2'], tso=['TERNA', 'RTE'])
    network.create_voltage_levels(id=['VLTEST', 'VLTEST2'], high_voltage_limit=[250, 250],
                                  low_voltage_limit=[200, 200],
                                  nominal_v=[225, 225],
                                  topology_kind=['BUS_BREAKER', 'BUS_BREAKER'])
    network.create_buses(id=['BUS_TEST', 'BUS_TEST2'], voltage_level_id=['VLTEST', 'VLTEST2'])
    with pytest.deprecated_call():
        network.create_dangling_lines(pd.DataFrame.from_records(
            columns=['id', 'voltage_level_id', 'bus_id', 'p0', 'q0', 'r', 'x', 'g', 'b', 'ucte_xnode_code'],
            data=[('DL_TEST', 'VLTEST', 'BUS_TEST', 100, 101, 2, 2, 1, 1, 'XNODE1'),
                  ('DL_TEST2', 'VLTEST2', 'BUS_TEST2', 100, 101, 2, 2, 1, 1, 'XNODE')],
            index='id'))
    assert 'DL_TEST' in network.get_dangling_lines().index
    assert 'DL_TEST2' in network.get_dangling_lines().index
    assert 'ucte_xnode_code' in network.get_dangling_lines().columns


def test_3_windings_transformers_creation():
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
    n.create_buses(pd.DataFrame(index=['BUS_TEST'],
                                columns=['voltage_level_id'],
                                data=[['VLTEST']]))
    n.create_3_windings_transformers(id='TWT_TEST', rated_u0=225, voltage_level1_id='VLHV1', bus1_id='NHV1',
                                     voltage_level2_id='VLTEST', bus2_id='BUS_TEST',
                                     voltage_level3_id='VLGEN', bus3_id='NGEN',
                                     b1=1e-6, g1=1e-6, r1=0.5, x1=10, rated_u1=380, rated_s1=100,
                                     b2=1e-6, g2=1e-6, r2=0.5, x2=10, rated_u2=225, rated_s2=100,
                                     b3=1e-6, g3=1e-6, r3=0.5, x3=10, rated_u3=24, rated_s3=100)

    twt = n.get_3_windings_transformers().loc['TWT_TEST']
