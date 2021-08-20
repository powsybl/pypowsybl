#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest
import pypowsybl as pp
import pandas as pd


class SecurityAnalysisTestCase(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_security_analysis(self):
        pd.set_option('display.max_columns', None)
        n = pp.network.create_eurostag_tutorial_example1_network()
        sa = pp.security.create_analysis()
        sa.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
        sa_result = sa.run_ac(n)
        self.assertEqual(1, len(sa_result.post_contingency_results))
        self.assertEqual('CONVERGED', sa_result.pre_contingency_result.status.name)
        self.assertEqual('CONVERGED', sa_result.post_contingency_results['First contingency'].status.name)
        expected = pd.DataFrame(index=pd.MultiIndex.from_tuples(names=['contingency_id', 'subject_id'],
                                                                tuples=[('First contingency', 'NHV1_NHV2_2'),
                                                                        ('First contingency', 'VLHV1')]),
                                columns=['subject_name', 'limit_type', 'limit_name', 'limit', 'acceptable_duration',
                                         'limit_reduction', 'value', 'side'],
                                data=[
                                    ['', 'CURRENT', '', 500, 2147483647, 1, 1047.825769, 'TWO'],
                                    ['', 'LOW_VOLTAGE', '', 400, 2147483647, 1, 398.264725, ''],
                                ])
        pd.testing.assert_frame_equal(expected, sa_result.limit_violations, check_dtype=False)

    def test_monitored_elements(self):
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

        self.assertEqual(['contingency_id', 'branch_id'], branch_results.index.to_frame().columns.tolist())
        self.assertEqual(['p1', 'q1', 'i1', 'p2', 'q2', 'i2'], branch_results.columns.tolist())
        self.assertEqual(4, len(branch_results))
        self.assertAlmostEqual(302.44, branch_results.loc['', 'NHV1_NHV2_2']['p1'], places=2)
        self.assertAlmostEqual(610.56, branch_results.loc['NHV1_NHV2_1', 'NHV1_NHV2_2']['p1'], places=2)
        self.assertAlmostEqual(301.06, branch_results.loc['NGEN_NHV1', 'NHV1_NHV2_2']['p1'], places=2)
        self.assertAlmostEqual(301.06, branch_results.loc['NGEN_NHV1', 'NHV1_NHV2_1']['p1'], places=2)


if __name__ == '__main__':
    unittest.main()
