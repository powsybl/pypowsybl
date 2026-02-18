/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dataframe;

import com.powsybl.dataframe.update.DoubleSeries;
import org.graalvm.nativeimage.c.type.CDoublePointer;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */

public class CDoubleSeries implements DoubleSeries {

    private final CDoublePointer values;

    public CDoubleSeries(CDoublePointer values) {
        this.values = values;
    }

    @Override
    public double get(int index) {
        return values.read(index);
    }
}
