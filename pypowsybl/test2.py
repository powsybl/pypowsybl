from math import hypot, atan2

import pyoptinterface as poi
from pyoptinterface import nlfunc, ipopt

import pypowsybl as pp
from pypowsybl import PyPowsyblError

R2 = 1
A2 = 0

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

    p1 = r1 * v1 * (g1 * r1 * v1 + y * r1 * v1 * sinKsi - y * R2 * v2 * sinTheta1)
    q1 = r1 * v1 * (-b1 * r1 * v1 + y * r1 * v1 * cosKsi - y * R2 * v2 * cosTheta1)
    p2 = R2 * v2 * (g2 * R2 * v2 - y * r1 * v1 * sinTheta2 + y * R2 * v2 * sinKsi)
    q2 = R2 * v2 * (-b2 * R2 * v2 - y * r1 * v1 * cosTheta2 + y * R2 * v2 * cosKsi)

    return [p1, q1, p2, q2]


def add_branch_constraint(bf, v1, v2, ph1, ph2, p1, q1, p2, q2,
                          r, x, g1, b1, g2, b2, r1, a1):
    z = hypot(r, x)
    y = 1.0 / z
    ksi = atan2(r, x)

    model.add_nl_constraint(
        bf,
        vars=nlfunc.Vars(
            v1=v1,
            v2=v2,
            ph1=ph1,
            ph2=ph2,
            p1=p1,
            q1=q1,
            p2=p2,
            q2=q2
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

#    n = pp.network.create_ieee9()
    n = pp.network.create_eurostag_tutorial_example1_network()
    n.per_unit = True
    lines = n.get_lines()
    buses = n.get_buses()
    generators = n.get_generators()
    print(generators)
    transfos = n.get_2_windings_transformers(all_attributes=True)
    branches = n.get_branches()
    loads = n.get_loads()
    n.save("/tmp/toto.xiidm")

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
    branch_p1 = model.add_variables(range(branch_count), name='branch_p1')
    branch_q1 = model.add_variables(range(branch_count), name='branch_q1')
    branch_p2 = model.add_variables(range(branch_count), name='branch_p2')
    branch_q2 = model.add_variables(range(branch_count), name='branch_q2')

    v = model.add_variables(range(bus_count), name="v")
    ph = model.add_variables(range(bus_count), name="ph")

    # voltage buses bounds
    for i in range(bus_count):
        vmin, vmax = 0.5, 1.5  # FIXME get from voltage level dataframe
        model.set_variable_bounds(v[i], vmin, vmax)

    # slack bus angle forced to 0
    slack_bus_num = buses.index.get_loc(slack_bus_id)
    model.set_variable_bounds(ph[slack_bus_num], 0.0, 0.0)

    # generators reactive power bounds
    gen_p = model.add_variables(range(gen_count), name="gen_p")
    gen_q = model.add_variables(range(gen_count), name="gen_q")

    # for num, row in enumerate(generators.itertuples(index=False)):
    #     model.set_variable_bounds(gen_p[num], row.min_p, row.max_p)
    #     model.set_variable_bounds(gen_q[num], row.min_q, row.max_q)

    # branch flow nonlinear constraints
    for branch_num, row in enumerate(lines.itertuples(index=False)):
        r, x, g1, b1, g2, b2 = row.r, row.x, row.g1, row.b1, row.g2, row.b2
        r1 = 1
        a1 = 0

        p1 = branch_p1[branch_num]
        q1 = branch_q1[branch_num]
        p2 = branch_p2[branch_num]
        q2 = branch_q2[branch_num]

        if row.bus1_id and row.bus2_id:
            bus1_num = buses.index.get_loc(row.bus1_id)
            bus2_num = buses.index.get_loc(row.bus2_id)
            v1 = v[bus1_num]
            v2 = v[bus2_num]
            ph1 = ph[bus1_num]
            ph2 = ph[bus2_num]
            add_branch_constraint(bf, v1, v2, ph1, ph2, p1, q1, p2, q2, r, x, g1, b1, g2, b2, r1, a1)
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
        p1 = branch_p1[branch_num]
        q1 = branch_q1[branch_num]
        p2 = branch_p2[branch_num]
        q2 = branch_q2[branch_num]

        if row.bus1_id and row.bus2_id:
            bus1_num = buses.index.get_loc(row.bus1_id)
            bus2_num = buses.index.get_loc(row.bus2_id)
            v1 = v[bus1_num]
            v2 = v[bus2_num]
            ph1 = ph[bus1_num]
            ph2 = ph[bus2_num]
            add_branch_constraint(bf, v1, v2, ph1, ph2, p1, q1, p2, q2, r, x, g1, b1, g2, b2, r1, a1)
        else:
            raise PyPowsyblError("Only branches connected to both sides are supported")

    # power balance constraints
    bus_p = [poi.ExprBuilder() for i in range(bus_count)]
    bus_q = [poi.ExprBuilder() for i in range(bus_count)]

    bus_branch_p1 = {}
    bus_branch_q1 = {}
    bus_branch_p2 = {}
    bus_branch_q2 = {}
    for num, row in enumerate(branches.itertuples(index=False)):
        if row.bus1_id and row.bus2_id:
            bus1_num = buses.index.get_loc(row.bus1_id)
            bus2_num = buses.index.get_loc(row.bus2_id)
            bus_branch_p1.setdefault(bus1_num, []).append(branch_p1[num])
            bus_branch_q1.setdefault(bus1_num, []).append(branch_q1[num])
            bus_branch_p2.setdefault(bus2_num, []).append(branch_p2[num])
            bus_branch_q2.setdefault(bus2_num, []).append(branch_q2[num])
        else:
            raise PyPowsyblError("Only branches connected to both sides are supported")

    bus_gen_p = {}
    bus_gen_q = {}
    for num, row in enumerate(generators.itertuples(index=False)):
        bus_id = row.bus_id
        if bus_id:
            bus_num = buses.index.get_loc(bus_id)
            bus_gen_p.setdefault(bus_num, []).append(gen_p[num])
            bus_gen_q.setdefault(bus_num, []).append(gen_q[num])

    for bus_num in range(bus_count):
        bus_p[bus_num] += poi.quicksum(bus_branch_p1.setdefault(bus_num, []))
        bus_p[bus_num] += poi.quicksum(bus_branch_p2.setdefault(bus_num, []))
        bus_p[bus_num] += poi.quicksum(bus_gen_p.setdefault(bus_num, []))
        bus_q[bus_num] += poi.quicksum(bus_branch_q1.setdefault(bus_num, []))
        bus_q[bus_num] += poi.quicksum(bus_branch_q2.setdefault(bus_num, []))
        bus_q[bus_num] += poi.quicksum(bus_gen_q.setdefault(bus_num, []))

    loads_sum = loads.groupby("bus_id", as_index=False).agg({"p0": "sum", "q0": "sum"})
    for row in loads_sum.itertuples(index=False):
        bus_id = row.bus_id
        if bus_id:
            bus_num = buses.index.get_loc(bus_id)
            bus_p[bus_num] -= row.p0
            bus_q[bus_num] -= row.q0

    # TODO shunts

    for bus_num in range(bus_count):
        model.add_quadratic_constraint(bus_p[bus_num], poi.Eq, 0.0)
        model.add_quadratic_constraint(bus_q[bus_num], poi.Eq, 0.0)

    # cost function: minimize active power
    cost = poi.ExprBuilder()
    for gen_num in range(gen_count):
        a, b, c = 0, 1.0, 0 # TODO
        cost += a * gen_p[gen_num] * gen_p[gen_num] + b * gen_p[gen_num] + c
    model.set_objective(cost)

    model.optimize()

    print(model.get_model_attribute(poi.ModelAttribute.TerminationStatus))

    for i in range(gen_count):
        print(f"Generator {i} p={model.get_value(gen_p[i])} q={model.get_value(gen_q[i])}")

    for i in range(bus_count):
        print(f"Bus {i} v=: {model.get_value(v[i])}")
