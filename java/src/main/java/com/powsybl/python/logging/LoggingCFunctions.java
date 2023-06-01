/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.logging;

import ch.qos.logback.classic.Logger;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.slf4j.LoggerFactory;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * C functions related to logging.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class LoggingCFunctions {

    public static CFunctionPointer loggerCallback;

    private LoggingCFunctions() {
    }

    interface LoggerCallback extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(int level, long timestamp, CCharPointer loggerName, CCharPointer message);
    }

    @CEntryPoint(name = "setupLoggerCallback")
    public static void setupLoggerCallback(IsolateThread thread, LoggerCallback fpointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            loggerCallback = fpointer;
        });
    }

    @CEntryPoint(name = "setLogLevel")
    public static void setLogLevel(IsolateThread thread, int logLevel, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(PyLoggingUtil.pythonLevelToLogbackLevel(logLevel));
        });
    }
}
