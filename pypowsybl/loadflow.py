#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pandas as pd
from pandas import DataFrame as _DataFrame
import _pypowsybl
from _pypowsybl import (
    LoadFlowComponentStatus as ComponentStatus,
    ConnectedComponentMode,
    BalanceType,
    VoltageInitMode,
    ValidationType
)
from pypowsybl.network import Network as _Network
from pypowsybl.util import create_data_frame_from_series_array as _create_data_frame_from_series_array
from typing import (
    List as _List,
    Sequence as _Sequence
)

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
VoltageInitMode.__module__ = __name__
BalanceType.__module__ = __name__
ConnectedComponentMode.__module__ = __name__
ComponentStatus.__name__ = 'ComponentStatus'
ComponentStatus.__module__ = __name__

# Pure python wrapper for C ext object
# although it adds some boiler plate code, it integrates better with tools such as sphinx
class ComponentResult(object):
    """
    Loadflow result for one connected component of the network.
    """

    def __init__(self, res: _pypowsybl.LoadFlowComponentResult):
        self._res = res

    @property
    def status(self):
        """Status of the loadflow for this component."""
        return self._res.status

    @property
    def connected_component_num(self):
        """Number of the connected component."""
        return self._res.connected_component_num

    @property
    def synchronous_component_num(self):
        """Number of the synchronous component."""
        return self._res.synchronous_component_num

    @property
    def iteration_count(self):
        """The number of iterations performed by the loadflow."""
        return self._res.iteration_count

    @property
    def slack_bus_id(self):
        """ID of the slack bus used for this component."""
        return self._res.slack_bus_id

    @property
    def slack_bus_active_power_mismatch(self):
        """Remaining active power slack at the end of the loadflow"""
        return self._res.slack_bus_active_power_mismatch

    def __repr__(self):
        return f"{self.__class__.__name__}("\
               f"connected_component_num={self.connected_component_num!r}"\
               f", synchronous_component_num={self.synchronous_component_num!r}"\
               f", status={self.status.name}"\
               f", iteration_count={self.iteration_count!r}"\
               f", slack_bus_id={self.slack_bus_id!r}"\
               f", slack_bus_active_power_mismatch={self.slack_bus_active_power_mismatch!r}"\
               f")"


class Parameters(_pypowsybl.LoadFlowParameters):
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
    """

    def __init__(self,
                 voltage_init_mode: VoltageInitMode = None,
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
                 countries_to_balance: _Sequence[str] = None,
                 connected_component_mode: ConnectedComponentMode = None):
        super().__init__()  # loads from platform config
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

    def __repr__(self):
        return f"{self.__class__.__name__}("\
               f"voltage_init_mode={self.voltage_init_mode.name}"\
               f", transformer_voltage_control_on={self.transformer_voltage_control_on!r}"\
               f", no_generator_reactive_limits={self.no_generator_reactive_limits!r}"\
               f", phase_shifter_regulation_on={self.phase_shifter_regulation_on!r}"\
               f", twt_split_shunt_admittance={self.twt_split_shunt_admittance!r}"\
               f", simul_shunt={self.simul_shunt!r}"\
               f", read_slack_bus={self.read_slack_bus!r}"\
               f", write_slack_bus={self.write_slack_bus!r}"\
               f", distributed_slack={self.distributed_slack!r}"\
               f", balance_type={self.balance_type.name}"\
               f", dc_use_transformer_ratio={self.dc_use_transformer_ratio!r}"\
               f", countries_to_balance={self.countries_to_balance}"\
               f", connected_component_mode={self.connected_component_mode!r}"\
               f")"


def run_ac(network: _Network, parameters: Parameters = None, provider='OpenLoadFlow') -> _List[ComponentResult]:
    """
    Run an AC loadflow on a network.

    Args:
        network:    a network
        parameters: the loadflow parameters
        provider:   the loadflow implementation provider, default OpenLoadFlow

    Returns:
        A list of component results, one for each component of the network.
    """
    p = parameters if parameters is not None else Parameters()
    return [ComponentResult(res) for res in _pypowsybl.run_load_flow(network._handle, False, p, provider)]


def run_dc(network: _Network, parameters: Parameters = None, provider='OpenLoadFlow') -> _List[ComponentResult]:
    """
    Run a DC loadflow on a network.

    Args:
        network:    a network
        parameters: the loadflow parameters
        provider:   the loadflow implementation provider, default OpenLoadFlow

    Returns:
        A list of component results, one for each component of the network.
    """
    p = parameters if parameters is not None else Parameters()
    return [ComponentResult(res) for res in _pypowsybl.run_load_flow(network._handle, True, p, provider)]


ValidationType.ALL = [ValidationType.BUSES, ValidationType.FLOWS, ValidationType.GENERATORS, ValidationType.SHUNTS,
                      ValidationType.SVCS, ValidationType.TWTS, ValidationType.TWTS3W]


class ValidationResult:
    """
    The result of a loadflow validation.
    """

    def __init__(self, branch_flows, buses, generators, svcs, shunts, twts, t3wts):
        self._branch_flows = branch_flows
        self._buses = buses
        self._generators = generators
        self._svcs = svcs
        self._shunts = shunts
        self._twts = twts
        self._t3wts = t3wts
        self._valid = self._is_valid_or_unchecked(self.branch_flows) and self._is_valid_or_unchecked(self.buses) \
                      and self._is_valid_or_unchecked(self.generators) and self._is_valid_or_unchecked(self.svcs) \
                      and self._is_valid_or_unchecked(self.shunts) and self._is_valid_or_unchecked(self.twts) \
                      and self._is_valid_or_unchecked(self.t3wts)

    @staticmethod
    def _is_valid_or_unchecked(df: _DataFrame) -> bool:
        return df is None or df['validated'].all()

    @property
    def branch_flows(self) -> _DataFrame:
        """
        Validation results for branch flows.
        """
        return self._branch_flows

    @property
    def buses(self) -> _DataFrame:
        """
        Validation results for buses.
        """
        return self._buses

    @property
    def generators(self) -> _DataFrame:
        """
        Validation results for generators.
        """
        return self._generators

    @property
    def svcs(self) -> _DataFrame:
        """
        Validation results for SVCs.
        """
        return self._svcs

    @property
    def shunts(self) -> _DataFrame:
        """
        Validation results for shunts.
        """
        return self._shunts

    @property
    def twts(self) -> _DataFrame:
        """
        Validation results for two winding transformers.
        """
        return self._twts

    @property
    def t3wts(self) -> _DataFrame:
        """
        Validation results for three winding transformers.
        """
        return self._t3wts

    @property
    def valid(self):
        """
        True if all checked data is valid.
        """
        return self._valid


def run_validation(network: _Network, validation_types: _List[ValidationType] = None) -> ValidationResult:
    """
    Checks that the network data are consistent with AC loadflow equations.

    Args:
        network: The network to be checked.
        validation_types: The types of data to be checked. If None, all types will be checked.

    Returns:
        The validation result.
    """
    if validation_types is None:
        validation_types = ValidationType.ALL
    res_by_type = {}
    for validation_type in validation_types:
        series_array = _pypowsybl.run_load_flow_validation(network._handle, validation_type)
        res_by_type[validation_type] = _create_data_frame_from_series_array(series_array)

    return ValidationResult(buses=res_by_type.get(ValidationType.BUSES, None),
                            branch_flows=res_by_type.get(ValidationType.FLOWS, None),
                            generators=res_by_type.get(ValidationType.GENERATORS, None),
                            svcs=res_by_type.get(ValidationType.SVCS, None),
                            shunts=res_by_type.get(ValidationType.SHUNTS, None),
                            twts=res_by_type.get(ValidationType.TWTS, None),
                            t3wts=res_by_type.get(ValidationType.TWTS3W, None))


