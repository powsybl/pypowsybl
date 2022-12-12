/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.DataframeNetworkModificationType;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.SwitchKind;

import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.DataframeNetworkModificationType.VOLTAGE_LEVEL_TOPOLOGY_CREATION;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public final class NetworkModifications {

    private NetworkModifications() {
    }

    private static final Map<DataframeNetworkModificationType, NetworkModification> MODIFICATION = Map.ofEntries(
            Map.entry(VOLTAGE_LEVEL_TOPOLOGY_CREATION, new VoltageLevelTopologyCreation())
    );

    public static NetworkModification getModification(DataframeNetworkModificationType type) {
        return MODIFICATION.get(type);
    }

    public static void applyModification(DataframeNetworkModificationType type, Network network, UpdatingDataframe dataframe, List<SwitchKind> switchKinds, boolean throwException, ReporterModel reporter) {
        NetworkModification modification = MODIFICATION.get(type);
        if (modification == null) {
            throw new PowsyblException("Creation not implemented for type " + type.name());
        }
        modification.applyModification(network, dataframe, switchKinds, throwException, reporter);
    }

}
