import logging

import pyoptinterface as poi

from pypowsybl.network import Network
from pypowsybl.opf.impl.ac_model import AcModel
from pypowsybl.opf.impl.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.network_cache import NetworkCache

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

        ac_model = AcModel.build(network_cache, parameters)

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
