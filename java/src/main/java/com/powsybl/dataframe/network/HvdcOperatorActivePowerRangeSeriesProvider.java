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
