#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp
import dateutil.parser

class GLSKImportTestCases(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_import_glsk(self):
        importer = pp.glsk.GLSKImporter("data/glsk-sample.xml")
        countries = importer.get_countries()
        t = dateutil.parser.isoparse('2020-10-06T22:00:00Z')
        print("GLSK start timestamp : ")
        print(importer.get_gsk_time_interval_start())
        print("GLSK end timestamp : ")
        print(importer.get_gsk_time_interval_end())
        print("Keys for country : ")
        print(countries)
        points = importer.get_points_for_country(countries[2], t.timestamp())
        print("Points keys : ")
        print(points)
        factors = importer.get_glsk_factors(countries[2], t.timestamp())
        print("Factors for country : ")
        print(factors)

    def test_network(self):
        n = pp.network.load('data/simple-eu.uct')
        t = dateutil.parser.isoparse('2020-10-06T22:00:00Z')
        importer = pp.glsk.GLSKImporter("data/glsk-sample.xml")
        de_generators = importer.get_points_for_country('10YCB-GERMANY--8', t.timestamp())
        de_shift_keys = importer.get_glsk_factors('10YCB-GERMANY--8', t.timestamp())

        #Rename some of the generator to names from simple-eu.uct
        de_generators[0] = 'DDE1AA1'
        de_generators[1] = 'DDE2AA1'
        de_generators[2] = 'DDE3AA1'

        print("Germany generators : ")
        print(de_generators)
        print("Germany factors : ")
        print(de_shift_keys)

        zone_de = pp.sensitivity.create_country_zone_generator(n, 'DE', de_generators, de_shift_keys)
        print(zone_de.shift_keys_by_injections_ids)

