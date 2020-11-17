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
import gridpy as gp


class GridPyTestCase(unittest.TestCase):
    def test_print_version(self):
        gp.print_version()

    def test_create_empty_network(self):
        n = gp.network.create_empty("test")
        self.assertIsNotNone(n)

    def test_run_lf(self):
        n = gp.network.create_ieee14()
        r = gp.loadflow.run(n)
        self.assertTrue(r.is_ok())

    def test_load_network(self):
        dir = os.path.dirname(os.path.realpath(__file__))
        n = gp.network.load(dir + "/empty-network.xml")
        self.assertIsNotNone(n)

    def test_connect_disconnect(self):
        n = gp.network.create_ieee14()
        n.disconnect('L1-2-1')
        n.connect('L1-2-1')


if __name__ == '__main__':
    unittest.main()
