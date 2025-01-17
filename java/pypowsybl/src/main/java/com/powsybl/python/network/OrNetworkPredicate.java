/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.reducer.NetworkPredicate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
public class OrNetworkPredicate implements NetworkPredicate {

    private final List<NetworkPredicate> predicateList;

    public OrNetworkPredicate(List<NetworkPredicate> predicateList) {
        Objects.requireNonNull(predicateList);
        this.predicateList = Collections.unmodifiableList(predicateList);
    }

    @Override
    public boolean test(Substation substation) {
        return predicateList.stream().anyMatch(p -> p.test(substation));
    }

    @Override
    public boolean test(VoltageLevel voltageLevel) {
        return predicateList.stream().anyMatch(p -> p.test(voltageLevel));
    }
}
