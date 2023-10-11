# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from .sld_parameters import SldParameters
from .parent_parameters import ParentParameters


class LayoutParameters(ParentParameters):
    """
    This class is only used for backward compatibility and represents sld parameters for a single line diagram svg
    generation."""

    def __init__(self, use_name: bool = False, center_name: bool = False, diagonal_label: bool = False,
                 topological_coloring: bool = True, nodes_infos: bool = False, component_library: str = 'Convergence'):
        self._use_name = use_name
        self._center_name = center_name
        self._diagonal_label = diagonal_label
        self._nodes_infos = nodes_infos
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
    def topological_coloring(self) -> bool:
        """When False, coloring is based only on nominal voltage."""
        return self._topological_coloring

    @property
    def nodes_infos(self) -> bool:
        """When True, add infos about voltage and angle."""
        return self._nodes_infos

    @property
    def component_library(self) -> str:
        """name of the library used for component"""
        return self._component_library

    def _to_sld_parameters(self) -> SldParameters:
        sld_parameters = SldParameters(self._use_name, self._center_name, self._diagonal_label, self._nodes_infos, self._topological_coloring, self._component_library)
        return sld_parameters
