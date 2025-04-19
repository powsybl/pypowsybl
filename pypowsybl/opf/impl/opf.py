from dataclasses import dataclass
from math import hypot, atan2
from typing import Any
import logging

import pyoptinterface as poi
from pyoptinterface import nlfunc, ipopt

from pypowsybl import PyPowsyblError
from pypowsybl.network import Network

logger = logging.getLogger(__name__)


R2 = 1.0
A2 = 0.0


def branch_flow(vars, params):
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

class NetworkCache:
    def __init__(self, network: Network) -> None:
        self._network = network
        self._network.per_unit = True
        self._buses = self._network.get_buses()
        self._generators = self._network.get_generators()
        self._loads = self._network.get_loads()
        self._shunts = self._network.get_shunt_compensators()
        self._lines = self._network.get_lines()
        self._transformers = self._network.get_2_windings_transformers(all_attributes=True)
        self._branches = self._network.get_branches()
        self._slack_terminal = self._network.get_extensions('slackTerminal')

    @property
    def network(self):
        return self._network

    @property
    def buses(self):
        return self._buses

    @property
    def generators(self):
        return self._generators

    @property
    def loads(self):
        return self._loads

    @property
    def shunts(self):
        return self._shunts

    @property
    def lines(self):
        return self._lines

    @property
    def transformers(self):
        return self._transformers

    @property
    def branches(self):
        return self._branches

    @property
    def slack_terminal(self):
        return self._slack_terminal


@dataclass
class VariableContext:
    gen_p_vars: Any
    gen_q_vars: Any
    ph_vars: Any
    v_vars: Any


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

    def add_branch_constraint(model, bf, v1_var, v2_var, ph1_var, ph2_var, p1_var, q1_var, p2_var, q2_var,
                              r, x, g1, b1, g2, b2, r1, a1):
        z = hypot(r, x)
        y = 1.0 / z
        ksi = atan2(r, x)

        model.add_nl_constraint(
            bf,
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

    def create_model(self, network_cache: NetworkCache):
        model = ipopt.Model()
        bf = model.register_function(branch_flow)
        sf = model.register_function(shunt_flow)

        branch_count = len(network_cache.lines) + len(network_cache.transformers)
        bus_count = len(network_cache.buses)
        gen_count = len(network_cache.generators)
        shunt_count = len(network_cache.shunts)

        # create variables
        branch_p1_vars = model.add_variables(range(branch_count), name='branch_p1')
        branch_q1_vars = model.add_variables(range(branch_count), name='branch_q1')
        branch_p2_vars = model.add_variables(range(branch_count), name='branch_p2')
        branch_q2_vars = model.add_variables(range(branch_count), name='branch_q2')
        v_vars = model.add_variables(range(bus_count), name="v")
        ph_vars = model.add_variables(range(bus_count), name="ph")

        # voltage buses bounds
        for i in range(bus_count):
            vmin, vmax = 0.90, 1.1  # FIXME get from voltage level dataframe
            model.set_variable_bounds(v_vars[i], vmin, vmax)

        # slack bus angle forced to 0
        if len(network_cache.slack_terminal) > 0:
            slack_bus_id = network_cache.slack_terminal.iloc[0].bus_id
        else:
            slack_bus_id = network_cache.buses.iloc[0].name
        slack_bus_num = network_cache.buses.index.get_loc(slack_bus_id)
        model.set_variable_bounds(ph_vars[slack_bus_num], 0.0, 0.0)

        # generators reactive power bounds
        gen_p_vars = model.add_variables(range(gen_count), name="gen_p")
        gen_q_vars = model.add_variables(range(gen_count), name="gen_q")
        shunt_p_vars = model.add_variables(range(gen_count), name="shunt_p")
        shunt_q_vars = model.add_variables(range(gen_count), name="shunt_q")
        q_margin = 1.0 / self._network.nominal_apparent_power
        for gen_num, row in enumerate(network_cache.generators.itertuples(index=False)):
            model.set_variable_bounds(gen_p_vars[gen_num], row.min_p, row.max_p)
            model.set_variable_bounds(gen_q_vars[gen_num], row.min_q + q_margin, row.max_q - q_margin)

        # branch flow nonlinear constraints
        for branch_num, row in enumerate(network_cache.lines.itertuples(index=False)):
            r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
            r1 = 1.0
            a1 = 0.0

            p1_var = branch_p1_vars[branch_num]
            q1_var = branch_q1_vars[branch_num]
            p2_var = branch_p2_vars[branch_num]
            q2_var = branch_q2_vars[branch_num]

            if row.bus1_id and row.bus2_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                v1_var = v_vars[bus1_num]
                v2_var = v_vars[bus2_num]
                ph1_var = ph_vars[bus1_num]
                ph2_var = ph_vars[bus2_num]
                OptimalPowerFlow.add_branch_constraint(model, bf,
                                                       v1_var, v2_var, ph1_var, ph2_var, p1_var, q1_var, p2_var, q2_var,
                                                       r, x, g1, b1, g2, b2, r1, a1)
            else:
                raise PyPowsyblError("Only branches connected to both sides are supported")
        for transfo_num, row in enumerate(network_cache.transformers.itertuples(index=False)):
            r, x, g, b, rho = row.r, row.x, row.g, row.b, row.rho
            g1 = g / 2
            g2 = g / 2
            b1 = b / 2
            b2 = b / 2
            r1 = rho
            a1 = 0  # TODO

            branch_num = len(network_cache.lines) + transfo_num
            p1_var = branch_p1_vars[branch_num]
            q1_var = branch_q1_vars[branch_num]
            p2_var = branch_p2_vars[branch_num]
            q2_var = branch_q2_vars[branch_num]

            if row.bus1_id and row.bus2_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                v1_var = v_vars[bus1_num]
                v2_var = v_vars[bus2_num]
                ph1_var = ph_vars[bus1_num]
                ph2_var = ph_vars[bus2_num]
                OptimalPowerFlow.add_branch_constraint(model, bf,
                                                       v1_var, v2_var, ph1_var, ph2_var, p1_var, q1_var, p2_var, q2_var,
                                                       r, x, g1, b1, g2, b2, r1, a1)
            else:
                raise PyPowsyblError("Only branches connected to both sides are supported")

        # power balance constraints
        bus_p_gen = [[] for i in range(bus_count)]
        bus_q_gen = [[] for i in range(bus_count)]
        bus_p_load = [0.0 for i in range(bus_count)]
        bus_q_load = [0.0 for i in range(bus_count)]
        for branch_num, row in enumerate(network_cache.branches.itertuples(index=False)):
            if row.bus1_id and row.bus2_id:
                bus1_num = network_cache.buses.index.get_loc(row.bus1_id)
                bus2_num = network_cache.buses.index.get_loc(row.bus2_id)
                bus_p_gen[bus1_num].append(branch_p1_vars[branch_num])
                bus_q_gen[bus1_num].append(branch_q1_vars[branch_num])
                bus_p_gen[bus2_num].append(branch_p2_vars[branch_num])
                bus_q_gen[bus2_num].append(branch_q2_vars[branch_num])
            else:
                raise PyPowsyblError("Only branches connected to both sides are supported")
        for num, row in enumerate(network_cache.generators.itertuples(index=False)):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_gen[bus_num].append(gen_p_vars[num])
                bus_q_gen[bus_num].append(gen_q_vars[num])
        loads_sum = network_cache.loads.groupby("bus_id", as_index=False).agg({"p0": "sum", "q0": "sum"})
        for row in loads_sum.itertuples(index=False):
            bus_id = row.bus_id
            if bus_id:
                bus_num = network_cache.buses.index.get_loc(bus_id)
                bus_p_load[bus_num] -= row.p0
                bus_q_load[bus_num] -= row.q0
        for num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            g, b, bus_id = row.g, row.b, row.bus_id
            if bus_id:
                p_var = shunt_p_vars[num]
                q_var = shunt_q_vars[num]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = v_vars[bus_num]
                model.add_nl_constraint(
                    sf,
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
                bus_p_gen[bus_num].append(p_var)
                bus_q_gen[bus_num].append(q_var)
        for bus_num in range(bus_count):
            bus_p_expr = poi.ExprBuilder()
            bus_p_expr += poi.quicksum(bus_p_gen[bus_num])
            bus_p_expr -= bus_p_load[bus_num]
            model.add_quadratic_constraint(bus_p_expr, poi.Eq, 0.0)

            bus_q_expr = poi.ExprBuilder()
            bus_q_expr += poi.quicksum(bus_q_gen[bus_num])
            bus_q_expr -= bus_q_load[bus_num]
            model.add_quadratic_constraint(bus_q_expr, poi.Eq, 0.0)

        # cost function: minimize active power
        cost = poi.ExprBuilder()
        for gen_num in range(gen_count):
            a, b, c = 0, 1.0, 0  # TODO
            cost += a * gen_p_vars[gen_num] * gen_p_vars[gen_num] + b * gen_p_vars[gen_num] + c
        model.set_objective(cost)

        return model, VariableContext(gen_p_vars, gen_q_vars, ph_vars, v_vars)

    def update_network(self, network_cache: NetworkCache, model, variable_context: VariableContext):
        gen_ids = []
        gen_target_p = []
        gen_target_q = []
        gen_target_v = []
        for gen_num, (gen_id, row) in enumerate(network_cache.generators.iterrows()):
            bus_id = row.bus_id
            if bus_id:
                gen_ids.append(gen_id)
                gen_target_p.append(-model.get_value(variable_context.gen_p_vars[gen_num]))
                gen_target_q.append(-model.get_value(variable_context.gen_q_vars[gen_num]))
                bus_num = network_cache.buses.index.get_loc(bus_id)
                gen_target_v.append(model.get_value(variable_context.v_vars[bus_num]))
        self._network.update_generators(id=gen_ids, target_p=gen_target_p, target_q=gen_target_q, target_v=gen_target_v,
                                        voltage_regulator_on=[True] * len(gen_ids))

        bus_ids = []
        bus_v_mag = []
        bus_v_angle = []
        for bus_num, (bus_id, row) in enumerate(network_cache.buses.iterrows()):
            bus_ids.append(bus_id)
            bus_v_mag.append(model.get_value(variable_context.v_vars[bus_num]))
            bus_v_angle.append(model.get_value(variable_context.ph_vars[bus_num]))
        self._network.update_buses(id=bus_ids, v_mag=bus_v_mag, v_angle=bus_v_angle)

    def run(self) -> bool:
        network_cache = NetworkCache(self._network)

        model, variable_context = self.create_model(network_cache)
        model.optimize()

        status = model.get_model_attribute(poi.ModelAttribute.TerminationStatus)
        logger.info(f"Optimizer end with status {status}")

        self.update_network(network_cache, model, variable_context)

        return status == poi.TerminationStatusCode.LOCALLY_SOLVED


def run_ac(network: Network) -> bool:
    opf = OptimalPowerFlow(network)
    return opf.run()
