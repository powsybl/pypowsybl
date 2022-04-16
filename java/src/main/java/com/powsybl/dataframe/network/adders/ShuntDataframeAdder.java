/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            SeriesMetadata.strings("model_type")
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
        TObjectIntMap<String> linearModelsIndexes = getLinearModelsIndexes(linearModelsDf);
        Map<String, TIntArrayList> sectionsIndexes = getSectionsIndexes(sectionsDf);
        for (int index = 0; index < shuntsDf.getRowCount(); index++) {
            createShunt(network, shuntsDf, linearModelsDf, sectionsDf, linearModelsIndexes, sectionsIndexes, index);
        }
    }

    /**
     * Mapping shunt ID --> index of line in dataframe
     */
    private static TObjectIntMap<String> getLinearModelsIndexes(UpdatingDataframe linearModelsDf) {
        TObjectIntMap<String> indexes = new TObjectIntHashMap<>(10, 0.5f, -1);
        for (int modelIndex = 0; modelIndex < linearModelsDf.getRowCount(); modelIndex++) {
            String shuntId = linearModelsDf.getStringValue("id", modelIndex)
                    .orElseThrow(() -> new PowsyblException("Linear models dataframe: id is not set"));
            indexes.put(shuntId, modelIndex);
        }
        return indexes;
    }

    /**
     * Mapping shunt ID --> index of lines in dataframe
     */
    private static Map<String, TIntArrayList> getSectionsIndexes(UpdatingDataframe sectionsDf) {
        Map<String, TIntArrayList> sectionsIndexes = new HashMap<>();
        for (int sectionIndex = 0; sectionIndex < sectionsDf.getRowCount(); sectionIndex++) {
            String shuntId = sectionsDf.getStringValue("id", sectionIndex)
                    .orElseThrow(() -> new PowsyblException("Shunt sections dataframe: id is not set"));
            sectionsIndexes.computeIfAbsent(shuntId, k -> new TIntArrayList())
                    .add(sectionIndex);
        }
        return sectionsIndexes;
    }

    private static void createShunt(Network network,
                                    UpdatingDataframe shuntsDf,
                                    UpdatingDataframe linearModelsDf,
                                    UpdatingDataframe sectionsDf,
                                    TObjectIntMap<String> linearModelIndexes,
                                    Map<String, TIntArrayList> sectionsIndexes,
                                    int shuntIndex) {
        String voltageLevelId = shuntsDf.getStringValue("voltage_level_id", shuntIndex)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"));
        String shuntId = shuntsDf.getStringValue("id", shuntIndex)
                .orElseThrow(() -> new PowsyblException("Shunts dataframe: id is not set"));

        ShuntCompensatorAdder adder = network.getVoltageLevel(voltageLevelId)
                .newShuntCompensator();
        NetworkElementCreationUtils.createInjection(adder, shuntsDf, shuntIndex);
        shuntsDf.getIntValue("section_count", shuntIndex).ifPresent(adder::setSectionCount);
        shuntsDf.getDoubleValue("target_deadband", shuntIndex).ifPresent(adder::setTargetDeadband);
        shuntsDf.getDoubleValue("target_v", shuntIndex).ifPresent(adder::setTargetV);

        ShuntCompensatorModelType modelType = shuntsDf.getStringValue("model_type", shuntIndex)
                .map(ShuntCompensatorModelType::valueOf)
                .orElseThrow(() -> new PowsyblException("model_type must be defined for a linear shunt"));

        if (modelType == ShuntCompensatorModelType.LINEAR) {
            ShuntCompensatorLinearModelAdder linearModelAdder = adder.newLinearModel();
            int index = linearModelIndexes.get(shuntId);
            if (index == -1) {
                throw new PowsyblException("one section must be defined for a linear shunt");
            }
            linearModelsDf.getDoubleValue("b_per_section", index).ifPresent(linearModelAdder::setBPerSection);
            linearModelsDf.getDoubleValue("g_per_section", index).ifPresent(linearModelAdder::setGPerSection);
            linearModelsDf.getIntValue("max_section_count", index).ifPresent(linearModelAdder::setMaximumSectionCount);
            linearModelAdder.add();
        } else if (modelType == ShuntCompensatorModelType.NON_LINEAR) {
            ShuntCompensatorNonLinearModelAdder nonLinearAdder = adder.newNonLinearModel();

            TIntArrayList sections = sectionsIndexes.get(shuntId);
            if (sections == null) {
                throw new PowsyblException("At least one section must be defined for a non linear shunt.");
            }
            sections.forEach(i -> {
                ShuntCompensatorNonLinearModelAdder.SectionAdder section = nonLinearAdder.beginSection();
                sectionsDf.getDoubleValue("g", i).ifPresent(section::setG);
                sectionsDf.getDoubleValue("b", i).ifPresent(section::setB);
                section.endSection();
                return true;
            });
            nonLinearAdder.add();
        } else {
            throw new PowsyblException("shunt model type non valid");
        }
        adder.add();
    }
}
