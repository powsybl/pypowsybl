/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.DiscreteMeasurement;
import com.powsybl.iidm.network.extensions.DiscreteMeasurementAdder;
import com.powsybl.iidm.network.extensions.DiscreteMeasurements;
import com.powsybl.iidm.network.extensions.DiscreteMeasurementsAdder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class DiscreteMeasurementsDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DiscreteMeasurementsDataframeProvider.ELEMENT_ID),
            SeriesMetadata.strings(DiscreteMeasurementsDataframeProvider.ID),
            SeriesMetadata.strings(DiscreteMeasurementsDataframeProvider.TYPE),
            SeriesMetadata.strings(DiscreteMeasurementsDataframeProvider.TAP_CHANGER),
            SeriesMetadata.strings(DiscreteMeasurementsDataframeProvider.VALUE_TYPE),
            SeriesMetadata.strings(DiscreteMeasurementsDataframeProvider.VALUE),
            SeriesMetadata.booleans(DiscreteMeasurementsDataframeProvider.VALID)
    );

    private static class DiscreteMeasurementsSeries {
        private final StringSeries id;
        private final StringSeries elementId;
        private final StringSeries type;
        private final StringSeries tapChanger;
        private final StringSeries valueType;
        private final StringSeries value;
        private final IntSeries valid;
        private final Map<String, DiscreteMeasurements<?>> measurementsByElementId = new HashMap<>();

        DiscreteMeasurementsSeries(UpdatingDataframe dataframe) {
            this.elementId = dataframe.getStrings(DiscreteMeasurementsDataframeProvider.ELEMENT_ID);
            this.id = dataframe.getStrings(DiscreteMeasurementsDataframeProvider.ID);
            this.type = dataframe.getStrings(DiscreteMeasurementsDataframeProvider.TYPE);
            this.tapChanger = dataframe.getStrings(DiscreteMeasurementsDataframeProvider.TAP_CHANGER);
            this.valueType = dataframe.getStrings(DiscreteMeasurementsDataframeProvider.VALUE_TYPE);
            this.value = dataframe.getStrings(DiscreteMeasurementsDataframeProvider.VALUE);
            this.valid = dataframe.getInts(DiscreteMeasurementsDataframeProvider.VALID);
        }

        void create(int row) {
            String elementId = this.elementId.get(row);
            DiscreteMeasurements<?> measurements = measurementsByElementId.get(elementId);
            DiscreteMeasurementAdder adder = measurements.newDiscreteMeasurement();
            SeriesUtils.applyIfPresent(id, row, adder::setId);
            SeriesUtils.applyIfPresent(type, row, type -> adder.setType(DiscreteMeasurement.Type.valueOf(type)));
            SeriesUtils.applyIfPresent(tapChanger, row, tapChanger -> adder.setTapChanger(DiscreteMeasurement.TapChanger.valueOf(tapChanger)));
            SeriesUtils.applyIfPresent(value, row, value -> {
                switch (DiscreteMeasurement.ValueType.valueOf(valueType.get(row))) {
                    case BOOLEAN -> adder.setValue(Boolean.parseBoolean(value));
                    case INT -> adder.setValue(Integer.parseInt(value));
                    case STRING -> adder.setValue(value);
                }
            });
            SeriesUtils.applyBooleanIfPresent(valid, row, adder::setValid);
            adder.add();
        }

        @SuppressWarnings("unchecked")
        DiscreteMeasurements<?> createDiscreteMeasurement(Identifiable<?> identifiable) {
            DiscreteMeasurementsAdder<?> adder = identifiable.newExtension(DiscreteMeasurementsAdder.class);
            return adder.add();
        }

        void removeAndInitialize(Network network, int row) {
            String elementId = this.elementId.get(row);
            if (!measurementsByElementId.containsKey(elementId)) {
                Identifiable<?> identifiable = network.getIdentifiable(elementId);
                if (identifiable == null) {
                    throw new PowsyblException("Invalid element id '" + elementId + "'");
                }
                DiscreteMeasurementsDataframeProvider.removeDiscreteMeasurements(identifiable);
                measurementsByElementId.put(elementId, createDiscreteMeasurement(identifiable));
            }
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        DiscreteMeasurementsSeries series = new DiscreteMeasurementsSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.removeAndInitialize(network, row);
        }
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(row);
        }
    }

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }
}
