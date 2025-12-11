/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class CategoryAttributeTest {

    @Test
    void testAttributeFromMetadata() {
        List<SeriesMetadata> metadata = List.of(
                SeriesMetadata.stringIndex("id"),
                SeriesMetadata.booleans("param1"),
                SeriesMetadata.doubles("param2"),
                SeriesMetadata.ints("param3"));
        String attribute = CategoryAttributeUtils.createFromMetadata(metadata);
        assertEquals("index : id (str), param1 (bool), param2 (double), param3 (int)", attribute);
    }

    @Test
    void testAttributeFromMetadataList() {
        List<List<SeriesMetadata>> metadataList = List.of(
                List.of(SeriesMetadata.stringIndex("id"),
                        SeriesMetadata.booleans("param1")),
                List.of(SeriesMetadata.stringIndex("id"),
                        SeriesMetadata.strings("data1"),
                        SeriesMetadata.doubles("Data2")),
                List.of(SeriesMetadata.stringIndex("id"),
                        SeriesMetadata.doubles("data3")));
        List<String> names = List.of("base data", "aux data", "aux data 2");
        String attribute = CategoryAttributeUtils.createFromMetadata(metadataList, names);
        assertEquals("[dataframe \"base data\"] index : id (str), param1 (bool) / [dataframe \"aux data\"] index : id (str), data1 (str), Data2 (double) / [dataframe \"aux data 2\"] index : id (str), data3 (double)",
                attribute);
    }

    @Test
    void testListSizeException() {
        List<List<SeriesMetadata>> metadataList = List.of(
                List.of(SeriesMetadata.stringIndex("id"), SeriesMetadata.booleans("param")));
        List<String> names = List.of("base data", "aux data");
        Assertions.assertThatThrownBy(() -> CategoryAttributeUtils.createFromMetadata(metadataList, names))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Name list and metadata list size mismatch");
    }
}
