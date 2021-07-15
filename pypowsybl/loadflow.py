#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
from _pypowsybl import LoadFlowParameters as Parameters
from _pypowsybl import LoadFlowComponentStatus as ComponentStatus
from _pypowsybl import LoadFlowComponentResult as ComponentResult
from _pypowsybl import VoltageInitMode
from _pypowsybl import BalanceType
from _pypowsybl import ConnectedComponentMode
from pypowsybl.network import Network
from pypowsybl.util import create_data_frame_from_series_array
from _pypowsybl import ValidationType

Parameters.__repr__ = lambda self: f"{self.__class__.__name__}("\
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

ComponentResult.__repr__ = lambda self: f"{self.__class__.__name__}("\
                                        f"connected_component_num={self.connected_component_num!r}"\
                                        f", synchronous_component_num={self.synchronous_component_num!r}"\
                                        f", status={self.status.name}"\
                                        f", iteration_count={self.iteration_count!r}"\
                                        f", slack_bus_id={self.slack_bus_id!r}"\
                                        f", slack_bus_active_power_mismatch={self.slack_bus_active_power_mismatch!r}"\
                                        f")"


def run_ac(network: Network, parameters: Parameters = Parameters(), provider = 'OpenLoadFlow'):
    return _pypowsybl.run_load_flow(network._handle, False, parameters, provider)


def run_dc(network: Network, parameters: Parameters = Parameters(), provider = 'OpenLoadFlow'):
    return _pypowsybl.run_load_flow(network._handle, True, parameters, provider)


def run_validation(network: Network, validation_types = []):
    result = ValidationResult()
    for validation_type in validation_types:
        series_array = _pypowsybl.run_load_flow_validation(network._handle, validation_type)
        value = create_data_frame_from_series_array(series_array)
        if validation_type == ValidationType.BUSES:
            result.buses = value
        elif validation_type == ValidationType.GENERATORS:
            result.generators = value
        elif validation_type == ValidationType.FLOWS:
            result.branch_flows = value
        elif validation_type == ValidationType.SVCS:
            result.svcs = value
        elif validation_type == ValidationType.SHUNTS:
            result.shunts = value
        elif validation_type == ValidationType.TWTS:
            result.twts = value
        elif validation_type == ValidationType.TWTS3W:
            result.twt3ws = value
        else:
            raise Exception(validation_type + " not support")
    return result


class ValidationResult:

    def __init__(self):
        self._branch_flows = None
        self._buses = None
        self._generators = None
        self._svcs = None
        self._shunts = None
        self._twts = None
        self._twt3ws = None
        self._valid = True

    @property
    def branch_flows(self):
        return self._branch_flows

    @branch_flows.setter
    def branch_flows(self, value):
        self._branch_flows = value
        self._valid &= all(value["validated"].tolist())

    @property
    def buses(self):
        return self._buses

    @buses.setter
    def buses(self, value):
        self._buses = value
        self._valid &= all(value["validated"].tolist())

    @property
    def generators(self):
        return self._generators

    @generators.setter
    def generators(self, value):
        self._generators = value
        self._valid &= all(value["validated"].tolist())

    @property
    def svcs(self):
        return self._svcs

    @svcs.setter
    def svcs(self, value):
        self._svcs = value
        self._valid &= all(value["validated"].tolist())

    @property
    def shunts(self):
        return self._shunts

    @shunts.setter
    def shunts(self, value):
        self._shunts = value
        self._valid &= all(value["validated"].tolist())
        
    @property
    def twts(self):
        return self._twts

    @twts.setter
    def twts(self, value):
        self._twts = value
        self._valid &= all(value["validated"].tolist())
        
    @property
    def twt3ws(self):
        return self._twt3ws

    @twt3ws.setter
    def twt3ws(self, value):
        self._twt3ws = value
        self._valid &= all(value["validated"].tolist())

    @property
    def valid(self):
        return self._valid
