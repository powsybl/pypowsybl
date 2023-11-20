# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp


class NadParameters:
    """
    This class represents nad parameters for a network area diagram svg generation."""

    def __init__(self, edge_name_displayed: bool = True, id_displayed: bool = False,
                 edge_info_along_edge: bool = True):
        self._edge_name_displayed = edge_name_displayed
        self._edge_info_along_edge = edge_info_along_edge
        self._id_displayed = id_displayed

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

    def _to_c_parameters(self) -> _pp.NadParameters:
        c_parameters = _pp.NadParameters()
        c_parameters.edge_name_displayed = self._edge_name_displayed
        c_parameters.edge_info_along_edge = self._edge_info_along_edge
        c_parameters.id_displayed = self._id_displayed
        return c_parameters