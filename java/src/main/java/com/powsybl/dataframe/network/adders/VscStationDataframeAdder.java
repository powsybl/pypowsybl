/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.VscConverterStationAdder;
import com.powsybl.python.network.NetworkUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrowWithBusOrBusbarSectionId;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class VscStationDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.doubles("target_q"),
            SeriesMetadata.doubles("loss_factor"),
            SeriesMetadata.booleans("voltage_regulator_on"),
            SeriesMetadata.strings("regulating_element_id")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class VscStationSeries extends InjectionSeries {

        private final StringSeries voltageLevels;
        private final DoubleSeries lossFactors;
        private final DoubleSeries targetV;
        private final DoubleSeries targetQ;
        private final IntSeries voltageRegulatorOn;
        private final StringSeries busOrBusbarSections;
        private final StringSeries regulatingElements;

        VscStationSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            this.lossFactors = dataframe.getDoubles("loss_factor");
            this.targetV = dataframe.getDoubles("target_v");
            this.targetQ = dataframe.getDoubles("target_q");
            this.voltageRegulatorOn = dataframe.getInts("voltage_regulator_on");
            this.busOrBusbarSections = dataframe.getStrings("bus_or_busbar_section_id");
            this.regulatingElements = dataframe.getStrings("regulating_element_id");
        }

        Optional<VscConverterStationAdder> createAdder(Network network, int row, boolean throwException) {
            Optional<VoltageLevel> vl = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels, busOrBusbarSections, throwException);
            if (vl.isPresent()) {
                VscConverterStationAdder adder = vl.get().newVscConverterStation();
                setInjectionAttributes(adder, row);
                applyIfPresent(lossFactors, row, f -> adder.setLossFactor((float) f));
                applyIfPresent(targetV, row, adder::setVoltageSetpoint);
                applyIfPresent(targetQ, row, adder::setReactivePowerSetpoint);
                applyBooleanIfPresent(voltageRegulatorOn, row, adder::setVoltageRegulatorOn);
                applyIfPresent(regulatingElements, row, elementId -> NetworkUtil
                        .setRegulatingTerminal(adder::setRegulatingTerminal, network, elementId));
                return Optional.of(adder);
            }
            return Optional.empty();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe, AdditionStrategy additionStrategy, boolean throwException, ReportNode reportNode) {
        VscStationSeries series = new VscStationSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            Optional<VscConverterStationAdder> adder = series.createAdder(network, row, throwException);
            if (adder.isPresent()) {
                additionStrategy.add(network, dataframe, adder.get(), row, throwException, reportNode);
            }
        }
    }
}
