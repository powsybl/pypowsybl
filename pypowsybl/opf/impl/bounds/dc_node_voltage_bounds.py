import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)


class DcNodeVoltageBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        for dc_node_num, row in enumerate(network_cache.dc_nodes.itertuples()):
            #TODO add voltage limits in DC NODE core modelization
            low_voltage_limit = 0
            high_voltage_limit = 600
            v_bounds = Bounds.get_voltage_bounds(low_voltage_limit, high_voltage_limit)
            print(v_bounds.max_value)
            logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to dc_node '{row.Index}' (num={dc_node_num})'")
            model.set_variable_bounds(variable_context.v_dc_vars[dc_node_num],
                                      *Bounds.fix(row.Index, v_bounds.min_value, v_bounds.max_value))
            model.set_variable_start(variable_context.v_dc_vars[dc_node_num], 1.0)
