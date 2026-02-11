import logging

from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)


class Transformer3wMiddleVoltageBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model):
        for t3_num, t3_row in enumerate(network_cache.transformers_3w.itertuples()):
            if t3_row.bus1_id or t3_row.bus2_id or t3_row.bus3_id:
                v_bounds = Bounds.get_voltage_bounds(None, None, parameters.default_voltage_bounds)
                logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to 3 windings transformer middle '{t3_row.Index}' (num={t3_num})'")
                t3_index = variable_context.t3_num_2_index[t3_num]
                model.set_variable_bounds(variable_context.t3_middle_v_vars[t3_index],
                                          *Bounds.fix(t3_row.Index, v_bounds.min_value, v_bounds.max_value))
                model.set_variable_start(variable_context.t3_middle_ph_vars[t3_index], 1.0)
