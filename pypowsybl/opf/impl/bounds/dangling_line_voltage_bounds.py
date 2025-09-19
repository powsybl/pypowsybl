import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)


class DanglingLineVoltageBounds(VariableBounds):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model):
        for dl_num, row in enumerate(network_cache.dangling_lines.itertuples()):
            if row.bus_id:
                v_bounds = Bounds.get_voltage_bounds(None, None)
                logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to dangling line bus '{row.Index}' (num={dl_num})'")
                dl_index = variable_context.dl_num_2_index[dl_num]
                model.set_variable_bounds(variable_context.dl_v_vars[dl_index],
                                          *Bounds.fix(row.Index, v_bounds.min_value, v_bounds.max_value))
                model.set_variable_start(variable_context.dl_v_vars[dl_index], 1.0)
