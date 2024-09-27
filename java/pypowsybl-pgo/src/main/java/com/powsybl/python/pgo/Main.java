package com.powsybl.python.pgo;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Network network = EurostagTutorialExample1Factory.create();
    }
}
