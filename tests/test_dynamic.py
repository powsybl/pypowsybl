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


def test_add_mapping():
    id = "test_id"
    parameter_id = "test_parameter"
    model_mapping = dyn.ModelMapping()
    model_mapping.add_base_load(id, parameter_id, "LoadPQ")
    model_mapping.add_synchronous_generator(id, parameter_id, "GeneratorSynchronousThreeWindings")
    # TODO test remaining adders


def test_dataframe_mapping():
    network = pp.network.create_ieee9()
    model_mapping = dyn.ModelMapping()
    load_mapping_df = pd.DataFrame.from_dict({"static_id": [network.get_loads().loc[l].name for l in network.get_loads().index],
                                              "parameter_set_id": ["LAB" for l in network.get_loads().index],
                                              "model_name": "LoadPQ"})
    generator_mapping_df = pd.DataFrame.from_dict({"static_id": [network.get_generators().loc[l].name for l in network.get_generators().index],
                                                   "parameter_set_id": ["GSTWPR" for l in network.get_generators().index],
                                                   "model_name": "GeneratorSynchronousThreeWindings"})

    model_mapping.add_all_dynamic_mappings(dyn.DynamicMappingType.BASE_LOAD,
                                           load_mapping_df.set_index("static_id"))
    model_mapping.add_all_dynamic_mappings(
        dyn.DynamicMappingType.SYNCHRONOUS_GENERATOR,
        generator_mapping_df.set_index("static_id"))


def test_add_event():
    events = dyn.EventMapping()
    events.add_disconnection("GEN", 5)
    events.add_disconnection("LINE", 3.3, pp.dynamic.Side.TWO)
    events.add_active_power_variation("LOAD", 14, 2)
    events.add_node_fault("BUS", 12, 2, 0.1, 0.2)


def test_add_curve():
    timeseries = dyn.CurveMapping()
    timeseries.add_curves("test_load_id_1", ["load_PPu", "load_QPu"])
    timeseries.add_curve("test_load_id_2", "load_PPu")
