#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import copy
import datetime
import os
import pathlib
import re
import tempfile
import unittest
import io
from os.path import exists

import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
import pandas as pd
import pytest
from numpy import NaN

import pypowsybl as pp
import pypowsybl.report as rp
import util
from pypowsybl import PyPowsyblError
from pypowsybl.network import ValidationLevel, LayoutParameters

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'


def test_create_empty_network():
    n = pp.network.create_empty("test")
    assert n is not None


def test_load_network_from_string():
    file_content = """
##C 2007.05.01
##N
##ZBE
BBE1AA1               0 2 400.00 3000.00 0.00000 -1500.0 0.00000 0.00000 -9000.0 9000.00 -9000.0                               F
    """
    n = pp.network.load_from_string('simple-eu.uct', file_content)
    assert 1 == len(n.get_substations())

def test_load_cgmes_zipped():
    with open(DATA_DIR.joinpath('CGMES_Full.zip'), "rb") as fh:
        n = pp.network.load_from_binary_buffer(io.BytesIO(fh.read()))
        assert 3 == len(n.get_substations())

def test_load_cgmes_two_zip():
    with open(DATA_DIR.joinpath('CGMES_Partial.zip'), "rb") as cgmesPartial:
        with open(DATA_DIR.joinpath('Boundary.zip'), "rb") as boundary:
            n = pp.network.load_from_binary_buffers([io.BytesIO(cgmesPartial.read()), io.BytesIO(boundary.read())])
    assert 3 == len(n.get_substations())

def test_load_zipped_xiidm():
    with open(DATA_DIR.joinpath('battery_xiidm.zip'), "rb") as fh:
        n = pp.network.load_from_binary_buffer(io.BytesIO(fh.read()))
        assert 2 == len(n.get_substations())

def test_dump_to_string():
    bat_path = TEST_DIR.joinpath('battery.xiidm')
    xml = bat_path.read_text()
    n = pp.network.load(str(bat_path))
    assert xml == n.dump_to_string()


def test_dump_ampl():
    n = pp.network.create_eurostag_tutorial_example1_network()
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        tmp_dir_path = pathlib.Path(tmp_dir_name)
        ampl_base_file = tmp_dir_path.joinpath('ampl')
        n.dump(ampl_base_file, format='AMPL')
        file_names = os.listdir(tmp_dir_path)
        file_names_expected = ['ampl_network_vsc_converter_stations.txt', 'ampl_network_branches.txt',
                               'ampl_network_rtc.txt', 'ampl_network_generators.txt',
                               'ampl_network_substations.txt', 'ampl_network_tct.txt', 'ampl_network_loads.txt',
                               'ampl_network_lcc_converter_stations.txt', 'ampl_network_static_var_compensators.txt',
                               'ampl_network_hvdc.txt', 'ampl_network_limits.txt', 'ampl_network_shunts.txt',
                               'ampl_network_batteries.txt', 'ampl_network_ptc.txt', 'ampl_network_buses.txt']
        assert len(file_names) == len(file_names_expected)
        for file_name in file_names:
            assert file_name in file_names_expected


def test_dump_import_iidm():
    n = pp.network.create_eurostag_tutorial_example1_network()
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        tmp_dir_path = pathlib.Path(tmp_dir_name)
        iidm_file = tmp_dir_path.joinpath('test.xiidm')
        n.dump(iidm_file, format='XIIDM')
        file_names = os.listdir(tmp_dir_path)
        assert len(file_names) == 1
        assert 'test.xiidm' in file_names
        n2 = pp.network.load(iidm_file)
        assert n2.dump_to_string() == n.dump_to_string()
        assert isinstance(n2, pp.network.Network)


def test_dump_matpower():
    n = pp.network.create_eurostag_tutorial_example1_network()
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        tmp_dir_path = pathlib.Path(tmp_dir_name)
        mat_file = tmp_dir_path.joinpath('test.mat')
        n.dump(mat_file, format='MATPOWER')
        file_names = os.listdir(tmp_dir_path)
        assert len(file_names) == 1
        assert 'test.mat' in file_names
        n2 = pp.network.load(mat_file)
        assert isinstance(n2, pp.network.Network)
        # assert n2.dump_to_string() == n.dump_to_string() # problem import/export matpower


def test_dump_ucte():
    ucte_local_path = TEST_DIR.joinpath('test.uct')
    n = pp.network.load(str(ucte_local_path))
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        tmp_dir_path = pathlib.Path(tmp_dir_name)
        ucte_temporary_path = tmp_dir_path.joinpath('test.uct')
        n.dump(ucte_temporary_path, format='UCTE')
        file_names = os.listdir(tmp_dir_path)
        assert len(file_names) == 1
        assert 'test.uct' in file_names
        with open(ucte_temporary_path, 'r') as test_file:
            with open(ucte_local_path, 'r') as expected_file:
                # remove header with specific date
                test_file_str = test_file.read()[0] + test_file.read()[3:]
                expected_file_str = expected_file.read()[0] + expected_file.read()[3:]
                assert test_file_str == expected_file_str


def test_get_import_format():
    formats = pp.network.get_import_formats()
    assert ['CGMES', 'MATPOWER', 'IEEE-CDF', 'PSS/E', 'UCTE', 'XIIDM', 'POWER-FACTORY'] == formats


def test_get_import_parameters():
    parameters = pp.network.get_import_parameters('PSS/E')
    assert 1 == len(parameters)
    assert ['psse.import.ignore-base-voltage'] == parameters.index.tolist()
    assert 'Ignore base voltage specified in the file' == parameters['description']['psse.import.ignore-base-voltage']
    assert 'BOOLEAN' == parameters['type']['psse.import.ignore-base-voltage']
    assert 'false' == parameters['default']['psse.import.ignore-base-voltage']
    parameters = pp.network.get_import_parameters('CGMES')
    assert '[mRID, rdfID]' == parameters['possible_values']['iidm.import.cgmes.source-for-iidm-id']


def test_get_export_parameters():
    parameters = pp.network.get_export_parameters('CGMES')
    assert 9 == len(parameters)
    name = 'iidm.export.cgmes.cim-version'
    assert name == parameters.index.tolist()[1]
    assert 'CIM version to export' == parameters['description'][name]
    assert 'STRING' == parameters['type'][name]
    assert '' == parameters['default'][name]
    assert '[EQ, TP, SSH, SV]' == parameters['possible_values']['iidm.export.cgmes.profiles']


def test_get_export_format():
    formats = set(pp.network.get_export_formats())
    assert set(['AMPL', 'CGMES', 'MATPOWER', 'PSS/E', 'UCTE', 'XIIDM']).intersection(formats)


def test_load_network():
    n = pp.network.load(str(TEST_DIR.joinpath('empty-network.xml')))
    assert n is not None


def test_load_power_factory_network():
    n = pp.network.load(str(DATA_DIR.joinpath('ieee14.dgs')))
    assert n is not None


def test_connect_disconnect():
    n = pp.network.create_ieee14()
    assert n.disconnect('L1-2-1')
    assert n.connect('L1-2-1')


def test_network_attributes():
    n = pp.network.create_eurostag_tutorial_example1_network()
    assert 'sim1' == n.id
    assert datetime.datetime(2018, 1, 1, 10, 0, tzinfo=datetime.timezone.utc) == n.case_date
    assert 'sim1' == n.name
    assert datetime.timedelta(0) == n.forecast_distance
    assert 'test' == n.source_format


def test_network_representation():
    n = pp.network.create_eurostag_tutorial_example1_network()
    expected = 'Network(id=sim1, name=sim1, case_date=2018-01-01 10:00:00+00:00, ' + \
               'forecast_distance=0:00:00, source_format=test)'
    assert str(n) == expected
    assert repr(n) == expected


def test_get_network_element_ids():
    n = pp.network.create_eurostag_tutorial_example1_network()
    assert ['NGEN_NHV1', 'NHV2_NLOAD'] == n.get_elements_ids(pp.network.ElementType.TWO_WINDINGS_TRANSFORMER)
    assert ['NGEN_NHV1'] == n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
                                               nominal_voltages={24})
    assert ['NGEN_NHV1', 'NHV2_NLOAD'] == n.get_elements_ids(
        element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
        nominal_voltages={24, 150})
    assert ['LOAD'] == n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150})
    assert ['LOAD'] == n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150},
                                          countries={'BE'})
    assert [] == n.get_elements_ids(element_type=pp.network.ElementType.LOAD, nominal_voltages={150}, countries={'FR'})
    assert ['NGEN_NHV1'] == n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER,
                                               nominal_voltages={24}, countries={'FR'})
    assert [] == n.get_elements_ids(element_type=pp.network.ElementType.TWO_WINDINGS_TRANSFORMER, nominal_voltages={24},
                                    countries={'BE'})


def test_buses():
    n = pp.network.create_eurostag_tutorial_example1_network()
    buses = n.get_buses()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                            columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                                     'voltage_level_id'],
                            data=[['', NaN, NaN, 0, 0, 'VLGEN'],
                                  ['', 380, NaN, 0, 0, 'VLHV1'],
                                  ['', 380, NaN, 0, 0, 'VLHV2'],
                                  ['', NaN, NaN, 0, 0, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected, buses, check_dtype=False)

    n.update_buses(pd.DataFrame(index=['VLGEN_0'], columns=['v_mag', 'v_angle'], data=[[400, 0]]))
    buses = n.get_buses()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                            columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                                     'voltage_level_id'],
                            data=[['', 400, 0, 0, 0, 'VLGEN'],
                                  ['', 380, NaN, 0, 0, 'VLHV1'],
                                  ['', 380, NaN, 0, 0, 'VLHV2'],
                                  ['', NaN, NaN, 0, 0, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected, buses, check_dtype=False)


def test_loads_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    loads = n.get_loads(all_attributes=True)
    assert 600 == loads['p0']['LOAD']
    assert 200 == loads['q0']['LOAD']
    assert 'UNDEFINED' == loads['type']['LOAD']
    assert not loads['fictitious']['LOAD']
    df2 = pd.DataFrame(data=[[500, 300]], columns=['p0', 'q0'], index=['LOAD'])
    n.update_loads(df2)
    df3 = n.get_loads()
    assert 300 == df3['q0']['LOAD']
    assert 500 == df3['p0']['LOAD']
    n = pp.network.create_four_substations_node_breaker_network()
    loads = n.get_loads(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['LD1', 'LD2', 'LD3', 'LD4', 'LD5', 'LD6']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['S1VL1_2', 2], ['S1VL2_13', 13], ['S1VL2_15', 15], ['S1VL2_17', 17], ['S3VL1_4', 4], ['S4VL1_2', 2]])
    pd.testing.assert_frame_equal(expected, loads, check_dtype=False, atol=1e-2)


def test_batteries_data_frame():
    n = pp.network.load(str(TEST_DIR.joinpath('battery.xiidm')))
    batteries = n.get_batteries(all_attributes=True)
    assert 200.0 == batteries['max_p']['BAT2']
    assert not batteries['fictitious']['BAT2']
    df2 = pd.DataFrame(data=[[101, 201]], columns=['target_p', 'target_q'], index=['BAT2'])
    n.update_batteries(df2)
    df3 = n.get_batteries()
    assert 101 == df3['target_p']['BAT2']
    assert 201 == df3['target_q']['BAT2']
    batteries = n.get_batteries(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['BAT', 'BAT2']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['NBAT', -1], ['NBAT', -1]])
    pd.testing.assert_frame_equal(expected, batteries, check_dtype=False, atol=1e-2)


def test_vsc_data_frame():
    n = pp.network.create_four_substations_node_breaker_network()
    stations = n.get_vsc_converter_stations(all_attributes=True)
    assert -550.0 == stations['min_q_at_p']['VSC1']
    assert 570.0 == stations['max_q_at_p']['VSC1']
    assert not stations['fictitious']['VSC1']
    assert 'S1VL2_5' == stations['bus_breaker_bus_id']['VSC1']
    stations = n.get_vsc_converter_stations()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VSC1', 'VSC2']),
                            columns=['name', 'loss_factor', 'min_q', 'max_q', 'reactive_limits_kind', 'target_v',
                                     'target_q', 'voltage_regulator_on',
                                     'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id',
                                     'connected'],
                            data=[['VSC1', 1.1, NaN, NaN, 'CURVE', 400, 500, True, 'VSC1', 10.11, -512.081, 739.27,
                                   'S1VL2', 'S1VL2_0',
                                   True],
                                  ['VSC2', 1.1, -400, 500, 'MIN_MAX', 0, 120, False, 'VSC2', -9.89, -120, 170.032,
                                   'S2VL1', 'S2VL1_0',
                                   True]])
    pd.testing.assert_frame_equal(expected, stations, check_dtype=False, atol=1e-2)

    stations2 = pd.DataFrame(data=[[300.0, 400.0, 'VSC2'], [1.0, 2.0, 'VSC1']],
                             columns=['target_v', 'target_q', 'regulated_element_id'], index=['VSC1', 'VSC2'])
    n.update_vsc_converter_stations(stations2)
    stations3 = pd.DataFrame(data=[[-350, 400]],
                             columns=['min_q', 'max_q'], index=['VSC2'])
    n.update_vsc_converter_stations(stations3)
    stations = n.get_vsc_converter_stations()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VSC1', 'VSC2']),
                            columns=['name', 'loss_factor', 'min_q', 'max_q', 'reactive_limits_kind', 'target_v',
                                     'target_q', 'voltage_regulator_on',
                                     'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id',
                                     'connected'],
                            data=[['VSC1', 1.1, NaN, NaN, 'CURVE', 300, 400, True, 'VSC2', 10.11, -512.081, 739.27,
                                   'S1VL2', 'S1VL2_0',
                                   True],
                                  ['VSC2', 1.1, -350, 400, 'MIN_MAX', 1, 2, False, 'VSC1', -9.89, -120, 170.032,
                                   'S2VL1', 'S2VL1_0',
                                   True]])
    pd.testing.assert_frame_equal(expected, stations, check_dtype=False, atol=1e-2)
    stations = n.get_vsc_converter_stations(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['VSC1', 'VSC2']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['S1VL2_5', 5], ['S2VL1_4', 4]])
    pd.testing.assert_frame_equal(expected, stations, check_dtype=False, atol=1e-2)


def test_lcc_data_frame():
    n = pp.network.create_four_substations_node_breaker_network()
    stations = n.get_lcc_converter_stations(all_attributes=True)
    assert not stations['fictitious']['LCC1']
    assert 'S1VL2_21' == stations['bus_breaker_bus_id']['LCC1']
    assert 21 == stations['node']['LCC1']
    stations = n.get_lcc_converter_stations()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LCC1', 'LCC2']),
                            columns=['name', 'power_factor', 'loss_factor', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id', 'connected'],
                            data=[['LCC1', 0.6, 1.1, 80.88, NaN, NaN, 'S1VL2', 'S1VL2_0', True],
                                  ['LCC2', 0.6, 1.1, - 79.12, NaN, NaN, 'S3VL1', 'S3VL1_0', True]])
    pd.testing.assert_frame_equal(expected, stations, check_dtype=False)
    n.update_lcc_converter_stations(
        pd.DataFrame(index=['LCC1'],
                     columns=['power_factor', 'loss_factor', 'p', 'q', 'fictitious'],
                     data=[[0.7, 1.2, 82, 69, True]]))
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['LCC1', 'LCC2']),
                            columns=['name', 'power_factor', 'loss_factor', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id', 'connected'],
                            data=[['LCC1', 0.7, 1.2, 82, 69, 154.68, 'S1VL2', 'S1VL2_0', True],
                                  ['LCC2', 0.6, 1.1, - 79.12, NaN, NaN, 'S3VL1', 'S3VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_lcc_converter_stations(), check_dtype=False, atol=1e-2)


def test_hvdc_data_frame():
    n = pp.network.create_four_substations_node_breaker_network()
    lines = n.get_hvdc_lines(all_attributes=True)
    assert 10 == lines['target_p']['HVDC1']
    assert not lines['fictitious']['HVDC1']
    lines2 = pd.DataFrame(data=[11], columns=['target_p'], index=['HVDC1'])
    n.update_hvdc_lines(lines2)
    lines = n.get_hvdc_lines()
    assert 11 == lines['target_p']['HVDC1']


def test_svc_data_frame():
    n = pp.network.create_four_substations_node_breaker_network()
    svcs = n.get_static_var_compensators(all_attributes=True)
    assert not svcs['fictitious']['SVC']
    assert 'S4VL1_4' == svcs['bus_breaker_bus_id']['SVC']
    assert 4 == svcs['node']['SVC']
    svcs = n.get_static_var_compensators()
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['SVC']),
        columns=['name', 'b_min', 'b_max', 'target_v', 'target_q',
                 'regulation_mode', 'regulated_element_id', 'p', 'q', 'i', 'voltage_level_id', 'bus_id',
                 'connected'],
        data=[['', -0.05, 0.05, 400, NaN, 'VOLTAGE', 'SVC', NaN, -12.54, NaN, 'S4VL1', 'S4VL1_0', True]])
    pd.testing.assert_frame_equal(expected, svcs, check_dtype=False, atol=1e-2)
    n.update_static_var_compensators(pd.DataFrame(
        index=pd.Series(name='id', data=['SVC']),
        columns=['b_min', 'b_max', 'target_v', 'target_q', 'regulation_mode', 'p', 'q', 'regulated_element_id'],
        data=[[-0.06, 0.06, 398, 100, 'REACTIVE_POWER', -12, -13, 'VSC1']]))

    svcs = n.get_static_var_compensators()
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['SVC']),
        columns=['name', 'b_min', 'b_max', 'target_v', 'target_q', 'regulation_mode', 'regulated_element_id', 'p',
                 'q', 'i',
                 'voltage_level_id', 'bus_id', 'connected'],
        data=[['', -0.06, 0.06, 398, 100, 'REACTIVE_POWER', 'VSC1', -12, -13, 25.54, 'S4VL1', 'S4VL1_0', True]])
    pd.testing.assert_frame_equal(expected, svcs, check_dtype=False, atol=1e-2)
    svcs = n.get_static_var_compensators(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['SVC']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['S4VL1_4', 4]])
    pd.testing.assert_frame_equal(expected, svcs,
                                  check_dtype=False, atol=1e-2)


def test_generators_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    generators = n.get_generators(all_attributes=True)
    assert 'OTHER' == generators['energy_source']['GEN']
    assert 607 == generators['target_p']['GEN']
    assert not generators['fictitious']['GEN']
    assert 'NGEN' == generators['bus_breaker_bus_id']['GEN']
    assert -1 == generators['node']['GEN']
    assert -9999.99 == generators['min_q_at_p']['GEN']
    assert 9999.99 == generators['max_q_at_p']['GEN']
    assert -9999.99 == generators['min_q_at_target_p']['GEN']
    assert 9999.99 == generators['max_q_at_target_p']['GEN']
    n.update_generators(id='GEN', rated_s=100)
    generators = n.get_generators(all_attributes=True)
    assert 100 == generators['rated_s']['GEN']
    n = pp.network.create_four_substations_node_breaker_network()
    generators = n.get_generators(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['GH1', 'GH2', 'GH3', 'GTH1', 'GTH2']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['S1VL2_7', 7], ['S1VL2_9', 9], ['S1VL2_11', 11], ['S2VL1_2', 2], ['S3VL1_6', 6]])
    pd.testing.assert_frame_equal(expected, generators, check_dtype=False, atol=1e-2)


def test_generator_maxq_minq_reactive_limits():
    n = pp.network.create_four_substations_node_breaker_network()
    gen1_id = 'GH1'
    n.update_generators(id=gen1_id, p=50, target_p=80)
    generators = n.get_generators(attributes=['reactive_limits_kind',
                                              'min_q', 'max_q',
                                              'min_q_at_p', 'max_q_at_p',
                                              'min_q_at_target_p', 'max_q_at_target_p'])
    gen1 = generators.loc[gen1_id]
    assert 'CURVE' == gen1['reactive_limits_kind']
    assert np.isnan(gen1['min_q'])
    assert np.isnan(gen1['max_q'])
    assert -769.3, gen1['min_q_at_p']
    assert 860, gen1['max_q_at_p']
    assert -845.5, gen1['min_q_at_target_p']
    assert 929.0, gen1['max_q_at_target_p']
    n = pp.network.create_ieee118()
    gen2_id = 'B1-G'
    gen2 = n.get_generators().loc[gen2_id]
    assert 'MIN_MAX' == gen2['reactive_limits_kind']
    assert -5 == gen2['min_q']
    assert 15 == gen2['max_q']


def test_vsc_maxq_minq_reactive_limits():
    n = pp.network.create_four_substations_node_breaker_network()
    vsc1_id = 'VSC1'
    n.update_vsc_converter_stations(id=vsc1_id, p=50)
    vsc = n.get_vsc_converter_stations(attributes=['min_q_at_p', 'max_q_at_p'])
    vsc1 = vsc.loc[vsc1_id]
    pd.testing.assert_series_equal(pd.Series({'min_q_at_p': -550.0, 'max_q_at_p': 570.0}, name='VSC1'), vsc1)


def test_generator_disconnected_bus_breaker_id():
    n = pp.network.create_eurostag_tutorial_example1_network()
    gen1_id = 'GEN'
    generators = n.get_generators(attributes=['bus_id', 'bus_breaker_bus_id', 'connected'])
    gen1 = generators.loc[gen1_id]
    assert 'VLGEN_0' == gen1['bus_id']
    assert 'NGEN' == gen1['bus_breaker_bus_id']
    assert gen1['connected']

    n.disconnect(gen1_id)
    generators = n.get_generators(attributes=['bus_id', 'bus_breaker_bus_id', 'connected'])
    gen1 = generators.loc[gen1_id]
    assert '' == gen1['bus_id']
    assert 'NGEN' == gen1['bus_breaker_bus_id']
    assert not gen1['connected']


def test_ratio_tap_changer_steps_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    steps = n.get_ratio_tap_changer_steps()
    assert 0.8505666905244191 == steps.loc['NHV2_NLOAD']['rho'][0]
    assert 0.8505666905244191 == steps.loc[('NHV2_NLOAD', 0), 'rho']
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 0)],
                                   pd.Series(data={'rho': 0.850567, 'r': 0, 'x': 0, 'g': 0, 'b': 0}, name=('NHV2_NLOAD', 0)), check_dtype=False)
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 1)],
                                   pd.Series(data={'rho': 1.00067, 'r': 0, 'x': 0, 'g': 0, 'b': 0}, name=('NHV2_NLOAD', 1)), check_dtype=False)
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 2)],
                                   pd.Series(data={'rho': 1.15077, 'r': 0, 'x': 0, 'g': 0, 'b': 0}, name=('NHV2_NLOAD', 2)), check_dtype=False)
    n.update_ratio_tap_changer_steps(pd.DataFrame(
        index=pd.MultiIndex.from_tuples([('NHV2_NLOAD', 0), ('NHV2_NLOAD', 1)],
                                        names=['id', 'position']), columns=['rho', 'r', 'x', 'g', 'b'],
        data=[[1, 1, 1, 1, 1],
              [1, 1, 1, 1, 1]]))
    steps = n.get_ratio_tap_changer_steps()
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 0)],
                                   pd.Series(data={'rho': 1, 'r': 1, 'x': 1, 'g': 1, 'b': 1},
                                             name=('NHV2_NLOAD', 0)), check_dtype=False)
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 1)],
                                   pd.Series(data={'rho': 1, 'r': 1, 'x': 1, 'g': 1, 'b': 1},
                                             name=('NHV2_NLOAD', 1)), check_dtype=False)
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 2)],
                                   pd.Series(data={'rho': 1.15077, 'r': 0, 'x': 0, 'g': 0, 'b': 0},
                                             name=('NHV2_NLOAD', 2)), check_dtype=False)
    n.update_ratio_tap_changer_steps(id='NHV2_NLOAD', position=0, rho=2, r=3, x=4, g=5, b=6)
    steps = n.get_ratio_tap_changer_steps()
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 0)],
                                   pd.Series(data={'rho': 2, 'r': 3, 'x': 4, 'g': 5, 'b': 6},
                                             name=('NHV2_NLOAD', 0)), check_dtype=False)
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 1)],
                                   pd.Series(data={'rho': 1, 'r': 1, 'x': 1, 'g': 1, 'b': 1},
                                             name=('NHV2_NLOAD', 1)), check_dtype=False)
    pd.testing.assert_series_equal(steps.loc[('NHV2_NLOAD', 2)],
                                   pd.Series(data={'rho': 1.15077, 'r': 0, 'x': 0, 'g': 0, 'b': 0},
                                             name=('NHV2_NLOAD', 2)), check_dtype=False)


def test_phase_tap_changer_steps_data_frame():
    n = pp.network.create_ieee300()
    steps = n.get_phase_tap_changer_steps()
    assert 11.4 == steps.loc[('T196-2040-1', 0), 'alpha']
    pd.testing.assert_series_equal(steps.loc[('T196-2040-1', 0)],
                                   pd.Series(data={'rho': 1, 'alpha': 11.4, 'r': 0, 'x': 0, 'g': 0, 'b': 0},
                                             name=('T196-2040-1', 0)), check_dtype=False)
    # pd.testing.assert_frame_equal(expected, n.get_phase_tap_changer_steps(), check_dtype=False)
    n.update_phase_tap_changer_steps(pd.DataFrame(
        index=pd.MultiIndex.from_tuples([('T196-2040-1', 0)],
                                        names=['id', 'position']), columns=['alpha', 'rho', 'r', 'x', 'g', 'b'],
        data=[[1, 1, 1, 1, 1, 1]]))
    steps = n.get_phase_tap_changer_steps()
    pd.testing.assert_series_equal(steps.loc[('T196-2040-1', 0)],
                                   pd.Series(data={'rho': 1, 'alpha': 1, 'r': 1, 'x': 1, 'g': 1, 'b': 1},
                                             name=('T196-2040-1', 0)), check_dtype=False)
    n.update_phase_tap_changer_steps(id=['T196-2040-1'], position=[0], rho=[2], alpha=[7], r=[3], x=[4], g=[5],
                                     b=[6])
    steps = n.get_phase_tap_changer_steps()
    pd.testing.assert_series_equal(steps.loc[('T196-2040-1', 0)],
                                   pd.Series(data={'rho': 2, 'alpha': 7, 'r': 3, 'x': 4, 'g': 5, 'b': 6},
                                             name=('T196-2040-1', 0)), check_dtype=False)


def test_update_generators_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    generators = n.get_generators()
    assert 607 == generators['target_p']['GEN']
    assert generators['voltage_regulator_on']['GEN']
    assert '' == generators['regulated_element_id']['GEN']
    generators2 = pd.DataFrame(data=[[608.0, 302.0, 25.0, False]],
                               columns=['target_p', 'target_q', 'target_v', 'voltage_regulator_on'], index=['GEN'])
    n.update_generators(generators2)
    generators = n.get_generators()
    assert 608 == generators['target_p']['GEN']
    assert 302.0 == generators['target_q']['GEN']
    assert 25.0 == generators['target_v']['GEN']
    assert not generators['voltage_regulator_on']['GEN']


def test_regulated_terminal_node_breaker():
    n = pp.network.create_four_substations_node_breaker_network()
    gens = n.get_generators()
    assert 'GH1' == gens['regulated_element_id']['GH1']

    n.update_generators(id='GH1', regulated_element_id='S1VL1_BBS')
    updated_gens = n.get_generators()
    assert 'S1VL1_BBS' == updated_gens['regulated_element_id']['GH1']

    with pytest.raises(pp.PyPowsyblError):
        n.update_generators(id='GH1', regulated_element_id='LINE_S2S3')


def test_regulated_terminal_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    generators = n.get_generators()
    assert '' == generators['regulated_element_id']['GEN']

    with pytest.raises(pp.PyPowsyblError):
        n.update_generators(id='GEN', regulated_element_id='NHV1')
    with pytest.raises(pp.PyPowsyblError):
        n.update_generators(id='GEN', regulated_element_id='LOAD')


def test_update_unknown_data():
    n = pp.network.create_eurostag_tutorial_example1_network()
    update = pd.DataFrame(data=[['blob']], columns=['unknown'], index=['GEN'])
    with pytest.raises(ValueError) as context:
        n.update_generators(update)
    assert 'No column named unknown' in str(context)


def test_update_non_modifiable_data():
    n = pp.network.create_eurostag_tutorial_example1_network()
    update = pd.DataFrame(data=[['blob']], columns=['voltage_level_id'], index=['GEN'])
    with pytest.raises(pp.PyPowsyblError) as context:
        n.update_generators(update)
    assert 'Series \'voltage_level_id\' is not modifiable.' == str(context.value)


def test_update_switches_data_frame():
    n = pp.network.load(str(TEST_DIR.joinpath('node-breaker.xiidm')))
    switches = n.get_switches()
    # no open switch
    open_switches = switches[switches['open']].index.tolist()
    assert 0 == len(open_switches)
    # open 1 breaker
    n.update_switches(pd.DataFrame(index=['BREAKER-BB2-VL1_VL2_1'], data={'open': [True]}))
    switches = n.get_switches()
    open_switches = switches[switches['open']].index.tolist()
    assert ['BREAKER-BB2-VL1_VL2_1'] == open_switches


def test_update_2_windings_transformers_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = n.get_2_windings_transformers(all_attributes=True)
    assert not df['fictitious']['NGEN_NHV1']
    df = n.get_2_windings_transformers()
    assert ['name', 'r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
            'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1',
            'connected2'] == df.columns.tolist()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['NGEN_NHV1', 'NHV2_NLOAD']),
                            columns=['name', 'r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1',
                                     'i1', 'p2',
                                     'q2', 'i2', 'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id',
                                     'connected1', 'connected2'],
                            data=[['', 0.27, 11.10, 0, 0, 24, 400, NaN, NaN, NaN, NaN, NaN, NaN,
                                   NaN, 'VLGEN', 'VLHV1', 'VLGEN_0', 'VLHV1_0', True, True],
                                  ['', 0.05, 4.05, 0, 0, 400, 158, NaN, NaN, NaN, NaN, NaN, NaN, NaN,
                                   'VLHV2', 'VLLOAD', 'VLHV2_0', 'VLLOAD_0', True, True]])
    pd.testing.assert_frame_equal(expected, n.get_2_windings_transformers(), check_dtype=False, atol=1e-2)
    n.update_2_windings_transformers(
        pd.DataFrame(index=['NGEN_NHV1'],
                     columns=['r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'connected1', 'connected2'],
                     data=[[0.3, 11.2, 1, 1, 90, 225, False, False]]))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['NGEN_NHV1', 'NHV2_NLOAD']),
                            columns=['name', 'r', 'x', 'g', 'b', 'rated_u1', 'rated_u2', 'rated_s', 'p1', 'q1',
                                     'i1', 'p2',
                                     'q2', 'i2', 'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id',
                                     'connected1', 'connected2'],
                            data=[['', 0.3, 11.2, 1, 1, 90, 225, NaN, NaN, NaN, NaN, NaN, NaN, NaN,
                                   'VLGEN', 'VLHV1', '', '', False, False],
                                  ['', 0.047, 4.05, 0, 0, 400, 158, NaN, NaN, NaN, NaN, NaN, NaN, NaN,
                                   'VLHV2', 'VLLOAD', 'VLHV2_0', 'VLLOAD_0', True, True]])
    pd.testing.assert_frame_equal(expected, n.get_2_windings_transformers(), check_dtype=False, atol=1e-2)
    n = pp.network.create_four_substations_node_breaker_network()
    twt = n.get_2_windings_transformers(attributes=['bus_breaker_bus1_id', 'node1', 'bus_breaker_bus2_id', 'node2'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['TWT']),
        columns=['bus_breaker_bus1_id', 'node1', 'bus_breaker_bus2_id', 'node2'],
        data=[['S1VL1_4', 4, 'S1VL2_3', 3]])
    pd.testing.assert_frame_equal(expected, twt, check_dtype=False, atol=1e-2)


def test_voltage_levels_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    voltage_levels = n.get_voltage_levels(all_attributes=True)
    vlgen = voltage_levels.loc['VLGEN']
    assert 24.0 == vlgen['nominal_v']
    assert 'BUS_BREAKER' == vlgen['topology_kind']


def test_substations_data_frame():
    n = pp.network.create_eurostag_tutorial_example1_network()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['P1', 'P2']),
                            columns=['name', 'TSO', 'geo_tags', 'country'],
                            data=[['', 'RTE', 'A', 'FR'],
                                  ['', 'RTE', 'B', 'BE']])
    pd.testing.assert_frame_equal(expected, n.get_substations(), check_dtype=False, atol=1e-2)
    n.update_substations(id='P2', TSO='REE', country='ES')
    expected = pd.DataFrame(index=pd.Series(name='id', data=['P1', 'P2']),
                            columns=['name', 'TSO', 'geo_tags', 'country'],
                            data=[['', 'RTE', 'A', 'FR'],
                                  ['', 'REE', 'B', 'ES']])
    pd.testing.assert_frame_equal(expected, n.get_substations(), check_dtype=False, atol=1e-2)


def test_reactive_capability_curve_points_data_frame():
    n = pp.network.create_four_substations_node_breaker_network()
    points = n.get_reactive_capability_curve_points()
    assert 0 == pytest.approx(points.loc['GH1']['p'][0])
    assert 100 == pytest.approx(points.loc['GH1']['p'][1])
    assert -769.3 == pytest.approx(points.loc['GH1']['min_q'][0])
    assert -864.55 == pytest.approx(points.loc['GH1']['min_q'][1])
    assert 860 == pytest.approx(points.loc['GH1']['max_q'][0])
    assert 946.25 == pytest.approx(points.loc['GH1']['max_q'][1])


def test_exception():
    n = pp.network.create_ieee14()
    with pytest.raises(pp.PyPowsyblError) as e:
        n.open_switch("aa")
    assert "Switch 'aa' not found" == str(e.value)


def test_ratio_tap_changers():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = n.get_ratio_tap_changers(all_attributes=True)
    assert not df['fictitious']['NHV2_NLOAD']
    expected = pd.DataFrame(index=pd.Series(name='id', data=['NHV2_NLOAD']),
                            columns=['tap', 'low_tap', 'high_tap', 'step_count', 'on_load', 'regulating',
                                     'target_v', 'target_deadband', 'regulating_bus_id', 'rho',
                                     'alpha'],
                            data=[[1, 0, 2, 3, True, True, 158.0, 0.0, 'VLLOAD_0', 0.4, NaN]])
    pd.testing.assert_frame_equal(expected, n.get_ratio_tap_changers(), check_dtype=False, atol=1e-2)
    update = pd.DataFrame(index=['NHV2_NLOAD'],
                          columns=['tap', 'regulating', 'target_v'],
                          data=[[0, False, 180]])
    n.update_ratio_tap_changers(update)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['NHV2_NLOAD']),
                            columns=['tap', 'low_tap', 'high_tap', 'step_count', 'on_load', 'regulating',
                                     'target_v', 'target_deadband', 'regulating_bus_id', 'rho',
                                     'alpha'],
                            data=[[0, 0, 2, 3, True, False, 180.0, 0.0, 'VLLOAD_0', 0.34, NaN]])
    pd.testing.assert_frame_equal(expected, n.get_ratio_tap_changers(), check_dtype=False, atol=1e-2)


def test_phase_tap_changers():
    n = pp.network.create_four_substations_node_breaker_network()
    df = n.get_phase_tap_changers(all_attributes=True)
    assert not df['fictitious']['TWT']
    tap_changers = n.get_phase_tap_changers()
    assert ['tap', 'low_tap', 'high_tap', 'step_count', 'regulating', 'regulation_mode',
            'regulation_value', 'target_deadband', 'regulating_bus_id'] == tap_changers.columns.tolist()
    twt_values = tap_changers.loc['TWT']
    assert 15 == twt_values.tap
    assert 0 == twt_values.low_tap
    assert 32 == twt_values.high_tap
    assert 33 == twt_values.step_count
    assert not twt_values.regulating
    assert 'FIXED_TAP' == twt_values.regulation_mode
    assert pd.isna(twt_values.regulation_value)
    assert pd.isna(twt_values.target_deadband)
    update = pd.DataFrame(index=['TWT'],
                          columns=['tap', 'target_deadband', 'regulation_value', 'regulation_mode', 'regulating'],
                          data=[[10, 100, 1000, 'CURRENT_LIMITER', True]])
    n.update_ratio_tap_changers(id='TWT', regulating=False)
    n.update_phase_tap_changers(update)
    tap_changers = n.get_phase_tap_changers()
    assert ['tap', 'low_tap', 'high_tap', 'step_count', 'regulating', 'regulation_mode',
            'regulation_value', 'target_deadband', 'regulating_bus_id'] == tap_changers.columns.tolist()
    twt_values = tap_changers.loc['TWT']
    assert 10 == twt_values.tap
    assert twt_values.regulating
    assert 'CURRENT_LIMITER' == twt_values.regulation_mode
    assert 1000 == pytest.approx(twt_values.regulation_value)
    assert 100 == pytest.approx(twt_values.target_deadband)


def test_variant():
    n = pp.network.load(str(TEST_DIR.joinpath('node-breaker.xiidm')))
    assert 'InitialState' == n.get_working_variant_id()
    n.clone_variant('InitialState', 'WorkingState')
    n.update_switches(pd.DataFrame(index=['BREAKER-BB2-VL1_VL2_1'], data={'open': [True]}))
    n.set_working_variant('WorkingState')
    assert 'WorkingState' == n.get_working_variant_id()
    assert ['InitialState', 'WorkingState'] == n.get_variant_ids()
    assert 0 == len(n.get_switches()[n.get_switches()['open']].index.tolist())
    n.set_working_variant('InitialState')
    n.remove_variant('WorkingState')
    assert ['BREAKER-BB2-VL1_VL2_1'] == n.get_switches()[n.get_switches()['open']].index.tolist()
    assert 'InitialState' == n.get_working_variant_id()
    assert 1 == len(n.get_variant_ids())


def test_layout_parameters():
    parameters = LayoutParameters()
    assert not parameters.use_name
    assert not parameters.center_name
    assert not parameters.diagonal_label
    assert parameters.topological_coloring
    assert not parameters.nodes_infos
    parameters = LayoutParameters(use_name=True, center_name=True, diagonal_label=True, topological_coloring=False,
                                  nodes_infos=True)
    assert parameters.use_name
    assert parameters.center_name
    assert parameters.diagonal_label
    assert not parameters.topological_coloring
    assert parameters.nodes_infos


def test_sld_svg():
    n = pp.network.create_four_substations_node_breaker_network()
    sld = n.get_single_line_diagram('S1VL1')
    assert re.search('.*<svg.*', sld.svg)
    assert len(sld.metadata) > 0
    sld1 = n.get_single_line_diagram('S1VL1', LayoutParameters(use_name=True, center_name=True, diagonal_label=True,
                                                               topological_coloring=False))
    assert re.search('.*<svg.*', sld1.svg)
    assert len(sld1.metadata) > 0
    sld2 = n.get_single_line_diagram('S1VL1', LayoutParameters(use_name=True, center_name=True, diagonal_label=True,
                                                               topological_coloring=True, nodes_infos=True))
    assert re.search('.*<svg.*', sld2.svg)
    assert len(sld2.metadata) > 0


def test_sld_nad():
    n = pp.network.create_ieee14()
    sld = n.get_network_area_diagram()
    assert re.search('.*<svg.*', sld.svg)
    sld = n.get_network_area_diagram(voltage_level_ids=None)
    assert re.search('.*<svg.*', sld.svg)
    sld = n.get_network_area_diagram('VL1')
    assert re.search('.*<svg.*', sld.svg)
    sld = n.get_network_area_diagram(['VL1', 'VL2'])
    assert re.search('.*<svg.*', sld.svg)
    sld = n.get_network_area_diagram('VL6', high_nominal_voltage_bound=50, low_nominal_voltage_bound=10, depth=10)
    assert re.search('.*<svg.*', sld.svg)
    sld = n.get_network_area_diagram('VL6', low_nominal_voltage_bound=10, depth=10)
    assert re.search('.*<svg.*', sld.svg)
    sld = n.get_network_area_diagram('VL6', high_nominal_voltage_bound=50, depth=10)
    assert re.search('.*<svg.*', sld.svg)
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        test_svg = tmp_dir_name + "test.svg"
        n.write_network_area_diagram_svg(test_svg, None)
        n.write_network_area_diagram_svg(test_svg, ['VL1'])
        n.write_network_area_diagram_svg(test_svg, ['VL1', 'VL2'])
        n.write_network_area_diagram_svg('VL6', high_nominal_voltage_bound=50, low_nominal_voltage_bound=10, depth=10)
        n.write_network_area_diagram_svg('VL6', low_nominal_voltage_bound=10, depth=10)
        n.write_network_area_diagram_svg('VL6', high_nominal_voltage_bound=50, depth=10)


def test_current_limits():
    network = pp.network.create_eurostag_tutorial_example1_network()
    assert 9 == len(network.get_current_limits())
    assert 5 == len(network.get_current_limits().loc['NHV1_NHV2_1'])
    current_limit = network.get_current_limits().loc['NHV1_NHV2_1', '10\'']
    expected = pd.DataFrame(index=pd.MultiIndex.from_tuples(names=['branch_id', 'name'],
                                                            tuples=[('NHV1_NHV2_1', '10\'')]),
                            columns=['side', 'value', 'acceptable_duration'],
                            data=[['TWO', 1200.0, 600]])
    pd.testing.assert_frame_equal(expected, current_limit, check_dtype=False)


def test_deep_copy():
    n = pp.network.create_eurostag_tutorial_example1_network()
    copy_n = copy.deepcopy(n)
    assert copy_n.id == "sim1"
    assert ['NGEN_NHV1', 'NHV2_NLOAD'] == copy_n.get_elements_ids(pp.network.ElementType.TWO_WINDINGS_TRANSFORMER)


def test_lines():
    n = pp.network.create_four_substations_node_breaker_network()
    df = n.get_lines(all_attributes=True)
    assert not df['fictitious']['LINE_S2S3']
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
                            columns=['name', 'r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                                     'voltage_level1_id',
                                     'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1', 'connected2'],
                            data=[
                                ['', 0.01, 19.1, 0, 0, 0, 0, 109.889, 190.023, 309.979, -109.886, -184.517, 309.978,
                                 'S2VL1',
                                 'S3VL1',
                                 'S2VL1_0', 'S3VL1_0', True, True],
                                ['', 0.01, 13.1, 0, 0, 0, 0, 240.004, 2.1751, 346.43, -240, 2.5415, 346.43, 'S3VL1',
                                 'S4VL1',
                                 'S3VL1_0', 'S4VL1_0', True, True]])
    pd.testing.assert_frame_equal(expected, n.get_lines(), check_dtype=False)
    lines_update = pd.DataFrame(index=['LINE_S2S3'],
                                columns=['r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'p2', 'q2'],
                                data=[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]])
    n.update_lines(lines_update)
    expected = pd.DataFrame(index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
                            columns=['name', 'r', 'x', 'g1', 'b1', 'g2', 'b2', 'p1', 'q1', 'i1', 'p2', 'q2', 'i2',
                                     'voltage_level1_id', 'voltage_level2_id', 'bus1_id', 'bus2_id', 'connected1',
                                     'connected2'],
                            data=[['', 1, 2, 3, 4, 5, 6, 7, 8, 15.011282, 9, 10, 19.418634,
                                   'S2VL1', 'S3VL1', 'S2VL1_0', 'S3VL1_0', True, True],
                                  ['', 0.01, 13.1, 0, 0, 0, 0, 240.004, 2.1751, 346.429584, -240, 2.5415,
                                   346.429584, 'S3VL1', 'S4VL1', 'S3VL1_0', 'S4VL1_0', True, True]])
    pd.testing.assert_frame_equal(expected, n.get_lines(), check_dtype=False)
    lines = n.get_lines()
    pd.testing.assert_frame_equal(expected, lines, check_dtype=False)
    lines = n.get_lines(attributes=['bus_breaker_bus1_id', 'node1', 'bus_breaker_bus2_id', 'node2'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['LINE_S2S3', 'LINE_S3S4']),
        columns=['bus_breaker_bus1_id', 'node1', 'bus_breaker_bus2_id', 'node2'],
        data=[['S2VL1_6', 6, 'S3VL1_2', 2], ['S3VL1_8', 8, 'S4VL1_6', 6]])
    pd.testing.assert_frame_equal(expected, lines, check_dtype=False, atol=1e-2)


def test_dangling_lines():
    n = util.create_dangling_lines_network()
    df = n.get_dangling_lines(all_attributes=True)
    assert not df['fictitious']['DL']
    expected = pd.DataFrame(index=pd.Series(name='id', data=['DL']),
                            columns=['name', 'r', 'x', 'g', 'b', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id',
                                     'bus_id',
                                     'connected', 'ucte-x-node-code', 'tie_line_id'],
                            data=[['', 10.0, 1.0, 0.0001, 0.00001, 50.0, 30.0, NaN, NaN, NaN, 'VL', 'VL_0', True,
                                   '', '']])
    pd.testing.assert_frame_equal(expected, n.get_dangling_lines(), check_dtype=False)
    n.update_dangling_lines(
        pd.DataFrame(index=['DL'], columns=['r', 'x', 'g', 'b', 'p0', 'q0', 'connected'],
                     data=[[11.0, 1.1, 0.0002, 0.00002, 40.0, 40.0, False]]))
    updated = pd.DataFrame(index=pd.Series(name='id', data=['DL']),
                           columns=['name', 'r', 'x', 'g', 'b', 'p0', 'q0', 'p', 'q', 'i', 'voltage_level_id',
                                    'bus_id',
                                    'connected', 'ucte-x-node-code', 'tie_line_id'],
                           data=[['', 11.0, 1.1, 0.0002, 0.00002, 40.0, 40.0, NaN, NaN, NaN, 'VL', '', False, '', '']])
    pd.testing.assert_frame_equal(updated, n.get_dangling_lines(), check_dtype=False)
    n = util.create_dangling_lines_network()
    dangling_lines = n.get_dangling_lines(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['DL']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['BUS', -1]])
    pd.testing.assert_frame_equal(expected, dangling_lines, check_dtype=False, atol=1e-2)


def test_batteries():
    n = util.create_battery_network()
    df = n.get_batteries(all_attributes=True)
    assert not df['fictitious']['BAT']
    expected = pd.DataFrame(index=pd.Series(name='id', data=['BAT', 'BAT2']),
                            columns=['name', 'max_p', 'min_p', 'min_q', 'max_q', 'reactive_limits_kind', 'target_p',
                                     'target_q',
                                     'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 9999.99, -9999.99, -9999.99, 9999.99, 'MIN_MAX', 9999.99, 9999.99, -605, -225,
                                   NaN, 'VLBAT', 'VLBAT_0', True],
                                  ['', 200, -200, NaN, NaN, 'CURVE', 100, 200, -605, -225, NaN, 'VLBAT', 'VLBAT_0',
                                   True]])
    pd.testing.assert_frame_equal(expected, n.get_batteries(), check_dtype=False)
    n.update_batteries(pd.DataFrame(index=['BAT2'], columns=['target_p', 'target_q'], data=[[50, 100]]))
    n.update_batteries(pd.DataFrame(index=['BAT'], columns=['min_q', 'max_q'], data=[[-500, 500]]))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['BAT', 'BAT2']),
                            columns=['name', 'max_p', 'min_p', 'min_q', 'max_q', 'reactive_limits_kind', 'target_p',
                                     'target_q',
                                     'p', 'q', 'i', 'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 9999.99, -9999.99, -500, 500, 'MIN_MAX', 9999.99, 9999.99, -605, -225, NaN,
                                   'VLBAT', 'VLBAT_0', True],
                                  ['', 200, -200, NaN, NaN, 'CURVE', 50, 100, -605, -225, NaN, 'VLBAT', 'VLBAT_0',
                                   True]])
    pd.testing.assert_frame_equal(expected, n.get_batteries(), check_dtype=False)


def test_shunt():
    n = pp.network.create_four_substations_node_breaker_network()
    df = n.get_shunt_compensators(all_attributes=True)
    assert not df['fictitious']['SHUNT']
    expected = pd.DataFrame(index=pd.Series(name='id', data=['SHUNT']),
                            columns=['name', 'g', 'b', 'model_type', 'max_section_count', 'section_count',
                                     'voltage_regulation_on', 'target_v',
                                     'target_deadband', 'regulating_bus_id', 'p', 'q', 'i',
                                     'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 0.0, -0.012, 'LINEAR', 1, 1, False, NaN, NaN,
                                   'S1VL2_0', NaN, 1920, NaN, 'S1VL2', 'S1VL2_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_shunt_compensators(), check_dtype=False)
    n.update_shunt_compensators(
        pd.DataFrame(index=['SHUNT'],
                     columns=['q', 'section_count', 'target_v', 'target_deadband',
                              'connected'],
                     data=[[1900, 0, 50, 3, False]]))
    n.update_shunt_compensators(
        pd.DataFrame(index=['SHUNT'],
                     columns=['voltage_regulation_on'],
                     data=[[True]]))
    expected = pd.DataFrame(index=pd.Series(name='id', data=['SHUNT']),
                            columns=['name', 'g', 'b', 'model_type', 'max_section_count', 'section_count',
                                     'voltage_regulation_on', 'target_v',
                                     'target_deadband', 'regulating_bus_id', 'p', 'q', 'i',
                                     'voltage_level_id', 'bus_id', 'connected'],
                            data=[['', 0.0, -0.0, 'LINEAR', 1, 0, True, 50, 3,
                                   '', NaN, 1900, NaN, 'S1VL2', '', False]])
    pd.testing.assert_frame_equal(expected, n.get_shunt_compensators(), check_dtype=False)
    shunts = n.get_shunt_compensators(attributes=['bus_breaker_bus_id', 'node'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['SHUNT']),
        columns=['bus_breaker_bus_id', 'node'],
        data=[['S1VL2_19', 19]])
    pd.testing.assert_frame_equal(expected, shunts, check_dtype=False, atol=1e-2)


def test_3_windings_transformers():
    n = util.create_three_windings_transformer_network()
    expected = pd.DataFrame(index=pd.Series(name='id', data=['3WT']),
                            columns=['name', 'rated_u0', 'r1', 'x1', 'g1', 'b1', 'rated_u1', 'rated_s1',
                                     'ratio_tap_position1', 'phase_tap_position1', 'p1', 'q1', 'i1',
                                     'voltage_level1_id', 'bus1_id', 'connected1', 'r2', 'x2', 'g2', 'b2',
                                     'rated_u2', 'rated_s2', 'ratio_tap_position2', 'phase_tap_position2', 'p2',
                                     'q2', 'i2', 'voltage_level2_id', 'bus2_id', 'connected2', 'r3', 'x3', 'g3',
                                     'b3', 'rated_u3', 'rated_s3', 'ratio_tap_position3', 'phase_tap_position3',
                                     'p3', 'q3', 'i3', 'voltage_level3_id', 'bus3_id', 'connected3'],
                            data=[['', 132, 17.424, 1.7424, 0.00573921, 0.000573921, 132, NaN, -99999, -99999, NaN,
                                   NaN,
                                   NaN, 'VL_132', 'VL_132_0', True, 1.089, 0.1089, 0, 0, 33, NaN, 2, -99999, NaN,
                                   NaN, NaN, 'VL_33', 'VL_33_0', True, 0.121, 0.0121, 0, 0, 11, NaN, 0, -99999, NaN,
                                   NaN, NaN, 'VL_11', 'VL_11_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_3_windings_transformers(), check_dtype=False)
    t3wt = n.get_3_windings_transformers(
        attributes=['bus_breaker_bus1_id', 'node1', 'bus_breaker_bus2_id', 'node2', 'bus_breaker_bus3_id', 'node3'])
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['3WT']),
        columns=['bus_breaker_bus1_id', 'node1', 'bus_breaker_bus2_id', 'node2', 'bus_breaker_bus3_id', 'node3'],
        data=[['BUS_132', -1, 'BUS_33', -1, 'BUS_11', -1]])
    pd.testing.assert_frame_equal(expected, t3wt, check_dtype=False, atol=1e-2)
    df = n.get_3_windings_transformers(all_attributes=True)
    assert not df['fictitious']['3WT']
    # test update
    n.update_3_windings_transformers(id='3WT', r1=20, x1=2, g1=0.008, b1=0.0007, rated_u1=125, rated_s1=10,
                                     ratio_tap_position1=1, phase_tap_position1=1, r2=22, x2=1, g2=0.009, b2=0.0009,
                                     rated_u2=127, rated_s2=11,
                                     ratio_tap_position2=1, phase_tap_position2=1, r3=24, x3=3, g3=0.01, b3=0.001,
                                     rated_u3=129, rated_s3=12,
                                     ratio_tap_position3=2, phase_tap_position3=1, connected3=False, fictitious=True)
    t3wt = n.get_3_windings_transformers(all_attributes=True).loc['3WT']
    assert 20 == t3wt.r1
    assert 2 == t3wt.x1
    assert 0.008 == t3wt.g1
    assert 0.0007 == t3wt.b1
    assert 125 == t3wt.rated_u1
    assert 10 == t3wt.rated_s1
    assert -99999 == t3wt.ratio_tap_position1
    assert -99999 == t3wt.phase_tap_position1
    assert 22 == t3wt.r2
    assert 1 == t3wt.x2
    assert 0.009 == t3wt.g2
    assert 0.0009 == t3wt.b2
    assert 127 == t3wt.rated_u2
    assert 11 == t3wt.rated_s2
    assert 1 == t3wt.ratio_tap_position2
    assert -99999 == t3wt.phase_tap_position2
    assert 24 == t3wt.r3
    assert 3 == t3wt.x3
    assert 0.01 == t3wt.g3
    assert 0.001 == t3wt.b3
    assert 129 == t3wt.rated_u3
    assert 12 == t3wt.rated_s3
    assert 2 == t3wt.ratio_tap_position3
    assert -99999 == t3wt.phase_tap_position3
    assert not t3wt.connected3
    assert t3wt.fictitious


def test_busbar_sections():
    n = pp.network.create_four_substations_node_breaker_network()
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['S1VL1_BBS', 'S1VL2_BBS1', 'S1VL2_BBS2', 'S2VL1_BBS', 'S3VL1_BBS',
                                                  'S4VL1_BBS']),
                            columns=['name', 'v', 'angle', 'voltage_level_id', 'bus_id', 'connected'],
                            data=[['S1VL1_BBS', 224.6139, 2.2822, 'S1VL1', 'S1VL1_0', True],
                                  ['S1VL2_BBS1', 400.0000, 0.0000, 'S1VL2', 'S1VL2_0', True],
                                  ['S1VL2_BBS2', 400.0000, 0.0000, 'S1VL2', 'S1VL2_0', True],
                                  ['S2VL1_BBS', 408.8470, 0.7347, 'S2VL1', 'S2VL1_0', True],
                                  ['S3VL1_BBS', 400.0000, 0.0000, 'S3VL1', 'S3VL1_0', True],
                                  ['S4VL1_BBS', 400.0000, -1.1259, 'S4VL1', 'S4VL1_0', True]])
    pd.testing.assert_frame_equal(expected, n.get_busbar_sections(), check_dtype=False)

    n.update_busbar_sections(
        pd.DataFrame(index=['S1VL1_BBS'],
                     columns=['fictitious'],
                     data=[[True]]))
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['S1VL1_BBS', 'S1VL2_BBS1', 'S1VL2_BBS2', 'S2VL1_BBS', 'S3VL1_BBS',
                                                  'S4VL1_BBS']),
                            columns=['name', 'v', 'angle', 'voltage_level_id', 'bus_id', 'connected', 'fictitious'],
                            data=[['S1VL1_BBS', 224.6139, 2.2822, 'S1VL1', 'S1VL1_0', True, True],
                                  ['S1VL2_BBS1', 400.0000, 0.0000, 'S1VL2', 'S1VL2_0', True, False],
                                  ['S1VL2_BBS2', 400.0000, 0.0000, 'S1VL2', 'S1VL2_0', True, False],
                                  ['S2VL1_BBS', 408.8470, 0.7347, 'S2VL1', 'S2VL1_0', True, False],
                                  ['S3VL1_BBS', 400.0000, 0.0000, 'S3VL1', 'S3VL1_0', True, False],
                                  ['S4VL1_BBS', 400.0000, -1.1259, 'S4VL1', 'S4VL1_0', True, False]])
    pd.testing.assert_frame_equal(expected, n.get_busbar_sections(all_attributes=True), check_dtype=False)


def test_non_linear_shunt():
    n = util.create_non_linear_shunt_network()
    non_linear_shunt_sections = n.get_non_linear_shunt_compensator_sections()
    pd.testing.assert_series_equal(non_linear_shunt_sections.loc[('SHUNT', 0)],
                                   pd.Series(data={'g': 0.0, 'b': 0.00001},
                                             name=('SHUNT', 0)), check_dtype=False)
    pd.testing.assert_series_equal(non_linear_shunt_sections.loc[('SHUNT', 1)],
                                   pd.Series(data={'g': 0.3, 'b': 0.0200},
                                             name=('SHUNT', 1)), check_dtype=False)
    update = pd.DataFrame(index=pd.MultiIndex.from_tuples([('SHUNT', 0), ('SHUNT', 1)], names=['id', 'section']),
                          columns=['g', 'b'],
                          data=[[0.1, 0.00002],
                                [0.4, 0.03]])
    n.update_non_linear_shunt_compensator_sections(update)
    non_linear_shunt_sections = n.get_non_linear_shunt_compensator_sections()
    pd.testing.assert_series_equal(non_linear_shunt_sections.loc[('SHUNT', 0)],
                                   pd.Series(data={'g': 0.1, 'b': 0.00002},
                                             name=('SHUNT', 0)), check_dtype=False)
    pd.testing.assert_series_equal(non_linear_shunt_sections.loc[('SHUNT', 1)],
                                   pd.Series(data={'g': 0.4, 'b': 0.03},
                                             name=('SHUNT', 1)), check_dtype=False)


def test_voltage_levels():
    n = pp.network.create_eurostag_tutorial_example1_network()
    df = n.get_voltage_levels(all_attributes=True)
    assert not df['fictitious']['VLGEN']
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['VLGEN', 'VLHV1', 'VLHV2', 'VLLOAD']),
                            columns=['name', 'substation_id', 'nominal_v', 'high_voltage_limit',
                                     'low_voltage_limit'],
                            data=[['', 'P1', 24, NaN, NaN],
                                  ['', 'P1', 380, 500, 400],
                                  ['', 'P2', 380, 500, 300],
                                  ['', 'P2', 150, NaN, NaN]])
    pd.testing.assert_frame_equal(expected, n.get_voltage_levels(), check_dtype=False)
    n.update_voltage_levels(id=['VLGEN', 'VLLOAD'], nominal_v=[25, 151], high_voltage_limit=[50, 175],
                            low_voltage_limit=[20, 125])
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['VLGEN', 'VLHV1', 'VLHV2', 'VLLOAD']),
                            columns=['name', 'substation_id', 'nominal_v', 'high_voltage_limit',
                                     'low_voltage_limit'],
                            data=[['', 'P1', 25, 50, 20],
                                  ['', 'P1', 380, 500, 400],
                                  ['', 'P2', 380, 500, 300],
                                  ['', 'P2', 151, 175, 125]])
    pd.testing.assert_frame_equal(expected, n.get_voltage_levels(), check_dtype=False)


def test_update_with_keywords():
    n = util.create_non_linear_shunt_network()
    n.update_non_linear_shunt_compensator_sections(id='SHUNT', section=0, g=0.2, b=0.000001)
    assert 0.2 == n.get_non_linear_shunt_compensator_sections().loc['SHUNT', 0]['g']
    assert 0.000001 == n.get_non_linear_shunt_compensator_sections().loc['SHUNT', 0]['b']


def test_update_generators_with_keywords():
    n = pp.network.create_four_substations_node_breaker_network()
    n.update_generators(id=['GTH1', 'GTH2'], target_p=[200, 300])
    assert [200, 300] == n.get_generators().loc[['GTH1', 'GTH2'], 'target_p'].to_list()


def test_update_generators_minax_reactive_limits():
    n = pp.network.create_micro_grid_be_network()
    generators = n.get_generators()
    gen_with_min_max_reactive_limits = '550ebe0d-f2b2-48c1-991f-cebea43a21aa'
    assert 'MIN_MAX' == generators['reactive_limits_kind'][gen_with_min_max_reactive_limits]
    assert -200.0 == generators['min_q'][gen_with_min_max_reactive_limits]
    assert 200.0 == generators['max_q'][gen_with_min_max_reactive_limits]
    n.update_generators(id=[gen_with_min_max_reactive_limits], min_q=[-205], max_q=[205])
    generators = n.get_generators()
    assert 'MIN_MAX' == generators['reactive_limits_kind'][gen_with_min_max_reactive_limits]
    assert -205.0 == generators['min_q'][gen_with_min_max_reactive_limits]
    assert 205.0 == generators['max_q'][gen_with_min_max_reactive_limits]
    gen_with_curve_reactive_limits = '3a3b27be-b18b-4385-b557-6735d733baf0'
    with pytest.raises(pp.PyPowsyblError):
        n.update_generators(id=[gen_with_curve_reactive_limits], min_q=[-200])


def test_invalid_update_kwargs():
    n = pp.network.create_four_substations_node_breaker_network()

    with pytest.raises(RuntimeError) as context:
        n.update_generators(df=pd.DataFrame(index=['GTH1'], columns=['target_p'], data=[300]),
                            id='GTH1', target_p=300)
    assert 'only one form' in str(context)

    with pytest.raises(ValueError) as context:
        n.update_generators(id=['GTH1', 'GTH2'], target_p=100)
    assert 'same size' in str(context)

    with pytest.raises(ValueError) as context:
        n.update_generators(id=np.array(0, ndmin=3))
    assert 'dimensions' in str(context)


def test_create_network():
    n = pp.network.create_ieee9()
    assert 'ieee9cdf' == n.id
    n = pp.network.create_ieee30()
    assert 'ieee30cdf' == n.id
    n = pp.network.create_ieee57()
    assert 'ieee57cdf' == n.id
    n = pp.network.create_ieee118()
    assert 'ieee118cdf' == n.id


def test_node_breaker_view():
    n = pp.network.create_four_substations_node_breaker_network()
    topology = n.get_node_breaker_topology('S4VL1')
    switches = topology.switches
    nodes = topology.nodes
    assert 6 == len(switches)
    assert 'S4VL1_BBS_LINES3S4_DISCONNECTOR' == switches.loc['S4VL1_BBS_LINES3S4_DISCONNECTOR']['name']
    assert 'DISCONNECTOR' == switches.loc['S4VL1_BBS_LINES3S4_DISCONNECTOR']['kind']
    assert not switches.loc['S4VL1_BBS_LINES3S4_DISCONNECTOR']['open']
    assert 0 == switches.loc['S4VL1_BBS_LINES3S4_DISCONNECTOR']['node1']
    assert 5 == switches.loc['S4VL1_BBS_LINES3S4_DISCONNECTOR']['node2']
    assert 7 == len(nodes)
    assert topology.internal_connections.empty


def test_graph():
    n = pp.network.create_four_substations_node_breaker_network()
    network_topology = n.get_node_breaker_topology('S4VL1')
    graph = network_topology.create_graph()
    assert 7 == len(graph.nodes)
    assert [(0, 5), (0, 1), (0, 3), (1, 2), (3, 4), (5, 6)] == list(graph.edges)


@unittest.skip("plot graph skipping")
def test_node_breaker_view_draw_graph():
    n = pp.network.create_four_substations_node_breaker_network()
    network_topology = n.get_node_breaker_topology('S4VL1')
    graph = network_topology.create_graph()
    nx.draw_shell(graph, with_labels=True)
    plt.show()


def test_network_merge():
    be = pp.network.create_micro_grid_be_network()
    assert 6 == len(be.get_voltage_levels())
    nl = pp.network.create_micro_grid_nl_network()
    assert 4 == len(nl.get_voltage_levels())
    be.merge(nl)
    assert 10 == len(be.get_voltage_levels())


def test_linear_shunt_compensator_sections():
    n = pp.network.create_four_substations_node_breaker_network()
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['SHUNT']),
                            columns=['g_per_section', 'b_per_section', 'max_section_count'],
                            data=[[NaN, -0.012, 1]])
    pd.testing.assert_frame_equal(expected, n.get_linear_shunt_compensator_sections(), check_dtype=False)
    n.update_linear_shunt_compensator_sections(
        pd.DataFrame(index=['SHUNT'],
                     columns=['g_per_section', 'b_per_section', 'max_section_count'],
                     data=[[0.14, -0.01, 4]]))
    expected = pd.DataFrame(index=pd.Series(name='id',
                                            data=['SHUNT']),
                            columns=['g_per_section', 'b_per_section', 'max_section_count'],
                            data=[[0.14, -0.01, 4]])
    pd.testing.assert_frame_equal(expected, n.get_linear_shunt_compensator_sections(), check_dtype=False)
    n.update_linear_shunt_compensator_sections(id='SHUNT', g_per_section=0.15, b_per_section=-0.02)
    assert 0.15 == n.get_linear_shunt_compensator_sections().loc['SHUNT']['g_per_section']
    assert -0.02 == n.get_linear_shunt_compensator_sections().loc['SHUNT']['b_per_section']


def test_bus_breaker_view():
    n = pp.network.create_four_substations_node_breaker_network()
    n.update_switches(pd.DataFrame(index=['S1VL2_COUPLER'], data={'open': [True]}))
    topology = n.get_bus_breaker_topology('S1VL2')
    switches = topology.switches
    buses = topology.buses
    elements = topology.elements
    expected_switches = pd.DataFrame(index=pd.Series(name='id',
                                                     data=['S1VL2_TWT_BREAKER', 'S1VL2_VSC1_BREAKER',
                                                           'S1VL2_GH1_BREAKER', 'S1VL2_GH2_BREAKER',
                                                           'S1VL2_GH3_BREAKER', 'S1VL2_LD2_BREAKER',
                                                           'S1VL2_LD3_BREAKER', 'S1VL2_LD4_BREAKER',
                                                           'S1VL2_SHUNT_BREAKER', 'S1VL2_LCC1_BREAKER',
                                                           'S1VL2_COUPLER']),
                                     columns=['kind', 'open', 'bus1_id', 'bus2_id'],
                                     data=[['BREAKER', False, 'S1VL2_0', 'S1VL2_3'],
                                           ['BREAKER', False, 'S1VL2_1', 'S1VL2_5'],
                                           ['BREAKER', False, 'S1VL2_0', 'S1VL2_7'],
                                           ['BREAKER', False, 'S1VL2_0', 'S1VL2_9'],
                                           ['BREAKER', False, 'S1VL2_0', 'S1VL2_11'],
                                           ['BREAKER', False, 'S1VL2_1', 'S1VL2_13'],
                                           ['BREAKER', False, 'S1VL2_1', 'S1VL2_15'],
                                           ['BREAKER', False, 'S1VL2_1', 'S1VL2_17'],
                                           ['BREAKER', False, 'S1VL2_0', 'S1VL2_19'],
                                           ['BREAKER', False, 'S1VL2_1', 'S1VL2_21'],
                                           ['BREAKER', True, 'S1VL2_0', 'S1VL2_1']])
    expected_buses = pd.DataFrame(index=pd.Series(name='id',
                                                  data=['S1VL2_0', 'S1VL2_1', 'S1VL2_3', 'S1VL2_5', 'S1VL2_7',
                                                        'S1VL2_9', 'S1VL2_11', 'S1VL2_13', 'S1VL2_15', 'S1VL2_17',
                                                        'S1VL2_19', 'S1VL2_21']),
                                  columns=['name', 'bus_id'],
                                  data=[['', 'S1VL2_0'], ['', 'S1VL2_1'], ['', 'S1VL2_0'], ['', 'S1VL2_1'],
                                        ['', 'S1VL2_0'], ['', 'S1VL2_0'], ['', 'S1VL2_0'], ['', 'S1VL2_1'],
                                        ['', 'S1VL2_1'], ['', 'S1VL2_1'], ['', 'S1VL2_0'], ['', 'S1VL2_1']])

    expected_elements = pd.DataFrame.from_records(index='id', columns=['id', 'type', 'bus_id', 'side'],
                                                  data=[
                                                      ('S1VL2_BBS1', 'BUSBAR_SECTION', 'S1VL2_0', ''),
                                                      ('S1VL2_BBS2', 'BUSBAR_SECTION', 'S1VL2_1', ''),
                                                      ('TWT', 'TWO_WINDINGS_TRANSFORMER', 'S1VL2_3', 'TWO'),
                                                      ('VSC1', 'HVDC_CONVERTER_STATION', 'S1VL2_5', ''),
                                                      ('GH1', 'GENERATOR', 'S1VL2_7', ''),
                                                      ('GH2', 'GENERATOR', 'S1VL2_9', ''),
                                                      ('GH3', 'GENERATOR', 'S1VL2_11', ''),
                                                      ('LD2', 'LOAD', 'S1VL2_13', ''),
                                                      ('LD3', 'LOAD', 'S1VL2_15', ''),
                                                      ('LD4', 'LOAD', 'S1VL2_17', ''),
                                                      ('SHUNT', 'SHUNT_COMPENSATOR', 'S1VL2_19', ''),
                                                      ('LCC1', 'HVDC_CONVERTER_STATION', 'S1VL2_21', ''),
                                                  ])
    pd.testing.assert_frame_equal(expected_switches, switches, check_dtype=False)
    pd.testing.assert_frame_equal(expected_buses, buses, check_dtype=False)
    pd.testing.assert_frame_equal(expected_elements, elements, check_dtype=False)


def test_bb_topology_with_no_bus_view_bus_does_not_throw():
    n = pp.network.create_empty()
    n.create_substations(id='S')
    n.create_voltage_levels(id='VL', substation_id='S', nominal_v=380, topology_kind='NODE_BREAKER')
    n.create_busbar_sections(id='BB', voltage_level_id='VL', node=0)
    n.create_loads(id='L', voltage_level_id='VL', p0=0, q0=0, node=1)
    n.create_switches(id='SW', kind='DISCONNECTOR', voltage_level_id='VL', node1=0, node2=1, open=True)

    # must not throw
    topo = n.get_bus_breaker_topology('VL')
    assert topo.buses.index.to_list() == ['VL_0', 'VL_1']


def test_not_connected_bus_breaker():
    n = pp.network.create_eurostag_tutorial_example1_network()
    expected = pd.DataFrame.from_records(index='id', data=[{'id': 'NHV1', 'name': '', 'bus_id': 'VLHV1_0'}])
    pd.testing.assert_frame_equal(expected, n.get_bus_breaker_topology('VLHV1').buses, check_dtype=False)
    n.update_lines(id=['NHV1_NHV2_1', 'NHV1_NHV2_2'], connected1=[False, False], connected2=[False, False])
    n.update_2_windings_transformers(id='NGEN_NHV1', connected1=False, connected2=False)

    topo = n.get_bus_breaker_topology('VLHV1')
    bb_bus = topo.buses.loc['NHV1']
    assert '' == bb_bus['name']
    assert '' == bb_bus['bus_id']

    line = topo.elements.loc['NHV1_NHV2_1']
    assert '' == line['bus_id']


def test_graph_busbreakerview():
    n = pp.network.create_four_substations_node_breaker_network()
    network_topology = n.get_bus_breaker_topology('S4VL1')
    graph = network_topology.create_graph()
    assert 4 == len(graph.nodes)
    assert [('S4VL1_0', 'S4VL1_6'), ('S4VL1_0', 'S4VL1_2'), ('S4VL1_0', 'S4VL1_4')] == list(graph.edges)


@unittest.skip("plot graph skipping")
def test_bus_breaker_view_draw_graph():
    n = pp.network.create_four_substations_node_breaker_network()
    network_topology = n.get_bus_breaker_topology('S1VL2')
    graph = network_topology.create_graph()
    nx.draw_shell(graph, with_labels=True)
    plt.show()


def test_dataframe_attributes_filtering():
    n = pp.network.create_eurostag_tutorial_example1_network()
    buses_selected_attributes = n.get_buses(attributes=['v_mag', 'voltage_level_id'])
    expected = pd.DataFrame(index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
                            columns=['v_mag', 'voltage_level_id'],
                            data=[[NaN, 'VLGEN'],
                                  [380, 'VLHV1'],
                                  [380, 'VLHV2'],
                                  [NaN, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected, buses_selected_attributes, check_dtype=False)
    buses_default_attributes = n.get_buses(all_attributes=False)
    expected_default_attributes = pd.DataFrame(
        index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
        columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                 'voltage_level_id'],
        data=[['', NaN, NaN, 0, 0, 'VLGEN'],
              ['', 380, NaN, 0, 0, 'VLHV1'],
              ['', 380, NaN, 0, 0, 'VLHV2'],
              ['', NaN, NaN, 0, 0, 'VLLOAD']])
    pd.testing.assert_frame_equal(expected_default_attributes, buses_default_attributes, check_dtype=False)
    buses_empty = n.get_buses(attributes=[])
    expected_empty = expected_default_attributes[[]]
    assert expected_empty.empty
    assert buses_empty.empty

    buses_all_attributes = n.get_buses(all_attributes=True)
    expected_all_attributes = pd.DataFrame(
        index=pd.Series(name='id', data=['VLGEN_0', 'VLHV1_0', 'VLHV2_0', 'VLLOAD_0']),
        columns=['name', 'v_mag', 'v_angle', 'connected_component', 'synchronous_component',
                 'voltage_level_id', 'fictitious'],
        data=[['', NaN, NaN, 0, 0, 'VLGEN', False],
              ['', 380, NaN, 0, 0, 'VLHV1', False],
              ['', 380, NaN, 0, 0, 'VLHV2', False],
              ['', NaN, NaN, 0, 0, 'VLLOAD', False]])
    pd.testing.assert_frame_equal(expected_all_attributes, buses_all_attributes, check_dtype=False)
    with pytest.raises(RuntimeError) as e:
        n.get_buses(all_attributes=True, attributes=['v_mag', 'voltage_level_id'])
    assert "parameters \"all_attributes\" and \"attributes\" are mutually exclusive" in str(e)


def test_metadata():
    meta_gen = pp._pypowsybl.get_network_elements_dataframe_metadata(pp._pypowsybl.ElementType.GENERATOR)
    meta_gen_index_default = [x for x in meta_gen if (x.is_index == True) and (x.is_default == True)]
    assert len(meta_gen_index_default) > 0


def test_dataframe_elements_filtering():
    network_four_subs = pp.network.create_four_substations_node_breaker_network()
    network_micro_grid = pp.network.create_micro_grid_be_network()
    network_eurostag = pp.network.create_eurostag_tutorial_example1_network()
    network_non_linear_shunt = util.create_non_linear_shunt_network()
    network_with_batteries = pp.network.load(str(TEST_DIR.joinpath('battery.xiidm')))

    expected_selection = network_four_subs.get_2_windings_transformers().loc[['TWT']]
    filtered_selection = network_four_subs.get_2_windings_transformers(id='TWT')
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)
    expected_selection = network_micro_grid.get_3_windings_transformers().loc[
        ['84ed55f4-61f5-4d9d-8755-bba7b877a246']]
    filtered_selection = network_micro_grid.get_3_windings_transformers(
        id=['84ed55f4-61f5-4d9d-8755-bba7b877a246'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_micro_grid.get_shunt_compensators().loc[['002b0a40-3957-46db-b84a-30420083558f']]
    filtered_selection = network_micro_grid.get_shunt_compensators(id=['002b0a40-3957-46db-b84a-30420083558f'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_with_batteries.get_batteries().loc[['BAT2']]
    filtered_selection = network_with_batteries.get_batteries(id=['BAT2'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_busbar_sections().loc[['S1VL2_BBS2']]
    filtered_selection = network_four_subs.get_busbar_sections(id=['S1VL2_BBS2'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_buses().loc[['S3VL1_0']]
    filtered_selection = network_four_subs.get_buses(id=['S3VL1_0'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_eurostag.get_generators().loc[['GEN2', 'GEN']]
    filtered_selection = network_eurostag.get_generators(id=['GEN2', 'GEN'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_hvdc_lines().loc[['HVDC2']]
    filtered_selection = network_four_subs.get_hvdc_lines(id=['HVDC2'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_lcc_converter_stations().loc[['LCC2']]
    filtered_selection = network_four_subs.get_lcc_converter_stations(id=['LCC2'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_linear_shunt_compensator_sections().loc[['SHUNT']]
    filtered_selection = network_four_subs.get_linear_shunt_compensator_sections(id=['SHUNT'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_lines().loc[['LINE_S3S4']]
    filtered_selection = network_four_subs.get_lines(id=['LINE_S3S4'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_loads().loc[['LD4']]
    filtered_selection = network_four_subs.get_loads(id=['LD4'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_non_linear_shunt.get_non_linear_shunt_compensator_sections().loc[
        pd.MultiIndex.from_tuples([('SHUNT', 1)], names=['id', 'section'])]
    filtered_selection = network_non_linear_shunt.get_non_linear_shunt_compensator_sections(id=['SHUNT'],
                                                                                            section=[1])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_phase_tap_changer_steps().loc[
        pd.MultiIndex.from_tuples([('TWT', 6)], names=['id', 'position'])]
    filtered_selection = network_four_subs.get_phase_tap_changer_steps(id=['TWT'], position=[6])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_phase_tap_changers().loc[['TWT']]
    filtered_selection = network_four_subs.get_phase_tap_changers(id=['TWT'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_eurostag.get_ratio_tap_changer_steps().loc[
        pd.MultiIndex.from_tuples([('NHV2_NLOAD', 0), ('NHV2_NLOAD', 2)],
                                  names=['id', 'position'])]
    filtered_selection = network_eurostag.get_ratio_tap_changer_steps(id=['NHV2_NLOAD', 'NHV2_NLOAD'],
                                                                      position=[0, 2])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_eurostag.get_ratio_tap_changers().loc[['NHV2_NLOAD']]
    filtered_selection = network_eurostag.get_ratio_tap_changers(id=['NHV2_NLOAD'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_static_var_compensators().loc[['SVC']]
    filtered_selection = network_four_subs.get_static_var_compensators(id=['SVC'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_eurostag.get_substations().loc[['P2']]
    filtered_selection = network_eurostag.get_substations(id=['P2'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_switches().loc[
        ['S1VL2_GH1_BREAKER', 'S4VL1_BBS_SVC_DISCONNECTOR', 'S1VL2_COUPLER']]
    filtered_selection = network_four_subs.get_switches(
        id=['S1VL2_GH1_BREAKER', 'S4VL1_BBS_SVC_DISCONNECTOR', 'S1VL2_COUPLER'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_voltage_levels().loc[['S2VL1']]
    filtered_selection = network_four_subs.get_voltage_levels(id=['S2VL1'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection = network_four_subs.get_vsc_converter_stations().loc[['VSC2']]
    filtered_selection = network_four_subs.get_vsc_converter_stations(id=['VSC2'])
    pd.testing.assert_frame_equal(expected_selection, filtered_selection, check_dtype=True)

    expected_selection_empty = network_four_subs.get_generators().loc[pd.Index([], name='id')]
    assert expected_selection_empty.empty
    filtered_selection_empty = network_four_subs.get_generators(id=[])
    assert filtered_selection_empty.empty


def test_limits():
    network = util.create_dangling_lines_network()

    expected = pd.DataFrame.from_records(
        index='element_id',
        columns=['element_id', 'element_type', 'side', 'name', 'type', 'value', 'acceptable_duration',
                 'fictitious'],
        data=[('DL', 'DANGLING_LINE', 'NONE', 'permanent_limit', 'CURRENT', 100, -1, False),
              ('DL', 'DANGLING_LINE', 'NONE', '20\'', 'CURRENT', 120, 1200, False),
              ('DL', 'DANGLING_LINE', 'NONE', '10\'', 'CURRENT', 140, 600, False)]
    )
    pd.testing.assert_frame_equal(expected, network.get_operational_limits(all_attributes=True), check_dtype=False)

    network = pp.network.create_eurostag_tutorial_example1_with_power_limits_network()
    expected = pd.DataFrame.from_records(
        index='element_id',
        columns=['element_id', 'element_type', 'side', 'name', 'type', 'value', 'acceptable_duration',
                 'fictitious'],
        data=[('NHV1_NHV2_1', 'LINE', 'ONE', 'permanent_limit', 'ACTIVE_POWER', 500, -1, False),
              ('NHV1_NHV2_1', 'LINE', 'TWO', 'permanent_limit', 'ACTIVE_POWER', 1100, -1, False),
              ('NHV1_NHV2_1', 'LINE', 'ONE', 'permanent_limit', 'APPARENT_POWER', 500, -1, False),
              ('NHV1_NHV2_1', 'LINE', 'TWO', 'permanent_limit', 'APPARENT_POWER', 1100, -1, False)])
    limits = network.get_operational_limits(all_attributes=True).loc['NHV1_NHV2_1']
    limits = limits[limits['name'] == 'permanent_limit']
    pd.testing.assert_frame_equal(expected, limits, check_dtype=False)
    expected = pd.DataFrame.from_records(
        index='element_id',
        columns=['element_id', 'element_type', 'side', 'name', 'type', 'value', 'acceptable_duration',
                 'fictitious'],
        data=[['NHV1_NHV2_2', 'LINE', 'ONE', "20'", 'ACTIVE_POWER', 1200, 1200, False],
              ['NHV1_NHV2_2', 'LINE', 'ONE', "20'", 'APPARENT_POWER', 1200, 1200, False]])
    limits = network.get_operational_limits(all_attributes=True).loc['NHV1_NHV2_2']
    limits = limits[limits['name'] == '20\'']
    pd.testing.assert_frame_equal(expected, limits, check_dtype=False)
    network = util.create_three_windings_transformer_with_current_limits_network()
    expected = pd.DataFrame.from_records(
        index='element_id',
        columns=['element_id', 'element_type', 'side', 'name', 'type', 'value', 'acceptable_duration',
                 'fictitious'],
        data=[['3WT', 'THREE_WINDINGS_TRANSFORMER', 'ONE', "10'", 'CURRENT', 1400, 600, False],
              ['3WT', 'THREE_WINDINGS_TRANSFORMER', 'TWO', "10'", 'CURRENT', 140, 600, False],
              ['3WT', 'THREE_WINDINGS_TRANSFORMER', 'THREE', "10'", 'CURRENT', 14, 600, False]])
    limits = network.get_operational_limits(all_attributes=True).loc['3WT']
    limits = limits[limits['name'] == '10\'']
    pd.testing.assert_frame_equal(expected, limits, check_dtype=False)


def test_validation_level():
    n = pp.network.create_ieee14()
    vl = n.get_validation_level()
    assert ValidationLevel.STEADY_STATE_HYPOTHESIS == vl
    with pytest.raises(pp.PyPowsyblError) as exc:
        n.update_generators(id='B1-G', target_p=np.NaN)
    assert "Generator 'B1-G': invalid value (NaN) for active power setpoint" == str(exc.value)

    n.set_min_validation_level(ValidationLevel.EQUIPMENT)
    n.update_generators(id='B1-G', target_p=np.NaN)
    vl = n.get_validation_level()
    assert ValidationLevel.EQUIPMENT == vl

    n.update_generators(id='B1-G', target_p=100)
    level = n.validate()
    assert ValidationLevel.STEADY_STATE_HYPOTHESIS == level


def test_validate():
    n = pp.network.create_ieee14()
    vl = n.validate()
    assert ValidationLevel.STEADY_STATE_HYPOTHESIS == vl
    n.set_min_validation_level(ValidationLevel.EQUIPMENT)
    vl = n.validate()
    assert ValidationLevel.STEADY_STATE_HYPOTHESIS == vl
    n.update_generators(id='B1-G', target_p=np.NaN)
    with pytest.raises(pp.PyPowsyblError) as exc:
        n.validate()
    assert "Generator 'B1-G': invalid value (NaN) for active power setpoint" == str(exc.value)


def test_switches_node_breaker_connection_info():
    n = pp.network.create_four_substations_node_breaker_network()
    switches = n.get_switches(attributes=['bus_breaker_bus1_id', 'bus_breaker_bus2_id', 'node1', 'node2'])
    assert (switches['node1'] >= 0).all()
    assert (switches['node2'] >= 0).all()
    disc = switches.loc['S1VL1_BBS_LD1_DISCONNECTOR']
    assert disc.node1 == 0
    assert disc.node2 == 1
    breaker = switches.loc['S1VL1_LD1_BREAKER']
    assert breaker.bus_breaker_bus1_id == 'S1VL1_0'
    assert breaker.bus_breaker_bus2_id == 'S1VL1_2'
    assert breaker.node1 == 1
    assert breaker.node2 == 2


def test_switches_bus_breaker_connection_info():
    n = pp.network.create_empty()
    n.create_substations(id='S')
    n.create_voltage_levels(id='VL', substation_id='S', topology_kind='BUS_BREAKER', nominal_v=400)
    n.create_buses(id=['B1', 'B2'], voltage_level_id=['VL', 'VL'])
    n.create_switches(id='BREAKER', voltage_level_id='VL', bus1_id='B1', bus2_id='B2')
    switches = n.get_switches(attributes=['bus_breaker_bus1_id', 'bus_breaker_bus2_id', 'node1', 'node2'])
    expected = pd.DataFrame.from_records(index='id',
                                         data=[{'id': 'BREAKER',
                                                'bus_breaker_bus1_id': 'B1',
                                                'bus_breaker_bus2_id': 'B2',
                                                'node1': -1,
                                                'node2': -1}])

    pd.testing.assert_frame_equal(switches, expected, check_dtype=False)


def test_get_empty_attributes():
    network = pp.network.create_eurostag_tutorial_example1_network()
    gens = network.get_generators(attributes=[])
    assert gens.index.tolist() == ['GEN', 'GEN2']
    assert gens.columns.empty


def test_properties():
    network = pp.network.create_eurostag_tutorial_example1_network()
    properties = pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'prop1': 'test_prop1', 'prop2': 'test_prop2'},
        {'id': 'NHV1_NHV2_1', 'prop1': 'test_prop1', 'prop2': 'test_prop2'}
    ])
    network.add_elements_properties(properties)
    expected = pd.DataFrame.from_records(index='id',
                                         data=[{'id': 'NHV1_NHV2_1', 'prop1': 'test_prop1', 'prop2': 'test_prop2'},
                                               {'id': 'NHV1_NHV2_2', 'prop1': '', 'prop2': ''}])
    pd.testing.assert_frame_equal(network.get_lines(attributes=['prop1', 'prop2']), expected, check_dtype=False)
    expected = pd.DataFrame.from_records(index='id',
                                         data=[{'id': 'GEN', 'prop1': 'test_prop1', 'prop2': 'test_prop2'},
                                               {'id': 'GEN2', 'prop1': '', 'prop2': ''}])
    pd.testing.assert_frame_equal(network.get_generators(attributes=['prop1', 'prop2']), expected,
                                  check_dtype=False)
    network.add_elements_properties(id='NHV1_NHV2_2', prop3='test_prop3')
    expected = pd.DataFrame.from_records(index='id',
                                         data=[{'id': 'NHV1_NHV2_1', 'prop1': 'test_prop1', 'prop2': 'test_prop2',
                                                'prop3': ''},
                                               {'id': 'NHV1_NHV2_2', 'prop1': '', 'prop2': '',
                                                'prop3': 'test_prop3'}])
    pd.testing.assert_frame_equal(network.get_lines(attributes=['prop1', 'prop2', 'prop3']), expected,
                                  check_dtype=False)
    network.remove_elements_properties(ids='GEN', properties=['prop1', 'prop2'])
    columns = network.get_generators(all_attributes=True).columns
    assert 'prop1' not in columns and 'prop2' not in columns and 'prop3' not in columns
    network.remove_elements_properties(ids='NHV1_NHV2_2', properties='prop3')
    network.remove_elements_properties(ids='NHV1_NHV2_1', properties=['prop2', 'prop1'])
    columns = network.get_lines(all_attributes=True).columns
    assert 'prop1' not in columns and 'prop2' not in columns and 'prop3' not in columns
    network.add_elements_properties(id='GEN', test=1)
    assert '1' == network.get_generators(all_attributes=True).loc['GEN']['test']

    properties = pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'prop1': 'test_prop1', 'prop2': 'test_prop2'},
        {'id': 'NHV1_NHV2_1', 'prop1': 'test_prop1'}
    ])
    with pytest.raises(pp.PyPowsyblError) as exc:
        network.add_elements_properties(properties)
    assert 'dataframe can not contain NaN values' in str(exc)


def test_pathlib_load_dump(tmpdir):
    bat_path = TEST_DIR.joinpath('battery.xiidm')
    n_path = pp.network.load(bat_path)
    n_str = pp.network.load(str(bat_path))
    assert n_path.dump_to_string() == n_str.dump_to_string()
    data = tmpdir.mkdir('data')
    n_path.dump(data.join('test.xiidm'))
    n_path = pp.network.load(data.join('test.xiidm'))
    assert n_path.dump_to_string() == n_str.dump_to_string()


def test_write_svg_file(tmpdir):
    data = tmpdir.mkdir('data')
    net = pp.network.create_four_substations_node_breaker_network()
    assert not exists(data.join('test_nad.svg'))
    net.write_network_area_diagram_svg(data.join('test_nad.svg'))
    assert exists(data.join('test_nad.svg'))
    assert not exists(data.join('test_sld.svg'))
    net.write_single_line_diagram_svg('S1VL1', data.join('test_sld.svg'))
    assert exists(data.join('test_sld.svg'))
    assert not exists(data.join('test2_sld.svg'))
    assert not exists(data.join('test2_sld.json'))
    net.write_single_line_diagram_svg('S1VL1', data.join('test2_sld.svg'), data.join('test2_sld.json'))
    assert exists(data.join('test2_sld.svg'))
    assert exists(data.join('test2_sld.json'))


def test_get_single_line_diagram_component_library_names():
    assert ['Convergence', 'FlatDesign'] == pp.network.get_single_line_diagram_component_library_names()


def test_attributes_order():
    n = pp.network.create_four_substations_node_breaker_network()
    assert ['target_p', 'energy_source'] == list(n.get_generators(attributes=['target_p', 'energy_source']).columns)
    assert ['energy_source', 'target_p'] == list(n.get_generators(attributes=['energy_source', 'target_p']).columns)
    assert ['r', 'x', 'g1'] == list(n.get_lines(attributes=['r', 'x', 'g1']).columns)
    assert ['g1', 'r', 'x'] == list(n.get_lines(attributes=['g1', 'r', 'x']).columns)


def test_load_network_with_report():
    reporter = rp.Reporter()
    report1 = str(reporter)
    assert len(report1) > 0
    n = pp.network.load(str(DATA_DIR.joinpath('ieee14.dgs')), reporter=reporter)
    report2 = str(reporter)
    assert len(report2) >= len(report1)


def test_load_network_from_string_with_report():
    file_content = """
##C 2007.05.01
##N
##ZBE
BBE1AA1               0 2 400.00 3000.00 0.00000 -1500.0 0.00000 0.00000 -9000.0 9000.00 -9000.0                               F
    """
    reporter = rp.Reporter()
    report1 = str(reporter)
    assert len(report1) > 0
    n = pp.network.load_from_string('simple-eu.uct', file_content, reporter=reporter)
    report2 = str(reporter)
    assert len(report2) > len(report1)


def test_dump_to_string_with_report():
    bat_path = TEST_DIR.joinpath('battery.xiidm')
    reporter = rp.Reporter()
    report1 = str(reporter)
    assert len(report1) > 0
    xml = bat_path.read_text()
    n = pp.network.load(str(bat_path), reporter=reporter)
    report2 = str(reporter)
    assert len(report2) >= len(report1)


def test_ratio_tap_changer_regulated_side():
    n = pp.network.create_ieee9()
    tap_changer = n.get_ratio_tap_changers(attributes=['regulated_side', 'regulating']).loc['T4-1-0']
    assert not tap_changer.regulating
    assert tap_changer.regulated_side == ''

    with pytest.raises(pp.PyPowsyblError, match='a regulation terminal has to be set'):
        n.update_ratio_tap_changers(id='T4-1-0', target_v=100, regulating=True)

    with pytest.raises(pp.PyPowsyblError, match='must be ONE or TWO'):
        n.update_ratio_tap_changers(id='T4-1-0', regulated_side='INVALID', target_v=100, regulating=True)

    n.update_ratio_tap_changers(id='T4-1-0', regulated_side='TWO', target_v=100, target_deadband=0, regulating=True)
    tap_changer = n.get_ratio_tap_changers(attributes=['regulated_side', 'regulating']).loc['T4-1-0']
    assert tap_changer.regulating
    assert tap_changer.regulated_side == 'TWO'


def test_phase_tap_changer_regulated_side():
    n = pp.network.create_four_substations_node_breaker_network()
    tap_changer = n.get_phase_tap_changers(all_attributes=True).loc['TWT']
    assert not tap_changer.regulating
    assert tap_changer.regulated_side == 'ONE'
    assert tap_changer.regulation_mode == 'FIXED_TAP'
    n.update_ratio_tap_changers(id='TWT', regulating=False)
    with pytest.raises(pp.PyPowsyblError, match='regulated terminal is not set'):
        n.update_phase_tap_changers(id='TWT', target_deadband=0, regulation_value=300,
                                    regulation_mode='CURRENT_LIMITER',
                                    regulating=True, regulated_side='')

    n.update_phase_tap_changers(id='TWT', target_deadband=0, regulation_value=300, regulation_mode='CURRENT_LIMITER',
                                regulating=True, regulated_side='TWO')
    tap_changer = n.get_phase_tap_changers(all_attributes=True).loc['TWT']
    assert tap_changer.regulating
    assert tap_changer.regulated_side == 'TWO'
    assert tap_changer.regulation_mode == 'CURRENT_LIMITER'


def test_aliases():
    n = pp.network.create_four_substations_node_breaker_network()
    assert n.get_aliases().empty
    n.add_aliases(id='TWT', alias='test', alias_type='type')
    alias = n.get_aliases().loc['TWT']
    assert alias['alias'] == 'test'
    assert alias['alias_type'] == 'type'
    twt = n.get_2_windings_transformers(id='test')
    assert twt.shape[0] == 1
    assert twt.loc['TWT']['r'] == 2.0
    n.add_aliases(id='GH1', alias='no_type_test')
    alias2 = n.get_aliases().loc['GH1']
    assert alias2['alias'] == 'no_type_test'
    assert alias2['alias_type'] == ''
    n.remove_aliases(id=['TWT', 'GH1'], alias=['test', 'no_type_test'])
    assert n.get_aliases().empty


def test_identifiables():
    n = pp.network.create_four_substations_node_breaker_network()
    assert n.get_identifiables().loc['GH3'].type == 'GENERATOR'
    assert n.get_identifiables().loc['TWT'].type == 'TWO_WINDINGS_TRANSFORMER'


def test_injections():
    n = pp.network.create_four_substations_node_breaker_network()
    load = n.get_injections().loc['LD1']
    assert load.type == 'LOAD'
    assert load.voltage_level_id == 'S1VL1'
    assert load.bus_id == 'S1VL1_0'


def test_branches():
    n = pp.network.create_four_substations_node_breaker_network()
    twt = n.get_branches().loc['TWT']
    assert twt.voltage_level1_id == 'S1VL1'
    assert twt.bus1_id == 'S1VL1_0'
    assert twt.voltage_level2_id == 'S1VL2'
    assert twt.bus2_id == 'S1VL2_0'
    assert twt.connected1
    assert twt.connected2
    n.update_branches(id='TWT', connected1=False, connected2=False)
    twt = n.get_branches().loc['TWT']
    assert not twt.connected1
    assert not twt.connected2


def test_terminals():
    n = pp.network.create_four_substations_node_breaker_network()
    terminals = n.get_terminals()
    shunt = terminals.loc['SHUNT']
    assert shunt.voltage_level_id == 'S1VL2'
    assert shunt.bus_id == 'S1VL2_0'
    line = terminals.loc['LINE_S2S3']
    assert line.shape[0] == 2
    assert line.iloc[0].element_side == 'ONE'
    assert line.iloc[1].element_side == 'TWO'
    n.update_terminals(element_id='GH1', connected=False)
    terminals = n.get_terminals()
    gen = terminals.loc['GH1']
    assert not gen.connected
    n.update_terminals(element_id='LINE_S2S3', connected=False, element_side='ONE')
    terminals = n.get_terminals()
    line = terminals.loc['LINE_S2S3']
    assert not line.iloc[0].connected
    with pytest.raises(PyPowsyblError) as e:
        n.update_terminals(element_id='GH1', connected=False, element_side='ONE')
    assert "no side ONE for this element" in str(e)
    with pytest.raises(PyPowsyblError) as e:
        n.update_terminals(element_id='LINE_S2S3', connected=False)
    assert "side must be provided for this element" in str(e)
    with pytest.raises(PyPowsyblError) as e:
        n.update_terminals(element_id='LINE_S2S3', connected=False, element_side='THREE')
    assert "no side THREE for this element" in str(e)
    with pytest.raises(PyPowsyblError) as e:
        n.update_terminals(element_id='LINE_S2S3', connected=False, element_side='side')
    assert "No enum constant" in str(e)


if __name__ == '__main__':
    unittest.main()
