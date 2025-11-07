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
from pathlib import Path

def test_simulation():
    """
    Running that test requires to have installed dynawo,
    and configured its path in your config.yml.
    """

    network = pp.network.create_ieee14()
    report_node = rp.ReportNode()

    model_mapping = dyn.ModelMapping()
    model_mapping.add_base_load(static_id='B3-L', parameter_set_id='LAB', model_name='LoadAlphaBeta')

    generator_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=['B6-G', 'B8-G']),
        data={
            'parameter_set_id': ['GSTWPR_6', 'GSTWPR_8'],
            'model_name': 'GeneratorSynchronousThreeWindingsProportionalRegulations'
        }
    )
    model_mapping.add_synchronous_generator(generator_mapping_df)

    event_mapping = dyn.EventMapping()
    event_mapping.add_disconnection(static_id='L1-2-1', start_time=5, disconnect_only='TWO')
    event_mapping.add_active_power_variation(static_id='B3-L', start_time=4, delta_p=0.02)

    variables_mapping = dyn.OutputVariableMapping()
    variables_mapping.add_dynamic_model_curves(dynamic_model_id='B6-G', variables=['generator_PGen', 'generator_QGen', 'generator_UStatorPu'])
    variables_mapping.add_standard_model_final_state_values(static_id='B3', variables='Upu_value')
    testPath = Path(__file__).parent
    dynawo_param = {
        'parametersFile': str(testPath.joinpath('models.par')),
        'network.parametersFile': str(testPath.joinpath('network.par')),
        'network.parametersId': 'Network',
        'solver.parametersFile': str(testPath.joinpath('solvers.par')),
        'solver.parametersId': 'IDA',
        'solver.type': 'IDA',
        'precision': '1e-5'
    }
    param = dyn.Parameters(start_time=0, stop_time=100, provider_parameters=dynawo_param)

    sim = dyn.Simulation()
    res = sim.run(network, model_mapping, event_mapping, variables_mapping, param, report_node)

    assert report_node
    assert DynamicSimulationStatus.SUCCESS == res.status()
    assert "" == res.status_text()
    curves_df = res.curves()
    assert '1970-01-01T00:00:00Z' == curves_df.index.values[0]
    assert 'B6-G_generator_PGen' in curves_df
    assert 'B6-G_generator_QGen' in curves_df
    assert 'B6-G_generator_UStatorPu' in curves_df
    assert False == res.final_state_values().loc['NETWORK_B3_Upu_value'].empty
    assert False == res.timeline().empty

def test_minimal_simulation():
    """
    Running that test requires to have installed dynawo,
    and configured its path in your config.yml.
    """
    network = pp.network.create_ieee14()
    model_mapping = dyn.ModelMapping()
    generator_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=['B6-G', 'B8-G']),
        data={
            'parameter_set_id': ['GSTWPR_6', 'GSTWPR_8'],
            'model_name': 'GeneratorSynchronousThreeWindingsProportionalRegulations'
        }
    )
    model_mapping.add_synchronous_generator(generator_mapping_df)

    testPath = Path(__file__).parent
    dynawo_param = {
        'parametersFile': str(testPath.joinpath('models.par')),
        'network.parametersFile': str(testPath.joinpath('network.par')),
        'network.parametersId': 'Network',
        'solver.parametersFile': str(testPath.joinpath('solvers.par')),
        'solver.parametersId': 'IDA',
        'solver.type': 'IDA',
    }
    param = dyn.Parameters(start_time=0, stop_time=100, provider_parameters=dynawo_param)

    sim = dyn.Simulation()
    res = sim.run(network=network, model_mapping=model_mapping, parameters=param)

    assert DynamicSimulationStatus.SUCCESS == res.status()
    assert "" == res.status_text()
    assert False == res.timeline().empty

def test_provider_parameters_list():
    assert dyn.Simulation.get_provider_parameters_names()
    parameters = dyn.Simulation.get_provider_parameters()
    assert 'Simulation step precision' == parameters['description']['precision']
    assert 'DOUBLE' == parameters['type']['precision']
    assert '1.0E-6' == parameters['default']['precision']
