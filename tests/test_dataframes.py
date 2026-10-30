#
# Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import numpy as np
import pandas as pd
from pypowsybl.utils import create_data_frame_from_series_array


class _FakeSeries:
    def __init__(self, name, data, index=False, dictionary=None):
        self.name = name
        self.data = data
        self.index = index
        self.dictionary = dictionary
        self.mask = np.array([])


def test_dictionary_encoded_index():
    contingencies = ['', 'C2', 'C1', 'C1']
    strategies = ['', '', '', 'S1']
    branches = ['L2', 'L1', 'L2', 'L1']
    p1 = np.array([1.0, np.nan, 3.0, 4.0])
    # dictionaries in order of first appearance, as written by the java side (not sorted)
    series = [_FakeSeries('contingency_id', np.array([0, 1, 2, 2], dtype=np.int32), index=True,
                          dictionary=['', 'C2', 'C1']),
              _FakeSeries('operator_strategy_id', np.array([0, 0, 0, 1], dtype=np.int32), index=True,
                          dictionary=['', 'S1']),
              _FakeSeries('branch_id', np.array([0, 1, 0, 1], dtype=np.int32), index=True, dictionary=['L2', 'L1']),
              _FakeSeries('p1', p1)]
    df = create_data_frame_from_series_array(series)
    expected = pd.DataFrame({'p1': p1}, index=pd.MultiIndex.from_arrays(
        [contingencies, strategies, branches], names=['contingency_id', 'operator_strategy_id', 'branch_id']))
    pd.testing.assert_frame_equal(expected, df)
    for level, expected_level in zip(df.index.levels, expected.index.levels):
        pd.testing.assert_index_equal(expected_level, level)
    for codes, expected_codes in zip(df.index.codes, expected.index.codes):
        np.testing.assert_array_equal(expected_codes, codes)
        assert codes.dtype == expected_codes.dtype

    # missing values (code -1) in unsorted dictionaries
    with_missing = create_data_frame_from_series_array(
        [_FakeSeries('a', np.array([0, 1, -1], dtype=np.int32), index=True, dictionary=['b', 'a']),
         _FakeSeries('b', np.array([-1, 1, 0], dtype=np.int32), index=True, dictionary=['y', 'x']),
         _FakeSeries('p1', p1[:3])])
    expected_missing = pd.DataFrame({'p1': p1[:3]}, index=pd.MultiIndex.from_arrays(
        [['b', 'a', None], [None, 'x', 'y']], names=['a', 'b']))
    pd.testing.assert_frame_equal(expected_missing, with_missing)
    for codes, expected_codes in zip(with_missing.index.codes, expected_missing.index.codes):
        np.testing.assert_array_equal(expected_codes, codes)

    # single dictionary-encoded index level, and a mix of plain and dictionary-encoded levels
    single = create_data_frame_from_series_array(
        [_FakeSeries('id', np.array([1, 0, 1], dtype=np.int32), index=True, dictionary=['a', 'b']),
         _FakeSeries('p1', p1[:3])])
    pd.testing.assert_frame_equal(pd.DataFrame({'p1': p1[:3]}, index=pd.Index(['b', 'a', 'b'], name='id')), single)
    mixed = create_data_frame_from_series_array(
        [_FakeSeries('id', ['x', 'y', 'z'], index=True),
         _FakeSeries('kind', np.array([1, 0, 1], dtype=np.int32), index=True, dictionary=['a', 'b']),
         _FakeSeries('p1', p1[:3])])
    expected_mixed = pd.DataFrame({'p1': p1[:3]}, index=pd.MultiIndex.from_arrays(
        [['x', 'y', 'z'], ['b', 'a', 'b']], names=['id', 'kind']))
    pd.testing.assert_frame_equal(expected_mixed, mixed)

    # a large sorted dictionary: pandas keeps the int32 codes array as is, so the index must be copied out of it
    large = [f'id{i:05d}' for i in range(40000)]
    large_codes = np.arange(40000, dtype=np.int32)
    large_df = create_data_frame_from_series_array(
        [_FakeSeries('contingency_id', np.zeros(40000, dtype=np.int32), index=True, dictionary=['C1']),
         _FakeSeries('branch_id', large_codes, index=True, dictionary=large),
         _FakeSeries('p1', np.ones(40000))])
    expected_large = pd.DataFrame({'p1': np.ones(40000)}, index=pd.MultiIndex.from_arrays(
        [['C1'] * 40000, large], names=['contingency_id', 'branch_id']))
    pd.testing.assert_frame_equal(expected_large, large_df)
    for codes, expected_codes in zip(large_df.index.codes, expected_large.index.codes):
        np.testing.assert_array_equal(expected_codes, codes)
        assert codes.dtype == expected_codes.dtype
        assert not np.may_share_memory(codes, large_codes)

    empty = create_data_frame_from_series_array(
        [_FakeSeries('contingency_id', np.array([], dtype=np.int32), index=True, dictionary=[]),
         _FakeSeries('transformer_id', np.array([], dtype=np.int32), index=True, dictionary=[]),
         _FakeSeries('p1', np.array([], dtype=np.float64))])
    expected_empty = pd.DataFrame({'p1': np.array([], dtype=np.float64)},
                                  index=pd.MultiIndex.from_arrays([[], []], names=['contingency_id', 'transformer_id']))
    pd.testing.assert_frame_equal(expected_empty, empty)


def test_dictionary_encoded_columns():
    codes = np.array([1, 0, -1, 1], dtype=np.int32)
    df = create_data_frame_from_series_array(
        [_FakeSeries('id', ['a', 'b', 'c', 'd'], index=True),
         _FakeSeries('name', codes, dictionary=['N0', 'N1']),
         _FakeSeries('p1', np.array([1.0, 2.0, 3.0, 4.0]))])
    assert df['name'].dtype == object
    assert df['name'].tolist() == ['N1', 'N0', None, 'N1']
    assert not np.may_share_memory(df['name'].to_numpy(), codes)
