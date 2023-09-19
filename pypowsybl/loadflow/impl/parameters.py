# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Sequence, Dict
from pypowsybl._pypowsybl import (
    ConnectedComponentMode,
    BalanceType,
    VoltageInitMode,
    LoadFlowParameters
)

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
VoltageInitMode.__module__ = __name__
BalanceType.__module__ = __name__
ConnectedComponentMode.__module__ = __name__


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
        no_generator_reactive_limits: Ignore generators reactive limits.
        phase_shifter_regulation_on: Simulate phase shifters regulation.
        twt_split_shunt_admittance: Split shunt admittance of transformers on both sides.
            Change the modelling of transformer legs. If you want to split the conductance and the susceptance in two,
            one at each side of the serie impedance, use ``True``.
        simul_shunt: Simulate voltage control of shunt compensators.
        read_slack_bus: Read slack bus from the network.
            The slack bus needs to be defined through a dedicate extension. Prefer ``False`` if you want to use
            your loadflow provider selection mechanism, typically the most meshed bus.
        write_slack_bus: Write selected slack bus to the network.
            Will tag the slack bus selected by your loadflow provider with an extension.
        distributed_slack: Distribute active power slack on the network.
            ``True`` means that the active power slack is distributed, on loads or on generators according to ``balance_type``.
        balance_type: How to distributed active power slack.
            Use ``PROPORTIONAL_TO_LOAD`` to distribute slack on loads,
            ``PROPORTIONAL_TO_GENERATION_P_MAX`` or ``PROPORTIONAL_TO_GENERATION_P`` to distribute on generators.
        dc_use_transformer_ratio: In DC mode, take into account transformer ratio.
            Used only for DC load flows, to include ratios in the equation system.
        countries_to_balance: List of countries participating to slack distribution.
            Used only if distributed_slack is ``True``.
        connected_component_mode: Define which connected components should be computed.
            Use ``MAIN`` to computes flows only on the main connected component,
            or prefer ``ALL`` for a run on all connected component.
        provider_parameters: Define parameters linked to the loadflow provider
            the names of the existing parameters can be found with method ``get_provider_parameters_names``
    """

    def __init__(self, voltage_init_mode: VoltageInitMode = None,
                 transformer_voltage_control_on: bool = None,
                 no_generator_reactive_limits: bool = None,
                 phase_shifter_regulation_on: bool = None,
                 twt_split_shunt_admittance: bool = None,
                 simul_shunt: bool = None,
                 read_slack_bus: bool = None,
                 write_slack_bus: bool = None,
                 distributed_slack: bool = None,
                 balance_type: BalanceType = None,
                 dc_use_transformer_ratio: bool = None,
                 countries_to_balance: Sequence[str] = None,
                 connected_component_mode: ConnectedComponentMode = None,
                 provider_parameters: Dict[str, str] = None):
        self._init_with_default_values()
        if voltage_init_mode is not None:
            self.voltage_init_mode = voltage_init_mode
        if transformer_voltage_control_on is not None:
            self.transformer_voltage_control_on = transformer_voltage_control_on
        if no_generator_reactive_limits is not None:
            self.no_generator_reactive_limits = no_generator_reactive_limits
        if phase_shifter_regulation_on is not None:
            self.phase_shifter_regulation_on = phase_shifter_regulation_on
        if twt_split_shunt_admittance is not None:
            self.twt_split_shunt_admittance = twt_split_shunt_admittance
        if simul_shunt is not None:
            self.simul_shunt = simul_shunt
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
        if connected_component_mode is not None:
            self.connected_component_mode = connected_component_mode
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_from_c(self, c_parameters: LoadFlowParameters) -> None:
        self.voltage_init_mode = c_parameters.voltage_init_mode
        self.transformer_voltage_control_on = c_parameters.transformer_voltage_control_on
        self.no_generator_reactive_limits = c_parameters.no_generator_reactive_limits
        self.phase_shifter_regulation_on = c_parameters.phase_shifter_regulation_on
        self.twt_split_shunt_admittance = c_parameters.twt_split_shunt_admittance
        self.simul_shunt = c_parameters.simul_shunt
        self.read_slack_bus = c_parameters.read_slack_bus
        self.write_slack_bus = c_parameters.write_slack_bus
        self.distributed_slack = c_parameters.distributed_slack
        self.balance_type = c_parameters.balance_type
        self.dc_use_transformer_ratio = c_parameters.dc_use_transformer_ratio
        self.countries_to_balance = c_parameters.countries_to_balance
        self.connected_component_mode = c_parameters.connected_component_mode
        self.provider_parameters = dict(
            zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))

    def _init_with_default_values(self) -> None:
        self._init_from_c(LoadFlowParameters())

    def _to_c_parameters(self) -> LoadFlowParameters:
        c_parameters = LoadFlowParameters()
        c_parameters.voltage_init_mode = self.voltage_init_mode
        c_parameters.transformer_voltage_control_on = self.transformer_voltage_control_on
        c_parameters.no_generator_reactive_limits = self.no_generator_reactive_limits
        c_parameters.phase_shifter_regulation_on = self.phase_shifter_regulation_on
        c_parameters.twt_split_shunt_admittance = self.twt_split_shunt_admittance
        c_parameters.simul_shunt = self.simul_shunt
        c_parameters.read_slack_bus = self.read_slack_bus
        c_parameters.write_slack_bus = self.write_slack_bus
        c_parameters.distributed_slack = self.distributed_slack
        c_parameters.balance_type = self.balance_type
        c_parameters.dc_use_transformer_ratio = self.dc_use_transformer_ratio
        c_parameters.countries_to_balance = self.countries_to_balance
        c_parameters.connected_component_mode = self.connected_component_mode
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"voltage_init_mode={self.voltage_init_mode.name}" \
               f", transformer_voltage_control_on={self.transformer_voltage_control_on!r}" \
               f", no_generator_reactive_limits={self.no_generator_reactive_limits!r}" \
               f", phase_shifter_regulation_on={self.phase_shifter_regulation_on!r}" \
               f", twt_split_shunt_admittance={self.twt_split_shunt_admittance!r}" \
               f", simul_shunt={self.simul_shunt!r}" \
               f", read_slack_bus={self.read_slack_bus!r}" \
               f", write_slack_bus={self.write_slack_bus!r}" \
               f", distributed_slack={self.distributed_slack!r}" \
               f", balance_type={self.balance_type.name}" \
               f", dc_use_transformer_ratio={self.dc_use_transformer_ratio!r}" \
               f", countries_to_balance={self.countries_to_balance}" \
               f", connected_component_mode={self.connected_component_mode!r}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"
