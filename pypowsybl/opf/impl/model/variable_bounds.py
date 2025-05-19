from abc import ABC, abstractmethod

from pyoptinterface import ipopt

from pypowsybl.opf.impl.model.parameters import OptimalPowerFlowParameters
from pypowsybl.opf.impl.model.variable_context import VariableContext
from pypowsybl.opf.impl.model.network_cache import NetworkCache


class VariableBounds(ABC):
    @abstractmethod
    def add(self,
            parameters: OptimalPowerFlowParameters,
            network_cache: NetworkCache,
            variable_context: VariableContext,
            model: ipopt.Model):
        pass
