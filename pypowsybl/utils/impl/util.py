#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from os import PathLike
from typing import Union
import pandas as pd
from pypowsybl import _pypowsybl

PathOrStr = Union[str, PathLike]


def create_data_frame_from_series_array(series_array: _pypowsybl.SeriesArray) -> pd.DataFrame:
    series_dict = {}
    index_data = []
    index_names = []
    for series in series_array:
        if series.index:
            index_data.append(series.data)
            index_names.append(series.name)
        else:
            series_dict[series.name] = series.data
    if not index_names:
        raise ValueError('No index in returned dataframe')
    if len(index_names) == 1:
        index = pd.Index(index_data[0], name=index_names[0])
    else:
        index = pd.MultiIndex.from_arrays(index_data, names=index_names)
    return pd.DataFrame(series_dict, index=index)


def path_to_str(path: PathOrStr) -> str:
    if isinstance(path, str):
        return path
    return path.__fspath__()
