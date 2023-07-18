#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp
import pypowsybl.voltage_initializer as voltage_initializer
import pytest
from pypowsybl._pypowsybl import VoltageInitializerObjective

@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)

def test_parameters():
    params = voltage_initializer.VoltageInitializerParameters()
    params.add_variable_shunt_compensators(["shunt1", "shunt2"])
    params.add_constant_q_generators(["gen1", "gen2"])
    params.add_variable_two_windings_transformers(["twt1", "twt2"])

    params.add_algorithm_param({"foo": "bar", "bar": "bar2"})
    params.add_specific_voltage_limits({"vl_id": (0.5,1.2)})

    params.set_objective(VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)
    params.set_objective_distance(1.3)


@pytest.mark.skip(reason="CI doesn't have a Ampl and Knitro runtime.")
def test_runner():
    from pypowsybl import network, voltage_initializer
    from pypowsybl._pypowsybl import VoltageInitializerObjective
    params = voltage_initializer.VoltageInitializerParameters()
    n = network.create_eurostag_tutorial_example1_network()
    some_gen_id = n.get_generators().iloc[0].name
    params.add_constant_q_generators([some_gen_id])
    # no shunts in eurostag_tutorial_example1_network
    # some_shunt_id = n.get_shunt_compensators().iloc[0].name
    # params.add_variable_shunt_compensators([n.get_shunt_compensators().iloc[0].name])
    some_2wt_id = n.get_2_windings_transformers().iloc[0].name
    params.add_variable_two_windings_transformers([some_2wt_id])

    params.add_algorithm_param({"foo": "bar", "bar": "bar2"})
    params.set_objective(VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)

    results = voltage_initializer.run_open_reac(n, params, True)
    results.apply_all_modification(n)

    print(results.get_status())
    print(results.get_indicators())

from pypowsybl import network, voltage_initializer
from pypowsybl._pypowsybl import VoltageInitializerObjective
params = voltage_initializer.VoltageInitializerParameters()
n = network.create_eurostag_tutorial_example1_network()
some_gen_id = n.get_generators().iloc[0].name
params.add_constant_q_generators([some_gen_id])
# no shunts in eurostag_tutorial_example1_network
# some_shunt_id = n.get_shunt_compensators().iloc[0].name
# params.add_variable_shunt_compensators([n.get_shunt_compensators().iloc[0].name])
some_2wt_id = n.get_2_windings_transformers().iloc[0].name
params.add_variable_two_windings_transformers([some_2wt_id])

params.add_algorithm_param({"foo": "bar", "bar": "bar2"})
params.set_objective(VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)

results = voltage_initializer.run_open_reac(n, params, True)
results.apply_all_modification(n)

print(results.get_status())
print(results.get_indicators())