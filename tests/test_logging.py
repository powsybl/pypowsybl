#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pytest

import pypowsybl as pp
import pypowsybl.loadflow as lf
import logging
import queue
from logging import handlers


@pytest.fixture(autouse=True)
def setUp():
    pp.set_config_read(False)


def test_loglevel_lf():
    # Retrieve the default powsybl logger
    pypowLogger = logging.getLogger('powsybl')
    pypowLogger.setLevel(level=logging.NOTSET)

    # Install a handler with a simple queue on logger for validation
    q = queue.Queue()
    handler = handlers.QueueHandler(q)
    pypowLogger.addHandler(handler)

    # Logging is off, not log should be in the queue
    n = pp.network.create_ieee14()
    lf.run_ac(n)
    assert 0 == q.qsize()

    # Set log level to INFO and re run the same load flow
    pypowLogger.setLevel(level=logging.INFO)
    lf.run_ac(n)

    # Now some logs should be available
    assert q.qsize() > 0
    assert find_in_queue(q, 'Start AC loadflow on network')
    assert find_in_queue(q, 'Load flow ran in')


def find_in_queue(q, str):
    for i in range(len(q.queue)):
        if str in q.queue[i].msg:
            return True
    return False
