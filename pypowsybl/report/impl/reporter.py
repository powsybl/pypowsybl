#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings

import pypowsybl._pypowsybl as _pp  # pylint: disable=protected-access

from pypowsybl.report import ReportNode

DEPRECATED_REPORTER_WARNING = "Use of deprecated attribute reporter. Use report_node instead."

class Reporter(ReportNode):  # pylint: disable=too-few-public-methods
    def __init__(self, task_key: str = '', default_name: str = ''):
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        ReportNode.__init__(self, task_key, default_name)

    def __repr__(self) -> str:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        return ReportNode.__repr__(self)

    @property
    def _reporter_model(self) -> _pp.JavaHandle:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        return self._report_node_handle

    def to_json(self) -> str:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        return ReportNode.to_json(self)
