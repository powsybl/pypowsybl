/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TwoWindingsTransformerAdder;
import com.powsybl.iidm.network.VoltageLevel;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrowWithBusOrBusbarSectionId;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

public class TwoWindingsTransformerSeries extends AbstractBranchSeries {

    private static final String COULD_NOT_CREATE_TRANSFORMER = "Could not create transformer ";

    private final DoubleSeries ratedU1;
    private final DoubleSeries ratedU2;
    private final DoubleSeries ratedS;
    private final DoubleSeries b;
    private final DoubleSeries g;
    private final DoubleSeries r;
    private final DoubleSeries x;

    TwoWindingsTransformerSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.ratedU1 = dataframe.getDoubles("rated_u1");
        this.ratedU2 = dataframe.getDoubles("rated_u2");
        this.ratedS = dataframe.getDoubles("rated_s");
        this.b = dataframe.getDoubles("b");
        this.g = dataframe.getDoubles("g");
        this.r = dataframe.getDoubles("r");
        this.x = dataframe.getDoubles("x");
    }

    TwoWindingsTransformerAdder create(Network network, int row, boolean throwException) {
        String id = ids.get(row);
        VoltageLevel vl1 = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels1, busOrBusbarSections1, throwException);
        VoltageLevel vl2 = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels2, busOrBusbarSections2, throwException);
        Substation s1 = vl1.getSubstation().orElseThrow(() -> new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + ": no substation."));
        Substation s2 = vl2.getSubstation().orElseThrow(() -> new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + ": no substation."));
        if (s1 != s2) {
            throw new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + ": both voltage ids must be on the same substation");
        }

        var adder = s1.newTwoWindingsTransformer();
        setBranchAttributes(adder, row);
        applyIfPresent(ratedU1, row, adder::setRatedU1);
        applyIfPresent(ratedU2, row, adder::setRatedU2);
        applyIfPresent(ratedS, row, adder::setRatedS);
        applyIfPresent(b, row, adder::setB);
        applyIfPresent(g, row, adder::setG);
        applyIfPresent(r, row, adder::setR);
        applyIfPresent(x, row, adder::setX);
        return adder;
    }
}
