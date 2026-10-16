/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * Creates {@link OverloadManagementSystem}s from two dataframes: one describing the overload management systems
 * themselves, the other one describing their trippings (grouped by overload management system id).
 */
public class OverloadManagementSystemDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("substation_id"),
            SeriesMetadata.strings("monitored_element_id"),
            SeriesMetadata.strings("monitored_element_side"),
            SeriesMetadata.booleans("enabled")
    );

    private static final List<SeriesMetadata> TRIPPINGS_METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("tripping_type"),
            SeriesMetadata.strings("tripping_key"),
            SeriesMetadata.doubles("current_limit"),
            SeriesMetadata.booleans("open_action"),
            SeriesMetadata.strings("element_to_operate_id"),
            SeriesMetadata.strings("side_to_operate")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(METADATA, TRIPPINGS_METADATA);
    }

    private static final class OverloadManagementSystemSeries {
        private final StringSeries ids;
        private final StringSeries names;
        private final StringSeries substationIds;
        private final StringSeries monitoredElementIds;
        private final StringSeries monitoredElementSides;
        private final IntSeries enabled;

        private final Map<String, TIntArrayList> trippingsIndexes;
        private final StringSeries trippingNames;
        private final StringSeries trippingTypes;
        private final StringSeries trippingKeys;
        private final DoubleSeries currentLimits;
        private final IntSeries openActions;
        private final StringSeries elementToOperateIds;
        private final StringSeries sidesToOperate;

        OverloadManagementSystemSeries(UpdatingDataframe omsDf, UpdatingDataframe trippingsDf) {
            this.ids = omsDf.getStrings("id");
            this.names = omsDf.getStrings("name");
            this.substationIds = omsDf.getStrings("substation_id");
            this.monitoredElementIds = omsDf.getStrings("monitored_element_id");
            this.monitoredElementSides = omsDf.getStrings("monitored_element_side");
            this.enabled = omsDf.getInts("enabled");

            this.trippingsIndexes = getTrippingsIndexes(trippingsDf);
            this.trippingNames = trippingsDf.getStrings("name");
            this.trippingTypes = trippingsDf.getStrings("tripping_type");
            this.trippingKeys = trippingsDf.getStrings("tripping_key");
            this.currentLimits = trippingsDf.getDoubles("current_limit");
            this.openActions = trippingsDf.getInts("open_action");
            this.elementToOperateIds = trippingsDf.getStrings("element_to_operate_id");
            this.sidesToOperate = trippingsDf.getStrings("side_to_operate");
        }

        void create(Network network, int row) {
            String id = ids.get(row);
            if (substationIds == null) {
                throw new PowsyblException("substation_id is missing: cannot create overload management system '" + id + "'.");
            }
            String substationId = substationIds.get(row);
            Substation substation = network.getSubstation(substationId);
            if (substation == null) {
                throw new PowsyblException("Substation '" + substationId + "' not found.");
            }
            OverloadManagementSystemAdder adder = substation.newOverloadManagementSystem()
                    .setId(id);
            applyIfPresent(names, row, adder::setName);
            applyIfPresent(monitoredElementIds, row, adder::setMonitoredElementId);
            applyIfPresent(monitoredElementSides, row, side -> adder.setMonitoredElementSide(ThreeSides.valueOf(side)));
            applyBooleanIfPresent(enabled, row, adder::setEnabled);

            TIntArrayList trippings = trippingsIndexes.get(id);
            if (trippings != null) {
                trippings.forEach(i -> {
                    createTripping(adder, i);
                    return true;
                });
            }
            adder.add();
        }

        private void createTripping(OverloadManagementSystemAdder adder, int row) {
            String typeStr = trippingTypes != null ? trippingTypes.get(row) : null;
            if (typeStr == null || typeStr.isEmpty()) {
                throw new PowsyblException("tripping_type is missing for an overload management system tripping.");
            }
            OverloadManagementSystem.Tripping.Type type = OverloadManagementSystem.Tripping.Type.valueOf(typeStr);
            switch (type) {
                case BRANCH_TRIPPING -> {
                    OverloadManagementSystemAdder.BranchTrippingAdder trippingAdder = adder.newBranchTripping();
                    applyCommon(trippingAdder, row);
                    applyIfPresent(elementToOperateIds, row, trippingAdder::setBranchToOperateId);
                    applyIfPresent(sidesToOperate, row, side -> trippingAdder.setSideToOperate(TwoSides.valueOf(side)));
                    trippingAdder.add();
                }
                case THREE_WINDINGS_TRANSFORMER_TRIPPING -> {
                    OverloadManagementSystemAdder.ThreeWindingsTransformerTrippingAdder trippingAdder =
                            adder.newThreeWindingsTransformerTripping();
                    applyCommon(trippingAdder, row);
                    applyIfPresent(elementToOperateIds, row, trippingAdder::setThreeWindingsTransformerToOperateId);
                    applyIfPresent(sidesToOperate, row, side -> trippingAdder.setSideToOperate(ThreeSides.valueOf(side)));
                    trippingAdder.add();
                }
                case SWITCH_TRIPPING -> {
                    OverloadManagementSystemAdder.SwitchTrippingAdder trippingAdder = adder.newSwitchTripping();
                    applyCommon(trippingAdder, row);
                    applyIfPresent(elementToOperateIds, row, trippingAdder::setSwitchToOperateId);
                    trippingAdder.add();
                }
            }
        }

        private void applyCommon(OverloadManagementSystemAdder.TrippingAdder<?> trippingAdder, int row) {
            applyIfPresent(trippingKeys, row, trippingAdder::setKey);
            applyIfPresent(trippingNames, row, trippingAdder::setName);
            applyIfPresent(currentLimits, row, trippingAdder::setCurrentLimit);
            applyBooleanIfPresent(openActions, row, trippingAdder::setOpenAction);
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 2) {
            throw new PowsyblException("Expected 2 dataframes: one for overload management systems, one for trippings.");
        }
        UpdatingDataframe omsDf = dataframes.get(0);
        UpdatingDataframe trippingsDf = dataframes.get(1);
        OverloadManagementSystemSeries series = new OverloadManagementSystemSeries(omsDf, trippingsDf);
        IntStream.range(0, omsDf.getRowCount())
                .forEach(row -> series.create(network, row));
    }

    /**
     * Mapping overload management system ID --> indexes of its trippings in the trippings dataframe.
     */
    private static Map<String, TIntArrayList> getTrippingsIndexes(UpdatingDataframe trippingsDataframe) {
        StringSeries ids = trippingsDataframe.getStrings("id");
        if (ids == null) {
            throw new PowsyblException("Trippings dataframe: id is not set");
        }
        Map<String, TIntArrayList> trippingIndexes = new HashMap<>();
        for (int trippingIndex = 0; trippingIndex < trippingsDataframe.getRowCount(); trippingIndex++) {
            String omsId = ids.get(trippingIndex);
            trippingIndexes.computeIfAbsent(omsId, k -> new TIntArrayList())
                    .add(trippingIndex);
        }
        return trippingIndexes;
    }
}
