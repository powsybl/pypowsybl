#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
from pypowsybl import *


class PyPowsyblImportTestCase(unittest.TestCase):
    """
    Only here to ensure star import works
    """

    def test_star_import(self):
        self.assertIsNotNone(security)
        self.assertIsNotNone(sensitivity)
        self.assertIsNotNone(network)
        self.assertIsNotNone(loadflow)


if __name__ == '__main__':
    unittest.main()
