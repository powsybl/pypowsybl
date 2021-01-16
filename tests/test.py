#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import os
import unittest
import gridpy.network
import gridpy.loadflow
import gridpy.security_analysis
import gridpy.sensitivity_analysis
import gridpy as gp


class GridPyTestCase(unittest.TestCase):
    @staticmethod
    def test_print_version():
        gp.print_version()

    def test_create_empty_network(self):
        n = gp.network.create_empty("test")
        self.assertIsNotNone(n)

    def test_run_lf(self):
        n = gp.network.create_ieee14()
        results = gp.loadflow.run_ac(n)
        self.assertEqual(1, len(results))
        self.assertEqual(gp.loadflow.ComponentStatus.CONVERGED, list(results)[0].status)
        parameters = gp.loadflow.Parameters(distributed_slack=False)
        results = gp.loadflow.run_dc(n, parameters)
        self.assertEqual(1, len(results))

    def test_load_network(self):
        dir = os.path.dirname(os.path.realpath(__file__))
        n = gp.network.load(dir + "/empty-network.xml")
        self.assertIsNotNone(n)

    def test_get_buses(self):
        n = gp.network.create_ieee14()
        self.assertEqual(14, len(n.get_buses()))
        self.assertEqual(14, len(n.get_buses(bus_breaker_view=True)))

    def test_connect_disconnect(self):
        n = gp.network.create_ieee14()
        self.assertTrue(n.disconnect('L1-2-1'))
        self.assertTrue(n.connect('L1-2-1'))

    def test_security_analysis(self):
        n = gp.network.create_eurostag_tutorial_example1_network()
        sa = gp.security_analysis.create()
        sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
#        sa_result = sa.run(n)
#        self.assertEqual(1, sa_result.contingencies)

    def test_get_network_element_ids(self):
        n = gp.network.create_eurostag_tutorial_example1_network()
        self.assertEqual(['NGEN_NHV1', 'NHV2_NLOAD'], n.get_elements_ids(gp.network.ElementType.TWO_WINDINGS_TRANSFORMER))
        self.assertEqual(['NGEN_NHV1'], n.get_elements_ids(gp.network.ElementType.TWO_WINDINGS_TRANSFORMER, 24))

    def test_sensitivity_analysis(self):
        n = gp.network.create_ieee14()
        sa = gp.sensitivity_analysis.create()
        sa.add_single_element_contingency('L1-2-1')
        sa.set_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G'])
        r = sa.run_dc(n)
        m = r.get_sensitivity_matrix()
        self.assertEqual((2, 2), m.shape)
        self.assertEqual(0.08099067519128486, m[0, 0])
        self.assertEqual(-0.013674968450008108, m[0, 1])
        self.assertEqual(-0.08099067519128486, m[1, 0])
        self.assertEqual(0.013674968450008108, m[1, 1])
        m2 = r.get_post_contingency_sensitivity_matrix('L1-2-1')
        self.assertEqual((2, 2), m2.shape)
        self.assertEqual(0.49999999999999994, m2[0, 0])
        self.assertEqual(-0.08442310437411704, m2[0, 1])
        self.assertEqual(-0.49999999999999994, m2[1, 0])
        self.assertEqual(0.08442310437411704, m2[1, 1])
        self.assertIsNone(r.get_post_contingency_sensitivity_matrix('aaa'))

    def test_exception(self):
        n = gp.network.create_ieee14()
        try:
            n.open_switch("aa")
            self.fail()
        except RuntimeError as e:
            self.assertEqual("Switch 'aa' not found", str(e))


if __name__ == '__main__':
    unittest.main()
