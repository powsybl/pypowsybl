/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.entsoe.util.XnodeAdder;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class XnodeDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("code")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class XnodeSeries {

        private final StringSeries id;
        private final StringSeries code;

        XnodeSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.code = dataframe.getStrings("code");
        }

        void create(Network network, int row) {
            String id = this.id.get(row);
            DanglingLine l = network.getDanglingLine(id);
            if (l == null) {
                throw new PowsyblException("Invalid dangling line id : could not find " + id);
            }
            var adder = l.newExtension(XnodeAdder.class);
            applyIfPresent(code, row, adder::withCode);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        XnodeSeries series = new XnodeSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
