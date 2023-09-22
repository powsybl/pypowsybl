#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp  # pylint: disable=protected-access


class Reporter:  # pylint: disable=too-few-public-methods
    def __init__(self, task_key: str = '', default_name: str = ''):
        self._reporter_model_handle = _pp.create_reporter_model(task_key, default_name)

    def __repr__(self) -> str:
        return _pp.print_report(self._reporter_model_handle)

    @property
    def _reporter_model(self) -> _pp.JavaHandle:
        return self._reporter_model_handle

    def to_json(self) -> str:
        return _pp.json_report(self._reporter_model_handle)
