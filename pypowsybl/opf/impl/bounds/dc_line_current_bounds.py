import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class DcLineCurrentBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        for dc_line_num, row in enumerate(network_cache.dc_lines.itertuples()):
            i_bounds = Bounds(-100, 100)
            logger.log(TRACE_LEVEL, f"Add current bounds {i_bounds} to dc line '{row.Index}' (num={dc_line_num})'")
            dc_line_index = variable_context.dc_line_num_2_index[dc_line_num]
            model.set_variable_bounds(variable_context.closed_dc_line_i1_vars[dc_line_index],
                                      *Bounds.fix(row.Index, i_bounds.min_value, i_bounds.max_value))
            model.set_variable_bounds(variable_context.closed_dc_line_i2_vars[dc_line_index],
                                      *Bounds.fix(row.Index, i_bounds.min_value, i_bounds.max_value))