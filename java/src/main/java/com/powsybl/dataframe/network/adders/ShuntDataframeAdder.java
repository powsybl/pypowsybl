/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.CreateFeederBay;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrow;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class ShuntDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> SHUNT_METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.ints("section_count"),
            SeriesMetadata.doubles("target_deadband"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.strings("model_type"),
            SeriesMetadata.strings("busbar_section_id"),
            SeriesMetadata.ints("position_order"),
            SeriesMetadata.strings("direction")
    );

    private static final List<SeriesMetadata> LINEAR_SECTIONS_METADATA = List.of(
            SeriesMetadata.strings("id"),
            SeriesMetadata.doubles("g_per_section"),
            SeriesMetadata.doubles("b_per_section"),
            SeriesMetadata.ints("max_section_count")
    );

    private static final List<SeriesMetadata> NON_LINEAR_SECTIONS_METADATA = List.of(
            SeriesMetadata.strings("id"),
            SeriesMetadata.doubles("g"),
            SeriesMetadata.doubles("b")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(SHUNT_METADATA, LINEAR_SECTIONS_METADATA, NON_LINEAR_SECTIONS_METADATA);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe shuntsDf = dataframes.get(0);
        UpdatingDataframe linearModelsDf = dataframes.get(1);
        UpdatingDataframe sectionsDf = dataframes.get(2);
        ShuntCompensatorSeries series = new ShuntCompensatorSeries(shuntsDf, linearModelsDf, sectionsDf);
        for (int row = 0; row < shuntsDf.getRowCount(); row++) {
            series.create(network, row);
        }
    }

    @Override
    public void addElementsWithBay(Network network, List<UpdatingDataframe> dataframes, boolean throwException, Reporter reporter) {
        UpdatingDataframe shuntsDf = dataframes.get(0);
        UpdatingDataframe linearModelsDf = dataframes.get(1);
        UpdatingDataframe sectionsDf = dataframes.get(2);
        ShuntCompensatorSeries series = new ShuntCompensatorSeries(shuntsDf, linearModelsDf, sectionsDf);
        for (int row = 0; row < shuntsDf.getRowCount(); row++) {
            ShuntCompensatorAdder adder = series.createAdder(network, row);
            String busbarSectionId = shuntsDf.getStrings("busbar_section_id").get(row);
            int injectionPositionOrder = shuntsDf.getInts("position_order").get(row);
            ConnectablePosition.Direction direction = ConnectablePosition.Direction.valueOf(shuntsDf.getStringValue("direction", row).orElse("BOTTOM"));
            CreateFeederBay modification = new CreateFeederBayBuilder()
                    .withInjectionAdder(adder)
                    .withBbsId(busbarSectionId)
                    .withInjectionPositionOrder(injectionPositionOrder)
                    .withInjectionDirection(direction)
                    .build();
            modification.apply(network, throwException, reporter == null ? Reporter.NO_OP : reporter);
        }
    }

    private static class ShuntCompensatorSeries extends InjectionSeries {
        private final StringSeries voltageLevels;
        private final IntSeries sectionCount;
        private final DoubleSeries targetDeadband;
        private final DoubleSeries targetV;
        private final StringSeries modelTypes;

        private final TObjectIntMap<String> linearModelsIndexes;
        private final DoubleSeries gPerSection;
        private final DoubleSeries bPerSection;
        private final IntSeries maxSectionCount;

        private final Map<String, TIntArrayList> sectionsIndexes;
        private final DoubleSeries g;
        private final DoubleSeries b;

        ShuntCompensatorSeries(UpdatingDataframe shuntsDf, UpdatingDataframe linearModelsDf, UpdatingDataframe sectionsDf) {
            super(shuntsDf);
            this.voltageLevels = shuntsDf.getStrings("voltage_level_id");
            if (voltageLevels == null) {
                throw new PowsyblException("voltage_level_id is missing");
            }
            this.sectionCount = shuntsDf.getInts("section_count");
            this.targetDeadband = shuntsDf.getDoubles("target_deadband");
            this.targetV = shuntsDf.getDoubles("target_v");
            this.modelTypes = shuntsDf.getStrings("model_type");
            if (this.modelTypes == null) {
                throw new PowsyblException("model_type must be defined for shunt compensators.");
            }
            if (linearModelsDf != null) {
                this.gPerSection = linearModelsDf.getDoubles("g_per_section");
                this.bPerSection = linearModelsDf.getDoubles("b_per_section");
                this.maxSectionCount = linearModelsDf.getInts("max_section_count");
                this.linearModelsIndexes = getLinearModelsIndexes(linearModelsDf);
            } else {
                this.gPerSection = null;
                this.bPerSection = null;
                this.maxSectionCount = null;
                this.linearModelsIndexes = null;
            }
            if (sectionsDf != null) {
                this.g = sectionsDf.getDoubles("g");
                this.b = sectionsDf.getDoubles("b");
                this.sectionsIndexes = getSectionsIndexes(sectionsDf);
            } else {
                this.g = null;
                this.b = null;
                this.sectionsIndexes = null;
            }
        }

        ShuntCompensatorAdder createAdder(Network network, int row) {
            String voltageLevelId = voltageLevels.get(row);
            String shuntId = ids.get(row);

            ShuntCompensatorAdder adder = getVoltageLevelOrThrow(network, voltageLevelId)
                    .newShuntCompensator();
            setInjectionAttributes(adder, row);
            applyIfPresent(sectionCount, row, adder::setSectionCount);
            applyIfPresent(targetDeadband, row, adder::setTargetDeadband);
            applyIfPresent(targetV, row, adder::setTargetV);

            ShuntCompensatorModelType modelType = ShuntCompensatorModelType.valueOf(modelTypes.get(row));

            if (modelType == ShuntCompensatorModelType.LINEAR) {
                ShuntCompensatorLinearModelAdder linearModelAdder = adder.newLinearModel();
                int index = linearModelsIndexes.get(shuntId);
                if (index == -1) {
                    throw new PowsyblException("one section must be defined for a linear shunt");
                }
                applyIfPresent(bPerSection, index, linearModelAdder::setBPerSection);
                applyIfPresent(gPerSection, index, linearModelAdder::setGPerSection);
                applyIfPresent(maxSectionCount, index, linearModelAdder::setMaximumSectionCount);
                linearModelAdder.add();
            } else if (modelType == ShuntCompensatorModelType.NON_LINEAR) {
                ShuntCompensatorNonLinearModelAdder nonLinearAdder = adder.newNonLinearModel();
                TIntArrayList sections = sectionsIndexes.get(shuntId);
                if (sections == null) {
                    throw new PowsyblException("At least one section must be defined for a non linear shunt.");
                }
                sections.forEach(i -> {
                    ShuntCompensatorNonLinearModelAdder.SectionAdder section = nonLinearAdder.beginSection();
                    applyIfPresent(g, i, section::setG);
                    applyIfPresent(b, i, section::setB);
                    section.endSection();
                    return true;
                });
                nonLinearAdder.add();
            } else {
                throw new PowsyblException("shunt model type non valid");
            }
            return adder;
        }

        void create(Network network, int row) {
            ShuntCompensatorAdder adder = createAdder(network, row);
            adder.add();
        }
    }

    /**
     * Mapping shunt ID --> index of line in dataframe
     */
    private static TObjectIntMap<String> getLinearModelsIndexes(UpdatingDataframe linearModelsDf) {
        StringSeries ids = linearModelsDf.getStrings("id");
        if (ids == null) {
            throw new PowsyblException("Linear models dataframe: id is not set");
        }
        TObjectIntMap<String> indexes = new TObjectIntHashMap<>(10, 0.5f, -1);
        for (int modelIndex = 0; modelIndex < linearModelsDf.getRowCount(); modelIndex++) {
            String shuntId = ids.get(modelIndex);
            indexes.put(shuntId, modelIndex);
        }
        return indexes;
    }

    /**
     * Mapping shunt ID --> index of lines in dataframe
     */
    private static Map<String, TIntArrayList> getSectionsIndexes(UpdatingDataframe sectionsDf) {
        StringSeries ids = sectionsDf.getStrings("id");
        if (ids == null) {
            throw new PowsyblException("Shunt sections dataframe: id is not set");
        }
        Map<String, TIntArrayList> sectionsIndexes = new HashMap<>();
        for (int sectionIndex = 0; sectionIndex < sectionsDf.getRowCount(); sectionIndex++) {
            String shuntId = ids.get(sectionIndex);
            sectionsIndexes.computeIfAbsent(shuntId, k -> new TIntArrayList())
                    .add(sectionIndex);
        }
        return sectionsIndexes;
    }
}
