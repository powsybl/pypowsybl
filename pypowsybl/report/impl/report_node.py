#
# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pypowsybl._pypowsybl as _pp  # pylint: disable=protected-access


class ReportNode:  # pylint: disable=too-few-public-methods
    def __init__(self, task_key: str = '', default_name: str = ''):
        self._report_node_handle = _pp.create_report_node(task_key, default_name)

    def __repr__(self) -> str:
        return _pp.print_report(self._report_node_handle)

    @property
    def _report_node(self) -> _pp.JavaHandle:
        return self._report_node_handle

    def to_json(self) -> str:
        return _pp.json_report(self._report_node_handle)
