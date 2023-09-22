#
# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations
from typing import Union, List, Callable, Optional
import pandas as pd
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ContingencyContextType, DefaultXnecProvider
from pypowsybl.network import Network
from pypowsybl.utils import create_data_frame_from_series_array
import pypowsybl.loadflow
from .parameters import Parameters

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
ContingencyContextType.__module__ = __name__


class FlowDecomposition:
    """
    Allow to specify monitored elements and run a flow decomposition on a network.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle) -> None:
        self._handle = handle

    def __get_contingency_id(self, contingency_id_provider: Optional[Callable[[str], str]], element_id: str) -> str:
        return contingency_id_provider(element_id) if contingency_id_provider else element_id

    def add_single_element_contingency(self, element_id: str, contingency_id: str = None) -> FlowDecomposition:
        """
        Add a contingency with a single element (1 N-1 state).

        Args:
            element_id:     Id of the element
            contingency_id: If of the contingency. By default, the id of the contingency is the one of the element.
        """
        if contingency_id is None:
            contingency_id = element_id
        return self.add_multiple_elements_contingency(elements_ids=[element_id], contingency_id=contingency_id)

    def add_single_element_contingencies(self, element_ids: List[str],
                                         contingency_id_provider: Callable[[str], str] = None) -> FlowDecomposition:
        """
        Add a contingency for each element (n N-1 states).

        Args:
            element_ids:                List of elements
            contingency_id_provider:    Function to transform an element id to a contingency id. By default, the identity function is used.
        """
        for element_id in element_ids:
            self.add_multiple_elements_contingency(elements_ids=[element_id],
                                                   contingency_id=self.__get_contingency_id(contingency_id_provider,
                                                                                            element_id))
        return self

    def add_multiple_elements_contingency(self, elements_ids: List[str],
                                          contingency_id: str = None) -> FlowDecomposition:
        """
        Add a contingency with multiple elements (1 N-k state).

        Args:
            element_ids:    List of elements
            contingency_id: Id of the contingency. By default, the concatenation of each element id is used.
        """
        if contingency_id is None:
            contingency_id = '_'.join(elements_ids)
        _pypowsybl.add_contingency_for_flow_decomposition(self._handle, contingency_id=contingency_id,
                                                          elements_ids=elements_ids)
        return self

    def add_monitored_elements(self, branch_ids: Union[List[str], str],
                               contingency_ids: Union[List[str], str] = None,
                               contingency_context_type: ContingencyContextType = ContingencyContextType.ALL) -> FlowDecomposition:
        """
        Add branches to be monitored by the flow decomposition.
        This will create a XNEC for each valid pair of branch and state.
        You can select the type of states you want.
        If you provide contingency ids, do not forget to create them **before** calling this function.
        If no contingency ids are provided, elements added by this function will be XNE only.

        Args:
            branch_ids:                 List of branches to monitor
            contingency_ids:            List of contingencies
            contingency_context_type:   Defines if the branches should be monitored for all states (ALL by default), only N situation (NONE) or only specified contingencies (SPECIFIC)
        """
        if contingency_context_type is not ContingencyContextType.NONE and contingency_ids:
            self.add_postcontingency_monitored_elements(branch_ids, contingency_ids)
        if contingency_context_type is not ContingencyContextType.SPECIFIC:
            self.add_precontingency_monitored_elements(branch_ids)
        return self

    def add_precontingency_monitored_elements(self, branch_ids: Union[List[str], str]) -> FlowDecomposition:
        """
        Add branches to be monitored by the flow decomposition on precontingency state (XNE(s)).

        Args:
            branch_ids: List of branches to be monitored
        """
        if isinstance(branch_ids, str):
            branch_ids = [branch_ids]
        _pypowsybl.add_precontingency_monitored_elements_for_flow_decomposition(self._handle, branch_ids)
        return self

    def add_postcontingency_monitored_elements(self, branch_ids: Union[List[str], str],
                                               contingency_ids: Union[List[str], str]) -> FlowDecomposition:
        """
        Add branches to be monitored by the flow decomposition on postcontingency state (XNEC(s)).
        This will create a XNEC for each valid pair of branch and contingency.
        A pair is valid if the contingency does not contain the branch.
        Do not forget to create contingencies **before** calling this function.

        Args:
            branch_ids:         List of branches to be monitored
            contingency_ids:    List of contingencies
        """
        if isinstance(branch_ids, str):
            branch_ids = [branch_ids]
        if isinstance(contingency_ids, str):
            contingency_ids = [contingency_ids]
        _pypowsybl.add_postcontingency_monitored_elements_for_flow_decomposition(self._handle, branch_ids=branch_ids,
                                                                                 contingency_ids=contingency_ids)
        return self

    def add_5perc_ptdf_as_monitored_elements(self) -> FlowDecomposition:
        '''
        Add branches that have a zone to zone PTDF greater than 5% or that are interconnections to be monitored on a precontingency state (XNE).
        '''
        _pypowsybl.add_additional_xnec_provider_for_flow_decomposition(self._handle,
                                                                       DefaultXnecProvider.GT_5_PERC_ZONE_TO_ZONE_PTDF)
        return self

    def add_interconnections_as_monitored_elements(self) -> FlowDecomposition:
        '''
        Add branches that are interconnections to be monitored on a precontingency state (XNE).
        '''
        _pypowsybl.add_additional_xnec_provider_for_flow_decomposition(self._handle,
                                                                       DefaultXnecProvider.INTERCONNECTIONS)
        return self

    def add_all_branches_as_monitored_elements(self) -> FlowDecomposition:
        '''
        Add all branches of the network to be monitored on a precontingency state (XNE).
        '''
        _pypowsybl.add_additional_xnec_provider_for_flow_decomposition(self._handle, DefaultXnecProvider.ALL_BRANCHES)
        return self

    def run(self, network: Network, flow_decomposition_parameters: Parameters = None,
            load_flow_parameters: pypowsybl.loadflow.Parameters = None) -> pd.DataFrame:
        """
        Runs a flow decomposition.

        Args:
            network:                        Network on which the flow decomposition will be computed
            flow_decomposition_parameters:  Flow decomposition parameters
            load_flow_parameters:           Load flow parameters

        Returns:
            A dataframe with decomposed flow for each relevant line

        Notes:
            The resulting dataframe, depending on the number of countries, will include the following columns:

                - **branch_id**: the id of the branch
                - **contingency_id**: the id of the contingency
                - **country1**: the country id of terminal 1
                - **country2**: the country id of terminal 2
                - **ac_reference_flow**: the ac reference flow on the line (in MW)
                - **dc_reference_flow**: the dc reference flow on the line (in MW)
                - **commercial_flow**: the commercial (or allocated) flow on the line (in MW)
                - **x_node_flow**: the flow created by unmerged xnodes (in MW)
                - **pst_flow**: the PST flow on the line (in MW)
                - **internal_flow**: the internal flow on the line (in MW)
                - **loop_flow_from_XX**: the loop flow from zone XX on the line (in MW). One column per country

            This dataframe is indexed on the xnec ID **xnec_id**.

        Example:
            .. code-block:: python

                >>> network = pp.network.create_eurostag_tutorial_example1_network()
                >>> flow_decomposition_parameters = pp.flowdecomposition.Parameters()
                >>> load_flow_parameters = pp.loadflow.Parameters()
                >>> branch_ids = ['NHV1_NHV2_1', 'NHV1_NHV2_2']
                >>> flowdecomposition = pp.flowdecomposition.create_decomposition()
                ...     .add_single_element_contingencies(branch_ids)
                ...     .add_monitored_elements(branch_ids, branch_ids)
                >>> flowdecomposition.run(network, flow_decomposition_parameters=flow_decomposition_parameters, load_flow_parameters=load_flow_parameters)

            It outputs something like:

            ======================= =========== ============== ======== ======== ================= ================= =============== =========== ======== ============= ================= =================
            /                       branch_id   contingency_id country1 country2 ac_reference_flow dc_reference_flow commercial_flow x_node_flow pst_flow internal_flow loop_flow_from_be loop_flow_from_fr
            ======================= =========== ============== ======== ======== ================= ================= =============== =========== ======== ============= ================= =================
            xnec_id
            NHV1_NHV2_1             NHV1_NHV2_1                      FR       BE        302.444049             300.0             0.0         0.0      0.0           0.0             300.0               0.0
            NHV1_NHV2_1_NHV1_NHV2_2 NHV1_NHV2_1 NHV1_NHV2_2          FR       BE        610.562161             600.0             0.0         0.0      0.0           0.0             600.0               0.0
            NHV1_NHV2_2             NHV1_NHV2_2                      FR       BE        302.444049             300.0             0.0         0.0      0.0           0.0             300.0               0.0
            NHV1_NHV2_2_NHV1_NHV2_1 NHV1_NHV2_2 NHV1_NHV2_1          FR       BE        610.562161             600.0             0.0         0.0      0.0           0.0             600.0               0.0
            ======================= =========== ============== ======== ======== ================= ================= =============== =========== ======== ============= ================= =================
        """
        fd_p = flow_decomposition_parameters._to_c_parameters() if flow_decomposition_parameters is not None else _pypowsybl.FlowDecompositionParameters()  # pylint: disable=protected-access
        lf_p = load_flow_parameters._to_c_parameters() if load_flow_parameters is not None else _pypowsybl.LoadFlowParameters()  # pylint: disable=protected-access
        res = _pypowsybl.run_flow_decomposition(self._handle, network._handle, fd_p, lf_p)
        return create_data_frame_from_series_array(res)
