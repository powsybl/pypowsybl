#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
from _pypowsybl import PyPowsyblError

__version__ = '0.7.0'


def set_debug_mode(debug: bool = True):
    _pypowsybl.set_debug_mode(debug)


def print_version():
    print(_pypowsybl.get_version_table())
