/**
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.AreaAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Damien Jeandemange {@literal <damien.jeandemange@artelys.com>}
 */
public class AreaDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("area_type"),
            SeriesMetadata.doubles("interchange_target")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class AreaSeries extends IdentifiableSeries {

        private final StringSeries areaTypes;
        private final DoubleSeries interchangeTargets;

        AreaSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.areaTypes = dataframe.getStrings("area_type");
            this.interchangeTargets = dataframe.getDoubles("interchange_target");
        }

        AreaAdder create(Network network, int row) {
            AreaAdder adder = network.newArea();
            setIdentifiableAttributes(adder, row);
            applyIfPresent(areaTypes, row, adder::setAreaType);
            applyIfPresent(interchangeTargets, row, adder::setInterchangeTarget);
            return adder;
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe, AdditionStrategy additionStrategy, boolean throwException, ReportNode reportNode) {
        AreaSeries series = new AreaSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            AreaAdder adder = series.create(network, row);
            adder.add();
        }
    }
}
