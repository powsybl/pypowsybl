/**
 * Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps distinct strings to consecutive int codes, in order of first appearance, for dictionary-encoded string series.
 * A null value is mapped like an empty string.
 *
 * @author Nico Westerbeck {@literal <nico.westerbeck at 50hertz.com>}
 */
public class StringDictionary {

    private final Map<String, Integer> codes = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    /**
     * Returns the code of a value, registering it when it is seen for the first time.
     *
     * @param value the value, null being encoded like an empty string
     * @return the code of the value: its index in {@link #getValues()}
     */
    public int code(String value) {
        return codes.computeIfAbsent(value == null ? "" : value, key -> {
            values.add(key);
            return values.size() - 1;
        });
    }

    /**
     * The distinct values, indexed by their code: the dictionary of the encoded series.
     */
    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }
}
