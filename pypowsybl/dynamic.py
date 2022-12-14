#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

from enum import Enum as _Enum
import os
from pypowsybl import _pypowsybl as _pp
from pypowsybl.network import Network as _Network


class BranchSide(_Enum):
    '''
    warning the values are hardcoded in java layer
    '''
    ONE = "one"
    TWO = "two"


class ModelMapping:
    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_model_mapping()

    def add_alphabeta_load(self, static_id: str, parameter_set_id: str) -> None:
        _pp.add_alphabeta_load(self._handle, static_id, parameter_set_id)

    def add_one_transformer_load(self, static_id: str, dynamic_param: str) -> None:
        _pp.add_one_transformer_load(self._handle, static_id, dynamic_param)

    def add_omega_ref(self, generator_id: str) -> None:
        _pp.add_omega_ref(self._handle, generator_id)

    def add_generator_synchronous_three_windings(self, static_id: str, dynamic_param: str) -> None:
        _pp.add_generator_synchronous_three_windings(
            self._handle, static_id, dynamic_param)

    def add_generator_synchronous_three_windings_proportional_regulations(self, static_id: str, dynamic_param: str) -> None:
        _pp.add_generator_synchronous_three_windings_proportional_regulations(
            self._handle, static_id, dynamic_param)

    def add_generator_synchronous_four_windings(self, static_id: str, dynamic_param: str) -> None:
        _pp.add_generator_synchronous_four_windings(
            self._handle, static_id, dynamic_param)

    def add_generator_synchronous_four_windings_proportional_regulations(self, static_id: str, dynamic_param: str) -> None:
        _pp.add_generator_synchronous_four_windings_proportional_regulations(
            self._handle, static_id, dynamic_param)

    def add_current_limit_automaton(self, static_id: str, dynamic_param: str, branch_side: BranchSide) -> None:
        _pp.add_current_limit_automaton(
            self._handle, static_id, dynamic_param, branch_side.value)


class CurveMapping:
    def __init__(self) -> None:
        self._handle = _pp.create_timeseries_mapping()

    def add_curve(self, dynamic_id: str, variable: str) -> None:
        _pp.add_curve(self._handle, dynamic_id, variable)


class EventMapping:
    def __init__(self) -> None:
        self._handle = _pp.create_event_mapping()

    def add_quadripole_disconnection(self, event_model_id: str, static_id: str, parameter_set_id: str) -> None:
        _pp.add_event_quadripole_disconnection(
            self._handle, event_model_id, static_id, parameter_set_id)

    def add_set_point_boolean(self, event_model_id: str, static_id: str, parameter_set_id: str) -> None:
        _pp.add_event_set_point_boolean(
            self._handle, event_model_id, static_id, parameter_set_id)


class SimulationResult:
    def __init__(self, handle: _pp.JavaHandle) -> None:
        self._handle = handle


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

    def set_config(self, path: str, config_name: str) -> None:
        os.environ["powsybl.config.dirs"] = path
        os.environ["powsybl.config.name"] = config_name
        _pp.set_powsybl_config_location(path, config_name)
