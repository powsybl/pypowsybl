from abc import ABC, abstractmethod

import pyoptinterface as poi
from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class CostFunction(ABC):
    def __init__(self, name):
        self._name = name

    @property
    def name(self):
        return self._name

    @abstractmethod
    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        pass


class MinimalActivePowerCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimal active power')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for gen_num in range(len(variable_context.gen_p_vars)):
            a, b, c = 0, 1.0, 0  # TODO
            cost += a * variable_context.gen_p_vars[gen_num] * variable_context.gen_p_vars[gen_num] + b * variable_context.gen_p_vars[gen_num] + c
        return cost


class MinimalLossesCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimal losses power')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for branch_index in range(len(variable_context.closed_branch_p1_vars)):
            cost += variable_context.closed_branch_p1_vars[branch_index] - variable_context.closed_branch_p2_vars[branch_index]
        return cost


class MinimizeAgainstReferenceCostFunction(CostFunction):
    def __init__(self):
        super().__init__('Minimize against reference')

    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        cost = poi.ExprBuilder()
        for gen_num, row in enumerate(network_cache.generators.itertuples(index=False)):
            if row.bus_id:
                gen_p_expr = poi.ExprBuilder()
                gen_p_expr += variable_context.gen_p_vars[gen_num]
                gen_p_expr += row.target_p
                cost += gen_p_expr * gen_p_expr
                if row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += (v_var - row.target_v) * (v_var - row.target_v)

        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if row.bus_id:
                if NetworkCache.is_rectifier(row):
                    vsc_cs_p_expr = poi.ExprBuilder()
                    vsc_cs_p_expr += variable_context.vsc_cs_p_vars[vsc_cs_num]
                    vsc_cs_p_expr -= row.target_p
                    cost += vsc_cs_p_expr * vsc_cs_p_expr
                if row.voltage_regulator_on:
                    bus_num = network_cache.buses.index.get_loc(row.bus_id)
                    v_var = variable_context.v_vars[bus_num]
                    cost += (v_var - row.target_v) * (v_var - row.target_v)

        return cost
