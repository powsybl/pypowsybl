/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.modification.topology.CreateVoltageLevelTopologyBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.SwitchKind;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class VoltageLevelTopologyCreation implements NetworkModification {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.ints("low_busbar_index"),
            SeriesMetadata.ints("busbar_count"),
            SeriesMetadata.ints("low_section_index"),
            SeriesMetadata.ints("section_count"),
            SeriesMetadata.strings("busbar_section_prefix_id"),
            SeriesMetadata.strings("switch_prefix_id"),
            SeriesMetadata.strings("switch_kinds")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private CreateVoltageLevelTopologyBuilder createBuilder(UpdatingDataframe dataframe) {
        CreateVoltageLevelTopologyBuilder builder = new CreateVoltageLevelTopologyBuilder();
        List<SwitchKind> switchKinds = null;
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            applyIfPresent(dataframe.getStrings("id"), row, builder::withVoltageLevelId);
            applyIfPresent(dataframe.getInts("low_busbar_index"), row, builder::withLowBusbarIndex);
            applyIfPresent(dataframe.getInts("busbar_count"), row, builder::withBusbarCount);
            applyIfPresent(dataframe.getInts("low_section_index"), row, builder::withLowSectionIndex);
            applyIfPresent(dataframe.getInts("section_count"), row, builder::withSectionCount);
            applyIfPresent(dataframe.getStrings("busbar_section_prefix_id"), row, builder::withBusbarSectionPrefixId);
            applyIfPresent(dataframe.getStrings("switch_prefix_id"), row, builder::withSwitchPrefixId);
            switchKinds = getSwitchKinds(dataframe.getStringValue("switch_kinds", row).orElse(""));
        }
        builder.withSwitchKinds(switchKinds);
        return builder;
    }

    private List<SwitchKind> getSwitchKinds(String switchKindsStr) {
        if (switchKindsStr.equals("")) {
            return Collections.emptyList();
        }
        return Arrays.stream(switchKindsStr.split(", ")).map(SwitchKind::valueOf).collect(Collectors.toList());
    }

    @Override
    public void applyModification(Network network, List<UpdatingDataframe> dataframes, boolean throwException, ReporterModel reporter) {
        if (dataframes.size() != 1) {
            throw new IllegalArgumentException("Expected only one input dataframe");
        }
        CreateVoltageLevelTopologyBuilder builder = createBuilder(dataframes.get(0));
        com.powsybl.iidm.modification.NetworkModification modification = builder.build();
        modification.apply(network, throwException, reporter == null ? Reporter.NO_OP : reporter);
    }
}
