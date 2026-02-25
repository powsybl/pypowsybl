# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List
from pypowsybl._pypowsybl import (ScalingType, ScalingConvention, Priority, ScalingParameters as CParameters)

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
ScalingType.__module__ = __name__
ScalingConvention.__module__ = __name__
Priority.__module__ = __name__
CParameters.__module__ = __name__


class ScalingParameters:
    """
    Parameters to modify active power with a Scalable.

    Args:
        scaling_convention: The scaling convention to use. The default is ``GENERATOR_CONVENTION`` and can be changed to ``LOAD_CONVENTION``.
        constant_power_factor: If True, the scaling is done with a constant power factor (``False`` by default)
        reconnect: If True, scaling can reconnect the underlying injection if it is disconnected (``False`` by default)
        allows_generator_out_of_active_power_limits:
            If True, the scalable can modify the active power of a generator even if it is out of its active power limits (``False`` by default)
        priority: representing the priority of the scaling for ProportionalScalable. It can be either :
                    - ``RESPECT_OF_VOLUME_ASKED`` (the scaling will distribute the power asked as much as possible by iterating if elements get saturated,
                     even if it means not respecting potential percentages)
                    - ``RESPECT_OF_DISTRIBUTION`` (the scaling will respect the percentages even if it means not scaling all what is asked)
                    - ``ONESHOT`` (the scaling will distribute the power asked as is, in one iteration even if elements get saturated and even if it means
                     not respecting potential percentages).
            (``ONESHOT`` by default)
        scaling_type: The type of scaling to use. The default is ``DELTA_P`` and can be changed to ``TARGET_P``.
        ignored_injection_ids: List of injection ids to ignore when scaling.
    """

    def __init__(self, scaling_convention: ScalingConvention  = None,
                 constant_power_factor: bool = None,
                 reconnect: bool = None,
                 allows_generator_out_of_active_power_limits: bool = None,
                 priority: Priority = None,
                 scaling_type: ScalingType = None,
                 ignored_injection_ids: List[str] = None):
        self._init_with_default_values()
        if scaling_convention is not None:
            self.scaling_convention = scaling_convention
        if constant_power_factor is not None:
            self.constant_power_factor = constant_power_factor
        if reconnect is not None:
            self.reconnect = reconnect
        if allows_generator_out_of_active_power_limits is not None:
            self.allows_generator_out_of_active_power_limits = allows_generator_out_of_active_power_limits
        if priority is not None:
            self.priority = priority
        if scaling_type is not None:
            self.scaling_type = scaling_type
        if ignored_injection_ids is not None:
            self.ignored_injection_ids = ignored_injection_ids

    def _init_from_c(self, c_parameters: CParameters) -> None:
        self.scaling_convention = c_parameters.scaling_convention
        self.constant_power_factor = c_parameters.constant_power_factor
        self.reconnect = c_parameters.reconnect
        self.allows_generator_out_of_active_power_limits = c_parameters.allows_generator_out_of_active_power_limits
        self.priority = c_parameters.priority
        self.scaling_type = c_parameters.scaling_type
        self.ignored_injection_ids = c_parameters.ignored_injection_ids

    def _init_with_default_values(self) -> None:
        self._init_from_c(CParameters())

    def _to_c_parameters(self) -> CParameters:
        c_parameters = CParameters()
        c_parameters.scaling_convention = self.scaling_convention
        c_parameters.constant_power_factor = self.constant_power_factor
        c_parameters.reconnect = self.reconnect
        c_parameters.allows_generator_out_of_active_power_limits = self.allows_generator_out_of_active_power_limits
        c_parameters.priority = self.priority
        c_parameters.scaling_type = self.scaling_type
        c_parameters.ignored_injection_ids = self.ignored_injection_ids
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"scaling_convention={self.scaling_convention}" \
               f", constant_power_factor={self.constant_power_factor!r}" \
               f", reconnect={self.reconnect!r}" \
               f", allows_generator_out_of_active_power_limits={self.allows_generator_out_of_active_power_limits!r}" \
               f", priority={self.priority!r}" \
               f", scaling_type={self.scaling_type!r}" \
               f", ignored_injection_ids={self.ignored_injection_ids!r}" \
               f")"