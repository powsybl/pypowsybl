#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from pypowsybl._pypowsybl import create_reporter_model, print_report, JavaHandle


class Reporter:
    def __init__(self, task_key: str = '', default_name: str = ''):
        self.reporterModelHandle = create_reporter_model(task_key, default_name)

    def __repr__(self) -> str:
        return print_report(self.reporterModelHandle)

    @property
    def reporterModel(self) -> JavaHandle:
        return self.reporterModelHandle