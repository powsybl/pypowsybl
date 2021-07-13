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
from _pypowsybl import ElementType

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


def run_validation(network: Network, element_types = []):
    result = ValidationResult()
    for element_type in element_types:
        series_array = _pypowsybl.run_load_flow_validation(network._handle, element_type)
        value = create_data_frame_from_series_array(series_array)
        if element_type == ElementType.BUS:
            result.buses = value
        elif element_type == ElementType.GENERATOR:
            result.generators = value
        elif element_type == ElementType.BRANCH:
            result.branch_flows = value
        elif element_type == ElementType.STATIC_VAR_COMPENSATOR:
            result.svcs = value
        elif element_type == ElementType.SHUNT_COMPENSATOR:
            result.shunts = value
        elif element_type == ElementType.TWO_WINDINGS_TRANSFORMER:
            result.twts = value
        elif element_type == ElementType.THREE_WINDINGS_TRANSFORMER:
            result.twt3ws = value
        else:
            raise Exception(element_type + " not support")
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
