/**
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Area;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.*;

import static com.powsybl.dataframe.network.adders.SeriesUtils.*;

/**
 * @author Damien Jeandemange <damien.jeandemange@artelys.com>
 */
public class AreaVoltageLevelsDataframeAdder implements NetworkElementAdder {

    public enum AdderType {
        ADD,
        REMOVE
    }

    private final AdderType adderType;

    public AreaVoltageLevelsDataframeAdder(AdderType adderType) {
        this.adderType = adderType;
    }

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level_id")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static final class AreaVoltageLevels {

        private final StringSeries ids;
        private final StringSeries voltageLevels;

        AreaVoltageLevels(UpdatingDataframe dataframe) {
            this.ids = getRequiredStrings(dataframe, "id");
            this.voltageLevels = getRequiredStrings(dataframe, "voltage_level_id");
        }

        public StringSeries getIds() {
            return ids;
        }

        public StringSeries getVoltageLevels() {
            return voltageLevels;
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryTable = dataframes.get(0);
        AreaVoltageLevels series = new AreaVoltageLevels(primaryTable);

        for (int i = 0; i < primaryTable.getRowCount(); i++) {
            String areaId = series.getIds().get(i);
            String voltageLevelId = series.getVoltageLevels().get(i);
            Area area = network.getArea(areaId);
            VoltageLevel voltageLevel = voltageLevelId.isEmpty() ? null : network.getVoltageLevel(voltageLevelId);
            if (area == null) {
                throw new PowsyblException("Area " + areaId + " not found");
            }
            if (voltageLevel == null) {
                throw new PowsyblException("VoltageLevel " + voltageLevelId + " not found");
            }
            switch (adderType) {
                case ADD -> area.addVoltageLevel(voltageLevel);
                case REMOVE -> area.removeVoltageLevel(voltageLevel);
            }
        }
    }
}
