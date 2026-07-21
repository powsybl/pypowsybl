# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
class ModifiedMonitoredElementsParameters:  # pylint: disable=too-few-public-methods
    """
    Parameters which define what monitored elements state should be stored in the post-contingency results
    (depending on whether this state has changed more than some threshold between N and post-contingency situations).

    If both voltage thresholds are different from 0, the applied voltage threshold is the minimum of the two.

    Args:
        power_modification_threshold: for branches and 3 windings transformers, if equal to 1, their state is stored in the post-contingency results
                                    only if their power (active or reactive) has changed by more than 1 MW / MVAR between N and post-contingency situations.
        voltage_modification_proportional_threshold: for buses, if equal to 0.1, their state is stored in the post-contingency results
                                    only if their voltage has changed by more than 10% between N and post-contingency.
        voltage_modification_absolute_threshold: for buses, if equal to 1, their state is stored in the post-contingency results
                                    only if their voltage has changed by more than 1 kV between N and post-contingency.
    """

    def __init__(self, power_modification_threshold: float = 0.0, voltage_modification_proportional_threshold: float = 0.0,
                 voltage_modification_absolute_threshold: float = 0.0):
        self.power_modification_threshold = power_modification_threshold
        self.voltage_modification_proportional_threshold = voltage_modification_proportional_threshold
        self.voltage_modification_absolute_threshold = voltage_modification_absolute_threshold


    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", power_modification_threshold={self.power_modification_threshold!r}" \
               f", voltage_modification_proportional_threshold={self.voltage_modification_proportional_threshold!r}" \
               f", voltage_modification_absolute_threshold={self.voltage_modification_absolute_threshold!r}" \
               f")"
