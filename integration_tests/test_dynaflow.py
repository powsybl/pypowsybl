#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp

from pypowsybl.loadflow import (
    run_ac,
    ComponentStatus
)


def test_dynaflow_ieee9():
    """
    Running that test requires to have installed dynaflow,
    and configured its path in your config.yml.
    """
    network = pp.network.create_ieee9()
    assert network.get_generators()['p'].isna().all()  # No data for computed P

    res = run_ac(network, provider='DynaFlow')
    assert res[0].status == ComponentStatus.CONVERGED
    # checks that computed P is almost equal to target_p
    gens = network.get_generators()
    assert max(abs(gens.p + gens.target_p)) < 0.1

