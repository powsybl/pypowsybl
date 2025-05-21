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
