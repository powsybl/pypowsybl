# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Callable
from pypowsybl import _pypowsybl

class ContingencyContainer:
    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    def add_single_element_contingency(self, element_id: str, contingency_id: str = None) -> None:
        """
        Add one N-1 contingency.

        Args:
            element_id: The ID of the lost network element.
            contingency_id: The ID of the contingency.
                If ``None``, element_id will be used.
        """
        _pypowsybl.add_contingency(self._handle, contingency_id if contingency_id else element_id, [element_id])

    def add_multiple_elements_contingency(self, elements_ids: List[str], contingency_id: str) -> None:
        """
        Add one N-K contingency.

        Args:
            elements_ids: The ID of the lost network elements.
            contingency_id: The ID of the contingency.
        """
        _pypowsybl.add_contingency(self._handle, contingency_id, elements_ids)

    def add_single_element_contingencies(self, elements_ids: List[str],
                                         contingency_id_provider: Callable[[str], str] = None) -> None:
        """
        Add multiple N-1 contingencies.

        Args:
            elements_ids: A list of network elements.
                One N- 1 contingency will be added for each element of the list.
            contingency_id_provider: A callable which maps elements IDs to a contingency ID.
                If ``None``, the element ID will be used as the contingency ID for each N-1 contingency.
        """
        for element_id in elements_ids:
            contingency_id = contingency_id_provider(element_id) if contingency_id_provider else element_id
            _pypowsybl.add_contingency(self._handle, contingency_id, [element_id])

    def add_contingencies_from_json_file(self, path_to_json_file: str) -> None:
        """
        Add contingencies from JSON file.

        Args:
            path_to_json_file: The path to the JSON file from which we extract the contingency list.
        """

        _pypowsybl.add_contingency_from_json_file(self._handle, path_to_json_file)