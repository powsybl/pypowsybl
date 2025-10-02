# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional

from pandas import DataFrame

import pypowsybl._pypowsybl as _pp
from pypowsybl.utils import _create_c_dataframe

class SldProfile:
    """
    This class represents parameters to customize a single line diagram (e.g., labels)."""

    _sld_labels_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('label',0,False,False,False),
                  _pp.SeriesMetadata('additional_label',0,False,False,False)]

    _sld_feeders_info_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('type',0,False,False,False),
                  _pp.SeriesMetadata('side',0,False,False,False),
                  _pp.SeriesMetadata('direction',0,False,False,False),
                  _pp.SeriesMetadata('label',0,False,False,False)]

    def __init__(self, labels: Optional[DataFrame] = None, feeders_info: Optional[DataFrame] = None):
        self._labels = labels
        self._feeders_info = feeders_info

    @property
    def labels(self) -> Optional[DataFrame]:
        """labels"""
        return self._labels

    @property
    def feeders_info(self) -> Optional[DataFrame]:
        """feeders_info"""
        return self._feeders_info


    def _create_sld_labels_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._labels is None else _create_c_dataframe(self._labels.fillna(''),
                                                                           SldProfile._sld_labels_metadata)

    def _create_sld_feeders_info_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._feeders_info is None else _create_c_dataframe(self._feeders_info.fillna(''),
                                                                           SldProfile._sld_feeders_info_metadata)
