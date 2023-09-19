# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl._pypowsybl import ValidationType, LoadFlowValidationParameters
from .parameters import Parameters
from .util import parameters_from_c

ValidationType.ALL = [ValidationType.BUSES, ValidationType.FLOWS, ValidationType.GENERATORS, ValidationType.SHUNTS,
                      ValidationType.SVCS, ValidationType.TWTS, ValidationType.TWTS3W]


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
        self._init_from_c(LoadFlowValidationParameters())

    def _init_from_c(self, c_parameters: LoadFlowValidationParameters) -> None:
        self.threshold = c_parameters.threshold
        self.verbose = c_parameters.verbose
        self.loadflow_name = c_parameters.loadflow_name
        self.epsilon_x = c_parameters.epsilon_x
        self.apply_reactance_correction = c_parameters.apply_reactance_correction
        self.loadflow_parameters = parameters_from_c(c_parameters.loadflow_parameters)
        self.ok_missing_values = c_parameters.ok_missing_values
        self.no_requirement_if_reactive_bound_inversion = c_parameters.no_requirement_if_reactive_bound_inversion
        self.compare_results = c_parameters.compare_results
        self.check_main_component_only = c_parameters.check_main_component_only
        self.no_requirement_if_setpoint_outside_power_bounds = c_parameters.no_requirement_if_setpoint_outside_power_bounds

    def to_c_parameters(self) -> LoadFlowValidationParameters:
        c_parameters = LoadFlowValidationParameters()
        c_parameters.threshold = self.threshold
        c_parameters.verbose = self.verbose
        c_parameters.loadflow_name = self.loadflow_name
        c_parameters.epsilon_x = self.epsilon_x
        c_parameters.apply_reactance_correction = self.apply_reactance_correction
        c_parameters.loadflow_parameters = self.loadflow_parameters._to_c_parameters()  # pylint: disable=protected-access
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
