#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import io
import unittest
import pandas as pd
import logging
import queue
from logging import handlers
import pytest
import math

import pypowsybl as pp
import pypowsybl.sensitivity
from pypowsybl._pypowsybl import (
    RaoComputationStatus,
    ObjectiveFunctionType,
    PstModel,
    RaRangeShrinking,
    Solver,
    ExecutionCondition,
    Unit)
from pypowsybl.rao import Parameters as RaoParameters, RaoResult, RaoSearchTreeParameters, FastRaoParameters
from pypowsybl.rao import Glsk as RaoGlsk
from pypowsybl.rao import (
    ObjectiveFunctionParameters,
    RangeActionOptimizationParameters,
    RangeActionSearchTreeParameters,
    TopoOptimizationParameters,
    TopoSearchTreeParameters,
    SecondPreventiveRaoParameters,
    NotOptimizedCnecsParameters,
    LoadFlowAndSensitivityParameters,
    RaoLogFilter,
    Crac)

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'
RAO_PROVIDERS = ["SearchTreeRao", "FastRao"]

def test_default_rao_parameters():
    parameters = RaoParameters()

    assert parameters.objective_function_parameters.objective_function_type == ObjectiveFunctionType.SECURE_FLOW
    assert parameters.objective_function_parameters.unit == Unit.MEGAWATT
    assert parameters.objective_function_parameters.enforce_curative_security == False

    assert math.isclose(parameters.range_action_optimization_parameters.pst_ra_min_impact_threshold, 0.01)
    assert math.isclose(parameters.range_action_optimization_parameters.hvdc_ra_min_impact_threshold, 0.001)
    assert math.isclose(parameters.range_action_optimization_parameters.injection_ra_min_impact_threshold, 0.001)

    assert math.isclose(parameters.topo_optimization_parameters.relative_min_impact_threshold, 0.0)
    assert math.isclose(parameters.topo_optimization_parameters.absolute_min_impact_threshold, 0.0)

    assert parameters.not_optimized_cnecs_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras == False

def test_default_rao_parameters_non_default_values():
    objective_function_param = ObjectiveFunctionParameters(
        objective_function_type=ObjectiveFunctionType.MIN_COST,
        unit=Unit.KILOVOLT,
        enforce_curative_security=True)

    range_action_optim_param = RangeActionOptimizationParameters(
        pst_ra_min_impact_threshold=5.0,
        hvdc_ra_min_impact_threshold=22.0,
        injection_ra_min_impact_threshold=44.0,
    )
    topo_optimization_param = TopoOptimizationParameters(
        relative_min_impact_threshold=13.0,
        absolute_min_impact_threshold=32.0,
    )
    not_optimized_cnecs_parameters = NotOptimizedCnecsParameters(
        do_not_optimize_curative_cnecs_for_tsos_without_cras=True
    )
    parameters = RaoParameters(objective_function_parameters=objective_function_param,
                               range_action_optimization_parameters=range_action_optim_param,
                               topo_optimization_parameters=topo_optimization_param,
                               not_optimized_cnecs_parameters=not_optimized_cnecs_parameters)

    assert parameters.objective_function_parameters.objective_function_type == ObjectiveFunctionType.MIN_COST
    assert parameters.objective_function_parameters.unit == Unit.KILOVOLT
    assert parameters.objective_function_parameters.enforce_curative_security == True

    assert math.isclose(parameters.range_action_optimization_parameters.pst_ra_min_impact_threshold, 5.0)
    assert math.isclose(parameters.range_action_optimization_parameters.hvdc_ra_min_impact_threshold, 22.0)
    assert math.isclose(parameters.range_action_optimization_parameters.injection_ra_min_impact_threshold, 44.0)

    assert math.isclose(parameters.topo_optimization_parameters.relative_min_impact_threshold, 13.0)
    assert math.isclose(parameters.topo_optimization_parameters.absolute_min_impact_threshold, 32.0)

    assert parameters.not_optimized_cnecs_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras == True

def test_rao_parameters_from_files():
    parameters = RaoParameters.from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json"))
    assert parameters.objective_function_parameters.objective_function_type == ObjectiveFunctionType.MAX_MIN_MARGIN
    assert parameters.search_tree_parameters.range_action_parameters.max_mip_iterations == 10

def test_rao_parameters():
    # Full setup
    objective_function_param = ObjectiveFunctionParameters(
        objective_function_type=ObjectiveFunctionType.MIN_COST,
        unit=Unit.MEGAWATT,
        enforce_curative_security=True
    )
    range_action_optim_param = RangeActionOptimizationParameters(
        pst_ra_min_impact_threshold=5.0,
        hvdc_ra_min_impact_threshold=22.0,
        injection_ra_min_impact_threshold=44.0
    )
    topo_optimization_param = TopoOptimizationParameters(
        relative_min_impact_threshold=1.0,
        absolute_min_impact_threshold=32.0
    )
    not_optimized_cnecs_parameters = NotOptimizedCnecsParameters(
        do_not_optimize_curative_cnecs_for_tsos_without_cras=True
    )
    provider_parameters = {'MNEC_EXT_acceptable_margin_decrease': '10.2'}

    parameters = RaoParameters(
        objective_function_parameters=objective_function_param,
        range_action_optimization_parameters=range_action_optim_param,
        topo_optimization_parameters=topo_optimization_param,
        not_optimized_cnecs_parameters=not_optimized_cnecs_parameters,
        provider_parameters=provider_parameters
    )

    validate_rao_parameter(parameters)

    # Serialization / deserialization roundtrip
    validate_rao_parameter(RaoParameters.from_buffer_source(parameters.serialize_to_binary_buffer()))

def validate_rao_parameter(parameters: RaoParameters):
    assert parameters.objective_function_parameters.objective_function_type == ObjectiveFunctionType.MIN_COST
    assert parameters.objective_function_parameters.unit == Unit.MEGAWATT
    assert parameters.objective_function_parameters.enforce_curative_security == True

    assert math.isclose(parameters.range_action_optimization_parameters.pst_ra_min_impact_threshold, 5.0)
    assert math.isclose(parameters.range_action_optimization_parameters.hvdc_ra_min_impact_threshold, 22.0)
    assert math.isclose(parameters.range_action_optimization_parameters.injection_ra_min_impact_threshold, 44.0)

    assert math.isclose(parameters.topo_optimization_parameters.relative_min_impact_threshold, 1.0)
    assert math.isclose(parameters.topo_optimization_parameters.absolute_min_impact_threshold, 32.0)

    assert parameters.not_optimized_cnecs_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras == True

    assert math.isclose(float(parameters.provider_parameters['MNEC_EXT_acceptable_margin_decrease']), 10.2)

def test_rao_search_tree_parameters():
    search_tree_range_action_param = RangeActionSearchTreeParameters(
        max_mip_iterations=10,
        pst_sensitivity_threshold=12.0,
        pst_model=PstModel.CONTINUOUS,
        hvdc_sensitivity_threshold=46.0,
        injection_ra_sensitivity_threshold=33.0,
        ra_range_shrinking=RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO,
        solver=Solver.XPRESS,
        relative_mip_gap=22.0
    )
    search_tree_topo_parameters = TopoSearchTreeParameters(
        max_preventive_search_tree_depth=10,
        max_curative_search_tree_depth=45,
        skip_actions_far_from_most_limiting_element=False,
        max_number_of_boundaries_for_skipping_actions=6
    )
    custom_sensi_param = pypowsybl.sensitivity.Parameters()
    custom_sensi_param.load_flow_parameters.phase_shifter_regulation_on = True
    sensitivity_parameters = LoadFlowAndSensitivityParameters(
        sensitivity_parameters=custom_sensi_param,
        sensitivity_failure_overcost=32.0
    )
    second_preventive_params = SecondPreventiveRaoParameters(
        execution_condition=ExecutionCondition.COST_INCREASE,
        hint_from_first_preventive_rao=False
    )
    search_tree_parameters = RaoSearchTreeParameters(
        curative_min_obj_improvement=1.0,
        range_action_parameters=search_tree_range_action_param,
        topo_parameters=search_tree_topo_parameters,
        available_cpus=8,
        second_preventive_rao_parameters=second_preventive_params,
        loadflow_and_sensitivity_parameters=sensitivity_parameters
    )

    parameters = RaoParameters(
        search_tree_parameters=search_tree_parameters
    )

    validate_search_tree_parameters(parameters)

    # Serialization / deserialization roundtrip
    validate_search_tree_parameters(RaoParameters.from_buffer_source(parameters.serialize_to_binary_buffer()))

def validate_search_tree_parameters(parameters: RaoParameters):
    assert math.isclose(parameters.search_tree_parameters.curative_min_obj_improvement, 1.0)

    assert math.isclose(parameters.search_tree_parameters.range_action_parameters.max_mip_iterations, 10.0)
    assert math.isclose(parameters.search_tree_parameters.range_action_parameters.pst_sensitivity_threshold, 12.0)
    assert parameters.search_tree_parameters.range_action_parameters.pst_model == PstModel.CONTINUOUS
    assert math.isclose(parameters.search_tree_parameters.range_action_parameters.hvdc_sensitivity_threshold, 46.0)
    assert math.isclose(parameters.search_tree_parameters.range_action_parameters.injection_ra_sensitivity_threshold, 33.0)
    assert parameters.search_tree_parameters.range_action_parameters.ra_range_shrinking == RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO
    assert parameters.search_tree_parameters.range_action_parameters.solver == Solver.XPRESS
    assert math.isclose(parameters.search_tree_parameters.range_action_parameters.relative_mip_gap, 22.0)

    assert parameters.search_tree_parameters.topo_parameters.max_preventive_search_tree_depth == 10
    assert parameters.search_tree_parameters.topo_parameters.max_curative_search_tree_depth == 45
    assert parameters.search_tree_parameters.topo_parameters.skip_actions_far_from_most_limiting_element == False
    assert parameters.search_tree_parameters.topo_parameters.max_number_of_boundaries_for_skipping_actions == 6

    assert parameters.search_tree_parameters.available_cpus == 8

    assert parameters.search_tree_parameters.second_preventive_rao_parameters.execution_condition == ExecutionCondition.COST_INCREASE
    assert parameters.search_tree_parameters.second_preventive_rao_parameters.hint_from_first_preventive_rao == False

    assert math.isclose(parameters.search_tree_parameters.loadflow_and_sensitivity_parameters.sensitivity_failure_overcost, 32.0)
    assert parameters.search_tree_parameters.loadflow_and_sensitivity_parameters.sensitivity_parameters.load_flow_parameters.phase_shifter_regulation_on == True


def test_fast_rao_parameters():
    fast_rao_param = FastRaoParameters(number_of_cnecs_to_add=32,add_unsecure_cnecs=True, margin_limit=10.0)
    parameters = RaoParameters(
        fast_rao_parameters=fast_rao_param
    )

    validate_fast_rao_parameters(parameters)
    # Serialization / deserialization roundtrip
    validate_fast_rao_parameters(RaoParameters.from_buffer_source(parameters.serialize_to_binary_buffer()))

def validate_fast_rao_parameters(parameters: RaoParameters):
    assert parameters.fast_rao_parameters.number_of_cnecs_to_add == 32
    assert parameters.fast_rao_parameters.add_unsecure_cnecs == True
    assert math.isclose(parameters.fast_rao_parameters.margin_limit, 10.0)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_from_files(rao_provider: str):
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))

    rao_runner = pp.rao.create_rao()
    result = rao_runner.run(network=network,
                            parameters=RaoParameters.from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json")),
                            rao_provider=rao_provider,
                            crac=Crac.from_file_source(network, DATA_DIR.joinpath("rao/rao_crac.json")),
                            loop_flow_glsk=RaoGlsk.from_file_source(DATA_DIR.joinpath("rao/rao_glsk.xml")))
    assert RaoComputationStatus.DEFAULT == result.status()

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_from_buffers(rao_provider: str):
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))
    crac = Crac.from_buffer_source(network, io.BytesIO(open(DATA_DIR.joinpath("rao/rao_crac.json"), "rb").read()))
    glsk = RaoGlsk.from_file_source(DATA_DIR.joinpath("rao/rao_glsk.xml"))
    parameters = RaoParameters.from_buffer_source(io.BytesIO(open(DATA_DIR.joinpath("rao/rao_parameters.json"), "rb").read()))

    rao_runner = pp.rao.create_rao()
    result = rao_runner.run(crac, network, parameters, rao_provider=rao_provider, loop_flow_glsk=glsk)

    assert RaoComputationStatus.DEFAULT == result.status()
    json_result = result.to_json()

    assert json_result["computationStatus"] == "default"
    expected_keys = ['type', 'version', 'info', 'computationStatus', 'executionDetails', 'costResults',
                     'computationStatusMap', 'flowCnecResults', 'angleCnecResults', 'voltageCnecResults',
                     'networkActionResults', 'rangeActionResults']
    if rao_provider == "FastRao":
        expected_keys.append("extensions")
    assert list(json_result.keys()) == expected_keys

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_angle_monitoring_redispatching(rao_provider: str):
    """
    AngleCNECs are required in CRAC in order to run angle monitoring.
    """

    network = pp.network.load(DATA_DIR.joinpath("rao/monitoring.xiidm"))
    parameters = RaoParameters.from_file_source(DATA_DIR.joinpath("rao/monitoring_parameters.json"))
    load_flow_parameters = parameters.search_tree_parameters.loadflow_and_sensitivity_parameters.sensitivity_parameters.load_flow_parameters
    crac = Crac.from_file_source(network, DATA_DIR.joinpath("rao/angle_monitoring_crac_redispatching.json"))
    monitoring_glsk = RaoGlsk.from_file_source(DATA_DIR.joinpath("rao/GlskB45test.xml"))

    rao_runner = pp.rao.create_rao()
    result = rao_runner.run(crac, network, parameters, rao_provider=rao_provider)
    result_with_angle_monitoring = rao_runner.run_angle_monitoring(crac, network, result, load_flow_parameters, "OpenLoadFlow",
                                                                   monitoring_glsk=monitoring_glsk)

    check_rao_monitoring_result(result_with_angle_monitoring)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_angle_monitoring_with_results_from_file(rao_provider: str):
    """
    Same test as rao angle monitoring but without a rao run and with a result loaded from file
    """

    network = pp.network.load(DATA_DIR.joinpath("rao/monitoring.xiidm"))
    parameters = RaoParameters.from_file_source(DATA_DIR.joinpath("rao/monitoring_parameters.json"))
    crac = Crac.from_file_source(network, DATA_DIR.joinpath("rao/angle_monitoring_crac_redispatching.json"))

    rao_runner = pp.rao.create_rao()
    result_with_angle_monitoring = rao_runner.run_angle_monitoring(crac=crac, network=network,
                                                                   rao_result=RaoResult.from_file_source(crac, DATA_DIR.joinpath("rao/rao_result_for_monitoring.json")),
                                                                   load_flow_parameters=parameters.search_tree_parameters.loadflow_and_sensitivity_parameters.sensitivity_parameters.load_flow_parameters,
                                                                   provider_str="OpenLoadFlow",
                                                                   monitoring_glsk=RaoGlsk.from_file_source(DATA_DIR.joinpath("rao/GlskB45test.xml")))

    check_rao_monitoring_result(result_with_angle_monitoring)

def check_rao_monitoring_result(result: RaoResult):
    angle_cnec_results = result.get_angle_cnec_results()

    expected = pd.DataFrame(columns=['cnec_id', 'optimized_instant', 'contingency', 'angle', 'margin'],
                            data=[["acCur1", "curative", "coL1", -3.783208, 2.216792]])

    assert RaoComputationStatus.DEFAULT == result.status()
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), angle_cnec_results.reset_index(drop=True),
                                  check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_angle_monitoring_topological_action(rao_provider: str):
    """
    AngleCNECs are required in CRAC in order to run angle monitoring.
    """

    network = pp.network.load(DATA_DIR.joinpath("rao/monitoring.xiidm"))
    parameters = RaoParameters.from_file_source(DATA_DIR.joinpath("rao/monitoring_parameters.json"))
    load_flow_parameters = parameters.search_tree_parameters.loadflow_and_sensitivity_parameters.sensitivity_parameters.load_flow_parameters
    crac = Crac.from_file_source(network, DATA_DIR.joinpath("rao/angle_monitoring_crac_topological_action.json"))

    rao_runner = pp.rao.create_rao()
    monitoring_glsk = RaoGlsk.from_file_source(DATA_DIR.joinpath("rao/GlskB45test.xml"))

    result = rao_runner.run(crac, network, parameters, rao_provider=rao_provider)
    result_with_angle_monitoring = rao_runner.run_angle_monitoring(crac, network, result, load_flow_parameters, "OpenLoadFlow", monitoring_glsk=monitoring_glsk)
    angle_cnec_results = result_with_angle_monitoring.get_angle_cnec_results()

    expected = pd.DataFrame(columns=['cnec_id', 'optimized_instant', 'contingency', 'angle', 'margin'],
                            data=[["acCur1", "curative", "coL1", -7.713852, -4.713852]])

    assert RaoComputationStatus.DEFAULT == result_with_angle_monitoring.status()
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), angle_cnec_results.reset_index(drop=True),
                                  check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_voltage_monitoring(rao_provider: str):
    """
    VoltageCNECs are required in CRAC in order to run voltage monitoring.
    """

    network = pp.network.load(DATA_DIR.joinpath("rao/monitoring.xiidm"))
    parameters = RaoParameters.from_file_source(DATA_DIR.joinpath("rao/monitoring_parameters.json"))
    load_flow_parameters = parameters.search_tree_parameters.loadflow_and_sensitivity_parameters.sensitivity_parameters.load_flow_parameters
    crac = Crac.from_file_source(network, DATA_DIR.joinpath("rao/voltage_monitoring_crac.json"))

    rao_runner = pp.rao.create_rao()
    result = rao_runner.run(crac, network, parameters, rao_provider=rao_provider)
    result_with_voltage_monitoring = rao_runner.run_voltage_monitoring(crac, network, result, load_flow_parameters, "OpenLoadFlow")

    voltage_cnec_results = result_with_voltage_monitoring.get_voltage_cnec_results()

    expected = pd.DataFrame(columns=['cnec_id', 'optimized_instant', 'contingency', 'side', 'min_voltage', 'max_voltage', 'margin'],
                            data=[["vc", "curative", "coL1", "ONE", 363.622121, 363.622121, -13.622121]])

    assert RaoComputationStatus.DEFAULT == result_with_voltage_monitoring.status()
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), voltage_cnec_results.reset_index(drop=True),
                                  check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_cnec_results(rao_provider: str):
    result = run_rao_12_node_with_curative(rao_provider)

    # Flow cnecs
    df_flow_cnecs = result.get_flow_cnec_results()
    assert ['cnec_id', 'optimized_instant', 'contingency', 'side', 'flow', 'margin', 'relative_margin', 'commercial_flow', 'loop_flow', 'ptdf_zonal_sum'] == list(df_flow_cnecs.columns)
    nl_be_cnec = df_flow_cnecs.loc[df_flow_cnecs['cnec_id'] == 'NNL2AA1  BBE3AA1  1 - preventive']
    nl_be_cnec_side1 = nl_be_cnec.loc[nl_be_cnec['side'] == 'ONE'][['cnec_id', 'optimized_instant', 'flow', 'margin']]
    nl_be_cnec_side1 = nl_be_cnec_side1.sort_values(['optimized_instant'], ascending=[True])
    expected = pd.DataFrame(columns=['cnec_id', 'optimized_instant', 'flow', 'margin'],
                            data=[['NNL2AA1  BBE3AA1  1 - preventive', 'initial', 499.996955, -89.996955],
                                  ['NNL2AA1  BBE3AA1  1 - preventive', 'preventive', 211.496250, 198.503750]])
    expected = expected.sort_values(['optimized_instant'], ascending=[True])
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), nl_be_cnec_side1.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

    # Voltage cnecs
    df_voltage_cnecs = result.get_voltage_cnec_results()
    assert ['cnec_id', 'optimized_instant', 'contingency', 'side', 'min_voltage', 'max_voltage', 'margin'] == list(df_voltage_cnecs.columns)

    # Voltage cnecs
    df_angle_cnecs = result.get_angle_cnec_results()
    assert ['cnec_id', 'optimized_instant', 'contingency', 'angle', 'margin'] == list(df_angle_cnecs.columns)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_remedial_action_results(rao_provider: str):
    result = run_rao_12_node_with_curative(rao_provider)

    # Ra results
    ra_results = result.get_remedial_action_results()
    ra_results = ra_results.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True]) # Sort to avoid row order difference
    assert ['remedial_action_id', 'optimized_instant', 'contingency'] == list(ra_results.columns)
    expected = pd.DataFrame(columns=['remedial_action_id', 'optimized_instant', 'contingency'],
                            data=[['close NL2 BE3 2', 'preventive', ""],
                                  ['pst-range-action', 'curative', "Contingency DE2 DE3"],
                                  ['pst-range-action', 'preventive', ""]])
    expected = expected.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True]) # Sort to avoid row order difference
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), ra_results.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_network_action_results(rao_provider: str):
    result = run_rao_12_node_with_curative(rao_provider)

    # Ra results
    network_action_results = result.get_network_action_results()
    network_action_results = network_action_results.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True]) # Sort to avoid row order difference
    assert ['remedial_action_id', 'optimized_instant', 'contingency'] == list(network_action_results.columns)
    expected = pd.DataFrame(columns=['remedial_action_id', 'optimized_instant', 'contingency'],
                            data=[['close NL2 BE3 2', 'preventive', ""]])
    expected = expected.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True]) # Sort to avoid row order difference
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), network_action_results.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_pst_range_action_results(rao_provider: str):
    result = run_rao_12_node_with_curative(rao_provider)

    # Ra results
    pst_range_action_results = result.get_pst_range_action_results()
    pst_range_action_results = pst_range_action_results.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True])  # Sort to avoid row order difference
    assert ['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_tap'] == list(pst_range_action_results.columns)
    expected = pd.DataFrame(
        columns=['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_tap'],
        data=[['pst-range-action', 'curative', "Contingency DE2 DE3", 6],
              ['pst-range-action', 'preventive', "", -10]])
    expected = expected.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True])  # Sort to avoid row order difference
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), pst_range_action_results.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_range_action_results(rao_provider: str):
    result = run_rao_12_node_with_curative(rao_provider)

    # Ra results
    range_action_results = result.get_range_action_results()
    range_action_results = range_action_results.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True])  # Sort to avoid row order difference
    assert ['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_set_point'] == list(range_action_results.columns)
    expected = pd.DataFrame(
        columns=['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_set_point'],
        data=[])
    expected = expected.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True])  # Sort to avoid row order difference
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), range_action_results.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_cost_results(rao_provider: str):
    result = run_rao_12_node_with_curative(rao_provider)

    # Cost results
    cost_results_df = result.get_cost_results()
    assert ['functional_cost', 'virtual_cost', 'cost'] == list(cost_results_df.columns)
    expected = pd.DataFrame(index=pd.Series(name='optimized_instant', data=['initial', 'preventive', 'outage', 'curative']),
                            columns=['functional_cost', 'virtual_cost', 'cost'],
                            data=[[133.304310, 0.0, 133.304310],
                                  [237.646702, 0.0, 237.646702],
                                  [237.646702, 0.0, 237.646702],
                                  [-187.219238, 0.0, -187.219238]])
    pd.testing.assert_frame_equal(expected, cost_results_df, check_dtype=False, check_like=True)

    assert ['sensitivity-failure-cost'] == result.get_virtual_cost_names()

    # Virtual cost results
    virtual_cost_df = result.get_virtual_cost_results('sensitivity-failure-cost')
    expected_virtual = pd.DataFrame(index=pd.Series(name='optimized_instant', data=['initial', 'preventive', 'outage', 'curative']),
                            columns=['sensitivity-failure-cost'],
                            data=[[0.0],
                                  [0.0],
                                  [0.0],
                                  [0.0]])
    pd.testing.assert_frame_equal(expected_virtual, virtual_cost_df, check_dtype=False, check_like=True)

@pytest.mark.parametrize("rao_provider", RAO_PROVIDERS)
def test_rao_log_filter(rao_provider: str):
    logger = logging.getLogger('powsybl')
    logger.setLevel(logging.INFO)
    logger.addFilter(RaoLogFilter())

    # Redirect to queue
    q = queue.Queue()
    handler = handlers.QueueHandler(q)
    logger.addHandler(handler)

    run_rao_12_node_with_curative(rao_provider)

    # Only open rao logs
    for i in range(len(q.queue)):
        assert("com.powsybl.openrao" in q.queue[i].java_logger_name)


def run_rao_12_node_with_curative(rao_provider: str):
    network =  pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    rao_runner = pp.rao.create_rao()
    return rao_runner.run(Crac.from_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json")),
                          network,
                          parameters=RaoParameters.from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json")),
                          rao_provider=rao_provider)

def parameters_round_trip():
    # Load from file
    initial_buffer = io.BytesIO(open(DATA_DIR.joinpath("rao/rao_parameters_non_default.json"), "rb").read())
    params = RaoParameters.from_buffer_source(initial_buffer)

    # Serialize
    serialized_params = params.serialize_to_binary_buffer()

    # Compare serialized to initial buffer
    assert initial_buffer.getvalue() == serialized_params.getvalue()

if __name__ == '__main__':
    unittest.main()