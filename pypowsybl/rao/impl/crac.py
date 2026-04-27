# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
import json
import shutil
from pathlib import Path
from tempfile import TemporaryDirectory

from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.utils import path_to_str
from pypowsybl.utils import create_data_frame_from_series_array
from pandas import DataFrame

from os import PathLike

from typing import (
    Union,
    Any, Optional
)

class Crac:
    """
    RAO Crac
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    @classmethod
    def load(cls, network: Network, crac_file: Union[str, PathLike], creation_parameters_file: Optional[Union[str, PathLike]] = None) -> "Crac":
        with TemporaryDirectory() as tmp_dir:
            tmp_dir = Path(tmp_dir)
            if creation_parameters_file is None:
                creation_parameters_file = tmp_dir / "crac-creation-parameters.json"
                with open(str(creation_parameters_file), "w") as outfile:
                    json.dump({"crac-factory": "CracImplFactory"}, outfile)
            else:
                shutil.copyfile(creation_parameters_file, tmp_dir / "crac-creation-parameters.json")
            crac_filename = Path(crac_file).name
            shutil.copyfile(crac_file, tmp_dir / crac_filename)
            return cls(_pypowsybl.load_crac_with_parameters(network._handle, str(tmp_dir / crac_filename), str(tmp_dir / "crac-creation-parameters.json")))

    @classmethod
    def from_file_source(cls, network: Network, crac_file: Union[str, PathLike]) -> Any :
        return Crac.from_buffer_source(network, io.BytesIO(open(path_to_str(crac_file), "rb").read()))

    @classmethod
    def from_buffer_source(cls, network: Network, crac_source: io.BytesIO) -> Any :
        return cls(_pypowsybl.load_crac_source(network._handle, crac_source.getbuffer()))

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
            - **exporting_country**: the name of the country the power is exported from
            - **importing_country**: the name of the country the power is imported in
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