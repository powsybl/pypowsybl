#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest

import pypowsybl as pp
import pypowsybl.loadflow as lf
from pypowsybl._pypowsybl import LoadFlowComponentStatus
from pypowsybl.loadflow import ValidationType
import pytest
import pypowsybl.report as rp

@pytest.fixture(autouse=True)
def setUp():
    pp.set_config_read(False)

def test_config():
    assert 'OpenLoadFlow' == pp.loadflow.get_default_provider()
    pp.loadflow.set_default_provider("provider")
    assert 'provider' == pp.loadflow.get_default_provider()
    n = pp.network.create_ieee14()
    with pytest.raises(Exception, match='LoadFlowProvider \'provider\' not found'):
        lf.run_ac(n)
    results = lf.run_ac(n, provider='OpenLoadFlow')
    assert lf.ComponentStatus.CONVERGED == results[0].status
    assert 'provider' == pp.loadflow.get_default_provider()
    pp.loadflow.set_default_provider("OpenLoadFlow")
    assert 'OpenLoadFlow' == pp.loadflow.get_default_provider()

def test_run_lf():
    n = pp.network.create_ieee14()
    results = lf.run_ac(n)
    assert 1 == len(results)
    assert lf.ComponentStatus.CONVERGED == results[0].status
    assert 0 == results[0].connected_component_num
    assert 0 == results[0].synchronous_component_num
    assert 'VL1_0' == results[0].slack_bus_id
    assert round(abs(0.5-results[0].slack_bus_active_power_mismatch), 1) == 0
    assert 7 == results[0].iteration_count

    parameters = lf.Parameters(distributed_slack=False)
    results = lf.run_dc(n, parameters)
    assert 1 == len(results)

def test_lf_parameters():
    parameters = lf.Parameters()
    assert parameters.dc_use_transformer_ratio
    assert 0 == len(parameters.countries_to_balance)
    assert lf.ConnectedComponentMode.MAIN == parameters.connected_component_mode

    # Testing setting independently every attributes
    attributes = {
        'voltage_init_mode': [lf.VoltageInitMode.DC_VALUES, lf.VoltageInitMode.UNIFORM_VALUES],
        'transformer_voltage_control_on': [True, False],
        'no_generator_reactive_limits': [True, False],
        'phase_shifter_regulation_on': [True, False],
        'twt_split_shunt_admittance': [True, False],
        'simul_shunt': [True, False],
        'read_slack_bus': [True, False],
        'write_slack_bus': [True, False],
        'distributed_slack': [True, False],
        'balance_type': [lf.BalanceType.PROPORTIONAL_TO_CONFORM_LOAD, lf.BalanceType.PROPORTIONAL_TO_GENERATION_P],
        'dc_use_transformer_ratio': [True, False],
        'countries_to_balance': [['FR'], ['BE']],
        'connected_component_mode': [lf.ConnectedComponentMode.MAIN, lf.ConnectedComponentMode.ALL]
    }

    for attribute, values in attributes.items():
        for value in values:
            parameters = lf.Parameters(**dict([(attribute, value)]))
            assert value == getattr(parameters, attribute)

            parameters = lf.Parameters()
            setattr(parameters, attribute, value)
            assert value == getattr(parameters, attribute)

def test_validation():
    n = pp.network.create_ieee14()
    pp.loadflow.run_ac(n)
    validation = pp.loadflow.run_validation(n,
                                            [ValidationType.FLOWS, ValidationType.GENERATORS, ValidationType.BUSES])
    assert abs(-232.4-validation.generators['p']['B1-G']) < 0.1
    assert abs(-47.8-validation.buses['incoming_p']['VL4_0']) < 0.1
    assert abs(157.8-validation.branch_flows['p1']['L1-2-1']) < 0.1
    assert not validation.valid
    n2 = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n)
    validation2 = pp.loadflow.run_validation(n2, [ValidationType.SVCS])
    assert 1 == len(validation2.svcs)
    assert validation2.svcs['validated']['SVC']

def test_twt_validation():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    validation = pp.loadflow.run_validation(n, [ValidationType.TWTS])
    assert abs(-10.421382-validation.twts['error']['NHV2_NLOAD']) < 0.00001
    assert validation.valid

def test_validation_all():
    n = pp.network.create_ieee14()
    pp.loadflow.run_ac(n)
    validation = pp.loadflow.run_validation(n)
    assert validation.buses is not None
    assert validation.generators is not None
    assert validation.branch_flows is not None
    assert validation.svcs is not None
    assert validation.shunts is not None
    assert validation.t3wts is not None
    assert validation.twts is not None

def test_provider_names():
    assert 'OpenLoadFlow' in pp.loadflow.get_provider_names()

def test_get_provider_parameters_names():
    specific_parameters = pp.loadflow.get_provider_parameters_names()
    assert ['slackBusSelectionMode', 'slackBusesIds', 'lowImpedanceBranchMode', 'voltageRemoteControl',
         'throwsExceptionInCaseOfSlackDistributionFailure', 'loadPowerFactorConstant',
         'plausibleActivePowerLimit', 'addRatioToLinesWithDifferentNominalVoltageAtBothEnds',
         'slackBusPMaxMismatch', 'voltagePerReactivePowerControl', 'reactivePowerRemoteControl',
         'maxIteration', 'newtonRaphsonConvEpsPerEq', 'voltageInitModeOverride',
         'transformerVoltageControlMode'] == specific_parameters

def test_provider_parameters():
    parameters = lf.Parameters(distributed_slack=False, provider_parameters={'maxIteration': '5'})
    assert '5' == parameters.provider_parameters['maxIteration']
    parameters.provider_parameters['voltageRemoteControl'] = 'false'
    n = pp.network.create_ieee14()
    result = pp.loadflow.run_ac(n, parameters)
    assert LoadFlowComponentStatus.MAX_ITERATION_REACHED == result[0].status
    assert 6 == result[0].iteration_count

def test_run_lf_with_report():
    n = pp.network.create_ieee14()
    reporter = rp.Reporter("test_run_lf", "test_run_lf")
    pp.loadflow.run_ac(n, reporter = reporter)
    report = str(reporter)
    assert len(report) > 0

    n2 = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n2, reporter = reporter)
    report2 = str(reporter)
    assert len(report2) > len(report)

