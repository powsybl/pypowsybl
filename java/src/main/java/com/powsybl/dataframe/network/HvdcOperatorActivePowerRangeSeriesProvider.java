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
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionSeriesProvider.class)
public class HvdcOperatorActivePowerRangeSeriesProvider implements NetworkExtensionSeriesProvider {
    @Override
    public String getExtensionName() {
        return "hvdcOperatorActivePowerRange";
    }

    @Override
    public DataframeElementType getElementType() {
        return DataframeElementType.HVDC_LINE;
    }

    @Override
    public void addSeries(NetworkDataframeMapperBuilder builder) {
        NetworkDataframeMapperBuilder<HvdcLine> sBuilder = builder;
        sBuilder.doubles("hvdc_OprFromCS1toCS2", l -> {
            HvdcOperatorActivePowerRange acpExt = l.getExtension(HvdcOperatorActivePowerRange.class);
            return acpExt != null ? acpExt.getOprFromCS1toCS2() : Double.NaN;
        }).doubles("hvdc_OprFromCS2toCS1", l -> {
            HvdcOperatorActivePowerRange acpExt = l.getExtension(HvdcOperatorActivePowerRange.class);
            return acpExt != null ? acpExt.getOprFromCS2toCS1() : Double.NaN;
        });
    }
}
