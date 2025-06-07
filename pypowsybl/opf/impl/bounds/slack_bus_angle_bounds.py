import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class SlackBusAngleBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        # slack bus angle forced to 0
        slack_bus_id = network_cache.slack_terminal.iloc[0].bus_id if len(network_cache.slack_terminal) > 0 else network_cache.buses.iloc[0].name
        slack_bus_num = network_cache.buses.index.get_loc(slack_bus_id)
        model.set_variable_bounds(variable_context.ph_vars[slack_bus_num], 0.0, 0.0)
        logger.info(f"Angle reference is at bus '{slack_bus_id}' (num={slack_bus_num})")
