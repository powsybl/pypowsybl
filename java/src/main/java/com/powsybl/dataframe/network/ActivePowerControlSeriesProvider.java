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

    public static final String EXTENSION_NAME = "ActivePowerControl";
    public static final String EXTENSION_SEPARATOR = "_";

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public DataframeElementType getElementType() {
        return DataframeElementType.GENERATOR;
    }

    @Override
    public void addSeries(NetworkDataframeMapperBuilder builder) {
        NetworkDataframeMapperBuilder<Generator> sBuilder = builder;
        sBuilder.booleans(EXTENSION_NAME + EXTENSION_SEPARATOR + "available", item ->
            item.getExtension(ActivePowerControl.class) != null
        ).doubles(EXTENSION_NAME + EXTENSION_SEPARATOR + "droop", item -> {
            ActivePowerControl itemExtension = item.getExtension(ActivePowerControl.class);
            return itemExtension != null ? itemExtension.getDroop() : Double.NaN;
        }).booleans(EXTENSION_NAME + EXTENSION_SEPARATOR + "participate", item -> {
            ActivePowerControl itemExtension = item.getExtension(ActivePowerControl.class);
            return itemExtension != null ? itemExtension.isParticipate() : Boolean.FALSE;
        });
    }
}
