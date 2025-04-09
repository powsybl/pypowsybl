#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import io
import unittest

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
        max_auto_search_tree_depth=22,
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
    assert parameters2.topo_optimization_parameters.max_auto_search_tree_depth == 22
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


if __name__ == '__main__':
    unittest.main()