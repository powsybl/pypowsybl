#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from _gridpy import LoadFlowParameters as Parameters
from _gridpy import LoadFlowComponentStatus as ComponentStatus
from _gridpy import LoadFlowComponentResult as ComponentResult
from _gridpy import VoltageInitMode
from _gridpy import BalanceType
from gridpy.network import Network


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
                                   f")"

ComponentResult.__repr__ = lambda self: f"{self.__class__.__name__}("\
                                        f"component_num={self.component_num!r}"\
                                        f", status={self.status.name}"\
                                        f", iteration_count={self.iteration_count!r}"\
                                        f", slack_bus_id={self.slack_bus_id!r}"\
                                        f", slack_bus_active_power_mismatch={self.slack_bus_active_power_mismatch!r}"\
                                        f")"


def run_ac(network: Network, parameters: Parameters = Parameters()):
    return _gridpy.run_load_flow(network.ptr, False, parameters)


def run_dc(network: Network, parameters: Parameters = Parameters()):
    return _gridpy.run_load_flow(network.ptr, True, parameters)
