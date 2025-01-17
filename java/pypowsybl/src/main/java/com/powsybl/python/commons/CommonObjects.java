/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.commons;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;

/**
 * Manages common runtime objects, typically library-wide singletons.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
public final class CommonObjects {

    private static ComputationManager computationManager;

    private CommonObjects() {
    }

    public static synchronized ComputationManager getComputationManager() {
        if (computationManager == null) {
            computationManager = LocalComputationManager.getDefault();
        }
        return computationManager;
    }

    public static synchronized void close() {
        if (computationManager != null) {
            computationManager.close();
        }
    }
}
