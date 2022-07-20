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
    """
    return create_data_frame_from_series_array(_pypowsybl.run_flow_decomposition(network._handle))

