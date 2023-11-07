/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.commons.reporter.ReporterModelJsonModule;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
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
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class ReportCFunctions {

    private ReportCFunctions() {
    }

    @CEntryPoint(name = "createReporterModel")
    public static ObjectHandle createReporterModel(IsolateThread thread, CCharPointer taskKeyPtr, CCharPointer defaultNamePtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Util.PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                String taskKey = CTypeUtil.toString(taskKeyPtr);
                String defaultName = CTypeUtil.toString(defaultNamePtr);
                ReporterModel reporterModel = new ReporterModel(taskKey, defaultName);
                return ObjectHandles.getGlobal().create(reporterModel);
            }
        });
    }

    @CEntryPoint(name = "printReport")
    public static CCharPointer printReport(IsolateThread thread, ObjectHandle reporterModelHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Util.PointerProvider<CCharPointer>() {
            @Override
            public CCharPointer get() {
                ReporterModel reporterModel = ObjectHandles.getGlobal().get(reporterModelHandle);
                StringWriter reporterOut = new StringWriter();
                reporterModel.export(reporterOut);
                return CTypeUtil.toCharPtr(reporterOut.toString());
            }
        });
    }

    @CEntryPoint(name = "jsonReport")
    public static CCharPointer jsonReport(IsolateThread thread, ObjectHandle reporterModelHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Util.PointerProvider<CCharPointer>() {
            @Override
            public CCharPointer get() {
                ReporterModel reporterModel = ObjectHandles.getGlobal().get(reporterModelHandle);
                StringWriter reporterOut = new StringWriter();
                ObjectMapper objectMapper = new ObjectMapper().registerModule(new ReporterModelJsonModule());
                try {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(reporterOut, reporterModel);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return CTypeUtil.toCharPtr(reporterOut.toString());
            }
        });
    }
}
