/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredDoubles;
import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredInts;
import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredStrings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ReactiveCapabilityCurveAdder;

/**
 * @author Massimo Ferraro <massimo.ferraro@soft.it>
 */
public class CurveReactiveLimitsDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("element_id"),
            SeriesMetadata.intIndex("num"),
            SeriesMetadata.doubles("p"),
            SeriesMetadata.doubles("min_q"),
            SeriesMetadata.doubles("max_q")
    );

    private static final class CurveReactiveLimitsSeries {

        private final StringSeries elementIds;
        private final IntSeries nums;
        private final DoubleSeries ps;
        private final DoubleSeries minQs;
        private final DoubleSeries maxQs;

        CurveReactiveLimitsSeries(UpdatingDataframe dataframe) {
            this.elementIds = getRequiredStrings(dataframe, "element_id");
            this.nums = getRequiredInts(dataframe, "num");
            this.ps = getRequiredDoubles(dataframe, "p");
            this.minQs = getRequiredDoubles(dataframe, "min_q");
            this.maxQs = getRequiredDoubles(dataframe, "max_q");
        }

        public StringSeries getElementIds() {
            return elementIds;
        }

        public IntSeries getNums() {
            return nums;
        }

        public DoubleSeries getPs() {
            return ps;
        }

        public DoubleSeries getMinQs() {
            return minQs;
        }

        public DoubleSeries getMaxQs() {
            return maxQs;
        }
    }

    private static final class CurvePoint {

        private final Integer num;
        private final Double p;
        private final Double minQ;
        private final Double maxQ;

        CurvePoint(Integer num, Double p, Double minQ, Double maxQ) {
            this.num = num;
            this.p = p;
            this.minQ = minQ;
            this.maxQ = maxQ;
        }

        public Integer getNum() {
            return num;
        }

        public Double getP() {
            return p;
        }

        public Double getMinQ() {
            return minQ;
        }

        public Double getMaxQ() {
            return maxQ;
        }

    }

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryTable = dataframes.get(0);
        CurveReactiveLimitsSeries series = new CurveReactiveLimitsSeries(primaryTable);
        Map<String, SortedSet<CurvePoint>> curvesPoints = new HashMap<>();
        for (int i = 0; i < primaryTable.getRowCount(); i++) {
            String elementId = series.getElementIds().get(i);
            Integer num = series.getNums().get(i);
            Double p = series.getPs().get(i);
            Double minQ = series.getMinQs().get(i);
            Double maxQ = series.getMaxQs().get(i);
            CurvePoint curvePoint = new CurvePoint(num, p, minQ, maxQ);
            if (!curvesPoints.containsKey(elementId)) {
                curvesPoints.put(elementId, new TreeSet<CurvePoint>((a, b) -> a.num - b.num));
            }
            curvesPoints.get(elementId).add(curvePoint);
        }
        curvesPoints.forEach((elementId, curvePoints) -> createLimits(network, elementId, curvePoints));
    }

    private static void createLimits(Network network, String elementId, SortedSet<CurvePoint> curvePoints) {
        Generator generator = network.getGenerator(elementId);
        if (generator == null) {
            throw new PowsyblException("Generator " + elementId + " does not exist.");
        }
        ReactiveCapabilityCurveAdder curveAdder = generator.newReactiveCapabilityCurve();
        for (CurvePoint curvePoint : curvePoints) {
            curveAdder.beginPoint()
                    .setP(curvePoint.getP())
                    .setMaxQ(curvePoint.getMaxQ())
                    .setMinQ(curvePoint.getMinQ())
                    .endPoint();
        }
        curveAdder.add();
    }
}
