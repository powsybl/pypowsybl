#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import os as _os
import inspect as _inspect
import logging
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import PyPowsyblError
from pypowsybl import (
    network,
    loadflow,
    security,
    sensitivity,
    glsk,
    flowdecomposition,
)

__version__ = '0.19.0'

# set JVM java.library.path to pypowsybl module installation directory to be able to load math library
_pypowsybl.set_java_library_path(_os.path.dirname(_inspect.getfile(_pypowsybl)))

# make this modules importable with pythonic syntax "from pypowsybl.XXX import YYY
# for example:
# >>> import pypowsybl as pp
# >>> network  = pp.network.create_ieee14()
__all__ = [
    "network",
    "loadflow",
    "security",
    "sensitivity",
    "glsk",
    "flowdecomposition"
]


# setup a default logger that is the powsybl logger with by default no handler to avoir printing logs >= WARNING
# to std err
powsyblLogger = logging.getLogger('powsybl')
powsyblLogger.addHandler(logging.NullHandler())
_pypowsybl.set_logger(powsyblLogger)


def set_config_read(read_config: bool = True) -> None:
    """Set read ~/.itools/config.yml or not

    Args:
        read_config(bool): defaults to True
    """
    _pypowsybl.set_config_read(read_config)


def is_config_read() -> bool:
    return _pypowsybl.is_config_read()


def print_version() -> None:
    print(_pypowsybl.get_version_table())
