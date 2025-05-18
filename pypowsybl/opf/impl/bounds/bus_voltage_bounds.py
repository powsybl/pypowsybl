import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.model.ac_variable_bounds import AcVariableBounds
from pypowsybl.opf.impl.model.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)


class BusVoltageBounds(AcVariableBounds):
    def add(self, parameters: AcOptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: AcVariableContext, model: ipopt.Model):
        for bus_num, row in enumerate(network_cache.buses.itertuples()):
            v_bounds = Bounds.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit)
            logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to bus '{row.Index}' (num={bus_num})'")
            model.set_variable_bounds(variable_context.v_vars[bus_num],
                                      *Bounds.fix(row.Index, v_bounds.min_value, v_bounds.max_value))
            model.set_variable_start(variable_context.v_vars[bus_num], 1.0)
