#
# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations  # Necessary for type alias like _DataFrame to work with sphinx

from typing import (
    List as _List,
    Dict as _Dict,
    Optional as _Optional,
)

import pandas as _pd
from numpy.typing import ArrayLike as _ArrayLike
from pandas import DataFrame as _DataFrame
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ShortCircuitFaultType, ShortCircuitStudyType
from pypowsybl.network import Network as _Network
from pypowsybl.report import Reporter as _Reporter
from pypowsybl.util import _create_data_frame_from_series_array
from pypowsybl.utils.dataframes import _get_c_dataframes

ShortCircuitStudyType.__module__ = __name__



