#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import os
import unittest
from _pypowsybl import PyPowsyblError
import pypowsybl.network
import pypowsybl.loadflow
import pypowsybl.security
import pypowsybl.sensitivity
import pypowsybl as pp
import pandas as pd
import pathlib

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')

class PyPowsyblTestCase(unittest.TestCase):
    @staticmethod
    def test_print_version():
        pp.print_version()

    def test_create_empty_network(self):
        n = pp.network.create_empty("test")
        self.assertIsNotNone(n)

    def test_run_lf(self):
        n = pp.network.create_ieee14()
        results = pp.loadflow.run_ac(n)
        self.assertEqual(1, len(results))
        self.assertEqual(pp.loadflow.ComponentStatus.CONVERGED, list(results)[0].status)
        parameters = pp.loadflow.Parameters(distributed_slack=False)
        results = pp.loadflow.run_dc(n, parameters)
        self.assertEqual(1, len(results))

    def test_get_import_format(self):
        formats = pp.network.get_import_formats()
        self.assertEqual(['CGMES', 'MATPOWER', 'IEEE-CDF', 'PSS/E', 'UCTE', 'XIIDM'], formats)

    def test_get_import_parameters(self):
        parameters = pp.network.get_import_parameters('PSS/E')
        self.assertEqual(1, len(parameters))
        self.assertEqual(['psse.import.ignore-base-voltage'], parameters.index.tolist())
        self.assertEqual('Ignore base voltage specified in the file', parameters['description']['psse.import.ignore-base-voltage'])
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

    def test_security_analysis(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        sa = pp.security.create_analysis()
        sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
        sa_result = sa.run_ac(n)
        self.assertEqual(1, len(sa_result._post_contingency_results))

    def test_get_network_element_ids(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        self.assertEqual(['NGEN_NHV1', 'NHV2_NLOAD'], n.get_elements_ids(pp.network.ElementType.TWO_WINDINGS_TRANSFORMER))
        self.assertEqual(['NGEN_NHV1'], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER, nominal_voltages={24}))
        self.assertEqual(['NGEN_NHV1', 'NHV2_NLOAD'], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER, nominal_voltages={24, 150}))
        self.assertEqual(['LOAD'], n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150}))
        self.assertEqual(['LOAD'], n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150}, countries={'FR'}))
        self.assertEqual([], n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150}, countries={'BE'}))
        self.assertEqual(['NGEN_NHV1'], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER, nominal_voltages={24}, countries={'FR'}))
        self.assertEqual([], n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER, nominal_voltages={24}, countries={'BE'}))

    def test_loads_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        loads = n.get_loads()
        self.assertEqual(600, loads['p0']['LOAD'])
        self.assertEqual(200, loads['q0']['LOAD'])
        self.assertEqual('UNDEFINED', loads['type']['LOAD'])
        df2 = pd.DataFrame(data=[[500, 300]], columns=['p0','q0'], index=['LOAD'])
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
        stations2 = pd.DataFrame(data=[[300.0, 400.0],[1.0, 2.0]], columns=['voltage_setpoint','reactive_power_setpoint'], index=['VSC1', 'VSC2'])
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

    def test_phase_tap_changer_steps_data_frame(self):
        n = pp.network.create_ieee300()
        steps = n.get_phase_tap_changer_steps()
        self.assertEqual(11.4, steps.loc[('T196-2040-1', 0), 'alpha'])

    def test_update_generators_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        generators = n.get_generators()
        self.assertEqual(607, generators['target_p']['GEN'])
        self.assertTrue(generators['voltage_regulator_on']['GEN'])
        generators2 = pd.DataFrame(data=[[608.0, 302.0, 25.0, False]], columns=['target_p','target_q','target_v','voltage_regulator_on'], index=['GEN'])
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
        transfos = n.get_2_windings_transformers()
        self.assertEqual(1, transfos['ratio_tap_position']['NHV2_NLOAD'])
        self.assertEqual(-99999, transfos['phase_tap_position']['NHV2_NLOAD'])
        n.update_2_windings_transformers(pd.DataFrame(index=['NHV2_NLOAD'], data={'ratio_tap_position': [0]}))
        transfos = n.get_2_windings_transformers()
        self.assertEqual(0, transfos['ratio_tap_position']['NHV2_NLOAD'])

        # also test phase shifter
        n = pp.network.create_four_substations_node_breaker_network();
        transfos = n.get_2_windings_transformers()
        self.assertEqual(15, transfos['phase_tap_position']['TWT'])
        n.update_2_windings_transformers(pd.DataFrame(index=['TWT'], data={'phase_tap_position': [16]}))
        transfos = n.get_2_windings_transformers()
        self.assertEqual(16, transfos['phase_tap_position']['TWT'])

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
        self.assertAlmostEqual(-769.3, points.loc['GH1']['min_p'][0])
        self.assertAlmostEqual(-864.55, points.loc['GH1']['min_p'][1])
        self.assertAlmostEqual(860, points.loc['GH1']['max_p'][0])
        self.assertAlmostEqual(946.25, points.loc['GH1']['max_p'][1])

    def test_sensitivity_analysis(self):
        n = pp.network.create_ieee14()
        sa = pp.sensitivity.create_dc_analysis()
        sa.add_single_element_contingency('L1-2-1')
        sa.set_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
        r = sa.run(n)

        df = r.get_branch_flows_sensitivity_matrix()
        self.assertEqual((3, 2), df.shape)
        self.assertEqual(0.08099067519128486, df['L1-5-1']['B1-G'])
        self.assertEqual(-0.08099067519128486, df['L1-5-1']['B2-G'])
        self.assertEqual(-0.17249763831611517, df['L1-5-1']['B3-G'])
        self.assertEqual(-0.013674968450008108, df['L2-3-1']['B1-G'])
        self.assertEqual(0.013674968450008108, df['L2-3-1']['B2-G'])
        self.assertEqual(-0.5456827116267954, df['L2-3-1']['B3-G'])

        df = r.get_reference_flows()
        self.assertEqual((1, 2), df.shape)
        self.assertAlmostEqual(72.24667948865367, df['L1-5-1']['reference_flows'], places=6)
        self.assertAlmostEqual(69.83139138110104, df['L2-3-1']['reference_flows'], places=6)

        df = r.get_branch_flows_sensitivity_matrix('L1-2-1')
        self.assertEqual((3, 2), df.shape)
        self.assertEqual(0.49999999999999994, df['L1-5-1']['B1-G'])
        self.assertEqual(-0.49999999999999994, df['L1-5-1']['B2-G'])
        self.assertEqual(-0.49999999999999994, df['L1-5-1']['B3-G'])
        self.assertEqual(-0.08442310437411704, df['L2-3-1']['B1-G'])
        self.assertEqual(0.08442310437411704, df['L2-3-1']['B2-G'])
        self.assertEqual(-0.49038517950037847, df['L2-3-1']['B3-G'])

        df = r.get_reference_flows('L1-2-1')
        self.assertEqual((1, 2), df.shape)
        self.assertAlmostEqual(225.69999999999996, df['L1-5-1']['reference_flows'], places=6)
        self.assertAlmostEqual(43.92137999293259, df['L2-3-1']['reference_flows'], places=6)

        self.assertIsNone(r.get_branch_flows_sensitivity_matrix('aaa'))

    def test_voltage_sa(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        sa = pp.sensitivity.create_ac_analysis()
        sa.set_bus_voltage_factor_matrix(['VLGEN_0'], ['GEN'])
        r = sa.run(n)
        df = r.get_bus_voltages_sensitivity_matrix()
        self.assertEqual((1, 1), df.shape)
        self.assertAlmostEqual(1.0, df['VLGEN_0']['GEN'], places=6)

    def test_exception(self):
        n = pp.network.create_ieee14()
        try:
            n.open_switch("aa")
            self.fail()
        except pp.PyPowsyblError as e:
            self.assertEqual("Switch 'aa' not found", str(e))

    def test_reduce_by_voltage(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.get_buses()))
        n.reduce(v_min=240, v_max=400)
        self.assertEqual(2, len(n.get_buses()))

    def test_reduce_by_ids(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.get_buses()))
        n.reduce(ids=['P2'])
        self.assertEqual(2, len(n.get_buses()))

    def test_reduce_by_subnetwork(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.get_buses()))
        n.reduce(vl_depths=(('VLGEN', 1), ('VLLOAD', 1)))
        self.assertEqual(4, len(n.get_buses()))

    def test_lf_parameters(self):
        parameters = pp.loadflow.Parameters()
        self.assertTrue(parameters.dc_use_transformer_ratio)
        self.assertEqual(0, len(parameters.countries_to_balance))
        self.assertEqual(pp.loadflow.ConnectedComponentMode.MAIN, parameters.connected_component_mode)

        parameters = pp.loadflow.Parameters(dc_use_transformer_ratio = False)
        self.assertFalse(parameters.dc_use_transformer_ratio)
        parameters.dc_use_transformer_ratio = True
        self.assertTrue(parameters.dc_use_transformer_ratio)

        parameters = pp.loadflow.Parameters(countries_to_balance = ['FR'])
        self.assertEqual(['FR'], parameters.countries_to_balance)
        parameters.countries_to_balance = ['BE']
        self.assertEqual(['BE'], parameters.countries_to_balance)

        parameters = pp.loadflow.Parameters(connected_component_mode = pp.loadflow.ConnectedComponentMode.ALL)
        self.assertEqual(pp.loadflow.ConnectedComponentMode.ALL, parameters.connected_component_mode)
        parameters.connected_component_mode = pp.loadflow.ConnectedComponentMode.MAIN
        self.assertEqual(pp.loadflow.ConnectedComponentMode.MAIN, parameters.connected_component_mode)

    def test_create_zone(self):
        n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))

        zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
        self.assertEqual(3, len(zone_fr.injections_ids))
        self.assertEqual(['FFR1AA1 _generator', 'FFR2AA1 _generator', 'FFR3AA1 _generator'], zone_fr.injections_ids)
        self.assertRaises(PyPowsyblError, zone_fr.get_shift_key, 'AA')
        self.assertEqual(2000, zone_fr.get_shift_key('FFR2AA1 _generator'))

        zone_fr = pp.sensitivity.create_country_zone(n, 'FR', pp.sensitivity.ZoneKeyType.GENERATOR_MAX_P)
        self.assertEqual(3, len(zone_fr.injections_ids))
        self.assertEqual(9000, zone_fr.get_shift_key('FFR2AA1 _generator'))

        zone_fr = pp.sensitivity.create_country_zone(n, 'FR', pp.sensitivity.ZoneKeyType.LOAD_P0)
        self.assertEqual(3, len(zone_fr.injections_ids))
        self.assertEqual(['FFR1AA1 _load', 'FFR2AA1 _load', 'FFR3AA1 _load'], zone_fr.injections_ids)
        self.assertEqual(1000, zone_fr.get_shift_key('FFR1AA1 _load'))

        zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
        zone_be = pp.sensitivity.create_country_zone(n, 'BE')

        # remove test
        self.assertEqual(3, len(zone_fr.injections_ids))
        zone_fr.remove_injection('FFR1AA1 _generator')
        self.assertEqual(2, len(zone_fr.injections_ids))

        # add test
        zone_fr.add_injection('gen', 333)
        self.assertEqual(3, len(zone_fr.injections_ids))
        self.assertEqual(333, zone_fr.get_shift_key('gen'))

        # move test
        zone_fr.move_injection_to(zone_be, 'gen')
        self.assertEqual(2, len(zone_fr.injections_ids))
        self.assertEqual(4, len(zone_be.injections_ids))
        self.assertEqual(333, zone_be.get_shift_key('gen'))

    def test_sensi_zone(self):
        n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))
        zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
        zone_be = pp.sensitivity.create_country_zone(n, 'BE')
        sa = pp.sensitivity.create_dc_analysis()
        sa.set_zones([zone_fr, zone_be])
        sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1', 'FFR2AA1  DDE3AA1  1'], ['FR', 'BE'])
        result = sa.run(n)
        s = result.get_branch_flows_sensitivity_matrix()
        self.assertEqual((2, 2), s.shape)
        self.assertEqual(-0.3798285559884689, s['BBE2AA1  FFR3AA1  1']['FR'])
        self.assertEqual(0.3701714440115307, s['FFR2AA1  DDE3AA1  1']['FR'])
        self.assertEqual(0.37842261758908524, s['BBE2AA1  FFR3AA1  1']['BE'])
        self.assertEqual(0.12842261758908563, s['FFR2AA1  DDE3AA1  1']['BE'])
        r = result.get_reference_flows()
        self.assertEqual((1, 2), r.shape)
        self.assertEqual(324.66561396238836, r['BBE2AA1  FFR3AA1  1']['reference_flows'])
        self.assertEqual(1324.6656139623885, r['FFR2AA1  DDE3AA1  1']['reference_flows'])

    def test_sensi_power_transfer(self):
        n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))
        zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
        zone_de = pp.sensitivity.create_country_zone(n, 'DE')
        zone_be = pp.sensitivity.create_country_zone(n, 'BE')
        zone_nl = pp.sensitivity.create_country_zone(n, 'NL')
        sa = pp.sensitivity.create_dc_analysis()
        sa.set_zones([zone_fr, zone_de, zone_be, zone_nl])
        sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1', 'FFR2AA1  DDE3AA1  1'], ['FR', ('FR', 'DE'), ('DE', 'FR'), 'NL'])
        result = sa.run(n)
        s = result.get_branch_flows_sensitivity_matrix()
        self.assertEqual((4, 2), s.shape)
        self.assertEqual(-0.3798285559884689, s['BBE2AA1  FFR3AA1  1']['FR'])
        self.assertEqual(-0.25664095577626006, s['BBE2AA1  FFR3AA1  1']['FR -> DE'])
        self.assertEqual(0.25664095577626006, s['BBE2AA1  FFR3AA1  1']['DE -> FR'])
        self.assertEqual(0.10342626899874961, s['BBE2AA1  FFR3AA1  1']['NL'])


if __name__ == '__main__':
    unittest.main()
