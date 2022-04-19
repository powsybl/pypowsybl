import pathlib
import pytest
import pypowsybl.network as pn

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'


def test_extensions():
    assert 'activePowerControl' in pn.get_extensions_names()
    no_extensions_network = pn.create_eurostag_tutorial_example1_network()
    assert no_extensions_network.get_extension('activePowerControl').empty
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    generators_extensions = n.get_extension('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.1, abs=1e-3)
    assert n.get_extension('hvdcOperatorActivePowerRange').empty


def test_merged_xnode():
    network = pn.load(str(DATA_DIR / 'uxTestGridForMerging.uct'))
    merged_x_nodes = network.get_extension('mergedXnode')
    x = merged_x_nodes.loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX11'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.5, 0.5, 0.5, 0.5, 0.5, 0.5)
    assert (x.p1, x.q1, x.p2, x.q2) == (0, 0, 0, 0)


def test_xnode():
    network = pn.load(str(DATA_DIR / 'simple-eu-xnode.uct'))
    x = network.get_extension('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert x.code == 'XXXXXX11'


def test_entsoe_area():
    network = pn.load(str(DATA_DIR / 'germanTsos.uct'))
    area = network.get_extension('entsoeArea').loc['D4NEUR']
    assert area.code == 'D4'


def test_entsoe_category():
    network = pn._create_network('eurostag_tutorial_example1_with_entsoe_category')
    gen = network.get_extension('entsoeCategory').loc['GEN']
    assert gen.code == 5