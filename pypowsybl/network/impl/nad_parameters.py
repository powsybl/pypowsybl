# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp


class NadParameters:
    """
    This class represents nad parameters for a network area diagram svg generation."""

    def __init__(self, edge_name_displayed: bool = False, id_displayed: bool = False,
                 edge_info_along_edge: bool = True, power_value_precision: int = 0, angle_value_precision: int = 1,
                 current_value_precision: int = 0, voltage_value_precision: int = 1, bus_legend: bool = True,
                 substation_description_displayed: bool = False):
        self._edge_name_displayed = edge_name_displayed
        self._edge_info_along_edge = edge_info_along_edge
        self._id_displayed = id_displayed
        self._power_value_precision = power_value_precision
        self._angle_value_precision = angle_value_precision
        self._current_value_precision = current_value_precision
        self._voltage_value_precision = voltage_value_precision
        self._bus_legend = bus_legend
        self._substation_description_displayed = substation_description_displayed

    @property
    def edge_name_displayed(self) -> bool:
        """edge_name_displayed"""
        return self._edge_name_displayed

    @property
    def edge_info_along_edge(self) -> bool:
        """edge_info_along_edge"""
        return self.edge_info_along_edge

    @property
    def id_displayed(self) -> bool:
        """id_displayed"""
        return self._id_displayed

    @property
    def power_value_precision(self) -> int:
        """power_value_precision"""
        return self._power_value_precision

    @property
    def angle_value_precision(self) -> int:
        """angle_value_precision"""
        return self._angle_value_precision

    @property
    def current_value_precision(self) -> int:
        """current_value_precision"""
        return self._current_value_precision

    @property
    def voltage_value_precision(self) -> int:
        """voltage_value_precision"""
        return self._voltage_value_precision

    @property
    def bus_legend(self) -> int:
        """bus_legend"""
        return self._bus_legend

    @property
    def substation_description_displayed(self) -> int:
        """substation_description_displayed"""
        return self._substation_description_displayed

    def _to_c_parameters(self) -> _pp.NadParameters:
        c_parameters = _pp.NadParameters()
        c_parameters.edge_name_displayed = self._edge_name_displayed
        c_parameters.edge_info_along_edge = self._edge_info_along_edge
        c_parameters.id_displayed = self._id_displayed
        c_parameters.power_value_precision = self._power_value_precision
        c_parameters.angle_value_precision = self._angle_value_precision
        c_parameters.current_value_precision = self._current_value_precision
        c_parameters.voltage_value_precision = self._voltage_value_precision
        c_parameters.bus_legend = self._bus_legend
        c_parameters.substation_description_displayed = self._substation_description_displayed
        return c_parameters
