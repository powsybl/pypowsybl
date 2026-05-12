from abc import ABC, abstractmethod

from pyoptinterface import ExprBuilder

from pypowsybl.opf.impl.model.network_cache import NetworkCache
from pypowsybl.opf.impl.model.variable_context import VariableContext


class CostFunction(ABC):
    def __init__(self, name: str):
        self._name = name

    @property
    def name(self) -> str:
        return self._name

    @abstractmethod
    def create(self, network_cache: NetworkCache, variable_context: VariableContext) -> ExprBuilder:
        pass
