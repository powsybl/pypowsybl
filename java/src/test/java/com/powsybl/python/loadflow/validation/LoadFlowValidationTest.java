/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.loadflow.validation;

import com.powsybl.dataframe.impl.Series;
import com.powsybl.dataframe.loadflow.validation.InMemoryValidationWriter;
import com.powsybl.dataframe.loadflow.validation.Validations;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.network.Dataframes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class LoadFlowValidationTest {

    // Note: disabled on windows because it seems to make cmake fail
    //       because of the presence of "error" in logs ...
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void test() {
        final Network network = IeeeCdfNetworkFactory.create9();
        LoadFlow.Runner runner = LoadFlow.find("OpenLoadFlow");
        runner.run(network, new LoadFlowParameters());
        ValidationConfig validationConfig = LoadFlowValidationCFunctions.createValidationConfig();
        InMemoryValidationWriter busWriter = LoadFlowValidationCFunctions.createLoadFlowValidationWriter(network, PyPowsyblApiHeader.ValidationType.BUSES, validationConfig);
        Assertions.assertThat(Dataframes.createSeries(Validations.busValidationsMapper(), busWriter.getBusData()))
                .extracting(Series::getName)
                .contains("id", "incoming_p");

        InMemoryValidationWriter genWriter = LoadFlowValidationCFunctions.createLoadFlowValidationWriter(network, PyPowsyblApiHeader.ValidationType.GENERATORS);
        Assertions.assertThat(Dataframes.createSeries(Validations.generatorValidationsMapper(), genWriter.getGeneratorData()))
                .extracting(Series::getName)
                .contains("id", "p");
    }
}
