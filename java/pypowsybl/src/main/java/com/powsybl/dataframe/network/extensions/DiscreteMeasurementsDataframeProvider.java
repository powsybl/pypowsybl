/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.DiscreteMeasurement;
import com.powsybl.iidm.network.extensions.DiscreteMeasurements;
import org.jgrapht.alg.util.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class DiscreteMeasurementsDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    public static final String ELEMENT_ID = "element_id";
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String TAP_CHANGER = "tap_changer";
    public static final String VALUE_TYPE = "value_type";
    public static final String VALUE = "value";
    public static final String VALID = "valid";

    @Override
    public String getExtensionName() {
        return DiscreteMeasurements.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(DiscreteMeasurements.NAME, "Provides discrete measurements about a specific equipment",
                "index : element_id (str),id (str), type (str), tap_changer (str), value_type (str), value (str), valid (bool)");
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream)
                .stringsIndex(ELEMENT_ID, DiscreteMeasurementInformation::getElementId)
                .strings(ID, DiscreteMeasurementInformation::getId)
                .strings(TYPE, info -> info.getType().toString())
                .strings(TAP_CHANGER, info -> info.getTapChanger() == null ? null : info.getTapChanger().toString())
                .strings(VALUE_TYPE, info -> info.getValueType().toString())
                .strings(VALUE, DiscreteMeasurementInformation::getValue)
                .booleans(VALID, DiscreteMeasurementInformation::isValid)
                .build();
    }

    @SuppressWarnings("unchecked")
    static void removeDiscreteMeasurements(Identifiable<?> identifiable) {
        identifiable.removeExtension(DiscreteMeasurements.class);
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getIdentifiable)
                .filter(Objects::nonNull)
                .forEach(DiscreteMeasurementsDataframeProvider::removeDiscreteMeasurements);
    }

    @SuppressWarnings("unchecked")
    static DiscreteMeasurements<?> getDiscreteMeasurements(Identifiable<?> identifiable) {
        return (DiscreteMeasurements<?>) identifiable.getExtension(DiscreteMeasurements.class);
    }

    private Stream<DiscreteMeasurementInformation> itemsStream(Network network) {
        return network.getIdentifiables().stream()
                .map(identifiable -> Pair.of(identifiable.getId(), getDiscreteMeasurements(identifiable)))
                .filter(pair -> pair.getSecond() != null)
                .flatMap(pair -> pair.getSecond().getDiscreteMeasurements().stream()
                        .map(measurement -> new DiscreteMeasurementInformation(measurement, pair.getFirst())));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new DiscreteMeasurementsDataframeAdder();
    }

    private static class DiscreteMeasurementInformation {
        private final String elementId;
        private final String id;
        private final DiscreteMeasurement.Type type;
        private final DiscreteMeasurement.TapChanger tapChanger;
        private final DiscreteMeasurement.ValueType valueType;
        private final String value;
        private final boolean valid;

        public DiscreteMeasurementInformation(DiscreteMeasurement measurement, String elementId) {
            this.elementId = elementId;
            this.id = measurement.getId();
            this.type = measurement.getType();
            this.tapChanger = measurement.getTapChanger();
            this.valueType = measurement.getValueType();
            // FIXME make object directly available in DiscreteMeasurement interface
            this.value = switch (measurement.getValueType()) {
                case BOOLEAN -> Boolean.toString(measurement.getValueAsBoolean());
                case INT -> Integer.toString(measurement.getValueAsInt());
                case STRING -> measurement.getValueAsString();
            };
            this.valid = measurement.isValid();
        }

        public String getId() {
            return id;
        }

        public String getElementId() {
            return elementId;
        }

        public DiscreteMeasurement.Type getType() {
            return type;
        }

        public DiscreteMeasurement.TapChanger getTapChanger() {
            return tapChanger;
        }

        public DiscreteMeasurement.ValueType getValueType() {
            return valueType;
        }

        public String getValue() {
            return value;
        }

        public boolean isValid() {
            return valid;
        }
    }
}
