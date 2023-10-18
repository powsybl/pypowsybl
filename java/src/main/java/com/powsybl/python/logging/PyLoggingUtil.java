/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.logging;

import ch.qos.logback.classic.Level;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public final class PyLoggingUtil {

    private PyLoggingUtil() {
    }

    public static int logbackLevelToPythonLevel(Level l) {
        return switch (l.toInt()) {
            case Level.OFF_INT -> 0;
            case Level.ERROR_INT -> 40;
            case Level.WARN_INT -> 30;
            case Level.INFO_INT -> 20;
            case Level.DEBUG_INT -> 10;
            case Level.TRACE_INT, Level.ALL_INT -> 1;
            default -> 0;
        };
    }

    public static Level pythonLevelToLogbackLevel(int l) {
        return switch (l) {
            case 0 -> Level.OFF;
            case 40 -> Level.ERROR;
            case 30 -> Level.WARN;
            case 20 -> Level.INFO;
            case 10 -> Level.DEBUG;
            case 1 -> Level.TRACE;
            default -> Level.OFF;
        };
    }
}
