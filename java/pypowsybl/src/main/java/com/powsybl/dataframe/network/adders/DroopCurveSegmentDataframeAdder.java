/**
 * Copyright (c) 2026, SuperGrid Institute (https://www.supergrid-institute.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.*;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getIdentifiableOrThrow;
import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredDoubles;
import static com.powsybl.dataframe.network.adders.SeriesUtils.getRequiredStrings;

/**
 * @author Landry Huet {@literal <landry.huet at supergrid-institute.com>}
 */
public class DroopCurveSegmentDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("min_v"),
            SeriesMetadata.doubles("max_v"),
            SeriesMetadata.doubles("k")
    );

    private static final class DroopCurveSegmentSeries {

        private final StringSeries elementIds;
        private final DoubleSeries minVs;
        private final DoubleSeries maxVs;
        private final DoubleSeries ks;

        DroopCurveSegmentSeries(UpdatingDataframe dataframe) {
            this.elementIds = getRequiredStrings(dataframe, "id");
            this.minVs = getRequiredDoubles(dataframe, "min_v");
            this.maxVs = getRequiredDoubles(dataframe, "max_v");
            this.ks = getRequiredDoubles(dataframe, "k");
        }
    }

    private record SegmentValue(double minV, double maxV, double k) {
    }

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryTable = dataframes.get(0);
        DroopCurveSegmentSeries series = new DroopCurveSegmentSeries(primaryTable);
        Map<String, List<SegmentValue>> curveSegments = new LinkedHashMap<>();
        for (int i = 0; i < primaryTable.getRowCount(); i++) {
            String elementId = series.elementIds.get(i);
            SegmentValue segment = new SegmentValue(series.minVs.get(i), series.maxVs.get(i), series.ks.get(i));
            curveSegments.computeIfAbsent(elementId, id -> new ArrayList<>()).add(segment);
        }
        curveSegments.forEach((elementId, segments) -> createDroopCurve(network, elementId, segments));
    }

    private static void createDroopCurve(Network network, String elementId, List<SegmentValue> segments) {
        Identifiable<?> identifiable = getIdentifiableOrThrow(network, elementId);
        if (identifiable instanceof VoltageSourceConverter converter) {
            DroopCurveAdder curveAdder = converter.newDroopCurve();
            for (SegmentValue segment : segments) {
                curveAdder.beginSegment()
                        .setMinV(segment.minV())
                        .setMaxV(segment.maxV())
                        .setK(segment.k())
                        .endSegment();
            }
            curveAdder.add();
        } else {
            throw new PowsyblException("Element " + elementId + " is not a voltage source converter.");
        }
    }
}
