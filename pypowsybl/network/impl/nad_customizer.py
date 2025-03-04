# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import (
    Optional
)

from pandas import DataFrame

import pypowsybl._pypowsybl as _pp
from pypowsybl.utils import (
    _create_c_dataframe
)

class NadCustomizer:
    """
    This class represents parameters to customize a network area diagram (e.g., labels on branches)."""

    _nad_branch_labels_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('side1',0,False,False,False),
                  _pp.SeriesMetadata('middle',0,False,False,False),
                  _pp.SeriesMetadata('side2',0,False,False,False),
                  _pp.SeriesMetadata('arrow1',0,False,False,False),
                  _pp.SeriesMetadata('arrow2',0,False,False,False)]

    _nad_three_wt_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('side1',0,False,False,False),
                  _pp.SeriesMetadata('side2',0,False,False,False),
                  _pp.SeriesMetadata('side3',0,False,False,False),
                  _pp.SeriesMetadata('arrow1',0,False,False,False),
                  _pp.SeriesMetadata('arrow2',0,False,False,False),
                  _pp.SeriesMetadata('arrow3',0,False,False,False)]

    _nad_descriptions_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('type',0,False,False,False),
                  _pp.SeriesMetadata('description',0,False,False,False)]

    _nad_bus_descriptions_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('description',0,False,False,False)]

    def __init__(self, branch_labels: Optional[DataFrame] = None, three_wt_labels: Optional[DataFrame] = None,
                 bus_descriptions: Optional[DataFrame] = None, vl_descriptions: Optional[DataFrame] = None):
        self._branch_labels = branch_labels
        self._three_wt_labels = three_wt_labels
        self._bus_descriptions = bus_descriptions
        self._vl_descriptions = vl_descriptions

    @property
    def branch_labels(self) -> Optional[DataFrame]:
        """branch_labels"""
        return self._branch_labels

    @property
    def three_wt_labels(self) -> Optional[DataFrame]:
        """three_wt_labels"""
        return self._three_wt_labels

    @property
    def bus_descriptions(self) -> Optional[DataFrame]:
        """bus_description"""
        return self._bus_descriptions

    @property
    def vl_descriptions(self) -> Optional[DataFrame]:
        """vl_descriptions"""
        return self._vl_descriptions

    def _create_nad_branch_labels_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._branch_labels is None else _create_c_dataframe(self._branch_labels.fillna(''),
                                                                           NadCustomizer._nad_branch_labels_metadata)

    def _create_nad_three_wt_labels_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._three_wt_labels is None else _create_c_dataframe(self._three_wt_labels.fillna(''),
                                                                           NadCustomizer._nad_three_wt_metadata)

    def _create_nad_bus_descriptions_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._bus_descriptions is None else _create_c_dataframe(self._bus_descriptions.fillna(''),
                                                                           NadCustomizer._nad_bus_descriptions_metadata)

    def _create_nad_vl_descriptions_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._vl_descriptions is None else _create_c_dataframe(self._vl_descriptions.fillna(''),
                                                                           NadCustomizer._nad_descriptions_metadata)
