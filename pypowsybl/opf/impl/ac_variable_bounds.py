from abc import ABC, abstractmethod

from pyoptinterface import ipopt

from pypowsybl.opf.impl.ac_parameters import AcOptimalPowerFlowParameters
from pypowsybl.opf.impl.ac_variable_context import AcVariableContext
from pypowsybl.opf.impl.network_cache import NetworkCache


class AcVariableBounds(ABC):
    @abstractmethod
    def add(self,
            parameters: AcOptimalPowerFlowParameters,
            network_cache: NetworkCache,
            variable_context: AcVariableContext,
            model: ipopt.Model):
        pass
