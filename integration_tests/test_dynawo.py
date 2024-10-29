#
# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl as pp
import pypowsybl.dynamic as dyn
import pypowsybl.report as rp

def test_simulation():
    """
    Running that test requires to have installed dynawo,
    and configured its path in your config.yml.
    """
    network = pp.network.create_ieee14()
    report_node = rp.Reporter()

    model_mapping = dyn.ModelMapping()
    model_mapping.add_base_load("_LOAD__10_EC", "LAB", "BBM_LOAD", "LoadAlphaBeta")
    model_mapping.add_synchronous_generator("_GEN____6_SM", "GSTWPR_GEN____6_SM", "BBM_GEN6", "GeneratorSynchronousThreeWindingsProportionalRegulations")
    model_mapping.add_synchronous_generator("_GEN____8_SM", "GSTWPR_GEN____8_SM", "BBM_GEN8", "GeneratorSynchronousThreeWindingsProportionalRegulations")

    event_mapping = dyn.EventMapping()
    event_mapping.add_disconnection("_BUS____1-BUS____5-1_AC", 1, "TWO")

    curves_mapping = dyn.CurveMapping()
    curves_mapping.add_curves("BBM_LOAD", ["load_PPu", "load_QPu"])

    sim = dyn.Simulation()
    res = sim.run(network, model_mapping, event_mapping, curves_mapping, 1, 10, report_node)

    assert 'Ok' == res.status()
    assert 'BBM_LOAD_load_PPu' in res.curves()
    assert 'BBM_LOAD_load_QPu' in res.curves()