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
import java.util.Optional;

/**
 * Define filters to apply to a dataframe.
 *
 * @author Massimo Ferraro <massimo.ferraro@soft.it>
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class DataframeFilter {

    public static final class ElementsFilter {
        private final List<String> rowsIds;
        private final List<Integer> rowsSubIds;

        public ElementsFilter(List<String> rowsIds, List<Integer> rowsSubIds) {
            this.rowsIds = Objects.requireNonNull(rowsIds);
            this.rowsSubIds = Objects.requireNonNull(rowsSubIds);
            if (rowsSubIds.size() > 0 && rowsSubIds.size() != rowsIds.size()) {
                throw new IllegalArgumentException("rowsIds and rowsSubIds must contain the same number of elements");
            }
        }

        public ElementsFilter(List<String> rowsIds) {
            this(rowsIds, Collections.emptyList());
        }

        public List<String> getRowsIds() {
            return rowsIds;
        }

        public List<Integer> getRowsSubIds() {
            return rowsSubIds;
        }
    }

    private final AttributeFilterType attributeFilterType;
    private final List<String> inputAttributes;
    ElementsFilter elementsFilter;

    public enum AttributeFilterType {
        DEFAULT_ATTRIBUTES,
        INPUT_ATTRIBUTES,
        ALL_ATTRIBUTES
    }

    public DataframeFilter(AttributeFilterType attributeFilterType, List<String> inputAttributes, ElementsFilter elementsFilter) {
        this.attributeFilterType = Objects.requireNonNull(attributeFilterType);
        this.inputAttributes = Objects.requireNonNull(inputAttributes);
        this.elementsFilter = elementsFilter;
    }

    public DataframeFilter(AttributeFilterType attributeFilterType, List<String> inputAttributes) {
        this(attributeFilterType, inputAttributes, null);
    }

    public DataframeFilter() {
        this(AttributeFilterType.DEFAULT_ATTRIBUTES, Collections.emptyList(), null);
    }

    public DataframeFilter(ElementsFilter elementsFilter) {
        this(AttributeFilterType.DEFAULT_ATTRIBUTES, Collections.emptyList(), elementsFilter);
    }

    public AttributeFilterType getAttributeFilterType() {
        return attributeFilterType;
    }

    public List<String> getInputAttributes() {
        return inputAttributes;
    }

    public Optional<ElementsFilter> getElementsFilter() {
        return Optional.ofNullable(elementsFilter);
    }
}
