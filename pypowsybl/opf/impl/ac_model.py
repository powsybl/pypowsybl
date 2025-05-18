import logging
from math import hypot, atan2

import pyoptinterface as poi
from pyoptinterface import nlfunc, ipopt

from pypowsybl.opf.impl.ac_function_context import AcFunctionContext
from pypowsybl.opf.impl.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.bounds import Bounds
from pypowsybl.opf.impl.network_cache import NetworkCache
from pypowsybl.opf.impl.util import TRACE_LEVEL

logger = logging.getLogger(__name__)

class AcModel:
    def __init__(self, network_cache: NetworkCache, model: ipopt.Model, variable_context: AcVariableContext):
        self._network_cache = network_cache
        self._model = model
        self._variable_context = variable_context

    @property
    def network_cache(self) -> NetworkCache:
        return self._network_cache

    @property
    def model(self) -> ipopt.Model:
        return self._model

    @property
    def variable_context(self) -> AcVariableContext:
        return self._variable_context

    @classmethod
    def build(cls, network_cache: NetworkCache, parameters: AcOptimalPowerFlowParameters) -> 'AcModel':
        model = ipopt.Model()

        # create variables
        variable_context = AcVariableContext.build(network_cache, model)

        # variable bounds
        cls._add_variables_bounds(network_cache, model, variable_context, parameters)

        # register functions
        function_context = AcFunctionContext.build(model)

        # constraints
        cls._add_constraints(network_cache, parameters, model, variable_context, function_context)

        # cost function
        logger.debug(f"Using cost function: '{parameters.cost_function.name}'")
        cost = parameters.cost_function.create(network_cache, variable_context)
        model.set_objective(cost)

        return AcModel(network_cache, model, variable_context)

    @staticmethod
    def _add_variables_bounds(network_cache: NetworkCache, model: ipopt.Model, variable_context: AcVariableContext,
                              parameters: AcOptimalPowerFlowParameters) -> None:
        # voltage buses bounds
        for bus_num, row in enumerate(network_cache.buses.itertuples()):
            v_bounds = Bounds.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit)
            logger.log(TRACE_LEVEL, f"Add voltage magnitude bounds {v_bounds} to bus '{row.Index}' (num={bus_num})'")
            model.set_variable_bounds(variable_context.v_vars[bus_num], *Bounds.fix(row.Index, v_bounds.min_value, v_bounds.max_value))
            model.set_variable_start(variable_context.v_vars[bus_num], 1.0)

        # slack bus angle forced to 0
        slack_bus_id = network_cache.slack_terminal.iloc[0].bus_id if len(network_cache.slack_terminal) > 0 else network_cache.buses.iloc[0].name
        slack_bus_num = network_cache.buses.index.get_loc(slack_bus_id)
        model.set_variable_bounds(variable_context.ph_vars[slack_bus_num], 0.0, 0.0)
        logger.log(TRACE_LEVEL, f"Angle reference is at bus '{slack_bus_id}' (num={slack_bus_num})")

        # generator active and reactive power bounds
        for gen_num, row in enumerate(network_cache.generators.itertuples()):
            if row.bus_id:
                p_bounds = Bounds(row.min_p, row.max_p).mirror()
                logger.log(TRACE_LEVEL, f"Add active power bounds {p_bounds} to generator '{row.Index}' (num={gen_num})")
                gen_p_index = variable_context.gen_p_num_2_index[gen_num]
                model.set_variable_bounds(variable_context.gen_p_vars[gen_p_index], *Bounds.fix(row.Index, p_bounds.min_value, p_bounds.max_value))

                gen_q_index = variable_context.gen_q_num_2_index[gen_num]
                if gen_q_index != -1: # valid
                    q_bounds = Bounds.get_generator_reactive_power_bounds(row).reduce(parameters.reactive_bounds_reduction).mirror()
                    logger.log(TRACE_LEVEL, f"Add reactive power bounds {q_bounds} to generator '{row.Index}' (num={gen_num})")
                    model.set_variable_bounds(variable_context.gen_q_vars[gen_q_index], *Bounds.fix(row.Index, q_bounds.min_value, q_bounds.max_value))

        # VSC converter station active and reactive power bounds
        for vsc_cs_num, row in enumerate(network_cache.vsc_converter_stations.itertuples()):
            if row.bus_id:
                p_bounds = Bounds(-row.max_p, row.max_p).mirror()
                logger.log(TRACE_LEVEL, f"Add active power bounds {p_bounds} to VSC converter station '{row.Index}' (num={vsc_cs_num})")
                model.set_variable_bounds(variable_context.vsc_cs_p_vars[vsc_cs_num], *Bounds.fix(row.Index, p_bounds.min_value, p_bounds.max_value))

                q_bounds = Bounds.get_generator_reactive_power_bounds(row).reduce(parameters.reactive_bounds_reduction).mirror()
                logger.log(TRACE_LEVEL, f"Add reactive power bounds {q_bounds} to VSC converter station '{row.Index}' (num={vsc_cs_num})")
                if abs(q_bounds.max_value - q_bounds.min_value) < 1.0 / network_cache.network.nominal_apparent_power:
                    logger.error(f"Too small reactive power bounds {q_bounds} for VSC converter station '{row.Index}' (num={vsc_cs_num})")
                model.set_variable_bounds(variable_context.vsc_cs_q_vars[vsc_cs_num], *Bounds.fix(row.Index, q_bounds.min_value, q_bounds.max_value))

    @staticmethod
    def _add_branch_constraint(branch_index: int, bus1_id: str, bus2_id: str, network_cache: NetworkCache, model,
                               r: float, x: float, g1: float, b1: float, g2: float, b2: float, r1: float, a1: float,
                               variable_context: AcVariableContext, function_context: AcFunctionContext) -> None:
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
                function_context.cbf_index,
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
                function_context.o1bf_index,
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
                function_context.o2bf_index,
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
    def _add_power_balance_constraint(network_cache: NetworkCache, model: ipopt.Model,
                                      variable_context:AcVariableContext) -> None:
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
                if gen_q_index == -1: # invalid
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

    @classmethod
    def _add_constraints(cls, network_cache: NetworkCache, parameters: AcOptimalPowerFlowParameters,
                         model: ipopt.Model, variable_context: AcVariableContext, function_context: AcFunctionContext) -> None:
        # branch flow nonlinear constraints
        for branch_num, row in enumerate(network_cache.lines.itertuples(index=False)):
            r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
            r1 = 1.0
            a1 = 0.0
            branch_index = variable_context.branch_num_2_index[branch_num]
            cls._add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                       r, x, g1, b1, g2, b2, r1, a1,
                                       variable_context, function_context)

        for transfo_num, row in enumerate(network_cache.transformers.itertuples(index=False)):
            r, x, g, b, rho, alpha = row.r_tap, row.x_tap, row.g_tap, row.b_tap, row.rho, row.alpha
            if parameters.twt_split_shunt_admittance:
                g1 = g / 2
                g2 = g / 2
                b1 = b / 2
                b2 = b / 2
            else:
                g1 = g
                g2 = 0
                b1 = b
                b2 = 0
            r1 = rho
            a1 = alpha
            branch_num = len(network_cache.lines) + transfo_num
            branch_index = variable_context.branch_num_2_index[branch_num]
            cls._add_branch_constraint(branch_index, row.bus1_id, row.bus2_id, network_cache, model,
                                       r, x, g1, b1, g2, b2, r1, a1,
                                       variable_context, function_context)

        # shunt flow nonlinear constraints
        for shunt_num, row in enumerate(network_cache.shunts.itertuples(index=False)):
            g, b, bus_id = row.g, row.b, row.bus_id
            if bus_id:
                shunt_index = variable_context.shunt_num_2_index[shunt_num]
                p_var = variable_context.shunt_p_vars[shunt_index]
                q_var = variable_context.shunt_q_vars[shunt_index]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = variable_context.v_vars[bus_num]
                model.add_nl_constraint(
                    function_context.sf_index,
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

        # static var compensator reactive limits quadratic constraints
        for svc_num, row in enumerate(network_cache.static_var_compensators.itertuples(index=False)):
            b_min, b_max, bus_id = row.b_min, row.b_max, row.bus_id
            if bus_id:
                svc_index = variable_context.svc_num_2_index[svc_num]
                q_var = variable_context.svc_q_vars[svc_index]
                bus_num = network_cache.buses.index.get_loc(bus_id)
                v_var = variable_context.v_vars[bus_num]
                q_min_expr = poi.ExprBuilder()
                q_min_expr += b_min * v_var * v_var
                q_min_expr -= q_var
                model.add_quadratic_constraint(q_min_expr, poi.Leq, 0.0)
                q_max_expr = poi.ExprBuilder()
                q_max_expr += b_max * v_var * v_var
                q_max_expr -= q_var
                model.add_quadratic_constraint(q_max_expr, poi.Geq, 0.0)

        # HVDC line constraints
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

        # power balance constraints
        cls._add_power_balance_constraint(network_cache, model, variable_context)

    def update_network(self):
        self.variable_context.update_network(self.network_cache, self.model)

    def analyze_violations(self) -> None:
        # check voltage bounds
        for bus_num, (bus_id, row) in enumerate(self.network_cache.buses.iterrows()):
            v = self.model.get_value(self.variable_context.v_vars[bus_num])
            v_bounds = Bounds.get_voltage_bounds(row.low_voltage_limit, row.high_voltage_limit)
            if not v_bounds.contains(v):
                logger.error(f"Voltage magnitude violation: bus '{bus_id}' (num={bus_num}) {v} not in {v_bounds}")

        # check generator limits
        for gen_num, (gen_id, row) in enumerate(self.network_cache.generators.iterrows()):
            if row.bus_id:
                gen_p_index = self.variable_context.gen_p_num_2_index[gen_num]
                p = self.model.get_value(self.variable_context.gen_p_vars[gen_p_index])

                p_bounds = Bounds(row.min_p, row.max_p).mirror()
                if not p_bounds.contains(p):
                    logger.error(f"Generator active power violation: generator '{gen_id}' (num={gen_num}) {p} not in [{-row.max_p}, {-row.min_p}]")

                gen_q_index = self.variable_context.gen_q_num_2_index[gen_num]
                if gen_q_index != -1: # valid
                    q = self.model.get_value(self.variable_context.gen_q_vars[gen_q_index])
                    q_bounds = Bounds.get_generator_reactive_power_bounds(row).mirror()
                    if not q_bounds.contains(q):
                        logger.error(f"Generator reactive power violation: generator '{gen_id}' (num={gen_num}) {q} not in {q_bounds}")
