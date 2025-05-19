import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.parameters import OptimalPowerFlowParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class VscCsPowerBounds(VariableBounds):
    def add(self, parameters: OptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        # VSC converter station active and reactive power bounds
        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if row.bus_id:
                p_bounds = Bounds(-row.max_p, row.max_p).mirror()
                logger.log(TRACE_LEVEL,
                           f"Add active power bounds {p_bounds} to VSC converter station '{row.Index}' (num={vsc_cs_num})")
                model.set_variable_bounds(variable_context.vsc_cs_p_vars[vsc_cs_num],
                                          *Bounds.fix(row.Index, p_bounds.min_value, p_bounds.max_value))

                q_bounds = Bounds.get_generator_reactive_power_bounds(row).reduce(
                    parameters.reactive_bounds_reduction).mirror()
                logger.log(TRACE_LEVEL,
                           f"Add reactive power bounds {q_bounds} to VSC converter station '{row.Index}' (num={vsc_cs_num})")
                if abs(q_bounds.max_value - q_bounds.min_value) < 1.0 / network_cache.network.nominal_apparent_power:
                    logger.error(
                        f"Too small reactive power bounds {q_bounds} for VSC converter station '{row.Index}' (num={vsc_cs_num})")
                model.set_variable_bounds(variable_context.vsc_cs_q_vars[vsc_cs_num],
                                          *Bounds.fix(row.Index, q_bounds.min_value, q_bounds.max_value))
