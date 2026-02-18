# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl import _pypowsybl
from .rao import Rao
from logging import LogRecord
import re

def create_rao() -> Rao:
    """ Creates a rao objet, which can be used to run a remedial action optimisation on a network
    Returns:
        A rao object
    """
    return Rao(_pypowsybl.create_rao())

class RaoLogFilter:
    def filter(self, record: LogRecord) -> bool:
        # Filter and keep only logs from open rao package
        if re.search("com\\.powsybl\\.openrao.*", getattr(record, 'java_logger_name')):
            return True
        else:
            return False
