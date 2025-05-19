import logging

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.function_context import FunctionContext
from pypowsybl.opf.impl.model.parameters import OptimalPowerFlowParameters
from pypowsybl.opf.impl.model.variable_bounds import VariableBounds
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.model.network_cache import NetworkCache

logger = logging.getLogger(__name__)

class Model:
    def __init__(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: VariableContext):
        self._network_cache = network_cache
        self._model = model
        self._variable_context = variable_context

    @property
    def network_cache(self) -> NetworkCache:
        return self._network_cache

    @property
    def model(self) -> ipopt.Model:
        return self._model

    @property
    def variable_context(self) -> VariableContext:
        return self._variable_context

    @classmethod
    def build(cls, network_cache: NetworkCache, parameters: OptimalPowerFlowParameters,
              variable_bounds: list[VariableBounds], constraints: list[Constraints]) -> 'Model':
        logger.info("Building model...")

        model = ipopt.Model()

        # create variables
        variable_context = VariableContext.build(network_cache, model)

        # variable bounds
        for variable_bounds in variable_bounds:
            variable_bounds.add(parameters, network_cache, variable_context, model)

        # register functions
        function_context = FunctionContext.build(model)

        # constraints
        for constraint in constraints:
            constraint.add(parameters, network_cache, variable_context, function_context, model)

        # cost function
        logger.debug(f"Using cost function: '{parameters.cost_function.name}'")
        cost = parameters.cost_function.create(network_cache, variable_context)
        model.set_objective(cost)

        return Model(network_cache, model, variable_context)

    def update_network(self):
        self.variable_context.update_network(self.network_cache, self.model)

    def analyze_violations(self) -> None:
        # check voltage bounds
        for bus_num, (bus_id, row) in enumerate(self.network_cache.buses.iterrows()):
            v = self.model.get_value(self.variable_context.v_vars[bus_num])
            v_bounds = Bounds.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit)
            if not v_bounds.contains(v):
                logger.error(f"Voltage magnitude violation: bus '{bus_id}' (num={bus_num}) {v} not in {v_bounds}")

        # check generator limits
        for gen_num, (gen_id, row) in enumerate(self.network_cache.generators.iterrows()):
            if row.bus_id:
                gen_p_index = self.variable_context.gen_p_num_2_index[gen_num]
                p = self.model.get_value(self.variable_context.gen_p_vars[gen_p_index])

                p_bounds = Bounds(row.min_p, row.max_p).mirror()
                if not p_bounds.contains(p):
                    logger.error(f"Generator active power violation: generator '{gen_id}' (num={gen_num}) {p} not in [{-row.max_p}, {-row.min_p}]")

                gen_q_index = self.variable_context.gen_q_num_2_index[gen_num]
                if gen_q_index != -1: # valid
                    q = self.model.get_value(self.variable_context.gen_q_vars[gen_q_index])
                    q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                    if not q_bounds.contains(q):
                        logger.error(f"Generator reactive power violation: generator '{gen_id}' (num={gen_num}) {q} not in {q_bounds}")
