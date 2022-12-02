#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import json

import pypowsybl as pp
import pypowsybl.dynamic as dyn
from pypowsybl._pypowsybl import LoadFlowComponentStatus
from pypowsybl.loadflow import ValidationType
import pytest
import pypowsybl.report as rp


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)

#TODO: Define a small but meaningful IIDM file as string and parse it on set up

def test_get_possible_events():
    assert set(dyn.EventMapping.get_possible_events()) == set([dyn.EventType.QUADRIPOLE_DISCONNECT,
            dyn.EventType.SET_POINT_BOOLEAN, dyn.EventType.BRANCH_DISCONNECTION])

def test_add_get_alpha_beta_load():
    # Add a load.
    # Get the load we just added
    # Assert that the id are correct
    # Assert that the characteristics from the getter are those from the IIDM