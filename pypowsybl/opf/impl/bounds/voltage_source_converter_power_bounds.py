import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class VoltageSourceConverterPowerBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        # Voltage source converter active and reactive power bounds
        for converter_num, row in enumerate(network_cache.voltage_source_converters.itertuples()):
            #TODO implements max P, Q in converter core modelization
            max_p = 100000.0
            max_q = 100000.0
            p_bounds = Bounds(-max_p, max_p).mirror()
            logger.log(TRACE_LEVEL,
                       f"Add active power bounds {p_bounds} to converter '{row.Index}' (num={converter_num})")
            model.set_variable_bounds(variable_context.conv_p_vars[converter_num],
                                      *Bounds.fix(row.Index, p_bounds.min_value, p_bounds.max_value))

            q_bounds = Bounds(-max_q, max_q).mirror()
            logger.log(TRACE_LEVEL,
                       f"Add reactive power bounds {q_bounds} to converter '{row.Index}' (num={converter_num})")
            if abs(q_bounds.max_value - q_bounds.min_value) < 1.0 / network_cache.network.nominal_apparent_power:
                logger.error(
                    f"Too small reactive power bounds {q_bounds} for converter '{row.Index}' (num={converter_num})")
            model.set_variable_bounds(variable_context.conv_q_vars[converter_num],
                                      *Bounds.fix(row.Index, q_bounds.min_value, q_bounds.max_value))
