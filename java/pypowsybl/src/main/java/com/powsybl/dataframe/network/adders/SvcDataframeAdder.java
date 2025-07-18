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
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.StaticVarCompensatorAdder;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.python.network.NetworkUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrowWithBusOrBusbarSectionId;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
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
            SeriesMetadata.booleans("regulating"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.doubles("target_q"),
            SeriesMetadata.strings("regulating_element_id")
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
        private final IntSeries regulating;
        private final DoubleSeries targetV;
        private final DoubleSeries targetQ;
        private final StringSeries busOrBusbarSections;
        private final StringSeries regulatingElements;

        StaticVarCompensatorSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            this.bMin = dataframe.getDoubles("b_min");
            this.bMax = dataframe.getDoubles("b_max");
            this.targetQ = dataframe.getDoubles("target_q");
            this.targetV = dataframe.getDoubles("target_v");
            this.regulationModes = dataframe.getStrings("regulation_mode");
            this.regulating = dataframe.getInts("regulating");
            this.busOrBusbarSections = dataframe.getStrings("bus_or_busbar_section_id");
            this.regulatingElements = dataframe.getStrings("regulating_element_id");
        }

        Optional<StaticVarCompensatorAdder> createAdder(Network network, int row, boolean throwException) {
            Optional<VoltageLevel> vl = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels, busOrBusbarSections, throwException);
            if (vl.isPresent()) {
                StaticVarCompensatorAdder adder = vl.get().newStaticVarCompensator();
                setInjectionAttributes(adder, row);
                applyIfPresent(bMin, row, adder::setBmin);
                applyIfPresent(bMax, row, adder::setBmax);
                applyIfPresent(targetQ, row, adder::setReactivePowerSetpoint);
                applyIfPresent(targetV, row, adder::setVoltageSetpoint);
                applyIfPresent(regulationModes, row, StaticVarCompensator.RegulationMode.class, adder::setRegulationMode);
                applyBooleanIfPresent(regulating, row, adder::setRegulating);
                applyIfPresent(regulatingElements, row, elementId -> NetworkUtil
                        .setRegulatingTerminal(adder::setRegulatingTerminal, network, elementId));
                return Optional.of(adder);
            }
            return Optional.empty();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe, AdditionStrategy addition, boolean throwException, ReportNode reportNode) {
        StaticVarCompensatorSeries series = new StaticVarCompensatorSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            Optional<StaticVarCompensatorAdder> adder = series.createAdder(network, row, throwException);
            if (adder.isPresent()) {
                addition.add(network, dataframe, adder.get(), row, throwException, reportNode);
            }
        }
    }
}
