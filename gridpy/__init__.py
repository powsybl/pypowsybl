#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from _gridpy import GridPyError

__version__ = '0.5.0'


def set_debug_mode(debug: bool = True):
    _gridpy.set_debug_mode(debug)


def print_version():
    print(_gridpy.get_version_table())
