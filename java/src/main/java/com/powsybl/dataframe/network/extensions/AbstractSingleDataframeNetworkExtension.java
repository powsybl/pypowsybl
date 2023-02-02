/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.dataframe.network.NetworkDataframeMapper;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * @author Hugo Kulesza <hugo.kulesza@rte-france.com>
 */
public abstract class AbstractSingleDataframeNetworkExtension {

    public List<Optional<String>> getExtensionTableNames() {
        return new ArrayList<>();
    }

    public Map<Optional<String>, NetworkDataframeMapper> createMappers() {
        return Map.of(Optional.empty(), createMapper());
    }

    public abstract NetworkDataframeMapper createMapper();

}
