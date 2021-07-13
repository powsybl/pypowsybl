/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.dataframe.impl.Series;
import com.powsybl.dataframe.loadflow.BusValidationWriter;
import com.powsybl.dataframe.loadflow.GeneratorValidationWriter;
import com.powsybl.dataframe.loadflow.Validations;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class PyPowsyblLoadFlowApiLibTest {

    @Test
    void test() {
        final Network network = IeeeCdfNetworkFactory.create9();
        LoadFlow.Runner runner = LoadFlow.find("OpenLoadFlow");
        runner.run(network, new LoadFlowParameters());
        BusValidationWriter buses = (BusValidationWriter) PyPowsyblLoadFlowApiLib.createLoadFlowValidationWriter(network, PyPowsyblApiHeader.ElementType.BUS);
        Assertions.assertThat(Dataframes.createSeries(Validations.busValidationsMapper(), buses))
                .extracting(Series::getName)
                .contains("id", "incoming_p");

        GeneratorValidationWriter gens = (GeneratorValidationWriter) PyPowsyblLoadFlowApiLib.createLoadFlowValidationWriter(network, PyPowsyblApiHeader.ElementType.GENERATOR);
        Assertions.assertThat(Dataframes.createSeries(Validations.generatorValidationsMapper(), gens))
                .extracting(Series::getName)
                .contains("id", "p");
    }
}
