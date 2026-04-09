#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations

from typing import List, Optional

from pypowsybl._pypowsybl import DistributionMode, compute_proportional_scalable_percentages

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType
from ... import Network


class ProportionalScalable(Scalable):
    """
    Scalable based on a proportional repartition of power change between underlying elements.
    """
    children: List[Scalable] = []
    percentages: List[float] = []

    def __init__(self, scalables: List[Scalable],
                 percentages: List[float],
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None):
        self.children = scalables
        self.percentages = percentages
        super().__init__(type=JavaScalableType.PROPORTIONAL, min_value=min_value, max_value=max_value,
                         scalables=scalables, percentages=self.percentages)

    @classmethod
    def from_ids_and_percentages(cls, injection_ids: List[str], percentages: List[float],
                                 min_value: Optional[float] = None, max_value: Optional[float] = None) -> ProportionalScalable:
        """
        Create a ProportionalScalable from a list of injection IDs and their corresponding percentage of power repartition.

        Args:
            injection_ids: List of injection IDs.
            percentages: List of percentages corresponding to each injection.
            min_value: Minimum value for the scalable. Defaults to None.
            max_value: Maximum value for the scalable. Defaults to None.
        """
        return cls([ElementScalable(injection_id=name) for name in injection_ids], percentages=percentages,
                   min_value=min_value, max_value=max_value)

    @classmethod
    def from_scalables_and_percentages(cls, scalables: List[Scalable], percentages: List[float],
                                       min_value: Optional[float] = None, max_value: Optional[float] = None) -> ProportionalScalable:
        """
        Create a ProportionalScalable from a list of Scalable and their corresponding percentage of power repartition.

        Args:
            scalables: List of underlying Scalable.
            percentages: List of percentages corresponding to each injection.
            min_value: Minimum value for the scalable. Defaults to None.
            max_value: Maximum value for the scalable. Defaults to None.
        """
        return cls(scalables=scalables, percentages=percentages, min_value=min_value, max_value=max_value)

    @classmethod
    def from_ids_and_distribution_mode(cls, injection_ids: List[str], network: Network, mode: DistributionMode,
                                  min_value: Optional[float] = None, max_value: Optional[float] = None) -> ProportionalScalable:
        """
        Create a ProportionalScalable from a list of injection IDs and their corresponding percentage of power repartition.

        Args:
            injection_ids: List of injection IDs.
            network: Network with which to compute the percentages according to "mode".
            mode: Distribution mode to use for computing percentages
                The available modes are : UNIFORM_DISTRIBUTION, PROPORTIONAL_TO_TARGETP, PROPORTIONAL_TO_PMAX, PROPORTIONAL_TO_DIFF_PMAX_TARGETP,
                PROPORTIONAL_TO_DIFF_TARGETP_PMIN, PROPORTIONAL_TO_P0
            min_value: Minimum value for the scalable. Defaults to None.
            max_value: Maximum value for the scalable. Defaults to None.
        """
        return cls(scalables=[ElementScalable(injection_id=name) for name in injection_ids],
                   percentages=cls.compute_scalables_percentages(injection_ids, network, mode),
                   min_value=min_value, max_value=max_value)

    @staticmethod
    def compute_scalables_percentages(injection_ids: List[str], network: Network, mode: DistributionMode) -> List[float]:
        return compute_proportional_scalable_percentages(injection_ids, mode, network._handle)

    def __repr__(self) -> str:
        desc: str =  f"{self.__class__.__name__}(" \
                     f"children={self.children}" \
                     f", percentages={self.percentages}"
        desc += f", min_value={self.min_value}" if self.min_value != -float('inf') else ""
        desc += f", max_value={self.max_value}" if self.max_value != float('inf') else ""
        return desc + f")"

