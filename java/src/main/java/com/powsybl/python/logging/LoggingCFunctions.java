/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.logging;

import ch.qos.logback.classic.Logger;
import com.powsybl.python.commons.ByteBufferInputStream;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.slf4j.LoggerFactory;

import static com.powsybl.python.commons.Util.doCatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;

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

    @CEntryPoint(name = "bytesIOtoJava")
    public static void bytesIOtoJava(IsolateThread thread, PointerBase data, int size, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            ByteBuffer buffer = CTypeConversion.asByteBuffer(data, size);
            System.out.println("From JAVA : " + buffer.get(0));
            System.out.println("From JAVA : " + buffer.get(1));
            System.out.println("From JAVA : " + buffer.get(2));
            System.out.println("From JAVA : " + buffer.get(3));

            byte[] directBuffer;
            InputStream stream;
            if (buffer.hasArray()) {
                System.out.println("Buffer has array");
                directBuffer = buffer.array();
                stream = new ByteArrayInputStream(directBuffer);
            } else {
                System.out.println("Buffer does not has an array");
                stream = new ByteBufferInputStream(buffer);
            }

            try {
                int count = 0;
                while (stream.available() > 0) {
                    stream.read();
                    //System.out.println("Byte " + count + " : " + stream.read());
                    count++;
                }
                System.out.println("Has read " + count + " bytes");
            } catch (IOException e) {
                System.out.println("Exception while streaming");
            }
        });
    }
}
