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
        importer = pp.glsk.GLSKImporter("C:/Users/brix/Documents/glsk-sample.xml")
        countries = importer.get_countries()
        t = dateutil.parser.isoparse('2020-10-06T22:00:00Z')
        print("GLSK start timestamp : ")
        print(importer.get_gsk_time_interval_start())
        print("GLSK end timestamp : ")
        print(importer.get_gsk_time_interval_end())
        print("Keys for country : ")
        print(countries)
        points = importer.get_points_for_country(countries[1], t.timestamp())
        print("Points keys : ")
        print(points)
        factors = importer.get_glsk_factors(countries[1], t.timestamp())
        print("Factors for country : ")
        print(factors)

