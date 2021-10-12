#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp
import pathlib

TEST_DIR = pathlib.Path(__file__).parent


class NetworkReductionTestCase(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_reduce_by_voltage(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.get_buses()))
        n.reduce(v_min=240, v_max=400)
        self.assertEqual(2, len(n.get_buses()))

    def test_reduce_by_ids(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.get_buses()))
        n.reduce(ids=['P2'])
        self.assertEqual(2, len(n.get_buses()))

    def test_reduce_by_subnetwork(self):
        n = pp.network.create_eurostag_tutorial_example1_network()
        pp.loadflow.run_ac(n)
        self.assertEqual(4, len(n.get_buses()))
        n.reduce(vl_depths=(('VLGEN', 1), ('VLLOAD', 1)))
        self.assertEqual(4, len(n.get_buses()))


if __name__ == '__main__':
    unittest.main()
