/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.contingency;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
class ContingencyContainerTest {

    protected FileSystem fileSystem;

    @Test
    void testContingencyConverter() {
        var container1 = new ContingencyContainerImpl();
        var network = EurostagTutorialExample1Factory.create();
        container1.addContingency("l1", List.of("NHV1_NHV2_1"));
        container1.addContingency("gen", List.of("GEN"));

        List<Contingency> contingencies = container1.createContingencies(network);
        assertThat(contingencies).hasSize(2);
        assertThat(contingencies.get(1).getElements().get(0)).isInstanceOf(LineContingency.class);
        assertThat(contingencies.get(0).getElements().get(0)).isInstanceOf(GeneratorContingency.class);

        network = HvdcTestNetwork.createVsc();
        var container2 = new ContingencyContainerImpl();
        container2.addContingency("bbs", List.of("BBS1", "L"));
        assertThat(container2.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getElements().get(0)).isInstanceOf(BusbarSectionContingency.class);
                    assertThat(c.getElements().get(1)).isInstanceOf(HvdcLineContingency.class);
                });

        network = DanglingLineNetworkFactory.create();
        var container3 = new ContingencyContainerImpl();
        container3.addContingency("ddl", List.of("DL"));
        assertThat(container3.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(DanglingLineContingency.class));

        network = SvcTestCaseFactory.create();
        var container4 = new ContingencyContainerImpl();
        container4.addContingency("svc", List.of("SVC2"));
        assertThat(container4.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(StaticVarCompensatorContingency.class));

        network = ShuntTestCaseFactory.create();
        var container5 = new ContingencyContainerImpl();
        container5.addContingency("shunt", List.of("SHUNT"));
        assertThat(container5.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(ShuntCompensatorContingency.class));

        network = ThreeWindingsTransformerNetworkFactory.create();
        var container6 = new ContingencyContainerImpl();
        container6.addContingency("twt3", List.of("3WT"));
        assertThat(container6.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(ThreeWindingsTransformerContingency.class));

        var container7 = new ContingencyContainerImpl();
        container7.addContingency("exception", List.of("not_exists_id"));
        assertThatThrownBy(() -> container7.createContingencies(EurostagTutorialExample1Factory.create()))
                .hasMessageContaining("Element 'not_exists_id' not found");

        network = FourSubstationsNodeBreakerFactory.create();
        var container8 = new ContingencyContainerImpl();
        container8.addContingency("LD1", List.of("LD1"));
        assertThat(container8.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(LoadContingency.class));

        var container9 = new ContingencyContainerImpl();
        container9.addContingency("S1VL2_BBS2_LD4_DISCONNECTOR", List.of("S1VL2_BBS2_LD4_DISCONNECTOR"));
        assertThat(container9.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(SwitchContingency.class));

        network = BatteryNetworkFactory.create();
        var container10 = new ContingencyContainerImpl();
        container10.addContingency("BAT", List.of("BAT"));
        assertThat(container10.createContingencies(network))
                .hasOnlyOneElementSatisfying(c -> assertThat(c.getElements().get(0)).isInstanceOf(BatteryContingency.class));
    }

    @Test
    void testToAddContingencyFromJsonFile() throws IOException {
        ContingencyContainerImpl container = new ContingencyContainerImpl();
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        Files.copy(getClass().getResourceAsStream("/contingencies.json"), fileSystem.getPath("/contingencies.json"));
        container.addContingencyFromJsonFile(fileSystem.getPath("/contingencies.json"));
        List<Contingency> contingencies = container.createContingencies(network);

        assertFalse(contingencies.isEmpty());
        assertEquals(2, contingencies.size());
        assertEquals("contingency", contingencies.get(0).getId());
        assertEquals("contingency2", contingencies.get(1).getId());
    }
}
