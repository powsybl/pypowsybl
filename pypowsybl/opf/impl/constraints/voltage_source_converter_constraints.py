# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pyoptinterface as poi
from pyoptinterface import ipopt, nl
from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext

class VoltageSourceConverterConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache, variable_context: VariableContext,
            model: ipopt.Model) -> None:
        for converter_num, converter_row in enumerate(network_cache.voltage_source_converters.itertuples(index=False)):
            with nl.graph():
                dc_node1_id, dc_node2_id, bus1_id = converter_row.dc_node1_id, converter_row.dc_node2_id, converter_row.bus1_id
                voltage_regulator_on, control_mode = converter_row.voltage_regulator_on, converter_row.control_mode
                target_p, target_q, target_v_dc, target_v_ac = (converter_row.target_p, converter_row.target_q,
                                                                converter_row.target_v_dc, converter_row.target_v_ac)
                idle_loss, switching_loss, resistive_loss = (converter_row.idle_loss, converter_row.switching_loss,
                                                             converter_row.resistive_loss)

                dc_node1_num = network_cache.dc_nodes.index.get_loc(dc_node1_id)
                dc_node2_num = network_cache.dc_nodes.index.get_loc(dc_node2_id)
                bus1_num = network_cache.buses.index.get_loc(bus1_id)
                v1_var = variable_context.v_dc_vars[dc_node1_num]
                v2_var = variable_context.v_dc_vars[dc_node2_num]

                bus1_v_var = variable_context.v_vars[bus1_num]
                conv_q_var = variable_context.conv_q_vars[converter_num]
                conv_p_var = variable_context.conv_p_vars[converter_num]
                conv_i_var = variable_context.conv_i_vars[converter_num]

                if control_mode == "P_PCC":
                    # target_p follows the VSC AC-terminal load convention:
                    # positive P_AC means the converter absorbs active power from the AC network.
                    p_ac_eq = conv_p_var - target_p
                    model.add_linear_constraint(p_ac_eq, poi.Eq, 0.0)

                if control_mode == "V_DC":
                    v_dc_eq = v1_var - v2_var - target_v_dc
                    model.add_linear_constraint(v_dc_eq, poi.Eq, 0.0)

                if voltage_regulator_on:
                    bus1_v_eq = bus1_v_var - target_v_ac
                    model.add_linear_constraint(bus1_v_eq, poi.Eq, 0.0)
                else:
                    q_ac_eq = conv_q_var - target_q
                    model.add_linear_constraint(q_ac_eq, poi.Eq, 0.0)

                # Converter losses are modeled from the DC current magnitude:
                i_dc_var = nl.abs(conv_i_var)
                p_loss = idle_loss + switching_loss*i_dc_var + resistive_loss*nl.pow(i_dc_var,2)
                # Power balance: P_AC + P_DC = P_loss with P_DC = I * (V(dc_node1) - V(dc_node2)) and I oriented from dc_node1 to dc_node2.
                # Thus rectifier: P_AC > 0, P_DC < 0; inverter: P_AC < 0, P_DC > 0.
                conv_p_dc_eq = conv_p_var + conv_i_var * (v1_var - v2_var) - p_loss
                model.add_nl_constraint(conv_p_dc_eq, poi.Eq, 0.0)
                
                