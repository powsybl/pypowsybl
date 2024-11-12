#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import numpy as np
import pytest
import pypowsybl as pp
import pandas as pd
import pypowsybl.report as rp
from pypowsybl._pypowsybl import ConditionType


@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)


def test_default_provider():
    assert 'OpenLoadFlow' == pp.security.get_default_provider()
    pp.security.set_default_provider("provider")
    assert 'provider' == pp.security.get_default_provider()
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
    with pytest.raises(pp.PyPowsyblError, match='No security analysis provider for name \'provider\''):
        sa.run_ac(n)
    with pytest.raises(pp.PyPowsyblError, match='No security analysis provider for name \'provider\''):
        sa.run_dc(n)
    sa_result = sa.run_ac(n, provider='OpenLoadFlow')
    assert sa_result.pre_contingency_result.status.name == 'CONVERGED'
    assert 'provider' == pp.security.get_default_provider()
    pp.security.set_default_provider('OpenLoadFlow')
    assert 'OpenLoadFlow' == pp.security.get_default_provider()


def test_ac_security_analysis():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
    sa_result = sa.run_ac(n)
    assert len(sa_result.post_contingency_results) == 1
    assert sa_result.pre_contingency_result.status.name == 'CONVERGED'
    assert sa_result.post_contingency_results['First contingency'].status.name == 'CONVERGED'
    expected = pd.DataFrame.from_records(
        index=['contingency_id', 'subject_id'],
        columns=['contingency_id', 'subject_id', 'subject_name', 'limit_type', 'limit_name',
                 'limit', 'acceptable_duration', 'limit_reduction', 'value', 'side'],
        data=[
            ['First contingency', 'NHV1_NHV2_2', '', 'CURRENT', 'permanent', 500, 2147483647, 1, 1047.825769, 'TWO'],
            ['First contingency', 'VLHV1', '', 'LOW_VOLTAGE', '', 400, 2147483647, 1, 398.264725, ''],
        ])
    pd.testing.assert_frame_equal(expected, sa_result.limit_violations, check_dtype=False)


def test_variant():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')

    sa_result = sa.run_ac(n)
    assert len(sa_result.post_contingency_results) == 1
    assert sa_result.pre_contingency_result.status.name == 'CONVERGED'
    assert sa_result.post_contingency_results['First contingency'].status == pp.security.ComputationStatus.CONVERGED
    assert not sa_result.limit_violations.empty

    # setting load  on variant so that we relieve the violations
    n.clone_variant(n.get_working_variant_id(), 'variant_2')
    n.set_working_variant('variant_2')
    n.update_loads(id='LOAD', p0=100)

    sa_variant_result = sa.run_ac(n)
    assert len(sa_variant_result.post_contingency_results) == 1
    assert sa_variant_result.pre_contingency_result.status.name == 'CONVERGED'
    assert sa_variant_result.post_contingency_results['First contingency'].status.name == 'CONVERGED'
    assert sa_variant_result.limit_violations.empty


def test_monitored_elements():
    n = pp.network.create_eurostag_tutorial_example1_with_more_generators_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'NHV1_NHV2_1')
    sa.add_single_element_contingency('GEN', 'GEN')
    sa.add_monitored_elements(voltage_level_ids=['VLHV2'])
    sa.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_2'], contingency_ids=['NHV1_NHV2_1', 'GEN'])
    sa.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_1'], contingency_ids='GEN')
    sa.add_precontingency_monitored_elements(branch_ids=['NHV1_NHV2_2'])

    sa_result = sa.run_ac(n)
    bus_results = sa_result.bus_results
    branch_results = sa_result.branch_results

    assert bus_results.index.to_frame().columns.tolist() == ['contingency_id', 'operator_strategy_id', 'voltage_level_id', 'bus_id']
    assert bus_results.columns.tolist() == ['v_mag', 'v_angle']
    assert len(bus_results) == 1
    assert bus_results.loc['', '', 'VLHV2', 'NHV2']['v_mag'] == pytest.approx(389.95, abs=1e-2)

    assert branch_results.index.to_frame().columns.tolist() == ['contingency_id', 'operator_strategy_id', 'branch_id']
    assert branch_results.columns.tolist() == ['p1', 'q1', 'i1', 'p2', 'q2', 'i2', 'flow_transfer']
    assert len(branch_results) == 4
    assert branch_results.loc['', '', 'NHV1_NHV2_2']['p1'] == pytest.approx(302.44, abs=1e-2)
    assert branch_results.loc['GEN', '', 'NHV1_NHV2_1']['p1'] == pytest.approx(302.44, abs=1e-2)
    assert branch_results.loc['GEN', '', 'NHV1_NHV2_2']['p1'] == pytest.approx(302.44, abs=1e-2)
    assert branch_results.loc['NHV1_NHV2_1', '', 'NHV1_NHV2_2']['p1'] == pytest.approx(610.56, abs=1e-2)


def test_flow_transfer():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingencies(['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    sa.add_monitored_elements(branch_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    sa_result = sa.run_ac(n)
    branch_results = sa_result.branch_results
    assert branch_results.loc['NHV1_NHV2_1', '', 'NHV1_NHV2_2']['flow_transfer'] == pytest.approx(1.01876, abs=1e-5)
    assert branch_results.loc['NHV1_NHV2_2', '', 'NHV1_NHV2_1']['flow_transfer'] == pytest.approx(1.01876, abs=1e-5)


def test_dc_analysis():
    n = pp.network.create_eurostag_tutorial_example1_with_power_limits_network()
    n.update_loads(id='LOAD', p0=900)
    n.update_generators(id='GEN', target_p=900)
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_2', 'First contingency')
    sa_result = sa.run_dc(n)
    assert len(sa_result.post_contingency_results) == 1
    assert sa_result.pre_contingency_result.status.name == 'CONVERGED'
    assert sa_result.post_contingency_results['First contingency'].status.name == 'CONVERGED'
    expected = pd.DataFrame.from_records(
        index=['contingency_id', 'subject_id'],
        columns=['contingency_id', 'subject_id', 'subject_name', 'limit_type', 'limit_name',
                 'limit', 'acceptable_duration', 'limit_reduction', 'value', 'side'],
        data=[['First contingency', 'NHV1_NHV2_1', '', 'ACTIVE_POWER', 'permanent', 500, 2147483647, 1, 900, 'ONE']])
    pd.testing.assert_frame_equal(expected, sa_result.limit_violations, check_dtype=False)


def test_provider_names():
    assert 'OpenLoadFlow' in pp.security.get_provider_names()


def test_provider_parameters():
    # setting max iterations to 5 will cause the computation to fail, if correctly taken into account
    parameters = pp.loadflow.Parameters(distributed_slack=False, provider_parameters={'maxNewtonRaphsonIterations': '3'})
    n = pp.network.create_ieee14()
    result = pp.security.create_analysis().run_ac(n, parameters)
    assert result.pre_contingency_result.status == pp.loadflow.ComponentStatus.MAX_ITERATION_REACHED

    n = pp.network.create_ieee14()
    result = pp.security.create_analysis().run_ac(n)
    assert result.pre_contingency_result.status == pp.loadflow.ComponentStatus.CONVERGED


def test_ac_security_analysis_with_report():
    report_node = rp.ReportNode()
    report1 = str(report_node)
    assert len(report1) > 0
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
    sa.run_ac(n, report_node=report_node)
    report2 = str(report_node)
    assert len(report2) >= len(report1)

def test_ac_security_analysis_with_deprecated_report():
    report_node = rp.Reporter()
    report1 = str(report_node)
    assert len(report1) > 0
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
    sa.run_ac(n, reporter=report_node)
    report2 = str(report_node)
    assert len(report2) >= len(report1)

def test_dc_analysis_with_report():
    report_node = rp.ReportNode()
    report1 = str(report_node)
    assert len(report1) > 0
    n = pp.network.create_eurostag_tutorial_example1_with_power_limits_network()
    n.update_loads(id='LOAD', p0=900)
    n.update_generators(id='GEN', target_p=900)
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_2', 'First contingency')
    sa_result = sa.run_dc(n, report_node=report_node)
    report2 = str(report_node)
    assert len(report2) >= len(report1)


def test_loadflow_parameters():
    network = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    parameters = pp.security.Parameters()
    parameters.load_flow_parameters.countries_to_balance = ['UNKNOWN']
    with pytest.raises(pp.PyPowsyblError, match='No enum constant com.powsybl.iidm.network.Country.UNKNOWN'):
        sa.run_ac(network, parameters=parameters)

    parameters.load_flow_parameters.countries_to_balance = ['FR']
    res = sa.run_ac(network, parameters=parameters)
    assert res.pre_contingency_result.status == pp.loadflow.ComponentStatus.CONVERGED


def test_security_analysis_parameters():
    network = pp.network.create_eurostag_tutorial_example1_network()
    network.create_operational_limits(element_id='NHV1_NHV2_1', name='permanent_limit', side='ONE', type='CURRENT', value=400.0,
         acceptable_duration=-1, fictitious=False)
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('', 'First contingency')
    sa.add_single_element_contingency('NHV1_NHV2_2', 'First contingency')
    sa.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_1'], contingency_ids='First contingency')

    # default security analysis
    result = sa.run_ac(network, parameters=pp.security.Parameters())
    expected = pd.DataFrame.from_records(
        index=['contingency_id', 'subject_id'],
        columns=['contingency_id', 'subject_id', 'subject_name', 'limit_type', 'limit_name',
                 'limit', 'acceptable_duration', 'limit_reduction', 'value', 'side'],
        data=[['', 'NHV1_NHV2_1', '', 'CURRENT', 'permanent', 400, 2147483647, 1, 456.77, 'ONE'],
              ['First contingency', 'NHV1_NHV2_1', '', 'CURRENT', 'permanent', 400, 2147483647, 1, 1008.93, 'ONE'],
              ['First contingency', 'VLHV1', '', 'LOW_VOLTAGE', '', 400, 2147483647, 1, 398.26, '']])
    pd.testing.assert_frame_equal(expected, result.limit_violations, check_dtype=False, atol=1e-2)

    # flow_proportional_threshold = 10
    parameters = pp.security.Parameters()
    parameters.increased_violations.flow_proportional_threshold = 10
    result = sa.run_ac(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['contingency_id', 'subject_id'],
        columns=['contingency_id', 'subject_id', 'subject_name', 'limit_type', 'limit_name',
                 'limit', 'acceptable_duration', 'limit_reduction', 'value', 'side'],
        data=[['', 'NHV1_NHV2_1', '', 'CURRENT', 'permanent', 400, 2147483647, 1, 456.77, 'ONE'],
              ['First contingency', 'VLHV1', '', 'LOW_VOLTAGE', '', 400, 2147483647, 1, 398.26, '']])
    pd.testing.assert_frame_equal(expected, result.limit_violations, check_dtype=False, atol=1e-2)

    # loadflow parameters only and specific parameters
    result = sa.run_ac(network, parameters=pp.security.Parameters(load_flow_parameters=pp.loadflow.Parameters(provider_parameters={'maxNewtonRaphsonIterations': '1'})))
    assert result.limit_violations.empty
    assert len(result.post_contingency_results) == 0
    assert result.pre_contingency_result.status == pp.loadflow.ComponentStatus.MAX_ITERATION_REACHED


def test_provider_parameters_names():
    assert pp.security.get_provider_parameters_names() == ['createResultExtension', 'contingencyPropagation', 'threadCount', 'dcFastMode']
    assert pp.security.get_provider_parameters_names('OpenLoadFlow') == ['createResultExtension', 'contingencyPropagation', 'threadCount', 'dcFastMode']
    with pytest.raises(pp.PyPowsyblError, match='No security analysis provider for name \'unknown\''):
        pp.security.get_provider_parameters_names('unknown')

def test_with_security_analysis_test_network():
    net = pp.network._create_network('security_analysis_test_with_current_limits')
    sa = pp.security.create_analysis()
    sa.add_single_element_contingencies(['LINE_S1S2V1_1', 'LINE_S1S2V1_2', 'LINE_S1S2V2'])
    result = sa.run_ac(net)
    assert result.pre_contingency_result.status == pp.loadflow.ComponentStatus.CONVERGED
    assert len(result.limit_violations) == 2
    line_s1s2v1_2_limit_violations = result.limit_violations.loc[('LINE_S1S2V1_2', 'TWT')]
    assert 93.65 == pytest.approx(line_s1s2v1_2_limit_violations.value, rel=0.01)
    assert 92.0 == pytest.approx(line_s1s2v1_2_limit_violations.limit, rel=0.01)
    assert 600 == pytest.approx(line_s1s2v1_2_limit_violations.acceptable_duration, rel=0.01)

def test_different_equipment_contingency():
    n = pp.network.create_four_substations_node_breaker_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('HVDC1', 'Hvdc contingency')
    sa.add_single_element_contingency('LINE_S2S3', 'Line contingency')
    sa_result = sa.run_ac(n)
    assert 'Line contingency' in sa_result.post_contingency_results.keys()
    assert 'Hvdc contingency' in sa_result.post_contingency_results.keys()
    n = pp.network._create_network('security_analysis_test_with_current_limits')
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('LD1', 'Load contingency')
    sa.add_single_element_contingency('TWT', 'Twt contingency')
    sa.add_single_element_contingency('S1VL1_BBS1_GEN_DISCONNECTOR', 'Switch contingency')
    sa_result = sa.run_ac(n)
    assert 'Load contingency' in sa_result.post_contingency_results.keys()
    assert 'Twt contingency' in sa_result.post_contingency_results.keys()
    assert 'Switch contingency' in sa_result.post_contingency_results.keys()

def test_load_action():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'Line contingency')
    sa.add_load_active_power_action('LoadAction1', 'LOAD', False, 750.0)
    sa.add_operator_strategy('OperatorStrategy1', 'Line contingency', ['LoadAction1'])
    sa_result = sa.run_ac(n)
    assert 'Line contingency' in sa_result.post_contingency_results.keys()
    assert len(sa_result.find_post_contingency_result('Line contingency').limit_violations) == 2
    assert 'OperatorStrategy1' in sa_result.operator_strategy_results.keys()
    assert len(sa_result.find_operator_strategy_results('OperatorStrategy1').limit_violations) == 3
    assert str(sa_result.get_table()) == """+------------------+----------------------+-----------+--------------+----------------+-------------+--------+------------+---------------------+-----------------+--------+------+
|  Contingency ID  | Operator strategy ID |   Status  | Equipment ID | Equipment name |  Limit type | Limit  | Limit name | Acceptable duration | Limit reduction | Value  | Side |
+------------------+----------------------+-----------+--------------+----------------+-------------+--------+------------+---------------------+-----------------+--------+------+
| Line contingency |                      | CONVERGED |              |                |             |        |            |                     |                 |        |      |
|                  |                      |           | NHV1_NHV2_2  |                |   CURRENT   | 500.0  | permanent  |      2147483647     |       1.0       | 1047.8 | TWO  |
|                  |                      |           |    VLHV1     |                | LOW_VOLTAGE | 400.0  |            |      2147483647     |       1.0       | 398.3  | NONE |
|                  |  OperatorStrategy1   | CONVERGED |              |                |             |        |            |                     |                 |        |      |
|                  |                      |           | NHV1_NHV2_2  |                |   CURRENT   | 1200.0 |    20'     |          60         |       1.0       | 1316.2 | ONE  |
|                  |                      |           | NHV1_NHV2_2  |                |   CURRENT   | 500.0  | permanent  |      2147483647     |       1.0       | 1355.4 | TWO  |
|                  |                      |           |    VLHV1     |                | LOW_VOLTAGE | 400.0  |            |      2147483647     |       1.0       | 394.1  | NONE |
+------------------+----------------------+-----------+--------------+----------------+-------------+--------+------------+---------------------+-----------------+--------+------+"""

def test_load_action_with_any_violation_condition():
    n = pp.network.create_four_substations_node_breaker_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('LINE_S3S4', 'Line contingency')
    sa.add_load_active_power_action('LoadAction1', 'LD6', False, 750.0)

    #LINE_S3S4 does not generate limit violations, OperatorStrategy1 should be applied but not OperatorStrategy2
    sa.add_operator_strategy('OperatorStrategy1', 'Line contingency', ['LoadAction1'], ConditionType.TRUE_CONDITION)
    sa.add_operator_strategy('OperatorStrategy2', 'Line contingency', ['LoadAction1'], ConditionType.ANY_VIOLATION_CONDITION)
    sa_result = sa.run_ac(n)
    assert 'Line contingency' in sa_result.post_contingency_results.keys()
    assert len(sa_result.find_post_contingency_result('Line contingency').limit_violations) == 0

    assert 'OperatorStrategy1' in sa_result.operator_strategy_results.keys()
    assert 'OperatorStrategy2' not in sa_result.operator_strategy_results.keys()

def test_load_action_with_all_violation_condition():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'Line contingency')
    sa.add_load_active_power_action('LoadAction1', 'LOAD', False, 750.0)

    #OperatorStrategy1 will be applied because we have a violation on NHV1_NHV2_2 after contingency
    #OperatorStrategy2 should not be applied because there is not violation on UnknownLine
    sa.add_operator_strategy('OperatorStrategy1', 'Line contingency', ['LoadAction1'], ConditionType.ALL_VIOLATION_CONDITION , ['NHV1_NHV2_2'])
    sa.add_operator_strategy('OperatorStrategy2', 'Line contingency', ['LoadAction1'], ConditionType.ALL_VIOLATION_CONDITION, ['NHV1_NHV2_2', 'UnknownLine'])
    sa_result = sa.run_ac(n)
    assert 'Line contingency' in sa_result.post_contingency_results.keys()
    assert len(sa_result.find_post_contingency_result('Line contingency').limit_violations) == 2
    assert 'OperatorStrategy1' in sa_result.operator_strategy_results.keys()
    assert len(sa_result.find_operator_strategy_results('OperatorStrategy1').limit_violations) == 3
    assert 'OperatorStrategy2' not in sa_result.operator_strategy_results.keys()

def test_generator_action():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'Line contingency')
    sa.add_generator_active_power_action(action_id='GeneratorActionAbsolute', generator_id='GEN', is_relative=False, active_power=750.0)
    sa.add_generator_active_power_action(action_id='GeneratorActionRelative', generator_id='GEN', is_relative=True, active_power=150.0)
    sa.add_operator_strategy('OperatorStrategy1', 'Line contingency', ['GeneratorActionAbsolute'])
    sa.add_operator_strategy('OperatorStrategy2', 'Line contingency', ['GeneratorActionRelative'])
    sa_result = sa.run_ac(n)
    assert 'Line contingency' in sa_result.post_contingency_results.keys()
    assert 'OperatorStrategy1' in sa_result.operator_strategy_results.keys()
    assert 'OperatorStrategy2' in sa_result.operator_strategy_results.keys()

def test_switch_action():
    n = pp.network.create_four_substations_node_breaker_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency(element_id='S4VL1_BBS_LD6_DISCONNECTOR', contingency_id='Breaker contingency')
    sa.add_switch_action(action_id='SwitchAction', switch_id='S4VL1_BBS_LD6_DISCONNECTOR', open=False)
    sa.add_operator_strategy(operator_strategy_id='OperatorStrategy1', contingency_id='Breaker contingency', action_ids=['SwitchAction'], condition_type=ConditionType.TRUE_CONDITION)
    sa.add_monitored_elements(branch_ids=['LINE_S3S4'])
    sa_result = sa.run_ac(n)
    df = sa_result.branch_results

    #Contingency open a switch, then action close it
    #Check p1 on line is the same pre contingency and post remedial action
    assert df.loc['', '', 'LINE_S3S4']['p1'] == pytest.approx(2.400036e+02, abs=1e-2)
    assert df.loc['Breaker contingency', '', 'LINE_S3S4']['p1'] == pytest.approx(0.0, abs=1e-2)
    assert df.loc['Breaker contingency', 'OperatorStrategy1', 'LINE_S3S4']['p1'] == pytest.approx(2.400036e+02, abs=1e-2)


def test_tap_changer_action():
    n = pp.network.create_micro_grid_be_network()

    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('550ebe0d-f2b2-48c1-991f-cebea43a21aa', 'BE-G2_contingency')

    sa.add_monitored_elements(branch_ids=['ffbabc27-1ccd-4fdc-b037-e341706c8d29'])

    sa.add_phase_tap_changer_position_action(action_id='PhaseTapChanger_Action', transformer_id='a708c3bc-465d-4fe7-b6ef-6fa6408a62b0', is_relative=True, tap_position=5)
    sa.add_ratio_tap_changer_position_action(action_id='RatioTapChanger_Action', transformer_id='e482b89a-fa84-4ea9-8e70-a83d44790957', is_relative=True, tap_position=5)

    sa.add_operator_strategy('Strategy_PhaseTapChanger', 'BE-G2_contingency', ['PhaseTapChanger_Action'])
    sa.add_operator_strategy('Strategy_RatioTapChanger', 'BE-G2_contingency', ['RatioTapChanger_Action'])

    sa_result = sa.run_ac(n)
    df = sa_result.branch_results

    assert df.loc['', '', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.218, abs=1e-2)
    assert df.loc['BE-G2_contingency', '', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.305, abs=1e-2)
    assert df.loc['BE-G2_contingency', 'Strategy_PhaseTapChanger', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.312, abs=1e-2)
    assert df.loc['BE-G2_contingency', 'Strategy_RatioTapChanger', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.306, abs=1e-2)

def test_shunt_action():
    n = pp.network.create_micro_grid_be_network()

    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('550ebe0d-f2b2-48c1-991f-cebea43a21aa', 'BE-G2_contingency')
    sa.add_monitored_elements(branch_ids=['ffbabc27-1ccd-4fdc-b037-e341706c8d29'])

    sa.add_shunt_compensator_position_action(action_id='ShuntCompensator_Action', shunt_id='d771118f-36e9-4115-a128-cc3d9ce3e3da', section=1)
    sa.add_operator_strategy('Strategy_ShuntCompensator', 'BE-G2_contingency', ['ShuntCompensator_Action'])
    sa_result = sa.run_ac(n)
    df = sa_result.branch_results

    assert df.loc['', '', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.218, abs=1e-2)
    assert df.loc['BE-G2_contingency', '', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.305, abs=1e-2)
    assert df.loc['BE-G2_contingency', 'Strategy_ShuntCompensator', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.312, abs=1e-2)
    assert df.loc['BE-G2_contingency', 'Strategy_ShuntCompensator', 'ffbabc27-1ccd-4fdc-b037-e341706c8d29']['p1'] == pytest.approx(-11.306, abs=1e-2)

def test_tie_line_contingency():
    n = pp.network._create_network("eurostag_tutorial_example1_with_tie_line")
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'tie line contingency')
    sa_result = sa.run_ac(n)
    assert 'tie line contingency' in sa_result.post_contingency_results.keys()
