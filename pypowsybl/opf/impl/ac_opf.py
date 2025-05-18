import logging

import pyoptinterface as poi

from pypowsybl.network import Network
from pypowsybl.opf.impl.bounds.ac_vsc_cs_power_bounds import AcVscCsPowerBounds
from pypowsybl.opf.impl.bounds.bus_voltage_bounds import BusVoltageBounds
from pypowsybl.opf.impl.bounds.slack_bus_angle_bounds import SlackBusAngleBounds
from pypowsybl.opf.impl.constraints.ac_branch_flow_constraints import AcBranchFlowConstraints
from pypowsybl.opf.impl.constraints.ac_generator_power_bounds import AcGeneratorPowerBounds
from pypowsybl.opf.impl.constraints.ac_hvdc_line_constraints import AcHvdcLineConstraints
from pypowsybl.opf.impl.constraints.ac_power_balance_constraints import AcPowerBalanceConstraints
from pypowsybl.opf.impl.constraints.shunt_flow_constraints import ShuntFlowConstraints
from pypowsybl.opf.impl.constraints.static_var_compensator_reactive_limits_constraints import \
    StaticVarCompensatorReactiveLimitsConstraints
from pypowsybl.opf.impl.model.ac_model import AcModel
from pypowsybl.opf.impl.model.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache

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
class AcOptimalPowerFlow:
    def __init__(self, network: Network) -> None:
        self._network = network

    def run(self, parameters: AcOptimalPowerFlowParameters) -> bool:
        network_cache = NetworkCache(self._network)

        variable_bounds = [BusVoltageBounds(),
                           SlackBusAngleBounds(),
                           AcGeneratorPowerBounds(),
                           AcVscCsPowerBounds()]
        constraints = [AcBranchFlowConstraints(),
                       ShuntFlowConstraints(),
                       StaticVarCompensatorReactiveLimitsConstraints(),
                       AcHvdcLineConstraints(),
                       AcPowerBalanceConstraints()]
        ac_model = AcModel.build(network_cache, parameters, variable_bounds, constraints)

        logger.info("Starting optimization...")
        ac_model.model.optimize()
        status = ac_model.model.get_model_attribute(poi.ModelAttribute.TerminationStatus)
        logger.info(f"Optimization ends with status {status}")

        # for debugging
        ac_model.analyze_violations()

        # update network
        ac_model.update_network()

        network_cache.network.per_unit = False # FIXME design to improve

        return status == poi.TerminationStatusCode.LOCALLY_SOLVED


def run_ac(network: Network, parameters: AcOptimalPowerFlowParameters = AcOptimalPowerFlowParameters()) -> bool:
    opf = AcOptimalPowerFlow(network)
    return opf.run(parameters)
