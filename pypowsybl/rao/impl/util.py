# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl import _pypowsybl
from .rao import Rao

def create_rao() -> Rao:
    """ Creates a rao objet, which can be used to run a remedial action optimisation on a network
    Returns:
        A rao object
    """
    return Rao(_pypowsybl.create_rao())