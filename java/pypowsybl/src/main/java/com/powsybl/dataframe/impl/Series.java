/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.impl;

import com.powsybl.commons.PowsyblException;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * POJO representation of a series.
 * Using a "union type" for now (only one type of array is used).
 */
public class Series {

    private final boolean index;
    private final String name;
    private final double[] doubles;
    private final int[] ints;
    private final OptionalInt[] optionalInts;
    private final OptionalDouble[] optionalDoubles;
    private final boolean[] booleans;
    private final String[] strings;
    private final List<String> dictionary;

    public Series(String name, double[] values) {
        this(false, name, values, null, null, null, null, null);
    }

    public Series(String name, String[] values) {
        this(false, name, null, null, null, null, null, values);
    }

    public Series(String name, int[] values) {
        this(false, name, null, values, null, null, null, null);
    }

    public Series(String name, OptionalInt[] values) {
        this(false, name, null, null, values, null, null, null);
    }

    public Series(String name, OptionalDouble[] values) {
        this(false, name, null, null, null, values, null, null);
    }

    public Series(String name, boolean[] values) {
        this(false, name, null, null, null, null, values, null);
    }

    public Series(boolean index, String name, double[] doubles, int[] ints,
                  OptionalInt[] optionalInts, OptionalDouble[] optionalDoubles, boolean[] booleans, String[] strings) {
        this.index = index;
        this.name = name;
        this.doubles = doubles;
        this.ints = ints;
        this.optionalInts = optionalInts;
        this.optionalDoubles = optionalDoubles;
        this.booleans = booleans;
        this.strings = strings;
        this.dictionary = null;
    }

    private Series(boolean index, String name, int[] codes, List<String> dictionary) {
        this.index = index;
        this.name = name;
        this.doubles = null;
        this.ints = codes;
        this.optionalInts = null;
        this.optionalDoubles = null;
        this.booleans = null;
        this.strings = null;
        this.dictionary = dictionary;
    }

    /**
     * Dictionary-encoded string series: {@code codes} refer to {@code dictionary}, the code -1 being a missing value.
     */
    public static Series dictionary(boolean index, String name, int[] codes, List<String> dictionary) {
        return new Series(index, name, codes, dictionary);
    }

    public static Series index(String name, String[] values) {
        return new Series(true, name, null, null, null, null, null, values);
    }

    public static Series index(String name, int[] values) {
        return new Series(true, name, null, values, null, null, null, null);
    }

    public boolean isIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public double[] getDoubles() {
        return Optional.ofNullable(doubles)
            .orElseThrow(() -> createException(getName(), "double"));
    }

    public int[] getInts() {
        return Optional.ofNullable(ints)
            .orElseThrow(() -> createException(getName(), "int"));
    }

    public OptionalInt[] getOptionalInts() {
        return Optional.ofNullable(optionalInts)
                .orElseThrow(() -> createException(getName(), "optional ints"));
    }

    public OptionalDouble[] getOptionalDoubles() {
        return Optional.ofNullable(optionalDoubles)
            .orElseThrow(() -> createException(getName(), "optional doubles"));
    }

    public boolean[] getBooleans() {
        return Optional.ofNullable(booleans)
            .orElseThrow(() -> createException(getName(), "boolean"));
    }

    /**
     * The strings of a string series, or the decoded values of a dictionary-encoded string series (null for missing values).
     */
    public String[] getStrings() {
        if (dictionary != null) {
            String[] values = new String[ints.length];
            for (int i = 0; i < ints.length; i++) {
                values[i] = ints[i] == -1 ? null : dictionary.get(ints[i]);
            }
            return values;
        }
        return Optional.ofNullable(strings)
            .orElseThrow(() -> createException(getName(), "string"));
    }

    /**
     * Whether this is a dictionary-encoded string series: {@link #getInts()} then gives the codes and
     * {@link #getDictionary()} the values they refer to.
     */
    public boolean isDictionary() {
        return dictionary != null;
    }

    /**
     * The dictionary (distinct values) of a dictionary-encoded string series, whose codes are given by {@link #getInts()}.
     */
    public List<String> getDictionary() {
        return Optional.ofNullable(dictionary)
            .orElseThrow(() -> createException(getName(), "dictionary-encoded string"));
    }

    private PowsyblException createException(String name, String type) {
        return new PowsyblException(String.format("Series %s is not of type %s", name, type));
    }
}
