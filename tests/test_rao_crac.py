#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import unittest
import pandas as pd
import pytest

import pypowsybl as pp
from pypowsybl.rao import Crac

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'

class TestRaoCrac:
    def setup_class(self):
        self.network = pp.network.load(DATA_DIR.joinpath("rao/network_crac.xiidm"))
        self.crac = Crac.from_file_source(self.network, DATA_DIR.joinpath("rao/crac-v2.8.json"))

    def test_crac_instants(self):
        df = self.crac.get_instants()
        assert ['kind', 'order'] == list(df.columns)
        expected = pd.DataFrame(index=pd.Series(name='id', data=['preventive', 'outage', 'auto', 'toto', 'curative']),
                                columns=['kind', 'order'],
                                data=[['PREVENTIVE', 0],
                                     ['OUTAGE', 1],
                                      ['AUTO', 2],
                                      ['CURATIVE', 3],
                                      ['CURATIVE', 4]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_crac_max_remedial_actions_usage_limits(self):
        df = self.crac.get_max_remedial_actions_usage_limits()
        assert ['value'] == list(df.columns)
        expected = pd.DataFrame(index=pd.Series(name='instant', data=['curative']),
                                columns=['value'],
                                data=[[4]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_crac_max_topological_actions_per_tso_usage_limits(self):
        df = self.crac.get_max_topological_actions_per_tso_usage_limits()
        assert ['value'] == list(df.columns)
        expected = pd.DataFrame(index=pd.MultiIndex.from_tuples([
                                ("curative", "BE"), ("curative", "FR")], names=["instant", "tso"]),
                                columns=['value'],
                                data=[[6] , [5]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_crac_max_pst_actions_per_tso_usage_limits(self):
        df = self.crac.get_max_pst_actions_per_tso_usage_limits()
        assert ['value'] == list(df.columns)
        expected = pd.DataFrame(index=pd.MultiIndex.from_tuples([
            ("curative", "FR")], names=["instant", "tso"]),
            columns=['value'],
            data=[[7]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_crac_max_remedial_actions_per_tso_usage_limits(self):
        df = self.crac.get_max_remedial_actions_per_tso_usage_limits()
        assert ['value'] == list(df.columns)
        expected = pd.DataFrame(index=pd.MultiIndex.from_tuples([
            ("curative", "FR")], names=["instant", "tso"]),
            columns=['value'],
            data=[[12]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_crac_max_elementary_actions_per_tso_usage_limits(self):
        df = self.crac.get_max_elementary_actions_per_tso_usage_limits()
        assert ['value'] == list(df.columns)
        expected = pd.DataFrame(index=pd.MultiIndex.from_tuples([
            ("curative", "FR")], names=["instant", "tso"]),
            columns=['value'],
            data=[[21]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_crac_contingencies(self):
        df = self.crac.get_contingencies()
        assert [] == list(df.columns)
        expected = pd.DataFrame(index=pd.Series(name='id', data=['contingency2Id', 'contingency1Id']),
                                columns=[],
                                data=[])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True, check_column_type=False)

    def test_contingency_elements(self):
        df = self.crac.get_contingency_elements()
        assert [] == list(df.columns)
        expected = pd.DataFrame(index=pd.MultiIndex.from_tuples([
            ('contingency2Id', 'ne2Id'), ('contingency2Id', 'ne3Id'), ('contingency1Id', 'ne1Id')], names=["id", "network_element_id"]),
            columns=[],
            data=[])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True, check_column_type=False)

    def test_flow_cnecs(self):
        df = self.crac.get_flow_cnecs()
        assert ['name', 'network_element_id', 'operator', 'border', 'instant',
                'contingency_id', 'optimized', 'monitored', 'reliability_margin'] == list(df.columns)
        cnec = df.loc['cnec1prevId']

        assert 'cnec1prevId' == cnec['name']
        assert 'ne4Id' == cnec['network_element_id']
        assert 'operator1' == cnec['operator']
        assert 'border1' == cnec['border']
        assert 'preventive' == cnec['instant']
        assert '' == cnec['contingency_id']
        assert True == cnec['optimized']
        assert False == cnec['monitored']
        assert 0.0 == cnec['reliability_margin']

    def test_angle_cnecs(self):
        df = self.crac.get_angle_cnecs()
        assert ['name', 'exporting_network_element_id', 'importing_network_element_id', 'operator', 'border', 'instant',
                'contingency_id', 'optimized', 'monitored', 'reliability_margin'] == list(df.columns)
        cnec = df.loc['angleCnecId']

        assert 'angleCnecName' == cnec['name']
        assert 'eneId' == cnec['exporting_network_element_id']
        assert 'ineId' == cnec['importing_network_element_id']
        assert 'operator1' == cnec['operator']
        assert 'border4' == cnec['border']
        assert 'curative' == cnec['instant']
        assert 'contingency1Id' == cnec['contingency_id']
        assert False== cnec['optimized']
        assert True == cnec['monitored']
        assert 0.0 == cnec['reliability_margin']

    def test_voltage_cnecs(self):
        df = self.crac.get_voltage_cnecs()
        assert ['name', 'network_element_id', 'operator', 'border', 'instant',
                'contingency_id', 'optimized', 'monitored', 'reliability_margin'] == list(df.columns)
        cnec = df.loc['voltageCnecId']

        assert 'voltageCnecName' == cnec['name']
        assert 'voltageCnecNeId' == cnec['network_element_id']
        assert 'operator1' == cnec['operator']
        assert 'border5' == cnec['border']
        assert 'curative' == cnec['instant']
        assert 'contingency1Id' == cnec['contingency_id']
        assert False == cnec['optimized']
        assert True == cnec['monitored']
        assert 0.0 == cnec['reliability_margin']

    def test_thresholds(self):
        df = self.crac.get_thresholds()
        assert ['min', 'max', 'unit', 'side'] == list(df.columns)
        threshold = df.loc['cnec1outageId']

        assert '-800.0' == threshold['min']
        assert 'AMPERE' == threshold['unit']
        assert 'ONE' == threshold['side']

    def test_pst_range_actions(self):
        df = self.crac.get_pst_range_actions()
        assert ['name', 'operator', 'network_element_id', 'group_id', 'speed', 'activation_cost',
                'variation_cost_up', 'variation_cost_down'] == list(df.columns)
        action = df.loc['pstRange1Id']

        assert 'pstRange1Name' == action['name']
        assert 'RTE' == action['operator']
        assert 'pst' == action['network_element_id']

    def test_hvdc_range_actions(self):
        df = self.crac.get_hvdc_range_actions()
        assert ['name', 'operator', 'network_element_id', 'group_id', 'speed', 'activation_cost',
                'variation_cost_up', 'variation_cost_down'] == list(df.columns)
        action = df.loc['hvdcRange1Id']

        assert 'hvdcRange1Name' == action['name']
        assert 'RTE' == action['operator']
        assert 'hvdc' == action['network_element_id']

    def test_injection_range_actions(self):
        df = self.crac.get_injection_range_actions()
        assert ['name', 'operator', 'group_id', 'speed', 'activation_cost',
                'variation_cost_up', 'variation_cost_down'] == list(df.columns)
        action = df.loc['injectionRange1Id']

        assert 'injectionRange1Name' == action['name']
        assert 30 == action['speed']
        assert '800.0' == action['activation_cost']
        assert '2000.0' == action['variation_cost_up']

    def test_network_elements_ids_and_keys(self):
        df = self.crac.get_network_element_ids_and_keys()
        assert ['network_element_id', 'distribution_key'] == list(df.columns)
        expected = pd.DataFrame(index=pd.Series(name='id', data=['injectionRange1Id', 'injectionRange1Id']),
                                columns=['network_element_id', 'distribution_key'],
                                data=[['generator2Id', -1.0],
                                      ['generator1Id', 1.0]])
        pd.testing.assert_frame_equal(expected, df, check_dtype=False, check_like=True)

    def test_counter_trade_range_actions(self):
        df = self.crac.get_counter_trade_range_actions()
        assert ['name', 'operator', 'exporting_country', 'importing_country', 'group_id', 'speed', 'activation_cost',
                'variation_cost_up', 'variation_cost_down'] == list(df.columns)
        action = df.loc['counterTradeRange1Id']
        assert 'counterTradeRange1Name' == action['name']
        assert 30 == action['speed']
        assert '10000.0' == action['activation_cost']

    def test_ranges(self):
        df = self.crac.get_ranges()
        action_range = df.loc['pstRange2Id']
        assert ['min', 'max', 'range_type'] == list(action_range.columns)
        expected = pd.DataFrame(index=pd.Series(name='id', data=['pstRange2Id', 'pstRange2Id']),
                                columns=['min', 'max', 'range_type'],
                                data=[[1.0, 7.0, 'ABSOLUTE'],
                                      [-3.0, 3.0, 'RELATIVE_TO_INITIAL_NETWORK']])
        pd.testing.assert_frame_equal(expected, action_range, check_dtype=False, check_like=True)

    def test_network_actions(self):
        df = self.crac.get_network_actions()
        assert ['name', 'operator', 'speed', 'activation_cost'] == list(df.columns)
        action = df.loc['complexNetworkActionId']
        assert 'complexNetworkActionName' == action['name']
        assert 40.0 == action['speed']

    def test_terminal_connection_actions(self):
        df = self.crac.get_terminal_connection_actions()
        assert ['network_element_id', 'action_type'] == list(df.columns)
        action = df.loc['complexNetworkActionId']
        assert 'ne1Id' == action['network_element_id']
        assert 'close' == action['action_type']

    def test_pst_tap_position_actions(self):
        df = self.crac.get_pst_tap_position_actions()
        assert ['network_element_id', 'tap_position'] == list(df.columns)
        action = df.loc['pstSetpointRaId']
        assert 'pst' == action['network_element_id']
        assert 15 == action['tap_position']

    def test_generator_actions(self):
        df = self.crac.get_generator_actions()
        assert ['network_element_id', 'active_power_value'] == list(df.columns)
        action = df.loc['injectionSetpointRaId']
        assert 'injection' == action['network_element_id']
        assert '260.0' == action['active_power_value']

    def test_load_actions(self):
        df = self.crac.get_load_actions()
        assert ['network_element_id', 'active_power_value'] == list(df.columns)
        action = df.loc['complexNetworkAction2Id']
        assert 'LD1' == action['network_element_id']
        assert '260.0' == action['active_power_value']

    def test_shunt_compensator_position_actions(self):
        df = self.crac.get_shunt_compensator_position_actions()
        assert ['network_element_id', 'section_count'] == list(df.columns)
        action = df.loc['complexNetworkAction2Id']
        assert 'SC1' == action['network_element_id']
        assert 13 == action['section_count']

    def test_switch_actions(self):
        df = self.crac.get_switch_actions()
        assert ['network_element_id', 'action_type'] == list(df.columns)
        action = df.loc['complexNetworkAction2Id']
        assert 'BR1' == action['network_element_id']
        assert 'open' == action['action_type']

    def test_switch_pairs(self):
        df = self.crac.get_switch_pairs()
        assert ['open', 'close'] == list(df.columns)
        action = df.loc['switchPairRaId']
        assert 'to-open' == action['open']
        assert 'to-close' == action['close']

    def test_on_instant_usage_rules(self):
        df = self.crac.get_on_instant_usage_rules()
        assert ['instant'] == list(df.columns)
        action = df.loc['injectionSetpointRaId']
        assert 'auto' == action['instant']

    def test_on_contingency_state_usage_ruless(self):
        df = self.crac.get_on_contingency_state_usage_rules()
        assert ['instant', 'contingency_id'] == list(df.columns)
        action = df.loc['pstSetpointRaId']
        assert 'curative' == action['instant']
        assert 'contingency1Id' == action['contingency_id']

    def test_on_contingency_state_usage_rules(self):
        df = self.crac.get_on_constraint_usage_rules()
        assert ['instant', 'cnec_id'] == list(df.columns)
        action = df.loc['injectionSetpointRaId']
        assert 'auto' == action['instant']
        assert 'cnec3autoId' == action['cnec_id']

    def test_on_flow_constraint_in_country_usage_rules(self):
        df = self.crac.get_on_flow_constraint_in_country_usage_rules()
        assert ['instant', 'contingency_id', 'country'] == list(df.columns)
        action = df.loc['injectionSetpointRa2Id']
        assert 'curative' == action['instant']
        assert 'contingency2Id' == action['contingency_id']
        assert 'FR' == action['country']