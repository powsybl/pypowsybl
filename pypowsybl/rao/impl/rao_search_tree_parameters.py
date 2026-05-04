# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pypowsybl._pypowsybl import (
    RaoParameters
)

from .second_preventive_rao_parameters import SecondPreventiveRaoParameters
from .loadflow_and_sensitivity_parameters import LoadFlowAndSensitivityParameters
from .range_action_search_tree_parameters import RangeActionSearchTreeParameters
from .topo_search_tree_parameters import TopoSearchTreeParameters

class RaoSearchTreeParameters:
    def __init__(self, curative_min_obj_improvement: Optional[float] = None,
                 range_action_parameters: Optional[RangeActionSearchTreeParameters] = None,
                 topo_parameters: Optional[TopoSearchTreeParameters] = None,
                 available_cpus: Optional[int] = None,
                 second_preventive_rao_parameters: Optional[SecondPreventiveRaoParameters] = None,
                 loadflow_and_sensitivity_parameters: Optional[LoadFlowAndSensitivityParameters] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()

        #Objective function search tree parameters
        if curative_min_obj_improvement is not None:
            self.curative_min_obj_improvement = curative_min_obj_improvement

        #Range action optimization search tree parameters
        if range_action_parameters is not None:
            self.range_action_parameters = range_action_parameters

        #Topo search tree parameters
        if topo_parameters is not None:
            self.topo_parameters = topo_parameters

        #Multithreading parameters
        if available_cpus is not None:
            self.available_cpus = available_cpus

        if second_preventive_rao_parameters is not None:
            self.second_preventive_rao_parameters = second_preventive_rao_parameters

        if loadflow_and_sensitivity_parameters is not None:
            self.loadflow_and_sensitivity_parameters = loadflow_and_sensitivity_parameters

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.curative_min_obj_improvement = c_parameters.curative_min_obj_improvement
        self.range_action_parameters = RangeActionSearchTreeParameters(rao_parameters=c_parameters)
        self.topo_parameters = TopoSearchTreeParameters(rao_parameters=c_parameters)
        self.available_cpus = c_parameters.available_cpus
        self.second_preventive_rao_parameters = SecondPreventiveRaoParameters(rao_parameters=c_parameters)
        self.loadflow_and_sensitivity_parameters = LoadFlowAndSensitivityParameters(rao_parameters=c_parameters)

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", curative_min_obj_improvement={self.curative_min_obj_improvement!r}" \
               f", range_action_parameters={self.range_action_parameters!r}" \
               f", topo_parameters={self.topo_parameters!r}" \
               f", available_cpus={self.available_cpus!r}" \
               f", second_preventive_rao_parameters={self.second_preventive_rao_parameters!r}" \
               f", loadflow_and_sensitivity_parameters={self.loadflow_and_sensitivity_parameters!r}" \
               f")"
