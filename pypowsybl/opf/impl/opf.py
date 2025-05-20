import logging

import pyoptinterface as poi

from pypowsybl.network import Network
from pypowsybl.opf.impl.bounds.bus_voltage_bounds import BusVoltageBounds
from pypowsybl.opf.impl.bounds.slack_bus_angle_bounds import SlackBusAngleBounds
from pypowsybl.opf.impl.bounds.vsc_cs_power_bounds import VscCsPowerBounds
from pypowsybl.opf.impl.constraints.branch_flow_constraints import BranchFlowConstraints
from pypowsybl.opf.impl.constraints.generator_power_bounds import GeneratorPowerBounds
from pypowsybl.opf.impl.constraints.hvdc_line_constraints import HvdcLineConstraints
from pypowsybl.opf.impl.constraints.power_balance_constraints import PowerBalanceConstraints
from pypowsybl.opf.impl.constraints.shunt_flow_constraints import ShuntFlowConstraints
from pypowsybl.opf.impl.constraints.static_var_compensator_reactive_limits_constraints import \
    StaticVarCompensatorReactiveLimitsConstraints
from pypowsybl.opf.impl.model.cost_function import MinimizeAgainstReferenceCostFunction
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.opf_model import OpfModel
from pypowsybl.opf.impl.parameters import OptimalPowerFlowParameters

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
                           VscCsPowerBounds()]
        constraints = [BranchFlowConstraints(),
                       ShuntFlowConstraints(),
                       StaticVarCompensatorReactiveLimitsConstraints(),
                       HvdcLineConstraints(),
                       PowerBalanceConstraints()]
        cost_function = MinimizeAgainstReferenceCostFunction()
        model_parameters = ModelParameters(parameters.reactive_bounds_reduction,
                                           parameters.twt_split_shunt_admittance)
        opf_model = OpfModel.build(network_cache, model_parameters, variable_bounds, constraints, cost_function)

        logger.info("Starting optimization...")
        opf_model.model.set_model_attribute(poi.ModelAttribute.Silent, True)
        opf_model.model.optimize()
        status = opf_model.model.get_model_attribute(poi.ModelAttribute.TerminationStatus)
        logger.info(f"Optimization ends with status {status}")

        # for debugging
        opf_model.analyze_violations()

        # update network
        opf_model.update_network()

        network_cache.network.per_unit = False # FIXME design to improve

        return status == poi.TerminationStatusCode.LOCALLY_SOLVED


def run_ac(network: Network, parameters: OptimalPowerFlowParameters = OptimalPowerFlowParameters()) -> bool:
    opf = OptimalPowerFlow(network)
    return opf.run(parameters)
