# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Union
import pandas as pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import EventMappingType, Side  # pylint: disable=protected-access
from pypowsybl.utils import \
    _adapt_df_or_kwargs, _add_index_to_kwargs, _create_c_dataframe  # pylint: disable=protected-access


class EventMapping:
    """
    Class to map events
    """

    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_disconnection(self, static_id: str, start_time: float, disconnect_only: Side = Side.NONE) -> None:
        """ Creates an equipment disconnection event

        Args:
            static_id: id of the network element to disconnect
            start_time: timestep at which the event happens
            disconnect_only: the disconnection is made on the provided side only (NONE by default)
        """
        self.add_all_event_mappings(static_id=static_id,
                                    start_time=start_time,
                                    disconnect_only=disconnect_only,
                                    mapping_type=EventMappingType.DISCONNECT)

    def add_active_power_variation(self, static_id: str, start_time: float, delta_p: float) -> None:
        """ Creates an equipment active power variation event

        Args:
            static_id: id of the load or generator affected by the event
            start_time: timestep at which the event happens
            delta_p: active power variation
        """
        self.add_all_event_mappings(static_id=static_id,
                                    start_time=start_time,
                                    delta_p=delta_p,
                                    mapping_type=EventMappingType.ACTIVE_POWER_VARIATION)

    def add_node_fault(self, static_id: str, start_time: float, fault_time: float, r_pu: float, x_pu: float) -> None:
        """ Creates a bus node fault event

        Args:
            static_id: id of the bus affected by the event
            start_time: timestep at which the event happens
            fault_time: delta with start_time at which the event ends
            r_pu: r pu variation
            x_pu: x pu variation
        """
        self.add_all_event_mappings(static_id=static_id,
                                    start_time=start_time,
                                    fault_time=fault_time,
                                    r_pu=r_pu,
                                    x_pu=x_pu,
                                    mapping_type=EventMappingType.NODE_FAULT)

    def add_all_event_mappings(self, mapping_type: EventMappingType, mapping_df: pd.DataFrame = None,
                               **kwargs: Union[str, float, Side, EventMappingType]) -> None:
        """
        Update the event mapping of a simulation, must provide a :class:`~pandas.DataFrame` or as named arguments.

        | The dataframe must contain these three columns:
        |     - static_id: id of the network element affected by the event
        |     - start_time: timestep at which the event happens
        |     - mapping_type: value of enum EventMappingType

        """
        # TODO index df on static id + event type
        metadata = _pp.get_event_mappings_meta_data(mapping_type)
        if kwargs:
            kwargs = _add_index_to_kwargs(metadata, **kwargs)
        mapping_df = _adapt_df_or_kwargs(metadata, mapping_df, **kwargs)
        c_mapping_df = _create_c_dataframe(mapping_df, metadata)
        _pp.add_all_event_mappings(self._handle, mapping_type, c_mapping_df)