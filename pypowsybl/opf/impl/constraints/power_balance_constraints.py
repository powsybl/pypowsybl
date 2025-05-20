import pyoptinterface as poi
from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.function_context import FunctionContext
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class PowerBalanceConstraints(Constraints):
    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, function_context: FunctionContext, model: ipopt.Model) -> None:
        bus_count = len(network_cache.buses)
        bus_p_gen = [[] for _ in range(bus_count)]
        bus_q_gen = [[] for _ in range(bus_count)]
        bus_p_load = [0.0 for _ in range(bus_count)]
        bus_q_load = [0.0 for _ in range(bus_count)]

        # branches
        for branch_num, row in enumerate(network_cache.branches.itertuples(index=False)):
            branch_index = variable_context.branch_num_2_index[branch_num]
            if row.bus1_id and row.bus2_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                bus_p_gen[bus1_num].append(variable_context.closed_branch_p1_vars[branch_index])
                bus_q_gen[bus1_num].append(variable_context.closed_branch_q1_vars[branch_index])
                bus_p_gen[bus2_num].append(variable_context.closed_branch_p2_vars[branch_index])
                bus_q_gen[bus2_num].append(variable_context.closed_branch_q2_vars[branch_index])
            elif row.bus2_id:
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                bus_p_gen[bus2_num].append(variable_context.open_side1_branch_p2_vars[branch_index])
                bus_q_gen[bus2_num].append(variable_context.open_side1_branch_q2_vars[branch_index])
            elif row.bus1_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                bus_p_gen[bus1_num].append(variable_context.open_side2_branch_p1_vars[branch_index])
                bus_q_gen[bus1_num].append(variable_context.open_side2_branch_q1_vars[branch_index])

        # generators
        for gen_num, gen_row in enumerate(network_cache.generators.itertuples(index=False)):
            bus_id = gen_row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                gen_p_index = variable_context.gen_p_num_2_index[gen_num]
                gen_q_index = variable_context.gen_q_num_2_index[gen_num]
                bus_p_gen[bus_num].append(variable_context.gen_p_vars[gen_p_index])
                if gen_q_index == -1:  # invalid
                    bus_q_load[bus_num] += gen_row.target_q
                else:
                    bus_q_gen[bus_num].append(variable_context.gen_q_vars[gen_q_index])

        # static var compensators
        for svc_num, row in enumerate(network_cache.static_var_compensators.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                svc_index = variable_context.svc_num_2_index[svc_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_q_gen[bus_num].append(variable_context.svc_q_vars[svc_index])

        # aggregated loads
        loads_sum = network_cache.loads.groupby("bus_id", as_index=False).agg({"p0": "sum", "q0": "sum"})
        for row in loads_sum.itertuples(index=False):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_load[bus_num] -= row.p0
                bus_q_load[bus_num] -= row.q0

        # shunts
        for shunt_num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                shunt_index = variable_context.shunt_num_2_index[shunt_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_gen[bus_num].append(variable_context.shunt_p_vars[shunt_index])
                bus_q_gen[bus_num].append(variable_context.shunt_q_vars[shunt_index])

        # VSC converter stations
        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                vsc_cs_index = variable_context.vsc_cs_num_2_index[vsc_cs_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_gen[bus_num].append(variable_context.vsc_cs_p_vars[vsc_cs_index])
                bus_q_gen[bus_num].append(variable_context.vsc_cs_q_vars[vsc_cs_index])

        for bus_num in range(bus_count):
            bus_p_expr = poi.ExprBuilder()
            bus_p_expr += poi.quicksum(bus_p_gen[bus_num])
            bus_p_expr -= bus_p_load[bus_num]
            model.add_quadratic_constraint(bus_p_expr, poi.Eq, 0.0)

            bus_q_expr = poi.ExprBuilder()
            bus_q_expr += poi.quicksum(bus_q_gen[bus_num])
            bus_q_expr -= bus_q_load[bus_num]
            model.add_quadratic_constraint(bus_q_expr, poi.Eq, 0.0)
