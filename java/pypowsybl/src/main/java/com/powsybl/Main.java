package com.powsybl;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        final Network network = IeeeCdfNetworkFactory.create9();
        LoadFlow.Runner runner = LoadFlow.find("OpenLoadFlow");

        LoadFlowParameters parameters = new LoadFlowParameters().setUseReactiveLimits(true)
                .setDistributedSlack(true);
        OpenLoadFlowParameters.create(parameters)
                .setAcSolverType("KNITRO");
        runner.run(network, parameters);
    }
}
