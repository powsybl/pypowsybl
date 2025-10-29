/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.extensions.VoltagePerReactivePowerControlAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Hugo Kulesza {@literal <hugo.kulesza at rte-france.com>}
 */
public class VoltagePerReactivePowerControlDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("slope")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class VoltagePerReactivePowerControlSeries {
        private final StringSeries id;
        private final DoubleSeries slope;

        VoltagePerReactivePowerControlSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.slope = dataframe.getDoubles("slope");
        }

        void create(Network network, int row) {
            String id = this.id.get(row);
            StaticVarCompensator svc = network.getStaticVarCompensator(id);
            if (svc == null) {
                throw new PowsyblException("Invalid static var compensator id : could not find " + id);
            }
            VoltagePerReactivePowerControlAdder adder = svc.newExtension(VoltagePerReactivePowerControlAdder.class);
            SeriesUtils.applyIfPresent(slope, row, adder::withSlope);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        VoltagePerReactivePowerControlSeries series = new VoltagePerReactivePowerControlSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
