#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
"""Every fast path must return exactly what the stock pandas API returns on the same network.

In these tests the stock API (``network.get_loads()`` and friends, which return pandas dataframes) is only the
reference: it is called to build the expected values and to read back what a fast setter wrote. Code using the fast
path calls ``fast.get`` / ``fast.set`` only, which exchange numpy arrays.
"""
import numpy as np
import pandas as pd
import pytest

import pypowsybl as pp
from pypowsybl.fast import NONE, SWITCH_KINDS, FastNetwork

# element type -> stock dataframe getter
DATAFRAMES = {
    'load': lambda n: n.get_loads(all_attributes=True),
    'generator': lambda n: n.get_generators(all_attributes=True),
    'line': lambda n: n.get_lines(all_attributes=True),
    'two_windings_transformer': lambda n: n.get_2_windings_transformers(all_attributes=True),
    'switch': lambda n: n.get_switches(all_attributes=True),
    'bus': lambda n: n.get_buses(all_attributes=True),
    'voltage_level': lambda n: n.get_voltage_levels(all_attributes=True),
    'busbar_section': lambda n: n.get_busbar_sections(all_attributes=True),
}

# fast columns which are not a plain copy of the dataframe column of the same name: (dataframe column, kind)
REFERENCES = {'bus': 'bus_id', 'bus1': 'bus1_id', 'bus2': 'bus2_id'}
VOLTAGE_LEVELS = {'voltage_level': 'voltage_level_id', 'voltage_level1': 'voltage_level1_id',
                  'voltage_level2': 'voltage_level2_id'}


def ids_of(numbers, ids):
    """Ids designated by an array of positions, '' for NONE."""
    padded = np.append(ids, '')  # NONE == -1 picks the last entry
    assert numbers.min(initial=0) >= NONE and numbers.max(initial=-1) < len(ids)
    return padded[numbers]


def assert_column_equal(fast, element_type, column, values, df):
    """Compares one fast column with the stock dataframe of the element type."""
    if element_type != 'bus' and column in REFERENCES:
        expected = df[REFERENCES[column]].to_numpy(dtype=object)
        actual = ids_of(values, fast.ids('bus'))
    elif column in VOLTAGE_LEVELS:
        expected = df[VOLTAGE_LEVELS[column]].to_numpy(dtype=object)
        actual = ids_of(values, fast.ids('voltage_level'))
    elif (element_type, column) == ('switch', 'kind'):
        expected = df['kind'].to_numpy(dtype=object)
        actual = np.array(SWITCH_KINDS, dtype=object)[values]
    elif (element_type, column) == ('voltage_level', 'node_breaker'):
        expected = (df['topology_kind'] == 'NODE_BREAKER').to_numpy()
        actual = values.astype(bool)
    else:
        expected = df[column].to_numpy()
        actual = values.astype(bool) if expected.dtype == bool else values
        assert actual.dtype == expected.dtype, f'{element_type}.{column}: {actual.dtype} != {expected.dtype}'
    # exact equality, NaN equal to NaN
    np.testing.assert_array_equal(actual, expected, err_msg=f'{element_type}.{column}')


def assert_all_equal(fast, network):
    """All ids and all columns of all types against the stock API."""
    for element_type in fast.element_types:
        df = DATAFRAMES[element_type](network)
        np.testing.assert_array_equal(fast.ids(element_type), df.index.to_numpy(dtype=object), err_msg=element_type)
        assert fast.count(element_type) == len(df)
        for column, (dtype, _) in fast.columns(element_type).items():
            values = fast.get(element_type, column)
            assert values.dtype == dtype and values.shape == (len(df),)
            assert_column_equal(fast, element_type, column, values, df)


def toggle_some_switches(fast, rng, fraction=0.02):
    """Random positions and new states (each chosen switch is toggled)."""
    n = fast.count('switch')
    if n == 0:  # bus/breaker test networks without switches: an empty change
        return np.empty(0, dtype=np.int32), np.empty(0, dtype=np.int32)
    positions = rng.choice(n, size=max(1, int(n * fraction)), replace=False).astype(np.int32)
    return positions, 1 - fast.get('switch', 'open')[positions]


def test_element_types(network):
    fast = FastNetwork(network)
    assert set(fast.element_types) == set(DATAFRAMES)


def test_ids_and_columns_equal_dataframes(network):
    assert_all_equal(FastNetwork(network), network)


def test_columns_after_loadflow(network):
    """Computed values (p, q, i, v) are read like the others."""
    pp.loadflow.run_ac(network)
    assert_all_equal(FastNetwork(network), network)


def test_get_writes_into_given_buffer(network):
    fast = FastNetwork(network)
    out = np.full(fast.count('load'), 123.0)
    assert fast.get('load', 'p0', out=out) is out
    np.testing.assert_array_equal(out, network.get_loads()['p0'].to_numpy())
    out_int = np.full(fast.count('load'), 77, dtype=np.int32)
    assert fast.get('load', 'connected', out=out_int) is out_int
    np.testing.assert_array_equal(out_int.astype(bool), network.get_loads()['connected'].to_numpy())


def test_get_rejects_wrong_buffers(network):
    fast = FastNetwork(network)
    n = fast.count('load')
    with pytest.raises(TypeError):
        fast.get('load', 'p0', out=np.zeros(n, dtype=np.float32))  # no silent conversion copy
    with pytest.raises(TypeError):
        fast.get('load', 'connected', out=np.zeros(n, dtype=np.int64))
    with pytest.raises(pp.PyPowsyblError, match='length'):
        fast.get('load', 'p0', out=np.zeros(n + 1))
    with pytest.raises(ValueError, match='Unknown column'):
        fast.get('load', 'nope')
    with pytest.raises(ValueError, match='Unknown element type'):
        fast.get('nope', 'p0')


def test_set_loads_by_position(network):
    fast = FastNetwork(network)
    rng = np.random.default_rng(0)
    n = fast.count('load')
    positions = rng.choice(n, size=max(1, n // 3), replace=False).astype(np.int32)
    p0 = rng.uniform(-50, 500, len(positions))
    q0 = rng.uniform(-50, 50, len(positions))
    # reference: the stock dataframe before the change, with the new values put at the same rows
    expected = network.get_loads(attributes=['p0', 'q0'])
    expected.iloc[positions, 0] = p0
    expected.iloc[positions, 1] = q0

    # the operation under test
    fast.set('load', 'p0', p0, positions=positions)
    fast.set('load', 'q0', q0, positions=positions)

    # the stock API must now read exactly the expected table
    pd.testing.assert_frame_equal(network.get_loads(attributes=['p0', 'q0']), expected, check_exact=True)
    assert_all_equal(fast, network)


def test_set_whole_columns(network):
    fast = FastNetwork(network)
    rng = np.random.default_rng(1)
    p0 = rng.uniform(0, 100, fast.count('load'))
    fast.set('load', 'p0', p0)
    np.testing.assert_array_equal(network.get_loads()['p0'].to_numpy(), p0)

    target_p = fast.get('generator', 'min_p') + 0.5 * (fast.get('generator', 'max_p') - fast.get('generator', 'min_p'))
    fast.set('generator', 'target_p', target_p)
    np.testing.assert_array_equal(network.get_generators()['target_p'].to_numpy(), target_p)

    v_mag = rng.uniform(0.9, 1.1, fast.count('bus')) * 100
    fast.set('bus', 'v_mag', v_mag)
    np.testing.assert_array_equal(network.get_buses()['v_mag'].to_numpy(), v_mag)
    assert_all_equal(fast, network)


def test_set_same_as_stock_update(network):
    """The fast setter and the stock update leave two copies of the network in the same state."""
    other = pp.network.load_from_binary_buffer(network.save_to_binary_buffer('BIIDM'))
    fast = FastNetwork(network)
    rng = np.random.default_rng(2)
    positions, states = toggle_some_switches(fast, rng)
    fast.set('switch', 'open', states, positions=positions)
    other.update_switches(id=fast.ids('switch')[positions], open=states.astype(bool))
    # a saved and reloaded network does not keep the row order: compare by id
    for getter in ('get_switches', 'get_buses', 'get_loads', 'get_lines'):
        pd.testing.assert_frame_equal(getattr(network, getter)().sort_index(), getattr(other, getter)().sort_index())


def test_set_switches_then_read_topology(network):
    fast = FastNetwork(network)
    rng = np.random.default_rng(3)
    for _ in range(3):
        positions, states = toggle_some_switches(fast, rng)
        expected = network.get_switches()['open'].to_numpy().copy()
        expected[positions] = states.astype(bool)
        fast.set('switch', 'open', states, positions=positions)
        np.testing.assert_array_equal(network.get_switches()['open'].to_numpy(), expected)
        assert_all_equal(fast, network)


def test_set_at(network):
    fast = FastNetwork(network)
    if fast.count('switch') > 0:
        position = fast.count('switch') // 2
        was_open = bool(fast.get('switch', 'open')[position])
        fast.set_at('switch', 'open', position, not was_open)
        assert network.get_switches()['open'].iloc[position] == (not was_open)
    fast.set_at('load', 'p0', 0, 12.5)
    assert network.get_loads()['p0'].iloc[0] == 12.5
    assert_all_equal(fast, network)


def test_set_rejects_bad_input_without_side_effect(network):
    fast = FastNetwork(network)
    before = fast.get('load', 'p0')
    n = fast.count('load')
    with pytest.raises(pp.PyPowsyblError, match='out of range'):
        fast.set('load', 'p0', [1.0, 2.0], positions=[0, n])  # the valid position 0 must not be written either
    with pytest.raises(pp.PyPowsyblError, match='out of range'):
        fast.set_at('load', 'p0', -1, 1.0)
    with pytest.raises(pp.PyPowsyblError, match='same length'):
        fast.set('load', 'p0', [1.0, 2.0], positions=[0])
    with pytest.raises(pp.PyPowsyblError, match='length'):
        fast.set('load', 'p0', np.zeros(n + 1))
    with pytest.raises(ValueError, match='read only'):
        fast.set('line', 'r', np.zeros(fast.count('line')))
    np.testing.assert_array_equal(fast.get('load', 'p0'), before)


def test_apply_switches_and_get_topology(network):
    fast = FastNetwork(network)
    rng = np.random.default_rng(4)
    for _ in range(3):
        positions, states = toggle_some_switches(fast, rng)
        expected_open = network.get_switches()['open'].to_numpy().copy()
        expected_open[positions] = states.astype(bool)

        topo = fast.apply_switches_and_get_topology(positions, states)

        np.testing.assert_array_equal(network.get_switches()['open'].to_numpy(), expected_open)
        buses = network.get_buses()
        bus_ids = buses.index.to_numpy(dtype=object)
        np.testing.assert_array_equal(topo.bus_connected_component, buses['connected_component'].to_numpy())
        loads, generators = network.get_loads(), network.get_generators()
        lines, transformers = network.get_lines(), network.get_2_windings_transformers()
        np.testing.assert_array_equal(ids_of(topo.load_bus, bus_ids), loads['bus_id'].to_numpy(dtype=object))
        np.testing.assert_array_equal(ids_of(topo.generator_bus, bus_ids), generators['bus_id'].to_numpy(dtype=object))
        np.testing.assert_array_equal(ids_of(topo.line_bus1, bus_ids), lines['bus1_id'].to_numpy(dtype=object))
        np.testing.assert_array_equal(ids_of(topo.line_bus2, bus_ids), lines['bus2_id'].to_numpy(dtype=object))
        np.testing.assert_array_equal(ids_of(topo.two_windings_transformer_bus1, bus_ids),
                                      transformers['bus1_id'].to_numpy(dtype=object))
        np.testing.assert_array_equal(ids_of(topo.two_windings_transformer_bus2, bus_ids),
                                      transformers['bus2_id'].to_numpy(dtype=object))
        # and the separate getters agree with the compound call
        np.testing.assert_array_equal(fast.get('load', 'bus'), topo.load_bus)
        np.testing.assert_array_equal(fast.get('line', 'bus2'), topo.line_bus2)


def test_apply_switches_when_bus_count_grows(network):
    """The bus buffer is sized from the previous bus count: opening everything must still give all the buses."""
    fast = FastNetwork(network)
    n = fast.count('switch')
    fast.apply_switches_and_get_topology([], [])
    topo = fast.apply_switches_and_get_topology(np.arange(n), np.zeros(n))  # close all
    np.testing.assert_array_equal(topo.bus_connected_component, network.get_buses()['connected_component'].to_numpy())
    topo = fast.apply_switches_and_get_topology(np.arange(n), np.ones(n))  # open all
    np.testing.assert_array_equal(topo.bus_connected_component, network.get_buses()['connected_component'].to_numpy())
    assert len(topo.bus_connected_component) == len(network.get_buses())


def test_positions(network):
    fast = FastNetwork(network)
    for element_type in fast.element_types:
        ids = fast.ids(element_type)
        picked = ids[::3][::-1]
        np.testing.assert_array_equal(ids[fast.positions(element_type, picked)], picked)
    with pytest.raises(ValueError, match='No load with id'):
        fast.positions('load', ['no such load'])


def test_node_breaker_topology(network):
    """Switch nodes, terminal nodes and internal connections against get_node_breaker_topology of each voltage level."""
    fast = FastNetwork(network)
    vl_ids = fast.ids('voltage_level')
    node_breaker = fast.get('voltage_level', 'node_breaker').astype(bool)
    ic = fast.internal_connections()
    assert ic.dtype == np.int32 and ic.shape[1] == 3
    switch_ids = fast.ids('switch')
    switch_vl = fast.get('switch', 'voltage_level')
    switch_nodes = np.column_stack([fast.get('switch', 'node1'), fast.get('switch', 'node2')])
    switch_open = fast.get('switch', 'open').astype(bool)
    checked = 0
    # all the voltage levels of small networks, a spread sample of the large ones (one stock call per voltage level)
    step = max(1, len(vl_ids) // 300)
    for v in range(0, len(vl_ids), step):
        if not node_breaker[v]:
            assert not (ic[:, 0] == v).any() and (switch_nodes[switch_vl == v] == NONE).all()
            continue
        topology = network.get_node_breaker_topology(vl_ids[v])
        expected_ic = topology.internal_connections[['node1', 'node2']].to_numpy()
        np.testing.assert_array_equal(ic[ic[:, 0] == v][:, 1:], expected_ic)
        mine = switch_vl == v
        expected_sw = topology.switches
        np.testing.assert_array_equal(switch_ids[mine], expected_sw.index.to_numpy(dtype=object))
        np.testing.assert_array_equal(switch_nodes[mine], expected_sw[['node1', 'node2']].to_numpy())
        np.testing.assert_array_equal(switch_open[mine], expected_sw['open'].to_numpy())
        checked += 1
    if node_breaker.any():
        assert checked > 0
    else:
        assert len(ic) == 0


def test_stale_view_is_detected(network):
    fast = FastNetwork(network)
    vl = network.get_voltage_levels().index[0]
    if network.get_voltage_levels(all_attributes=True)['topology_kind'].iloc[0] == 'NODE_BREAKER':
        free_node = int(network.get_node_breaker_topology(vl).nodes.index.max()) + 1
        network.create_loads(id='FAST_NEW_LOAD', voltage_level_id=vl, node=free_node, p0=1.0, q0=0.0)
    else:
        bus = network.get_bus_breaker_view_buses().query('voltage_level_id == @vl').index[0]
        network.create_loads(id='FAST_NEW_LOAD', voltage_level_id=vl, bus_id=bus, p0=1.0, q0=0.0)
    with pytest.raises(pp.PyPowsyblError, match='refresh'):
        fast.get('load', 'p0')
    fast.refresh()
    assert_all_equal(fast, network)
    assert 'FAST_NEW_LOAD' in fast.ids('load')


def test_variants(network):
    """Columns follow the working variant, like the dataframes."""
    fast = FastNetwork(network)
    network.clone_variant(network.get_working_variant_id(), 'other')
    initial = fast.get('load', 'p0')
    network.set_working_variant('other')
    fast.set('load', 'p0', initial + 1.0)
    assert_all_equal(fast, network)
    network.set_working_variant('InitialState')
    np.testing.assert_array_equal(fast.get('load', 'p0'), initial)
    assert_all_equal(fast, network)
