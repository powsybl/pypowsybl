/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.SeriesMetadata;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class CategoryAttributeUtils {

    private CategoryAttributeUtils() {
    }

    public static CategoryInformation createFromMetadata(String name, List<SeriesMetadata> metadata) {
        return new CategoryInformation(name, createFromMetadata(metadata));
    }

    public static String createFromMetadata(List<List<SeriesMetadata>> metadataList, List<String> metaDataName) {
        if (metadataList.size() != metaDataName.size()) {
            throw new PowsyblException("Name list and metadata list size mismatch");
        }
        String[] attributes = new String[metaDataName.size()];
        for (int i = 0; i < metaDataName.size(); i++) {
            attributes[i] = String.format("[dataframe \"%s\"] %s", metaDataName.get(i),
                    createFromMetadata(metadataList.get(i)));
        }
        return String.join(" / ", attributes);
    }

    public static String createFromMetadata(List<SeriesMetadata> metadata) {
        return metadata.stream()
                .map(m -> (m.isIndex() ? "index : " + m.getName() : m.getName())
                        + convertSeriesDataType(m.getType()))
                .collect(Collectors.joining(", "));
    }

    private static String convertSeriesDataType(SeriesDataType dataType) {
        return switch (dataType) {
            case STRING -> " (str)";
            case BOOLEAN -> " (bool)";
            case INT -> " (int)";
            case DOUBLE -> " (double)";
        };
    }
}
