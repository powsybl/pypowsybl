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

class NadProfile:
    """
    This class represents parameters to customize a network area diagram (e.g., labels)."""

    _nad_branch_labels_metadata=[_pp.SeriesMetadata('id',0,True,False,False),
                  _pp.SeriesMetadata('side1',0,False,False,False),
                  _pp.SeriesMetadata('middle',0,False,False,False),
                  _pp.SeriesMetadata('side2',0,False,False,False),
                  _pp.SeriesMetadata('arrow1',0,False,False,False),
                  _pp.SeriesMetadata('arrow2',0,False,False,False)]

    def __init__(self, branch_labels: Optional[DataFrame] = None):
        self._branch_labels = branch_labels

    @property
    def branch_labels(self) -> Optional[DataFrame]:
        """branch_labels"""
        return self._branch_labels


    def _create_nad_branch_labels_c_dataframe(self) -> Optional[_pp.Dataframe]:
        return None if self._branch_labels is None else _create_c_dataframe(self._branch_labels.fillna(''),
                                                                           NadProfile._nad_branch_labels_metadata)
