/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.ThreeWindingsTransformerAdder;
import com.powsybl.iidm.network.VoltageLevel;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrow;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class ThreeWindingsTransformerSeries extends IdentifiableSeries {
    private static final String COULD_NOT_CREATE_TRANSFORMER = "Could not create transformer ";
    private static final String NO_SUBSTATION = ": no substation.";

    private final DoubleSeries ratedU0;
    private final DoubleSeries r1;
    private final DoubleSeries x1;
    private final DoubleSeries g1;
    private final DoubleSeries b1;
    private final DoubleSeries ratedU1;
    private final DoubleSeries ratedS1;
    private final DoubleSeries r2;
    private final DoubleSeries x2;
    private final DoubleSeries g2;
    private final DoubleSeries b2;
    private final DoubleSeries ratedU2;
    private final DoubleSeries ratedS2;
    private final DoubleSeries r3;
    private final DoubleSeries x3;
    private final DoubleSeries g3;
    private final DoubleSeries b3;
    private final DoubleSeries ratedU3;
    private final DoubleSeries ratedS3;

    private final StringSeries voltageLevels1;
    private final StringSeries connectableBuses1;
    private final StringSeries buses1;
    private final IntSeries nodes1;
    private final StringSeries voltageLevels2;
    private final StringSeries connectableBuses2;
    private final StringSeries buses2;
    private final IntSeries nodes2;
    private final StringSeries voltageLevels3;
    private final StringSeries connectableBuses3;
    private final StringSeries buses3;
    private final IntSeries nodes3;

    ThreeWindingsTransformerSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.ratedU0 = dataframe.getDoubles("rated_u0");
        this.r1 = dataframe.getDoubles("r1");
        this.x1 = dataframe.getDoubles("x1");
        this.g1 = dataframe.getDoubles("g1");
        this.b1 = dataframe.getDoubles("b1");
        this.ratedU1 = dataframe.getDoubles("rated_u1");
        this.ratedS1 = dataframe.getDoubles("rated_s1");
        this.r2 = dataframe.getDoubles("r2");
        this.x2 = dataframe.getDoubles("x2");
        this.g2 = dataframe.getDoubles("g2");
        this.b2 = dataframe.getDoubles("b2");
        this.ratedU2 = dataframe.getDoubles("rated_u2");
        this.ratedS2 = dataframe.getDoubles("rated_s2");
        this.r3 = dataframe.getDoubles("r3");
        this.x3 = dataframe.getDoubles("x3");
        this.g3 = dataframe.getDoubles("g3");
        this.b3 = dataframe.getDoubles("b3");
        this.ratedU3 = dataframe.getDoubles("rated_u3");
        this.ratedS3 = dataframe.getDoubles("rated_s3");
        this.voltageLevels1 = dataframe.getStrings("voltage_level1_id");
        this.connectableBuses1 = dataframe.getStrings("connectable_bus1_id");
        this.buses1 = dataframe.getStrings("bus1_id");
        this.nodes1 = dataframe.getInts("node1");
        this.voltageLevels2 = dataframe.getStrings("voltage_level2_id");
        this.connectableBuses2 = dataframe.getStrings("connectable_bus2_id");
        this.buses2 = dataframe.getStrings("bus2_id");
        this.nodes2 = dataframe.getInts("node2");
        this.voltageLevels3 = dataframe.getStrings("voltage_level3_id");
        this.connectableBuses3 = dataframe.getStrings("connectable_bus3_id");
        this.buses3 = dataframe.getStrings("bus3_id");
        this.nodes3 = dataframe.getInts("node3");
    }

    ThreeWindingsTransformerAdder create(Network network, int row) {
        String id = ids.get(row);
        VoltageLevel vl1 = getVoltageLevelOrThrow(network, voltageLevels1.get(row));
        VoltageLevel vl2 = getVoltageLevelOrThrow(network, voltageLevels2.get(row));
        VoltageLevel vl3 = getVoltageLevelOrThrow(network, voltageLevels3.get(row));

        Substation s1 = vl1.getSubstation().orElseThrow(() -> new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + NO_SUBSTATION));
        Substation s2 = vl2.getSubstation().orElseThrow(() -> new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + NO_SUBSTATION));
        Substation s3 = vl3.getSubstation().orElseThrow(() -> new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + NO_SUBSTATION));
        if (!(s1 == s2 && s1 == s3)) {
            throw new PowsyblException(COULD_NOT_CREATE_TRANSFORMER + id + ": both voltage ids must be on the same substation");
        }
        var adder = s1.newThreeWindingsTransformer();
        setIdentifiableAttributes(adder, row);
        applyIfPresent(ratedU0, row, adder::setRatedU0);
        ThreeWindingsTransformerAdder.LegAdder leg1 = adder.newLeg1();
        ThreeWindingsTransformerAdder.LegAdder leg2 = adder.newLeg2();
        ThreeWindingsTransformerAdder.LegAdder leg3 = adder.newLeg3();
        createLeg(row, leg1, voltageLevels1, connectableBuses1, buses1, nodes1, r1, x1, g1, b1, ratedU1, ratedS1);
        createLeg(row, leg2, voltageLevels2, connectableBuses2, buses2, nodes2, r2, x2, g2, b2, ratedU2, ratedS2);
        createLeg(row, leg3, voltageLevels3, connectableBuses3, buses3, nodes3, r3, x3, g3, b3, ratedU3, ratedS3);
        leg1.add();
        leg2.add();
        leg3.add();

        return adder;
    }

    private void createLeg(int row, ThreeWindingsTransformerAdder.LegAdder leg, StringSeries voltageLevels, StringSeries connectableBuses,
                           StringSeries buses, IntSeries nodes, DoubleSeries r, DoubleSeries x, DoubleSeries g, DoubleSeries b,
                           DoubleSeries ratedU, DoubleSeries ratedS) {
        applyIfPresent(voltageLevels, row, leg::setVoltageLevel);
        applyIfPresent(connectableBuses, row, leg::setConnectableBus);
        applyIfPresent(buses, row, leg::setBus);
        applyIfPresent(nodes, row, leg::setNode);
        applyIfPresent(r, row, leg::setR);
        applyIfPresent(x, row, leg::setX);
        applyIfPresent(g, row, leg::setG);
        applyIfPresent(b, row, leg::setB);
        applyIfPresent(ratedU, row, leg::setRatedU);
        applyIfPresent(ratedS, row, leg::setRatedS);
    }
}
