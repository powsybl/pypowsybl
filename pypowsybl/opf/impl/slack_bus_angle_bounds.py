import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.ac_variable_bounds import AcVariableBounds
from pypowsybl.opf.impl.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class SlackBusAngleBounds(AcVariableBounds):
    def add(self, parameters: AcOptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: AcVariableContext, model: ipopt.Model):
        # slack bus angle forced to 0
        slack_bus_id = network_cache.slack_terminal.iloc[0].bus_id if len(network_cache.slack_terminal) > 0 else network_cache.buses.iloc[0].name
        slack_bus_num = network_cache.buses.index.get_loc(slack_bus_id)
        model.set_variable_bounds(variable_context.ph_vars[slack_bus_num], 0.0, 0.0)
        logger.log(TRACE_LEVEL, f"Angle reference is at bus '{slack_bus_id}' (num={slack_bus_num})")
