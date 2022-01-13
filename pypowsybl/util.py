#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
from typing import List as _List
from typing import Callable as _Callable
import pandas as _pd


class ContingencyContainer(object):
    def __init__(self, handle):
        self._handle = handle

    def add_single_element_contingency(self, element_id: str, contingency_id: str = None):
        """
        Add one N-1 contingency.

        Args:
            element_id: The ID of the lost network element.
            contingency_id: The ID of the contingency.
                If ``None``, element_id will be used.
        """
        _pypowsybl.add_contingency(self._handle, contingency_id if contingency_id else element_id, [element_id])

    def add_multiple_elements_contingency(self, elements_ids: _List[str], contingency_id: str):
        """
        Add one N-K contingency.

        Args:
            elements_ids: The ID of the lost network elements.
            contingency_id: The ID of the contingency.
        """
        _pypowsybl.add_contingency(self._handle, contingency_id, elements_ids)

    def add_single_element_contingencies(self, elements_ids: _List[str], contingency_id_provider: _Callable[[str], str] = None):
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


def create_data_frame_from_series_array(series_array):
    series_dict = {}
    index_data = []
    index_names = []
    for series in series_array:
        if series.index:
            index_data.append(series.data)
            index_names.append(series.name)
        else:
            series_dict[series.name] = series.data
    index = None
    if not index_names:
        raise ValueError('No index in returned dataframe')
    if len(index_names) == 1:
        index = _pd.Index(index_data[0], name=index_names[0])
    else:
        index = _pd.MultiIndex.from_arrays(index_data, names=index_names)
    return _pd.DataFrame(series_dict, index=index)
