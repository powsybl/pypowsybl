/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.pgo;

import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class Main {

    private Main() {
    }

    private static void runConverters(Path dataDir) throws IOException {
        System.out.println("Run converters");
        for (int i = 0; i < 10; i++) {
            System.out.println("    iteration " + i);
            Network network = Network.read(dataDir.resolve("CGMES_v2_4_15_RealGridTestConfiguration_v2.zip"));
            try (var tempDir = new WorkingDirectory(Files.createTempDirectory(null), "pp-tmp", false)) {
                network.write("XIIDM", null, tempDir.toPath().resolve("CGMES_v2_4_15_RealGridTestConfiguration_v2.xiidm"));
                network.write("JIIDM", null, tempDir.toPath().resolve("CGMES_v2_4_15_RealGridTestConfiguration_v2.jiidm"));
                network.write("BIIDM", null, tempDir.toPath().resolve("CGMES_v2_4_15_RealGridTestConfiguration_v2.biidm"));
            }
        }
    }

    private static void runLoadFlow(Path dataDir) {
        System.out.println("Run load flow");
        Network network = Network.read(dataDir.resolve("CGMES_v2_4_15_RealGridTestConfiguration_v2.zip"));
        LoadFlow.Runner runner = LoadFlow.find("OpenLoadFlow");
        for (int i = 0; i < 50; i++) {
            System.out.println("    iteration " + i);
            LoadFlowParameters parameters = new LoadFlowParameters()
                    .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.DC_VALUES)
                    .setUseReactiveLimits(false);
            runner.run(network, parameters);
        }
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(args[0]);
        runConverters(dataDir);
        runLoadFlow(dataDir);
    }
}
