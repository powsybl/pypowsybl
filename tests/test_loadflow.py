#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp

class LoadflowTestCase(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_run_lf(self):
        n = pp.network.create_ieee14()
        results = pp.loadflow.run_ac(n)
        self.assertEqual(1, len(results))
        self.assertEqual(pp.loadflow.ComponentStatus.CONVERGED, results[0].status)
        parameters = pp.loadflow.Parameters(distributed_slack=False)
        results = pp.loadflow.run_dc(n, parameters)
        self.assertEqual(1, len(results))

    def test_lf_parameters(self):
        parameters = pp.loadflow.Parameters()
        self.assertTrue(parameters.dc_use_transformer_ratio)
        self.assertEqual(0, len(parameters.countries_to_balance))
        self.assertEqual(pp.loadflow.ConnectedComponentMode.MAIN, parameters.connected_component_mode)

        parameters = pp.loadflow.Parameters(dc_use_transformer_ratio=False)
        self.assertFalse(parameters.dc_use_transformer_ratio)
        parameters.dc_use_transformer_ratio = True
        self.assertTrue(parameters.dc_use_transformer_ratio)

        parameters = pp.loadflow.Parameters(countries_to_balance=['FR'])
        self.assertEqual(['FR'], parameters.countries_to_balance)
        parameters.countries_to_balance = ['BE']
        self.assertEqual(['BE'], parameters.countries_to_balance)

        parameters = pp.loadflow.Parameters(connected_component_mode=pp.loadflow.ConnectedComponentMode.ALL)
        self.assertEqual(pp.loadflow.ConnectedComponentMode.ALL, parameters.connected_component_mode)
        parameters.connected_component_mode = pp.loadflow.ConnectedComponentMode.MAIN
        self.assertEqual(pp.loadflow.ConnectedComponentMode.MAIN, parameters.connected_component_mode)


if __name__ == '__main__':
    unittest.main()
