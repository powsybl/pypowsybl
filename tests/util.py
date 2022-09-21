# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from typing import Union, List
import io
import pandas as pd


def dataframe_from_string(df_str: str, index: Union[str, List[str]] = 'id') -> pd.DataFrame:
    """
    Creates a dataframe from a table provided as a fixed-width formatted string.
    """
    return pd.read_fwf(io.StringIO(df_str)).set_index(index)