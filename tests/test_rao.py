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
from pypowsybl.rao import Parameters as RaoParameters
from pypowsybl.loadflow import Parameters as LfParameters
from pypowsybl.rao import (
    ObjectiveFunctionParameters,
    RangeActionOptimizationParameters,
    TopoOptimizationParameters,
    MultithreadingParameters,
    SecondPreventiveRaoParameters,
    NotOptimizedCnecsParameters,
    LoadFlowAndSensitivityParameters)

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'

def test_default_rao_parameters():
    parameters = RaoParameters()
    assert parameters.objective_function_parameters.objective_function_type == ObjectiveFunctionType.SECURE_FLOW

def test_rao_parameters():
    # Default
    parameters = RaoParameters()
    assert parameters.range_action_optimization_parameters.max_mip_iterations == 10

    # From file
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json"))
    assert parameters.range_action_optimization_parameters.max_mip_iterations == 10
    assert parameters.objective_function_parameters.objective_function_type == ObjectiveFunctionType.MAX_MIN_MARGIN

    # Full setup
    objective_function_param = ObjectiveFunctionParameters(
        objective_function_type=ObjectiveFunctionType.MIN_COST,
        unit=Unit.MEGAWATT,
        curative_min_obj_improvement=1.0,
        enforce_curative_security=True
    )

    range_action_optim_param = RangeActionOptimizationParameters(
        max_mip_iterations=10,
        pst_ra_min_impact_threshold=5.0,
        pst_sensitivity_threshold=12.0,
        pst_model=PstModel.CONTINUOUS,
        hvdc_ra_min_impact_threshold=22.0,
        hvdc_sensitivity_threshold=46.0,
        injection_ra_min_impact_threshold=44.0,
        injection_ra_sensitivity_threshold=33.0,
        ra_range_shrinking=RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO,
        solver=Solver.XPRESS,
        relative_mip_gap=22.0
    )

    topo_optimization_param = TopoOptimizationParameters(
        max_preventive_search_tree_depth=10,
        max_curative_search_tree_depth=45,
        relative_min_impact_threshold=13.0,
        absolute_min_impact_threshold=32.0,
        skip_actions_far_from_most_limiting_element=False,
        max_number_of_boundaries_for_skipping_actions=6
    )

    multithreading_param = MultithreadingParameters(
        available_cpus=8
    )

    second_preventive_params = SecondPreventiveRaoParameters(
        execution_condition=ExecutionCondition.COST_INCREASE,
        re_optimize_curative_range_actions=False,
        hint_from_first_preventive_rao=False
    )

    not_optimized_cnecs_parameters = NotOptimizedCnecsParameters(
        do_not_optimize_curative_cnecs_for_tsos_without_cras=True
    )

    custom_sensi_param = pypowsybl.sensitivity.Parameters()
    custom_sensi_param.load_flow_parameters.phase_shifter_regulation_on = True
    sensitivity_parameters = LoadFlowAndSensitivityParameters(
        sensitivity_parameters=custom_sensi_param,
        sensitivity_failure_overcost=32.0
    )

    parameters2 = RaoParameters(
        objective_function_parameters=objective_function_param,
        range_action_optimization_parameters=range_action_optim_param,
        topo_optimization_parameters=topo_optimization_param,
        multithreading_parameters=multithreading_param,
        second_preventive_rao_parameters=second_preventive_params,
        not_optimized_cnecs_parameters=not_optimized_cnecs_parameters,
        loadflow_and_sensitivity_parameters=sensitivity_parameters
    )

    assert parameters2.objective_function_parameters.objective_function_type == ObjectiveFunctionType.MIN_COST
    assert parameters2.objective_function_parameters.unit == Unit.MEGAWATT
    assert parameters2.objective_function_parameters.curative_min_obj_improvement == 1.0
    assert parameters2.objective_function_parameters.enforce_curative_security == True

    assert parameters2.range_action_optimization_parameters.max_mip_iterations == 10.0
    assert parameters2.range_action_optimization_parameters.pst_ra_min_impact_threshold == 5.0
    assert parameters2.range_action_optimization_parameters.pst_sensitivity_threshold == 12.0
    assert parameters2.range_action_optimization_parameters.pst_model == PstModel.CONTINUOUS
    assert parameters2.range_action_optimization_parameters.hvdc_ra_min_impact_threshold == 22.0
    assert parameters2.range_action_optimization_parameters.hvdc_sensitivity_threshold == 46.0
    assert parameters2.range_action_optimization_parameters.injection_ra_min_impact_threshold == 44.0
    assert parameters2.range_action_optimization_parameters.injection_ra_sensitivity_threshold == 33.0
    assert parameters2.range_action_optimization_parameters.ra_range_shrinking == RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO
    assert parameters2.range_action_optimization_parameters.solver == Solver.XPRESS
    assert parameters2.range_action_optimization_parameters.relative_mip_gap == 22.0

    assert parameters2.topo_optimization_parameters.max_preventive_search_tree_depth == 10
    assert parameters2.topo_optimization_parameters.max_curative_search_tree_depth == 45
    assert parameters2.topo_optimization_parameters.relative_min_impact_threshold == 13.0
    assert parameters2.topo_optimization_parameters.absolute_min_impact_threshold == 32.0
    assert parameters2.topo_optimization_parameters.skip_actions_far_from_most_limiting_element == False
    assert parameters2.topo_optimization_parameters.max_number_of_boundaries_for_skipping_actions == 6

    assert parameters2.multithreading_parameters.available_cpus == 8

    assert parameters2.second_preventive_rao_parameters.execution_condition == ExecutionCondition.COST_INCREASE
    assert parameters2.second_preventive_rao_parameters.re_optimize_curative_range_actions == False
    assert parameters2.second_preventive_rao_parameters.hint_from_first_preventive_rao == False

    assert parameters2.not_optimized_cnecs_parameters.do_not_optimize_curative_cnecs_for_tsos_without_cras == True

    assert parameters2.loadflow_and_sensitivity_parameters.sensitivity_failure_overcost == 32.0
    assert parameters2.loadflow_and_sensitivity_parameters.sensitivity_parameters.load_flow_parameters.phase_shifter_regulation_on == True

def test_rao_from_files():
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/rao_crac.json"))
    rao_runner.set_glsk_file_source(network, DATA_DIR.joinpath("rao/rao_glsk.xml"))
    result = rao_runner.run(network, parameters)
    assert RaoComputationStatus.DEFAULT == result.status()

def test_rao_from_buffers():
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))
    crac = io.BytesIO(open(DATA_DIR.joinpath("rao/rao_crac.json"), "rb").read())
    glsks = io.BytesIO(open(DATA_DIR.joinpath("rao/rao_glsk.xml"), "rb").read())

    parameters = RaoParameters()
    parameters.load_from_buffer_source(
        io.BytesIO(open(DATA_DIR.joinpath("rao/rao_parameters.json"), "rb").read()))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_buffer_source(network, crac)
    rao_runner.set_glsk_buffer_source(network, glsks)
    result = rao_runner.run(network, parameters)
    assert RaoComputationStatus.DEFAULT == result.status()
    json_result = result.to_json()

    assert json_result["computationStatus"] == "default"
    assert list(json_result.keys()) == ['type', 'version', 'info', 'computationStatus', 'executionDetails', 'costResults',
                                    'computationStatusMap', 'flowCnecResults', 'angleCnecResults', 'voltageCnecResults',
                                    'networkActionResults', 'rangeActionResults']

def test_rao_monitoring():
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/rao_crac.json"))
    rao_runner.set_glsk_file_source(network, DATA_DIR.joinpath("rao/rao_glsk.xml"))
    result = rao_runner.run(network, parameters)

    result_with_voltage_monitoring = rao_runner.run_voltage_monitoring(network, result, LfParameters())
    assert RaoComputationStatus.DEFAULT == result_with_voltage_monitoring.status()

    result_with_angle_monitoring = rao_runner.run_angle_monitoring(network, result, LfParameters())
    assert RaoComputationStatus.DEFAULT == result_with_angle_monitoring.status()

def test_rao_cnec_results():
    network =  pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json"))
    result = rao_runner.run(network, parameters)

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

def test_rao_remedial_action_results():
    network =  pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json"))

    result = rao_runner.run(network, parameters)

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

def test_rao_network_action_results():
    network =  pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json"))

    result = rao_runner.run(network, parameters)

    # Ra results
    network_action_results = result.get_network_action_results()
    network_action_results = network_action_results.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True]) # Sort to avoid row order difference
    assert ['remedial_action_id', 'optimized_instant', 'contingency'] == list(network_action_results.columns)
    expected = pd.DataFrame(columns=['remedial_action_id', 'optimized_instant', 'contingency'],
                            data=[['close NL2 BE3 2', 'preventive', ""]])
    expected = expected.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True]) # Sort to avoid row order difference
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), network_action_results.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

def test_rao_pst_range_action_results():
    network = pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json"))

    result = rao_runner.run(network, parameters)

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

def test_rao_range_action_results():
    network = pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json"))

    result = rao_runner.run(network, parameters)

    # Ra results
    range_action_results = result.get_range_action_results()
    range_action_results = range_action_results.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True])  # Sort to avoid row order difference
    assert ['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_set_point'] == list(range_action_results.columns)
    expected = pd.DataFrame(
        columns=['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_set_point'],
        data=[])
    expected = expected.sort_values(['remedial_action_id', 'optimized_instant'], ascending=[True, True])  # Sort to avoid row order difference
    pd.testing.assert_frame_equal(expected.reset_index(drop=True), range_action_results.reset_index(drop=True), check_dtype=False, check_index_type=False, check_like=True)

def test_rao_cost_results():
    network =  pp.network.load(DATA_DIR.joinpath("rao/12_node_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters_with_curative.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/N-1_case_crac_curative.json"))

    result = rao_runner.run(network, parameters)

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

if __name__ == '__main__':
    unittest.main()