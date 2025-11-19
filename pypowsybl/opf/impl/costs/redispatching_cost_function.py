import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class RedispatchingCostFunction(CostFunction):
    def __init__(self, target_p_l1_weight=0.1, target_p_l2_weight=1.0, target_v_l2_weight=0.5):
        super().__init__('Redispatching')
        self._target_p_l1_weight = target_p_l1_weight
        self._target_p_l2_weight = target_p_l2_weight
        self._target_v_l2_weight = target_v_l2_weight

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for gen_num, gen_row in enumerate(network_cache.generators.itertuples(index=False)):
            if gen_row.bus_id:
                gen_p_expr = poi.ExprBuilder()
                gen_p_expr += variable_context.gen_p_vars[gen_num]
                gen_p_expr += gen_row.target_p
                cost += self._target_p_l2_weight * gen_p_expr * gen_p_expr
                cost += self._target_p_l1_weight * gen_p_expr
                if gen_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(gen_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += self._target_v_l2_weight * (v_var - gen_row.target_v) * (v_var - gen_row.target_v)

        for vsc_cs_num, vsc_cs_row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if vsc_cs_row.bus_id:
                if NetworkCache.is_rectifier(vsc_cs_row.Index, vsc_cs_row):
                    vsc_cs_p_expr = poi.ExprBuilder()
                    vsc_cs_p_expr += variable_context.vsc_cs_p_vars[vsc_cs_num]
                    vsc_cs_p_expr -= vsc_cs_row.target_p
                    cost += self._target_p_l2_weight * vsc_cs_p_expr * vsc_cs_p_expr
                    cost += self._target_p_l1_weight * vsc_cs_p_expr
                if vsc_cs_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(vsc_cs_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += self._target_v_l2_weight * (v_var - vsc_cs_row.target_v) * (v_var - vsc_cs_row.target_v)

        return cost
