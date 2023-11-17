<<<<<<< HEAD
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp


class NadParameters:
    """
    This class represents nad parameters for a network area diagram svg generation."""

    def __init__(self, edge_name_displayed: bool = True):
        self._edge_name_displayed = edge_name_displayed

    @property
    def edge_name_displayed(self) -> bool:
        """edge_name_displayed"""
        return self._edge_name_displayed

    def _to_c_parameters(self) -> _pp.NadParameters:
        c_parameters = _pp.NadParameters()
        c_parameters.edge_name_displayed = self._edge_name_displayed
        return c_parameters
||||||| constructed merge base
=======
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp


class NadParameters:
    """
    This class represents nad parameters for a network area diagram svg generation."""

    def __init__(self, edge_name_displayed: bool = True):
        self._edge_name_displayed = edge_name_displayed

    @property
    def edge_name_displayed(self) -> bool:
        """edge_name_displayed"""
        return self._edge_name_displayed

    def _to_c_parameters(self) -> _pp.NadParameters:
        c_parameters = _pp.NadParameters()
        c_parameters.edge_name_displayed = self._edge_name_displayed
        return c_parameters
>>>>>>> refactor network area diagram parameters
