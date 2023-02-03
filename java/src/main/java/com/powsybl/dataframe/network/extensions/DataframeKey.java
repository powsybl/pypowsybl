/**
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL-2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Hugo Kulesza <hugo.kulesza@rte-france.com>
 */
public class DataframeKey {
    private String extensionName;
    private Optional<String> tableName;

    private int hashCode;

    public DataframeKey(String extensionName, Optional<String> tableName) {
        this.extensionName = extensionName;
        this.tableName = tableName;
        this.hashCode = Objects.hash(extensionName, tableName);
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getTableName() {
        return tableName.orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataframeKey)) {
            return false;
        }
        DataframeKey key = (DataframeKey) o;
        return key.tableName.equals(this.tableName) && key.extensionName.equals(this.extensionName);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
