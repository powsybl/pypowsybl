#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import os
import unittest
import pypowsybl.network
import pypowsybl.loadflow
import pypowsybl.security_analysis
import pypowsybl.sensitivity_analysis
import pypowsybl as pp


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

    def test_load_network(self):
        dir = os.path.dirname(os.path.realpath(__file__))
        n = pp.network.load(dir + "/empty-network.xml")
        self.assertIsNotNone(n)

    def test_buses(self):
        n = pp.network.create_ieee14()
        self.assertEqual(14, len(n.buses))
        b = list(n.buses)[0]
        self.assertEqual('VL1_0', b.id)
        self.assertEqual(1.06, b.v_magnitude)
        self.assertEqual(0.0, b.v_angle)
        self.assertEqual(0, b.component_num)

    def test_generators(self):
        n = pp.network.create_ieee14()
        self.assertEqual(5, len(n.generators))
        g = list(n.generators)[0]
        self.assertEqual('B1-G', g.id)
        self.assertEqual(232.4, g.target_p)
        self.assertEqual(-9999.0, g.min_p)
        self.assertEqual(9999.0, g.max_p)
        self.assertEqual(1.0, g.nominal_voltage)
        self.assertIsNone(g.country)
        self.assertIsNotNone(g.bus)
        self.assertEqual('VL1_0', g.bus.id)

    def test_loads(self):
        n = pp.network.create_ieee14()
        self.assertEqual(11, len(n.loads))
        l = list(n.loads)[0]
        self.assertEqual('B2-L', l.id)
        self.assertEqual(21.7, l.p0)
        self.assertEqual(1.0, l.nominal_voltage)
        self.assertIsNone(l.country)
        self.assertIsNotNone(l.bus)
        self.assertEqual('VL2_0', l.bus.id)

    def test_connect_disconnect(self):
        n = pp.network.create_ieee14()
        self.assertTrue(n.disconnect('L1-2-1'))
        self.assertTrue(n.connect('L1-2-1'))

    def test_security_analysis(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        sa = pp.security_analysis.create()
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

    def test_create_generators_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        df = n.create_generators_data_frame()
        self.assertEqual('OTHER', df['energy_source']['GEN'])
        self.assertEqual(607, df['target_p']['GEN'])

    def test_create_2_windings_transformers_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        df = n.create_2_windings_transformers_data_frame()
        self.assertEqual(1, df['ratio_tap_position']['NHV2_NLOAD'])
        self.assertEqual(-99999, df['phase_tap_position']['NHV2_NLOAD'])

    def test_voltage_levels_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        df = n.create_voltage_levels_data_frame()
        self.assertEqual(24.0, df['nominal_v']['VLGEN'])

    def test_substations_data_frame(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        df = n.create_substations_data_frame()
        self.assertEqual('RTE', df['TSO']['P1'])
        self.assertEqual('FR', df['country']['P1'])

    def test_sensitivity_analysis(self):
        n = pp.network.create_ieee14()
        sa = pp.sensitivity_analysis.create()
        sa.add_single_element_contingency('L1-2-1')
        sa.set_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
        r = sa.run_dc(n)

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

        df = r.get_post_contingency_branch_flows_sensitivity_matrix('L1-2-1')
        self.assertEqual((3, 2), df.shape)
        self.assertEqual(0.49999999999999994, df['L1-5-1']['B1-G'])
        self.assertEqual(-0.49999999999999994, df['L1-5-1']['B2-G'])
        self.assertEqual(-0.49999999999999994, df['L1-5-1']['B3-G'])
        self.assertEqual(-0.08442310437411704, df['L2-3-1']['B1-G'])
        self.assertEqual(0.08442310437411704, df['L2-3-1']['B2-G'])
        self.assertEqual(-0.49038517950037847, df['L2-3-1']['B3-G'])

        df = r.get_post_contingency_reference_flows('L1-2-1')
        self.assertEqual((1, 2), df.shape)
        self.assertAlmostEqual(225.69999999999996, df['L1-5-1']['reference_flows'], places=6)
        self.assertAlmostEqual(43.92137999293259, df['L2-3-1']['reference_flows'], places=6)

        self.assertIsNone(r.get_post_contingency_branch_flows_sensitivity_matrix('aaa'))

    def test_voltage_sa(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        sa = pp.sensitivity_analysis.create()
        sa.set_bus_voltage_factor_matrix(['VLGEN_0'], ['GEN'])
        r = sa.run_ac(n)
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
        self.assertEqual(4, len(n.buses))
        n.reduce(v_min=240, v_max=400)
        self.assertEqual(2, len(n.buses))

    def test_reduce_by_ids(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.buses))
        n.reduce(ids=['P2'])
        self.assertEqual(2, len(n.buses))

    def test_reduce_by_subnetwork(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.buses))
        n.reduce(vl_depths=(('VLGEN', 1), ('VLLOAD', 1)))
        self.assertEqual(4, len(n.buses))


if __name__ == '__main__':
    unittest.main()
