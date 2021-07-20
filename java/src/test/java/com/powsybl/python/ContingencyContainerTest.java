/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.contingency.*;
import com.powsybl.iidm.network.test.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class ContingencyContainerTest {

    @Test
    void testContingencyConverter() {
        var container1 = new SecurityAnalysisContext();
        var network = EurostagTutorialExample1Factory.create();
        container1.addContingency("l1", List.of("NHV1_NHV2_1"));
        container1.addContingency("gen", List.of("GEN"));

        List<Contingency> contingencies = container1.createContingencies(network);
        assertThat(contingencies).hasSize(2);
        assertThat(contingencies.get(1).getElements().get(0)).isInstanceOf(BranchContingency.class);
        assertThat(contingencies.get(0).getElements().get(0)).isInstanceOf(GeneratorContingency.class);

        network = HvdcTestNetwork.createVsc();
        var container2 = new SecurityAnalysisContext();
        container2.addContingency("bbs", List.of("BBS1", "L"));
        assertThat(container2.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getElements().get(0)).isInstanceOf(BusbarSectionContingency.class);
                    assertThat(c.getElements().get(1)).isInstanceOf(HvdcLineContingency.class);
                });

        network = DanglingLineNetworkFactory.create();
        var container3 = new SecurityAnalysisContext();
        container3.addContingency("ddl", List.of("DL"));
        assertThat(container3.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(DanglingLineContingency.class));

        network = SvcTestCaseFactory.create();
        var container4 = new SecurityAnalysisContext();
        container4.addContingency("svc", List.of("SVC2"));
        assertThat(container4.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(StaticVarCompensatorContingency.class));

        network = ShuntTestCaseFactory.create();
        var container5 = new SecurityAnalysisContext();
        container5.addContingency("shunt", List.of("SHUNT"));
        assertThat(container5.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(ShuntCompensatorContingency.class));

        network = ThreeWindingsTransformerNetworkFactory.create();
        var container6 = new SecurityAnalysisContext();
        container6.addContingency("twt3", List.of("3WT"));
        assertThat(container6.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(ThreeWindingsTransformerContingency.class));

        var container7 = new SecurityAnalysisContext();
        container7.addContingency("exception", List.of("not_exists_id"));
        assertThatThrownBy(() -> container7.createContingencies(EurostagTutorialExample1Factory.create()))
                .hasMessageContaining("Element 'not_exists_id' not found");
    }

}
