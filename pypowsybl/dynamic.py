#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

import os
from enum import Enum as _Enum
from uuid import uuid4
from typing import List as _List, Union as _Union
import pandas as _pd
from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import DynamicMappingType
from pypowsybl.network import Network as _Network
from pypowsybl.util import create_data_frame_from_series_array
from pypowsybl.utils.dataframes import _adapt_df_or_kwargs, _add_index_to_kwargs, _create_c_dataframe


class BranchSide(_Enum):
    '''
    warning the values are hardcoded in java layer
    '''
    ONE = "one"
    TWO = "two"


class ModelMapping:
    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_model_mapping()

    def add_alpha_beta_load(self, static_id: str, parameter_set_id: str) -> None:
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=parameter_set_id,
                                      mapping_type=DynamicMappingType.ALPHA_BETA_LOAD)

    def add_one_transformer_load(self, static_id: str, dynamic_param: str) -> None:
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=dynamic_param,
                                      mapping_type=DynamicMappingType.ONE_TRANSFORMER_LOAD)

    def add_omega_ref(self, generator_id: str) -> None:
        self.add_all_dynamic_mappings(static_id=generator_id,
                                      parameter_set_id="",
                                      mapping_type=DynamicMappingType.OMEGA_REF)

    def add_generator_synchronous_three_windings(self, static_id: str, dynamic_param: str) -> None:
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=dynamic_param,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS)

    def add_generator_synchronous_three_windings_proportional_regulations(self, static_id: str, dynamic_param: str) -> None:
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=dynamic_param,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS)

    def add_generator_synchronous_four_windings(self, static_id: str, dynamic_param: str) -> None:
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=dynamic_param,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS)

    def add_generator_synchronous_four_windings_proportional_regulations(self, static_id: str, dynamic_param: str) -> None:
        self.add_all_dynamic_mappings(static_id=static_id,
                                      parameter_set_id=dynamic_param,
                                      mapping_type=DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS_PROPORTIONAL_REGULATIONS)

    def add_current_limit_automaton(self, static_id: str, dynamic_param: str, branch_side: BranchSide) -> None:
        _pp.add_current_limit_automaton(
            self._handle, static_id, dynamic_param, branch_side.value)

    def add_all_dynamic_mappings(self, df: _pd.DataFrame = None, **kwargs: _Union[str, DynamicMappingType]) -> None:
        """
        Update the dynamic mapping of a simulation, must provide a :class:`~pandas.DataFrame` or as named arguments.
        The dataframe must contains these three columns :
            - static_id: id of the network element to map (or the id of the generator for omega_ref)
            - parameter_set_id: set id in the parameter file
            - mapping_type: value of enum DynamicMappingType
        For current_limit_automaton the branch side defaults to ONE, see add_current_limit_automaton to add a model with another branch side.
        For omega_ref the static_id column refers to the generator's id, and the parameter_set_id will be ignored
        """
        metadata = _pp.get_dynamic_mappings_meta_data()
        if kwargs:
            kwargs = _add_index_to_kwargs(metadata, **kwargs)
        df = _adapt_df_or_kwargs(metadata, df, **kwargs)
        c_df = _create_c_dataframe(df, metadata)
        _pp.add_all_dynamic_mappings(self._handle, c_df)


class CurveMapping:
    def __init__(self) -> None:
        self._handle = _pp.create_timeseries_mapping()

    def add_curve(self, dynamic_id: str, variable: str) -> None:
        _pp.add_curve(self._handle, dynamic_id, variable)

    def add_curves(self, dynamic_id: str, variables: _List[str]) -> None:
        for var in variables:
            _pp.add_curve(self._handle, dynamic_id, var)


class EventType(_Enum):
    QUADRIPOLE_DISCONNECT = 'QUADRIPOLE_DISCONNECT'
    SET_POINT_BOOLEAN = 'SET_POINT_BOOLEAN'
    BRANCH_DISCONNECTION = 'BRANCH_DISCONNECTION'


class EventMapping:
    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_quadripole_disconnection(self, event_model_id: str, static_id: str, parameter_set_id: str) -> None:
        _pp.add_event_quadripole_disconnection(
            self._handle, event_model_id, static_id, parameter_set_id)

    def add_set_point_boolean(self, event_model_id: str, static_id: str, parameter_set_id: str) -> None:
        _pp.add_event_set_point_boolean(
            self._handle, event_model_id, static_id, parameter_set_id)

    def add_event(self, parameter_set_id: str, event: EventType, static_id: str, event_id: str = None) -> None:
        if not event_id:
            event_id = str(uuid4())

        if event is EventType.QUADRIPOLE_DISCONNECT:
            _pp.add_event_quadripole_disconnection(
                self._handle, event_id, static_id, parameter_set_id)
            return
        if event is EventType.SET_POINT_BOOLEAN:
            _pp.add_event_set_point_boolean(
                self._handle, event_id, static_id, parameter_set_id)
            return

        raise Exception(
            f"Pypowsybl-DynamicSimulationError: Unknown event {event}")

    @staticmethod
    def get_possible_events() -> _List[EventType]:
        return list(EventType)


class SimulationResult:
    def __init__(self, handle: _pp.JavaHandle) -> None:
        self._handle = handle

    def state(self) -> str:
        return _pp.get_dynamic_simulation_results_status(self._handle)

    @property
    def status(self) -> str:
        return self.state()

    def get_curve(self, curve_name: str) -> _pd.DataFrame:
        series_array = _pp.get_dynamic_curve(self._handle, curve_name)
        return create_data_frame_from_series_array(series_array)

    def get_all_curves(self, curve_mapping: CurveMapping, dynamic_id: str) -> _pd.DataFrame:
        curve_name_lst = _pp.get_all_dynamic_curves_ids(
            curve_mapping._handle, dynamic_id)
        df_curves = [self.get_curve(curve_name)
                     for curve_name in curve_name_lst]
        return _pd.concat(df_curves, axis=1)


class Simulation:
    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_simulation_context()

    def run(self,
            network: _Network,
            model_mapping: ModelMapping,
            event_mapping: EventMapping,
            timeseries_mapping: CurveMapping,
            start: int,
            stop: int,
            ) -> SimulationResult:
        return SimulationResult(
            _pp.run_dynamic_model(
                self._handle,
                network._handle,
                model_mapping._handle,
                event_mapping._handle,
                timeseries_mapping._handle,
                start, stop)
        )

    @staticmethod
    def set_config(path: str, config_name: str) -> None:
        os.environ["powsybl.config.dirs"] = path
        os.environ["powsybl.config.name"] = config_name
        _pp.set_powsybl_config_location(path, config_name)