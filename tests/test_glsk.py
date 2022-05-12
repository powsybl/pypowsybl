#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp
import dateutil.parser
import datetime

class GLSKImportTestCases(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_import_glsk(self):
        importer = pp.glsk.GLSKImporter("../data/glsk_sample.xml")
        self.assertEqual(['10YFR-RTE------C', '10YNL----------L', '10YBE----------2', '10YCB-GERMANY--8'], importer.get_countries())
        self.assertEqual(datetime.datetime(2019, 1, 8, 0, 0), importer.get_gsk_time_interval_start())
        self.assertEqual(datetime.datetime(2019, 1, 9, 0, 0), importer.get_gsk_time_interval_end())
        t = importer.get_gsk_time_interval_start()
        self.assertEqual(['FFR1AA1 ', 'FFR2AA1 ', 'FFR3AA1 '], importer.get_points_for_country('10YFR-RTE------C', t))
        self.assertEqual([0.0158, 0.1299, 0.1299], importer.get_glsk_factors('10YFR-RTE------C', t))

    def test_zone(self):
        n = pp.network.load('../data/simple-eu.uct')
        importer = pp.glsk.GLSKImporter("../data/glsk_sample.xml")
        t = importer.get_gsk_time_interval_start()
        de_generators = importer.get_points_for_country('10YCB-GERMANY--8', t)
        de_shift_keys = importer.get_glsk_factors('10YCB-GERMANY--8', t)

        self.assertEqual(['DDE1AA1 ', 'DDE2AA1 ', 'DDE3AA1 '], de_generators)
        self.assertEqual([0.0278, 0.0062, 0.0133], de_shift_keys)
        zone_de = pp.sensitivity.create_country_zone_generator(n, 'DE', de_generators, de_shift_keys)
        self.assertEqual({'DDE1AA1 ': 0.0278, 'DDE2AA1 ': 0.0062, 'DDE3AA1 ': 0.0133}, zone_de.shift_keys_by_injections_ids)

    def test_zones(self):
        n = pp.network.load('../data/simple-eu.uct')
        zones = pp.sensitivity.create_zones_from_glsk_file(n, "../data/glsk_sample.xml", datetime.datetime(2019, 1, 8, 0, 0))
        zone_fr = next(z for z in zones if z.id == '10YFR-RTE------C')
        zone_nl = next(z for z in zones if z.id == '10YNL----------L')
        zone_be = next(z for z in zones if z.id == '10YBE----------2')
        zone_de = next(z for z in zones if z.id == '10YCB-GERMANY--8')
        self.assertEqual({'FFR1AA1 ': 0.0158, 'FFR2AA1 ': 0.1299, 'FFR3AA1 ': 0.1299}, zone_fr.shift_keys_by_injections_ids)
        self.assertEqual({'NNL1AA1 ': 0.0641, 'NNL2AA1 ': 0.0184, 'NNL3AA1 ': 0.1003}, zone_nl.shift_keys_by_injections_ids)
        self.assertEqual({'BBE1AA1 ': 0.0641, 'BBE2AA1 ': 0.0145, 'BBE3AA1 ': 0.0145}, zone_be.shift_keys_by_injections_ids)
        self.assertEqual({'DDE1AA1 ': 0.0278, 'DDE2AA1 ': 0.0062, 'DDE3AA1 ': 0.0133}, zone_de.shift_keys_by_injections_ids)


