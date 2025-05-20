# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
from __future__ import annotations

from typing import List, Optional, Type, Literal, Dict, Any

import numpy as np
from pypowsybl._pypowsybl import Grid2opDoubleValueType
from pypowsybl._pypowsybl import Grid2opIntegerValueType
from pypowsybl._pypowsybl import Grid2opStringValueType
from pypowsybl._pypowsybl import Grid2opUpdateDoubleValueType
from pypowsybl._pypowsybl import Grid2opUpdateIntegerValueType

from pypowsybl import _pypowsybl
from pypowsybl.loadflow import Parameters, ComponentResult
from pypowsybl.network import Network


class Backend:
    def __init__(self, network: Network,
                 consider_open_branch_reactive_flow: bool = False,
                 check_isolated_and_disconnected_injections: bool = True,
                 buses_per_voltage_level: int = 2,
                 connect_all_elements_to_first_bus: bool = True):
        self._network = network
        self._consider_open_branch_reactive_flow = consider_open_branch_reactive_flow
        self._check_isolated_and_disconnected_injections = check_isolated_and_disconnected_injections
        self._buses_per_voltage_level = buses_per_voltage_level
        self._connect_all_elements_to_first_bus = connect_all_elements_to_first_bus
        self._handle = _pypowsybl.create_grid2op_backend(self._network._handle,
                                                         self._consider_open_branch_reactive_flow,
                                                         self._check_isolated_and_disconnected_injections,
                                                         self._buses_per_voltage_level,
                                                         self._connect_all_elements_to_first_bus)

    @property
    def network(self) -> Network:
        return self._network

    def close(self) -> None:
        _pypowsybl.free_grid2op_backend(self._handle)

    def __enter__(self) -> Backend:
        return self

    def __exit__(self, exc_type: Optional[Type[BaseException]],
                       exc_value: Optional[BaseException],
                       traceback: Optional[object]) -> Literal[False]:
        self.close()
        return False

    def __getstate__(self) -> Dict[str, Any]:
        return {'xiidm': self._network.save_to_binary_buffer('XIIDM', {}),
                'consider_open_branch_reactive_flow': self._consider_open_branch_reactive_flow,
                'check_isolated_and_disconnected_injections': self._check_isolated_and_disconnected_injections,
                'buses_per_voltage_level': self._buses_per_voltage_level,
                'connect_all_elements_to_first_bus': self._connect_all_elements_to_first_bus}

    def __setstate__(self, state: Dict[str, Any]) -> None:
        self._network = Network(_pypowsybl.load_network_from_binary_buffers([state['xiidm'].getbuffer()], {}, [], None))
        self._consider_open_branch_reactive_flow = state['consider_open_branch_reactive_flow']
        self._check_isolated_and_disconnected_injections = state['check_isolated_and_disconnected_injections']
        self._buses_per_voltage_level = state['buses_per_voltage_level']
        self._connect_all_elements_to_first_bus = state['connect_all_elements_to_first_bus']
        self._handle = _pypowsybl.create_grid2op_backend(self._network._handle,
                                                         self._connect_all_elements_to_first_bus,
                                                         self._check_isolated_and_disconnected_injections,
                                                         self._buses_per_voltage_level,
                                                         self._connect_all_elements_to_first_bus)

    def get_string_value(self, value_type: Grid2opStringValueType) -> np.ndarray:
        return np.array(_pypowsybl.get_grid2op_string_value(self._handle, value_type))

    def get_integer_value(self, value_type: Grid2opIntegerValueType) -> np.ndarray:
        return np.array(_pypowsybl.get_grid2op_integer_value(self._handle, value_type))

    def get_double_value(self, value_type: Grid2opDoubleValueType) -> np.ndarray:
        return np.array(_pypowsybl.get_grid2op_double_value(self._handle, value_type))

    def update_double_value(self, value_type: Grid2opUpdateDoubleValueType, value: np.ndarray, changed: np.ndarray) -> None:
        _pypowsybl.update_grid2op_double_value(self._handle, value_type, value, changed)

    def update_integer_value(self, value_type: Grid2opUpdateIntegerValueType, value: np.ndarray, changed: np.ndarray) -> None:
        _pypowsybl.update_grid2op_integer_value(self._handle, value_type, value, changed)

    def check_isolated_and_disconnected_injections(self) -> bool:
        return _pypowsybl.check_grid2op_isolated_and_disconnected_injections(self._handle)

    def run_pf(self, dc: bool = False, parameters: Parameters = None) -> List[ComponentResult]:
        p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()  # pylint: disable=protected-access
        return [ComponentResult(res) for res in _pypowsybl.run_grid2op_loadflow(self._handle, dc, p)]
