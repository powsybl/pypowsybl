#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import copy
import pathlib
import unittest
import datetime
import pandas as pd
from numpy import NaN

import pypowsybl as pp

TEST_DIR = pathlib.Path(__file__).parent


class NetworkTestCase(unittest.TestCase):
    @staticmethod
    def test_print_version():
        pp.print_version()

    def test_create_empty_network(self):
        n = pp.network.create_empty("test")
        self.assertIsNotNone(n)

    def test_load_network_from_string(self):
        file_content = """
##C 2007.05.01
##N
##ZBE
BBE1AA1               0 2 400.00 3000.00 0.00000 -1500.0 0.00000 0.00000 -9000.0 9000.00 -9000.0                               F
        """
        n = pp.network.load_from_string('simple-eu.uct', file_content)
        self.assertEqual(1, len(n.get_substations()))

    def test_dump_to_string(self):
        bat_path = TEST_DIR.joinpath('battery.xiidm')
        xml = bat_path.read_text()
        n = pp.network.load(str(bat_path))
        self.assertEqual(xml, n.dump_to_string())

    def test_get_import_format(self):
        formats = pp.network.get_import_formats()
        self.assertEqual(['CGMES', 'MATPOWER', 'IEEE-CDF', 'PSS/E', 'UCTE', 'XIIDM'], formats)

    def test_get_import_parameters(self):
        parameters = pp.network.get_import_parameters('PSS/E')
        self.assertEqual(1, len(parameters))
        self.assertEqual(['psse.import.ignore-base-voltage'], parameters.index.tolist())
        self.assertEqual('Ignore base voltage specified in the file',
                         parameters['description']['psse.import.ignore-base-voltage'])
        self.assertEqual('BOOLEAN', parameters['type']['psse.import.ignore-base-voltage'])
        self.assertEqual('false', parameters['default']['psse.import.ignore-base-voltage'])

    def test_get_export_format(self):
        formats = pp.network.get_export_formats()
        self.assertEqual(['CGMES', 'UCTE', 'XIIDM', 'ADN'], formats)

    def test_load_network(self):
        n = pp.network.load(str(TEST_DIR.joinpath('empty-network.xml')))
        self.assertIsNotNone(n)

    def test_connect_disconnect(self):
        n = pp.network.create_ieee14()
        self.assertTrue(n.disconnect('L1-2-1'))
        self.assertTrue(n.connect('L1-2-1'))

    def test_network_attributes(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        self.assertEqual('sim1', n.id)
        self.assertEqual(datetime.datetime(2018, 1, 1, 10, 0), n.case_date)
        self.assertEqual('sim1', n.name)
        self.assertEqual(datetime.timedelta(0), n.forecast_distance)
        self.assertEqual('test', n.source_format)

    def test_network_representation(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        expected = 'Network(id=sim1, name=sim1, case_date=2018-01-01 10:00:00, ' \
                   'forecast_distance=0:00:00, source_format=test)'
        self.assertEqual(expected, str(n))
        self.assertEqual(expected, repr(n))

    def test_get_network_element_ids(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        self.assertEqual(['NGEN_NHV1', 'NHV2_NLOAD'],
                         n.get_elements_ids(pp.network.ElementType.TWO_WINDINGS_TRANSFORMER))
        self.assertEqual(['NGEN_NHV1'], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
                                                           nominal_voltages={24}))
        self.assertEqual(['NGEN_NHV1', 'NHV2_NLOAD'],
                         n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
                                            nominal_voltages={24, 150}))
        self.assertEqual(['LOAD'], n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150}))
        self.assertEqual(['LOAD'], n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150},
                                                      countries={'BE'}))
        self.assertEqual([], n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150},
                                                countries={'FR'}))
        self.assertEqual(['NGEN_NHV1'], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
                                                           nominal_voltages={24}, countries={'FR'}))
        self.assertEqual([], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
                                                nominal_voltages={24}, countries={'BE'}))

    def test_buses(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        buses = n.get_buses()
        expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                                columns=['v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                                         'voltage_level_id'],
                                data=[[NaN, NaN, 0, 0, 'VLGEN'],
                                      [380, NaN, 0, 0, 'VLHV1'],
                                      [380, NaN, 0, 0, 'VLHV2'],
                                      [NaN, NaN, 0, 0, 'VLLOAD']])
        pd.testing.assert_frame_equal(expected, buses, check_dtype=False)

        n.update_buses(pd.DataFrame(index=['VLGEN_0'], columns=['v_mag', 'v_angle'], data=[[400, 0]]))
        buses = n.get_buses()
        expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                                columns=['v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                                         'voltage_level_id'],
                                data=[[400, 0, 0, 0, 'VLGEN'],
                                      [380, NaN, 0, 0, 'VLHV1'],
                                      [380, NaN, 0, 0, 'VLHV2'],
                                      [NaN, NaN, 0, 0, 'VLLOAD']])
        pd.testing.assert_frame_equal(expected, buses, check_dtype=False)

    def test_loads_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        loads = n.get_loads()
        self.assertEqual(600, loads['p0']['LOAD'])
        self.assertEqual(200, loads['q0']['LOAD'])
        self.assertEqual('UNDEFINED', loads['type']['LOAD'])
        df2 = pd.DataFrame(data=[[500, 300]], columns=['p0', 'q0'], index=['LOAD'])
        n.update_loads(df2)
        df3 = n.get_loads()
        self.assertEqual(300, df3['q0']['LOAD'])
        self.assertEqual(500, df3['p0']['LOAD'])

    def test_batteries_data_frame(self):
        n = pp.network.load(str(TEST_DIR.joinpath('battery.xiidm')))
        batteries = n.get_batteries()
        self.assertEqual(200.0, batteries['max_p']['BAT2'])
        df2 = pd.DataFrame(data=[[101, 201]], columns=['p0', 'q0'], index=['BAT2'])
        n.update_batteries(df2)
        df3 = n.get_batteries()
        self.assertEqual(101, df3['p0']['BAT2'])
        self.assertEqual(201, df3['q0']['BAT2'])

    def test_vsc_data_frame(self):
        n = pp.network.create_four_substations_node_breaker_network()
        stations = n.get_vsc_converter_stations()
        self.assertEqual(400.0, stations['voltage_setpoint']['VSC1'])
        self.assertEqual(500.0, stations['reactive_power_setpoint']['VSC1'])
        stations2 = pd.DataFrame(data=[[300.0, 400.0], [1.0, 2.0]],
                                 columns=['voltage_setpoint', 'reactive_power_setpoint'], index=['VSC1', 'VSC2'])
        n.update_vsc_converter_stations(stations2)
        stations = n.get_vsc_converter_stations()
        self.assertEqual(300.0, stations['voltage_setpoint']['VSC1'])
        self.assertEqual(400.0, stations['reactive_power_setpoint']['VSC1'])
        self.assertEqual(1.0, stations['voltage_setpoint']['VSC2'])
        self.assertEqual(2.0, stations['reactive_power_setpoint']['VSC2'])

    def test_hvdc_data_frame(self):
        n = pp.network.create_four_substations_node_breaker_network()
        lines = n.get_hvdc_lines()
        self.assertEqual(10, lines['active_power_setpoint']['HVDC1'])
        lines2 = pd.DataFrame(data=[11], columns=['active_power_setpoint'], index=['HVDC1'])
        n.update_hvdc_lines(lines2)
        lines = n.get_hvdc_lines()
        self.assertEqual(11, lines['active_power_setpoint']['HVDC1'])

    def test_svc_data_frame(self):
        n = pp.network.create_four_substations_node_breaker_network()
        svcs = n.get_static_var_compensators()
        self.assertEqual(400.0, svcs['voltage_setpoint']['SVC'])
        self.assertEqual('VOLTAGE', svcs['regulation_mode']['SVC'])
        svcs2 = pd.DataFrame(data=[[300.0, 400.0, 'off']],
                             columns=['voltage_setpoint', 'reactive_power_setpoint', 'regulation_mode'], index=['SVC'])
        n.update_static_var_compensators(svcs2)
        svcs = n.get_static_var_compensators()
        self.assertEqual(300.0, svcs['voltage_setpoint']['SVC'])
        self.assertEqual(400.0, svcs['reactive_power_setpoint']['SVC'])
        self.assertEqual('OFF', svcs['regulation_mode']['SVC'])

    def test_create_generators_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        generators = n.get_generators()
        self.assertEqual('OTHER', generators['energy_source']['GEN'])
        self.assertEqual(607, generators['target_p']['GEN'])

    def test_ratio_tap_changer_steps_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        steps = n.get_ratio_tap_changer_steps()
        self.assertEqual(0.8505666905244191, steps.loc['NHV2_NLOAD']['rho'][0])
        self.assertEqual(0.8505666905244191, steps.loc[('NHV2_NLOAD', 0), 'rho'])
        expected = pd.DataFrame(
            index=pd.MultiIndex.from_tuples([('NHV2_NLOAD', 0), ('NHV2_NLOAD', 1), ('NHV2_NLOAD', 2)],
                                            names=['id', 'position']),
            columns=['rho', 'r', 'x', 'g', 'b'],
            data=[[0.850567, 0, 0, 0, 0],
                  [1.00067, 0, 0, 0, 0],
                  [1.15077, 0, 0, 0, 0]])
        pd.testing.assert_frame_equal(expected, steps, check_dtype=False)

    def test_phase_tap_changer_steps_data_frame(self):
        n = pp.network.create_ieee300()
        steps = n.get_phase_tap_changer_steps()
        self.assertEqual(11.4, steps.loc[('T196-2040-1', 0), 'alpha'])

    def test_update_generators_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        generators = n.get_generators()
        self.assertEqual(607, generators['target_p']['GEN'])
        self.assertTrue(generators['voltage_regulator_on']['GEN'])
        generators2 = pd.DataFrame(data=[[608.0, 302.0, 25.0, False]],
                                   columns=['target_p', 'target_q', 'target_v', 'voltage_regulator_on'], index=['GEN'])
        n.update_generators(generators2)
        generators = n.get_generators()
        self.assertEqual(608, generators['target_p']['GEN'])
        self.assertEqual(302.0, generators['target_q']['GEN'])
        self.assertEqual(25.0, generators['target_v']['GEN'])
        self.assertFalse(generators['voltage_regulator_on']['GEN'])

    def test_update_switches_data_frame(self):
        n = pp.network.load(str(TEST_DIR.joinpath('node-breaker.xiidm')))
        switches = n.get_switches()
        # no open switch
        open_switches = switches[switches['open']].index.tolist()
        self.assertEqual(0, len(open_switches))
        # open 1 breaker
        n.update_switches(pd.DataFrame(index=['BREAKER-BB2-VL1_VL2_1'], data={'open': [True]}))
        switches = n.get_switches()
        open_switches = switches[switches['open']].index.tolist()
        self.assertEqual(['BREAKER-BB2-VL1_VL2_1'], open_switches)

    def test_create_and_update_2_windings_transformers_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        df = n.get_2_windings_transformers()
        self.assertEqual(['r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                          'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1', 'connected2'],
                         df.columns.tolist())
        load_tfo = df.loc['NHV2_NLOAD']
        self.assertAlmostEqual(0.042, load_tfo.r, places=1)
        self.assertAlmostEqual(04.05, load_tfo.x, places=1)
        self.assertEqual(0, load_tfo.g)
        self.assertEqual(0, load_tfo.b)
        self.assertEqual(400, load_tfo.rated_u1)
        self.assertEqual(158, load_tfo.rated_u2)
        self.assertEqual('VLHV2', load_tfo.voltage_level1_id)
        self.assertEqual('VLLOAD', load_tfo.voltage_level2_id)
        self.assertEqual('VLHV2_0', load_tfo.bus1_id)
        self.assertEqual('VLLOAD_0', load_tfo.bus2_id)

    def test_voltage_levels_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        voltage_levels = n.get_voltage_levels()
        self.assertEqual(24.0, voltage_levels['nominal_v']['VLGEN'])

    def test_substations_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        substations = n.get_substations()
        self.assertEqual('RTE', substations['TSO']['P1'])
        self.assertEqual('FR', substations['country']['P1'])

    def test_reactive_capability_curve_points_data_frame(self):
        n = pp.network.create_four_substations_node_breaker_network()
        points = n.get_reactive_capability_curve_points()
        self.assertAlmostEqual(0, points.loc['GH1']['p'][0])
        self.assertAlmostEqual(100, points.loc['GH1']['p'][1])
        self.assertAlmostEqual(-769.3, points.loc['GH1']['min_q'][0])
        self.assertAlmostEqual(-864.55, points.loc['GH1']['min_q'][1])
        self.assertAlmostEqual(860, points.loc['GH1']['max_q'][0])
        self.assertAlmostEqual(946.25, points.loc['GH1']['max_q'][1])

    def test_exception(self):
        n = pp.network.create_ieee14()
        try:
            n.open_switch("aa")
            self.fail()
        except pp.PyPowsyblError as e:
            self.assertEqual("Switch 'aa' not found", str(e))

    def test_ratio_tap_changers(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        tap_changers = n.get_ratio_tap_changers()
        self.assertEqual(['tap', 'low_tap', 'high_tap', 'step_count', 'on_load', 'regulating',
                          'target_v', 'target_deadband', 'regulating_bus_id'], tap_changers.columns.tolist())
        self.assertEqual([1, 0, 2, 3, True, True, 158.0, 0.0, 'VLLOAD_0'], tap_changers.loc['NHV2_NLOAD'].tolist())

        update = pd.DataFrame(index=['NHV2_NLOAD'],
                              columns=['tap', 'regulating', 'target_v'],
                              data=[[0, False, 180]])
        n.update_ratio_tap_changers(update)

        tap_changers = n.get_ratio_tap_changers()
        values = tap_changers.loc['NHV2_NLOAD']
        self.assertFalse(values.regulating)
        self.assertEqual(0, values.tap)
        self.assertAlmostEqual(180, values.target_v, 1)

    def test_phase_tap_changers(self):
        n = pp.network.create_four_substations_node_breaker_network()
        tap_changers = n.get_phase_tap_changers()
        self.assertEqual(['tap', 'low_tap', 'high_tap', 'step_count', 'regulating', 'regulation_mode',
                          'regulation_value', 'target_deadband', 'regulating_bus_id'], tap_changers.columns.tolist())
        twt_values = tap_changers.loc['TWT']
        self.assertEqual(15, twt_values.tap)
        self.assertEqual(0, twt_values.low_tap)
        self.assertEqual(32, twt_values.high_tap)
        self.assertEqual(33, twt_values.step_count)
        self.assertEqual(False, twt_values.regulating)
        self.assertEqual('FIXED_TAP', twt_values.regulation_mode)
        self.assertTrue(pd.isna(twt_values.regulation_value))
        self.assertTrue(pd.isna(twt_values.target_deadband))

        update = pd.DataFrame(index=['TWT'],
                              columns=['tap', 'target_deadband', 'regulation_value', 'regulation_mode', 'regulating'],
                              data=[[10, 100, 1000, 'CURRENT_LIMITER', True]])
        n.update_phase_tap_changers(update)

        tap_changers = n.get_phase_tap_changers()
        self.assertEqual(['tap', 'low_tap', 'high_tap', 'step_count', 'regulating', 'regulation_mode',
                          'regulation_value', 'target_deadband', 'regulating_bus_id'], tap_changers.columns.tolist())
        twt_values = tap_changers.loc['TWT']
        self.assertEqual(10, twt_values.tap)
        self.assertEqual(True, twt_values.regulating)
        self.assertEqual('CURRENT_LIMITER', twt_values.regulation_mode)
        self.assertAlmostEqual(1000, twt_values.regulation_value, 1)
        self.assertAlmostEqual(100, twt_values.target_deadband, 1)

    def test_variant(self):
        n = pp.network.load(str(TEST_DIR.joinpath('node-breaker.xiidm')))
        self.assertEqual('InitialState', n.get_working_variant_id())
        n.clone_variant('InitialState', 'WorkingState')
        n.update_switches(pd.DataFrame(index=['BREAKER-BB2-VL1_VL2_1'], data={'open': [True]}))
        n.set_working_variant('WorkingState')
        self.assertEqual('WorkingState', n.get_working_variant_id())
        self.assertEqual(['InitialState', 'WorkingState'], n.get_variant_ids())
        self.assertEqual(0, len(n.get_switches()[n.get_switches()['open']].index.tolist()))
        n.set_working_variant('InitialState')
        n.remove_variant('WorkingState')
        self.assertEqual(['BREAKER-BB2-VL1_VL2_1'], n.get_switches()[n.get_switches()['open']].index.tolist())
        self.assertEqual('InitialState', n.get_working_variant_id())
        self.assertEqual(1, len(n.get_variant_ids()))

    def test_sld_svg(self):
        n = pp.network.create_four_substations_node_breaker_network()
        sld = n.get_single_line_diagram('S1VL1')
        self.assertRegex(sld.svg, '.*<svg.*')

    def test_deep_copy(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        copy_n = copy.deepcopy(n)
        self.assertEqual(['NGEN_NHV1', 'NHV2_NLOAD'],
                         copy_n.get_elements_ids(pp.network.ElementType.TWO_WINDINGS_TRANSFORMER))

    def test_lines(self):
        n = pp.network.create_four_substations_node_breaker_network()
        expected = pd.DataFrame(index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
                                columns=['r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                                         'voltage_level1_id',
                                         'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1', 'connected2'],
                                data=[[0.01, 19.1, 0, 0, 0, 0, 109.889, 190.023, 309.979, -109.886, -184.517, 309.978,
                                       'S2VL1',
                                       'S3VL1',
                                       'S2VL1_0', 'S3VL1_0', True, True],
                                      [0.01, 13.1, 0, 0, 0, 0, 240.004, 2.1751, 346.43, -240, 2.5415, 346.43, 'S3VL1',
                                       'S4VL1',
                                       'S3VL1_0', 'S4VL1_0', True, True]])

        pd.options.display.max_columns = None
        pd.options.display.expand_frame_repr = False
        lines = n.get_lines()
        pd.testing.assert_frame_equal(expected, lines, check_dtype=False)

        lines_update = pd.DataFrame(index=['LINE_S2S3'],
                                    columns=['r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'p2', 'q2'],
                                    data=[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]])
        n.update_lines(lines_update)
        expected = pd.DataFrame(index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
                                columns=['r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                                         'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1', 'connected2'],
                                data=[[1, 2, 3, 4, 5, 6, 7, 8, 15.011282, 9, 10, 19.418634,
                                       'S2VL1', 'S3VL1', 'S2VL1_0', 'S3VL1_0', True, True],
                                      [0.01, 13.1, 0, 0, 0, 0, 240.004, 2.1751, 346.429584, -240, 2.5415, 346.429584,
                                       'S3VL1', 'S4VL1', 'S3VL1_0', 'S4VL1_0', True, True]])
        lines = n.get_lines()
        pd.testing.assert_frame_equal(expected, lines, check_dtype=False)

    def test_dangling_lines(self):
        n = pp.network._create_dangling_lines_network()
        expected = pd.DataFrame(index=pd.Series(name='id', data=['DL']),
                                columns=['r', 'x', 'g', 'b', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
                                data=[[10.0, 1.0, 0.0001, 0.00001, 50.0, 30.0, NaN, NaN, NaN, 'VL', 'VL_0', True]])
        dangling_lines = n.get_dangling_lines()
        pd.testing.assert_frame_equal(expected, dangling_lines, check_dtype=False)

    def test_batteries(self):
        n = pp.network._create_battery_network()
        expected = pd.DataFrame(index=pd.Series(name='id', data=['BAT', 'BAT2']),
                                columns=['max_p', 'min_p', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
                                data=[[9999.99, -9999.99, 9999.99, 9999.99, -605, -225, NaN, 'VLBAT', 'VLBAT_0', True],
                                      [200, -200, 100, 200, -605, -225, NaN, 'VLBAT', 'VLBAT_0', True]])
        pd.testing.assert_frame_equal(expected, n.get_batteries(), check_dtype=False)

    def test_shunt(self):
        n = pp.network.create_four_substations_node_breaker_network()
        expected = pd.DataFrame(index=pd.Series(name='id', data=['SHUNT']),
                                columns=['model_type', 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'max_section_count',
                                         'section_count', 'connected'],
                                data=[['LINEAR', NaN, 1920, NaN, 'S1VL2', 'S1VL2_0', 1, 1, True]])
        pd.testing.assert_frame_equal(expected, n.get_shunt_compensators(), check_dtype=False)
        n.update_shunt_compensator(
            pd.DataFrame(index=['SHUNT'], columns=['q', 'section_count', 'connected'], data=[[1900, 0, False]]))
        expected = pd.DataFrame(index=pd.Series(name='id', data=['SHUNT']),
                                columns=['model_type', 'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'max_section_count',
                                         'section_count', 'connected'],
                                data=[['LINEAR', NaN, 1900, NaN, 'S1VL2', '', 1, 0, False]])
        pd.testing.assert_frame_equal(expected, n.get_shunt_compensators(), check_dtype=False)


if __name__ == '__main__':
    unittest.main()
