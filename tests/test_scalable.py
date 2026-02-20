#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp
from pypowsybl._pypowsybl import ScalingConvention, ScalingType, Priority
from pypowsybl.network import ElementScalable, StackScalable, ScalingParameters


def test_element_scalable():
    n = pp.network.create_eurostag_tutorial_example1_with_more_generators_network()
    assert n.get_generators().loc["GEN", "target_p"] == 607.0
    assert n.get_loads().loc["LOAD", "p0"] == 600.0

    gen_scalable = ElementScalable(injection_id="GEN")

    done = gen_scalable.scale(n, asked=50)
    assert done == 50.0
    assert n.get_generators().loc["GEN", "target_p"] == 657.0

    load_scalable = ElementScalable(injection_id="LOAD", max_value=700)
    parameters = ScalingParameters(scaling_convention=ScalingConvention.LOAD_SCALING_CONVENTION)
    done = load_scalable.scale(n, parameters=parameters, asked=200)
    assert done == 100.0
    assert n.get_loads().loc["LOAD", "p0"] == 700.0


def test_stack_scalable():
    n = pp.network.create_eurostag_tutorial_example1_with_more_generators_network()

    assert n.get_generators().loc["GEN", "target_p"] == 607.0
    assert n.get_generators().loc["GEN2", "target_p"] == 607.0
    scal1 = ElementScalable(injection_id="GEN", max_value=1000)
    scal2 = ElementScalable(injection_id="GEN2", max_value=1000)
    generators_scalable = StackScalable(scalables=[scal1, scal2])

    done = generators_scalable.scale(n, asked=700)
    assert done == 700.0
    assert n.get_generators().loc["GEN", "target_p"] == 1000.0
    assert n.get_generators().loc["GEN2", "target_p"] == 914.0

    n1 = pp.network.create_eurostag_tutorial_example1_with_more_generators_network()
    done = generators_scalable.scale(n1, asked=1500)
    assert done == 786.0
    assert n1.get_generators().loc["GEN", "target_p"] == 1000.0
    assert n1.get_generators().loc["GEN2", "target_p"] == 1000.0

    n2 = pp.network.create_eurostag_tutorial_example1_with_more_generators_network()
    generators_scalable_from_injections = StackScalable(injection_ids=["GEN", "GEN2"], max_value=3000)
    done = generators_scalable_from_injections.scale(n2, asked=1500)
    assert done == 1500.0
    assert n2.get_generators().loc["GEN", "target_p"] == 2107.0
    assert n2.get_generators().loc["GEN2", "target_p"] == 607.0

def test_scaling_parameters():
    parameters = ScalingParameters()
    assert ScalingConvention.GENERATOR_SCALING_CONVENTION == parameters.scaling_convention
    assert False == parameters.constant_power_factor
    assert False == parameters.reconnect
    assert False == parameters.allows_generator_out_of_active_power_limits
    assert Priority.ONESHOT == parameters.priority
    assert ScalingType.DELTA_P == parameters.scaling_type
    assert 0 == len(parameters.ignored_injection_ids)


    # Testing setting independently every attributes
    attributes = {
        'scaling_convention': [ScalingConvention.GENERATOR_SCALING_CONVENTION, ScalingConvention.LOAD_SCALING_CONVENTION],
        'constant_power_factor': [True, False],
        'reconnect': [True, False],
        'allows_generator_out_of_active_power_limits': [True, False],
        'priority': [Priority.ONESHOT, Priority.RESPECT_OF_DISTRIBUTION, Priority.RESPECT_OF_VOLUME_ASKED],
        'scaling_type': [ScalingType.DELTA_P, ScalingType.TARGET_P],
        'ignored_injection_ids': [[], ["GEN"]]
    }

    for attribute, values in attributes.items():
        for value in values:
            parameters = ScalingParameters(**dict([(attribute, value)]))
            assert value == getattr(parameters, attribute)

            parameters = ScalingParameters()
            setattr(parameters, attribute, value)
            assert value == getattr(parameters, attribute)