# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Optional

from pypowsybl._pypowsybl import (
    RaoParameters
)

class TopoSearchTreeParameters:
    def __init__(self, max_preventive_search_tree_depth: Optional[int] = None,
                 max_curative_search_tree_depth: Optional[int] = None,
                 predefined_combinations: Optional[List[List[str]]] = None,
                 skip_actions_far_from_most_limiting_element: Optional[bool] = None,
                 max_number_of_boundaries_for_skipping_actions: Optional[int] = None,
                 rao_parameters: Optional[RaoParameters] = None) -> None:
        if rao_parameters is not None:
            self._init_from_c(rao_parameters)
        else:
            self._init_with_default_values()

        if max_preventive_search_tree_depth is not None:
            self.max_preventive_search_tree_depth = max_preventive_search_tree_depth
        if max_curative_search_tree_depth is not None:
            self.max_curative_search_tree_depth = max_curative_search_tree_depth
        if predefined_combinations is not None:
            self.predefined_combinations = predefined_combinations
        if skip_actions_far_from_most_limiting_element is not None:
            self.skip_actions_far_from_most_limiting_element = skip_actions_far_from_most_limiting_element
        if max_number_of_boundaries_for_skipping_actions is not None:
            self.max_number_of_boundaries_for_skipping_actions = max_number_of_boundaries_for_skipping_actions

    def _init_with_default_values(self) -> None:
        self._init_from_c(RaoParameters())

    def _init_from_c(self, c_parameters: RaoParameters) -> None:
        self.max_preventive_search_tree_depth = c_parameters.max_preventive_search_tree_depth
        self.max_curative_search_tree_depth = c_parameters.max_curative_search_tree_depth
        self.predefined_combinations = c_parameters.predefined_combinations
        self.skip_actions_far_from_most_limiting_element = c_parameters.skip_actions_far_from_most_limiting_element
        self.max_number_of_boundaries_for_skipping_actions = c_parameters.max_number_of_boundaries_for_skipping_actions

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", max_preventive_search_tree_depth={self.max_preventive_search_tree_depth!r}" \
               f", max_curative_search_tree_depth={self.max_curative_search_tree_depth!r}" \
               f", predefined_combinations={self.predefined_combinations!r}" \
               f", skip_actions_far_from_most_limiting_element={self.skip_actions_far_from_most_limiting_element!r}" \
               f", max_number_of_boundaries_for_skipping_actions={self.max_number_of_boundaries_for_skipping_actions!r}" \
               f")"
