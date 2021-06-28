#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp
from pypowsybl import PyPowsyblError
import pathlib

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')


class SensitivityAnalysisTestCase(unittest.TestCase):

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

    def test_voltage_sensitivities(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        sa = pp.sensitivity.create_ac_analysis()
        sa.set_bus_voltage_factor_matrix(['VLGEN_0'], ['GEN'])
        r = sa.run(n)
        df = r.get_bus_voltages_sensitivity_matrix()
        self.assertEqual((1, 1), df.shape)
        self.assertAlmostEqual(1.0, df['VLGEN_0']['GEN'], places=6)

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
        sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1', 'FFR2AA1  DDE3AA1  1'],
                                         ['FR', ('FR', 'DE'), ('DE', 'FR'), 'NL'])
        result = sa.run(n)
        s = result.get_branch_flows_sensitivity_matrix()
        self.assertEqual((4, 2), s.shape)
        self.assertEqual(-0.3798285559884689, s['BBE2AA1  FFR3AA1  1']['FR'])
        self.assertEqual(-0.25664095577626006, s['BBE2AA1  FFR3AA1  1']['FR -> DE'])
        self.assertEqual(0.25664095577626006, s['BBE2AA1  FFR3AA1  1']['DE -> FR'])
        self.assertEqual(0.10342626899874961, s['BBE2AA1  FFR3AA1  1']['NL'])


if __name__ == '__main__':
    unittest.main()
