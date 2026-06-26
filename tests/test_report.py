#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl.report import ReportNode

def test_task_key():
    report_node = ReportNode("pypowsybl.dynasim.pypowsyblDynamicModels")
    assert "PyPowsybl Dynamic Models Supplier" in str(report_node)

def test_task_key_not_found():
    report_node = ReportNode("Test")
    assert "Test" in str(report_node)
