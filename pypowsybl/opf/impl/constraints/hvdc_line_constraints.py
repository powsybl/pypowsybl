from pypowsybl.opf.impl.model.model import Model
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


def hvdc_line_losses(p, r, sb):
    return r * p * p / sb


def add_converter_losses(p, loss_factor):
    return p * (1.0 - loss_factor / 100.0)


class HvdcLineConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: Model) -> None:
        for hvdc_line_row in network_cache.hvdc_lines.itertuples(index=False):
            cs1_id, cs2_id, r, nominal_v = hvdc_line_row.converter_station1_id, hvdc_line_row.converter_station2_id, hvdc_line_row.r, hvdc_line_row.nominal_v
            cs1_num = network_cache.vsc_converter_stations.index.get_loc(cs1_id)
            cs2_num = network_cache.vsc_converter_stations.index.get_loc(cs2_id)
            cs1_row = network_cache.vsc_converter_stations.loc[cs1_id]
            cs2_row = network_cache.vsc_converter_stations.loc[cs2_id]
            cs1_index = variable_context.vsc_cs_num_2_index[cs1_num]
            cs2_index = variable_context.vsc_cs_num_2_index[cs2_num]
            p1_var = variable_context.vsc_cs_p_vars[cs1_index]
            p2_var = variable_context.vsc_cs_p_vars[cs2_index]

            loss_factor1 = cs1_row.loss_factor
            loss_factor2 = cs2_row.loss_factor
            sb = network_cache.network.nominal_apparent_power
            if NetworkCache.is_rectifier(cs1_id, hvdc_line_row):
                p_rectifier = add_converter_losses(p1_var, loss_factor1)
                p_eq = add_converter_losses(p_rectifier - hvdc_line_losses(p_rectifier, r, sb),
                                            loss_factor2) + p2_var
            else:
                p_rectifier = add_converter_losses(p2_var, loss_factor2)
                p_eq = add_converter_losses(p_rectifier - hvdc_line_losses(p_rectifier, r, sb),
                                            loss_factor1) + p1_var

            model.add_quadratic_constraint(p_eq == 0.0)
