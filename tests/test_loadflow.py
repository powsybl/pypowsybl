#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest

import pypowsybl as pp
import pypowsybl.loadflow as lf
from pypowsybl.loadflow import ValidationType


class LoadflowTestCase(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_run_lf(self):
        n = pp.network.create_ieee14()
        results = lf.run_ac(n)
        self.assertEqual(1, len(results))
        self.assertEqual(lf.ComponentStatus.CONVERGED, results[0].status)
        self.assertEqual(0, results[0].connected_component_num)
        self.assertEqual(0, results[0].synchronous_component_num)
        self.assertEqual('VL4_0', results[0].slack_bus_id)
        self.assertAlmostEqual(0.0, results[0].slack_bus_active_power_mismatch, 1)
        self.assertEqual(3, results[0].iteration_count)

        parameters = lf.Parameters(distributed_slack=False)
        results = lf.run_dc(n, parameters)
        self.assertEqual(1, len(results))

    def test_lf_parameters(self):
        parameters = lf.Parameters()
        self.assertTrue(parameters.dc_use_transformer_ratio)
        self.assertEqual(0, len(parameters.countries_to_balance))
        self.assertEqual(lf.ConnectedComponentMode.MAIN, parameters.connected_component_mode)

        # Testing setting independently every attributes
        attributes = {
            'voltage_init_mode': [lf.VoltageInitMode.DC_VALUES, lf.VoltageInitMode.UNIFORM_VALUES],
            'transformer_voltage_control_on': [True, False],
            'no_generator_reactive_limits': [True, False],
            'phase_shifter_regulation_on': [True, False],
            'twt_split_shunt_admittance': [True, False],
            'simul_shunt': [True, False],
            'read_slack_bus': [True, False],
            'write_slack_bus': [True, False],
            'distributed_slack': [True, False],
            'balance_type': [lf.BalanceType.PROPORTIONAL_TO_CONFORM_LOAD, lf.BalanceType.PROPORTIONAL_TO_GENERATION_P],
            'dc_use_transformer_ratio': [True, False],
            'countries_to_balance': [['FR'], ['BE']],
            'connected_component_mode': [lf.ConnectedComponentMode.MAIN, lf.ConnectedComponentMode.ALL]
        }

        for attribute, values in attributes.items():
            for value in values:
                parameters = lf.Parameters(**dict([(attribute, value)]))
                self.assertEqual(value, getattr(parameters, attribute))

                parameters = lf.Parameters()
                setattr(parameters, attribute, value)
                self.assertEqual(value, getattr(parameters, attribute))

    def test_validation(self):
        n = pp.network.create_ieee14()
        pp.loadflow.run_ac(n)
        validation = pp.loadflow.run_validation(n, [ValidationType.FLOWS, ValidationType.GENERATORS, ValidationType.BUSES])
        self.assertAlmostEqual(-232.4, validation.generators['p']['B1-G'], delta=0.00001)
        self.assertAlmostEqual(-47.80608, validation.buses['incoming_p']['VL4_0'], delta=0.00001)
        self.assertAlmostEqual(156.887407, validation.branch_flows['p1']['L1-2-1'], delta=0.00001)
        self.assertFalse(validation.valid)
        n2 = pp.network.create_four_substations_node_breaker_network()
        pp.loadflow.run_ac(n)
        validation2 = pp.loadflow.run_validation(n2, [ValidationType.SVCS])
        self.assertEqual(1, len(validation2.svcs))
        self.assertTrue(validation2.svcs['validated']['SVC'])

    def test_twt_validation(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        validation = pp.loadflow.run_validation(n, [ValidationType.TWTS])
        self.assertAlmostEqual(-10.421382, validation.twts['error']['NHV2_NLOAD'], delta=0.00001)
        self.assertTrue(validation.valid)

    def test_validation_all(self):
        n = pp.network.create_ieee14()
        pp.loadflow.run_ac(n)
        validation = pp.loadflow.run_validation(n)
        self.assertIsNotNone(validation.buses)
        self.assertIsNotNone(validation.generators)
        self.assertIsNotNone(validation.branch_flows)
        self.assertIsNotNone(validation.svcs)
        self.assertIsNotNone(validation.shunts)
        self.assertIsNotNone(validation.t3wts)
        self.assertIsNotNone(validation.twts)


if __name__ == '__main__':
    unittest.main()
