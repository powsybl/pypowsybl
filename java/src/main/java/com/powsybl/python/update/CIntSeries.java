/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.update;

import com.powsybl.dataframe.update.IntSeries;
import org.graalvm.nativeimage.c.type.CIntPointer;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public class CIntSeries implements IntSeries {

    private final CIntPointer values;

    public CIntSeries(CIntPointer values) {
        this.values = values;
    }

    @Override
    public int get(int index) {
        return values.read(index);
    }
}
