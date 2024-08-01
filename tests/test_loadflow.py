#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import json

import pypowsybl as pp
import pypowsybl.loadflow as lf
from pypowsybl._pypowsybl import LoadFlowComponentStatus
from pypowsybl.loadflow import ValidationType
import pytest
import pypowsybl.report as rp

TEST_DIR = pathlib.Path(__file__).parent

@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)


def test_config():
    assert 'OpenLoadFlow' == pp.loadflow.get_default_provider()
    pp.loadflow.set_default_provider("provider")
    assert 'provider' == pp.loadflow.get_default_provider()
    n = pp.network.create_ieee14()
    with pytest.raises(Exception, match='No loadflow provider for name \'provider\''):
        lf.run_ac(n)
    results = lf.run_ac(n, provider='OpenLoadFlow')
    assert lf.ComponentStatus.CONVERGED == results[0].status
    assert 'provider' == pp.loadflow.get_default_provider()
    pp.loadflow.set_default_provider("OpenLoadFlow")
    assert 'OpenLoadFlow' == pp.loadflow.get_default_provider()


def test_run_lf_ac():
    n = pp.network.create_ieee14()
    results = lf.run_ac(n)
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


def test_run_lf_ac_2slacks():
    n = pp.network.create_ieee14()
    p = lf.Parameters(read_slack_bus=False, distributed_slack=False, provider_parameters={'maxSlackBusCount': '2'})
    results = lf.run_ac(n, p)
    assert 1 == len(results)
    assert lf.ComponentStatus.CONVERGED == results[0].status
    assert 2 == len(results[0].slack_bus_results)
    sbr0 = results[0].slack_bus_results[0]
    sbr1 = results[0].slack_bus_results[1]
    assert 'VL4_0' == sbr0.id
    assert abs(-0.75 - sbr0.active_power_mismatch) < 0.01
    assert 'VL2_0' == sbr1.id
    assert abs(-0.75 - sbr1.active_power_mismatch) < 0.01


def test_run_lf_dc():
    n = pp.network.create_ieee14()
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
        'use_reactive_limits': [True, False],
        'phase_shifter_regulation_on': [True, False],
        'twt_split_shunt_admittance': [True, False],
        'shunt_compensator_voltage_control_on': [True, False],
        'read_slack_bus': [True, False],
        'write_slack_bus': [True, False],
        'distributed_slack': [True, False],
        'balance_type': [lf.BalanceType.PROPORTIONAL_TO_CONFORM_LOAD, lf.BalanceType.PROPORTIONAL_TO_GENERATION_P],
        'dc_use_transformer_ratio': [True, False],
        'countries_to_balance': [['FR'], ['BE']],
        'connected_component_mode': [lf.ConnectedComponentMode.MAIN, lf.ConnectedComponentMode.ALL],
        'dc_power_factor': [1.0, 0.95]
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
    assert abs(-232.4 - validation.generators['p']['B1-G']) < 0.1
    assert abs(-47.8 - validation.buses['incoming_p']['VL4_0']) < 0.1
    assert abs(156.9 - validation.branch_flows['p1']['L1-2-1']) < 0.1
    assert not validation.valid
    n2 = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n2)
    validation2 = pp.loadflow.run_validation(n2, [ValidationType.SVCS])
    assert 1 == len(validation2.svcs)
    assert validation2.svcs['validated']['SVC']


def test_twt_validation():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    validation = pp.loadflow.run_validation(n, [ValidationType.TWTS])
    assert abs(-10.421382 - validation.twts['error']['NHV2_NLOAD']) < 0.00001
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


def test_validation_parameters_get():
    # Testing setting independently every attributes
    attributes = {
        'threshold': [0.1, 0.2],
        'verbose': [True, False],
        'loadflow_name': ['loadFlow1', 'loadFLow2'],
        'epsilon_x': [0.1, 0.2],
        'apply_reactance_correction': [True, False],
        'ok_missing_values': [True, False],
        'no_requirement_if_reactive_bound_inversion': [True, False],
        'compare_results': [True, False],
        'check_main_component_only': [True, False],
        'no_requirement_if_setpoint_outside_power_bounds': [True, False]
    }

    for attribute, values in attributes.items():
        for value in values:
            parameters = lf.ValidationParameters(**dict([(attribute, value)]))
            assert value == getattr(parameters, attribute)


def test_validation_parameters_set():
    attributes = {
        'threshold': [0.1, 0.2],
        'verbose': [True, False],
        'loadflow_name': ['loadFlow1', 'loadFLow2'],
        'epsilon_x': [0.1, 0.2],
        'apply_reactance_correction': [True, False],
        'ok_missing_values': [True, False],
        'no_requirement_if_reactive_bound_inversion': [True, False],
        'compare_results': [True, False],
        'check_main_component_only': [True, False],
        'no_requirement_if_setpoint_outside_power_bounds': [True, False]
    }
    for attribute, values in attributes.items():
        for value in values:
            parameters = lf.ValidationParameters()
            setattr(parameters, attribute, value)
            assert value == getattr(parameters, attribute)


def test_validation_parameters_not_valid():
    n = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n)
    parameters = lf.ValidationParameters()
    validation = pp.loadflow.run_validation(n, validation_parameters=parameters)
    assert not validation.branch_flows['validated']['LINE_S2S3']


def test_validation_parameters_valid():
    n = pp.network.create_four_substations_node_breaker_network()
    pp.loadflow.run_ac(n)
    parameters = lf.ValidationParameters(threshold=0.1)
    validation = pp.loadflow.run_validation(n, validation_parameters=parameters)
    assert validation.branch_flows['validated']['LINE_S2S3']


def test_provider_names():
    assert 'OpenLoadFlow' in pp.loadflow.get_provider_names()
    assert 'DynaFlow' in pp.loadflow.get_provider_names()


def test_get_provider_parameters_names():
    specific_parameters = pp.loadflow.get_provider_parameters_names()
    assert specific_parameters == ['slackBusSelectionMode',
                                   'slackBusesIds',
                                   'lowImpedanceBranchMode',
                                   'voltageRemoteControl',
                                   'slackDistributionFailureBehavior',
                                   'loadPowerFactorConstant',
                                   'plausibleActivePowerLimit',
                                   'slackBusPMaxMismatch',
                                   'voltagePerReactivePowerControl',
                                   'generatorReactivePowerRemoteControl',
                                   'transformerReactivePowerControl',
                                   'maxNewtonRaphsonIterations',
                                   'maxOuterLoopIterations',
                                   'newtonRaphsonConvEpsPerEq',
                                   'voltageInitModeOverride',
                                   'transformerVoltageControlMode',
                                   'shuntVoltageControlMode',
                                   'minPlausibleTargetVoltage',
                                   'maxPlausibleTargetVoltage',
                                   'minRealisticVoltage',
                                   'maxRealisticVoltage',
                                   'reactiveRangeCheckMode',
                                   'lowImpedanceThreshold',
                                   'networkCacheEnabled',
                                   'svcVoltageMonitoring',
                                   'stateVectorScalingMode',
                                   'maxSlackBusCount',
                                   'debugDir',
                                   'incrementalTransformerRatioTapControlOuterLoopMaxTapShift',
                                   'secondaryVoltageControl',
                                   'reactiveLimitsMaxPqPvSwitch',
                                   'newtonRaphsonStoppingCriteriaType',
                                   'maxActivePowerMismatch',
                                   'maxReactivePowerMismatch',
                                   'maxVoltageMismatch',
                                   'maxAngleMismatch',
                                   'maxRatioMismatch',
                                   'maxSusceptanceMismatch',
                                   'phaseShifterControlMode',
                                   'alwaysUpdateNetwork',
                                   'mostMeshedSlackBusSelectorMaxNominalVoltagePercentile',
                                   'reportedFeatures',
                                   'slackBusCountryFilter',
                                   'actionableSwitchesIds',
                                   'actionableTransformersIds',
                                   'asymmetrical',
                                   'minNominalVoltageTargetVoltageCheck',
                                   'reactivePowerDispatchMode',
                                   'outerLoopNames',
                                   'useActiveLimits',
                                   'disableVoltageControlOfGeneratorsOutsideActivePowerLimits',
                                   'lineSearchStateVectorScalingMaxIteration',
                                   'lineSearchStateVectorScalingStepFold',
                                   'maxVoltageChangeStateVectorScalingMaxDv',
                                   'maxVoltageChangeStateVectorScalingMaxDphi',
                                   'linePerUnitMode',
                                   'useLoadModel',
                                   'dcApproximationType',
                                   'simulateAutomationSystems',
                                   'acSolverType',
                                   'maxNewtonKrylovIterations',
                                   'newtonKrylovLineSearch',
                                   'referenceBusSelectionMode',
                                   'writeReferenceTerminals',
                                   'voltageTargetPriorities',
                                   'generatorVoltageControlMinNominalVoltage']

def test_get_provider_parameters():
    specific_parameters = pp.loadflow.get_provider_parameters('OpenLoadFlow')
    assert 66 == len(specific_parameters)
    assert 'Slack bus selection mode' == specific_parameters['description']['slackBusSelectionMode']
    assert 'STRING' == specific_parameters['type']['slackBusSelectionMode']
    assert 'MOST_MESHED' == specific_parameters['default']['slackBusSelectionMode']


def test_provider_parameters():
    parameters = lf.Parameters(distributed_slack=False, provider_parameters={'maxNewtonRaphsonIterations': '2'})
    assert '2' == parameters.provider_parameters['maxNewtonRaphsonIterations']
    parameters.provider_parameters['voltageRemoteControl'] = 'false'
    n = pp.network.create_ieee14()
    result = pp.loadflow.run_ac(n, parameters)
    assert LoadFlowComponentStatus.MAX_ITERATION_REACHED == result[0].status
    assert 3 == result[0].iteration_count


def test_run_lf_with_report():
    n = pp.network.create_ieee14()
    report_node = rp.ReportNode()
    report1 = str(report_node)
    assert len(report1) > 0
    pp.loadflow.run_ac(n, report_node=report_node)
    report2 = str(report_node)
    assert len(report2) > len(report1)
    json_report = report_node.to_json()
    assert len(json_report) > 0
    json.loads(json_report)

    n2 = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n2, report_node=report_node)
    report3 = str(report_node)
    assert len(report3) > len(report2)

def test_run_lf_with_deprecated_reporter():
    n = pp.network.create_ieee14()
    report_node = rp.Reporter()
    report1 = str(report_node)
    assert len(report1) > 0
    pp.loadflow.run_ac(n, reporter=report_node)
    report2 = str(report_node)
    assert len(report2) > len(report1)
    json_report = report_node.to_json()
    assert len(json_report) > 0
    json.loads(json_report)

    n2 = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n2, reporter=report_node)
    report3 = str(report_node)
    assert len(report3) > len(report2)


def test_result_status_as_bool():
    n = pp.network.create_ieee14()
    r = pp.loadflow.run_ac(n)
    assert r[0].status


def test_wrong_regulated_bus_id():
    net = pp.network.load(str(TEST_DIR.joinpath('eurostag-example1_test_regulated_side_null.xiidm')))
    pp.loadflow.run_ac(net)
    parameters = lf.ValidationParameters()
    validation = pp.loadflow.run_validation(net, validation_parameters=parameters)

def test():
    pass