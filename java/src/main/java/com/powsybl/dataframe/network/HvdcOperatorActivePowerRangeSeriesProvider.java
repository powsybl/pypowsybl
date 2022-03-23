/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionSeriesProvider.class)
public class HvdcOperatorActivePowerRangeSeriesProvider implements NetworkExtensionSeriesProvider {

    public static final String EXTENSION_NAME = "hvdcOperatorActivePowerRange";

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public DataframeElementType getElementType() {
        return DataframeElementType.HVDC_LINE;
    }

    private HvdcOperatorActivePowerRange getExtensionOrThrow(HvdcLine item) {
        HvdcOperatorActivePowerRange itemExtension = item.getExtension(HvdcOperatorActivePowerRange.class);
        if (itemExtension == null) {
            throw new PowsyblException("extension " + " '" +  EXTENSION_NAME + "' not found");
        } else {
            return itemExtension;
        }
    }

    @Override
    public void addSeries(NetworkDataframeMapperBuilder builder) {
        NetworkDataframeMapperBuilder<HvdcLine> sBuilder = builder;
        sBuilder.doubles("OprFromCS1toCS2", item -> getExtensionOrThrow(item).getOprFromCS1toCS2())
                .doubles("OprFromCS2toCS1", item -> getExtensionOrThrow(item).getOprFromCS2toCS1());
    }
}
