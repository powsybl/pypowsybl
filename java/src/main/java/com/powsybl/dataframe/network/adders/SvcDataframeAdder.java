/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class SvcDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("b_max"),
            SeriesMetadata.doubles("b_min"),
            SeriesMetadata.strings("regulation_mode"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.doubles("target_q")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class StaticVarCompensatorSeries extends InjectionSeries {

        private final StringSeries voltageLevels;
        private final DoubleSeries bMin;
        private final DoubleSeries bMax;
        private final StringSeries regulationModes;
        private final DoubleSeries targetV;
        private final DoubleSeries targetQ;

        StaticVarCompensatorSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            if (voltageLevels == null) {
                throw new PowsyblException("voltage_level_id is missing");
            }
            this.bMin = dataframe.getDoubles("b_min");
            this.bMax = dataframe.getDoubles("b_max");
            this.targetQ = dataframe.getDoubles("target_q");
            this.targetV = dataframe.getDoubles("target_v");
            this.regulationModes = dataframe.getStrings("regulation_mode");
        }

        void create(Network network, int row) {
            StaticVarCompensatorAdder adder = network.getVoltageLevel(voltageLevels.get(row))
                    .newStaticVarCompensator();
            setInjectionAttributes(adder, row);
            applyIfPresent(bMin, row, adder::setBmin);
            applyIfPresent(bMax, row, adder::setBmax);
            applyIfPresent(targetQ, row, adder::setReactivePowerSetpoint);
            applyIfPresent(targetV, row, adder::setVoltageSetpoint);
            applyIfPresent(regulationModes, row, StaticVarCompensator.RegulationMode.class, adder::setRegulationMode);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        StaticVarCompensatorSeries series = new StaticVarCompensatorSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
