/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class SupplierReport {

    private SupplierReport() {
    }

    public static ReportNode createSupplierReportNode(ReportNode reportNode, String key, String message) {
        return reportNode.newReportNode()
                .withMessageTemplate(key, message)
                .add();
    }
}
