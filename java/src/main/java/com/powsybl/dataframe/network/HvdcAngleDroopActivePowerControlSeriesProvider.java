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

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionSeriesProvider.class)
public class HvdcAngleDroopActivePowerControlSeriesProvider implements NetworkExtensionSeriesProvider {
    @Override
    public String getExtensionName() {
        return "HvdcAngleDroopActivePowerControl";
    }

    @Override
    public DataframeElementType getElementType() {
        return DataframeElementType.HVDC_LINE;
    }

    @Override
    public void addSeries(NetworkDataframeMapperBuilder builder) {
        NetworkDataframeMapperBuilder<HvdcLine> sBuilder = builder;
        sBuilder.doubles("hvdc_droop", l -> {
            HvdcAngleDroopActivePowerControl acpExt = l.getExtension(HvdcAngleDroopActivePowerControl.class);
            return acpExt != null ? acpExt.getDroop() : Double.NaN;
        }).doubles("hvdc_P0", l -> {
            HvdcAngleDroopActivePowerControl acpExt = l.getExtension(HvdcAngleDroopActivePowerControl.class);
            return acpExt != null ? acpExt.getP0() : Double.NaN;
        }).booleans("hvdc_isEnabled", l -> {
            HvdcAngleDroopActivePowerControl acpExt = l.getExtension(HvdcAngleDroopActivePowerControl.class);
            return acpExt != null ? acpExt.isEnabled() : false;
        });
    }
}
