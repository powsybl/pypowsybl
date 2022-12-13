package com.powsybl.dataframe.dynamic.adders;

public class AlphaBetaLoadAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping.addAlphaBetaLoad(staticId, parameterSetId);
    }

}
