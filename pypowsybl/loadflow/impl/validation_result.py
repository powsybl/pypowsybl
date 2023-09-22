# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional
import pandas as pd

OptionalDf = Optional[pd.DataFrame]


class ValidationResult:
    """
    The result of a loadflow validation.
    """

    def __init__(self, branch_flows: OptionalDf, buses: OptionalDf, generators: OptionalDf, svcs: OptionalDf,
                 shunts: OptionalDf, twts: OptionalDf, t3wts: OptionalDf):
        self._branch_flows = branch_flows
        self._buses = buses
        self._generators = generators
        self._svcs = svcs
        self._shunts = shunts
        self._twts = twts
        self._t3wts = t3wts
        self._valid = self._is_valid_or_unchecked(self.branch_flows) and self._is_valid_or_unchecked(self.buses) \
                      and self._is_valid_or_unchecked(self.generators) and self._is_valid_or_unchecked(self.svcs) \
                      and self._is_valid_or_unchecked(self.shunts) and self._is_valid_or_unchecked(self.twts) \
                      and self._is_valid_or_unchecked(self.t3wts)

    @staticmethod
    def _is_valid_or_unchecked(df: OptionalDf) -> bool:
        return df is None or df['validated'].all()

    @property
    def branch_flows(self) -> OptionalDf:
        """
        Validation results for branch flows.
        """
        return self._branch_flows

    @property
    def buses(self) -> OptionalDf:
        """
        Validation results for buses.
        """
        return self._buses

    @property
    def generators(self) -> OptionalDf:
        """
        Validation results for generators.
        """
        return self._generators

    @property
    def svcs(self) -> OptionalDf:
        """
        Validation results for SVCs.
        """
        return self._svcs

    @property
    def shunts(self) -> OptionalDf:
        """
        Validation results for shunts.
        """
        return self._shunts

    @property
    def twts(self) -> OptionalDf:
        """
        Validation results for two winding transformers.
        """
        return self._twts

    @property
    def t3wts(self) -> OptionalDf:
        """
        Validation results for three winding transformers.
        """
        return self._t3wts

    @property
    def valid(self) -> bool:
        """
        True if all checked data is valid.
        """
        return self._valid
