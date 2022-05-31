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
from pypowsybl import PyPowsyblError

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')


@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)


def test_config():
    assert 'OpenSensitivityAnalysis' == pp.sensitivity.get_default_provider()
    pp.sensitivity.set_default_provider("provider")
    assert 'provider' == pp.sensitivity.get_default_provider()
    n = pp.network.create_ieee14()
    generators = pd.DataFrame(data=[4999.0, 4999.0, 4999.0, 4999.0, 4999.0],
                              columns=['max_p'], index=['B1-G', 'B2-G', 'B3-G', 'B6-G', 'B8-G'])
    n.update_generators(generators)
    sa = pp.sensitivity.create_dc_analysis()
    sa.add_single_element_contingency('L1-2-1')
    sa.set_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
    with pytest.raises(Exception) as exc_info:
        sa.run(n)
    assert 'SensitivityAnalysisProvider \'provider\' not found' == str(exc_info.value)
    r = sa.run(n, provider='OpenSensitivityAnalysis')
    assert 6 == r.get_branch_flows_sensitivity_matrix().size
    assert 'provider' == pp.sensitivity.get_default_provider()
    pp.sensitivity.set_default_provider('OpenSensitivityAnalysis')
    assert 'OpenSensitivityAnalysis' == pp.sensitivity.get_default_provider()


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

    df = r.get_branch_flows_sensitivity_matrix('m')
    assert (3, 2) == df.shape
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.080991, abs=1e-6)
    assert df['L1-5-1']['B2-G'] == pytest.approx(-0.080991, abs=1e-6)
    assert df['L1-5-1']['B3-G'] == pytest.approx(-0.172498, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.013675, abs=1e-6)
    assert df['L2-3-1']['B2-G'] == pytest.approx(0.013675, abs=1e-6)
    assert df['L2-3-1']['B3-G'] == pytest.approx(-0.545683, abs=1e-6)

    df = r.get_reference_flows('m')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['reference_flows'] == pytest.approx(72.247, abs=1e-3)
    assert df['L2-3-1']['reference_flows'] == pytest.approx(69.831, abs=1e-3)

    df = r.get_branch_flows_sensitivity_matrix('m', 'L1-2-1')
    assert df.shape == (3, 2)
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.5, abs=1e-6)
    assert df['L1-5-1']['B2-G'] == pytest.approx(-0.5, abs=1e-6)
    assert df['L1-5-1']['B3-G'] == pytest.approx(-0.5, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.084423, abs=1e-6)
    assert df['L2-3-1']['B2-G'] == pytest.approx(0.084423, abs=1e-6)
    assert df['L2-3-1']['B3-G'] == pytest.approx(-0.490385, abs=1e-6)

    df = r.get_reference_flows('m', 'L1-2-1')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['reference_flows'] == pytest.approx(225.7, abs=1e-3)
    assert df['L2-3-1']['reference_flows'] == pytest.approx(43.921, abs=1e-3)

    assert r.get_branch_flows_sensitivity_matrix('m', 'aaa') is None

    df = r.get_branch_flows_sensitivity_matrix('preContingency')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.080991, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.013675, abs=1e-6)
    df = r.get_branch_flows_sensitivity_matrix('postContingency', 'L1-2-1')
    assert df.shape == (1, 2)
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.5, abs=1e-6)
    assert df['L2-3-1']['B1-G'] == pytest.approx(-0.084423, abs=1e-6)


def test_voltage_sensitivities():
    n = pp.network.create_eurostag_tutorial_example1_network()
    sa = pp.sensitivity.create_ac_analysis()
    sa.set_bus_voltage_factor_matrix(['VLGEN_0'], ['GEN'])
    r = sa.run(n)
    df = r.get_bus_voltages_sensitivity_matrix()
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
    s = result.get_branch_flows_sensitivity_matrix('m')
    assert s.shape == (2, 2)
    assert s['BBE2AA1  FFR3AA1  1']['FR'] == pytest.approx(-0.379829, abs=1e-6)
    assert s['FFR2AA1  DDE3AA1  1']['FR'] == pytest.approx(0.370171, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['BE'] == pytest.approx(0.378423, abs=1e-6)
    assert s['FFR2AA1  DDE3AA1  1']['BE'] == pytest.approx(0.128423, abs=1e-6)
    r = result.get_reference_flows('m')
    assert r.shape == (1, 2)
    assert r['BBE2AA1  FFR3AA1  1']['reference_flows'] == pytest.approx(324.666, abs=1e-3)
    assert r['FFR2AA1  DDE3AA1  1']['reference_flows'] == pytest.approx(1324.666, abs=1e-3)


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
    s = result.get_branch_flows_sensitivity_matrix('m')
    assert s.shape == (4, 2)
    assert s['BBE2AA1  FFR3AA1  1']['FR'] == pytest.approx(-0.379829, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['FR -> DE'] == pytest.approx(-0.256641, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['DE -> FR'] == pytest.approx(0.256641, abs=1e-6)
    assert s['BBE2AA1  FFR3AA1  1']['NL'] == pytest.approx(0.103426, abs=1e-6)


def test_xnode_sensi():
    n = pp.network.load(str(DATA_DIR.joinpath('simple-eu-xnode.uct')))
    # assert there is one dangling line (corresponding to the UCTE xnode)
    dangling_lines = n.get_dangling_lines()
    assert len(dangling_lines) == 1
    # create a new zone with only one xnode, this is the dangling line id that has to be configured (corresponding
    # to the line connecting the xnode in the UCTE file)
    zone_x = pp.sensitivity.create_empty_zone("X")
    zone_x.add_injection('NNL2AA1  XXXXXX11 1')
    sa = pp.sensitivity.create_dc_analysis()
    sa.set_zones([zone_x])
    sa.add_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1'], ['X'], 'm')
    result = sa.run(n)
    s = result.get_branch_flows_sensitivity_matrix('m')
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

    df = r.get_branch_flows_sensitivity_matrix('m')
    assert (1, 1) == df.shape
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.080991, abs=1e-6)

    n.clone_variant(n.get_working_variant_id(), 'variant_2')
    n.set_working_variant('variant_2')
    n.update_lines(id='L2-3-1', connected1=False)
    r = sa.run(n)

    df = r.get_branch_flows_sensitivity_matrix('m')
    assert (1, 1) == df.shape
    assert df['L1-5-1']['B1-G'] == pytest.approx(0.078150, abs=1e-6)


def test_provider_names():
    assert 'OpenSensitivityAnalysis' in pp.sensitivity.get_provider_names()


def test_no_output_matrices_available():
    network = pp.network.create_eurostag_tutorial_example1_network()
    analysis = pp.sensitivity.create_ac_analysis()
    analysis.set_branch_flow_factor_matrix(network.get_lines().index.to_list(),
                                           network.get_generators().index.to_list())
    result = analysis.run(network)
    df = result.get_branch_flows_sensitivity_matrix('default')
    assert (2, 2) == df.shape

    with pytest.raises(pp.PyPowsyblError) as errorContext:
        result.get_bus_voltages_sensitivity_matrix()
    assert 'bus voltage sensitivity matrix does not exist' == str(errorContext.value)

    with pytest.raises(pp.PyPowsyblError) as errorContext:
        result.get_branch_flows_sensitivity_matrix('')
    assert 'Matrix \'\' not found' == str(errorContext.value)
