#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
"""
Provides utility methods for dataframes handling:
 - mapping with keyword arguments
 - creation of C API dataframes
 - ...
"""
from typing import List, Dict as _Dict
from typing import Optional as _Optional, Any as _Any
from pandas import DataFrame, Index, MultiIndex
import numpy as np
from numpy.typing import ArrayLike as _ArrayLike
import pypowsybl._pypowsybl as _pp


def _to_array(value: _Any) -> np.ndarray:
    """
    Converts a scalar or array to an array
    """
    as_array = np.asarray(value)
    if as_array.ndim == 0:
        as_array = np.expand_dims(as_array, axis=0)
    if as_array.ndim != 1:
        raise ValueError(f'Network elements update: expecting only scalar or 1 dimension array '
                         f'as keyword argument, got {as_array.ndim} dimensions')
    return as_array


def _adapt_kwargs(metadata: List[_pp.SeriesMetadata], **kwargs: _Any) -> DataFrame:
    """
    Converts named arguments to a dataframe.
    """
    index_columns = [col.name for col in metadata if col.is_index]

    columns = {}
    expected_size = None
    for key, value in kwargs.items():
        if value is not None:
            col = _to_array(value)
            size = col.shape[0]
            if expected_size is None:
                expected_size = size
            elif size != expected_size:
                raise ValueError(f'Network elements update: all arguments must have the same size, '
                                 f'got size {size} for series {key}, expected {expected_size}')
            columns[key] = col

    index = None
    if len(index_columns) == 1:
        index_name = index_columns[0]
        if not index_name in columns:
            raise ValueError('No data provided for index: ' + index_name)
        index = Index(name=index_name, data=columns[index_name])
    elif len(index_columns) > 1:
        index = MultiIndex.from_arrays(names=index_columns, arrays=[columns[name] for name in index_columns])
    data = dict((k, v) for k, v in columns.items() if k not in index_columns)
    return DataFrame(index=index, data=data)


def _adapt_df_or_kwargs(metadata: List[_pp.SeriesMetadata], df: DataFrame = None, **kwargs: _Any) -> DataFrame:
    """
    Ensures we get a dataframe, either from a ready to use dataframe, or from keyword arguments.
    """
    if df is None:
        return _adapt_kwargs(metadata, **kwargs)
    if kwargs:
        raise RuntimeError('You must provide data in only one form: dataframe or named arguments')
    return df


def _create_c_dataframe(df: DataFrame, series_metadata: List[_pp.SeriesMetadata]) -> _pp.Dataframe:
    """
    Creates the C representation of a dataframe.
    """
    metadata_by_name = {s.name: s for s in series_metadata}
    is_index = []
    columns_names = []
    columns_values = []
    columns_types = []
    is_multi_index = len(df.index.names) > 1

    for idx, index_name in enumerate(df.index.names):
        if index_name is None:
            index_name = series_metadata[idx].name
        if is_multi_index:
            columns_values.append(list(df.index.get_level_values(index_name)))
        else:
            columns_values.append(list(df.index.values))
        columns_names.append(index_name)
        columns_types.append(metadata_by_name[index_name].type)
        is_index.append(True)
    columns_names.extend(df.columns.values)
    for series_name in df.columns.values:
        if series_name not in metadata_by_name:
            raise ValueError(f'No column named {series_name}')
        series = df[series_name]
        series_type = metadata_by_name[series_name].type
        columns_types.append(series_type)
        if series.values.size and isinstance(series.values[0], np.bool_):
            # to avoid DeprecationWarning: In future, it will be an error for 'np.bool_' scalars to be interpreted as an index
            columns_values.append(series.values.astype(int))
        else:
            columns_values.append(series.values)
        is_index.append(False)
    return _pp.create_dataframe(columns_values, columns_names, columns_types, is_index)


def _find_index_in_metadata(series_metadata: List[_pp.SeriesMetadata]) -> _pp.SeriesMetadata:
    return [s for s in series_metadata if s.is_index][0]


def _add_index_to_kwargs(series_metadata: List[_pp.SeriesMetadata], **kwargs: _Any) -> _Dict:
    """autofill kwargs with a default index (like pandas would do)

    Args:
        series_metadata (List[_pp.SeriesMetadata]): metadata to make match the id column name

    Returns:
        Dict: new kwargs with the index if it was not present
    """
    index_name = _find_index_in_metadata(series_metadata).name
    any_value = next(iter(kwargs.values()))
    col = _to_array(any_value)
    size = col.shape[0]
    if index_name not in kwargs:
        kwargs[index_name] = list(range(size))
    return kwargs


def _create_properties_c_dataframe(df: DataFrame) -> _pp.Dataframe:
    """
       Creates the C representation of a dataframe of properties.
    """
    is_index = []
    columns_names = []
    columns_values = []
    columns_types = []
    # index
    for _, index_name in enumerate(df.index.names):
        columns_names.append(index_name)
        columns_types.append(0)
        columns_values.append(df.index.values)
        is_index.append(True)
    # data
    for series_name in df.columns.values:
        columns_names.append(series_name)
        columns_types.append(0)
        columns_values.append(df[series_name].values)
        is_index.append(False)
    return _pp.create_dataframe(columns_values, columns_names, columns_types, is_index)


def _adapt_properties_kwargs(**kwargs: _ArrayLike) -> DataFrame:
    """
    Converts named arguments to a dataframe.
    """
    columns = {}
    expected_size = None
    for key, value in kwargs.items():
        col = _to_array(value)
        size = col.shape[0]
        if expected_size is None:
            expected_size = size
        elif size != expected_size:
            raise ValueError(f'properties creation/update: all arguments must have the same size, '
                             f'got size {size} for series {key}, expected {expected_size}')
        columns[key] = col
    index_name = 'id'
    if index_name not in columns:
        raise ValueError('No data provided for index: ' + index_name)
    index = Index(name=index_name, data=columns[index_name])
    data = dict((k, v) for k, v in columns.items() if k != 'id')
    return DataFrame(index=index, data=data)


def _get_c_dataframes(dfs: List[_Optional[DataFrame]], metadata: List[List[_pp.SeriesMetadata]],
                      **kwargs: _ArrayLike) -> List[_Optional[_pp.Dataframe]]:
    c_dfs: List[_Optional[_pp.Dataframe]] = []
    dfs[0] = _adapt_df_or_kwargs(metadata[0], dfs[0], **kwargs)
    for i, df in enumerate(dfs):
        if df is None:
            c_dfs.append(None)
        else:
            c_dfs.append(_create_c_dataframe(df, metadata[i]))
    return c_dfs
