/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.Encoder;
import com.powsybl.python.commons.CTypeUtil;

/**
 * Custom appender for python logging
 *
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class CustomAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;

    @Override
    protected void append(final ILoggingEvent e) {
        LoggingCFunctions.LoggerCallback logMessage = (LoggingCFunctions.LoggerCallback) LoggingCFunctions.loggerCallback;

        String message = e.getFormattedMessage();
        IThrowableProxy throwable = e.getThrowableProxy();
        if (throwable != null) {
            message = message + CoreConstants.LINE_SEPARATOR + ThrowableProxyUtil.asString(throwable);
        }

        logMessage.invoke(PyLoggingUtil.logbackLevelToPythonLevel(e.getLevel()), e.getTimeStamp(), CTypeUtil.toCharPtr(e.getLoggerName()), CTypeUtil.toCharPtr(message));
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return this.encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
