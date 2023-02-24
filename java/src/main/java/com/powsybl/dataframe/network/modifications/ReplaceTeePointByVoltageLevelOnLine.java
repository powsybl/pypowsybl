/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.modifications;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.ReplaceTeePointByVoltageLevelOnLineBuilder;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class ReplaceTeePointByVoltageLevelOnLine implements NetworkModification {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("tee_point_line1"),
            SeriesMetadata.strings("tee_point_line2"),
            SeriesMetadata.strings("tee_point_line_to_remove"),
            SeriesMetadata.strings("bbs_or_bus_id"),
            SeriesMetadata.strings("new_line1_id"),
            SeriesMetadata.strings("new_line2_id"),
            SeriesMetadata.strings("new_line1_name"),
            SeriesMetadata.strings("new_line2_name")
    );

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private ReplaceTeePointByVoltageLevelOnLineBuilder createBuilder(UpdatingDataframe dataframe, int row) {
        ReplaceTeePointByVoltageLevelOnLineBuilder builder = new ReplaceTeePointByVoltageLevelOnLineBuilder();
        applyIfPresent(dataframe.getStrings("tee_point_line1"), row, builder::withTeePointLine1);
        applyIfPresent(dataframe.getStrings("tee_point_line2"), row, builder::withTeePointLine2);
        applyIfPresent(dataframe.getStrings("tee_point_line_to_remove"), row, builder::withTeePointLineToRemove);
        applyIfPresent(dataframe.getStrings("bbs_or_bus_id"), row, builder::withBbsOrBusId);
        applyIfPresent(dataframe.getStrings("new_line1_id"), row, builder::withNewLine1Id);
        applyIfPresent(dataframe.getStrings("new_line2_id"), row, builder::withNewLine2Id);
        applyIfPresent(dataframe.getStrings("new_line1_name"), row, builder::withNewLine1Name);
        applyIfPresent(dataframe.getStrings("new_line2_name"), row, builder::withNewLine2Id);
        if (dataframe.getStringValue("new_line1_name", row).isEmpty()) {
            applyIfPresent(dataframe.getStrings("new_line1_id"), row, builder::withNewLine1Name);
        }
        if (dataframe.getStringValue("new_line2_name", row).isEmpty()) {
            applyIfPresent(dataframe.getStrings("new_line2_id"), row, builder::withNewLine2Name);
        }
        return builder;
    }

    @Override
    public void applyModification(Network network, List<UpdatingDataframe> dataframe, boolean throwException, ReporterModel reporter) {
        for (int row = 0; row < dataframe.get(0).getRowCount(); row++) {
            ReplaceTeePointByVoltageLevelOnLineBuilder builder = createBuilder(dataframe.get(0), row);
            builder.build().apply(network, throwException, reporter == null ? Reporter.NO_OP : reporter);
        }
    }
}
