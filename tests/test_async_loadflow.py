#
# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pytest

import pypowsybl as pp
import pypowsybl.loadflow as lf


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)


@pytest.mark.asyncio
async def test_run_lf_ac_async():
    n = pp.network.create_ieee14()
    results = await lf.run_ac_async(n)
    assert 1 == len(results)
    assert lf.ComponentStatus.CONVERGED == results[0].status
    assert 'Converged' == results[0].status_text
    assert 0 == results[0].connected_component_num
    assert 0 == results[0].synchronous_component_num
    assert 'VL1_0' == results[0].reference_bus_id
    assert 1 == len(results[0].slack_bus_results)
    assert 'VL1_0' == results[0].slack_bus_results[0].id
    assert abs(results[0].slack_bus_results[0].active_power_mismatch) < 0.01
    assert 3 == results[0].iteration_count
