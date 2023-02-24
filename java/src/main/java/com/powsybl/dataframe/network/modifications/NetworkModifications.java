/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.modifications;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType.*;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public final class NetworkModifications {

    private NetworkModifications() {
    }

    private static final Map<DataframeNetworkModificationType, NetworkModification> MODIFICATION = Map.ofEntries(
            Map.entry(VOLTAGE_LEVEL_TOPOLOGY_CREATION, new VoltageLevelTopologyCreation()),
            Map.entry(CREATE_COUPLING_DEVICE, new CouplingDeviceCreation()),
            Map.entry(CREATE_FEEDER_BAY, new CreateFeederBay()),
            Map.entry(CREATE_LINE_FEEDER, new CreateLineFeeder()),
            Map.entry(CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER, new CreateTwoWindingsTransformer()),
            Map.entry(CREATE_LINE_ON_LINE, new CreateLineOnLine()),
            Map.entry(REVERT_CREATE_LINE_ON_LINE, new RevertCreateLineOnLine()),
            Map.entry(CONNECT_VOLTAGE_LEVEL_ON_LINE, new ConnectVoltageLevelOnLine()),
            Map.entry(REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE, new RevertConnectVoltageLevelOnLine()),
            Map.entry(REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE, new ReplaceTeePointByVoltageLevelOnLine())
    );

    public static NetworkModification getModification(DataframeNetworkModificationType type) {
        return MODIFICATION.get(type);
    }

    public static void applyModification(DataframeNetworkModificationType type, Network network, List<UpdatingDataframe> dataframe, boolean throwException, ReporterModel reporter) {
        NetworkModification modification = MODIFICATION.get(type);
        if (modification == null) {
            throw new PowsyblException("Creation not implemented for type " + type.name());
        }
        modification.applyModification(network, dataframe, throwException, reporter);
    }

}
