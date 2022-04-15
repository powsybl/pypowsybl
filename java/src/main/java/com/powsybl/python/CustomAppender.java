/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

/**
 * Custom appender for python logging
 *
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class CustomAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;

    @Override
    protected void append(final ILoggingEvent e) {
        PyPowsyblApiLib.Callback logMessage = (PyPowsyblApiLib.Callback) PyPowsyblApiLib.loggerCallback;
        logMessage.invoke(PyLoggingUtil.logbackLevelToPythonLevel(e.getLevel()), e.getTimeStamp(),
                CTypeUtil.toCharPtr(e.getLoggerName()), CTypeUtil.toCharPtr(e.getFormattedMessage()));
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return this.encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
