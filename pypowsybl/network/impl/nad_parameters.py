# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import (
    NadLayoutType, EdgeInfoType
)

class NadParameters:
    """
    This class represents nad parameters for a network area diagram svg generation."""

    def __init__(self, edge_name_displayed: bool = False, id_displayed: bool = False,
                 edge_info_along_edge: bool = True, power_value_precision: int = 0, angle_value_precision: int = 1,
                 current_value_precision: int = 0, voltage_value_precision: int = 1, bus_legend: bool = True,
                 substation_description_displayed: bool = False, layout_type: NadLayoutType = NadLayoutType.FORCE_LAYOUT,
                 scaling_factor: int = 150000, radius_factor: float = 150.0,
                 edge_info_displayed: EdgeInfoType = EdgeInfoType.ACTIVE_POWER, voltage_level_details: bool = True):
        self._edge_name_displayed = edge_name_displayed
        self._edge_info_along_edge = edge_info_along_edge
        self._id_displayed = id_displayed
        self._power_value_precision = power_value_precision
        self._angle_value_precision = angle_value_precision
        self._current_value_precision = current_value_precision
        self._voltage_value_precision = voltage_value_precision
        self._bus_legend = bus_legend
        self._substation_description_displayed = substation_description_displayed
        self._layout_type = layout_type
        self._scaling_factor = scaling_factor
        self._radius_factor = radius_factor
        self._edge_info_displayed = edge_info_displayed
        self._voltage_level_details = voltage_level_details

    @property
    def edge_name_displayed(self) -> bool:
        """edge_name_displayed"""
        return self._edge_name_displayed

    @property
    def edge_info_along_edge(self) -> bool:
        """edge_info_along_edge"""
        return self._edge_info_along_edge

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

    @property
    def layout_type(self) -> NadLayoutType:
        """layout_type"""
        return self._layout_type

    @property
    def scaling_factor(self) -> int:
        """scaling_factor"""
        return self._scaling_factor

    @property
    def radius_factor(self) -> float:
        """radius_factor"""
        return self._radius_factor

    @property
    def edge_info_displayed(self) -> EdgeInfoType:
        """edge_info_displayed"""
        return self._edge_info_displayed

    @property
    def voltage_level_details(self) -> bool:
        """voltage_level_details"""
        return self._voltage_level_details

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
        c_parameters.layout_type = self._layout_type
        c_parameters.scaling_factor = self._scaling_factor
        c_parameters.radius_factor = self._radius_factor
        c_parameters.edge_info_displayed = self._edge_info_displayed
        c_parameters.voltage_level_details = self._voltage_level_details
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"edge_name_displayed={self._edge_name_displayed}" \
               f", edge_info_along_edge={self._edge_info_along_edge}" \
               f", id_displayed={self._id_displayed}" \
               f", power_value_precision={self._power_value_precision}" \
               f", angle_value_precision={self._angle_value_precision}" \
               f", current_value_precision={self._current_value_precision}" \
               f", voltage_value_precision={self._voltage_value_precision}" \
               f", bus_legend={self._bus_legend}" \
               f", substation_description_displayed={self._substation_description_displayed}" \
               f", layout_type={self._layout_type}" \
               f", scaling_factor={self._scaling_factor}" \
               f", radius_factor={self._radius_factor}" \
               f", edge_info_displayed={self._edge_info_displayed}" \
               f", voltage_level_details={self._voltage_level_details}" \
               f")"
