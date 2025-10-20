import pyoptinterface as poi
from pandas import DataFrame
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.cost_function import CostFunction
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class MinimizeAgainstReferenceCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimize against reference')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()

        for gen_num, gen_row in enumerate(network_cache.generators.itertuples(index=False)):
            if gen_row.bus_id:
                gen_p_expr = poi.ExprBuilder()
                gen_p_expr += variable_context.gen_p_vars[gen_num]
                gen_p_expr += gen_row.target_p
                cost += gen_p_expr * gen_p_expr
                if gen_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(gen_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += (v_var - gen_row.target_v) * (v_var - gen_row.target_v)

        for bat_num, bat_row in enumerate(network_cache.batteries.itertuples(index=False)):
            if bat_row.bus_id:
                bat_p_expr = poi.ExprBuilder()
                bat_p_expr += variable_context.bat_p_vars[bat_num]
                bat_p_expr += bat_row.target_p
                cost += bat_p_expr * bat_p_expr
                if bat_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(bat_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += (v_var - bat_row.target_v) * (v_var - bat_row.target_v)

        for vsc_cs_num, vsc_cs_row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if vsc_cs_row.bus_id:
                if NetworkCache.is_rectifier(vsc_cs_row.Index, vsc_cs_row):
                    vsc_cs_p_expr = poi.ExprBuilder()
                    vsc_cs_p_expr += variable_context.vsc_cs_p_vars[vsc_cs_num]
                    vsc_cs_p_expr -= vsc_cs_row.target_p
                    cost += vsc_cs_p_expr * vsc_cs_p_expr
                if vsc_cs_row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(vsc_cs_row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += (v_var - vsc_cs_row.target_v) * (v_var - vsc_cs_row.target_v)

        return cost
