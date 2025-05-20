/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeJsonModule;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
@CContext(Directives.class)
public final class ReportCFunctions {

    private ReportCFunctions() {
    }

    @CEntryPoint(name = "createReportNode")
    public static ObjectHandle createReportNode(IsolateThread thread, CCharPointer taskKeyPtr, CCharPointer defaultNamePtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String taskKey = CTypeUtil.toString(taskKeyPtr);
            String defaultName = CTypeUtil.toString(defaultNamePtr);
            ReportNode reportNode = ReportNode.newRootReportNode()
                .withMessageTemplate(taskKey, defaultName)
                .build();
            return ObjectHandles.getGlobal().create(reportNode);
        });
    }

    @CEntryPoint(name = "printReport")
    public static CCharPointer printReport(IsolateThread thread, ObjectHandle reportNodeHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            StringWriter reportNodeOut = new StringWriter();
            reportNode.print(reportNodeOut);
            return CTypeUtil.toCharPtr(reportNodeOut.toString());
        });
    }

    @CEntryPoint(name = "jsonReport")
    public static CCharPointer jsonReport(IsolateThread thread, ObjectHandle reportNodeHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            StringWriter reportNodeOut = new StringWriter();
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new ReportNodeJsonModule());
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportNodeOut, reportNode);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return CTypeUtil.toCharPtr(reportNodeOut.toString());
        });
    }
}
