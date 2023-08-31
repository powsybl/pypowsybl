# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import (
    Optional,
    List
)
import pandas as pd
import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import ValidationType

from loadflow.impl.parameters import Parameters
from loadflow.impl.loadflow import _parameters_from_c
from pypowsybl.network import Network
from pypowsybl.util import create_data_frame_from_series_array

ValidationType.ALL = [ValidationType.BUSES, ValidationType.FLOWS, ValidationType.GENERATORS, ValidationType.SHUNTS,
                      ValidationType.SVCS, ValidationType.TWTS, ValidationType.TWTS3W]

OptionalDf = Optional[pd.DataFrame]


class ValidationParameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a loadflow validation.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    .. currentmodule:: pypowsybl.loadflow

    Args:
        threshold: Define the margin used for values comparison.
            The default value is ``0``.
        verbose: Define whether the load flow validation should run in verbose or quiet mode.
        loadflow_name: Implementation name to use for running the load flow.
        epsilon_x: Value used to correct the reactance in flows validation.
            The default value is ``0.1``.
        apply_reactance_correction: Define whether small reactance values have to be fixed to epsilon_x or not.
            The default value is ``False``.
        loadflow_parameters: Parameters that are common to loadflow and loadflow validation.
        ok_missing_values: Define whether the validation checks fail if some parameters of connected components have NaN values or not.
            The default value is ``False``.
        no_requirement_if_reactive_bound_inversion: Define whether the validation checks fail if there is a reactive
            bounds inversion (maxQ < minQ) or not.
            The default value is ``False``.
        compare_results: Should be set to ``True`` to compare the results of 2 validations, i.e. print output files with
            data of both ones.
            The default value is ``False``.
        check_main_component_only: Define whether the validation checks are done only on the equiments in the main
            connected component or in all components.
            The default value is ``True``.
        no_requirement_if_setpoint_outside_power_bounds: Define whether the validation checks fail if there is a
            setpoint outside the active power bounds (targetP < minP or targetP > maxP) or not.
            The default value is ``False``.
    """

    def __init__(self, threshold: float = None,
                 verbose: bool = None,
                 loadflow_name: str = None,
                 epsilon_x: float = None,
                 apply_reactance_correction: bool = None,
                 loadflow_parameters: Parameters = None,
                 ok_missing_values: bool = None,
                 no_requirement_if_reactive_bound_inversion: bool = None,
                 compare_results: bool = None,
                 check_main_component_only: bool = None,
                 no_requirement_if_setpoint_outside_power_bounds: bool = None):
        self._init_with_default_values()
        if threshold is not None:
            self.threshold = threshold
        if verbose is not None:
            self.verbose = verbose
        if loadflow_name is not None:
            self.loadflow_name = loadflow_name
        if epsilon_x is not None:
            self.epsilon_x = epsilon_x
        if apply_reactance_correction is not None:
            self.apply_reactance_correction = apply_reactance_correction
        if loadflow_parameters is not None:
            self.loadflow_parameters = loadflow_parameters
        if ok_missing_values is not None:
            self.ok_missing_values = ok_missing_values
        if no_requirement_if_reactive_bound_inversion is not None:
            self.no_requirement_if_reactive_bound_inversion = no_requirement_if_reactive_bound_inversion
        if compare_results is not None:
            self.compare_results = compare_results
        if check_main_component_only is not None:
            self.check_main_component_only = check_main_component_only
        if no_requirement_if_setpoint_outside_power_bounds is not None:
            self.no_requirement_if_setpoint_outside_power_bounds = no_requirement_if_setpoint_outside_power_bounds

    def _init_with_default_values(self) -> None:
        self._init_from_c(_pp.LoadFlowValidationParameters())

    def _init_from_c(self, c_parameters: _pp.LoadFlowValidationParameters) -> None:
        self.threshold = c_parameters.threshold
        self.verbose = c_parameters.verbose
        self.loadflow_name = c_parameters.loadflow_name
        self.epsilon_x = c_parameters.epsilon_x
        self.apply_reactance_correction = c_parameters.apply_reactance_correction
        self.loadflow_parameters = _parameters_from_c(c_parameters.loadflow_parameters)
        self.ok_missing_values = c_parameters.ok_missing_values
        self.no_requirement_if_reactive_bound_inversion = c_parameters.no_requirement_if_reactive_bound_inversion
        self.compare_results = c_parameters.compare_results
        self.check_main_component_only = c_parameters.check_main_component_only
        self.no_requirement_if_setpoint_outside_power_bounds = c_parameters.no_requirement_if_setpoint_outside_power_bounds

    def _to_c_parameters(self) -> _pp.LoadFlowValidationParameters:
        c_parameters = _pp.LoadFlowValidationParameters()
        c_parameters.threshold = self.threshold
        c_parameters.verbose = self.verbose
        c_parameters.loadflow_name = self.loadflow_name
        c_parameters.epsilon_x = self.epsilon_x
        c_parameters.apply_reactance_correction = self.apply_reactance_correction
        c_parameters.loadflow_parameters = self.loadflow_parameters._to_c_parameters()
        c_parameters.ok_missing_values = self.ok_missing_values
        c_parameters.no_requirement_if_reactive_bound_inversion = self.no_requirement_if_reactive_bound_inversion
        c_parameters.compare_results = self.compare_results
        c_parameters.check_main_component_only = self.check_main_component_only
        c_parameters.no_requirement_if_setpoint_outside_power_bounds = self.no_requirement_if_setpoint_outside_power_bounds
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"threshold={self.threshold}" \
               f", verbose={self.verbose!r}" \
               f", loadflow_name={self.loadflow_name!r}" \
               f", epsilon_x={self.epsilon_x!r}" \
               f", apply_reactance_correction={self.apply_reactance_correction!r}" \
               f", loadflow_parameters={self.loadflow_parameters!r}" \
               f", ok_missing_values={self.ok_missing_values!r}" \
               f", no_requirement_if_reactive_bound_inversion={self.no_requirement_if_reactive_bound_inversion}" \
               f", compare_results={self.compare_results!r}" \
               f", no_requirement_if_setpoint_outside_power_bounds={self.no_requirement_if_setpoint_outside_power_bounds}" \
               f")"


class ValidationResult:
    """
    The result of a loadflow validation.
    """

    def __init__(self, branch_flows: OptionalDf, buses: OptionalDf, generators: OptionalDf, svcs: OptionalDf,
                 shunts: OptionalDf, twts: OptionalDf, t3wts: OptionalDf):
        self._branch_flows = branch_flows
        self._buses = buses
        self._generators = generators
        self._svcs = svcs
        self._shunts = shunts
        self._twts = twts
        self._t3wts = t3wts
        self._valid = self._is_valid_or_unchecked(self.branch_flows) and self._is_valid_or_unchecked(self.buses) \
                      and self._is_valid_or_unchecked(self.generators) and self._is_valid_or_unchecked(self.svcs) \
                      and self._is_valid_or_unchecked(self.shunts) and self._is_valid_or_unchecked(self.twts) \
                      and self._is_valid_or_unchecked(self.t3wts)

    @staticmethod
    def _is_valid_or_unchecked(df: OptionalDf) -> bool:
        return df is None or df['validated'].all()

    @property
    def branch_flows(self) -> OptionalDf:
        """
        Validation results for branch flows.
        """
        return self._branch_flows

    @property
    def buses(self) -> OptionalDf:
        """
        Validation results for buses.
        """
        return self._buses

    @property
    def generators(self) -> OptionalDf:
        """
        Validation results for generators.
        """
        return self._generators

    @property
    def svcs(self) -> OptionalDf:
        """
        Validation results for SVCs.
        """
        return self._svcs

    @property
    def shunts(self) -> OptionalDf:
        """
        Validation results for shunts.
        """
        return self._shunts

    @property
    def twts(self) -> OptionalDf:
        """
        Validation results for two winding transformers.
        """
        return self._twts

    @property
    def t3wts(self) -> OptionalDf:
        """
        Validation results for three winding transformers.
        """
        return self._t3wts

    @property
    def valid(self) -> bool:
        """
        True if all checked data is valid.
        """
        return self._valid


def run_validation(network: Network, validation_types: List[ValidationType] = None,
                   validation_parameters: ValidationParameters = None) -> ValidationResult:
    """
    Checks that the network data are consistent with AC loadflow equations.

    Args:
        network: The network to be checked.
        validation_types: The types of data to be checked. If None, all types will be checked.
        validation_parameters: The parameters to run the validation with.

    Returns:
        The validation result.
    """
    if validation_types is None:
        validation_types = ValidationType.ALL
    validation_config = validation_parameters._to_c_parameters() if validation_parameters is not None else _pp.LoadFlowValidationParameters()
    res_by_type = {}
    for validation_type in validation_types:
        series_array = _pp.run_loadflow_validation(network._handle, validation_type, validation_config)
        res_by_type[validation_type] = create_data_frame_from_series_array(series_array)

    return ValidationResult(buses=res_by_type.get(ValidationType.BUSES, None),
                            branch_flows=res_by_type.get(ValidationType.FLOWS, None),
                            generators=res_by_type.get(ValidationType.GENERATORS, None),
                            svcs=res_by_type.get(ValidationType.SVCS, None),
                            shunts=res_by_type.get(ValidationType.SHUNTS, None),
                            twts=res_by_type.get(ValidationType.TWTS, None),
                            t3wts=res_by_type.get(ValidationType.TWTS3W, None))
