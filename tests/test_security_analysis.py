#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pytest
import pypowsybl as pp
import pandas as pd


@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)


def test_default_provider():
    assert 'OpenSecurityAnalysis' == pp.security.get_default_provider()
    pp.security.set_default_provider("provider")
    assert 'provider' == pp.security.get_default_provider()
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
    with pytest.raises(Exception) as exc_info:
        sa.run_ac(n)
    assert 'SecurityAnalysisProvider \'provider\' not found' == str(exc_info.value)
    with pytest.raises(Exception) as exc_info:
        sa.run_dc(n)
    assert 'SecurityAnalysisProvider \'provider\' not found' == str(exc_info.value)
    sa_result = sa.run_ac(n, provider='OpenSecurityAnalysis')
    assert sa_result.pre_contingency_result.status.name == 'CONVERGED'
    assert 'provider' == pp.security.get_default_provider()
    pp.security.set_default_provider('OpenSecurityAnalysis')
    assert 'OpenSecurityAnalysis' == pp.security.get_default_provider()


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
            ['First contingency', 'NHV1_NHV2_2', '', 'CURRENT', '', 500, 2147483647, 1, 1047.825769, 'TWO'],
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
    assert sa_result.post_contingency_results['First contingency'].status.name == 'CONVERGED'
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
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingency('NHV1_NHV2_1', 'NHV1_NHV2_1')
    sa.add_single_element_contingency('NGEN_NHV1', 'NGEN_NHV1')
    sa.add_monitored_elements(voltage_level_ids=['VLHV2'])
    sa.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_2'], contingency_ids=['NHV1_NHV2_1', 'NGEN_NHV1'])
    sa.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_1'], contingency_ids='NGEN_NHV1')
    sa.add_precontingency_monitored_elements(branch_ids=['NHV1_NHV2_2'])

    sa_result = sa.run_ac(n)
    bus_results = sa_result.bus_results
    branch_results = sa_result.branch_results

    expected = pd.DataFrame(index=pd.MultiIndex.from_tuples(names=['contingency_id', 'voltage_level_id', 'bus_id'],
                                                            tuples=[('', 'VLHV2', 'NHV2'),
                                                                    ('NGEN_NHV1', 'VLHV2', 'NHV2'),
                                                                    ('NHV1_NHV2_1', 'VLHV2', 'NHV2')]),
                            columns=['v_mag', 'v_angle'],
                            data=[[389.952654, -3.506358],
                                  [569.038987, -1.709471],
                                  [366.584814, -7.499211]])
    pd.testing.assert_frame_equal(expected, bus_results)

    assert branch_results.index.to_frame().columns.tolist() == ['contingency_id', 'branch_id']
    assert branch_results.columns.tolist() == ['p1', 'q1', 'i1', 'p2', 'q2', 'i2', 'flow_transfer']
    assert len(branch_results) == 4
    assert branch_results.loc['', 'NHV1_NHV2_2']['p1'] == pytest.approx(302.44, abs=1e-2)
    assert branch_results.loc['NHV1_NHV2_1', 'NHV1_NHV2_2']['p1'] == pytest.approx(610.56, abs=1e-2)
    assert branch_results.loc['NGEN_NHV1', 'NHV1_NHV2_2']['p1'] == pytest.approx(301.06, abs=1e-2)
    assert branch_results.loc['NGEN_NHV1', 'NHV1_NHV2_1']['p1'] == pytest.approx(301.06, abs=1e-2)


def test_flow_transfer():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.security.create_analysis()
    sa.add_single_element_contingencies(['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    sa.add_monitored_elements(branch_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    sa_result = sa.run_ac(n)
    branch_results = sa_result.branch_results
    assert branch_results.loc['NHV1_NHV2_1', 'NHV1_NHV2_2']['flow_transfer'] == pytest.approx(1.01876, abs=1e-5)
    assert branch_results.loc['NHV1_NHV2_2', 'NHV1_NHV2_1']['flow_transfer'] == pytest.approx(1.01876, abs=1e-5)


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
        data=[['First contingency', 'NHV1_NHV2_1', '', 'ACTIVE_POWER', '', 500, 2147483647, 1, 900, 'ONE']])
    pd.testing.assert_frame_equal(expected, sa_result.limit_violations, check_dtype=False)


def test_provider_names():
    assert 'OpenSecurityAnalysis' in pp.security.get_provider_names()
