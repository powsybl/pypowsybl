# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import RescaleMode

class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a flowdecomposition execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    .. currentmodule:: pypowsybl.flowdecomposition

    Args:
        enable_losses_compensation: Enable losses compensation.
            Use ``True`` to enable AC losses compensation on the DC network.
        losses_compensation_epsilon: Filter loads from the losses compensation.
            The loads with a too small absolute active power will be not be connected to the network.
            Use ``pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON = -1`` to disable filtering.
        sensitivity_epsilon: Filter sensitivity values
            The absolute small sensitivity values will be ignored.
            Use ``pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON = -1`` to disable filtering.
        rescale_enabled: Rescale the flow decomposition to the AC reference.
            Use``True`` to rescale flow decomposition to the AC reference.
        dc_fallback_enabled_after_ac_divergence: Defines the fallback bahavior after an AC divergence
            Use ``True`` to run DC loadflow if an AC loadflow diverges (default).
            Use ``False`` to throw an exception if an AC loadflow diverges.
        sensitivity_variable_batch_size: Defines the chunk size for sensitivity analysis.
            This will reduce memory footprint of flow decomposition but increase computation time.
            If setting a too high value, a max integer error may be thrown.
    """
    DISABLE_LOSSES_COMPENSATION_EPSILON = -1
    DISABLE_SENSITIVITY_EPSILON = -1

    def __init__(self,
                 enable_losses_compensation: bool = None,
                 losses_compensation_epsilon: float = None,
                 sensitivity_epsilon: float = None,
                 rescale_mode: RescaleMode = None,
                 dc_fallback_enabled_after_ac_divergence: bool = None,
                 sensitivity_variable_batch_size: int = None):

        self._init_with_default_values()
        if enable_losses_compensation is not None:
            self.enable_losses_compensation = enable_losses_compensation
        if losses_compensation_epsilon is not None:
            self.losses_compensation_epsilon = losses_compensation_epsilon
        if sensitivity_epsilon is not None:
            self.sensitivity_epsilon = sensitivity_epsilon
        if rescale_mode is not None:
            self.rescale_mode = rescale_mode
        if dc_fallback_enabled_after_ac_divergence is not None:
            self.dc_fallback_enabled_after_ac_divergence = dc_fallback_enabled_after_ac_divergence
        if sensitivity_variable_batch_size is not None:
            self.sensitivity_variable_batch_size = sensitivity_variable_batch_size

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.FlowDecompositionParameters()
        self.enable_losses_compensation = default_parameters.enable_losses_compensation
        self.losses_compensation_epsilon = default_parameters.losses_compensation_epsilon
        self.sensitivity_epsilon = default_parameters.sensitivity_epsilon
        self.rescale_mode = default_parameters.rescale_mode
        self.dc_fallback_enabled_after_ac_divergence = default_parameters.dc_fallback_enabled_after_ac_divergence
        self.sensitivity_variable_batch_size = default_parameters.sensitivity_variable_batch_size

    def _to_c_parameters(self) -> _pypowsybl.FlowDecompositionParameters:
        c_parameters = _pypowsybl.FlowDecompositionParameters()
        c_parameters.enable_losses_compensation = self.enable_losses_compensation
        c_parameters.losses_compensation_epsilon = self.losses_compensation_epsilon
        c_parameters.sensitivity_epsilon = self.sensitivity_epsilon
        c_parameters.rescale_mode = self.rescale_mode
        c_parameters.dc_fallback_enabled_after_ac_divergence = self.dc_fallback_enabled_after_ac_divergence
        c_parameters.sensitivity_variable_batch_size = self.sensitivity_variable_batch_size
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"enable_losses_compensation={self.enable_losses_compensation!r}" \
               f", losses_compensation_epsilon={self.losses_compensation_epsilon!r}" \
               f", sensitivity_epsilon={self.sensitivity_epsilon!r}" \
               f", rescale_mode={self.rescale_mode!r}" \
               f", dc_fallback_enabled_after_ac_divergence={self.dc_fallback_enabled_after_ac_divergence}" \
               f", sensitivity_variable_batch_size={self.sensitivity_variable_batch_size}" \
               f")"
