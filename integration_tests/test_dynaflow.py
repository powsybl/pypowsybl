#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp
import pypowsybl.loadflow as lf
import pypowsybl.report as rp


def test_dynaflow_ieee9():
    """
    Running that test requires to have installed dynaflow,
    and configured its path in your config.yml.
    """
    network = pp.network.create_ieee9()
    assert network.get_generators()['p'].isna().all()  # No data for computed P

    res = lf.run_ac(network, provider='DynaFlow')
    assert res[0].status == lf.ComponentStatus.CONVERGED
    # checks that computed P is almost equal to target_p
    gens = network.get_generators()
    assert max(abs(gens.p + gens.target_p)) < 0.1

def test_check_loadflow_parameters():
    assert lf.check_loadflow_parameters(provider='DynaFlow')
    report_node = rp.ReportNode()
    parameters = lf.Parameters(distributed_slack=False, dc=True)
    assert not lf.check_loadflow_parameters(parameters=parameters, provider='DynaFlow', report_node=report_node)
    assert report_node
    print(report_node)
