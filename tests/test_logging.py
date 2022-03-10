#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import unittest

import pypowsybl as pp
import pypowsybl.loadflow as lf
import logging
import queue
from logging import handlers


class LoggingTestCase(unittest.TestCase):

    def setUp(self):
        pp.set_config_read(False)

    def test_loglevel_lf(self):

        #Build a PyPow logger to handle logs and install it
        pypowLogger = logging.getLogger("PyPow")
        pp.set_logger(pypowLogger)
        pypowLogger.setLevel(level=logging.NOTSET)

        #Install a handler with a simple queue on logger for validation
        q = queue.Queue()
        handler = handlers.QueueHandler(q)
        pypowLogger.addHandler(handler)

        #Logging is off, not log should be in the queue
        n = pp.network.create_ieee14()
        lf.run_ac(n)
        self.assertEqual(0, q.qsize())

        #Set log level to INFO and re run the same load flow
        pypowLogger.setLevel(level=logging.INFO)
        lf.run_ac(n)

        #Now some logs should be available
        self.assertTrue(q.qsize() > 0)
        self.assertTrue(self.find_in_queue(q, 'Start AC loadflow on network'))
        self.assertTrue(self.find_in_queue(q, 'Load flow ran in'))

    def find_in_queue(self, q, str):
        for i in range(len(q.queue)):
            if str in q.queue[i].msg :
                return True
        return False

if __name__ == '__main__':
    unittest.main()
