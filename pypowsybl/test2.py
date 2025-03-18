import logging
from math import hypot, atan2

import pyoptinterface as poi
from pyoptinterface import nlfunc, ipopt

import pypowsybl as pp
from pypowsybl import PyPowsyblError

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

    sinKsi = nlfunc.sin(ksi)
    cosKsi = nlfunc.cos(ksi)
    theta1 = ksi - a1 + A2 - ph1 + ph2
    theta2 = ksi + a1 - A2 + ph1 - ph2
    sinTheta1 = nlfunc.sin(theta1)
    cosTheta1 = nlfunc.cos(theta1)
    sinTheta2 = nlfunc.sin(theta2)
    cosTheta2 = nlfunc.cos(theta2)

    p1_eq = r1 * v1 * (g1 * r1 * v1 + y * r1 * v1 * sinKsi - y * R2 * v2 * sinTheta1) - p1
    q1_eq = r1 * v1 * (-b1 * r1 * v1 + y * r1 * v1 * cosKsi - y * R2 * v2 * cosTheta1) - q1
    p2_eq = R2 * v2 * (g2 * R2 * v2 - y * r1 * v1 * sinTheta2 + y * R2 * v2 * sinKsi) - p2
    q2_eq = R2 * v2 * (-b2 * R2 * v2 - y * r1 * v1 * cosTheta2 + y * R2 * v2 * cosKsi) - q2

    return [p1_eq, q1_eq, p2_eq, q2_eq]


def add_branch_constraint(bf, v1_var, v2_var, ph1_var, ph2_var, p1_var, q1_var, p2_var, q2_var,
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
if __name__ == "__main__":
    import pandas as pd

    pd.options.display.max_columns = None
    pd.options.display.expand_frame_repr = False

    n = pp.network.create_ieee9()
#    n = pp.network.create_eurostag_tutorial_example1_network()
    n.per_unit = True
    buses = n.get_buses()
    generators = n.get_generators()
    loads = n.get_loads()
    lines = n.get_lines()
    transfos = n.get_2_windings_transformers(all_attributes=True)
    branches = n.get_branches()

    slack_terminal = n.get_extensions('slackTerminal')
    if len(slack_terminal) > 0:
        slack_bus_id = slack_terminal.iloc[0].bus_id
    else:
        slack_bus_id = buses.iloc[0].name

    model = ipopt.Model()
    bf = model.register_function(branch_flow)
    branch_count = len(lines) + len(transfos)
    bus_count = len(buses)
    gen_count = len(generators)

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
    slack_bus_num = buses.index.get_loc(slack_bus_id)
    model.set_variable_bounds(ph_vars[slack_bus_num], 0.0, 0.0)

    # generators reactive power bounds
    gen_p_vars = model.add_variables(range(gen_count), name="gen_p")
    gen_q_vars = model.add_variables(range(gen_count), name="gen_q")

    for gen_num, row in enumerate(generators.itertuples(index=False)):
        model.set_variable_bounds(gen_p_vars[gen_num], row.min_p, row.max_p)
        model.set_variable_bounds(gen_q_vars[gen_num], row.min_q, row.max_q)

    # branch flow nonlinear constraints
    for branch_num, row in enumerate(lines.itertuples(index=False)):
        r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
        r1 = 1.0
        a1 = 0.0

        p1_var = branch_p1_vars[branch_num]
        q1_var = branch_q1_vars[branch_num]
        p2_var = branch_p2_vars[branch_num]
        q2_var = branch_q2_vars[branch_num]

        if row.bus1_id and row.bus2_id:
            bus1_num = buses.index.get_loc(row.bus1_id)
            bus2_num = buses.index.get_loc(row.bus2_id)
            v1_var = v_vars[bus1_num]
            v2_var = v_vars[bus2_num]
            ph1_var = ph_vars[bus1_num]
            ph2_var = ph_vars[bus2_num]
            add_branch_constraint(bf,
                                  v1_var, v2_var, ph1_var, ph2_var, p1_var, q1_var, p2_var, q2_var,
                                  r, x, g1, b1, g2, b2, r1, a1)
        else:
            raise PyPowsyblError("Only branches connected to both sides are supported")

    for transfo_num, row in enumerate(transfos.itertuples(index=False)):
        r, x, g, b, rho = row.r, row.x, row.g, row.b, row.rho
        g1 = g / 2
        g2 = g / 2
        b1 = b / 2
        b2 = b / 2
        r1 = rho
        a1 = 0 # TODO

        branch_num = len(lines) + transfo_num
        p1_var = branch_p1_vars[branch_num]
        q1_var = branch_q1_vars[branch_num]
        p2_var = branch_p2_vars[branch_num]
        q2_var = branch_q2_vars[branch_num]

        if row.bus1_id and row.bus2_id:
            bus1_num = buses.index.get_loc(row.bus1_id)
            bus2_num = buses.index.get_loc(row.bus2_id)
            v1_var = v_vars[bus1_num]
            v2_var = v_vars[bus2_num]
            ph1_var = ph_vars[bus1_num]
            ph2_var = ph_vars[bus2_num]
            add_branch_constraint(bf,
                                  v1_var, v2_var, ph1_var, ph2_var, p1_var, q1_var, p2_var, q2_var,
                                  r, x, g1, b1, g2, b2, r1, a1)
        else:
            raise PyPowsyblError("Only branches connected to both sides are supported")

    # power balance constraints
    bus_p_gen = [[] for i in range(bus_count)]
    bus_q_gen = [[] for i in range(bus_count)]
    bus_p_load = [0.0 for i in range(bus_count)]
    bus_q_load = [0.0 for i in range(bus_count)]

    for branch_num, row in enumerate(branches.itertuples(index=False)):
        if row.bus1_id and row.bus2_id:
            bus1_num = buses.index.get_loc(row.bus1_id)
            bus2_num = buses.index.get_loc(row.bus2_id)
            bus_p_gen[bus1_num].append(branch_p1_vars[branch_num])
            bus_q_gen[bus1_num].append(branch_q1_vars[branch_num])
            bus_p_gen[bus2_num].append(branch_p2_vars[branch_num])
            bus_q_gen[bus2_num].append(branch_q2_vars[branch_num])
        else:
            raise PyPowsyblError("Only branches connected to both sides are supported")

    for num, row in enumerate(generators.itertuples(index=False)):
        bus_id = row.bus_id
        if bus_id:
            bus_num = buses.index.get_loc(bus_id)
            bus_p_gen[bus_num].append(gen_p_vars[num])
            bus_q_gen[bus_num].append(gen_q_vars[num])

    loads_sum = loads.groupby("bus_id", as_index=False).agg({"p0": "sum", "q0": "sum"})
    for row in loads_sum.itertuples(index=False):
        bus_id = row.bus_id
        if bus_id:
            bus_num = buses.index.get_loc(bus_id)
            bus_p_load[bus_num] -= row.p0
            bus_q_load[bus_num] -= row.q0

    # TODO shunts
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
        a, b, c = 0, 1.0, 0 # TODO
        cost += a * gen_p_vars[gen_num] * gen_p_vars[gen_num] + b * gen_p_vars[gen_num] + c
    model.set_objective(cost)

    model.optimize()

    print(model.get_model_attribute(poi.ModelAttribute.TerminationStatus))

    # update
    gen_ids = []
    gen_target_p = []
    gen_target_q = []
    gen_target_v = []
    for gen_num, (gen_id, row) in enumerate(generators.iterrows()):
        bus_id = row.bus_id
        if bus_id:
            gen_ids.append(gen_id)
            gen_target_p.append(-model.get_value(gen_p_vars[gen_num]))
            gen_target_q.append(-model.get_value(gen_q_vars[gen_num]))
            bus_num = buses.index.get_loc(bus_id)
            gen_target_v.append(model.get_value(v_vars[bus_num]))

    n.update_generators(id=gen_ids, target_p=gen_target_p, target_q=gen_target_q, target_v=gen_target_v)

    bus_ids = []
    bus_v_mag = []
    bus_v_angle = []
    for bus_num, (bus_id, row) in enumerate(buses.iterrows()):
        bus_ids.append(bus_id)
        bus_v_mag.append(model.get_value(v_vars[bus_num]))
        bus_v_angle.append(model.get_value(ph_vars[bus_num]))

    n.update_buses(id=bus_ids, v_mag=bus_v_mag, v_angle=bus_v_angle)
    print(n.get_buses())

    parameters = pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.PREVIOUS_VALUES)
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(logging.DEBUG)
    r = pp.loadflow.run_ac(n, parameters)
    print(r)
