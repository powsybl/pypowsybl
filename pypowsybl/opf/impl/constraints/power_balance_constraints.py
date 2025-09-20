import pyoptinterface as poi
from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.constraints import Constraints
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class PowerBalanceConstraints(Constraints):

    def add(self, parameters: ModelParameters, network_cache: NetworkCache,
            variable_context: VariableContext, model: ipopt.Model) -> None:
        for bus_expr in self.create_bus_expr_list(network_cache, variable_context):
            model.add_linear_constraint(bus_expr, poi.Eq, 0.0)


    class BusesBalance:
        def __init__(self, bus_count: int):
            self.p_gen = [[] for _ in range(bus_count)]
            self.q_gen = [[] for _ in range(bus_count)]
            self.p_load = [0.0 for _ in range(bus_count)]
            self.q_load = [0.0 for _ in range(bus_count)]

        @staticmethod
        def _to_expr(bus_gen: list, bus_load: list) -> list[poi.ExprBuilder]:
            return [poi.ExprBuilder() + poi.quicksum(gen) - load
                    for gen, load in zip(bus_gen, bus_load)]

        def to_expr(self) -> list[poi.ExprBuilder]:
            return self._to_expr(self.p_gen, self.p_load) + self._to_expr(self.q_gen, self.q_load)


    @classmethod
    def create_bus_expr_list(cls, network_cache, variable_context):
        buses_balance = cls.BusesBalance(len(network_cache.buses))

        # branches
        for branch_num, row in enumerate(network_cache.branches.itertuples(index=False)):
            branch_index = variable_context.branch_num_2_index[branch_num]
            if row.bus1_id and row.bus2_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                buses_balance.p_gen[bus1_num].append(variable_context.closed_branch_p1_vars[branch_index])
                buses_balance.q_gen[bus1_num].append(variable_context.closed_branch_q1_vars[branch_index])
                buses_balance.p_gen[bus2_num].append(variable_context.closed_branch_p2_vars[branch_index])
                buses_balance.q_gen[bus2_num].append(variable_context.closed_branch_q2_vars[branch_index])
            elif row.bus2_id:
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                buses_balance.p_gen[bus2_num].append(variable_context.open_side1_branch_p2_vars[branch_index])
                buses_balance.q_gen[bus2_num].append(variable_context.open_side1_branch_q2_vars[branch_index])
            elif row.bus1_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                buses_balance.p_gen[bus1_num].append(variable_context.open_side2_branch_p1_vars[branch_index])
                buses_balance.q_gen[bus1_num].append(variable_context.open_side2_branch_q1_vars[branch_index])

        # generators
        for gen_num, gen_row in enumerate(network_cache.generators.itertuples(index=False)):
            bus_id = gen_row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                gen_p_index = variable_context.gen_p_num_2_index[gen_num]
                gen_q_index = variable_context.gen_q_num_2_index[gen_num]
                buses_balance.p_gen[bus_num].append(variable_context.gen_p_vars[gen_p_index])
                if gen_q_index == -1:  # invalid
                    buses_balance.q_load[bus_num] += gen_row.target_q
                else:
                    buses_balance.q_gen[bus_num].append(variable_context.gen_q_vars[gen_q_index])

        # static var compensators
        for svc_num, row in enumerate(network_cache.static_var_compensators.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                svc_index = variable_context.svc_num_2_index[svc_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                buses_balance.q_gen[bus_num].append(variable_context.svc_q_vars[svc_index])

        # aggregated loads
        loads_sum = network_cache.loads.groupby("bus_id", as_index=False).agg({"p0": "sum", "q0": "sum"})
        for row in loads_sum.itertuples(index=False):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                buses_balance.p_load[bus_num] -= row.p0
                buses_balance.q_load[bus_num] -= row.q0

        # shunts
        for shunt_num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                shunt_index = variable_context.shunt_num_2_index[shunt_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                buses_balance.p_gen[bus_num].append(variable_context.shunt_p_vars[shunt_index])
                buses_balance.q_gen[bus_num].append(variable_context.shunt_q_vars[shunt_index])

        # VSC converter stations
        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                vsc_cs_index = variable_context.vsc_cs_num_2_index[vsc_cs_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                buses_balance.p_gen[bus_num].append(variable_context.vsc_cs_p_vars[vsc_cs_index])
                buses_balance.q_gen[bus_num].append(variable_context.vsc_cs_q_vars[vsc_cs_index])

        # dangling lines
        dl_buses_balance = cls.BusesBalance(len(variable_context.dl_v_vars))
        for dl_num, row in enumerate(network_cache.dangling_lines.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                dl_index = variable_context.dl_num_2_index[dl_num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                buses_balance.p_gen[bus_num].append(variable_context.dl_branch_p1_vars[dl_index])
                buses_balance.q_gen[bus_num].append(variable_context.dl_branch_q1_vars[dl_index])
                dl_buses_balance.p_gen[dl_index].append(variable_context.dl_branch_p2_vars[dl_index])
                dl_buses_balance.q_gen[dl_index].append(variable_context.dl_branch_q2_vars[dl_index])
                dl_buses_balance.p_load[dl_index] -= row.p0
                dl_buses_balance.q_load[dl_index] -= row.q0

        # 3 windings transformers
        t3_buses_balance = cls.BusesBalance(len(variable_context.t3_middle_v_vars))
        for t3_num, (t3_id, t3_row) in enumerate(network_cache.transformers_3w.iterrows()):
            t3_index = variable_context.t3_num_2_index[t3_num]
            if t3_row.bus1_id or t3_row.bus2_id or t3_row.bus3_id:
                leg1_index = variable_context.t3_leg1_num_2_index[t3_num]
                leg2_index = variable_context.t3_leg2_num_2_index[t3_num]
                leg3_index = variable_context.t3_leg3_num_2_index[t3_num]

                if t3_row.bus1_id:
                    bus1_num = network_cache.buses.index.get_loc(t3_row.bus1_id)
                    buses_balance.p_gen[bus1_num].append(variable_context.t3_closed_branch_p1_vars[leg1_index])
                    buses_balance.q_gen[bus1_num].append(variable_context.t3_closed_branch_q1_vars[leg1_index])
                    t3_buses_balance.p_gen[t3_index].append(variable_context.t3_closed_branch_p2_vars[leg1_index])
                    t3_buses_balance.q_gen[t3_index].append(variable_context.t3_closed_branch_q2_vars[leg1_index])
                else:
                    t3_buses_balance.p_gen[t3_index].append(variable_context.t3_open_side1_branch_p2_vars[leg1_index])
                    t3_buses_balance.q_gen[t3_index].append(variable_context.t3_open_side1_branch_q2_vars[leg1_index])

                if t3_row.bus2_id:
                    bus2_num = network_cache.buses.index.get_loc(t3_row.bus2_id)
                    buses_balance.p_gen[bus2_num].append(variable_context.t3_closed_branch_p1_vars[leg2_index])
                    buses_balance.q_gen[bus2_num].append(variable_context.t3_closed_branch_q1_vars[leg2_index])
                    t3_buses_balance.p_gen[t3_index].append(variable_context.t3_closed_branch_p2_vars[leg2_index])
                    t3_buses_balance.q_gen[t3_index].append(variable_context.t3_closed_branch_q2_vars[leg2_index])
                else:
                    t3_buses_balance.p_gen[t3_index].append(variable_context.t3_open_side1_branch_p2_vars[leg2_index])
                    t3_buses_balance.q_gen[t3_index].append(variable_context.t3_open_side1_branch_q2_vars[leg2_index])

                if t3_row.bus3_id:
                    bus3_num = network_cache.buses.index.get_loc(t3_row.bus3_id)
                    buses_balance.p_gen[bus3_num].append(variable_context.t3_closed_branch_p1_vars[leg3_index])
                    buses_balance.q_gen[bus3_num].append(variable_context.t3_closed_branch_q1_vars[leg3_index])
                    t3_buses_balance.p_gen[t3_index].append(variable_context.t3_closed_branch_p2_vars[leg3_index])
                    t3_buses_balance.q_gen[t3_index].append(variable_context.t3_closed_branch_q2_vars[leg3_index])
                else:
                    t3_buses_balance.p_gen[t3_index].append(variable_context.t3_open_side1_branch_p2_vars[leg3_index])
                    t3_buses_balance.q_gen[t3_index].append(variable_context.t3_open_side1_branch_q2_vars[leg3_index])

        return buses_balance.to_expr() + dl_buses_balance.to_expr() + t3_buses_balance.to_expr()
