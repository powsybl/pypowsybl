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
from pypowsybl._pypowsybl import DynamicSimulationStatus
import pandas as pd

def test_simulation():
    """
    Running that test requires to have installed dynawo,
    and configured its path in your config.yml.
    """

    network = pp.network.create_ieee14()
    report_node = rp.Reporter()

    model_mapping = dyn.ModelMapping()
    model_mapping.add_base_load(static_id='B3-L', parameter_set_id='LAB', dynamic_model_id='BBM_LOAD', model_name='LoadAlphaBeta')

    generator_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=['B6-G', 'B8-G']),
        data={
            'dynamic_model_id': ['BBM_GEN6', 'BBM_GEN8'],
            'parameter_set_id': ['GSTWPR_GEN____6_SM', 'GSTWPR_GEN____8_SM'],
            'model_name': 'GeneratorSynchronousThreeWindingsProportionalRegulations'
        }
    )
    model_mapping.add_synchronous_generator(generator_mapping_df)

    event_mapping = dyn.EventMapping()
    event_mapping.add_disconnection(static_id='L1-2-1', start_time=5, disconnect_only='TWO')
    event_mapping.add_active_power_variation(static_id='B3-L', start_time=4, delta_p=0.02)

    variables_mapping = dyn.OutputVariableMapping()
    variables_mapping.add_dynamic_model_curves('BBM_GEN6', ['generator_PGen', 'generator_QGen', 'generator_UStatorPu'])
    variables_mapping.add_standard_model_final_state_values('B3', 'Upu_value')

    sim = dyn.Simulation()
    res = sim.run(network, model_mapping, event_mapping, variables_mapping, 0, 100, report_node)

    assert report_node
    assert DynamicSimulationStatus.SUCCESS == res.status()
    assert "" == res.status_text()
    assert 'BBM_GEN6_generator_PGen' in res.curves()
    assert 'BBM_GEN6_generator_QGen' in res.curves()
    assert 'BBM_GEN6_generator_UStatorPu' in res.curves()
    assert False == res.final_state_values().loc['NETWORK_B3_Upu_value'].empty
    assert False == res.timeline().empty
