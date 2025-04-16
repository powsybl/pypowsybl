/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
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
