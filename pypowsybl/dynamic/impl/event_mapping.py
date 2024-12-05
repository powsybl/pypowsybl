# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional
from numpy.typing import ArrayLike
from pandas import DataFrame
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import EventMappingType  # pylint: disable=protected-access
from pypowsybl.utils import _add_index_to_kwargs, \
    _adapt_df_or_kwargs, _create_c_dataframe  # pylint: disable=protected-access


class EventMapping:
    """
    Class to map events
    """

    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_disconnection(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """ Creates an equipment disconnection event

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the network element to disconnect
            - **start_time**: timestep at which the event happens
            - **disconnect_only**: the disconnection is made on the provided side only for branch equipment (ONE or TWO)

        Examples:
            Using keyword arguments:

            .. code-block:: python

                event_mapping.add_disconnection(static_id='LINE', start_time=3.3, disconnect_only='TWO')
        """
        self._add_all_event_mappings(EventMappingType.DISCONNECT, df, **kwargs)

    def add_active_power_variation(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """ Creates an equipment active power variation event

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the load or generator affected by the event
            - **start_time**: timestep at which the event happens
            - **delta_p**: active power variation

        Examples:
            Using keyword arguments:

            .. code-block:: python

                event_mapping.add_active_power_variation(static_id='LOAD', start_time=14, delta_p=2)
        """
        self._add_all_event_mappings(EventMappingType.ACTIVE_POWER_VARIATION, df, **kwargs)

    def add_node_fault(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """ Creates a bus node fault event

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **static_id**: id of the bus affected by the event
            - **start_time**: timestep at which the event happens
            - **fault_time:** delta with start_time at which the event ends
            - **r_pu**: r pu variation
            - **x_pu**: x pu variation

        Examples:
            Using keyword arguments:

            .. code-block:: python

                event_mapping.add_node_fault(static_id='BUS', start_time=12, fault_time=2, r_pu=0.1, x_pu=0.2)
        """
        self._add_all_event_mappings(EventMappingType.NODE_FAULT, df, **kwargs)

    def _add_all_event_mappings(self, mapping_type: EventMappingType, mapping_df: Optional[DataFrame], **kwargs: ArrayLike) -> None:
        metadata = _pp.get_event_mappings_meta_data(mapping_type)
        if kwargs:
            kwargs = _add_index_to_kwargs(metadata, **kwargs)
        mapping_df = _adapt_df_or_kwargs(metadata, mapping_df, **kwargs)
        c_mapping_df = _create_c_dataframe(mapping_df, metadata)
        _pp.add_all_event_mappings(self._handle, mapping_type, c_mapping_df)
