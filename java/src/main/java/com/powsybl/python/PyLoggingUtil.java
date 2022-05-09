/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import ch.qos.logback.classic.Level;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public final class PyLoggingUtil {

    private PyLoggingUtil() {
    }

    public static int logbackLevelToPythonLevel(Level l) {
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
                return 1;
            case Level.ALL_INT:
                return 60;
            default:
                return 0;
        }
    }

    public static Level pythonLevelToLogbackLevel(int l) {
        switch (l) {
            case 0:
                return Level.OFF;
            case 40:
                return Level.ERROR;
            case 30:
                return Level.WARN;
            case 20:
                return Level.INFO;
            case 10:
                return Level.DEBUG;
            case 1:
                return Level.TRACE;
            case 60:
                return Level.ALL;
            default:
                return Level.OFF;
        }
    }
}
