# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Union
from os import PathLike
from pypowsybl import _pypowsybl
from pypowsybl.utils import path_to_str  # pylint: disable=protected-access
from .glsk_document import GLSKDocument


def load(file: Union[str, PathLike]) -> GLSKDocument:
    """
    Loads a GLSK file.

    Args:
        file: path to the GLSK file

    Returns:
        A GLSK document object.
    """
    file = path_to_str(file)
    return GLSKDocument(_pypowsybl.create_glsk_document(file))
