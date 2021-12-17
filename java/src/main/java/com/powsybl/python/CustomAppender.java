package com.powsybl.python;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.StandardCharsets;

public class CustomAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;

    @Override
    protected void append(final ILoggingEvent e) {
        PyPowsyblApiLib.Callback logMessage = (PyPowsyblApiLib.Callback) PyPowsyblApiLib.loggerCallback;
        logMessage.invoke(logbackLevelToPythonLevel(e.getLevel()), CTypeUtil.toCharPtr(new String(this.getEncoder().encode(e), StandardCharsets.UTF_8)));
    }

    private int logbackLevelToPythonLevel(Level l) {
        switch (l.toInt()) {
            case Level.OFF_INT:
                return 0;
            case Level.ERROR_INT:
                return 40;
            case Level.WARN_INT:
                return 30;
            case Level.INFO_INT:
                return 20;
            case Level.DEBUG_INT:
                return 10;
            case Level.TRACE_INT:
                return 10;
            case Level.ALL_INT:
                return 60;
            default:
                return 0;
        }
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return this.encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
