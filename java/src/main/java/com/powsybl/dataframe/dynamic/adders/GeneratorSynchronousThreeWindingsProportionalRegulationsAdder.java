package com.powsybl.dataframe.dynamic.adders;

public class GeneratorSynchronousThreeWindingsProportionalRegulationsAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping
                .addGeneratorSynchronousThreeWindingsProportionalRegulations(staticId, parameterSetId);
    }

}
