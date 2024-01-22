# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings

from .sld_parameters import SldParameters


class LayoutParameters(SldParameters):
    """
    .. deprecated:: 1.1.0
      Use :class:`SldParameters` instead.

    This class is only used for backward compatibility and represents sld parameters for a single line diagram svg
    generation."""

    def __init__(self, use_name: bool = False, center_name: bool = False, diagonal_label: bool = False,
                 topological_coloring: bool = True, nodes_infos: bool = False, component_library: str = 'Convergence'):
        warnings.warn("LayoutParameters is deprecated, use SldParameters instead", DeprecationWarning)
        super().__init__(use_name=use_name, center_name=center_name, diagonal_label=diagonal_label,
                         nodes_infos=nodes_infos, topological_coloring=topological_coloring,
                         component_library=component_library)
