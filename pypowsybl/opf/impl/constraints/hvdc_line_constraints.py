from pyoptinterface import ipopt, nlfunc

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.function_context import FunctionContext
from pypowsybl.opf.impl.model.parameters import OptimalPowerFlowParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class HvdcLineConstraints(Constraints):
    def add(self, parameters: OptimalPowerFlowParameters, network_cache: NetworkCache,
            variable_context: VariableContext, function_context: FunctionContext, model: ipopt.Model) -> None:
        for row in network_cache.hvdc_lines.itertuples(index=False):
            cs1_id, cs2_id, r, nominal_v = row.converter_station1_id, row.converter_station2_id, row.r, row.nominal_v
            cs1_num = network_cache.vsc_converter_stations.index.get_loc(cs1_id)
            cs2_num = network_cache.vsc_converter_stations.index.get_loc(cs2_id)
            row_cs1 = network_cache.vsc_converter_stations.loc[cs1_id]
            row_cs2 = network_cache.vsc_converter_stations.loc[cs2_id]
            cs1_index = variable_context.vsc_cs_num_2_index[cs1_num]
            cs2_index = variable_context.vsc_cs_num_2_index[cs2_num]
            p1_var = variable_context.vsc_cs_p_vars[cs1_index]
            p2_var = variable_context.vsc_cs_p_vars[cs2_index]
            model.add_nl_constraint(
                function_context.dclf_index,
                vars=nlfunc.Vars(
                    p1=p1_var,
                    p2=p2_var,
                ),
                params=nlfunc.Params(
                    r=r,
                    nominal_v=nominal_v,
                    loss_factor1=row_cs1.loss_factor,
                    loss_factor2=row_cs2.loss_factor,
                    sb=network_cache.network.nominal_apparent_power
                ),
                eq=0.0,
            )

