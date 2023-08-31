#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

import pypowsybl as pp
import pandas as pd
from numpy import NaN
import util
from pypowsybl.network import per_unit_view


def test_bus_per_unit():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)
    buses = n.get_buses()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                            columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                                     'voltage_level_id'],
                            data=[['', 1.02, 0.04059612739187691, 0, 0, 'VLGEN'],
                                  ['', 1.06, 0.0, 0, 0, 'VLHV1'],
                                  ['', 1.03, -0.06119749366730875, 0, 0, 'VLHV2'],
                                  ['', 0.98, -0.16780443393265765, 0, 0, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected, buses, check_dtype=False, atol=1e-2)
    n.update_buses(id='VLGEN_0', v_mag=1, v_angle=0.1)
    buses = n.get_buses()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                            columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                                     'voltage_level_id'],
                            data=[['', 1, 0.1, 0, 0, 'VLGEN'],
                                  ['', 1.06, 0, 0, 0, 'VLHV1'],
                                  ['', 1.03, -0.06119749366730875, 0, 0, 'VLHV2'],
                                  ['', 0.98, -0.16780443393265765, 0, 0, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected, buses, check_dtype=False, atol=1e-2)


def test_generator_per_unit():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'energy_source', 'target_p', 'min_p', 'max_p', 'min_q', 'max_q', 'rated_s', 'reactive_limits_kind',
                 'target_v',
                 'target_q', 'voltage_regulator_on', 'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id',
                 'bus_id', 'connected'],
        data=[['GEN', '', 'OTHER', 6.07, -100, 49.99, -100, 100, None, 'MIN_MAX', 1.02, 3.01, True, '', -3.03, -1.12641,
               3.16461,
               'VLGEN', 'VLGEN_0', True],
              ['GEN2', '', 'OTHER', 6.07, -100, 49.99, -1.79769e+306, 1.79769e+306, None, 'MIN_MAX', 1.02, 3.01, True, '',
               -3.03,
               -1.13, 3.16, 'VLGEN', 'VLGEN_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_generators(), check_dtype=False, atol=1e-2)
    generators2 = pd.DataFrame(data=[[6.080, 3.02, 1.1, False, False]],
                               columns=['target_p', 'target_q', 'target_v', 'voltage_regulator_on', 'connected'],
                               index=['GEN'])
    n.update_generators(generators2)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'energy_source', 'target_p', 'min_p', 'max_p', 'min_q', 'max_q', 'rated_s', 'reactive_limits_kind',
                 'target_v',
                 'target_q', 'voltage_regulator_on', 'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id',
                 'bus_id', 'connected'],
        data=[['GEN', '', 'OTHER', 6.07, -100, 49.99, -100, 100, None, 'MIN_MAX', 1.1, 3.02, False, '', -3.03, -1.12641, NaN,
               'VLGEN', '', False],
              ['GEN2', '', 'OTHER', 6.07, -100, 49.99, -1.79769e+306, 1.79769e+306, None, 'MIN_MAX', 1.02, 3.01, True, '',
               -3.03,
               -1.13, 3.16, 'VLGEN', 'VLGEN_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_generators(), check_dtype=False, atol=1e-2)


def test_loads_per_unit():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LOAD']),
                            columns=['name', 'type', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id', 'bus_id',
                                     'connected'],
                            data=[['', 'UNDEFINED', 6, 2, 6, 2, 6.43, 'VLLOAD', 'VLLOAD_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_loads(), check_dtype=False, atol=1e-2)
    n.update_loads(pd.DataFrame(data=[[5, 3, False]], columns=['p0', 'q0', 'connected'], index=['LOAD']))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LOAD']),
                            columns=['name', 'type', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id', 'bus_id',
                                     'connected'],
                            data=[['', 'UNDEFINED', 5, 3, 6, 2, NaN, 'VLLOAD', '', False]])
    pd.testing.assert_frame_equal(expected, n.get_loads(), check_dtype=False, atol=1e-2)


def test_busbar_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['S1VL1_BBS', 'S1VL2_BBS1', 'S1VL2_BBS2', 'S2VL1_BBS', 'S3VL1_BBS',
                                                  'S4VL1_BBS']),
                            columns=['name', 'v', 'angle', 'voltage_level_id', 'bus_id', 'connected'],
                            data=[['S1VL1_BBS', 1.00, 0.04193194938608592, 'S1VL1', 'S1VL1_0', True],
                                  ['S1VL2_BBS1', 1, 0, 'S1VL2', 'S1VL2_0', True],
                                  ['S1VL2_BBS2', 1, 0, 'S1VL2', 'S1VL2_0', True],
                                  ['S2VL1_BBS', 1.02, 0.01282301263193765, 'S2VL1', 'S2VL1_0', True],
                                  ['S3VL1_BBS', 1, 0, 'S3VL1', 'S3VL1_0', True],
                                  ['S4VL1_BBS', 1, -0.019651423679619508, 'S4VL1', 'S4VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_busbar_sections(), check_dtype=False, atol=1e-2)


def test_hvdc_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'converters_mode', 'target_p', 'max_p', 'nominal_v', 'r',
                 'converter_station1_id', 'converter_station2_id', 'connected1', 'connected2'],
        data=[('HVDC1', 'HVDC1', 'SIDE_1_RECTIFIER_SIDE_2_INVERTER', 0.1, 3, 400, 0, 'VSC1', 'VSC2', True, True),
              ('HVDC2', 'HVDC2', 'SIDE_1_RECTIFIER_SIDE_2_INVERTER', 0.8, 3, 400, 0, 'LCC1', 'LCC2', True, True)])

    pd.testing.assert_frame_equal(expected, n.get_hvdc_lines(), check_dtype=False, atol=1e-2)
    n.update_hvdc_lines(id='HVDC1', target_p=[0.11])
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'converters_mode', 'target_p', 'max_p', 'nominal_v', 'r',
                 'converter_station1_id', 'converter_station2_id', 'connected1', 'connected2'],
        data=[('HVDC1', 'HVDC1', 'SIDE_1_RECTIFIER_SIDE_2_INVERTER', 0.11, 3, 400, 0, 'VSC1', 'VSC2', True, True),
              ('HVDC2', 'HVDC2', 'SIDE_1_RECTIFIER_SIDE_2_INVERTER', 0.8, 3, 400, 0, 'LCC1', 'LCC2', True, True)])
    pd.testing.assert_frame_equal(expected, n.get_hvdc_lines(), check_dtype=False, atol=1e-2)


def test_lines_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    lines = n.get_lines()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
                            columns=['name', 'r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                                     'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1',
                                     'connected2'],
                            data=[['', 0, 0.0119, 0, 0, 0, 0, 1.0989, 1.9002, 2.1476, -1.0989, -1.8451,
                                   2.1476, 'S2VL1', 'S3VL1', 'S2VL1_0', 'S3VL1_0', True, True],
                                  ['', 0, 0.0082, 0, 0, 0, 0, 2.4, 0.0218, 2.4001, -2.4, 0.0254, 2.4001,
                                   'S3VL1', 'S4VL1', 'S3VL1_0', 'S4VL1_0', True, True]])
    pd.testing.assert_frame_equal(expected, lines, check_dtype=False, atol=10 ** -4)
    n.update_lines(pd.DataFrame(data=[[1, 9, 0.1, 0.01, 0.1, 0.1, 1.2, 2, -1.2, -2]],
                                columns=['r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'p2', 'q2'],
                                index=['LINE_S2S3']))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
                            columns=['name', 'r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                                     'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1',
                                     'connected2'],
                            data=[['', 1, 9, 0.1, 0.01, 0.1, 0.1, 1.2, 2, 2.2819, -1.2, -2,
                                   2.3324, 'S2VL1', 'S3VL1', 'S2VL1_0', 'S3VL1_0', True, True],
                                  ['', 0, 0.0082, 0, 0, 0, 0, 2.40, 0.0218, 2.4001, -2.4, 0.0254, 2.4001,
                                   'S3VL1', 'S4VL1', 'S3VL1_0', 'S4VL1_0', True, True]])
    pd.testing.assert_frame_equal(expected, n.get_lines(), check_dtype=False, atol=10 ** -4)


def test_two_windings_transformers_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['TWT']),
                            columns=['name', 'r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1', 'i1',
                                     'p2',
                                     'q2', 'i2', 'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id',
                                     'connected1', 'connected2'],
                            data=[['', 0.0013, 0.0092, 0, 0.0512, 1, 1, NaN, -0.8, -0.1, 0.8076, 0.8008,
                                   0.05486, 0.8027, 'S1VL1', 'S1VL2', 'S1VL1_0', 'S1VL2_0', True, True]])
    pd.testing.assert_frame_equal(expected, n.get_2_windings_transformers(), check_dtype=False, atol=10 ** -4)
    n.update_2_windings_transformers(
        pd.DataFrame(data=[[1, 9, 0, 0, 1, 1, 100, -1, -0.2, 1,
                            0.04]],
                     columns=['r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1', 'p2', 'q2'],
                     index=['TWT']))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['TWT']),
                            columns=['name', 'r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1', 'i1',
                                     'p2',
                                     'q2', 'i2', 'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id',
                                     'connected1', 'connected2'],
                            data=[
                                ['', 1, 9, 0, 0, 1, 1, 100, -1, -0.2, 1.02, 1,
                                 0.04, 1, 'S1VL1', 'S1VL2', 'S1VL1_0', 'S1VL2_0',
                                 True, True]])
    pd.testing.assert_frame_equal(expected, n.get_2_windings_transformers(), check_dtype=False, atol=1e-2)


def test_shunt_compensators_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['SHUNT']),
                            columns=['name', 'g', 'b', 'model_type', 'max_section_count', 'section_count',
                                     'voltage_regulation_on', 'target_v', 'target_deadband', 'regulating_bus_id',
                                     'p', 'q', 'i',
                                     'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 0, -19.2, 'LINEAR', 1, 1, False, NaN, NaN, 'S1VL2_0', NaN, 19.2, NaN, 'S1VL2',
                                   'S1VL2_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_shunt_compensators(), check_dtype=False, atol=1e-2)


def test_dangling_lines_per_unit():
    n = util.create_dangling_lines_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)

    expected = pd.DataFrame(index=pd.Series(name='id', data=['DL']),
                            columns=['name', 'r', 'x', 'g', 'b', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id', 'connected', 'ucte-x-node-code', 'tie_line_id'],
                            data=[['', 0.1, 0.01, 0.01, 0.001, 0.5, 0.3, 0.5482, 0.3029, 0.6263, 'VL', 'VL_0',
                                   True, '', '']])
    dangling_lines = n.get_dangling_lines()
    pd.testing.assert_frame_equal(expected, dangling_lines, check_dtype=False, atol=10 ** -4)
    n.update_dangling_lines(pd.DataFrame(index=['DL'], columns=['p0', 'q0'], data=[[0.75, 0.25]]))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['DL']),
                            columns=['name', 'r', 'x', 'g', 'b', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id', 'connected', 'ucte-x-node-code', 'tie_line_id'],
                            data=[['', 0.1, 0.01, 0.01, 0.001, 0.75, 0.25, 0.5482, 0.3029, 0.6263, 'VL', 'VL_0',
                                   True, '', '']])
    dangling_lines = n.get_dangling_lines()
    pd.testing.assert_frame_equal(expected, dangling_lines, check_dtype=False, atol=10 ** -4)


def test_lcc_converter_stations_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LCC1', 'LCC2']),
                            columns=['name', 'power_factor', 'loss_factor', 'p', 'q', 'i', 'voltage_level_id', 'bus_id',
                                     'connected'],
                            data=[['LCC1', 0.6, 1.1, 0.8, 1.07, 1.33, 'S1VL2', 'S1VL2_0', True],
                                  ['LCC2', 0.6, 1.1, -0.78, 1.04, 1.30, 'S3VL1', 'S3VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_lcc_converter_stations(), check_dtype=False, atol=1e-2)
    n.update_lcc_converter_station(pd.DataFrame(data=[[3.0, 4.0], [1.0, 2.0]],
                                                columns=['p', 'q'],
                                                index=['LCC1', 'LCC2']))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LCC1', 'LCC2']),
                            columns=['name', 'power_factor', 'loss_factor', 'p', 'q', 'i', 'voltage_level_id', 'bus_id',
                                     'connected'],
                            data=[['LCC1', 0.6, 1.1, 3.0, 4.0, 5.0, 'S1VL2', 'S1VL2_0', True],
                                  ['LCC2', 0.6, 1.1, 1.0, 2.0, 2.24, 'S3VL1', 'S3VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_lcc_converter_stations(), check_dtype=False, atol=1e-2)


def test_vsc_converter_stations_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'loss_factor', 'min_q', 'max_q', 'reactive_limits_kind', 'target_v', 'target_q', 'voltage_regulator_on',
                 'regulated_element_id',
                 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],

        data=[['VSC1', 'VSC1', 1.1, NaN, NaN, 'CURVE', 1, 5, True, 'VSC1', 0.10, -5.12, 5.12, 'S1VL2', 'S1VL2_0', True],
              ['VSC2', 'VSC2', 1.1, -4, 5, 'MIN_MAX', 0, 1.2, False, 'VSC2', -0.1, -1.2, 1.18, 'S2VL1', 'S2VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_vsc_converter_stations(), check_dtype=False, atol=1e-2)
    n.update_vsc_converter_stations(pd.DataFrame(data=[[3.0, 4.0], [1.0, 2.0]],
                                                 columns=['target_v', 'target_q'],
                                                 index=['VSC1', 'VSC2']))
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'loss_factor', 'min_q', 'max_q', 'reactive_limits_kind', 'target_v', 'target_q', 'voltage_regulator_on', 'regulated_element_id',
                 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
        data=[['VSC1', 'VSC1', 1.1, NaN, NaN, 'CURVE', 3, 4, True, 'VSC1', 0.10, -5.12, 5.12, 'S1VL2', 'S1VL2_0', True],
              ['VSC2', 'VSC2', 1.1, -4, 5, 'MIN_MAX', 1, 2, False, 'VSC2', -0.1, -1.2, 1.18, 'S2VL1', 'S2VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_vsc_converter_stations(), check_dtype=False, atol=1e-2)


def test_get_static_var_compensators_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n)
    n = per_unit_view(n, 100)
    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'b_min', 'b_max', 'target_v', 'target_q', 'regulation_mode', 'regulated_element_id',
                 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
        data=[['SVC', '', -0.05, 0.05, 1.0, NaN, 'VOLTAGE', 'SVC', 0, -0.13, 0.13, 'S4VL1', 'S4VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_static_var_compensators(), check_dtype=False, atol=1e-2)

    n.update_static_var_compensators(pd.DataFrame.from_records(
        index=['id'], columns=['id', 'target_v', 'target_q'],
        data=[('SVC', 3.0, 4.0)]))

    expected = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'name', 'b_min', 'b_max', 'target_v', 'target_q', 'regulation_mode', 'regulated_element_id',
                 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
        data=[['SVC', '', -0.05, 0.05, 3, 4, 'VOLTAGE', 'SVC', 0, -0.13, 0.13, 'S4VL1', 'S4VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_static_var_compensators(), check_dtype=False, atol=1e-2)


def test_voltage_level_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['S1VL1', 'S1VL2', 'S2VL1', 'S3VL1', 'S4VL1']),
                            columns=['name', 'substation_id', 'nominal_v', 'high_voltage_limit', 'low_voltage_limit'],
                            data=[['', 'S1', 225, 1.07, 0.98], ['', 'S1', 400, 1.1, 0.98], ['', 'S2', 400, 1.1, 0.98],
                                  ['', 'S3', 400, 1.1, 0.98], ['', 'S4', 400, 1.1, 0.98]])
    pd.testing.assert_frame_equal(expected, n.get_voltage_levels(), check_dtype=False, atol=1e-2)


def test_reactive_capability_curve_points_per_unit():
    n = pp.network.create_four_substations_node_breaker_network()
    n = per_unit_view(n, 100)
    reactive_capability_curve_points = n.get_reactive_capability_curve_points()
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GH1', 0)],
                                   pd.Series(data={'p': 0, 'min_q': -7.69, 'max_q': 8.6},
                                             name=('GH1', 0)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GH1', 1)],
                                   pd.Series(data={'p': 1, 'min_q': -8.65, 'max_q': 9.46},
                                             name=('GH1', 1)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GH2', 0)],
                                   pd.Series(data={'p': 0, 'min_q': -5.57, 'max_q': 5.57},
                                             name=('GH2', 0)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GH2', 1)],
                                   pd.Series(data={'p': 2, 'min_q': -5.53, 'max_q': 5.36},
                                             name=('GH2', 1)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GH3', 0)],
                                   pd.Series(data={'p': 0, 'min_q': -6.81, 'max_q': 6.88},
                                             name=('GH3', 0)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GH3', 1)],
                                   pd.Series(data={'p': 2, 'min_q': -6.82, 'max_q': 7.16},
                                             name=('GH3', 1)), check_dtype=False, atol=True)

    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GTH1', 0)],
                                   pd.Series(data={'p': 0, 'min_q': -0.77, 'max_q': 0.77},
                                             name=('GTH1', 0)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GTH1', 1)],
                                   pd.Series(data={'p': 0, 'min_q': -0.74, 'max_q': 0.76},
                                             name=('GTH1', 1)), check_dtype=False, atol=True)

    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GTH2', 0)],
                                   pd.Series(data={'p': 0, 'min_q': -1.69, 'max_q': 2},
                                             name=('GTH2', 0)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('GTH2', 1)],
                                   pd.Series(data={'p': 4, 'min_q': -1.75, 'max_q': 1.76},
                                             name=('GTH2', 1)), check_dtype=False, atol=True)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('VSC1', 0)],
                                   pd.Series(data={'p': -1, 'min_q': -5.5, 'max_q': 5.7},
                                             name=('VSC1', 0)), check_dtype=False, atol=1e-2)
    pd.testing.assert_series_equal(reactive_capability_curve_points.loc[('VSC1', 1)],
                                   pd.Series(data={'p': 1, 'min_q': -5.5, 'max_q': 5.7},
                                             name=('VSC1', 1)), check_dtype=False, atol=1e-2)


def test_three_windings_transformer_per_unit():
    n = util.create_three_windings_transformer_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['3WT']),
        columns=['name', 'rated_u0', 'r1', 'x1', 'g1', 'b1', 'rated_u1', 'rated_s1', 'ratio_tap_position1',
                 'phase_tap_position1', 'p1', 'q1', 'i1', 'voltage_level1_id', 'bus1_id', 'connected1', 'r2', 'x2',
                 'g2', 'b2', 'rated_u2', 'rated_s2', 'ratio_tap_position2', 'phase_tap_position2', 'p2', 'q2', 'i2',
                 'voltage_level2_id', 'bus2_id', 'connected2', 'r3', 'x3', 'g3', 'b3', 'rated_u3', 'rated_s3',
                 'ratio_tap_position3', 'phase_tap_position3', 'p3', 'q3', 'i3', 'voltage_level3_id', 'bus3_id',
                 'connected3'],
        data=[
            ['', 1, 0.1, 0.01, 1, 0.1, 1, NaN, -99999, -99999, NaN, NaN, NaN, 'VL_132', 'VL_132_0', True, 0.00625,
             0.000625,
             0, 0,
             1, NaN, 2, -99999, NaN, NaN, NaN, 'VL_33', 'VL_33_0', True, 0.001, 6.94444e-05, 0, 0, 1, NaN, 0, -99999,
             NaN,
             NaN, NaN, 'VL_11', 'VL_11_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_3_windings_transformers(), check_dtype=False,
                                  atol=1e-2)
    n.update_3_windings_transformers(pd.DataFrame(data=[
        [99, 9, 0, 0, 1, 100, 1, 2, 0.5, 0.1, 99, 9, 0, 0, 1, 100, 1, 2, 0.5, 0.1, 99, 9, 0, 0, 1, 100, 1, 2, 0.5,
         0.1]],
        columns=['r1', 'x1', 'g1', 'b1', 'rated_u1', 'rated_s1',
                 'ratio_tap_position1', 'phase_tap_position1', 'p1', 'q1',
                 'r2', 'x2', 'g2', 'b2', 'rated_u2', 'rated_s2',
                 'ratio_tap_position2', 'phase_tap_position2', 'p2', 'q2',
                 'r3', 'x3', 'g3', 'b3', 'rated_u3', 'rated_s3',
                 'ratio_tap_position3', 'phase_tap_position3', 'p3',
                 'q3'],
        index=['3WT']))
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['3WT']),
        columns=['name', 'rated_u0', 'r1', 'x1', 'g1', 'b1', 'rated_u1', 'rated_s1', 'ratio_tap_position1',
                 'phase_tap_position1', 'p1', 'q1', 'i1', 'voltage_level1_id', 'bus1_id', 'connected1', 'r2', 'x2',
                 'g2', 'b2', 'rated_u2', 'rated_s2', 'ratio_tap_position2', 'phase_tap_position2', 'p2', 'q2', 'i2',
                 'voltage_level2_id', 'bus2_id', 'connected2', 'r3', 'x3', 'g3', 'b3', 'rated_u3', 'rated_s3',
                 'ratio_tap_position3', 'phase_tap_position3', 'p3', 'q3', 'i3', 'voltage_level3_id', 'bus3_id',
                 'connected3'],
        data=[['', 1, 99, 9, 0, 0, 1, 100, -99999, -99999, 0.5, 0.1, 0.5, 'VL_132', 'VL_132_0', True, 99, 9, 0, 0,
               1, 100, 1, -99999, 0.5, 0.1, 0.48, 'VL_33', 'VL_33_0', True, 99, 9, 0, 0, 1, 100, 1, -99999, 0.5,
               0.1, 0.48, 'VL_11', 'VL_11_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_3_windings_transformers(), check_dtype=False,
                                  atol=1e-2)


def test_batteries():
    n = util.create_battery_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['BAT', 'BAT2']),
                            columns=['name', 'max_p', 'min_p', 'min_q', 'max_q', 'reactive_limits_kind', 'target_p', 'target_q',
                                     'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 100, -100, -100, 100, 'MIN_MAX', 100, 100, -6.05, -2.25, NaN, 'VLBAT', 'VLBAT_0', True],
                                  ['', 2, -2, NaN, NaN, 'CURVE', 1, 2, -6.05, -2.25, NaN, 'VLBAT', 'VLBAT_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_batteries(), check_dtype=False, atol=1e-2)
    n.update_batteries(pd.DataFrame(data=[[50, -50, 50, 50, -7, -3]],
                                    columns=['max_p', 'min_p', 'target_p', 'target_q', 'p', 'q'],
                                    index=['BAT']))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['BAT', 'BAT2']),
                            columns=['name', 'max_p', 'min_p', 'min_q', 'max_q', 'reactive_limits_kind', 'target_p', 'target_q',
                                     'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 50, -50, -100, 100, 'MIN_MAX', 50, 50, -7, -3, NaN, 'VLBAT', 'VLBAT_0', True],
                                  ['', 2, -2, NaN, NaN, 'CURVE', 1, 2, -6.05, -2.25, NaN, 'VLBAT', 'VLBAT_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_batteries(), check_dtype=False, atol=1e-2)


def test_ratio_tap_changers_per_unit():
    n = pp.network.create_eurostag_tutorial_example1_network()
    n = per_unit_view(n, 100)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['NHV2_NLOAD']),
                            columns=['tap', 'low_tap', 'high_tap', 'step_count', 'on_load', 'regulating',
                                     'target_v', 'target_deadband', 'regulating_bus_id', 'rho',
                                     'alpha'],
                            data=[[1, 0, 2, 3, True, True, 158.0, 0.0, 'VLLOAD_0', 1.00, NaN]])
    pd.testing.assert_frame_equal(expected, n.get_ratio_tap_changers(), check_dtype=False, atol=1e-2)
