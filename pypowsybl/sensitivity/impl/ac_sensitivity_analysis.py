# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from collections.abc import Mapping as MappingABC
from typing import List, Mapping, Sequence, Union, Optional
import pandas as pd
from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.report import ReportNode
from pypowsybl.loadflow import Parameters as LfParameters
from pypowsybl._pypowsybl import ContingencyContextType, SensitivityFunctionType, SensitivityVariableType
from .ac_sensitivity_analysis_result import AcSensitivityAnalysisResult
from .ac_sensitivity_analysis_adjoint_result import AcSensitivityAnalysisAdjointResult
from .sensitivity_analysis_result import DEFAULT_MATRIX_ID
from .sensitivity import SensitivityAnalysis
from .parameters import Parameters

#: ``dL/dfunction`` as accepted by :meth:`AcSensitivityAnalysis.run_adjoint`: either a flat sequence over
#: every declared function, or one entry per factor matrix — a sequence in that matrix's declared function
#: order, or a ``pandas.Series`` indexed by function id, which is aligned on its index rather than on
#: position.
Cotangents = Union[Sequence[float], Mapping[str, Union[Sequence[float], pd.Series]]]


class AcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents an AC sensitivity analysis."""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        SensitivityAnalysis.__init__(self, handle)
        self.bus_voltage_ids: List[str] = []
        self.target_voltage_ids: List[str] = []

    def set_bus_voltage_factor_matrix(self, bus_ids: List[str], target_voltage_ids: List[str]) -> None:
        """
        .. deprecated:: 1.1.0
          Use :meth:`add_bus_voltage_factor_matrix` instead.

        Defines buses voltage sensitivities to be computed.

        Args:
            bus_ids:            IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
       """
        warnings.warn("set_bus_voltage_factor_matrix is deprecated, use add_bus_voltage_factor_matrix instead",
                      DeprecationWarning)
        self.add_bus_voltage_factor_matrix(bus_ids, target_voltage_ids)

    def add_bus_voltage_factor_matrix(self, bus_ids: List[str], target_voltage_ids: List[str], matrix_id: str = DEFAULT_MATRIX_ID) -> None:
        """
        Defines buses voltage sensitivities to be computed.

        Args:
            bus_ids:            IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
            matrix_id:          The matrix unique identifier, to be used to retrieve the sensibility value
        """
        self.add_factor_matrix(bus_ids, target_voltage_ids, [], ContingencyContextType.ALL,
                               SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.BUS_TARGET_VOLTAGE, matrix_id)
        self.bus_voltage_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def add_shunt_susceptance_factor_matrix(self, functions_ids: List[str], shunt_ids: List[str],
                                            sensitivity_function_type: SensitivityFunctionType,
                                            matrix_id: str = DEFAULT_MATRIX_ID) -> None:
        """
        Declare sensitivities of monitored functions to shunt susceptances — the TVC shunt lever.

        Args:
            functions_ids:             ids of the monitored elements (buses for BUS_VOLTAGE, branches for
                                       BRANCH_CURRENT / BRANCH_ACTIVE_POWER, ...)
            shunt_ids:                 ids of the controllable shunt compensators (the levers)
            sensitivity_function_type: type of the monitored function (e.g. BUS_VOLTAGE, BRANCH_CURRENT_1)
            matrix_id:                 the factor matrix unique identifier
        """
        self.add_factor_matrix(functions_ids, shunt_ids, [], ContingencyContextType.ALL,
                               sensitivity_function_type, SensitivityVariableType.SHUNT_COMPENSATOR_SUSCEPTANCE, matrix_id)

    def add_branch_admittance_factor_matrix(self, functions_ids: List[str], branch_ids: List[str],
                                            sensitivity_function_type: SensitivityFunctionType,
                                            matrix_id: str = DEFAULT_MATRIX_ID) -> None:
        """
        Declare sensitivities of monitored functions to branch admittances — the TVC line lever.

        Args:
            functions_ids:             ids of the monitored elements
            branch_ids:                ids of the controllable branches/lines (the levers)
            sensitivity_function_type: type of the monitored function (e.g. BUS_VOLTAGE, BRANCH_CURRENT_1)
            matrix_id:                 the factor matrix unique identifier
        """
        self.add_factor_matrix(functions_ids, branch_ids, [], ContingencyContextType.ALL,
                               sensitivity_function_type, SensitivityVariableType.BRANCH_ADMITTANCE, matrix_id)

    def add_svc_pilot_factor_matrix(self, functions_ids: List[str], zone_ids: List[str],
                                    sensitivity_function_type: SensitivityFunctionType,
                                    matrix_id: str = DEFAULT_MATRIX_ID) -> None:
        """
        Declare sensitivities of monitored functions to secondary-voltage-control pilot-point target
        voltages — the TVC RST lever. The variable ids are the SVC zone names (the network must carry the
        SecondaryVoltageControl extension and the load flow must run with secondaryVoltageControl on).

        Args:
            functions_ids:             ids of the monitored elements
            zone_ids:                  names of the SVC zones (the levers)
            sensitivity_function_type: type of the monitored function (e.g. BUS_VOLTAGE, BRANCH_CURRENT_1)
            matrix_id:                 the factor matrix unique identifier
        """
        self.add_factor_matrix(functions_ids, zone_ids, [], ContingencyContextType.ALL,
                               sensitivity_function_type, SensitivityVariableType.SVC_PILOT_POINT_TARGET_VOLTAGE, matrix_id)

    @staticmethod
    def _to_ac_c_parameters(parameters: Optional[Union[Parameters, LfParameters]]) -> _pypowsybl.SensitivityAnalysisParameters:
        # Bare load flow parameters are wrapped, None means defaults, and dc is forced off: this is the AC
        # analysis, so a dc flag left on by the caller would silently run the wrong thing.
        sensitivity_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                          LfParameters) else parameters
        if sensitivity_parameters is None:
            sensitivity_parameters = Parameters()
        c_parameters: _pypowsybl.SensitivityAnalysisParameters = sensitivity_parameters._to_c_parameters()  # pylint: disable=protected-access
        c_parameters.loadflow_parameters.dc = False
        return c_parameters

    def run(self, network: Network, parameters: Optional[Union[Parameters, LfParameters]] = None,
            provider: str = '', report_node: Optional[ReportNode] = None) -> AcSensitivityAnalysisResult:
        """
        Runs the sensitivity analysis.

        Args:
            network:     The network
            parameters:  The sensitivity parameters
            provider:    Name of the sensitivity analysis provider
            report_node: The reporter to be used to create an execution report, default is None (no report)

        Returns:
            a sensitivity analysis result
        """
        p = self._to_ac_c_parameters(parameters)
        return AcSensitivityAnalysisResult(
            _pypowsybl.run_sensitivity_analysis(self._handle, network._handle, p, provider, None if report_node is None else report_node._report_node), # pylint: disable=protected-access
            functions_ids=self.functions_ids, function_data_frame_index=self.function_data_frame_index)

    def run_adjoint(self, network: Network, cotangents: Cotangents,
                    parameters: Optional[Union[Parameters, LfParameters]] = None,
                    provider: str = '') -> AcSensitivityAnalysisAdjointResult:
        """
        Runs a reverse-mode (adjoint / VJP) sensitivity analysis.

        Given output cotangents ``dL/dfunction`` over the declared functions, returns ``dL/dvariable`` for
        every declared variable — ``theta_bar = S^T . y_bar`` — in a single transpose solve, without
        materialising the sensitivity matrix. Reuses the OpenLoadFlow load flow retained in the network
        cache, so a load flow with ``network_cache_enabled=True`` must have run on ``network`` first.

        Args:
            network:    The network (with a warm networkCacheEnabled AC load flow)
            cotangents: ``dL/dfunction``; either a flat sequence aligned with all declared functions in
                        declaration order, or a dict ``{matrix_id: values over that factor matrix's functions}``
                        (the ``matrix_id`` used when the factors were declared). Per-matrix
                        values may be a plain sequence, taken in that matrix's declared function order, or a
                        ``pandas.Series``, aligned on its index by function id — a flat ``Series`` is refused,
                        having no unambiguous alignment across matrices.
                        A monitored function has ONE cotangent whichever matrices declare it: when several
                        factor matrices monitor the same function — the usual shape when fusing lever families,
                        each pairing the same functions with its own variables — the repeated entries must
                        agree, and 0.0 reads as "not stated here". Conflicting non-zero values raise rather
                        than being summed into a gradient that is silently a multiple of the right one.
            parameters: The sensitivity parameters
            provider:   Name of the sensitivity analysis provider

        Returns:
            an adjoint sensitivity analysis result (``dL/dvariable`` per factor matrix)
        """
        p = self._to_ac_c_parameters(parameters)
        flat_cotangents = self._flatten_cotangents(cotangents)
        return AcSensitivityAnalysisAdjointResult(
            _pypowsybl.run_sensitivity_analysis_adjoint(self._handle, network._handle, flat_cotangents, p, provider),
            variable_ids=self.function_data_frame_index)

    def _flatten_cotangents(self, cotangents: Cotangents) -> List[float]:
        # flatten to a vector aligned with the declared functions across all matrices, in declaration order
        # Mapping, not dict: the accepted type is a Mapping, and anything mapping-like that fell through to
        # the flat branch below would be iterated as its KEYS and rejected for a nonsensical reason.
        if isinstance(cotangents, MappingABC):
            flat: List[float] = []
            for matrix_id, function_ids in self.functions_ids.items():
                values = cotangents.get(matrix_id)
                if values is None:
                    flat.extend([0.0] * len(function_ids))
                    continue
                flat.extend(self._matrix_cotangents(matrix_id, function_ids, values))
            return flat
        if isinstance(cotangents, pd.Series):
            # A Series carries an index, and the flat form has no unambiguous one to match it against: it
            # spans every factor matrix in declaration order, and the same function id may appear in several
            # of them. Silently taking the values in Series order would misalign whenever that order is not
            # the declaration order, so ask for the form that can be aligned instead.
            raise ValueError(
                'cotangents given as a pandas Series must be passed per factor matrix, as '
                '{matrix_id: series}, so the index can be aligned with that matrix\'s functions; '
                'a flat Series has no unambiguous alignment across matrices.')
        flat = [float(v) for v in cotangents]
        expected = sum(len(f) for f in self.functions_ids.values())
        if len(flat) != expected:
            raise ValueError(f"cotangents must have length {expected} (total declared functions), got {len(flat)}")
        return flat

    @staticmethod
    def _matrix_cotangents(matrix_id: str, function_ids: List[str], values: Union[Sequence[float], pd.Series]) -> List[float]:
        # One factor matrix's cotangents, in its declared function order.
        if isinstance(values, pd.Series):
            # Align on the index rather than on position: a Series is usually built by name (from a
            # get_sensitivity_matrix column, a groupby, a dict), and its order need not be the declared
            # one. Taking it positionally would pair each cotangent with the wrong function and return a
            # plausible, wrong gradient. Reindexing raises on a duplicate label, which is what we want.
            missing = [f for f in function_ids if f not in values.index]
            if missing:
                raise ValueError(
                    f"cotangents for factor matrix '{matrix_id}' are missing {len(missing)} of its "
                    f'{len(function_ids)} declared functions, e.g. {missing[:5]}')
            return [float(v) for v in values.reindex(function_ids).to_numpy(dtype=float)]
        listed = list(values)
        if len(listed) != len(function_ids):
            raise ValueError(
                f"cotangents for factor matrix '{matrix_id}' must have length {len(function_ids)}, got {len(listed)}")
        return [float(v) for v in listed]
