#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
"""Loading a .biidm file with parts skipped: what is not skipped must be identical to a normal load."""
import pandas as pd
import pytest
from conftest import ASSETS

import pypowsybl as pp
import pypowsybl.fast

# dataframes compared between a normal and a partial load (all attributes, without the property columns)
GETTERS = ['get_substations', 'get_voltage_levels', 'get_busbar_sections', 'get_switches', 'get_loads',
           'get_generators', 'get_lines', 'get_2_windings_transformers', 'get_3_windings_transformers',
           'get_shunt_compensators', 'get_boundary_lines', 'get_static_var_compensators', 'get_hvdc_lines',
           'get_ratio_tap_changers', 'get_phase_tap_changers', 'get_ratio_tap_changer_steps']
# columns which are stored bus state, or derived from it
BUS_STATE_COLUMNS = {'v_mag', 'v_angle', 'v', 'angle', 'i', 'i1', 'i2', 'i3'}


def _biidm_files(tmp_path_factory):
    directory = tmp_path_factory.mktemp('biidm')
    files = {}
    for name, factory in [('four_substations', pp.network.create_four_substations_node_breaker_network),
                          ('eurostag_with_limits',
                           pp.network.create_eurostag_tutorial_example1_with_more_generators_network),
                          ('ieee14', pp.network.create_ieee14)]:
        network = factory()
        if name == 'four_substations':
            pp.loadflow.run_ac(network)  # so that there is a bus state to skip
            network.add_elements_properties(pd.DataFrame(index=pd.Index(['LD1'], name='id'), data={'prop': ['x']}))
        path = directory / f'{name}.biidm'
        network.save(path, format='BIIDM')
        files[name] = str(path)
    if ASSETS.is_dir():
        for f in sorted(ASSETS.glob('*.biidm'))[:1]:
            files[f.name] = str(f)
    return files


@pytest.fixture(scope='module')
def biidm_files(tmp_path_factory):
    return _biidm_files(tmp_path_factory)


@pytest.fixture(scope='module', params=['four_substations', 'eurostag_with_limits', 'ieee14', 'asset'])
def biidm(request, biidm_files):
    if request.param == 'asset':
        assets = [k for k in biidm_files if k.endswith('.biidm')]
        if not assets:
            pytest.skip('no .biidm file in tests/assets')
        return biidm_files[assets[0]]
    return biidm_files[request.param]


@pytest.fixture(scope='module')
def full(biidm):
    return pp.network.load(biidm)


def base_columns(network, getter):
    """Columns of a dataframe which are not element properties (properties come last, after the attributes)."""
    return list(getattr(pp.network.create_empty(), getter)(all_attributes=True).columns)


def assert_same_elements(partial, full, drop=frozenset(), with_properties=True):
    for getter in GETTERS:
        expected = getattr(full, getter)(all_attributes=True).sort_index()
        actual = getattr(partial, getter)(all_attributes=True).sort_index()
        if not with_properties:
            columns = base_columns(full, getter)
            assert list(actual.columns) == columns, f'{getter}: properties were not skipped'
            expected = expected[columns]
        columns = [c for c in expected.columns if c not in drop]
        pd.testing.assert_frame_equal(actual[columns], expected[columns], obj=getter)


def limits(network):
    return network.get_operational_limits(all_attributes=True).reset_index() \
        .sort_values(['element_id', 'side', 'group_name', 'type', 'acceptable_duration', 'name']).reset_index(drop=True)


def test_skip_nothing_is_a_normal_load(biidm, full):
    partial = pp.fast.load(biidm)
    assert_same_elements(partial, full)
    pd.testing.assert_frame_equal(limits(partial), limits(full))
    pd.testing.assert_frame_equal(partial.get_buses().sort_index(), full.get_buses().sort_index())


def test_skip_temporary_limits_keeps_permanent_limits(biidm, full):
    partial = pp.fast.load(biidm, skip=['temporary-limits'])
    assert_same_elements(partial, full)
    pd.testing.assert_frame_equal(partial.get_buses().sort_index(), full.get_buses().sort_index())
    expected = limits(full)
    expected = expected[expected['acceptable_duration'] == -1].reset_index(drop=True)
    pd.testing.assert_frame_equal(limits(partial), expected)


def test_skip_limits(biidm, full):
    partial = pp.fast.load(biidm, skip=['limits'])
    assert len(partial.get_operational_limits()) == 0
    assert_same_elements(partial, full)
    pd.testing.assert_frame_equal(partial.get_buses().sort_index(), full.get_buses().sort_index())


def test_skip_bus_state(biidm, full):
    partial = pp.fast.load(biidm, skip=['bus-state'])
    assert_same_elements(partial, full, drop=BUS_STATE_COLUMNS)
    pd.testing.assert_frame_equal(limits(partial), limits(full))
    expected = full.get_buses().sort_index()
    actual = partial.get_buses().sort_index()
    # same buses, same components; the state is gone only where it was stored per calculated bus (node/breaker)
    columns = [c for c in expected.columns if c not in BUS_STATE_COLUMNS]
    pd.testing.assert_frame_equal(actual[columns], expected[columns])
    node_breaker = full.get_voltage_levels(all_attributes=True)['topology_kind'] == 'NODE_BREAKER'
    in_node_breaker = expected['voltage_level_id'].map(node_breaker)
    assert actual.loc[in_node_breaker, 'v_mag'].isna().all()
    pd.testing.assert_frame_equal(actual[~in_node_breaker], expected[~in_node_breaker])


def test_skip_properties(biidm, full):
    partial = pp.fast.load(biidm, skip=['properties'])
    assert_same_elements(partial, full, with_properties=False)
    pd.testing.assert_frame_equal(limits(partial), limits(full))


def test_skip_extensions(biidm, full):
    partial = pp.fast.load(biidm, skip=['extensions'])
    assert_same_elements(partial, full)
    present = 0
    for name in pp.network.get_extensions_names():
        try:
            present += len(full.get_extensions(name))
            assert len(partial.get_extensions(name)) == 0, name
        except pp.PyPowsyblError:  # extension without a dataframe
            continue
    if 'four_substations' not in biidm and 'ieee14' not in biidm and 'eurostag' not in biidm:
        assert present > 0, 'the snapshot was expected to hold extensions'


def test_skip_everything(biidm, full):
    partial = pp.fast.load(biidm, skip=pp.fast.SKIPPABLE)
    assert_same_elements(partial, full, drop=BUS_STATE_COLUMNS, with_properties=False)
    assert len(partial.get_operational_limits()) == 0
    # the parameter given directly to the stock loader is the same thing
    direct = pp.network.load(biidm, parameters={pp.fast.SKIP_PARAMETER: ','.join(pp.fast.SKIPPABLE)})
    assert_same_elements(direct, partial)


def test_skip_keeps_other_import_parameters(biidm_files):
    partial = pp.fast.load(biidm_files['four_substations'], skip=['limits'],
                           parameters={'iidm.import.xml.included.extensions': ''})
    assert len(partial.get_loads()) == 6


def test_invalid_skip(biidm_files, tmp_path):
    with pytest.raises(ValueError, match='Cannot skip'):
        pp.fast.load(biidm_files['ieee14'], skip=['nope'])
    with pytest.raises(pp.PyPowsyblError, match='Unknown value'):
        pp.network.load(biidm_files['ieee14'], parameters={pp.fast.SKIP_PARAMETER: 'nope'})
    xiidm = tmp_path / 'n.xiidm'
    pp.network.create_ieee14().save(xiidm)
    with pytest.raises(pp.PyPowsyblError, match='only supported'):
        pp.fast.load(xiidm, skip=['limits'])
