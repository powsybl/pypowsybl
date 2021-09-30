#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp
import pandas as pd
from pypowsybl import PyPowsyblError
import pathlib

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')


class SensitivityAnalysisTestCase(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_sensitivity_analysis(self):
        n = pp.network.create_ieee14()
        # fix max_p to be less than olf max_p plausible value
        # (otherwise generators will be discarded from slack distribution)
        generators = pd.DataFrame(data=[4999.0, 4999.0, 4999.0, 4999.0, 4999.0],
                                  columns=['max_p'], index=['B1-G', 'B2-G', 'B3-G', 'B6-G', 'B8-G'])
        n.update_generators(generators)
        sa = pp.sensitivity.create_dc_analysis()
        sa.add_single_element_contingency('L1-2-1')
        sa.set_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
        r = sa.run(n)

        df = r.get_branch_flows_sensitivity_matrix()
        self.assertEqual((3, 2), df.shape)
        self.assertAlmostEqual(0.080991, df['L1-5-1']['B1-G'], places=6)
        self.assertAlmostEqual(-0.080991, df['L1-5-1']['B2-G'], places=6)
        self.assertAlmostEqual(-0.172498, df['L1-5-1']['B3-G'], places=6)
        self.assertAlmostEqual(-0.013675, df['L2-3-1']['B1-G'], places=6)
        self.assertAlmostEqual(0.013675, df['L2-3-1']['B2-G'], places=6)
        self.assertAlmostEqual(-0.545683, df['L2-3-1']['B3-G'], places=6)

        df = r.get_reference_flows()
        self.assertEqual((1, 2), df.shape)
        self.assertAlmostEqual(72.247, df['L1-5-1']['reference_flows'], places=3)
        self.assertAlmostEqual(69.831, df['L2-3-1']['reference_flows'], places=3)

        df = r.get_branch_flows_sensitivity_matrix('L1-2-1')
        self.assertEqual((3, 2), df.shape)
        self.assertAlmostEqual(0.5, df['L1-5-1']['B1-G'], places=6)
        self.assertAlmostEqual(-0.5, df['L1-5-1']['B2-G'], places=6)
        self.assertAlmostEqual(-0.5, df['L1-5-1']['B3-G'], places=6)
        self.assertAlmostEqual(-0.084423, df['L2-3-1']['B1-G'], places=6)
        self.assertAlmostEqual(0.084423, df['L2-3-1']['B2-G'], places=6)
        self.assertAlmostEqual(-0.490385, df['L2-3-1']['B3-G'], places=6)

        df = r.get_reference_flows('L1-2-1')
        self.assertEqual((1, 2), df.shape)
        self.assertAlmostEqual(225.7, df['L1-5-1']['reference_flows'], places=3)
        self.assertAlmostEqual(43.921, df['L2-3-1']['reference_flows'], places=3)

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
        self.assertEqual(4999, zone_fr.get_shift_key('FFR2AA1 _generator'))

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
        self.assertAlmostEqual(-0.379829, s['BBE2AA1  FFR3AA1  1']['FR'], places=6)
        self.assertAlmostEqual(0.370171, s['FFR2AA1  DDE3AA1  1']['FR'], places=6)
        self.assertAlmostEqual(0.378423, s['BBE2AA1  FFR3AA1  1']['BE'], places=6)
        self.assertAlmostEqual(0.128423, s['FFR2AA1  DDE3AA1  1']['BE'], places=6)
        r = result.get_reference_flows()
        self.assertEqual((1, 2), r.shape)
        self.assertAlmostEqual(324.666, r['BBE2AA1  FFR3AA1  1']['reference_flows'], places=3)
        self.assertAlmostEqual(1324.666, r['FFR2AA1  DDE3AA1  1']['reference_flows'], places=3)

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
        self.assertAlmostEqual(-0.379829, s['BBE2AA1  FFR3AA1  1']['FR'], places=6)
        self.assertAlmostEqual(-0.256641, s['BBE2AA1  FFR3AA1  1']['FR -> DE'], places=6)
        self.assertAlmostEqual(0.256641, s['BBE2AA1  FFR3AA1  1']['DE -> FR'], places=6)
        self.assertAlmostEqual(0.103426, s['BBE2AA1  FFR3AA1  1']['NL'], places=6)

    def test_xnode_sensi(self):
        n = pp.network.load(str(DATA_DIR.joinpath('simple-eu-xnode.uct')))
        # assert there is one dangling line (corresponding to the UCTE xnode)
        dangling_lines = n.get_dangling_lines()
        self.assertEqual(1, len(dangling_lines))
        # create a new zone with only one xnode, this is the dangling line id that has to be configured (corresponding
        # to the line connecting the xnode in the UCTE file)
        zone_x = pp.sensitivity.create_empty_zone("X")
        zone_x.add_injection('NNL2AA1  XXXXXX11 1')
        sa = pp.sensitivity.create_dc_analysis()
        sa.set_zones([zone_x])
        sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1'], ['X'])
        result = sa.run(n)
        s = result.get_branch_flows_sensitivity_matrix()
        self.assertEqual((1, 1), s.shape)
        self.assertAlmostEqual(0.176618, s['BBE2AA1  FFR3AA1  1']['X'], places=6)


if __name__ == '__main__':
    unittest.main()
