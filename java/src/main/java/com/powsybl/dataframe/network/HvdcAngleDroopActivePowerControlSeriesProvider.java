/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.google.auto.service.AutoService;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionSeriesProvider.class)
public class HvdcAngleDroopActivePowerControlSeriesProvider implements NetworkExtensionSeriesProvider {

    public static final String EXTENSION_NAME = "HvdcAngleDroopActivePowerControl";
    public static final String EXTENSION_SEPARATOR = "_";

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public DataframeElementType getElementType() {
        return DataframeElementType.HVDC_LINE;
    }

    @Override
    public void addSeries(NetworkDataframeMapperBuilder builder) {
        NetworkDataframeMapperBuilder<HvdcLine> sBuilder = builder;
        sBuilder.booleans(EXTENSION_NAME + EXTENSION_SEPARATOR + "available", item ->
            item.getExtension(HvdcOperatorActivePowerRange.class) != null
        ).doubles(EXTENSION_NAME + EXTENSION_SEPARATOR + "droop", item -> {
            HvdcAngleDroopActivePowerControl itemExtension = item.getExtension(HvdcAngleDroopActivePowerControl.class);
            return itemExtension != null ? itemExtension.getDroop() : Double.NaN;
        }).doubles(EXTENSION_NAME + EXTENSION_SEPARATOR + "P0", item -> {
            HvdcAngleDroopActivePowerControl itemExtension = item.getExtension(HvdcAngleDroopActivePowerControl.class);
            return itemExtension != null ? itemExtension.getP0() : Double.NaN;
        }).booleans(EXTENSION_NAME + EXTENSION_SEPARATOR + "isEnabled", item -> {
            HvdcAngleDroopActivePowerControl itemExtension = item.getExtension(HvdcAngleDroopActivePowerControl.class);
            return itemExtension != null ? itemExtension.isEnabled() : false;
        });
    }
}
