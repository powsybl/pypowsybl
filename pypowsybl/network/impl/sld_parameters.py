# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp


class SldParameters:
    """
    This class represents sld parameters for a single line diagram svg generation."""

    def __init__(self, use_name: bool = False, center_name: bool = False, diagonal_label: bool = False,
                 nodes_infos: bool = False, tooltip_enabled: bool = False, topological_coloring: bool = True,
                 component_library: str = 'Convergence'):
        self._use_name = use_name
        self._center_name = center_name
        self._diagonal_label = diagonal_label
        self._nodes_infos = nodes_infos
        self._tooltip_enabled = tooltip_enabled
        self._topological_coloring = topological_coloring
        self._component_library = component_library

    @property
    def use_name(self) -> bool:
        """Use names instead of ids in labels."""
        return self._use_name

    @property
    def center_name(self) -> bool:
        """Center labels."""
        return self._center_name

    @property
    def diagonal_label(self) -> bool:
        """Display diagonal labels."""
        return self._diagonal_label

    @property
    def nodes_infos(self) -> bool:
        """When True, add infos about voltage and angle."""
        return self._nodes_infos

    @property
    def tool_tip_enabled(self) -> bool:
        """when True display tooltip"""
        return self._tooltip_enabled

    @property
    def topological_coloring(self) -> bool:
        """When False, coloring is based only on nominal voltage."""
        return self._topological_coloring

    @property
    def component_library(self) -> str:
        """name of the library used for component"""
        return self._component_library

    def _to_c_parameters(self) -> _pp.SldParameters:
        c_parameters = _pp.SldParameters()
        c_parameters.use_name = self._use_name
        c_parameters.center_name = self._center_name
        c_parameters.diagonal_label = self._diagonal_label
        c_parameters.topological_coloring = self._topological_coloring
        c_parameters.nodes_infos = self._nodes_infos
        c_parameters.tooltip_enabled = self._tooltip_enabled
        c_parameters.component_library = self._component_library
        return c_parameters
