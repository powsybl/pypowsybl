package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;

public class RaoContext {

    public RaoContext() {
    }

    private RaoResult results;

    private Crac crac;

    private ZonalData<SensitivityVariableSet> glsks;

    public void setCrac(Crac crac) {
        this.crac = crac;
    }

    public void setGlsks(ZonalData<SensitivityVariableSet> glsks) {
        this.glsks = glsks;
    }

    public void run(Network network, RaoParameters parameters) {
        if (crac == null) {
            throw new PowsyblException("Providing a crac source is mandatory to run a Rao.");
        }
        RaoInput.RaoInputBuilder inputBuilder = RaoInput.build(network, crac);
        if (glsks != null) {
            inputBuilder.withGlskProvider(glsks);
        }
        results = Rao.run(inputBuilder.build(), parameters);
    }

    public RaoResult getResults() {
        return results;
    }

    public Crac getCrac() {
        return crac;
    }

    public ZonalData<SensitivityVariableSet> getGlsks() {
        return glsks;
    }
}
