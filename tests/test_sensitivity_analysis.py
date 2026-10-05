#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import pytest
import pypowsybl as pp
import pandas as pd
import re
from pypowsybl import PyPowsyblError
import pypowsybl.report as rp
from pypowsybl.sensitivity import SensitivityFunctionType, SensitivityVariableType, ContingencyContextType

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')


@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)


def test_config():
    assert 'OpenLoadFlow' == pp.sensitivity.get_default_provider()
    pp.sensitivity.set_default_provider("provider")
    assert 'provider' == pp.sensitivity.get_default_provider()
    n = pp.network.create_ieee14()
    generators = pd.DataFrame(data=[4999.0, 4999.0, 4999.0, 4999.0, 4999.0],
                              columns=['max_p'], index=['B1-G', 'B2-G', 'B3-G', 'B6-G', 'B8-G'])
    n.update_generators(generators)
    sa = pp.sensitivity.create_dc_analysis()
    sa.add_single_element_contingency('L1-2-1')
    sa.add_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
    with pytest.raises(PyPowsyblError, match='No sensitivity analysis provider for name \'provider\''):
        sa.run(n)
    r = sa.run(n, provider='OpenLoadFlow')
    assert 6 == r.get_sensitivity_matrix().size
    assert 'provider' == pp.sensitivity.get_default_provider()
    pp.sensitivity.set_default_provider('OpenLoadFlow')
    assert 'OpenLoadFlow' == pp.sensitivity.get_default_provider()


def test_sensitivity_analysis():
    n = pp.network.create_ieee14()
    # fix max_p to be less than olf max_p plausible value
    # (otherwise generators will be discarded from slack distribution)
    generators = pd.DataFrame(data=[4999.0, 4999.0, 4999.0, 4999.0, 4999.0],
                              columns=['max_p'], index=['B1-G', 'B2-G', 'B3-G', 'B6-G', 'B8-G'])
    n.update_generators(generators)
    sa = pp.sensitivity.create_dc_analysis()
    sa.add_single_element_contingency('L1-2-1')
    sa.add_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'], 'm')
    sa.add_precontingency_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G'], 'preContingency')
    sa.add_postcontingency_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G'], ['L1-2-1'], 'postContingency')
    r = sa.run(n)

    df = r.get_sensitivity_matrix('m')
    assert (3, 2) == df.shape
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.080991, abs=1e-6)
    assert df['L1-5-1']['B2-G'] == pytest.approx(-0.080991, abs=1e-6)
    assert df['L1-5-1']['B3-G'] == pytest.approx(-0.172497, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.013675, abs=1e-6)
    assert df['L2-3-1']['B2-G'] == pytest.approx(0.013675, abs=1e-6)
    assert df['L2-3-1']['B3-G'] == pytest.approx(-0.545682, abs=1e-6)

    df = r.get_reference_matrix('m')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['reference_values'] == pytest.approx(72.246, abs=1e-3)
    assert df['L2-3-1']['reference_values'] == pytest.approx(69.831, abs=1e-3)

    df = r.get_sensitivity_matrix('m', 'L1-2-1')
    assert df.shape == (3, 2)
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.5, abs=1e-6)
    assert df['L1-5-1']['B2-G'] == pytest.approx(-0.5, abs=1e-6)
    assert df['L1-5-1']['B3-G'] == pytest.approx(-0.5, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.084423, abs=1e-6)
    assert df['L2-3-1']['B2-G'] == pytest.approx(0.084423, abs=1e-6)
    assert df['L2-3-1']['B3-G'] == pytest.approx(-0.490385, abs=1e-6)

    df = r.get_reference_matrix('m', 'L1-2-1')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['reference_values'] == pytest.approx(225.7, abs=1e-3)
    assert df['L2-3-1']['reference_values'] == pytest.approx(43.921, abs=1e-3)

    assert r.get_sensitivity_matrix('m', 'aaa') is None

    df = r.get_sensitivity_matrix('preContingency')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.080991, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.013675, abs=1e-6)
    df = r.get_sensitivity_matrix('postContingency', 'L1-2-1')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.5, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.084423, abs=1e-6)


def test_voltage_sensitivities():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.sensitivity.create_ac_analysis()
    sa.add_bus_voltage_factor_matrix(['VLGEN_0'], ['GEN'])
    r = sa.run(n)
    df = r.get_sensitivity_matrix()
    assert df.shape == (1, 1)
    assert df['VLGEN_0']['GEN'] == pytest.approx(1.0, abs=1e-6)


def test_create_zone():
    n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))

    zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
    assert len(zone_fr.injections_ids) == 3
    assert zone_fr.injections_ids == ['FFR1AA1 _generator', 'FFR2AA1 _generator', 'FFR3AA1 _generator']
    with pytest.raises(PyPowsyblError) as exc:
        zone_fr.get_shift_key('AA')
    assert zone_fr.get_shift_key('FFR2AA1 _generator') == 2000

    zone_fr = pp.sensitivity.create_country_zone(n, 'FR', pp.sensitivity.ZoneKeyType.GENERATOR_MAX_P)
    assert len(zone_fr.injections_ids) == 3
    assert zone_fr.get_shift_key('FFR2AA1 _generator') == 4999

    zone_fr = pp.sensitivity.create_country_zone(n, 'FR', pp.sensitivity.ZoneKeyType.LOAD_P0)
    assert len(zone_fr.injections_ids) == 3
    assert zone_fr.injections_ids == ['FFR1AA1 _load', 'FFR2AA1 _load', 'FFR3AA1 _load']
    assert zone_fr.get_shift_key('FFR1AA1 _load') == 1000

    zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
    zone_be = pp.sensitivity.create_country_zone(n, 'BE')

    # remove test
    assert len(zone_fr.injections_ids) == 3
    zone_fr.remove_injection('FFR1AA1 _generator')
    assert len(zone_fr.injections_ids) == 2

    # add test
    zone_fr.add_injection('gen', 333)
    assert len(zone_fr.injections_ids) == 3
    assert zone_fr.get_shift_key('gen') == 333

    # move test
    zone_fr.move_injection_to(zone_be, 'gen')
    assert len(zone_fr.injections_ids) == 2
    assert len(zone_be.injections_ids) == 4
    assert zone_be.get_shift_key('gen') == 333


def test_sensi_zone():
    n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))
    zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
    zone_be = pp.sensitivity.create_country_zone(n, 'BE')
    sa = pp.sensitivity.create_dc_analysis()
    sa.set_zones([zone_fr, zone_be])
    sa.add_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1', 'FFR2AA1  DDE3AA1  1'], ['FR', 'BE'], 'm')
    result = sa.run(n)
    s = result.get_sensitivity_matrix('m')
    assert s.shape == (2, 2)
    assert s['BBE2AA1  FFR3AA1  1']['FR'] == pytest.approx(-0.379829, abs=1e-6)
    assert s['FFR2AA1  DDE3AA1  1']['FR'] == pytest.approx(0.370171, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['BE'] == pytest.approx(0.378423, abs=1e-6)
    assert s['FFR2AA1  DDE3AA1  1']['BE'] == pytest.approx(0.128423, abs=1e-6)
    r = result.get_reference_matrix('m')
    assert r.shape == (1, 2)
    assert r['BBE2AA1  FFR3AA1  1']['reference_values'] == pytest.approx(324.666, abs=1e-3)
    assert r['FFR2AA1  DDE3AA1  1']['reference_values'] == pytest.approx(1324.666, abs=1e-3)


def test_sensi_power_transfer():
    n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))
    zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
    zone_de = pp.sensitivity.create_country_zone(n, 'DE')
    zone_be = pp.sensitivity.create_country_zone(n, 'BE')
    zone_nl = pp.sensitivity.create_country_zone(n, 'NL')
    sa = pp.sensitivity.create_dc_analysis()
    sa.set_zones([zone_fr, zone_de, zone_be, zone_nl])
    sa.add_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1', 'FFR2AA1  DDE3AA1  1'],
                                     ['FR', ('FR', 'DE'), ('DE', 'FR'), 'NL'], 'm')
    result = sa.run(n)
    s = result.get_sensitivity_matrix('m')
    assert s.shape == (4, 2)
    assert s['BBE2AA1  FFR3AA1  1']['FR'] == pytest.approx(-0.379829, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['FR -> DE'] == pytest.approx(-0.256641, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['DE -> FR'] == pytest.approx(0.256641, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['NL'] == pytest.approx(0.103426, abs=1e-6)


def test_xnode_sensi():
    n = pp.network.load(str(DATA_DIR.joinpath('simple-eu-xnode.uct')))
    # assert there is one boundary line (corresponding to the UCTE xnode)
    boundary_lines = n.get_boundary_lines()
    assert len(boundary_lines) == 1
    # create a new zone with only one xnode, this is the boundary line id that has to be configured (corresponding
    # to the line connecting the xnode in the UCTE file)
    zone_x = pp.sensitivity.create_empty_zone("X")
    zone_x.add_injection('NNL2AA1  XXXXXX11 1')
    sa = pp.sensitivity.create_dc_analysis()
    sa.set_zones([zone_x])
    sa.add_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1'], ['X'], 'm')
    result = sa.run(n)
    s = result.get_sensitivity_matrix('m')
    assert s.shape == (1, 1)
    assert s['BBE2AA1  FFR3AA1  1']['X'] == pytest.approx(0.176618, abs=1e-6)


def test_variant():
    n = pp.network.create_ieee14()
    # fix max_p to be less than olf max_p plausible value
    # (otherwise generators will be discarded from slack distribution)
    generators = pd.DataFrame(data=[4999.0, 4999.0, 4999.0, 4999.0, 4999.0],
                              columns=['max_p'], index=['B1-G', 'B2-G', 'B3-G', 'B6-G', 'B8-G'])
    n.update_generators(generators)
    sa = pp.sensitivity.create_dc_analysis()
    sa.add_branch_flow_factor_matrix(['L1-5-1'], ['B1-G'], 'm')
    r = sa.run(n)

    df = r.get_sensitivity_matrix('m')
    assert (1, 1) == df.shape
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.080991, abs=1e-6)

    n.clone_variant(n.get_working_variant_id(), 'variant_2')
    n.set_working_variant('variant_2')
    n.update_lines(id='L2-3-1', connected1=False)
    r = sa.run(n)

    df = r.get_sensitivity_matrix('m')
    assert (1, 1) == df.shape
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.078151, abs=1e-6)


def test_provider_names():
    assert 'OpenLoadFlow' in pp.sensitivity.get_provider_names()


def test_no_output_matrices_available():
    network = pp.network.create_eurostag_tutorial_example1_network()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_branch_flow_factor_matrix(network.get_lines().index.to_list(),
                                           network.get_generators().index.to_list())
    result = analysis.run(network)
    df = result.get_sensitivity_matrix('default')
    assert (2, 2) == df.shape

    with pytest.raises(pp.PyPowsyblError) as errorContext:
        result.get_sensitivity_matrix('')
    assert 'Matrix \'\' not found' == str(errorContext.value)


def test_provider_parameters():
    # setting max iterations to 5 will cause the computation to fail, if correctly taken into account
    parameters = pp.loadflow.Parameters(distributed_slack=False,
                                        provider_parameters={'maxNewtonRaphsonIterations': '1'})
    n = pp.network.create_eurostag_tutorial_example1_network()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_branch_flow_factor_matrix(['NHV1_NHV2_1'], ['GEN'])
    with pytest.raises(pp.PyPowsyblError, match='Initial load flow of base situation ended with solver status MAX_ITERATION_REACHED'):
        analysis.run(n, parameters)
    # does not throw
    result = analysis.run(n)
    assert result.get_reference_matrix().loc['reference_values', 'NHV1_NHV2_1'] == pytest.approx(302.45, abs=0.01)


def test_voltage_sensitivities_with_report():
    report_node = rp.ReportNode()
    report1 = str(report_node)
    assert len(report1) > 0
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.sensitivity.create_ac_analysis()
    sa.add_bus_voltage_factor_matrix(['VLGEN_0'], ['GEN'])
    sa.run(n, report_node=report_node)
    report2 = str(report_node)
    assert len(report2) > len(report1)


def test_sensitivity_parameters():
    n = pp.network.create_eurostag_tutorial_example1_network()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_branch_flow_factor_matrix(['NHV1_NHV2_1'], ['GEN'])

    # 1. distributing on generators
    parameters = pp.sensitivity.Parameters()
    parameters.load_flow_parameters.distributed_slack = True
    parameters.load_flow_parameters.balance_type = pp.loadflow.BalanceType.PROPORTIONAL_TO_GENERATION_P
    result = analysis.run(n, parameters)
    assert result.get_reference_matrix().loc['reference_values', 'NHV1_NHV2_1'] == pytest.approx(302.45, abs=0.01)
    assert result.get_sensitivity_matrix().loc['GEN', 'NHV1_NHV2_1'] == 0

    # 2. distributing on loads
    parameters.load_flow_parameters.balance_type = pp.loadflow.BalanceType.PROPORTIONAL_TO_LOAD
    result = analysis.run(n, parameters)
    assert result.get_reference_matrix().loc['reference_values', 'NHV1_NHV2_1'] == pytest.approx(605.35, abs=0.01)
    assert result.get_sensitivity_matrix().loc['GEN', 'NHV1_NHV2_1'] == pytest.approx(0.53, abs=0.01)


def test_provider_parameters_names():
    assert pp.sensitivity.get_provider_parameters_names() == ['debugDir', 'startWithFrozenACEmulation', 'threadCount']
    assert pp.sensitivity.get_provider_parameters_names('OpenLoadFlow') == ['debugDir', 'startWithFrozenACEmulation', 'threadCount']
    with pytest.raises(pp.PyPowsyblError, match='No sensitivity analysis provider for name \'unknown\''):
        pp.sensitivity.get_provider_parameters_names('unknown')


def test_hvdc():
    network = pp.network.create_four_substations_node_breaker_network()
    analysis = pp.sensitivity.create_dc_analysis()
    analysis.add_branch_flow_factor_matrix(["LINE_S2S3"], ["HVDC1"])
    results = analysis.run(network)
    assert {'default': ['HVDC1']} == results.function_data_frame_index
    assert {'default': ['LINE_S2S3']} == results.functions_ids
    pytest.approx(results.get_sensitivity_matrix().loc['HVDC1']['LINE_S2S3'], 0.7824, 0.001)


def test_add_branch_factor_matrix():
    network = pp.network.create_four_substations_node_breaker_network()
    analysis = pp.sensitivity.create_ac_analysis()

    analysis.add_factor_matrix(['LINE_S3S4'], ['GTH2'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_REACTIVE_POWER_1, SensitivityVariableType.BUS_TARGET_VOLTAGE, 'test')
    analysis.add_factor_matrix(['LINE_S2S3'], ['GTH1'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.INJECTION_ACTIVE_POWER, 'test1')
    analysis.add_factor_matrix(['LINE_S3S4'], ['GTH2'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_CURRENT_1, SensitivityVariableType.BUS_TARGET_VOLTAGE, 'test2')
    result = analysis.run(network)
    assert 30.5280 == pytest.approx(result.get_sensitivity_matrix('test').loc['GTH2']['LINE_S3S4'], 1e-4)
    assert 0.8 == result.get_sensitivity_matrix('test1').loc['GTH1']['LINE_S2S3']
    assert -0.4668 == pytest.approx(result.get_sensitivity_matrix('test2').loc['GTH2']['LINE_S3S4'], 1e-4)


def test_busbar_section_sensi():
    network = pp.network.create_four_substations_node_breaker_network()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['LINE_S2S3'], ['S2VL1_BBS'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_2, SensitivityVariableType.INJECTION_ACTIVE_POWER, 'm1')
    result = analysis.run(network)
    assert -0.8 == pytest.approx(result.get_sensitivity_matrix('m1').loc['S2VL1_BBS']['LINE_S2S3'], 1e-4)


@pytest.mark.parametrize('network_factory, bus_id, target_voltage_id', [
    (pp.network.create_eurostag_tutorial_example1_network, 'VLGEN_0', 'GEN'),
    (pp.network.create_eurostag_tutorial_example1_network, 'NGEN', 'GEN'),
    (pp.network.create_four_substations_node_breaker_network, 'S3VL1_0', 'GTH2'),
    (pp.network.create_four_substations_node_breaker_network, 'S3VL1_BBS', 'GTH2'),
])
def test_bus_voltage_sensi_id_resolution(network_factory, bus_id, target_voltage_id):
    network = network_factory()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_bus_voltage_factor_matrix([bus_id], [target_voltage_id])
    result = analysis.run(network)
    df = result.get_sensitivity_matrix()
    assert df.shape == (1, 1)
    assert df[bus_id][target_voltage_id] == pytest.approx(1.0, abs=1e-6)

def test_transfo3_sensi():
    network = pp.network.create_micro_grid_be_network()
    t3e_id = network.get_3_windings_transformers().head(1).index[0]
    line_id = network.get_lines().head(1).index[0]
    # Adding a phase tap changer to the three windings transformer leg one
    ptc_df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': t3e_id,
               'tap': 1,
               'low_tap': 0,
               'side': 'ONE'}])
    step_df = pd.DataFrame.from_records(
        index='id',
        data=[{'id': t3e_id, 'b': 2, 'g': 2, 'alpha': 2},
              {'id': t3e_id, 'b': 2, 'g': 2, 'alpha': 4}])
    network.create_phase_tap_changers(ptc_df, step_df)

    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_branch_flow_factor_matrix([line_id], [t3e_id], "ptc_test")
    result = analysis.run(network)
    assert -0.002685 == pytest.approx(result.get_sensitivity_matrix('ptc_test').loc[t3e_id][line_id], 1e-3)

def test_shunt_sensi():
    n = pp.network.create_ieee14()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['VL1_0', 'VL4_0', 'VL9_0'], ['B9-SH'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.SHUNT_COMPENSATOR_SUSCEPTANCE, 'm1')
    result = analysis.run(n)
    assert 0.0 == pytest.approx(result.get_sensitivity_matrix('m1').loc['B9-SH']['VL1_0'], 1e-3)
    assert 3.577598 == pytest.approx(result.get_sensitivity_matrix('m1').loc['B9-SH']['VL4_0'], 1e-3)
    assert 2.062478 == pytest.approx(result.get_sensitivity_matrix('m1').loc['B9-SH']['VL9_0'], 1e-3)


def test_branch_parameter_sensi():
    n = pp.network.create_eurostag_tutorial_example1_network()
    line = 'NHV1_NHV2_1'
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix([line], [line], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.BRANCH_RESISTANCE, 'r')
    analysis.add_factor_matrix([line], [line], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.BRANCH_REACTANCE, 'x')
    analysis.add_factor_matrix([line], [line], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.BRANCH_ADMITTANCE, 'y')
    result = analysis.run(n)
    assert 1.880628 == pytest.approx(result.get_sensitivity_matrix('r').loc[line][line], 1e-3)
    assert -4.719600 == pytest.approx(result.get_sensitivity_matrix('x').loc[line][line], 1e-3)
    assert 4973.888550 == pytest.approx(result.get_sensitivity_matrix('y').loc[line][line], 1e-3)


def test_svc_pilot_point_sensi():
    n = pp.network.create_ieee14()
    zones = pd.DataFrame.from_records(index='name',
                                      data=[{'name': 'z1', 'target_v': 12.7, 'bus_ids': 'B10'}])
    units = pd.DataFrame.from_records(index='unit_id',
                                      data=[{'unit_id': 'B6-G', 'zone_name': 'z1', 'participate': True},
                                            {'unit_id': 'B8-G', 'zone_name': 'z1', 'participate': True}])
    n.create_extensions('secondaryVoltageControl', [zones, units])

    params = pp.sensitivity.Parameters()
    params.load_flow_parameters.use_reactive_limits = False
    params.load_flow_parameters.provider_parameters = {'secondaryVoltageControl': 'true',
                                                        'maxPlausibleTargetVoltage': '1.6'}
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['B10', 'VL6_0', 'VL8_0'], ['z1'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.SVC_PILOT_POINT_TARGET_VOLTAGE, 'm1')
    result = analysis.run(n, params)
    df = result.get_sensitivity_matrix('m1')
    assert 1.0 == pytest.approx(df.loc['z1']['B10'], 1e-3)
    assert 1.112380 == pytest.approx(df.loc['z1']['VL6_0'], 1e-3)
    assert 2.559211 == pytest.approx(df.loc['z1']['VL8_0'], 1e-3)


def _adjoint_lf_parameters():
    # run_adjoint reuses the AC load flow OpenLoadFlow retains in its network cache, so the warm-up run and
    # the adjoint run must agree on the load flow parameters.
    return pp.loadflow.Parameters(distributed_slack=False,
                                  provider_parameters={'networkCacheEnabled': 'true'})


def _ieee14_with_warm_cache(params):
    n = pp.network.create_ieee14()
    pp.loadflow.run_ac(n, params)
    return n


def test_sensitivity_adjoint_reproduces_the_forward_sensitivity():
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1'], ['B2-G', 'B3-G', 'B6-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'm')

    # a unit cotangent on the single monitored flow makes theta_bar = S^T . e, i.e. the forward S column
    gradient = analysis.run_adjoint(n, [1.0], params).get_gradient('m')
    forward = analysis.run(n, params).get_sensitivity_matrix('m')['L1-2-1']

    assert list(gradient.index) == ['B2-G', 'B3-G', 'B6-G']
    pd.testing.assert_series_equal(forward, gradient, check_names=False, atol=1e-10, rtol=1e-10)
    assert gradient.abs().max() > 0.1


def test_sensitivity_adjoint_bus_voltage_to_shunt_susceptance():
    # the shunt-lever wrapper, on a BUS_VOLTAGE function: exercises the bus id convention too, since a
    # BUS_VOLTAGE function id is resolved to its bus-view bus before reaching OpenLoadFlow
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_shunt_susceptance_factor_matrix(['VL10_0'], ['B9-SH'], SensitivityFunctionType.BUS_VOLTAGE, 'm')

    gradient = analysis.run_adjoint(n, [1.0], params).get_gradient('m')
    forward = analysis.run(n, params).get_sensitivity_matrix('m')['VL10_0']

    pd.testing.assert_series_equal(forward, gradient, check_names=False, atol=1e-10, rtol=1e-10)
    assert gradient['B9-SH'] > 0.1


def test_sensitivity_adjoint_flat_and_per_matrix_cotangents_agree():
    # two factor matrices: the flat vector is their columns concatenated in declaration order, which is the
    # offset layout the dict form is flattened into
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1', 'L1-5-1'], ['B2-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'first')
    analysis.add_factor_matrix(['L2-3-1'], ['B3-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'second')

    flat = analysis.run_adjoint(n, [0.75, -1.5, 0.4], params)
    per_matrix = analysis.run_adjoint(n, {'first': [0.75, -1.5], 'second': [0.4]}, params)
    for matrix_id in ('first', 'second'):
        pd.testing.assert_series_equal(flat.get_gradient(matrix_id), per_matrix.get_gradient(matrix_id))

    # an omitted matrix reads as zeros, not as "missing"
    omitted = analysis.run_adjoint(n, {'first': [0.75, -1.5]}, params)
    zero_filled = analysis.run_adjoint(n, {'first': [0.75, -1.5], 'second': [0.0]}, params)
    pd.testing.assert_series_equal(omitted.get_gradient('first'), zero_filled.get_gradient('first'))

    with pytest.raises(ValueError, match='must have length 3'):
        analysis.run_adjoint(n, [1.0, 0.0], params)
    with pytest.raises(ValueError, match="'first' must have length 2"):
        analysis.run_adjoint(n, {'first': [1.0]}, params)


def test_sensitivity_adjoint_repeated_cotangents_must_agree():
    # one monitored function declared by two matrices, each pairing it with its own lever family: y_bar is a
    # property of the function, so the repeated slots must state one value, and 0.0 reads as "not stated here"
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1'], ['B2-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'gens')
    analysis.add_factor_matrix(['L1-2-1'], ['B9-SH'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.SHUNT_COMPENSATOR_SUSCEPTANCE, 'shunts')

    stated_once = analysis.run_adjoint(n, {'gens': [1.0]}, params).get_gradient('gens')
    stated_twice = analysis.run_adjoint(n, {'gens': [1.0], 'shunts': [1.0]}, params).get_gradient('gens')
    # agreeing slots are used once, never summed: the same value, not twice it
    pd.testing.assert_series_equal(stated_once, stated_twice)

    with pytest.raises(PyPowsyblError, match='Conflicting cotangents'):
        analysis.run_adjoint(n, {'gens': [1.0], 'shunts': [2.0]}, params)


def test_sensitivity_adjoint_series_cotangents_align_on_index():
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1', 'L1-5-1'], ['B2-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'm')

    ordered = analysis.run_adjoint(n, {'m': [0.75, -1.5]}, params).get_gradient('m')
    # the same cotangents as a Series in the OPPOSITE order: aligned on the index, not taken positionally
    shuffled = pd.Series({'L1-5-1': -1.5, 'L1-2-1': 0.75})
    aligned = analysis.run_adjoint(n, {'m': shuffled}, params).get_gradient('m')
    pd.testing.assert_series_equal(ordered, aligned)

    with pytest.raises(ValueError, match='missing 1 of its 2 declared functions'):
        analysis.run_adjoint(n, {'m': pd.Series({'L1-2-1': 0.75})}, params)
    with pytest.raises(ValueError, match='per factor matrix'):
        analysis.run_adjoint(n, pd.Series([0.75, -1.5]), params)


def test_sensitivity_adjoint_lever_outside_the_component_answers_zero():
    # A lever whose element is not in the solved network moves nothing, so its gradient is 0 — the same
    # answer the forward path writes for it. Every declared lever is answered, in declaration order, so a
    # caller never has to tell an absent row from a zero one.
    params = _adjoint_lf_parameters()
    n = pp.network.create_ieee14()
    n.update_lines(id='L4-5-1', connected1=False, connected2=False)
    pp.loadflow.run_ac(n, params)

    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_branch_admittance_factor_matrix(['L1-2-1'], ['L2-3-1', 'L4-5-1'],
                                                 SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, 'm')
    gradient = analysis.run_adjoint(n, [1.0], params).get_gradient('m')

    assert list(gradient.index) == ['L2-3-1', 'L4-5-1']
    assert gradient.notna().all()
    assert abs(gradient['L2-3-1']) > 1e-6
    assert gradient['L4-5-1'] == 0.0


def test_sensitivity_adjoint_auto_detected_variable_types():
    # AUTO_DETECT levers: the type is inferred from the network element, not declared. Every other adjoint
    # test states the type explicitly, so without this the inference branch is untested in reverse mode.
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_branch_flow_factor_matrix(['L1-2-1'], ['B2-G', 'B9-SH'], 'm')  # generator + shunt, undeclared

    gradient = analysis.run_adjoint(n, [1.0], params).get_gradient('m')
    forward = analysis.run(n, params).get_sensitivity_matrix('m')['L1-2-1']

    assert list(gradient.index) == ['B2-G', 'B9-SH']
    pd.testing.assert_series_equal(forward, gradient, check_names=False, atol=1e-10, rtol=1e-10)
    assert gradient.abs().max() > 0.1


def test_sensitivity_adjoint_zone_and_power_transfer_levers():
    # A zone id names a variable SET, not a network element, and a (zone, zone) pair is a power transfer
    # occupying two rows that get folded into one. Both paths are shared with the forward matrix and neither
    # was reached in reverse mode.
    n = pp.network.load(str(DATA_DIR.joinpath('simple-eu.uct')))
    params = _adjoint_lf_parameters()
    pp.loadflow.run_ac(n, params)

    zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
    zone_be = pp.sensitivity.create_country_zone(n, 'BE')
    branch = 'BBE2AA1  FFR3AA1  1'
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.set_zones([zone_fr, zone_be])
    analysis.add_branch_flow_factor_matrix([branch], ['FR', ('FR', 'BE')], 'm')

    gradient = analysis.run_adjoint(n, [1.0], params).get_gradient('m')
    forward = analysis.run(n, params).get_sensitivity_matrix('m')[branch]

    # the transfer pair is one row, labelled 'FR -> BE', not two
    assert list(gradient.index) == ['FR', 'FR -> BE']
    pd.testing.assert_series_equal(forward, gradient, check_names=False, atol=1e-10, rtol=1e-10)
    assert gradient.abs().max() > 1e-6


def test_sensitivity_adjoint_svc_pilot_lever():
    # the RST lever wrapper: the variable ids are SVC zone names, which exist only in the extension
    n = pp.network.create_ieee14()
    n.update_generators(id='B8-G', min_q=-6, max_q=200)
    zones = pd.DataFrame.from_records(index='name',
                                      data=[{'name': 'z1', 'target_v': 12.7, 'bus_ids': 'B10'}])
    units = pd.DataFrame.from_records(index='unit_id',
                                      data=[{'unit_id': 'B6-G', 'zone_name': 'z1', 'participate': True},
                                            {'unit_id': 'B8-G', 'zone_name': 'z1', 'participate': True}])
    n.create_extensions('secondaryVoltageControl', [zones, units])
    params = pp.loadflow.Parameters(use_reactive_limits=False,
                                    provider_parameters={'networkCacheEnabled': 'true',
                                                         'secondaryVoltageControl': 'true',
                                                         'maxPlausibleTargetVoltage': '1.6'})
    pp.loadflow.run_ac(n, params)

    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_svc_pilot_factor_matrix(['B10'], ['z1'], SensitivityFunctionType.BUS_VOLTAGE, 'm')
    gradient = analysis.run_adjoint(n, [1.0], params).get_gradient('m')

    # the closed loop makes the pilot bus track its own target
    assert gradient['z1'] == pytest.approx(1.0, abs=1e-3)


def test_sensitivity_adjoint_error_paths():
    params = _adjoint_lf_parameters()

    # no cached load flow: runAdjoint has no factorized Jacobian to reuse and must say so
    cold = pp.network.create_ieee14()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1'], ['B2-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'm')
    with pytest.raises(PyPowsyblError, match='networkCacheEnabled'):
        analysis.run_adjoint(cold, [1.0], params)

    # an unknown factor matrix id is named rather than returning an empty vector
    n = _ieee14_with_warm_cache(params)
    result = analysis.run_adjoint(n, [1.0], params)
    with pytest.raises(PyPowsyblError, match="'not_declared' not found"):
        result.get_gradient('not_declared')


def test_sensitivity_adjoint_gradient_survives_its_result():
    # get_gradient hands back a matrix allocated on the Java side and released through the C++ deleter.
    # A premature release would corrupt the values that are already in the caller's hands, so read one
    # gradient, drop every reference to the result that produced it, and require it to be unchanged.
    import gc
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1'], ['B2-G', 'B3-G', 'B6-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'm')

    result = analysis.run_adjoint(n, [1.0], params)
    kept = result.get_gradient('m')
    expected = list(kept)
    del result
    gc.collect()
    assert list(kept) == expected

    # and repeated reads of a fresh result agree with it, so the release is not corrupting the next one
    for _ in range(5):
        again = analysis.run_adjoint(n, [1.0], params).get_gradient('m')
        gc.collect()
        pd.testing.assert_series_equal(kept, again)


def test_sensitivity_adjoint_refuses_declared_contingencies():
    # reverse mode is base case only: a declared contingency cannot change the answer, so returning the
    # base-case gradient would be indistinguishable from the post-contingency one the caller asked for
    params = _adjoint_lf_parameters()
    n = _ieee14_with_warm_cache(params)
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.add_factor_matrix(['L1-2-1'], ['B2-G'], [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                               SensitivityVariableType.INJECTION_ACTIVE_POWER, 'm')
    analysis.add_single_element_contingency('L2-3-1', 'lostLine')

    with pytest.raises(PyPowsyblError, match='lostLine'):
        analysis.run_adjoint(n, [1.0], params)

    # the forward run still serves post-contingency sensitivities from the same declaration
    assert analysis.run(n, params).get_sensitivity_matrix('m', 'lostLine') is not None
