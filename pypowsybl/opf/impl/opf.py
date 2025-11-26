import logging
import time

import pyoptinterface as poi
from pypowsybl._pypowsybl import ElementType

from pypowsybl.network import Network
from pypowsybl.opf.impl.bounds.battery_power_bounds import BatteryPowerBounds
from pypowsybl.opf.impl.bounds.bus_voltage_bounds import BusVoltageBounds
from pypowsybl.opf.impl.bounds.dangling_line_voltage_bounds import DanglingLineVoltageBounds
from pypowsybl.opf.impl.bounds.generator_power_bounds import GeneratorPowerBounds
from pypowsybl.opf.impl.bounds.slack_bus_angle_bounds import SlackBusAngleBounds
from pypowsybl.opf.impl.bounds.transformer_3w_middle_voltage_bounds import Transformer3wMiddleVoltageBounds
from pypowsybl.opf.impl.bounds.vsc_cs_power_bounds import VscCsPowerBounds
from pypowsybl.opf.impl.bounds.dc_node_voltage_bounds import DcNodeVoltageBounds
from pypowsybl.opf.impl.bounds.dc_line_current_bounds import DcLineCurrentBounds
from pypowsybl.opf.impl.bounds.voltage_source_converter_power_bounds import VoltageSourceConverterPowerBounds
from pypowsybl.opf.impl.constraints.branch_flow_constraints import BranchFlowConstraints
from pypowsybl.opf.impl.constraints.current_limit_constraints import CurrentLimitConstraints
from pypowsybl.opf.impl.constraints.dangling_line_flow_constraints import DanglingLineFlowConstraints
from pypowsybl.opf.impl.constraints.hvdc_line_constraints import HvdcLineConstraints
from pypowsybl.opf.impl.constraints.power_balance_constraints import PowerBalanceConstraints
from pypowsybl.opf.impl.constraints.shunt_flow_constraints import ShuntFlowConstraints
from pypowsybl.opf.impl.constraints.static_var_compensator_reactive_limits_constraints import \
    StaticVarCompensatorReactiveLimitsConstraints
from pypowsybl.opf.impl.constraints.transformer_3w_flow_constraints import Transformer3wFlowConstraints
from pypowsybl.opf.impl.constraints.dc_line_constraints import DcLineConstraints
from pypowsybl.opf.impl.constraints.voltage_source_converter_constraints import VoltageSourceConverterConstraints
from pypowsybl.opf.impl.constraints.dc_current_balance_constraints import DcCurrentBalanceConstraints
from pypowsybl.opf.impl.constraints.dc_ground_constraints import DcGroundConstraints
from pypowsybl.opf.impl.costs.minimize_against_reference_cost_function import MinimizeAgainstReferenceCostFunction
from pypowsybl.opf.impl.costs.redispatching_cost_function import RedispatchingCostFunction
from pypowsybl.opf.impl.costs.minimize_dc_losses import MinimizeDcLossesFunction
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.opf_model import OpfModel
from pypowsybl.opf.impl.network_statistics import NetworkStatistics
from pypowsybl.opf.impl.parameters import OptimalPowerFlowParameters, OptimalPowerFlowMode

logger = logging.getLogger(__name__)


# pip install pyoptinterface llvmlite tccbox
#
# git clone https://github.com/coin-or-tools/ThirdParty-Mumps.git
# cd ThirdParty-Mumps
# ./get.Mumps
# ./configure --prefix $HOME/mumps
# make -j 8
# make install
#
# git clone https://github.com/coin-or/Ipopt
# cd Ipopt/
# ./configure --prefix $HOME/ipopt --with-mumps-cflags="-I$HOME/mumps/include/coin-or/mumps/" --with-mumps-lflags="-L$HOME/mumps/lib -lcoinmumps"
# make -j 8
# make install
#
# export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/mumps/lib:$HOME/ipopt/lib
#
class OptimalPowerFlow:
    def __init__(self, network: Network) -> None:
        self._network = network

    def run(self, parameters: OptimalPowerFlowParameters) -> bool:
        network_cache = NetworkCache(self._network)

        variable_bounds = [BusVoltageBounds(),
                           SlackBusAngleBounds(),
                           GeneratorPowerBounds(),
                           BatteryPowerBounds(),
                           VscCsPowerBounds(),
                           DanglingLineVoltageBounds(),
                           Transformer3wMiddleVoltageBounds(),
                           VoltageSourceConverterPowerBounds(),
                           DcNodeVoltageBounds(),
                           DcLineCurrentBounds()]
        constraints: list[Constraints] = [BranchFlowConstraints(),
                                          ShuntFlowConstraints(),
                                          StaticVarCompensatorReactiveLimitsConstraints(),
                                          HvdcLineConstraints(),
                                          PowerBalanceConstraints(),
                                          DanglingLineFlowConstraints(),
                                          Transformer3wFlowConstraints(),
                                          DcLineConstraints(),
                                          VoltageSourceConverterConstraints(),
                                          DcCurrentBalanceConstraints(),
                                          DcGroundConstraints()]
        if parameters.mode == OptimalPowerFlowMode.REDISPATCHING:
            constraints.append(CurrentLimitConstraints())
            cost_function = RedispatchingCostFunction(1.0, 1.0, 1.0)
        elif parameters.mode == OptimalPowerFlowMode.LOADFLOW:
            cost_function = MinimizeAgainstReferenceCostFunction()
        else:
            cost_function = MinimizeDcLossesFunction()
        model_parameters = ModelParameters(parameters.reactive_bounds_reduction,
                                           parameters.twt_split_shunt_admittance,
                                           Bounds(parameters.default_voltage_bounds[0], parameters.default_voltage_bounds[1]))
        opf_model = OpfModel.build(network_cache, model_parameters, variable_bounds, constraints, cost_function)

        network_stats = NetworkStatistics(network_cache)
        network_stats.add(ElementType.GENERATOR, 'target_v')
        network_stats.add(ElementType.BATTERY, 'target_v')
        network_stats.add(ElementType.VSC_CONVERTER_STATION, 'target_v')
        network_stats.add(ElementType.GENERATOR, 'target_p')
        network_stats.add(ElementType.BATTERY, 'target_p')
        network_stats.add(ElementType.VSC_CONVERTER_STATION, 'target_p')

        logger.info("Starting optimization...")
        start = time.perf_counter()

        opf_model.model.set_model_attribute(poi.ModelAttribute.Silent, False)
        opf_model.model.optimize()
        status = opf_model.model.get_model_attribute(poi.ModelAttribute.TerminationStatus)

        logger.info(f"Optimization ends with status {status} in {time.perf_counter() - start:.3f} seconds.")

        # for debugging
        opf_model.analyze_violations(model_parameters)

        # update network
        opf_model.update_network()

        network_stats.print()

        network_cache.network.per_unit = False # FIXME design to improve

        return status == poi.TerminationStatusCode.LOCALLY_SOLVED


def run_ac(network: Network, parameters: OptimalPowerFlowParameters = OptimalPowerFlowParameters()) -> bool:
    opf = OptimalPowerFlow(network)
    return opf.run(parameters)
