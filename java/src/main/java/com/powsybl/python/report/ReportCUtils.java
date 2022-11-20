/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.report;

import com.powsybl.commons.reporter.Reporter;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class ReportCUtils {

    private ReportCUtils() {
    }

    public static Reporter getReporter(ObjectHandle reporterHandle) {
        Reporter reporter = ObjectHandles.getGlobal().get(reporterHandle);
        return reporter != null ? reporter : Reporter.NO_OP;
    }
}
