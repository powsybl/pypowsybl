package com.powsybl.dataframe.dynamic.adders;

public class GeneratorSynchronousThreeWindingsAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping
                .addGeneratorSynchronousThreeWindings(staticId, parameterSetId);
    }

}
