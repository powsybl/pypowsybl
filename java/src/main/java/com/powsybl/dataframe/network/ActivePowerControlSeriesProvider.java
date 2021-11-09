/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.google.auto.service.AutoService;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.extensions.ActivePowerControl;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionSeriesProvider.class)
public class ActivePowerControlSeriesProvider implements NetworkExtensionSeriesProvider  {
    @Override
    public String getExtensionName() {
        return "ActivePowerControl";
    }

    @Override
    public DataframeElementType getElementType() {
        return DataframeElementType.GENERATOR;
    }

    @Override
    public void addSeries(NetworkDataframeMapperBuilder builder) {
        NetworkDataframeMapperBuilder<Generator> genBuilder = builder;
        genBuilder.doubles("generator_droop", g -> {
            ActivePowerControl acpExt = g.getExtension(ActivePowerControl.class);
            return acpExt != null ? acpExt.getDroop() : Double.NaN;
        }).booleans("generator_participate", g -> {
            ActivePowerControl acpExt = g.getExtension(ActivePowerControl.class);
            return acpExt != null ? acpExt.isParticipate() : Boolean.FALSE;
        });
    }
}
