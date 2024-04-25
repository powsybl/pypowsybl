# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from typing import List, Optional

from numpy.typing import ArrayLike
from pandas import DataFrame
from pypowsybl.report import ReportNode
from pypowsybl.network import Network
from pypowsybl.utils import _get_c_dataframes
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ShortCircuitStudyType
from .parameters import Parameters
from .short_circuit_analysis_result import ShortCircuitAnalysisResult

ShortCircuitStudyType.__module__ = __name__


class ShortCircuitAnalysis:
    """
    Allows to run a short-circuit analysis on a network.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    def set_branch_fault(self, branch_id: ArrayLike, element_id: str, r: ArrayLike, x: ArrayLike,
                         proportional_location: ArrayLike) -> None:
        self.set_faults(id=branch_id, element_id=element_id, r=r, x=x,
                        proportional_location=proportional_location, fault_type='BRANCH_FAULT')

    def set_bus_fault(self, bus_id: str, element_id: str, r: ArrayLike, x: ArrayLike) -> None:
        self.set_faults(id=bus_id, element_id=element_id, r=r, x=x, fault_type='BUS_FAULT')

    def _set_faults(self, dfs: List[Optional[DataFrame]], **kwargs: ArrayLike) -> None:
        metadata = _pypowsybl.get_faults_dataframes_metadata()
        c_dfs = _get_c_dataframes(dfs, [metadata], **kwargs)
        _pypowsybl.set_faults(self._handle, c_dfs[0])

    def set_faults(self, df: DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Define faults to be analysed in the short-circuit simulation.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:
            The current implementation allows the simulation of three-phase bus faults, where
            the fault resistance and reactance, when specified, are connected to the ground in series.

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the id of the fault.
            - **element_id**: the id of the bus on which the fault will be simulated (bus/view topology).
            - **r**: The fault resistance to ground, in Ohm (optional).
            - **x**: The fault reactance to ground, in Ohm (optional).
            - **proportional_location**: location of the fault on the branch as a percentage of the length of the branch, side 1 is the reference (optional, only for branch fault).
            - **fault_type**: The fault type either BUS_FAULT or BRANCH_FAULT

        Examples:

        .. code-block::

            analysis = pypowsybl.shortcircuit.create_analysis()

            # define a single fault as keyword arguments
            analysis.set_faults(id='F1', element_id='Bus1', r= 0, x= 0)

            # or, define multiple faults as keyword arguments
            analysis.set_faults(id=['F1', 'F2'], element_id= [ 'Bus1', 'Bus2'], r= [0, 0], x= [0,0])

            # or, define faults as a dataframe
            analysis.set_faults(pd.DataFrame.from_records(index='id', data=[{'id': 'F1', 'element_id': buses.index[0], 'r': 1, 'x': 2}]))

            # or, since resistance and reactance are not mandatory parameters
            analysis.set_faults(pd.DataFrame.from_records(index='id', data=[{'id': 'F1', 'element_id': buses.index[0]}]))
        """
        self._set_faults([df], **kwargs)

    def run(self, network: Network, parameters: Parameters = None,
            provider: str = '', reporter: ReportNode = None, report_node: ReportNode = None) -> ShortCircuitAnalysisResult:
        """ Runs a short-circuit analysis.

        Args:
            network:    Network on which the short-circuit analysis will be computed
            parameters: short-circuit analysis parameters
            provider:   Name of the short-circuit analysis implementation provider to be used.
            reporter: deprecated, use report_node instead
            report_node: the reporter to be used to create an execution report, default is None (no report)

        Returns:
            A short-circuit analysis result.
        """
        if reporter is not None:
            warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
            report_node = reporter

        p = parameters._to_c_parameters() if parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=protected-access

        return ShortCircuitAnalysisResult(
            _pypowsybl.run_shortcircuit_analysis(self._handle, network._handle, p, provider,
                                                 None if report_node is None else report_node._report_node), # pylint: disable=protected-access
            p.with_fortescue_result)
