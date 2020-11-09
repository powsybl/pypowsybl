#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import gridpy


class GridPyTestCase(unittest.TestCase):
    def test_hello(self):
        gridpy.hello()


if __name__ == '__main__':
    unittest.main()
