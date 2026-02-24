from __future__ import annotations

from typing import List, Optional

from pypowsybl._pypowsybl import DistributionMode, compute_proportional_scalable_percentages

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType
from ... import Network


class ProportionalScalable(Scalable):
    """Scalable based on a proportional repartition between underlying network elements."""
    children: List[Scalable] = []
    percentages: List[float] = []

    def __init__(self, injection_ids: Optional[List[str]] = None,
                 scalables: Optional[List[Scalable]] = None,
                 min_value: Optional[float] = None,
                 max_value: Optional[float] = None,
                 percentages: Optional[List[float]] = None,
                 network: Optional[Network] = None,
                 mode: Optional[DistributionMode] = None):
        if injection_ids is not None and scalables is not None:
            raise RuntimeError('Parameters "injection_ids" and "scalables" are mutually exclusive.')
        if injection_ids is None and scalables is None:
            raise RuntimeError('One of "injection_ids" and "scalables" parameters must be defined.')
        if percentages is None and (network is None or mode is None):
            raise RuntimeError('Parameters "network", "injection_ids" and "mode" must be defined if a list of percentages is not provided.')
        self.percentages = percentages if percentages is not None else self.compute_scalables_percentages(injection_ids, network, mode)
        self.children = scalables if scalables is not None else [ElementScalable(injection_id) for injection_id in injection_ids]
        super().__init__(type=JavaScalableType.PROPORTIONAL, min_value=min_value, max_value=max_value,
                         scalables=scalables, percentages=self.percentages)

    @staticmethod
    def compute_scalables_percentages(injection_ids: List[str], network: Network, mode: DistributionMode) -> List[float]:
        compute_proportional_scalable_percentages(injection_ids, mode, network._handle)
