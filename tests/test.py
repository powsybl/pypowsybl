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
        results = gp.loadflow.run(n)
        self.assertEqual(1, len(results))
        self.assertEqual(gp.LoadFlowComponentStatus.CONVERGED, list(results)[0].status)
        results = gp.loadflow.run(n, distributed_slack=False, dc=True)
        self.assertEqual(0, len(results))

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
        sa.add_contingency('First contingency', 'L1-2-1')
#        sa_result = sa.run(n)
#        self.assertEqual(1, sa_result.contingencies)


if __name__ == '__main__':
    unittest.main()
