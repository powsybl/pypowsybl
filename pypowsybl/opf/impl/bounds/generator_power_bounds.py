import logging

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class GeneratorPowerBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model):
        # generator active and reactive power bounds
        for gen_num, row in enumerate(network_cache.generators.itertuples()):
            if row.bus_id:
                p_bounds = Bounds(row.min_p, row.max_p).mirror()
                logger.log(TRACE_LEVEL, f"Add active power bounds {p_bounds} to generator '{row.Index}' (num={gen_num})")
                gen_p_index = variable_context.gen_p_num_2_index[gen_num]
                model.set_variable_bounds(variable_context.gen_p_vars[gen_p_index], *Bounds.fix(row.Index, p_bounds.min_value, p_bounds.max_value))

                gen_q_index = variable_context.gen_q_num_2_index[gen_num]
                if gen_q_index != -1: # valid
                    q_bounds = Bounds.get_reactive_power_bounds(row).reduce(parameters.reactive_bounds_reduction).mirror()
                    logger.log(TRACE_LEVEL, f"Add reactive power bounds {q_bounds} to generator '{row.Index}' (num={gen_num})")
                    model.set_variable_bounds(variable_context.gen_q_vars[gen_q_index], *Bounds.fix(row.Index, q_bounds.min_value, q_bounds.max_value))
