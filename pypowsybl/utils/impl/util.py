#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from os import PathLike
from typing import Any, List, Optional, Tuple, Union
import pandas as pd
import numpy as np
from pypowsybl import _pypowsybl

PathOrStr = Union[str, PathLike]


def _sorted_dictionary(dictionary: List[str], codes: np.ndarray) -> Tuple[pd.Index, np.ndarray]:
    """
    Sorts the (small) dictionary of one index level and remaps the codes accordingly
    (``MultiIndex.from_arrays`` sorts the levels; sorting here gives the same index).
    The returned codes may be the given (native) array when the dictionary is already sorted.
    """
    level = pd.Index(dictionary)
    if level.is_monotonic_increasing:
        return level, codes
    sorted_level, order = level.sort_values(return_indexer=True)
    # order[new code] = old code: its inverse permutation maps old codes to new codes (and -1, missing, to itself)
    return sorted_level, np.append(np.argsort(order), -1).astype(codes.dtype, copy=False)[codes]


def _decode_dictionary(dictionary: List[str], codes: np.ndarray) -> np.ndarray:
    """
    Strings of a dictionary-encoded string series (None for the code -1, a missing value).
    """
    strings = np.asarray(dictionary + [None], dtype=object)  # index -1: missing value
    return strings[codes]


def _create_index(index_series: List[Tuple[str, Optional[List[str]], Any]]) -> pd.Index:
    """
    Index built from the index series, given as (name, dictionary, data) with dictionary None for a plain series
    and data holding the codes for a dictionary-encoded one.
    """
    if len(index_series) == 1:
        name, dictionary, data = index_series[0]
        return pd.Index(_decode_dictionary(dictionary, data) if dictionary is not None else data, name=name)
    names = [name for name, _, _ in index_series]
    dictionaries = [(dictionary, data) for _, dictionary, data in index_series if dictionary is not None]
    if len(dictionaries) == len(index_series):
        # built directly from dictionaries and codes: nothing is factorized, no string is materialized per row
        sorted_levels = [_sorted_dictionary(dictionary, data) for dictionary, data in dictionaries]
        index = pd.MultiIndex(levels=[level for level, _ in sorted_levels], codes=[codes for _, codes in sorted_levels],
                              names=names, verify_integrity=False)
        # pd.MultiIndex copies the codes when it narrows their dtype (fewer than 32767 distinct values); otherwise
        # they may still point to the native memory, which is freed with the series array: copy them then.
        if any(np.may_share_memory(index_codes, data) for index_codes, (_, data) in zip(index.codes, dictionaries)):
            index = index.copy(deep=True)
        return index
    arrays = [_decode_dictionary(dictionary, data) if dictionary is not None else data
              for _, dictionary, data in index_series]
    return pd.MultiIndex.from_arrays(arrays, names=names)


def create_data_frame_from_series_array(series_array: _pypowsybl.SeriesArray) -> pd.DataFrame:
    """
    Builds a dataframe from the series returned by the native library.

    The series flagged as index become the index of the dataframe (a ``MultiIndex`` when there are several),
    the other series its columns, in the order of the series array. A series with a mask is read as a masked array.

    A dictionary-encoded string series (``dictionary`` is not None and ``data`` holds one int code per row, -1 for
    a missing value) is decoded here: when all the levels of a multi-level index are dictionary-encoded, the
    ``MultiIndex`` is built directly from the dictionaries and codes, without materializing a string per row; a single
    index level, a level mixed with plain string levels, or a column is decoded to its strings (None for a missing
    value).

    Args:
        series_array: the series, as returned by a ``_pypowsybl`` function

    Returns:
        The dataframe. It does not share memory with the series array, which may be freed afterwards.

    Raises:
        ValueError: if no series is flagged as index
    """
    series_dict: dict[str, Any] = {}
    index_series: List[Tuple[str, Optional[List[str]], Any]] = []
    for series in series_array:
        dictionary = series.dictionary  # read once: each access converts the native strings
        if series.index:
            index_series.append((series.name, dictionary, series.data))
        elif dictionary is not None:
            series_dict[series.name] = _decode_dictionary(dictionary, series.data)
        elif series.mask.any():
            series_dict[series.name] = np.ma.masked_array(series.data, series.mask)
        else:
            series_dict[series.name] = series.data
    if not index_series:
        raise ValueError('No index in returned dataframe')
    # dict input: the dataframe copies the value columns out of the native memory
    return pd.DataFrame(series_dict, index=_create_index(index_series))


def path_to_str(path: PathOrStr) -> str:
    if isinstance(path, str):
        return path
    return path.__fspath__()
