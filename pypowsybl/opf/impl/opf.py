import logging
from dataclasses import dataclass
from math import hypot, atan2
from typing import Any

import pandas as pd
import pyoptinterface as poi
from pandas import DataFrame
from pyoptinterface import nlfunc, ipopt
from pyoptinterface._src.nleval_ext import FunctionIndex

from pypowsybl.network import Network
from pypowsybl.opf.impl.bounds import Bounds
from pypowsybl.opf.impl.network_cache import NetworkCache

logger = logging.getLogger(__name__)

TRACE_LEVEL = 5
logging.addLevelName(TRACE_LEVEL, "TRACE")


R2 = 1.0
A2 = 0.0
DEFAULT_V_BOUNDS = Bounds(0.9, 1.1)

def closed_branch_flow(vars, params):
    y, ksi, g1, b1, g2, b2, r1, a1 = (
        params.y,
        params.ksi,
        params.g1,
        params.b1,
        params.g2,
        params.b2,
        params.r1,
        params.a1
    )
    v1, v2, ph1, ph2, p1, q1, p2, q2 = (
        vars.v1,
        vars.v2,
        vars.ph1,
        vars.ph2,
        vars.p1,
        vars.q1,
        vars.p2,
        vars.q2,
    )

    sin_ksi = nlfunc.sin(ksi)
    cos_ksi = nlfunc.cos(ksi)
    theta1 = ksi - a1 + A2 - ph1 + ph2
    theta2 = ksi + a1 - A2 + ph1 - ph2
    sin_theta1 = nlfunc.sin(theta1)
    cos_theta1 = nlfunc.cos(theta1)
    sin_theta2 = nlfunc.sin(theta2)
    cos_theta2 = nlfunc.cos(theta2)

    p1_eq = r1 * v1 * (g1 * r1 * v1 + y * r1 * v1 * sin_ksi - y * R2 * v2 * sin_theta1) - p1
    q1_eq = r1 * v1 * (-b1 * r1 * v1 + y * r1 * v1 * cos_ksi - y * R2 * v2 * cos_theta1) - q1
    p2_eq = R2 * v2 * (g2 * R2 * v2 - y * r1 * v1 * sin_theta2 + y * R2 * v2 * sin_ksi) - p2
    q2_eq = R2 * v2 * (-b2 * R2 * v2 - y * r1 * v1 * cos_theta2 + y * R2 * v2 * cos_ksi) - q2

    return [p1_eq, q1_eq, p2_eq, q2_eq]


def open_side1_branch_flow(vars, params):
    y, ksi, g1, b1, g2, b2 = (
        params.y,
        params.ksi,
        params.g1,
        params.b1,
        params.g2,
        params.b2,
    )
    v2, ph2, p2, q2 = (
        vars.v2,
        vars.ph2,
        vars.p2,
        vars.q2,
    )

    sin_ksi = nlfunc.sin(ksi)
    cos_ksi = nlfunc.cos(ksi)

    shunt = (g1 + y * sin_ksi) * (g1 + y * sin_ksi) + (-b1 + y * cos_ksi) * (-b1 + y * cos_ksi)
    p2_eq = R2 * R2 * v2 * v2 * (g2 + y * y * g1 / shunt + (b1 * b1 + g1 * g1) * y * sin_ksi / shunt) - p2
    q2_eq = -R2 * R2 * v2 * v2 * (b2 + y * y * b1 / shunt - (b1 * b1 + g1 * g1) * y * cos_ksi / shunt) - q2

    return [p2_eq, q2_eq]


def open_side2_branch_flow(vars, params):
    y, ksi, g1, b1, g2, b2, r1, a1 = (
        params.y,
        params.ksi,
        params.g1,
        params.b1,
        params.g2,
        params.b2,
        params.r1,
        params.a1,
    )
    v1, ph1, p1, q1, = (
        vars.v1,
        vars.ph1,
        vars.p1,
        vars.q1,
    )

    sin_ksi = nlfunc.sin(ksi)
    cos_ksi = nlfunc.cos(ksi)

    shunt = (g2 + y * sin_ksi) * (g2 + y * sin_ksi) + (-b2 + y * cos_ksi) * (-b2 + y * cos_ksi)
    p1_eq = r1 * r1 * v1 * v1 * (g1 + y * y * g2 / shunt + (b2 * b2 + g2 * g2) * y * sin_ksi / shunt) - p1
    q1_eq = -r1 * r1 * v1 * v1 * (b1 + y * y * b2 / shunt - (b2 * b2 + g2 * g2) * y * cos_ksi / shunt) - q1

    return [p1_eq, q1_eq]


def shunt_flow(vars, params):
    g, b = (
        params.g,
        params.b
    )
    v, p, q = (
        vars.v,
        vars.p,
        vars.q
    )

    p_eq = -g * v * v - p
    q_eq = -b * v * v - q

    return [p_eq, q_eq]


@dataclass
class VariableContext:
    ph_vars: Any
    v_vars: Any
    gen_p_vars: Any
    gen_q_vars: Any
    shunt_p_vars: Any
    shunt_q_vars: Any
    closed_branch_p1_vars: Any
    closed_branch_q1_vars: Any
    closed_branch_p2_vars: Any
    closed_branch_q2_vars: Any
    open_side1_branch_p2_vars: Any
    open_side1_branch_q2_vars: Any
    open_side2_branch_p1_vars: Any
    open_side2_branch_q1_vars: Any
    branch_num_2_index: list[int]


class OptimalPowerFlowParameters:
    def __init__(self) -> None:
        self._reactive_bounds_reduction = 0.1

    @property
    def reactive_bounds_reduction(self) -> float:
        return self._reactive_bounds_reduction


# pip install pyoptinterface llvmlite tccbox
#
# git clone https://github.com/coin-or-tools/ThirdParty-Mumps.git
# cd ThirdParty-Mumps
# ./get.Mumps
# ./configure --prefix $HOME/mumps
# make -j 8
# make install
#
# git clone https://github.com/coin-or/Ipopt
# cd Ipopt/
# ./configure --prefix $HOME/ipopt --with-mumps-cflags="-I$HOME/mumps/include/coin-or/mumps/" --with-mumps-lflags="-L$HOME/mumps/lib -lcoinmumps"
# make -j 8
# make install
#
# export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/mumps/lib:$HOME/ipopt/lib
#
class OptimalPowerFlow:
    def __init__(self, network: Network) -> None:
        self._network = network

    @staticmethod
    def add_branch_constraint(branch_index: int, bus1_id: str, bus2_id: str, network_cache: NetworkCache, model,
                              cbf_index: FunctionIndex, o1bf_index: FunctionIndex, o2bf_index: FunctionIndex,
                              r: float, x: float, g1: float, b1: float, g2: float, b2: float, r1: float, a1: float,
                              variable_context: VariableContext):
        z = hypot(r, x)
        y = 1.0 / z
        ksi = atan2(r, x)

        if bus1_id and bus2_id:
            bus1_num = network_cache.buses.index.get_loc(bus1_id)
            bus2_num = network_cache.buses.index.get_loc(bus2_id)
            v1_var = variable_context.v_vars[bus1_num]
            v2_var = variable_context.v_vars[bus2_num]
            ph1_var = variable_context.ph_vars[bus1_num]
            ph2_var = variable_context.ph_vars[bus2_num]
            p1_var = variable_context.closed_branch_p1_vars[branch_index]
            q1_var = variable_context.closed_branch_q1_vars[branch_index]
            p2_var = variable_context.closed_branch_p2_vars[branch_index]
            q2_var = variable_context.closed_branch_q2_vars[branch_index]
            model.add_nl_constraint(
                cbf_index,
                vars=nlfunc.Vars(
                    v1=v1_var,
                    v2=v2_var,
                    ph1=ph1_var,
                    ph2=ph2_var,
                    p1=p1_var,
                    q1=q1_var,
                    p2=p2_var,
                    q2=q2_var
                ),
                params=nlfunc.Params(
                    y=y,
                    ksi=ksi,
                    g1=g1,
                    b1=b1,
                    g2=g2,
                    b2=b2,
                    r1=r1,
                    a1=a1
                ),
                eq=0.0,
            )
        elif bus2_id:
            bus2_num = network_cache.buses.index.get_loc(bus2_id)
            v2_var = variable_context.v_vars[bus2_num]
            ph2_var = variable_context.ph_vars[bus2_num]
            p2_var = variable_context.open_side1_branch_p2_vars[branch_index]
            q2_var = variable_context.open_side1_branch_q2_vars[branch_index]
            model.add_nl_constraint(
                o1bf_index,
                vars=nlfunc.Vars(
                    v2=v2_var,
                    ph2=ph2_var,
                    p2=p2_var,
                    q2=q2_var
                ),
                params=nlfunc.Params(
                    y=y,
                    ksi=ksi,
                    g1=g1,
                    b1=b1,
                    g2=g2,
                    b2=b2,
                ),
                eq=0.0,
            )
        elif bus1_id:
            bus1_num = network_cache.buses.index.get_loc(bus1_id)
            v1_var = variable_context.v_vars[bus1_num]
            ph1_var = variable_context.ph_vars[bus1_num]
            p1_var = variable_context.open_side2_branch_p1_vars[branch_index]
            q1_var = variable_context.open_side2_branch_q1_vars[branch_index]
            model.add_nl_constraint(
                o2bf_index,
                vars=nlfunc.Vars(
                    v1=v1_var,
                    ph1=ph1_var,
                    p1=p1_var,
                    q1=q1_var
                ),
                params=nlfunc.Params(
                    y=y,
                    ksi=ksi,
                    g1=g1,
                    b1=b1,
                    g2=g2,
                    b2=b2,
                    a1=a1,
                    r1=r1,
                ),
                eq=0.0,
            )

    @staticmethod
    def create_variable_context(network_cache: NetworkCache, model: ipopt.Model) -> VariableContext:
        branch_count = len(network_cache.lines) + len(network_cache.transformers)
        bus_count = len(network_cache.buses)
        gen_count = len(network_cache.generators)

        v_vars = model.add_variables(range(bus_count), name="v")
        ph_vars = model.add_variables(range(bus_count), name="ph")

        gen_p_vars = model.add_variables(range(gen_count), name="gen_p")
        gen_q_vars = model.add_variables(range(gen_count), name="gen_q")

        shunt_p_vars = model.add_variables(range(gen_count), name="shunt_p")
        shunt_q_vars = model.add_variables(range(gen_count), name="shunt_q")

        closed_branch_nums = []
        open_side1_branch_nums = []
        open_side2_branch_nums = []
        branch_num_2_index = [-1] * branch_count
        for branch_num, row in enumerate(network_cache.branches.itertuples(index=False)):
            if row.bus1_id and row.bus2_id:
                branch_num_2_index[branch_num] = len(closed_branch_nums)
                closed_branch_nums.append(branch_num)
            elif row.bus2_id:
                branch_num_2_index[branch_num] = len(open_side1_branch_nums)
                open_side1_branch_nums.append(branch_num)
            elif row.bus1_id:
                branch_num_2_index[branch_num] = len(open_side2_branch_nums)
                open_side2_branch_nums.append(branch_num)
        closed_branch_p1_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_p1')
        closed_branch_q1_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_q1')
        closed_branch_p2_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_p2')
        closed_branch_q2_vars = model.add_variables(range(len(closed_branch_nums)), name='closed_branch_q2')
        open_side1_branch_p2_vars = model.add_variables(range(len(open_side1_branch_nums)), name='open_side1_branch_p2')
        open_side1_branch_q2_vars = model.add_variables(range(len(open_side1_branch_nums)), name='open_side1_branch_q2')
        open_side2_branch_p1_vars = model.add_variables(range(len(open_side2_branch_nums)), name='open_side2_branch_p1')
        open_side2_branch_q1_vars = model.add_variables(range(len(open_side2_branch_nums)), name='open_side2_branch_q1')

        return VariableContext(ph_vars, v_vars,
                               gen_p_vars, gen_q_vars,
                               shunt_p_vars, shunt_q_vars,
                               closed_branch_p1_vars, closed_branch_q1_vars,
                               closed_branch_p2_vars, closed_branch_q2_vars,
                               open_side1_branch_p2_vars, open_side1_branch_q2_vars,
                               open_side2_branch_p1_vars, open_side2_branch_q1_vars,
                               branch_num_2_index)

    @staticmethod
    def add_power_balance_constraint(network_cache: NetworkCache, model: ipopt.Model, sf_index: FunctionIndex,
                                     variable_context:VariableContext) -> None:
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
        for num, row in enumerate(network_cache.generators.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_gen[bus_num].append(variable_context.gen_p_vars[num])
                bus_q_gen[bus_num].append(variable_context.gen_q_vars[num])

        # aggregated loads
        loads_sum = network_cache.loads.groupby("bus_id", as_index=False).agg({"p0": "sum", "q0": "sum"})
        for row in loads_sum.itertuples(index=False):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_load[bus_num] -= row.p0
                bus_q_load[bus_num] -= row.q0

        # shunts
        for num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_gen[bus_num].append(variable_context.shunt_p_vars[num])
                bus_q_gen[bus_num].append(variable_context.shunt_q_vars[num])

        for bus_num in range(bus_count):
            bus_p_expr = poi.ExprBuilder()
            bus_p_expr += poi.quicksum(bus_p_gen[bus_num])
            bus_p_expr -= bus_p_load[bus_num]
            model.add_quadratic_constraint(bus_p_expr, poi.Eq, 0.0)

            bus_q_expr = poi.ExprBuilder()
            bus_q_expr += poi.quicksum(bus_q_gen[bus_num])
            bus_q_expr -= bus_q_load[bus_num]
            model.add_quadratic_constraint(bus_q_expr, poi.Eq, 0.0)

    @staticmethod
    def get_voltage_bounds(low_voltage_limit: float, high_voltage_limit: float):
        return DEFAULT_V_BOUNDS  # FIXME get from voltage level dataframe

    def set_variables_bounds(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: VariableContext,
                             parameters: OptimalPowerFlowParameters):
        # voltage buses bounds
        for bus_num, row in enumerate(network_cache.buses.itertuples()):
            v_bounds = self.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit)
            logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to bus '{row.Index}' (num={bus_num})'")
            model.set_variable_bounds(variable_context.v_vars[bus_num], v_bounds.min_value, v_bounds.max_value)
            model.set_variable_start(variable_context.v_vars[bus_num], 1.0)

        # slack bus angle forced to 0
        if len(network_cache.slack_terminal) > 0:
            slack_bus_id = network_cache.slack_terminal.iloc[0].bus_id
        else:
            slack_bus_id = network_cache.buses.iloc[0].name
        slack_bus_num = network_cache.buses.index.get_loc(slack_bus_id)
        model.set_variable_bounds(variable_context.ph_vars[slack_bus_num], 0.0, 0.0)
        logger.log(TRACE_LEVEL, f"Angle reference is at bus '{slack_bus_id}' (num={slack_bus_num})")

        # generator active and reactive power bounds
        for gen_num, row in enumerate(network_cache.generators.itertuples()):
            p_bounds = Bounds(row.min_p, row.max_p).mirror()
            logger.log(TRACE_LEVEL, f"Add active power bounds {p_bounds} to generator '{row.Index}' (num={gen_num})")
            model.set_variable_bounds(variable_context.gen_p_vars[gen_num], p_bounds.min_value, p_bounds.max_value)
            q_bounds = Bounds(row.min_q_at_target_p, row.max_q_at_target_p).reduce(parameters.reactive_bounds_reduction).mirror()
            logger.log(TRACE_LEVEL, f"Add reactive power bounds {q_bounds} to generator '{row.Index}' (num={gen_num})")
            if abs(q_bounds.max_value - q_bounds.min_value) < 1.0 / network_cache.network.nominal_apparent_power:
                logger.error(f"Too small reactive power bounds {q_bounds} for generator '{row.Index}' (num={gen_num})")
            model.set_variable_bounds(variable_context.gen_q_vars[gen_num], q_bounds.min_value, q_bounds.max_value)

    def set_constraints(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: VariableContext,
                        cbf_index: FunctionIndex, o1bf_index: FunctionIndex, o2bf_index: FunctionIndex, sf_index: FunctionIndex):
        # branch flow nonlinear constraints
        for branch_num, row in enumerate(network_cache.lines.itertuples(index=False)):
            r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
            r1 = 1.0
            a1 = 0.0
            branch_index = variable_context.branch_num_2_index[branch_num]
            self.add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                       cbf_index, o1bf_index, o2bf_index,
                                       r, x, g1, b1, g2, b2, r1, a1, variable_context)

        for transfo_num, row in enumerate(network_cache.transformers.itertuples(index=False)):
            r, x, g, b, rho, alpha = row.r, row.x, row.g, row.b, row.rho, row.alpha
            g1 = g / 2
            g2 = g / 2
            b1 = b / 2
            b2 = b / 2
            r1 = rho
            a1 = alpha
            branch_num = len(network_cache.lines) + transfo_num
            branch_index = variable_context.branch_num_2_index[branch_num]
            self.add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                       cbf_index, o1bf_index, o2bf_index,
                                       r, x, g1, b1, g2, b2, r1, a1, variable_context)

        # shunt flow nonlinear constraints
        for num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            g, b, bus_id = row.g, row.b, row.bus_id
            if bus_id:
                p_var = variable_context.shunt_p_vars[num]
                q_var = variable_context.shunt_q_vars[num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = variable_context.v_vars[bus_num]
                model.add_nl_constraint(
                    sf_index,
                    vars=nlfunc.Vars(
                        v=v_var,
                        p=p_var,
                        q=q_var,
                    ),
                    params=nlfunc.Params(
                        g=g,
                        b=b,
                    ),
                    eq=0.0,
                )

        # power balance constraints
        self.add_power_balance_constraint(network_cache, model, sf_index, variable_context)

    def create_model(self, network_cache: NetworkCache, parameters: OptimalPowerFlowParameters) -> tuple[ipopt.Model, VariableContext]:
        model = ipopt.Model()

        # register functions
        cbf_index = model.register_function(closed_branch_flow)
        o1bf_index = model.register_function(open_side1_branch_flow)
        o2bf_index = model.register_function(open_side2_branch_flow)
        sf_index = model.register_function(shunt_flow)

        # create variables
        variable_context = self.create_variable_context(network_cache, model)

        # variable bounds
        self.set_variables_bounds(network_cache, model, variable_context, parameters)

        # constraints
        self.set_constraints(network_cache, model, variable_context, cbf_index, o1bf_index, o2bf_index, sf_index)

        # cost function
#        cost = self.create_minimal_active_power_cost_function(variable_context)
        cost = self.create_minimal_losses_cost_function(variable_context)
        model.set_objective(cost)

        return model, variable_context

    @staticmethod
    def create_minimal_active_power_cost_function(variable_context: VariableContext):
        cost = poi.ExprBuilder()
        for gen_num in range(len(variable_context.gen_p_vars)):
            a, b, c = 0, 1.0, 0  # TODO
            cost += a * variable_context.gen_p_vars[gen_num] * variable_context.gen_p_vars[gen_num] + b * variable_context.gen_p_vars[gen_num] + c
        return cost

    @staticmethod
    def create_minimal_losses_cost_function(variable_context: VariableContext) -> None:
        cost = poi.ExprBuilder()
        for branch_index in range(len(variable_context.closed_branch_p1_vars)):
            cost += variable_context.closed_branch_p1_vars[branch_index] - variable_context.closed_branch_p2_vars[branch_index]
        return cost

    def update_generators(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: VariableContext):
        gen_ids = []
        gen_target_p = []
        gen_target_q = []
        gen_target_v = []
        gen_voltage_regulator_on = []
        for gen_num, (gen_id, row) in enumerate(network_cache.generators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                gen_ids.append(gen_id)
                p = model.get_value(variable_context.gen_p_vars[gen_num])
                target_p = -p
                gen_target_p.append(target_p)
                q = model.get_value(variable_context.gen_q_vars[gen_num])
                target_q = -q
                gen_target_q.append(target_q)
                bus_num = network_cache.buses.index.get_loc(bus_id)
                target_v = model.get_value(variable_context.v_vars[bus_num])
                gen_target_v.append(target_v)
                q_bounds = Bounds(row.min_q_at_target_p, row.max_q_at_target_p).mirror()
                voltage_regulator_on = q_bounds.contains(q)
                logger.log(TRACE_LEVEL, f"Update generator '{gen_id}' (num={gen_num}): target_p={target_p}, target_q={target_q}, target_v={target_v}, voltage_regulator_on={voltage_regulator_on}")
                gen_voltage_regulator_on.append(voltage_regulator_on)

        self._network.update_generators(id=gen_ids, target_p=gen_target_p, target_q=gen_target_q, target_v=gen_target_v,
                                        voltage_regulator_on=gen_voltage_regulator_on)

    def update_buses(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: VariableContext):
        bus_ids = []
        bus_v_mag = []
        bus_v_angle = []
        for bus_num, (bus_id, row) in enumerate(network_cache.buses.iterrows()):
            bus_ids.append(bus_id)
            v = model.get_value(variable_context.v_vars[bus_num])
            bus_v_mag.append(v)
            angle = model.get_value(variable_context.ph_vars[bus_num])
            bus_v_angle.append(angle)
            logger.log(TRACE_LEVEL, f"Update bus '{bus_id}' (num={bus_num}): v={v}, angle={angle}")

        self._network.update_buses(id=bus_ids, v_mag=bus_v_mag, v_angle=bus_v_angle)

    def update_network(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: VariableContext) -> None:
        self.update_generators(network_cache, model, variable_context)
        self.update_buses(network_cache, model, variable_context)

    @staticmethod
    def analyze_violations(network_cache: NetworkCache, model: ipopt.Model,
                           variable_context: VariableContext) -> None:
        # check voltage bounds
        for bus_num, (bus_id, row) in enumerate(network_cache.buses.iterrows()):
            v = model.get_value(variable_context.v_vars[bus_num])
            v_bounds = OptimalPowerFlow.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit)
            if not v_bounds.contains(v):
                logger.error(f"Voltage magnitude violation: bus '{bus_id}' (num={bus_num}) {v} not in {v_bounds}")

        # check generator limits
        for gen_num, (gen_id, row) in enumerate(network_cache.generators.iterrows()):
            if row.bus_id:
                p = model.get_value(variable_context.gen_p_vars[gen_num])
                q = model.get_value(variable_context.gen_q_vars[gen_num])

                p_bounds = Bounds(row.min_p, row.max_p).mirror()
                if not p_bounds.contains(p):
                    logger.error(f"Generator active power violation: generator '{gen_id}' (num={gen_num}) {p} not in [{-row.max_p}, {-row.min_p}]")

                q_bounds = Bounds(row.min_q_at_target_p, row.max_q_at_target_p).mirror()
                if not q_bounds.contains(q):
                    logger.error(f"Generator reactive power violation: generator '{gen_id}' (num={gen_num}) {q} not in {q_bounds}")

    def run(self, parameters: OptimalPowerFlowParameters) -> bool:
        network_cache = NetworkCache(self._network)

        model, variable_context = self.create_model(network_cache, parameters)
        model.optimize()

        status = model.get_model_attribute(poi.ModelAttribute.TerminationStatus)
        logger.info(f"Optimizer ends with status {status}")

        self.analyze_violations(network_cache, model, variable_context)

        self.update_network(network_cache, model, variable_context)

        return status == poi.TerminationStatusCode.LOCALLY_SOLVED


def run_ac(network: Network, parameters: OptimalPowerFlowParameters = OptimalPowerFlowParameters()) -> bool:
    opf = OptimalPowerFlow(network)
    return opf.run(parameters)
