package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.io.*;

public class RaoContext {

    public RaoContext() {
    }

    private RaoResult results;

    private Crac crac;

    private ZonalData<SensitivityVariableSet> glsks;

    private RaoParameters parameters;

    public void run(Network network, InputStream cracStream, InputStream raoParametersStream, InputStream glsksStream) {
        try {
            crac = Crac.read("crac.json", cracStream, network);
        } catch (IOException e) {
            throw new PowsyblException("Cannot read provided crac data : " + e.getMessage());
        }
        glsks = UcteGlskDocument.importGlsk(glsksStream)
            .getZonalGlsks(network);
        RaoInput input = RaoInput.build(network, crac)
            .withGlskProvider(glsks)
            .build();
        parameters = JsonRaoParameters.read(raoParametersStream);
        results = Rao.run(input, parameters);
        System.out.println("Rao done");
    }

    public RaoResult getResults() {
        return results;
    }

    public Crac getCrac() {
        return crac;
    }

    public RaoParameters getParameters() {
        return parameters;
    }

    public ZonalData<SensitivityVariableSet> getGlsks() {
        return glsks;
    }
}
