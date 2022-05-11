#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from pypowsybl import *

def test_star_import():
    assert security is not None
    assert sensitivity is not None
    assert network is not None
    assert loadflow is not None

