from abc import ABC, abstractmethod

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.function_context import FunctionContext
from pypowsybl.opf.impl.model.model_parameters import ModelParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class Constraints(ABC):
    @abstractmethod
    def add(self,
            parameters: ModelParameters,
            network_cache: NetworkCache,
            variable_context: VariableContext,
            function_context: FunctionContext,
            model: ipopt.Model) -> None:
        pass
