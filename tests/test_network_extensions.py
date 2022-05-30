#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pandas as pd
import pathlib
import pytest
import pypowsybl.network as pn

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'


def test_extensions():
    assert 'activePowerControl' in pn.get_extensions_names()
    no_extensions_network = pn.create_eurostag_tutorial_example1_network()
    assert no_extensions_network.get_extensions('activePowerControl').empty
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.1, abs=1e-3)
    assert n.get_extensions('hvdcOperatorActivePowerRange').empty

def test_update_extensions():
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    n.update_extensions('activePowerControl', pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'droop': 1.2}
    ]))
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.2, abs=1e-3)
    n.update_extensions('activePowerControl', id='GEN', droop=1.4)
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.4, abs=1e-3)

def test_remove_extensions():
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    n.remove_extensions('activePowerControl', ['GEN', 'GEN2'])
    assert n.get_extensions('activePowerControl').empty

def test_create_extensions():
    n = pn._create_network('eurostag_tutorial_example1')
    n.create_extensions('activePowerControl', pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'droop': 1.2, 'participate': True}
    ]))
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.2, abs=1e-3)

    n.create_extensions('activePowerControl', id='GEN2', droop=1.3, participate=False)
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 2
    assert not generators_extensions['participate']['GEN2']
    assert generators_extensions['droop']['GEN2'] == pytest.approx(1.3, abs=1e-3)


def test_merged_xnode():
    network = pn.load(str(DATA_DIR / 'uxTestGridForMerging.uct'))
    merged_x_nodes = network.get_extensions('mergedXnode')
    x = merged_x_nodes.loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX11'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.5, 0.5, 0.5, 0.5, 0.5, 0.5)

    network.update_extensions('mergedXnode', id='BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1', code='XXXXXX15',
                              line1='BBBBBB11 XXXXXX11 1', line2='FFFFFF11 XXXXXX11 1',
                              r_dp=0.6, x_dp=0.6, g1_dp=0.6, g2_dp=0.6, b1_dp=0.6, b2_dp=0.6,
                              p1=0, q1=0, p2=0, q2=0)
    x = network.get_extensions('mergedXnode').loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX15'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.6, 0.6, 0.6, 0.6, 0.6, 0.6)
    assert (x.p1, x.q1, x.p2, x.q2) == (0, 0, 0, 0)

    network.remove_extensions('mergedXnode', ['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1', 'BBBBBB11 XXXXXX12 1 + FFFFFF11 XXXXXX12 1'])
    assert network.get_extensions('mergedXnode').empty

    network.create_extensions('mergedXnode', id='BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1', code='XXXXXX11',
                              line1='BBBBBB11 XXXXXX11 1', line2='FFFFFF11 XXXXXX11 1',
                              r_dp=0.4, x_dp=0.4, g1_dp=0.4, g2_dp=0.4, b1_dp=0.4, b2_dp=0.4,
                              p1=0, q1=0, p2=0, q2=0)
    x = network.get_extensions('mergedXnode').loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX11'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.4, 0.4, 0.4, 0.4, 0.4, 0.4)
    assert (x.p1, x.q1, x.p2, x.q2) == (0, 0, 0, 0)


def test_xnode():
    network = pn.load(str(DATA_DIR / 'simple-eu-xnode.uct'))
    x = network.get_extensions('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert x.code == 'XXXXXX11'

    network.update_extensions('xnode', id='NNL2AA1  XXXXXX11 1', code='XXXXXX12')
    e = network.get_extensions('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert e.code == 'XXXXXX12'

    network.remove_extensions('xnode', ['NNL2AA1  XXXXXX11 1'])
    assert network.get_extensions('xnode').empty

    network.create_extensions('xnode', id='NNL2AA1  XXXXXX11 1', code='XXXXXX13')
    e = network.get_extensions('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert e.code == 'XXXXXX13'


def test_entsoe_area():
    network = pn.load(str(DATA_DIR / 'germanTsos.uct'))
    area = network.get_extensions('entsoeArea').loc['D4NEUR']
    assert area.code == 'D4'

    network.update_extensions('entsoeArea', id='D4NEUR', code='FR')
    e = network.get_extensions('entsoeArea').loc['D4NEUR']
    assert e.code == 'FR'

    network.remove_extensions('entsoeArea', ['D4NEUR'])
    assert network.get_extensions('entsoeArea').empty

    network.create_extensions('entsoeArea', id='D4NEUR', code='D4')
    e = network.get_extensions('entsoeArea').loc['D4NEUR']
    assert e.code == 'D4'



def test_entsoe_category():
    network = pn._create_network('eurostag_tutorial_example1_with_entsoe_category')
    gen = network.get_extensions('entsoeCategory').loc['GEN']
    assert gen.code == 5

    network.update_extensions('entsoeCategory', id='GEN', code=6)
    e = network.get_extensions('entsoeCategory').loc['GEN']
    assert e.code == 6

    network.remove_extensions('entsoeCategory', ['GEN'])
    assert network.get_extensions('entsoeCategory').empty

    network.create_extensions('entsoeCategory', id='GEN', code=5)
    e = network.get_extensions('entsoeCategory').loc['GEN']
    assert e.code == 5


def test_hvdc_angle_droop_active_power_control():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'hvdcAngleDroopActivePowerControl'
    element_id = 'HVDC1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, droop=0.1, p0=200, enabled=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.droop == pytest.approx(0.1, abs=1e-3)
    assert e.p0 == pytest.approx(200, abs=1e-3)
    assert e.enabled == True

    n.update_extensions(extension_name, id=element_id, droop=0.15, p0=210, enabled=False)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.droop == pytest.approx(0.15, abs=1e-3)
    assert e.p0 == pytest.approx(210, abs=1e-3)
    assert e.enabled == False

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty

def test_hvdc_operator_active_power_range():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'hvdcOperatorActivePowerRange'
    element_id = 'HVDC1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, opr_from_cs1_to_cs2=0.1, opr_from_cs2_to_cs1=0.2)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.opr_from_cs1_to_cs2 == pytest.approx(0.1, abs=1e-3)
    assert e.opr_from_cs2_to_cs1 == pytest.approx(0.2, abs=1e-3)

    n.update_extensions(extension_name, id=element_id, opr_from_cs1_to_cs2=0.15, opr_from_cs2_to_cs1=0.25)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.opr_from_cs1_to_cs2 == pytest.approx(0.15, abs=1e-3)
    assert e.opr_from_cs2_to_cs1 == pytest.approx(0.25, abs=1e-3)

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty
