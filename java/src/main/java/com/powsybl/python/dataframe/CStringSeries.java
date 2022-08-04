/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.dataframe;

import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.python.commons.CTypeUtil;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public class CStringSeries implements StringSeries {

    private final CCharPointerPointer values;

    public CStringSeries(CCharPointerPointer values) {
        this.values = values;
    }

    @Override
    public String get(int index) {
        return CTypeUtil.toString(values.read(index));
    }
}
