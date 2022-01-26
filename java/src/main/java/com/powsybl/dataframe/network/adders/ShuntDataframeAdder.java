/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class ShuntDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> SHUNT_METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
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
    public void addElement(Network network, List<UpdatingDataframe> dataframes, int indexElement) {
        UpdatingDataframe shuntDataframe = dataframes.get(0);
        UpdatingDataframe linearDataframe = dataframes.get(1);
        UpdatingDataframe nonLinearDataframe = dataframes.get(2);
        String voltageLevelId = shuntDataframe.getStringValue("voltage_level_id", indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"));
        if (shuntDataframe.getStringValue("id", indexElement).isEmpty()) {
            throw new PowsyblException("id must be defined for a linear shunt");
        }
        String shuntId = shuntDataframe.getStringValue("id", indexElement).get();
        ShuntCompensatorAdder adder = network.getVoltageLevel(voltageLevelId)
                .newShuntCompensator();
        CreateEquipmentHelper.createInjection(adder, shuntDataframe, indexElement);
        shuntDataframe.getIntValue("section_count", indexElement).ifPresent(adder::setSectionCount);
        shuntDataframe.getDoubleValue("target_deadband", indexElement).ifPresent(adder::setTargetDeadband);
        shuntDataframe.getDoubleValue("target_v", indexElement).ifPresent(adder::setTargetV);

        ShuntCompensatorModelType modelType = shuntDataframe.getStringValue("model_type", indexElement)
                .map(ShuntCompensatorModelType::valueOf)
                .orElseThrow(() -> new PowsyblException("model_type must be defined for a linear shunt"));

        if (modelType == ShuntCompensatorModelType.LINEAR) {
            ShuntCompensatorLinearModelAdder linearModelAdder = adder.newLinearModel();
            int index = linearDataframe.getIndex("id", shuntId);
            if (index == -1) {
                throw new PowsyblException("one section must be defined for a linear shunt");
            }
            linearDataframe.getDoubleValue("b_per_section", index).ifPresent(linearModelAdder::setBPerSection);
            linearDataframe.getDoubleValue("g_per_section", index).ifPresent(linearModelAdder::setGPerSection);
            linearDataframe.getIntValue("max_section_count", index).ifPresent(linearModelAdder::setMaximumSectionCount);
            linearModelAdder.add();
        } else if (modelType == ShuntCompensatorModelType.NON_LINEAR) {
            ShuntCompensatorNonLinearModelAdder nonLinearAdder = adder.newNonLinearModel();
            int sectionNumber = 0;
            for (int sectionIndex = 0; sectionIndex < nonLinearDataframe.getLineCount(); sectionIndex++) {
                String id = nonLinearDataframe.getStringValue("id", sectionIndex).orElse(null);
                if (shuntId.equals(id)) {
                    sectionNumber++;
                    ShuntCompensatorNonLinearModelAdder.SectionAdder section = nonLinearAdder.beginSection();
                    nonLinearDataframe.getDoubleValue("g", sectionIndex).ifPresent(section::setG);
                    nonLinearDataframe.getDoubleValue("b", sectionIndex).ifPresent(section::setB);
                    section.endSection();
                }
            }
            if (sectionNumber == 0) {
                throw new PowsyblException("at least one section must be defined for a shunt");
            }
            nonLinearAdder.add();
        } else {
            throw new PowsyblException("shunt model type non valid");
        }
        adder.add();
    }
}
