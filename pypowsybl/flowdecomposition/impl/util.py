# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
from pypowsybl import _pypowsybl
from .flowdecomposition import FlowDecomposition


def create_decomposition() -> FlowDecomposition:
    """ Creates a flow decomposition objet, which can be used to run a flow decomposition on a network

    Example:
        .. code-block::

            >>> flowdecomposition = pp.flowdecomposition.create_decomposition()
            >>> flowdecomposition.add_monitored_elements(['line_1', 'line_2'])
            >>> flowdecomposition.run(network)

    Returns:
        A flow decomposition object, which allows to run a flow decomposition on a network.
    """
    return FlowDecomposition(_pypowsybl.create_flow_decomposition())
