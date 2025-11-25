# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
import json

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import (
    RaoParameters
)
from .objective_function_parameters import ObjectiveFunctionParameters
from .range_action_optimization_parameters import RangeActionOptimizationParameters
from .topo_optimization_parameters import TopoOptimizationParameters
from .multithreading_parameters import MultithreadingParameters
from .second_preventive_rao_parameters import SecondPreventiveRaoParameters
from .not_optimized_cnecs_parameters import NotOptimizedCnecsParameters
from .loadflow_and_sensitivity_parameters import LoadFlowAndSensitivityParameters
from pypowsybl.utils import path_to_str
from typing import Union, Dict, Any, Optional
from os import PathLike

class Parameters:
    def __init__(self, objective_function_parameters: Optional[ObjectiveFunctionParameters] = None,
                 range_action_optimization_parameters: Optional[RangeActionOptimizationParameters] = None,
                 topo_optimization_parameters: Optional[TopoOptimizationParameters] = None,
                 multithreading_parameters: Optional[MultithreadingParameters] = None,
                 second_preventive_rao_parameters: Optional[SecondPreventiveRaoParameters] = None,
                 not_optimized_cnecs_parameters: Optional[NotOptimizedCnecsParameters] = None,
                 loadflow_and_sensitivity_parameters: Optional[LoadFlowAndSensitivityParameters] = None,
                 provider_parameters: Optional[Dict[str, str]] = None) -> None:
        self._init_with_default_values()
        if objective_function_parameters is not None:
            self.objective_function_parameters = objective_function_parameters
        if range_action_optimization_parameters is not None:
            self.range_action_optimization_parameters = range_action_optimization_parameters
        if topo_optimization_parameters is not None:
            self.topo_optimization_parameters = topo_optimization_parameters
        if multithreading_parameters is not None:
            self.multithreading_parameters = multithreading_parameters
        if second_preventive_rao_parameters is not None:
            self.second_preventive_rao_parameters = second_preventive_rao_parameters
        if not_optimized_cnecs_parameters is not None:
            self.not_optimized_cnecs_parameters = not_optimized_cnecs_parameters
        if loadflow_and_sensitivity_parameters is not None:
            self.loadflow_and_sensitivity_parameters = loadflow_and_sensitivity_parameters
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.objective_function_parameters = ObjectiveFunctionParameters(rao_parameters=c_parameters)
        self.range_action_optimization_parameters = RangeActionOptimizationParameters(rao_parameters=c_parameters)
        self.topo_optimization_parameters = TopoOptimizationParameters(rao_parameters=c_parameters)
        self.multithreading_parameters = MultithreadingParameters(rao_parameters=c_parameters)
        self.second_preventive_rao_parameters = SecondPreventiveRaoParameters(rao_parameters=c_parameters)
        self.not_optimized_cnecs_parameters = NotOptimizedCnecsParameters(rao_parameters=c_parameters)
        self.loadflow_and_sensitivity_parameters = LoadFlowAndSensitivityParameters(rao_parameters=c_parameters)
        self.provider_parameters = dict(
            zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> RaoParameters:
        c_parameters = RaoParameters()
        c_parameters.objective_function_type = self.objective_function_parameters.objective_function_type
        c_parameters.unit = self.objective_function_parameters.unit
        c_parameters.curative_min_obj_improvement = self.objective_function_parameters.curative_min_obj_improvement
        c_parameters.enforce_curative_security = self.objective_function_parameters.enforce_curative_security

        c_parameters.max_mip_iterations = self.range_action_optimization_parameters.max_mip_iterations
        c_parameters.pst_ra_min_impact_threshold = self.range_action_optimization_parameters.pst_ra_min_impact_threshold
        c_parameters.pst_sensitivity_threshold = self.range_action_optimization_parameters.pst_sensitivity_threshold
        c_parameters.pst_model = self.range_action_optimization_parameters.pst_model
        c_parameters.hvdc_ra_min_impact_threshold = self.range_action_optimization_parameters.hvdc_ra_min_impact_threshold
        c_parameters.hvdc_sensitivity_threshold = self.range_action_optimization_parameters.hvdc_sensitivity_threshold
        c_parameters.injection_ra_min_impact_threshold = self.range_action_optimization_parameters.injection_ra_min_impact_threshold
        c_parameters.injection_ra_sensitivity_threshold = self.range_action_optimization_parameters.injection_ra_sensitivity_threshold
        c_parameters.ra_range_shrinking = self.range_action_optimization_parameters.ra_range_shrinking
        c_parameters.solver = self.range_action_optimization_parameters.solver
        c_parameters.relative_mip_gap = self.range_action_optimization_parameters.relative_mip_gap
        c_parameters.solver_specific_parameters = self.range_action_optimization_parameters.solver_specific_parameters

        c_parameters.max_preventive_search_tree_depth = self.topo_optimization_parameters.max_preventive_search_tree_depth
        c_parameters.max_curative_search_tree_depth = self.topo_optimization_parameters.max_curative_search_tree_depth
        c_parameters.predefined_combinations = self.topo_optimization_parameters.predefined_combinations
        c_parameters.relative_min_impact_threshold = self.topo_optimization_parameters.relative_min_impact_threshold
        c_parameters.absolute_min_impact_threshold = self.topo_optimization_parameters.absolute_min_impact_threshold
        c_parameters.skip_actions_far_from_most_limiting_element = self.topo_optimization_parameters.skip_actions_far_from_most_limiting_element
        c_parameters.max_number_of_boundaries_for_skipping_actions = self.topo_optimization_parameters.max_number_of_boundaries_for_skipping_actions

        c_parameters.available_cpus = self.multithreading_parameters.available_cpus

        c_parameters.execution_condition = self.second_preventive_rao_parameters.execution_condition
        c_parameters.hint_from_first_preventive_rao = self.second_preventive_rao_parameters.hint_from_first_preventive_rao

        c_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras = self.not_optimized_cnecs_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras

        c_parameters.load_flow_provider = self.loadflow_and_sensitivity_parameters.load_flow_provider
        c_parameters.sensitivity_provider = self.loadflow_and_sensitivity_parameters.sensitivity_provider
        c_parameters.sensitivity_parameters = self.loadflow_and_sensitivity_parameters.sensitivity_parameters._to_c_parameters()
        c_parameters.sensitivity_failure_overcost = self.loadflow_and_sensitivity_parameters.sensitivity_failure_overcost

        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    @classmethod
    def from_file_source(cls, parameters_file: Union[str, PathLike]) -> Any :
        parameters = io.BytesIO(open(path_to_str(parameters_file), "rb").read())
        return cls.from_buffer_source(parameters)

    @classmethod
    def from_buffer_source(cls, parameters_source: io.BytesIO) -> Any :
        p = cls()
        p._init_from_c(_pypowsybl.load_rao_parameters(parameters_source.getbuffer()))
        return p

    def serialize(self, output_file: str) -> None:
        with open(output_file, "wb") as f:
            f.write(self.serialize_to_binary_buffer().getbuffer())

    def serialize_to_binary_buffer(self) -> io.BytesIO:
        return io.BytesIO(_pypowsybl.serialize_rao_parameters(self._to_c_parameters()))

    def to_json(self) -> Dict[str, Any]:
        return json.load(self.serialize_to_binary_buffer())

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"objective_function_parameters={self.objective_function_parameters!r}" \
               f", range_action_optimization_parameters={self.range_action_optimization_parameters!r}" \
               f", topo_optimization_parameters={self.topo_optimization_parameters!r}" \
               f", multithreading_parameters={self.multithreading_parameters!r}" \
               f", second_preventive_rao_parameters={self.second_preventive_rao_parameters!r}" \
               f", not_optimized_cnecs_parameters={self.not_optimized_cnecs_parameters!r}" \
               f", loadflow_and_sensitivity_parameters={self.loadflow_and_sensitivity_parameters!r}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"