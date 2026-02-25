from __future__ import annotations

from typing import List, Optional

from pypowsybl._pypowsybl import DistributionMode, compute_proportional_scalable_percentages

from .element import ElementScalable
from .scalable import Scalable, JavaScalableType
from ... import Network


class ProportionalScalable(Scalable):
    """
    Scalable based on a proportional repartition between underlying network elements.
    A ProportionalScalable can be defined by
    - providing a list of injection ids or scalables with their respective repartition percentages
    - providing a list of injection ids, a network, and a distribution mode to infer the percentages

    Args:
        injection_ids: A list of injection ids on which the scalable will distribute the power change asked (incompatible with scalables)
        scalables:     A list of Scalable on which the scalable will distribute the power change asked (incompatible with injection_ids)

        percentages: A list of percentages of repartition associated to each underlying scalable/injection

        network: The network on which to evaluate the percentages according to "mode" (incompatible with percentages)
        mode: The DistributionMode used to infer percentages (incompatible with percentages),
            The available modes are : UNIFORM_DISTRIBUTION, PROPORTIONAL_TO_TARGETP, PROPORTIONAL_TO_PMAX, PROPORTIONAL_TO_DIFF_PMAX_TARGETP,
            PROPORTIONAL_TO_DIFF_TARGETP_PMIN, PROPORTIONAL_TO_P0

        min_value (optional): The minimum active power value the modification can reach
        max_value (optional): The maximum active power value the modification can reach
    """
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
        if injection_ids is not None:
            scalables = [ElementScalable(injection_id=name) for name in injection_ids]
        self.percentages = percentages if percentages is not None else self.compute_scalables_percentages(injection_ids, network, mode)
        self.children = scalables if scalables is not None else [ElementScalable(injection_id) for injection_id in injection_ids]
        super().__init__(type=JavaScalableType.PROPORTIONAL, min_value=min_value, max_value=max_value,
                         scalables=scalables, percentages=self.percentages)

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

