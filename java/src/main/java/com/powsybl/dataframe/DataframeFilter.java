/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Define filters to apply to a dataframe.
 *
 * @author Massimo Ferraro <massimo.ferraro@soft.it>
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class DataframeFilter {

    private final AttributeFilterType attributeFilterType;
    private final List<String> inputAttributes;
    private final List<String> rowsIds;
    private final List<Integer> rowsSubIds;

    public enum AttributeFilterType {
        DEFAULT_ATTRIBUTES,
        INPUT_ATTRIBUTES,
        ALL_ATTRIBUTES
    }

    public DataframeFilter(AttributeFilterType attributeFilterType, List<String> inputAttributes, List<String> rowsIds, List<Integer> rowsSubIds) {
        this.attributeFilterType = Objects.requireNonNull(attributeFilterType);
        this.inputAttributes = Objects.requireNonNull(inputAttributes);
        this.rowsIds = Objects.requireNonNull(rowsIds);
        this.rowsSubIds = Objects.requireNonNull(rowsSubIds);
        if (rowsSubIds.size() > 0 && rowsSubIds.size() != rowsIds.size()) {
            throw new IllegalArgumentException("rowsIds and rowsSubIds must contain the same number of elements");
        }
    }

    public DataframeFilter(AttributeFilterType attributeFilterType, List<String> inputAttributes) {
        this(attributeFilterType, inputAttributes, Collections.emptyList(), Collections.emptyList());
    }

    public DataframeFilter() {
        this(AttributeFilterType.DEFAULT_ATTRIBUTES, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public DataframeFilter(List<String> rowsIds, List<Integer> rowsSubIds) {
        this(AttributeFilterType.DEFAULT_ATTRIBUTES, Collections.emptyList(), rowsIds, rowsSubIds);
    }

    public AttributeFilterType getAttributeFilterType() {
        return attributeFilterType;
    }

    public List<String> getInputAttributes() {
        return inputAttributes;
    }

    public List<String> getRowsIds() {
        return rowsIds;
    }

    public List<Integer> getRowsSubIds() {
        return rowsSubIds;
    }
}
