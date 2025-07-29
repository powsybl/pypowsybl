/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.CreateFeederBay;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.network.DanglingLineAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ConnectablePosition;

import java.util.*;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrowWithBusOrBusbarSectionId;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
public class DanglingLineDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
        SeriesMetadata.stringIndex("id"),
        SeriesMetadata.strings("voltage_level_id"),
        SeriesMetadata.strings("bus_id"),
        SeriesMetadata.strings("connectable_bus_id"),
        SeriesMetadata.ints("node"),
        SeriesMetadata.strings("name"),
        SeriesMetadata.doubles("p0"),
        SeriesMetadata.doubles("q0"),
        SeriesMetadata.doubles("r"),
        SeriesMetadata.doubles("x"),
        SeriesMetadata.doubles("g"),
        SeriesMetadata.doubles("b"),
        SeriesMetadata.strings("pairing_key")
    );

    private static final List<SeriesMetadata> GENERATION_METADATA = List.of(
        SeriesMetadata.stringIndex("id"),
        SeriesMetadata.doubles("min_p"),
        SeriesMetadata.doubles("max_p"),
        SeriesMetadata.doubles("target_p"),
        SeriesMetadata.doubles("target_q"),
        SeriesMetadata.doubles("target_v"),
        SeriesMetadata.booleans("voltage_regulator_on")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(METADATA, GENERATION_METADATA);
    }

    private static class DanglingLineSeries extends InjectionSeries {

        private final StringSeries voltageLevels;
        private final DoubleSeries p0;
        private final DoubleSeries q0;
        private final DoubleSeries r;
        private final DoubleSeries x;
        private final DoubleSeries g;
        private final DoubleSeries b;
        private final StringSeries busOrBusbarSections;
        private final StringSeries pairingKey;

        private final Map<String, Integer> generationIndexes;
        private final DoubleSeries minP;
        private final DoubleSeries maxP;
        private final DoubleSeries targetP;
        private final DoubleSeries targetQ;
        private final DoubleSeries targetV;
        private final IntSeries voltageRegulatorOn;

        DanglingLineSeries(UpdatingDataframe dataframe, UpdatingDataframe generationDataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            this.p0 = dataframe.getDoubles("p0");
            this.q0 = dataframe.getDoubles("q0");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
            this.g = dataframe.getDoubles("g");
            this.b = dataframe.getDoubles("b");
            this.busOrBusbarSections = dataframe.getStrings("bus_or_busbar_section_id");
            this.pairingKey = dataframe.getStrings("pairing_key");

            if (generationDataframe != null && generationDataframe.getRowCount() > 0) {
                this.minP = generationDataframe.getDoubles("min_p");
                this.maxP = generationDataframe.getDoubles("max_p");
                this.targetP = generationDataframe.getDoubles("target_p");
                this.targetQ = generationDataframe.getDoubles("target_q");
                this.targetV = generationDataframe.getDoubles("target_v");
                this.voltageRegulatorOn = generationDataframe.getInts("voltage_regulator_on");
                this.generationIndexes = getGenerationIndexes(generationDataframe);
            } else {
                this.minP = null;
                this.maxP = null;
                this.targetP = null;
                this.targetQ = null;
                this.targetV = null;
                this.voltageRegulatorOn = null;
                this.generationIndexes = null;
            }
        }

        Optional<DanglingLineAdder> createAdder(Network network, int row, boolean throwException) {
            Optional<VoltageLevel> vl = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels,
                busOrBusbarSections, throwException);
            if (vl.isPresent()) {
                DanglingLineAdder adder = vl.get().newDanglingLine();
                setInjectionAttributes(adder, row);
                applyIfPresent(p0, row, adder::setP0);
                applyIfPresent(q0, row, adder::setQ0);
                applyIfPresent(r, row, adder::setR);
                applyIfPresent(x, row, adder::setX);
                applyIfPresent(g, row, adder::setG);
                applyIfPresent(b, row, adder::setB);
                applyIfPresent(pairingKey, row, adder::setPairingKey);
                addGenerationIfPresent(adder, row);
                return Optional.of(adder);
            } else {
                return Optional.empty();
            }
        }

        private void addGenerationIfPresent(DanglingLineAdder adder, int row) {
            if (generationIndexes == null) {
                return;
            }
            String id = ids.get(row);
            Integer generationRow = generationIndexes.get(id);
            if (generationRow != null) {
                DanglingLineAdder.GenerationAdder genAdder = adder.newGeneration();
                applyIfPresent(minP, generationRow, genAdder::setMinP);
                applyIfPresent(maxP, generationRow, genAdder::setMaxP);
                applyIfPresent(targetP, generationRow, genAdder::setTargetP);
                applyIfPresent(targetQ, generationRow, genAdder::setTargetQ);
                applyIfPresent(targetV, generationRow, genAdder::setTargetV);
                applyBooleanIfPresent(voltageRegulatorOn, generationRow, genAdder::setVoltageRegulationOn);
                genAdder.add();
            }
        }

        /**
         * Mapping shunt ID --> index of line in dataframe
         */
        private static Map<String, Integer> getGenerationIndexes(UpdatingDataframe generationDf) {
            StringSeries ids = generationDf.getStrings("id");
            if (ids == null) {
                throw new PowsyblException("Dangling line generation dataframe: id is not set");
            }
            Map<String, Integer> indexes = new HashMap<>();
            for (int generationIndex = 0; generationIndex < generationDf.getRowCount(); generationIndex++) {
                String danglingLineId = ids.get(generationIndex);
                indexes.put(danglingLineId, generationIndex);
            }
            return indexes;
        }

        void create(Network network, int row, boolean throwException) {
            Optional<DanglingLineAdder> adder = createAdder(network, row, throwException);
            adder.ifPresent(DanglingLineAdder::add);
        }

        void createWithBay(Network network, int row, UpdatingDataframe primaryDataframe, boolean throwException, ReportNode reportNode) {
            Optional<DanglingLineAdder> adder = createAdder(network, row, throwException);
            adder.ifPresent(presentAdder -> addWithBay(network, row, primaryDataframe, presentAdder, throwException, reportNode));
        }

        void addWithBay(Network network, int row, UpdatingDataframe dataframe, DanglingLineAdder adder, boolean throwException, ReportNode reportNode) {
            String busOrBusbarSectionId = busOrBusbarSections.get(row);
            OptionalInt injectionPositionOrder = dataframe.getIntValue("position_order", row);
            ConnectablePosition.Direction direction = ConnectablePosition.Direction.valueOf(dataframe.getStringValue("direction", row).orElse("BOTTOM"));
            CreateFeederBayBuilder builder = new CreateFeederBayBuilder()
                    .withInjectionAdder(adder)
                    .withBusOrBusbarSectionId(busOrBusbarSectionId)
                    .withInjectionDirection(direction);
            if (injectionPositionOrder.isPresent()) {
                builder.withInjectionPositionOrder(injectionPositionOrder.getAsInt());
            }
            CreateFeederBay modification = builder.build();
            modification.apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode);
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe dataframe = dataframes.get(0);
        UpdatingDataframe generationDataframe = dataframes.get(1);
        DanglingLineSeries series = new DanglingLineSeries(dataframe, generationDataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row, true);
        }
    }

    @Override
    public void addElementsWithBay(Network network, List<UpdatingDataframe> dataframes, boolean throwException, ReportNode reportNode) {
        UpdatingDataframe dataframe = dataframes.get(0);
        UpdatingDataframe generationDataframe = dataframes.get(1);
        DanglingLineSeries series = new DanglingLineSeries(dataframe, generationDataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.createWithBay(network, row, dataframe, true, reportNode);
        }
    }

}
