# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl._pypowsybl import (
    ComponentMode,
    ConnectedComponentMode,
    BalanceType,
    VoltageInitMode,
    LoadFlowParameters,
    PyPowsyblError
)
import warnings
from typing import Sequence, Dict, Optional, Any

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
VoltageInitMode.__module__ = __name__
BalanceType.__module__ = __name__
ConnectedComponentMode.__module__ = __name__
ComponentMode.__module__ = __name__

import pypowsybl._pypowsybl


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a loadflow execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    Please note that loadflow providers may not honor all parameters, according to their capabilities.
    For example, some providers will not be able to simulate the voltage control of shunt compensators, etc.
    The exact behaviour of some parameters may also depend on your loadflow provider.
    Please check the documentation of your provider for that information.

    .. currentmodule:: pypowsybl.loadflow

    Args:
        voltage_init_mode: The resolution starting point.
            Use ``UNIFORM_VALUES`` for a flat start,
            and ``DC_VALUES`` for a DC load flow based starting point.
        transformer_voltage_control_on: Simulate transformer voltage control.
            The initial tap position is used as starting point for the resolution.
        use_reactive_limits: Use reactive limits (named no_generator_reactive_limits with inverted logic before PyPowSyBl 1.3.0).
        phase_shifter_regulation_on: Simulate phase shifters regulation.
        twt_split_shunt_admittance: Split shunt admittance of transformers on both sides.
            Change the modelling of transformer legs. If you want to split the conductance and the susceptance in two,
            one at each side of the serie impedance, use ``True``.
        shunt_compensator_voltage_control_on: Simulate voltage control of shunt compensators (named simul_shunt before PyPowSyBl 1.3.0).
        read_slack_bus: Read slack bus from the network.
            The slack bus needs to be defined through a dedicate extension. Prefer ``False`` if you want to use
            your loadflow provider selection mechanism, typically the most meshed bus.
        write_slack_bus: Write selected slack bus to the network.
            Will tag the slack bus selected by your loadflow provider with an extension.
        distributed_slack: Distribute active power slack on the network.
            ``True`` means that the active power slack is distributed, on loads or on generators according to ``balance_type``.
        balance_type: How to distribute active power slack.
            Use ``PROPORTIONAL_TO_LOAD`` to distribute slack on loads,
            ``PROPORTIONAL_TO_GENERATION_P_MAX`` or ``PROPORTIONAL_TO_GENERATION_P`` to distribute on generators.
        dc_use_transformer_ratio: In DC mode, take into account transformer ratio.
            Used only for DC load flows, to include ratios in the equation system.
        countries_to_balance: List of countries participating to slack distribution.
            Used only if distributed_slack is ``True``.
        component_mode: Defines which network components should be computed.
            Use ``MAIN_SYNCHRONOUS`` to computes flows only on the main synchronous component,
            ``MAIN_CONNECTED`` to computes flows only on the main connected component,
            or prefer ``ALL_CONNECTED`` for a run on all connected components.
        connected_component_mode: Deprecated, use parameter ``component_mode`` (``MAIN`` corresponds to component_mode = ``MAIN_CONNECTED``,
            ``ALL`` to component_mode = ``ALL_CONNECTED``).
        hvdc_ac_emulation: Enable AC emulation of HVDC links.
        dc_power_factor: Power factor used to convert current limits into active power limits in DC calculations.
        provider_parameters: Define parameters linked to the loadflow provider
            the names of the existing parameters can be found with method ``get_provider_parameters_names``
    """

    def __init__(self, voltage_init_mode: Optional[VoltageInitMode] = None,
                 transformer_voltage_control_on: Optional[bool] = None,
                 use_reactive_limits: Optional[bool] = None,
                 phase_shifter_regulation_on: Optional[bool] = None,
                 twt_split_shunt_admittance: Optional[bool] = None,
                 shunt_compensator_voltage_control_on: Optional[bool] = None,
                 read_slack_bus: Optional[bool] = None,
                 write_slack_bus: Optional[bool] = None,
                 distributed_slack: Optional[bool] = None,
                 balance_type: Optional[BalanceType] = None,
                 dc_use_transformer_ratio: Optional[bool] = None,
                 countries_to_balance: Optional[Sequence[str]] = None,
                 component_mode: Optional[ComponentMode] = None,
                 connected_component_mode: Optional[ConnectedComponentMode] = None,
                 dc_power_factor: Optional[float] = None,
                 hvdc_ac_emulation: Optional[bool] = None,
                 provider_parameters: Optional[Dict[str, str]] = None):
        self._init_with_default_values()
        if voltage_init_mode is not None:
            self.voltage_init_mode = voltage_init_mode
        if transformer_voltage_control_on is not None:
            self.transformer_voltage_control_on = transformer_voltage_control_on
        if use_reactive_limits is not None:
            self.use_reactive_limits = use_reactive_limits
        if phase_shifter_regulation_on is not None:
            self.phase_shifter_regulation_on = phase_shifter_regulation_on
        if twt_split_shunt_admittance is not None:
            self.twt_split_shunt_admittance = twt_split_shunt_admittance
        if shunt_compensator_voltage_control_on is not None:
            self.shunt_compensator_voltage_control_on = shunt_compensator_voltage_control_on
        if read_slack_bus is not None:
            self.read_slack_bus = read_slack_bus
        if write_slack_bus is not None:
            self.write_slack_bus = write_slack_bus
        if distributed_slack is not None:
            self.distributed_slack = distributed_slack
        if balance_type is not None:
            self.balance_type = balance_type
        if dc_use_transformer_ratio is not None:
            self.dc_use_transformer_ratio = dc_use_transformer_ratio
        if countries_to_balance is not None:
            self.countries_to_balance = countries_to_balance
        if component_mode is not None and connected_component_mode is not None:
            raise PyPowsyblError("connected_component_mode and component_mode cannot be set at the same time, " +
                                 "use only component_mode as the other one is depreciated")
        if component_mode is not None:
            self.component_mode = component_mode
        if connected_component_mode is not None:
            warnings.warn("connected_component_mode is deprecated, use component_mode parameter instead", DeprecationWarning)
            self._convert_to_component_mode(connected_component_mode)
        if hvdc_ac_emulation is not None:
            self.hvdc_ac_emulation = hvdc_ac_emulation
        if dc_power_factor is not None:
            self.dc_power_factor = dc_power_factor
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_from_c(self, c_parameters: LoadFlowParameters) -> None:
        self.voltage_init_mode = c_parameters.voltage_init_mode
        self.transformer_voltage_control_on = c_parameters.transformer_voltage_control_on
        self.use_reactive_limits = c_parameters.use_reactive_limits
        self.phase_shifter_regulation_on = c_parameters.phase_shifter_regulation_on
        self.twt_split_shunt_admittance = c_parameters.twt_split_shunt_admittance
        self.shunt_compensator_voltage_control_on = c_parameters.shunt_compensator_voltage_control_on
        self.read_slack_bus = c_parameters.read_slack_bus
        self.write_slack_bus = c_parameters.write_slack_bus
        self.distributed_slack = c_parameters.distributed_slack
        self.balance_type = c_parameters.balance_type
        self.dc_use_transformer_ratio = c_parameters.dc_use_transformer_ratio
        self.countries_to_balance = c_parameters.countries_to_balance
        self.component_mode = c_parameters.component_mode
        self.hvdc_ac_emulation = c_parameters.hvdc_ac_emulation
        self.dc_power_factor = c_parameters.dc_power_factor
        self.provider_parameters = dict(
            zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))

    def _init_with_default_values(self) -> None:
        self._init_from_c(LoadFlowParameters())

    def _to_c_parameters(self) -> LoadFlowParameters:
        c_parameters = LoadFlowParameters()
        c_parameters.voltage_init_mode = self.voltage_init_mode
        c_parameters.transformer_voltage_control_on = self.transformer_voltage_control_on
        c_parameters.use_reactive_limits = self.use_reactive_limits
        c_parameters.phase_shifter_regulation_on = self.phase_shifter_regulation_on
        c_parameters.twt_split_shunt_admittance = self.twt_split_shunt_admittance
        c_parameters.shunt_compensator_voltage_control_on = self.shunt_compensator_voltage_control_on
        c_parameters.read_slack_bus = self.read_slack_bus
        c_parameters.write_slack_bus = self.write_slack_bus
        c_parameters.distributed_slack = self.distributed_slack
        c_parameters.balance_type = self.balance_type
        c_parameters.dc_use_transformer_ratio = self.dc_use_transformer_ratio
        c_parameters.countries_to_balance = self.countries_to_balance
        c_parameters.component_mode = self.component_mode
        c_parameters.hvdc_ac_emulation = self.hvdc_ac_emulation
        c_parameters.dc_power_factor = self.dc_power_factor
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    @property
    def connected_component_mode(self) -> Optional[ConnectedComponentMode]:
        warnings.warn("connected_component_mode is deprecated, use component_mode parameter instead", DeprecationWarning)
        return self._convert_to_connected_component_mode()

    @connected_component_mode.setter
    def connected_component_mode(self, connected_component_mode: ConnectedComponentMode) -> None:
        warnings.warn("connected_component_mode is deprecated, use component_mode parameter instead", DeprecationWarning)
        self._convert_to_component_mode(connected_component_mode)

    def _convert_to_component_mode(self, connected_component_mode: ConnectedComponentMode) -> None:
        if connected_component_mode == ConnectedComponentMode.MAIN:
            self.component_mode = ComponentMode.MAIN_CONNECTED
        else:
            self.component_mode = ComponentMode.ALL_CONNECTED

    def _convert_to_connected_component_mode(self) -> Optional[ConnectedComponentMode]:
        if self.component_mode == ComponentMode.MAIN_CONNECTED:
            return ConnectedComponentMode.MAIN
        elif self.component_mode == ComponentMode.ALL_CONNECTED:
            return ConnectedComponentMode.ALL
        else:
            return None

    @staticmethod
    def from_json(json_str: str) -> "Parameters":
        parameters = Parameters()
        parameters._init_from_c(pypowsybl._pypowsybl.create_loadflow_parameters_from_json(json_str))
        return parameters

    def to_json(self) -> str:
        return pypowsybl._pypowsybl.write_loadflow_parameters_to_json(self._to_c_parameters())

    def __getstate__(self) -> Dict[str, Any]:
        return {'json': self.to_json()}

    def __setstate__(self, state: Dict[str, Any]) -> None:
        self._init_from_c(pypowsybl._pypowsybl.create_loadflow_parameters_from_json(state['json']))

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"voltage_init_mode={self.voltage_init_mode.name}" \
               f", transformer_voltage_control_on={self.transformer_voltage_control_on!r}" \
               f", use_reactive_limits={self.use_reactive_limits!r}" \
               f", phase_shifter_regulation_on={self.phase_shifter_regulation_on!r}" \
               f", twt_split_shunt_admittance={self.twt_split_shunt_admittance!r}" \
               f", shunt_compensator_voltage_control_on={self.shunt_compensator_voltage_control_on!r}" \
               f", read_slack_bus={self.read_slack_bus!r}" \
               f", write_slack_bus={self.write_slack_bus!r}" \
               f", distributed_slack={self.distributed_slack!r}" \
               f", balance_type={self.balance_type.name}" \
               f", dc_use_transformer_ratio={self.dc_use_transformer_ratio!r}" \
               f", countries_to_balance={self.countries_to_balance}" \
               f", component_mode={self.component_mode!r}" \
               f", hvdc_ac_emulation={self.hvdc_ac_emulation!r}" \
               f", dc_power_factor={self.dc_power_factor!r}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"
