# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
class IncreasedViolationsParameters:  # pylint: disable=too-few-public-methods
    """
    Parameters which define what violations should be considered as "increased" between N and post-contingency situations

    Args:
        flow_proportional_threshold: for current and flow violations, if equal to 0.1, the violations which value
                                     have increased of more than 10% between N and post-contingency are considered "increased"
        low_voltage_proportional_threshold: for low voltage violations, if equal to 0.1, the violations which value
                                            have reduced of more than 10% between N and post-contingency are considered "increased"
        low_voltage_absolute_threshold: for low voltage violations, if equal to 1, the violations which value
                                        have reduced of more than 1 kV between N and post-contingency are considered "increased"
        high_voltage_proportional_threshold: for high voltage violations, if equal to 0.1, the violations which value
                                             have increased of more than 10% between N and post-contingency are considered "increased"
        high_voltage_absolute_threshold: for high voltage violations, if equal to 1, the violations which value
                                         have increased of more than 1 kV between N and post-contingency are considered "increased"
    """

    def __init__(self, flow_proportional_threshold: float, low_voltage_proportional_threshold: float,
                 low_voltage_absolute_threshold: float, high_voltage_proportional_threshold: float,
                 high_voltage_absolute_threshold: float):
        self.flow_proportional_threshold = flow_proportional_threshold
        self.low_voltage_proportional_threshold = low_voltage_proportional_threshold
        self.low_voltage_absolute_threshold = low_voltage_absolute_threshold
        self.high_voltage_proportional_threshold = high_voltage_proportional_threshold
        self.high_voltage_absolute_threshold = high_voltage_absolute_threshold

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", flow_proportional_threshold={self.flow_proportional_threshold!r}" \
               f", low_voltage_proportional_threshold={self.low_voltage_proportional_threshold!r}" \
               f", low_voltage_absolute_threshold={self.low_voltage_absolute_threshold!r}" \
               f", high_voltage_proportional_threshold={self.high_voltage_proportional_threshold!r}" \
               f", high_voltage_absolute_threshold={self.high_voltage_absolute_threshold!r}" \
               f")"
