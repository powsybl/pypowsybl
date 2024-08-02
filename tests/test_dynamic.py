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
    static_id = "test_id"
    dynamic_id = "test_dynamic_id"
    parameter_id = "test_parameter"
    model_mapping = dyn.ModelMapping()
    # Equipments
    model_mapping.add_base_load(static_id, parameter_id, "LoadPQ")
    model_mapping.add_load_one_transformer(static_id, parameter_id, "LoadOneTransformer")
    model_mapping.add_load_one_transformer_tap_changer(static_id, parameter_id, "LoadOneTransformerTapChanger")
    model_mapping.add_load_two_transformers(static_id, parameter_id, "LoadTwoTransformers")
    model_mapping.add_load_two_transformers_tap_changers(static_id, parameter_id, "LoadTwoTransformersTapChangers")
    model_mapping.add_base_generator(static_id, parameter_id, "GeneratorFictitious")
    model_mapping.add_synchronized_generator(static_id, parameter_id, "GeneratorPVFixed")
    model_mapping.add_synchronous_generator(static_id, parameter_id, "GeneratorSynchronousThreeWindings")
    model_mapping.add_wecc(static_id, parameter_id, "WT4BWeccCurrentSource")
    model_mapping.add_grid_forming_converter(static_id, parameter_id, "GridFormingConverterMatchingControl")
    model_mapping.add_hvdc_p(static_id, parameter_id, model_name="HvdcPV")
    model_mapping.add_hvdc_vsc(static_id, parameter_id, pp.dynamic.Side.ONE, "HvdcVSCDanglingP")
    model_mapping.add_base_transformer(static_id, parameter_id, "TransformerFixedRatio")
    model_mapping.add_base_static_var_compensator(static_id, parameter_id, "StaticVarCompensatorPV")
    model_mapping.add_base_line(static_id, parameter_id, "Line")
    model_mapping.add_base_bus(static_id, parameter_id, "Bus")
    model_mapping.add_infinite_bus(static_id, parameter_id, "InfiniteBus")
    # Dynamic automation systems
    model_mapping.add_overload_management_system(dynamic_id, parameter_id, "LINE1", "LINE2",
                                                 pp.dynamic.Side.TWO, "OverloadManagementSystem")
    model_mapping.add_two_levels_overload_management_system(dynamic_id, parameter_id, "LINE1",
                                                            "LINE1", pp.dynamic.Side.TWO,
                                                            "LINE2", pp.dynamic.Side.ONE,
                                                            "TwoLevelsOverloadManagementSystem")
    model_mapping.add_under_voltage_automation_system(dynamic_id, parameter_id, "GEN", "UnderVoltage")
    model_mapping.add_phase_shifter_i_automation_system(dynamic_id, parameter_id, "TRA", "PhaseShifterI")
    model_mapping.add_phase_shifter_p_automation_system(dynamic_id, parameter_id, "TRA", "PhaseShifterP")
    model_mapping.add_tap_changer_automation_system(dynamic_id, parameter_id, "LOAD", pp.dynamic.Side.ONE,
                                                    "TapChangerAutomaton")
    model_mapping.add_tap_changer_blocking_automation_system(dynamic_id, parameter_id, "TRA", "BUS",
                                                             "TapChangerBlockingAutomaton")


def test_dynamic_dataframe_mapping():
    network = pp.network.create_ieee9()
    model_mapping = dyn.ModelMapping()
    load_mapping_df = pd.DataFrame.from_dict({"static_id": [network.get_loads().loc[l].name for l in network.get_loads().index],
                                              "parameter_set_id": ["LAB" for l in network.get_loads().index],
                                              "model_name": ["LoadPQ" for l in network.get_loads().index]})
    generator_mapping_df = pd.DataFrame.from_dict({"static_id": [network.get_generators().loc[l].name for l in network.get_generators().index],
                                                   "parameter_set_id": ["GSTWPR" for l in network.get_generators().index],
                                                   "model_name": ["GeneratorSynchronousThreeWindings" for l in network.get_generators().index]})

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


def test_event_dataframe_mapping():
    events = dyn.EventMapping()
    event_mapping_df = pd.DataFrame.from_dict({"static_id": ["GEN"], "start_time": [10], "delta_p": [2]})

    events.add_all_event_mappings(dyn.EventMappingType.ACTIVE_POWER_VARIATION, event_mapping_df.set_index("static_id"))


def test_add_curve():
    timeseries = dyn.CurveMapping()
    timeseries.add_curves("test_load_id_1", ["load_PPu", "load_QPu"])
    timeseries.add_curve("test_load_id_2", "load_PPu")
