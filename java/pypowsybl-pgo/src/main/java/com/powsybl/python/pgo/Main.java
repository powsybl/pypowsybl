package com.powsybl.python.pgo;

import com.powsybl.iidm.network.Network;

import java.nio.file.Path;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path dataDir = Path.of(args[0]);
        for (int i = 0; i < 10; i++) {
            Network network = Network.read(dataDir.resolve("CGMES_v2_4_15_RealGridTestConfiguration_v2.zip"));
        }
    }
}
