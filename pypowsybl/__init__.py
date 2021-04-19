#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
from _pypowsybl import PyPowsyblError

__version__ = '0.8.0'

import pypowsybl.network
import pypowsybl.loadflow
import pypowsybl.security_analysis
import pypowsybl.sensitivity_analysis


# make this modules importable with pythonic syntax "from pypowsybl.XXX import YYY
# for example:
# >>> import pypowsybl as pp
# >>> network  = pp.network.create_ieee14()
__all__ = [
    "network",
    "loadflow",
    "security_analysis",
    "sensitivity_analysis"
]


def set_debug_mode(debug: bool = True):
    _pypowsybl.set_debug_mode(debug)


def print_version():
    print(_pypowsybl.get_version_table())
