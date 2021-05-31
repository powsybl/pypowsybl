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
    return _pypowsybl.run_load_flow(network.ptr, False, parameters, provider)


def run_dc(network: Network, parameters: Parameters = Parameters(), provider = 'OpenLoadFlow'):
    return _pypowsybl.run_load_flow(network.ptr, True, parameters, provider)
