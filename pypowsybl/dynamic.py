#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

from enum import Enum as _Enum
from typing import List as _List, Union as _Union
import pandas as _pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import DynamicMappingType, BranchSide
from pypowsybl.network import Network
from pypowsybl.util import create_data_frame_from_series_array
from pypowsybl.utils.dataframes import _adapt_df_or_kwargs, _add_index_to_kwargs, _create_c_dataframe


class ModelMapping:
    """
        class to map elements of a network to their respective dynamic behavior
    """

    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_model_mapping()

    def add_alpha_beta_load(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a alpha beta load mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.ALPHA_BETA_LOAD)

    def add_one_transformer_load(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a one transformer load mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.ONE_TRANSFORMER_LOAD)

    def add_generator_synchronous_three_windings(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a generator synchronous three windings mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS)

    def add_generator_synchronous_three_windings_proportional_regulations(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a generator synchronous three windings proportional regulations mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS)

    def add_generator_synchronous_four_windings(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a generator synchronous four windings mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS)

    def add_generator_synchronous_four_windings_proportional_regulations(self, static_id: str, parameter_set_id: str) -> None:
        """
        Add a generator synchronous four windings proportional regulations mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS_PROPORTIONAL_REGULATIONS)

    def add_current_limit_automaton(self, static_id: str, parameter_set_id: str, branch_side: BranchSide) -> None:
        """
        Add a current limit automaton mapping

        :param static_id: id of the network element to map
        :param parameter_set_id: id of the parameter for this model given in the dynawaltz configuration
        """
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      branch_side=branch_side,
                                      mapping_type=DynamicMappingType.CURRENT_LIMIT_AUTOMATON)

    def add_all_dynamic_mappings(self, mapping_type: DynamicMappingType, mapping_df: _pd.DataFrame = None, **kwargs: _Union[str, BranchSide, DynamicMappingType]) -> None:
        """
        Update the dynamic mapping of a simulation, must provide a :class:`~pandas.DataFrame` or as named arguments.

        | The dataframe must contains these three columns :
        |     - static_id: id of the network element to map
        |     - parameter_set_id: set id in the parameter file
        |     - mapping_type: value of enum DynamicMappingType

        """
        metadata = _pp.get_dynamic_mappings_meta_data(mapping_type)
        if kwargs:
            kwargs = _add_index_to_kwargs(metadata, **kwargs)
        mapping_df = _adapt_df_or_kwargs(metadata, mapping_df, **kwargs)
        c_mapping_df = _create_c_dataframe(mapping_df, metadata)
        _pp.add_all_dynamic_mappings(self._handle, mapping_type, c_mapping_df)


class CurveMapping:
    """
    Class to map Curves
    """

    def __init__(self) -> None:
        self._handle = _pp.create_timeseries_mapping()

    def add_curve(self, dynamic_id: str, variable: str) -> None:
        """
        adds one curve mapping

        :param dynamic_id: id of the network's element
        :param variable: variable name to record
        """
        _pp.add_curve(self._handle, dynamic_id, variable)

    def add_curves(self, dynamic_id: str, variables: _List[str]) -> None:
        """
        adds curves mapping in batch on a single network element

        :param dynamic_id: id of the network's element
        :param variable: list of variables names to record
        """
        for var in variables:
            self.add_curve(dynamic_id, var)


class EventType(_Enum):
    SET_POINT_BOOLEAN = 'SET_POINT_BOOLEAN'
    BRANCH_DISCONNECTION = 'BRANCH_DISCONNECTION'


class EventMapping:
    """
    Class to map events
    """

    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_branch_disconnection(self, static_id: str, event_time: float, disconnect_origin: bool, disconnect_extremity: bool) -> None:
        """ Creates a branch disconnection event

        Args:
            static_id (str): network element to disconnect
            event_time (float): timestep at which the event happens
            disconnect_origin (bool) : the disconnection is made at the origin
            disconnect_extremity (bool) : the disconnection is made at the extremity
        """
        _pp.add_event_branch_disconnection(
            self._handle, static_id, event_time, disconnect_origin, disconnect_extremity)

    def add_injection_disconnection(self, static_id: str, event_time: float, state_event: bool) -> None:
        """ Creates an injection disconnection event

        Args:
            static_id (str): network element to disconnect
            event_time (float): timestep at which the event happens
            state_event (bool): TODO
        """
        _pp.add_event_injection_disconnection(
            self._handle, static_id, event_time, state_event)

    @staticmethod
    def get_possible_events() -> _List[EventType]:
        return list(EventType)


class SimulationResult:
    """Can only be instantiated by :func:`~Simulation.run`"""

    def __init__(self, handle: _pp.JavaHandle) -> None:
        self._handle = handle
        self._status = _pp.get_dynamic_simulation_results_status(self._handle)
        self._curves = self._get_all_curves()

    def status(self) -> str:
        """
        status of the simulation

        :returns 'Ok' or 'Not OK'
        """
        return self._status

    def curves(self) -> _pd.DataFrame:
        """Dataframe of the curves results, columns are the curves names and rows are timestep"""
        return self._curves

    def _get_curve(self, curve_name: str) -> _pd.DataFrame:
        series_array = _pp.get_dynamic_curve(self._handle, curve_name)
        return create_data_frame_from_series_array(series_array)

    def _get_all_curves(self) -> _pd.DataFrame:
        curve_name_lst = _pp.get_all_dynamic_curves_ids(self._handle)
        df_curves = [self._get_curve(curve_name)
                     for curve_name in curve_name_lst]
        return _pd.concat(df_curves, axis=1)


class Simulation:
    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_simulation_context()

    def run(self,
            network: Network,
            model_mapping: ModelMapping,
            event_mapping: EventMapping,
            timeseries_mapping: CurveMapping,
            start: int,
            stop: int,
            ) -> SimulationResult:
        """Run the dynawaltz simulation"""
        return SimulationResult(
            _pp.run_dynamic_model(
                self._handle,
                network._handle,
                model_mapping._handle,
                event_mapping._handle,
                timeseries_mapping._handle,
                start, stop)
        )
