# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List
from pypowsybl import _pypowsybl


class LoadsVariationMapping:
    """
    Mapping of the loads variations applied during a margin calculation.

    Each variation increases (or decreases) the active power of a group of loads by the
    same variation value.
    """

    def __init__(self) -> None:
        self._handle = _pypowsybl.create_loads_variation_mapping()

    def add_loads_variation(self, load_ids: List[str], variation_value: float) -> None:
        """ Add a loads variation.

        Args:
            load_ids: identifiers of the loads whose active power is varied
            variation_value: the active power variation applied to the group of loads
        """
        _pypowsybl.add_loads_variation(self._handle, load_ids, variation_value)
