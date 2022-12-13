package com.powsybl.dataframe.dynamic.adders;

public class GeneratorSynchronousFourWindingsAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping
                .addGeneratorSynchronousFourWindings(staticId, parameterSetId);
    }

}
