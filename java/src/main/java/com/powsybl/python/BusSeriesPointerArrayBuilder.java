/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.Bus;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class BusSeriesPointerArrayBuilder extends SeriesPointerArrayBuilder<Bus> {

    BusSeriesPointerArrayBuilder(List<Bus> elements) {
        super(elements);
    }

    BusSeriesPointerArrayBuilder convert() {
        super.addStringSeries("id", Bus::getId)
                .addDoubleSeries("v_mag", Bus::getV)
                .addDoubleSeries("v_angle", Bus::getAngle);
        return this;
    }
}
