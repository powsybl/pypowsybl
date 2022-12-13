package com.powsybl.dataframe.dynamic.adders;

public class GeneratorSynchronousFourWindingsProportionalRegulationsAdder extends AbstractBlackBoxAdder {

    @Override
    protected AddBlackBoxToModelMapping getAddBlackBoxToModelMapping() {
        return (modelMapping, staticId, parameterSetId) -> modelMapping
                .addGeneratorSynchronousFourWindingsProportionalRegulations(staticId, parameterSetId);
    }

}
