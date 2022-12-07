#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp
import pypowsybl.dynamic as dyn
import pytest
import pandas as pd


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)


# TODO: Define a small but meaningful IIDM file as string and parse it on set up


def test_get_possible_events():
    assert set(dyn.EventMapping.get_possible_events()) == set([dyn.EventType.QUADRIPOLE_DISCONNECT,
                                                               dyn.EventType.SET_POINT_BOOLEAN, dyn.EventType.BRANCH_DISCONNECTION])


def test_add_mapping():
    id = "test_id"
    parameter_id = "test_parameter"
    model_mapping = dyn.ModelMapping()
    model_mapping.add_alpha_beta_load(id, parameter_id)
    model_mapping.add_one_transformer_load(id, parameter_id)
    model_mapping.add_omega_ref(id)
    model_mapping.add_generator_synchronous_three_windings(id, parameter_id)
    model_mapping.add_generator_synchronous_three_windings_proportional_regulations(
        id, parameter_id)
    model_mapping.add_generator_synchronous_four_windings(id, parameter_id)
    model_mapping.add_generator_synchronous_four_windings_proportional_regulations(
        id, parameter_id)
    model_mapping.add_current_limit_automaton(
        id, parameter_id, dyn.BranchSide.TWO)


def test_dataframe_mapping():
    network = pp.network.create_ieee9()
    model_mapping = dyn.ModelMapping()
    load_mapping_df = pd.DataFrame.from_dict({"static_id": [network.get_loads().loc[l].name for l in network.get_loads().index],
                                              "parameter_set_id": ["LAB" for l in network.get_loads().index],
                                              "mapping_type": [dyn.DynamicMappingType.ALPHA_BETA_LOAD for l in network.get_loads().index]})

    generator_mapping_df = pd.DataFrame.from_dict({"static_id": [network.get_generators().loc[l].name for l in network.get_generators().index],
                                                   "parameter_set_id": ["GSTWPR" for l in network.get_generators().index],
                                                   "mapping_type": [dyn.DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS for l in network.get_generators().index]})

    omega_ref_df = pd.DataFrame.from_dict({"static_id": [network.get_generators().loc[l].name for l in network.get_generators().index],
                                           "parameter_set_id": ["ignored" for l in network.get_generators().index],
                                           "mapping_type": [dyn.DynamicMappingType.OMEGA_REF for l in network.get_generators().index]})

    model_mapping.add_all_dynamic_mappings(
        pd.concat([load_mapping_df, generator_mapping_df, omega_ref_df],
                  ignore_index=True)
    )


def test_add_event():
    events = dyn.EventMapping()
    events.add_event("EQD", dyn.EventType.QUADRIPOLE_DISCONNECT,
                     "test_quadripole_id")


def test_add_curve():
    timeseries = dyn.CurveMapping()
    timeseries.add_curves("test_load_id", ["load_PPu", "load_QPu"])
