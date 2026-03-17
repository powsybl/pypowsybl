# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.utils import path_to_str
from pypowsybl.utils import create_data_frame_from_series_array
from os import PathLike

from typing import (
    Union,
    Any
)

class Crac:
    """
    RAO Crac
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    @classmethod
    def from_file_source(cls, network: Network, crac_file: Union[str, PathLike]) -> Any :
        return Crac.from_buffer_source(network, io.BytesIO(open(path_to_str(crac_file), "rb").read()))

    @classmethod
    def from_buffer_source(cls, network: Network, crac_source: io.BytesIO) -> Any :
        return cls(_pypowsybl.load_crac_source(network._handle, crac_source.getbuffer()))

        """
    Get crac instants

    Returns:
        A dataframe containing the crac instants
    """
    def get_crac_instants(self):
        series = _pypowsybl.get_crac_instants(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get max remedial actions usage limit

    Returns:
        A dataframe containing the max remedial actions usage limit
    """
    def get_max_remedial_actions_usage_limit(self):
        series = _pypowsybl.get_max_remedial_actions_usage_limit(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get max tso usage limit

    Returns:
        A dataframe containing the max tso usage limit
    """
    def get_max_tso_usage_limit(self):
        series = _pypowsybl.get_max_tso_usage_limit(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get max topological action per tso usage limit

    Returns:
        A dataframe containing the max topological action per tso usage limit
    """
    def get_max_topological_action_per_tso_usage_limit(self):
        series = _pypowsybl.get_max_topological_action_per_tso_usage_limit(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get max pst action per tso usage limit

    Returns:
        A dataframe containing the max pst action per tso usage limit
    """
    def get_max_pst_action_per_tso_usage_limit(self):
        series = _pypowsybl.get_max_pst_action_per_tso_usage_limit(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get max remedial action per tso usage limit

    Returns:
        A dataframe containing the max remedial action per tso usage limit
    """
    def get_max_remedial_action_per_tso_usage_limit(self):
        series = _pypowsybl.get_max_remedial_action_per_tso_usage_limit(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get max elementary action per tso usage limit

    Returns:
        A dataframe containing the max elementary action per tso usage limit
    """
    def get_max_elementary_action_per_tso_usage_limit(self):
        series = _pypowsybl.get_max_elementary_action_per_tso_usage_limit(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac contingencies

    Returns:
        A dataframe containing the crac contingencies
    """
    def get_crac_contingencies(self):
        series = _pypowsybl.get_crac_contingencies(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac contingency elements

    Returns:
        A dataframe containing the crac contingency elements
    """
    def get_crac_contingency_elements(self):
        series = _pypowsybl.get_crac_contingency_elements(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac flow cnecs

    Returns:
        A dataframe containing the crac flow cnecs
    """
    def get_crac_flow_cnecs(self):
        series = _pypowsybl.get_crac_flow_cnecs(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac angle cnecs

    Returns:
        A dataframe containing the crac angle cnecs
    """
    def get_crac_angle_cnecs(self):
        series = _pypowsybl.get_crac_angle_cnecs(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac voltage cnecs

    Returns:
        A dataframe containing the crac voltage cnecs
    """
    def get_crac_voltage_cnecs(self):
        series = _pypowsybl.get_crac_voltage_cnecs(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac thresholds

    Returns:
        A dataframe containing the crac thresholds
    """
    def get_crac_thresholds(self):
        series = _pypowsybl.get_crac_thresholds(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac pst range actions

    Returns:
        A dataframe containing the crac pst range actions
    """
    def get_crac_pst_range_actions(self):
        series = _pypowsybl.get_crac_pst_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac hvdc range actions

    Returns:
        A dataframe containing the crac hvdc range actions
    """
    def get_crac_hvdc_range_actions(self):
        series = _pypowsybl.get_crac_hvdc_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac injection range actions

    Returns:
        A dataframe containing the crac injection range actions
    """
    def get_crac_injection_range_actions(self):
        series = _pypowsybl.get_crac_injection_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac injection ra elements

    Returns:
        A dataframe containing the crac injection ra elements
    """
    def get_crac_injection_ra_elements(self):
        series = _pypowsybl.get_crac_injection_ra_elements(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac counter trade range actions

    Returns:
        A dataframe containing the crac counter trade range actions
    """
    def get_crac_counter_trade_range_actions(self):
        series = _pypowsybl.get_crac_counter_trade_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac range action ranges

    Returns:
        A dataframe containing the crac range action ranges
    """
    def get_crac_range_action_ranges(self):
        series = _pypowsybl.get_crac_range_action_ranges(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get crac network actions

    Returns:
        A dataframe containing the crac network actions
    """
    def get_crac_network_actions(self):
        series = _pypowsybl.get_crac_network_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get terminal connection actions

    Returns:
        A dataframe containing the terminal connection actions
    """
    def get_terminal_connection_actions(self):
        series = _pypowsybl.get_terminal_connection_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get pst tap position actions

    Returns:
        A dataframe containing the pst tap position actions
    """
    def get_pst_tap_position_actions(self):
        series = _pypowsybl.get_pst_tap_position_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get generator actions

    Returns:
        A dataframe containing the generator actions
    """
    def get_generator_actions(self):
        series = _pypowsybl.get_generator_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get load actions

    Returns:
        A dataframe containing the load actions
    """
    def get_load_actions(self):
        series = _pypowsybl.get_load_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get dangling line actions

    Returns:
        A dataframe containing the dangling line actions
    """
    def get_dangling_line_actions(self):
        series = _pypowsybl.get_dangling_line_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get shunt compensator position actions

    Returns:
        A dataframe containing the shunt compensator position actions
    """
    def get_shunt_compensator_position_actions(self):
        series = _pypowsybl.get_shunt_compensator_position_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get switch actions

    Returns:
        A dataframe containing the switch actions
    """
    def get_switch_actions(self):
        series = _pypowsybl.get_switch_actions(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get switch pairs action

    Returns:
        A dataframe containing the switch pairs action
    """
    def get_switch_pairs_action(self):
        series = _pypowsybl.get_switch_pairs_action(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get on instant usage rule

    Returns:
        A dataframe containing the on instant usage rule
    """
    def get_on_instant_usage_rule(self):
        series = _pypowsybl.get_on_instant_usage_rule(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get on contingency state usage rule

    Returns:
        A dataframe containing the on contingency state usage rule
    """
    def get_on_contingency_state_usage_rule(self):
        series = _pypowsybl.get_on_contingency_state_usage_rule(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get on constraint usage rule

    Returns:
        A dataframe containing the on constraint usage rule
    """
    def get_on_constraint_usage_rule(self):
        series = _pypowsybl.get_on_constraint_usage_rule(self._handle)
        return create_data_frame_from_series_array(series)

    """
    Get on flow constraint in country usage rule

    Returns:
        A dataframe containing the on flow constrain in country usage rule
    """
    def get_on_flow_constraint_in_country_usage_rule(self):
        series = _pypowsybl.get_on_flow_constraint_in_country_usage_rule(self._handle)
        return create_data_frame_from_series_array(series)