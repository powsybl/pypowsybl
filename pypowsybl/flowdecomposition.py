#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl._pypowsybl as _pypowsybl
from pypowsybl._pypowsybl import (
    XnecSelectionStrategy,
    ContingencyStrategy,
)
from pypowsybl.network import Network as _Network
from pypowsybl.util import create_data_frame_from_series_array
import pandas as _pd

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
XnecSelectionStrategy.__module__ = __name__
ContingencyStrategy.__module__ = __name__

class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a flowdecomposition execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    .. currentmodule:: pypowsybl.flowdecomposition

    Args:
        save_intermediates: Parameter only implemented in Java.
            Use``True`` to save intermediates results in the Java flow decomposition object.
        enable_losses_compensation: Enable losses compensation.
            Use ``True`` to enable AC losses compensation on the DC network.
        losses_compensation_epsilon: Filter loads from the losses compensation.
            The loads with a too small absolute active power will be not be connected to the network.
            Use ``pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON = -1`` to disable filtering.
        sensitivity_epsilon: Filter sensitivity values
            The absolute small sensitivity values will be ignored.
            Use ``pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON = -1`` to disable filtering.
        rescale_enabled: Rescale the flow decomposition to the AC reference.
            Use``True`` to rescale flow decomposition to the AC reference.
        xnec_selection_strategy: Defines how to select branches.
            Use ``ONLY_INTERCONNECTIONS`` to select only interconnections.
            Use ``INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC`` to select interconnections and branches that have at least a zone to zone PTDF greater than 5%.
        contingency_strategy: Defines how to select contingencies.
            Use ``ONLY_N_STATE`` to only work in state N.
    """
    DISABLE_LOSSES_COMPENSATION_EPSILON = -1
    DISABLE_SENSITIVITY_EPSILON = -1

    def __init__(self, 
                 save_intermediates: bool = None,
                 enable_losses_compensation: bool = None,
                 losses_compensation_epsilon: float = None,
                 sensitivity_epsilon: float = None,
                 rescale_enabled: bool = None,
                 xnec_selection_strategy: XnecSelectionStrategy = None,
                 contingency_strategy: ContingencyStrategy = None):
    
        self._init_with_default_values()
        if save_intermediates is not None:
            self.save_intermediates = save_intermediates
        if enable_losses_compensation is not None:
            self.enable_losses_compensation = enable_losses_compensation
        if losses_compensation_epsilon is not None:
            self.losses_compensation_epsilon = losses_compensation_epsilon
        if sensitivity_epsilon is not None:
            self.sensitivity_epsilon = sensitivity_epsilon
        if rescale_enabled is not None:
            self.rescale_enabled = rescale_enabled
        if xnec_selection_strategy is not None:
            self.xnec_selection_strategy = xnec_selection_strategy
        if contingency_strategy is not None:
            self.contingency_strategy = contingency_strategy

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.FlowDecompositionParameters()
        self.save_intermediates = default_parameters.save_intermediates
        self.enable_losses_compensation = default_parameters.enable_losses_compensation
        self.losses_compensation_epsilon = default_parameters.losses_compensation_epsilon
        self.sensitivity_epsilon = default_parameters.sensitivity_epsilon
        self.rescale_enabled = default_parameters.rescale_enabled
        self.xnec_selection_strategy = default_parameters.xnec_selection_strategy
        self.contingency_strategy = default_parameters.contingency_strategy

    def _to_c_parameters(self) -> _pypowsybl.FlowDecompositionParameters:
        c_parameters = _pypowsybl.FlowDecompositionParameters()
        c_parameters.save_intermediates = self.save_intermediates
        c_parameters.enable_losses_compensation = self.enable_losses_compensation
        c_parameters.losses_compensation_epsilon = self.losses_compensation_epsilon
        c_parameters.sensitivity_epsilon = self.sensitivity_epsilon
        c_parameters.rescale_enabled = self.rescale_enabled
        c_parameters.xnec_selection_strategy = self.xnec_selection_strategy
        c_parameters.contingency_strategy = self.contingency_strategy
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"save_intermediates={self.save_intermediates!r}" \
               f", enable_losses_compensation={self.enable_losses_compensation!r}" \
               f", losses_compensation_epsilon={self.losses_compensation_epsilon!r}" \
               f", sensitivity_epsilon={self.sensitivity_epsilon!r}" \
               f", rescale_enabled={self.rescale_enabled!r}" \
               f", xnec_selection_strategy={self.xnec_selection_strategy.name}" \
               f", contingency_strategy={self.contingency_strategy.name}" \
               f")"

def run(network: _Network, parameters: Parameters = None) -> _pd.DataFrame:
    """
    Runs a flow decomposition.
    
    Args:
        network:    Network on which the flow decomposition will be computed
        parameter:  Flow decomposition parameters
    
    Returns:
        A dataframe with decomposed flow for each relevant line

    Notes:
        The resulting dataframe, depending on the number of countries, will include the following columns:

            - **branch_id**: the id of the branch
            - **contingency_id**: the id of the contingency
            - **commercial_flow**: the commercial (or allocated) flow on the line (in MW)
            - **pst_flow**: the PST flow on the line (in MW)
            - **loop_flow_from_XX**: the loop flow from zone XX on the line (in MW)
            - **ac_reference_flow**: the ac reference flow on the line (in MW)
            - **dc_reference_flow**: the dc reference flow on the line (in MW)
            - **country1**: the country id of terminal 1
            - **country2**: the country id of terminal 2

        This dataframe is indexed on the xnec ID **xnec_id**.

    Examples:

        .. code-block:: python

            network = pp.network.create_eurostag_tutorial_example1_network()
            parameters = pp.flowdecomposition.Parameters()
            pp.flowdecomposition.run(network, parameters)

        It outputs something like:

        ======================== ============ ============== ======== ======== ================= ================= =============== ============= ================= ================= ========
        /                           branch_id contingency_id country1 country2 ac_reference_flow dc_reference_flow commercial_flow internal_flow loop_flow_from_be loop_flow_from_fr pst_flow
        ======================== ============ ============== ======== ======== ================= ================= =============== ============= ================= ================= ========
        xnec_id                                                                                                                                                                                
        NHV1_NHV2_1_InitialState  NHV1_NHV2_1   InitialState       FR       BE        302.444049             300.0             0.0           0.0             300.0               0.0      0.0
        NHV1_NHV2_2_InitialState  NHV1_NHV2_2   InitialState       FR       BE        302.444049             300.0             0.0           0.0             300.0               0.0      0.0
        ======================== ============ ============== ======== ======== ================= ================= =============== ============= ================= ================= ========
    """
    p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.FlowDecompositionParameters()
    res = _pypowsybl.run_flow_decomposition(network._handle, p)
    return create_data_frame_from_series_array(res)
