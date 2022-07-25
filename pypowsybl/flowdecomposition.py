#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl._pypowsybl as _pypowsybl
from pypowsybl.network import Network as _Network
from pypowsybl.util import create_data_frame_from_series_array
import pandas as _pd


def run(network: _Network) -> _pd.DataFrame:
    """
    Runs a flow decomposition.
    
    Args:
        network:    Network on which the flow decomposition will be computed
    
    Returns:
        A dataframe with decomposed flow for each relevant line

    Notes:
        The resulting dataframe, depending on the number of countries, will include the following columns:

            - **allocated_flow**: the allocated flow on the line (in MW)
            - **pst_flow**: the PST flow on the line (in MW)
            - **loop_flow_from_XX**: the loop flow from zone XX on the line (in MW)
            - **ac_reference_flow**: the ac reference flow on the line (in MW)
            - **dc_reference_flow**: the dc reference flow on the line (in MW)

        This dataframe is indexed on the xnec ID.

    Examples:

        .. code-block:: python

            network = pp.network.create_eurostag_tutorial_example1_network()
            pp.flowdecomposition.run(network)

        It outputs something like:

        =========== ============== ======== ================= ================= ================= =================
        \           allocated_flow pst_flow loop_flow_from_be loop_flow_from_fr ac_reference_flow dc_reference_flow
        =========== ============== ======== ================= ================= ================= =================
        xnec_id                                                                                                          
        NHV1_NHV2_1            0.0      0.0             300.0               0.0        302.444049             300.0
        NHV1_NHV2_2            0.0      0.0             300.0               0.0        302.444049             300.0
        =========== ============== ======== ================= ================= ================= =================
    """
    return create_data_frame_from_series_array(_pypowsybl.run_flow_decomposition(network._handle))

