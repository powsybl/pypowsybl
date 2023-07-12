#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp
import pypowsybl.openreac as openreac
import pytest
import pandas as pd


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)

def test_parameters():
    params = openreac.OpenReacParameters()
    params.add_variable_shunt_compensators(["shunt1", "shunt2"])
    params.add_constant_q_generators(["gen1", "gen2"])
    params.add_variable_two_windings_transformers(["twt1", "twt2"])

    params.add_algorithm_param([("foo", "bar"), ("bar", "bar2")])
    params.add_specific_voltage_limits({"vl_id": (0.5,1.2)})


# can't test runner because it need ampl and knitro.