package com.powsybl.dataframe.dynamic.adders;

public class OneTransformerLoadAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping.addOneTransformerLoad(staticId, parameterSetId);
    }

}
