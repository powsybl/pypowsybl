# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
from os import PathLike
from typing import (
    Optional,
    Union,
    Any,
    Dict,
    Literal
)

import pandas as pd
from pandas import DataFrame

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import PyPowsyblError
from pypowsybl.network import Network
from pypowsybl.utils import create_data_frame_from_series_array, _create_c_dataframe
from pypowsybl.utils import path_to_str


class Crac:
    """
    RAO Crac
    """

    def __init__(self, handle: _pypowsybl.JavaHandle, network: Optional[Network] = None):
        self._handle = handle
        # network the CRAC was created with, needed to add some CRAC objects (contingencies, flow CNECs, PSTs)
        self._network = network

    @classmethod
    def from_file_source(cls, network: Network, crac_file: Union[str, PathLike], creation_parameters_file: Optional[Union[str, PathLike]] = None) -> Any :
        if creation_parameters_file is not None:
            creation_parameters_source =  io.BytesIO(open(path_to_str(creation_parameters_file), "rb").read())
        else:
            creation_parameters_source = None
        return Crac.from_buffer_source(network, io.BytesIO(open(path_to_str(crac_file), "rb").read()), path_to_str(crac_file), creation_parameters_source)

    @classmethod
    def from_buffer_source(cls, network: Network, crac_source: io.BytesIO, crac_file_name: Optional[str] = None, parameters_source: Optional[io.BytesIO] = None) -> Any :
        if parameters_source is None:
            parameters_source = io.BytesIO(b"{\"crac-factory\": \"CracImplFactory\"}")
        if crac_file_name is None:
            crac_file_name = 'crac.json'
        return cls(_pypowsybl.load_crac_source_with_parameters(network._handle, crac_source.getbuffer(), crac_file_name, parameters_source.getbuffer()), network)

    def get_instants(self) -> DataFrame:
        """
        Get the instants defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the CRAC instants

        Notes:
            The resulting DataFrame, will include the following columns:
            
            - **kind**: the type of instant (preventive, outage, auto or curative)
            - **order**: the order of occurrence of the instant in the RAO workflow
            
            This DataFrame is indexed by the id of the instants.
        """
        series = _pypowsybl.get_instants(self._handle)
        return create_data_frame_from_series_array(series)

    def get_max_remedial_actions_usage_limits(self) -> DataFrame:
        """
        Get the maximum number of remedial actions that can be activated by the RAO per instant in a DataFrame.

        Returns:
            A DataFrame containing the maximum number of remedial actions that can be activated by the RAO per instant

        Notes:
            The resulting DataFrame, will include the following columns:

            - **value**: the maximum number of remedial actions that can be activated by the RAO at the given instant

            This DataFrame is indexed by the id of the instants.
        """
        series = _pypowsybl.get_max_remedial_actions_usage_limits(self._handle)
        return create_data_frame_from_series_array(series)

    def get_max_topological_actions_per_tso_usage_limits(self) -> DataFrame:
        """
        Get the maximum number of topological actions that can be activated by the RAO per instant for a given TSO in a
        DataFrame.

        Returns:
            A DataFrame containing the maximum number of topological actions that can be activated by the RAO per
            instant for a given TSO

        Notes:
            The resulting DataFrame, will include the following columns:

            - **value**: the maximum number of topological actions that can be activated by the RAO per instant for a given TSO

            This DataFrame is indexed by the id of the instants and the name of the TSOs.
        """
        series = _pypowsybl.get_max_topological_actions_per_tso_usage_limits(self._handle)
        return create_data_frame_from_series_array(series)

    def get_max_pst_actions_per_tso_usage_limits(self) -> DataFrame:
        """
        Get the maximum number of PST range actions that can be activated by the RAO per instant for a given TSO in a
        DataFrame.

        Returns:
            A DataFrame containing the maximum number of PST range actions that can be activated by the RAO per instant
            for a given TSO

        Notes:
            The resulting DataFrame, will include the following columns:

            - **value**: the maximum number of PST range actions that can be activated by the RAO per instant for a given TSO

            This DataFrame is indexed by the id of the instants and the name of the TSOs.
        """
        series = _pypowsybl.get_max_pst_actions_per_tso_usage_limits(self._handle)
        return create_data_frame_from_series_array(series)

    def get_max_remedial_actions_per_tso_usage_limits(self) -> DataFrame:
        """
        Get the maximum number of remedial actions that can be activated by the RAO per instant for a given TSO in a
        DataFrame.

        Returns:
            A DataFrame containing the maximum number of remedial actions that can be activated by the RAO per instant
            for a given TSO

        Notes:
            The resulting DataFrame, will include the following columns:

            - **value**: the maximum number of remedial actions that can be activated by the RAO per instant for a given TSO

            This DataFrame is indexed by the id of the instants and the name of the TSOs.
        """
        series = _pypowsybl.get_max_remedial_actions_per_tso_usage_limits(self._handle)
        return create_data_frame_from_series_array(series)

    def get_max_elementary_actions_per_tso_usage_limits(self) -> DataFrame:
        """
        Get the maximum number of elementary actions that can be activated by the RAO per instant for a given TSO in a
        DataFrame.

        Returns:
            A DataFrame containing the maximum number of elementary actions that can be activated by the RAO per instant
            for a given TSO

        Notes:
            The resulting DataFrame, will include the following columns:

            - **value**: the maximum number of elementary actions that can be activated by the RAO per instant for a given TSO

            This DataFrame is indexed by the id of the instants and the name of the TSOs.
        """
        series = _pypowsybl.get_max_elementary_actions_per_tso_usage_limits(self._handle)
        return create_data_frame_from_series_array(series)

    def get_contingencies(self) -> DataFrame:
        """
        Get the CRAC contingencies in a DataFrame.

        Returns:
            A DataFrame containing the CRAC contingencies

        Notes:
            This DataFrame is indexed by the id of the contingencies.
        """
        series = _pypowsybl.get_crac_contingencies(self._handle)
        return create_data_frame_from_series_array(series)

    def get_contingency_elements(self) -> DataFrame:
        """
        Get the contingency elements for each contingency defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the contingency elements for each contingency defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the network element

            This DataFrame is indexed by the id of the contingencies.
        """
        series = _pypowsybl.get_crac_contingency_elements(self._handle)
        return create_data_frame_from_series_array(series)

    def get_flow_cnecs(self) -> DataFrame:
        """
        Get the FlowCNECs defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the FlowCNECs defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the FlowCNEC
            - **network_element_id**: the id of the network element the FlowCNEC is defined upon
            - **operator**: the name of the operator the FlowCNEC belongs to
            - **border**: the border to which the element of the FlowCNEC is associated
            - **instant**: the instant at which the FlowCNEC is defined
            - **contingency_id**: the id of the contingency after which the FlowCNEC is defined
            - **optimized**: whether the FlowCNEC is optimized or not by the RAO
            - **monitored**: whether the FlowCNEC is monitored or not by the RAO
            - **reliability_margin**: the reliability margin of the FlowCNEC in MW

            This DataFrame is indexed by the id of the FlowCNECs.
        """
        series = _pypowsybl.get_flow_cnecs(self._handle)
        return create_data_frame_from_series_array(series)

    def get_angle_cnecs(self) -> DataFrame:
        """
        Get the AngleCNECs defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the AngleCNECs defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the AngleCNEC
            - **exporting_network_element_id**: the id of the exporting network element the AngleCNEC is defined upon
            - **importing_network_element_id**: the id of the importing network element the AngleCNEC is defined upon
            - **operator**: the name of the operator the AngleCNEC belongs to
            - **border**: the border to which the element of the AngleCNEC is associated
            - **instant**: the instant at which the AngleCNEC is defined
            - **contingency_id**: the id of the contingency after which the AngleCNEC is defined
            - **optimized**: whether the AngleCNEC is optimized or not by the RAO
            - **monitored**: whether the AngleCNEC is monitored or not by the RAO
            - **reliability_margin**: the reliability margin of the AngleCNEC in degrees

            This DataFrame is indexed by the id of the AngleCNECs.
        """
        series = _pypowsybl.get_angle_cnecs(self._handle)
        return create_data_frame_from_series_array(series)

    def get_voltage_cnecs(self) -> DataFrame:
        """
        Get the VoltageCNECs defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the VoltageCNECs defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the VoltageCNEC
            - **network_element_id**: the id of the network element the VoltageCNEC is defined upon
            - **operator**: the name of the operator the VoltageCNEC belongs to
            - **border**: the border to which the element of the VoltageCNEC is associated
            - **instant**: the instant at which the VoltageCNEC is defined
            - **contingency_id**: the id of the contingency after which the VoltageCNEC is defined
            - **optimized**: whether the VoltageCNEC is optimized or not by the RAO
            - **monitored**: whether the VoltageCNEC is monitored or not by the RAO
            - **reliability_margin**: the reliability margin of the VoltageCNEC in kV

            This DataFrame is indexed by the id of the VoltageCNECs.
        """
        series = _pypowsybl.get_voltage_cnecs(self._handle)
        return create_data_frame_from_series_array(series)

    def get_thresholds(self) -> DataFrame:
        """
        Get the thresholds of the CNECs defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the thresholds of the CNECs defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **min**: the lower bound of the threshold
            - **max**: the upper bound of the threshold
            - **units**: the physical unit of the threshold
            - **side**: the network element side on which the threshold holds (only for flow thresholds)

            This DataFrame is indexed by the id of the CNECs.
        """
        series = _pypowsybl.get_thresholds(self._handle)
        return create_data_frame_from_series_array(series)

    def get_pst_range_actions(self) -> DataFrame:
        """
        Get the PST range actions defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the PST range actions defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the PST range action
            - **operator**: the name of the operator the PST range action belongs to
            - **network_element_id**: the id of the PST the remedial action is defined upon
            - **group_id**: the id of the group the PST range action belongs to
            - **speed**: the activation speed of the PST range action, if it is an automaton
            - **activation_cost**: the amount of money to spend to activate the PST range action
            - **variation_cost_up**: the amount of money to spend to increase the PST tap position in unit per tap
            - **variation_cost_down**: the amount of money to spend to decrease the PST tap position in unit per tap

            This DataFrame is indexed by the id of the PST range actions.
        """
        series = _pypowsybl.get_crac_pst_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_hvdc_range_actions(self) -> DataFrame:
        """
        Get the HVDC range actions defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the HVDC range actions defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the HVDC range action
            - **operator**: the name of the operator the HVDC range action belongs to
            - **network_element_id**: the id of the HVDC the remedial action is defined upon
            - **group_id**: the id of the group the HVDC range action belongs to
            - **speed**: the activation speed of the HVDC range action, if it is an automaton
            - **activation_cost**: the amount of money to spend to activate the HVDC range action
            - **variation_cost_up**: the amount of money to spend to increase the HVDC set-point in unit per MW
            - **variation_cost_down**: the amount of money to spend to decrease the HVDC set-point in unit per MW

            This DataFrame is indexed by the id of the HVDC range actions.
        """
        series = _pypowsybl.get_crac_hvdc_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_injection_range_actions(self) -> DataFrame:
        """
        Get the injection range actions defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the injection range actions defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the injection range action
            - **operator**: the name of the operator the injection range action belongs to
            - **network_element_id**: the id of the injection the remedial action is defined upon
            - **group_id**: the id of the group the injection range action belongs to
            - **speed**: the activation speed of the injection range action, if it is an automaton
            - **activation_cost**: the amount of money to spend to activate the injection range action
            - **variation_cost_up**: the amount of money to spend to increase the injection set-point in unit per MW
            - **variation_cost_down**: the amount of money to spend to decrease the injection set-point in unit per MW

            This DataFrame is indexed by the id of the injection range actions.
        """
        series = _pypowsybl.get_crac_injection_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_network_element_ids_and_keys(self) -> DataFrame:
        """
        Get the network elements and their associated distribution key for each injection range action defined in the
        CRAC in a DataFrame.

        Returns:
            A DataFrame containing the network elements and their associated distribution key for each injection range
            action defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the network element the injection range action is defined upon
            - **distribution_key**: the distribution key of the network element in the injection range action

            This DataFrame is indexed by the id of the injection range actions.
        """
        series = _pypowsybl.get_network_element_ids_and_keys(self._handle)
        return create_data_frame_from_series_array(series)

    def get_counter_trade_range_actions(self) -> DataFrame:
        """
        Get the counter-trade range actions defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the counter-trade range actions defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the counter-trade range action
            - **operator**: the name of the operator the counter-trade range action belongs to
            - **exporting_area**: the name of the area the power is exported from
            - **importing_area**: the name of the area the power is imported in
            - **group_id**: the id of the group the counter-trade range action belongs to
            - **speed**: the activation speed of the counter-trade range action, if it is an automaton
            - **activation_cost**: the amount of money to spend to activate the counter-trade range action
            - **variation_cost_up**: the amount of money to spend to increase the counter-trade set-point in unit per MW
            - **variation_cost_down**: the amount of money to spend to decrease the counter-trade set-point in unit per MW

            This DataFrame is indexed by the id of the counter-trade range actions.
        """
        series = _pypowsybl.get_crac_counter_trade_range_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_ranges(self) -> DataFrame:
        """
        Get the admissible ranges of the range actions defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the admissible ranges of the range actions defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **min**: the lower bound of the range
            - **max**: the upper bound of the range
            - **range_type**: the type of range (absolute, relative to previous instant, relative to initial network)

            This DataFrame is indexed by the id of the range actions.
        """
        series = _pypowsybl.get_crac_range_action_ranges(self._handle)
        return create_data_frame_from_series_array(series)

    def get_network_actions(self) -> DataFrame:
        """
        Get the network actions defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the network actions defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **name**: the name of the network action
            - **operator**: the name of the operator the network action belongs to
            - **speed**: the activation speed of the network action, if it is an automaton
            - **activation_cost**: the amount of money to spend to activate the network action

            This DataFrame is indexed by the id of the network actions.
        """
        series = _pypowsybl.get_crac_network_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_terminal_connection_actions(self) -> DataFrame:
        """
        Get the terminal connection actions defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing terminal the connection actions defined as elementary actions of network actions in the
            CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the terminal the action is defined upon
            - **action_type**: whether to connect (close) or disconnect (open) the terminal

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_terminal_connection_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_pst_tap_position_actions(self) -> DataFrame:
        """
        Get the PST tap position actions defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the PST tap position actions defined as elementary actions of network actions in the
            CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the PST the action is defined upon
            - **tap_position**: the target tap position of the PST

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_pst_tap_position_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_generator_actions(self) -> DataFrame:
        """
        Get the generator actions defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the generator actions defined as elementary actions of network actions in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the generator the action is defined upon
            - **active_power_value**: the target set-point of the generator

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_generator_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_load_actions(self) -> DataFrame:
        """
        Get the load actions defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the load actions defined as elementary actions of network actions in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the load the action is defined upon
            - **active_power_value**: the target set-point of the load

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_load_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_boundary_line_actions(self) -> DataFrame:
        """
        Get the boundary line actions defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the boundary line actions defined as elementary actions of network actions in the
            CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the boundary line the action is defined upon
            - **active_power_value**: the target set-point of the boundary line

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_boundary_line_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_shunt_compensator_position_actions(self) -> DataFrame:
        """
        Get the shunt compensator position actions defined as elementary actions of network actions in the CRAC in a
        DataFrame.

        Returns:
            A DataFrame containing the shunt compensator position actions defined as elementary actions of network
            actions in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the shunt compensator the action is defined upon
            - **section_count**: the target number of section of the shunt compensator

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_shunt_compensator_position_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_switch_actions(self) -> DataFrame:
        """
        Get the switch actions defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the switch actions defined as elementary actions of network actions in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **network_element_id**: the id of the switch the action is defined upon
            - **action_type**: whether to open or close the switch

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_switch_actions(self._handle)
        return create_data_frame_from_series_array(series)

    def get_switch_pairs(self) -> DataFrame:
        """
        Get the switch pairs defined as elementary actions of network actions in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the switch pairs defined as elementary actions of network actions in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **open**: the id of the switch to open
            - **close**: the id of the switch to close

            This DataFrame is indexed by the id of the parent network actions.
        """
        series = _pypowsybl.get_crac_switch_pairs(self._handle)
        return create_data_frame_from_series_array(series)

    def get_on_instant_usage_rules(self) -> DataFrame:
        """
        Get the remedial actions on-instant usage rules defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the on-instant usage rules defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **instant**: the instant at which the usage rule holds

            This DataFrame is indexed by the id of the parent remedial actions.
        """
        series = _pypowsybl.get_on_instant_usage_rules(self._handle)
        return create_data_frame_from_series_array(series)

    def get_on_contingency_state_usage_rules(self) -> DataFrame:
        """
        Get the remedial actions on-contingency-state usage rules defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the on-contingency-state usage rules defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **instant**: the instant at which the usage rule holds
            - **contingency_id**: the contingency after which the usage rule holds

            This DataFrame is indexed by the id of the parent remedial actions.
        """
        series = _pypowsybl.get_on_contingency_state_usage_rules(self._handle)
        return create_data_frame_from_series_array(series)

    def get_on_constraint_usage_rules(self) -> DataFrame:
        """
        Get the remedial actions on-constraint usage rules defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the on-constraint usage rules defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **instant**: the instant at which the usage rule holds
            - **cnec_id**: the id of the CNEC that needs to be in violation of the usage rule to be valid

            This DataFrame is indexed by the id of the parent remedial actions.
        """
        series = _pypowsybl.get_on_constraint_usage_rules(self._handle)
        return create_data_frame_from_series_array(series)

    def get_on_flow_constraint_in_country_usage_rules(self) -> DataFrame:
        """
        Get the remedial actions on-flow-constraint-in-country usage rules defined in the CRAC in a DataFrame.

        Returns:
            A DataFrame containing the on-flow-constraint-in-country usage rules defined in the CRAC

        Notes:
            The resulting DataFrame, will include the following columns:

            - **instant**: the instant at which the usage rule holds
            - **contingency_id**: the contingency after which the usage rule holds
            - **country**: the country in which a FlowCNEC needs to be in violation of the usage rule to be valid

            This DataFrame is indexed by the id of the parent remedial actions.
        """
        series = _pypowsybl.get_on_flow_constraint_in_country_usage_rules(self._handle)
        return create_data_frame_from_series_array(series)

    def _get_network_handle(self) -> _pypowsybl.JavaHandle:
        if self._network is None:
            raise PyPowsyblError('This operation requires the network the CRAC was created with')
        return self._network._handle  # pylint: disable=protected-access

    def add_instant(self, _id: str, instant_kind: Literal["preventive", "outage", "auto", "curative"]) -> None:
        """
        Add an instant to the CRAC. Instants are ordered by their order of creation.

        Args:
            _id: the id of the instant
            instant_kind: the kind of instant (preventive, outage, auto or curative)
        """
        _pypowsybl.add_crac_instant(self._handle, _id, instant_kind)

    def add_remedial_action_usage_limits(
        self,
        instant_id: str,
        max_remedial_actions: Optional[int] = None,
        max_tsos: Optional[int] = None,
        max_topological_actions_per_tso: Optional[Dict[str, int]] = None,
        max_psts_per_tso: Optional[Dict[str, int]] = None,
        max_remedial_actions_per_tso: Optional[Dict[str, int]] = None,
        max_elementary_actions_per_tso: Optional[Dict[str, int]] = None
    ) -> None:
        """
        Add remedial action usage limits for a given instant.

        Args:
            instant_id: the id of the instant the limits apply to
            max_remedial_actions: the maximum number of remedial actions
            max_tsos: the maximum number of TSOs that can activate remedial actions
            max_topological_actions_per_tso: the maximum number of topological actions per TSO
            max_psts_per_tso: the maximum number of PST range actions per TSO
            max_remedial_actions_per_tso: the maximum number of remedial actions per TSO
            max_elementary_actions_per_tso: the maximum number of elementary actions per TSO
        """
        attributes = _attributes_c_df('instant', instant_id,
                                      ints={'max_remedial_actions': max_remedial_actions, 'max_tsos': max_tsos})
        per_tso_rows = [(limit_type, tso, value)
                        for limit_type, limits in (('max_topological_actions_per_tso', max_topological_actions_per_tso),
                                                   ('max_psts_per_tso', max_psts_per_tso),
                                                   ('max_remedial_actions_per_tso', max_remedial_actions_per_tso),
                                                   ('max_elementary_actions_per_tso', max_elementary_actions_per_tso))
                        if limits is not None
                        for tso, value in limits.items()]
        per_tso_limits = _sub_c_df(pd.DataFrame(per_tso_rows, columns=['limit_type', 'tso', 'value']),
                                   {'limit_type': _STRING, 'tso': _STRING, 'value': _INT})
        _pypowsybl.add_crac_ra_usage_limits(self._handle, attributes, per_tso_limits)

    def add_contingency(self, _id: str, *network_elements_ids: str) -> None:
        """
        Add a contingency to the CRAC. The type of each contingency element is deduced from the network.

        Args:
            _id: the id of the contingency
            network_elements_ids: the ids of the network elements lost in the contingency
        """
        _pypowsybl.add_crac_contingency(self._handle, self._get_network_handle(), _id, list(network_elements_ids))

    def add_flow_cnec(
        self,
        _id: str,
        network_element_id: str,
        instant_id: str,
        thresholds: pd.DataFrame, # columns: unit (Literal["megawatt", "ampere", "percent_i_max"]), lower_bound (float | None), upper_bound (float | None), side (Literal["one", "two"]])
        name: Optional[str] = None,
        operator: Optional[str] = None,
        border: Optional[str] = None,
        contingency_id: Optional[str] = None,
        optimized: bool = True,
        monitored: bool = False,
        reliability_margin: Optional[float | int] = None,
    ) -> None:
        """
        Add a flow CNEC to the CRAC. Nominal voltages and maximum currents of the branch are read from the network.

        Args:
            _id: the id of the CNEC
            network_element_id: the id of the monitored branch
            instant_id: the id of the instant of the CNEC
            thresholds: the thresholds, with columns unit (megawatt, ampere or percent_i_max), lower_bound, upper_bound
                and side (one or two)
            name: the name of the CNEC
            operator: the operator of the CNEC
            border: the border of the CNEC
            contingency_id: the id of the contingency of the CNEC (mandatory for non preventive CNECs)
            optimized: whether the CNEC is optimized
            monitored: whether the CNEC is monitored
            reliability_margin: the reliability margin of the CNEC
        """
        attributes = _cnec_attributes(_id, instant_id, name, operator, border, contingency_id, optimized, monitored,
                                      reliability_margin, network_element_id=network_element_id)
        c_thresholds = _sub_c_df(thresholds, {'unit': _STRING, 'lower_bound': _DOUBLE, 'upper_bound': _DOUBLE, 'side': _STRING})
        _pypowsybl.add_crac_flow_cnec(self._handle, self._get_network_handle(), attributes, c_thresholds)

    def add_angle_cnec(
        self,
        _id: str,
        exporting_network_element_id: str,
        importing_network_element_id: str,
        instant_id: str,
        thresholds: pd.DataFrame, # columns: unit (Literal["degree"]), lower_bound (float | None), upper_bound (float | None)
        name: Optional[str] = None,
        operator: Optional[str] = None,
        border: Optional[str] = None,
        contingency_id: Optional[str] = None,
        optimized: bool = False,
        monitored: bool = True,
        reliability_margin: Optional[float | int] = None
    ) -> None:
        """
        Add an angle CNEC to the CRAC.

        Args:
            _id: the id of the CNEC
            exporting_network_element_id: the id of the exporting network element
            importing_network_element_id: the id of the importing network element
            instant_id: the id of the instant of the CNEC
            thresholds: the thresholds, with columns unit (degree), lower_bound and upper_bound
            name: the name of the CNEC
            operator: the operator of the CNEC
            border: the border of the CNEC
            contingency_id: the id of the contingency of the CNEC (mandatory for non preventive CNECs)
            optimized: whether the CNEC is optimized (not supported by OpenRAO for angle CNECs)
            monitored: whether the CNEC is monitored
            reliability_margin: the reliability margin of the CNEC
        """
        attributes = _cnec_attributes(_id, instant_id, name, operator, border, contingency_id, optimized, monitored,
                                      reliability_margin,
                                      exporting_network_element_id=exporting_network_element_id,
                                      importing_network_element_id=importing_network_element_id)
        c_thresholds = _sub_c_df(thresholds, {'unit': _STRING, 'lower_bound': _DOUBLE, 'upper_bound': _DOUBLE})
        _pypowsybl.add_crac_angle_cnec(self._handle, attributes, c_thresholds)

    def add_voltage_cnec(
        self,
        _id: str,
        network_element_id: str,
        instant_id: str,
        thresholds: pd.DataFrame, # columns: unit (Literal["kilovolt"]), lower_bound (float | None), upper_bound (float | None)
        name: Optional[str] = None,
        operator: Optional[str] = None,
        border: Optional[str] = None,
        contingency_id: Optional[str] = None,
        optimized: bool = True,
        monitored: bool = False,
        reliability_margin: Optional[float | int] = None
    ) -> None:
        """
        Add a voltage CNEC to the CRAC.

        Args:
            _id: the id of the CNEC
            network_element_id: the id of the monitored network element
            instant_id: the id of the instant of the CNEC
            thresholds: the thresholds, with columns unit (kilovolt), lower_bound and upper_bound
            name: the name of the CNEC
            operator: the operator of the CNEC
            border: the border of the CNEC
            contingency_id: the id of the contingency of the CNEC (mandatory for non preventive CNECs)
            optimized: whether the CNEC is optimized (not supported by OpenRAO for voltage CNECs)
            monitored: whether the CNEC is monitored
            reliability_margin: the reliability margin of the CNEC
        """
        attributes = _cnec_attributes(_id, instant_id, name, operator, border, contingency_id, optimized, monitored,
                                      reliability_margin, network_element_id=network_element_id)
        c_thresholds = _sub_c_df(thresholds, {'unit': _STRING, 'lower_bound': _DOUBLE, 'upper_bound': _DOUBLE})
        _pypowsybl.add_crac_voltage_cnec(self._handle, attributes, c_thresholds)

    def add_network_action(
        self,
        _id: str,
        name: Optional[str] = None,
        operator: Optional[str] = None,
        speed: Optional[int] = None,
        activation_cost: Optional[float | int] = None,
        terminal_connection_action: Optional[pd.DataFrame] = None, # columns: terminal_id (str), action_type (Literal["open", "close"])
        phase_tap_changer_tap_position_actions: Optional[pd.DataFrame] = None, # columns: phase_tap_changer_id (str), tap_position (int)
        generator_actions: Optional[pd.DataFrame] = None, # columns: generator_id (str), active_power_value (float)
        load_actions: Optional[pd.DataFrame] = None, # columns: load_id (str), active_power_value (float)
        dangling_line_actions: Optional[pd.DataFrame] = None, # columns: dangling_line_id (str), active_power_value (float)
        shunt_compensator_position_actions: Optional[pd.DataFrame] = None, # columns: shunt_compensator_id (str), section_count (int)
        switch_actions: Optional[pd.DataFrame] = None, # columns: switch_id (str), action_type (Literal["open", "close"])
        switch_pairs: Optional[pd.DataFrame] = None, # columns: switch_to_open_id (str), switch_to_close_id (str),
        on_instant_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str)
        on_contingency_state_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), contingency_id (str)
        on_constraint_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), cnec_id (str)
        on_flow_constraint_in_country_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), country (str), contingency_id (str | None)
    ) -> None:
        """
        Add a network action to the CRAC, made of one or several elementary actions.

        Each elementary action type and each usage rule type is described by a dataframe, one row per elementary
        action or usage rule.
        """
        attributes = _remedial_action_attributes(_id, name, operator, speed, activation_cost)
        _pypowsybl.add_crac_network_action(
            self._handle, attributes,
            _sub_c_df(terminal_connection_action, {'terminal_id': _STRING, 'action_type': _STRING}),
            _sub_c_df(phase_tap_changer_tap_position_actions, {'phase_tap_changer_id': _STRING, 'tap_position': _INT}),
            _sub_c_df(generator_actions, {'generator_id': _STRING, 'active_power_value': _DOUBLE}),
            _sub_c_df(load_actions, {'load_id': _STRING, 'active_power_value': _DOUBLE}),
            _sub_c_df(dangling_line_actions, {'dangling_line_id': _STRING, 'active_power_value': _DOUBLE}),
            _sub_c_df(shunt_compensator_position_actions, {'shunt_compensator_id': _STRING, 'section_count': _INT}),
            _sub_c_df(switch_actions, {'switch_id': _STRING, 'action_type': _STRING}),
            _sub_c_df(switch_pairs, {'switch_to_open_id': _STRING, 'switch_to_close_id': _STRING}),
            _usage_rules_c_df(on_instant_usage_rules, on_contingency_state_usage_rules, on_constraint_usage_rules,
                              on_flow_constraint_in_country_usage_rules))

    def add_pst_range_action(
        self,
        _id: str,
        network_element_id: str,
        name: Optional[str] = None,
        operator: Optional[str] = None,
        speed: Optional[int] = None,
        activation_cost: Optional[float | int] = None,
        upward_variation_cost: Optional[float | int] = None,
        downward_variation_cost: Optional[float | int] = None,
        group_id: Optional[str] = None,
        ranges: Optional[pd.DataFrame] = None, # columns: lower_bound (int | None), upper_bound (int | None), range_type (Literal["absolute", "relative_to_previous_instant", "relative_to_initial_network", "relative_to_previous_time_step"])
        on_instant_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str)
        on_contingency_state_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), contingency_id (str)
        on_constraint_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), cnec_id (str)
        on_flow_constraint_in_country_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), country (str), contingency_id (str | None)
    ) -> None:
        """
        Add a PST range action to the CRAC. The initial tap and the tap to angle conversion map are read from the network.
        """
        attributes = _standard_range_action_attributes(_id, name, operator, speed, activation_cost, upward_variation_cost,
                                                       downward_variation_cost, group_id,
                                                       network_element_id=network_element_id)
        _pypowsybl.add_crac_pst_range_action(
            self._handle, self._get_network_handle(), attributes,
            # tap bounds are sent as floats so that they can be undefined (NaN)
            _sub_c_df(ranges, _RANGES_COLUMNS),
            _usage_rules_c_df(on_instant_usage_rules, on_contingency_state_usage_rules, on_constraint_usage_rules,
                              on_flow_constraint_in_country_usage_rules))

    def add_hvdc_range_action(
        self,
        _id: str,
        network_element_id: str,
        ranges: pd.DataFrame, # columns: lower_bound (float | None), upper_bound (float | None), range_type (Literal["absolute", "relative_to_previous_instant", "relative_to_initial_network", "relative_to_previous_time_step"])
        name: Optional[str] = None,
        operator: Optional[str] = None,
        speed: Optional[int] = None,
        activation_cost: Optional[float | int] = None,
        upward_variation_cost: Optional[float | int] = None,
        downward_variation_cost: Optional[float | int] = None,
        group_id: Optional[str] = None,
        on_instant_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str)
        on_contingency_state_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), contingency_id (str)
        on_constraint_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), cnec_id (str)
        on_flow_constraint_in_country_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), country (str), contingency_id (str | None)
    ) -> None:
        """
        Add an HVDC range action to the CRAC.
        """
        attributes = _standard_range_action_attributes(_id, name, operator, speed, activation_cost, upward_variation_cost,
                                                       downward_variation_cost, group_id,
                                                       network_element_id=network_element_id)
        _pypowsybl.add_crac_hvdc_range_action(
            self._handle, attributes, _sub_c_df(ranges, _RANGES_COLUMNS),
            _usage_rules_c_df(on_instant_usage_rules, on_contingency_state_usage_rules, on_constraint_usage_rules,
                              on_flow_constraint_in_country_usage_rules))

    def add_injection_range_action(
        self,
        _id: str,
        ranges: pd.DataFrame, # columns: lower_bound (float | None), upper_bound (float | None), range_type (Literal["absolute", "relative_to_previous_instant", "relative_to_initial_network", "relative_to_previous_time_step"])
        network_elements_and_keys: Dict[str, float | int],
        name: Optional[str] = None,
        operator: Optional[str] = None,
        speed: Optional[int] = None,
        activation_cost: Optional[float | int] = None,
        upward_variation_cost: Optional[float | int] = None,
        downward_variation_cost: Optional[float | int] = None,
        group_id: Optional[str] = None,
        on_instant_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str)
        on_contingency_state_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), contingency_id (str)
        on_constraint_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), cnec_id (str)
        on_flow_constraint_in_country_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), country (str), contingency_id (str | None)
    ) -> None:
        """
        Add an injection range action to the CRAC, acting on several injections with distribution keys.
        """
        attributes = _standard_range_action_attributes(_id, name, operator, speed, activation_cost, upward_variation_cost,
                                                       downward_variation_cost, group_id)
        elements_and_keys = pd.DataFrame(list(network_elements_and_keys.items()), columns=['network_element_id', 'key'])
        _pypowsybl.add_crac_injection_range_action(
            self._handle, attributes,
            _sub_c_df(elements_and_keys, {'network_element_id': _STRING, 'key': _DOUBLE}),
            _sub_c_df(ranges, _RANGES_COLUMNS),
            _usage_rules_c_df(on_instant_usage_rules, on_contingency_state_usage_rules, on_constraint_usage_rules,
                              on_flow_constraint_in_country_usage_rules))

    def add_counter_trade_range_action(
        self,
        _id: str,
        ranges: pd.DataFrame, # columns: lower_bound (float | None), upper_bound (float | None), range_type (Literal["absolute", "relative_to_previous_instant", "relative_to_initial_network", "relative_to_previous_time_step"])
        exporting_country: str,
        importing_country: str,
        name: Optional[str] = None,
        operator: Optional[str] = None,
        speed: Optional[int] = None,
        activation_cost: Optional[float | int] = None,
        upward_variation_cost: Optional[float | int] = None,
        downward_variation_cost: Optional[float | int] = None,
        group_id: Optional[str] = None,
        on_instant_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str)
        on_contingency_state_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), contingency_id (str)
        on_constraint_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), cnec_id (str)
        on_flow_constraint_in_country_usage_rules: Optional[pd.DataFrame] = None, # columns: instant (str), country (str), contingency_id (str | None)
    ) -> None:
        """
        Add a counter trade range action to the CRAC, between two countries.
        """
        attributes = _standard_range_action_attributes(_id, name, operator, speed, activation_cost, upward_variation_cost,
                                                       downward_variation_cost, group_id,
                                                       exporting_country=exporting_country,
                                                       importing_country=importing_country)
        _pypowsybl.add_crac_counter_trade_range_action(
            self._handle, attributes, _sub_c_df(ranges, _RANGES_COLUMNS),
            _usage_rules_c_df(on_instant_usage_rules, on_contingency_state_usage_rules, on_constraint_usage_rules,
                              on_flow_constraint_in_country_usage_rules))


# series types of the C dataframes
_STRING = 0
_DOUBLE = 1
_INT = 2
_BOOL = 3

_RANGES_COLUMNS = {'lower_bound': _DOUBLE, 'upper_bound': _DOUBLE, 'range_type': _STRING}

_USAGE_RULES_COLUMNS = {'usage_rule_type': _STRING, 'instant': _STRING, 'contingency_id': _STRING,
                        'cnec_id': _STRING, 'country': _STRING}


def _metadata(name: str, series_type: int, is_index: bool = False) -> _pypowsybl.SeriesMetadata:
    return _pypowsybl.SeriesMetadata(name, series_type, is_index, False, False)


def _attributes_c_df(index_name: str, index_value: str,
                     strings: Optional[Dict[str, Optional[str]]] = None,
                     doubles: Optional[Dict[str, Optional[float]]] = None,
                     ints: Optional[Dict[str, Optional[int]]] = None,
                     bools: Optional[Dict[str, Optional[bool]]] = None) -> _pypowsybl.Dataframe:
    """
    Creates a one row C dataframe holding the attributes of a CRAC object.
    Undefined (None) attributes are not sent.
    """
    metadata = [_metadata(index_name, _STRING, True)]
    values: Dict[str, Any] = {}
    for series_type, attributes, convert in ((_STRING, strings, str), (_DOUBLE, doubles, float),
                                             (_INT, ints, int), (_BOOL, bools, bool)):
        for name, value in (attributes or {}).items():
            if value is not None:
                metadata.append(_metadata(name, series_type))
                values[name] = [convert(value)]
    df = pd.DataFrame(values, index=pd.Index([index_value], name=index_name))
    return _create_c_dataframe(df, metadata)


def _cnec_attributes(_id: str, instant_id: str, name: Optional[str], operator: Optional[str], border: Optional[str],
                     contingency_id: Optional[str], optimized: bool, monitored: bool,
                     reliability_margin: Optional[float | int], **network_elements: str) -> _pypowsybl.Dataframe:
    return _attributes_c_df('id', _id,
                            strings={'instant': instant_id, 'name': name, 'operator': operator, 'border': border,
                                     'contingency_id': contingency_id, **network_elements},
                            doubles={'reliability_margin': reliability_margin},
                            bools={'optimized': optimized, 'monitored': monitored})


def _remedial_action_attributes(_id: str, name: Optional[str], operator: Optional[str], speed: Optional[int],
                                activation_cost: Optional[float | int],
                                doubles: Optional[Dict[str, Optional[float | int]]] = None,
                                **strings: Optional[str]) -> _pypowsybl.Dataframe:
    return _attributes_c_df('id', _id,
                            strings={'name': name, 'operator': operator, **strings},
                            doubles={'activation_cost': activation_cost, **(doubles or {})},
                            ints={'speed': speed})


def _standard_range_action_attributes(_id: str, name: Optional[str], operator: Optional[str], speed: Optional[int],
                                      activation_cost: Optional[float | int],
                                      upward_variation_cost: Optional[float | int],
                                      downward_variation_cost: Optional[float | int],
                                      group_id: Optional[str], **strings: str) -> _pypowsybl.Dataframe:
    return _remedial_action_attributes(_id, name, operator, speed, activation_cost,
                                       doubles={'upward_variation_cost': upward_variation_cost,
                                                'downward_variation_cost': downward_variation_cost},
                                       group_id=group_id, **strings)


def _flatten_index(df: DataFrame) -> DataFrame:
    """
    Moves named index levels to columns and replaces the index by a default range index.
    """
    if any(index_name is not None for index_name in df.index.names):
        return df.reset_index()
    return df.reset_index(drop=True)


def _sub_c_df(df: Optional[DataFrame], columns: Dict[str, int]) -> Optional[_pypowsybl.Dataframe]:
    """
    Creates the C dataframe of sub-elements of a CRAC object (thresholds, ranges, elementary actions...).
    Missing values of string columns are sent as empty strings, missing values of float columns as NaN.
    """
    if df is None:
        return None
    df = _flatten_index(df)
    unknown_columns = set(df.columns) - set(columns)
    if unknown_columns:
        raise PyPowsyblError(f'Unexpected columns {sorted(unknown_columns)}, expected columns are {list(columns)}')
    df = df.copy()
    for column in df.columns:
        series_type = columns[column]
        if series_type == _STRING:
            df[column] = df[column].fillna('').astype(str)
        elif series_type == _DOUBLE:
            df[column] = df[column].astype(float)
        else:
            df[column] = df[column].astype(int)
    # the default range index is sent as an int index column
    metadata = [_metadata('row', _INT, True)] + [_metadata(name, series_type) for name, series_type in columns.items()]
    return _create_c_dataframe(df, metadata)


def _usage_rules_c_df(on_instant_usage_rules: Optional[DataFrame],
                      on_contingency_state_usage_rules: Optional[DataFrame],
                      on_constraint_usage_rules: Optional[DataFrame],
                      on_flow_constraint_in_country_usage_rules: Optional[DataFrame]) -> Optional[_pypowsybl.Dataframe]:
    """
    Merges all the usage rules of a remedial action in a single C dataframe, with a usage_rule_type column.
    """
    usage_rules = []
    for usage_rule_type, df in (('on_instant', on_instant_usage_rules),
                                ('on_contingency_state', on_contingency_state_usage_rules),
                                ('on_constraint', on_constraint_usage_rules),
                                ('on_flow_constraint_in_country', on_flow_constraint_in_country_usage_rules)):
        if df is not None:
            usage_rules.append(_flatten_index(df).assign(usage_rule_type=usage_rule_type))
    if not usage_rules:
        return None
    return _sub_c_df(pd.concat(usage_rules, ignore_index=True), _USAGE_RULES_COLUMNS)
