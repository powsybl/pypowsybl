/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.Monitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class RaoContext {

    public RaoContext() {
    }

    private Crac crac;

    private GlskDocument glsks;

    public void setCrac(Crac crac) {
        this.crac = crac;
    }

    public void setGlsks(GlskDocument glskDocument) {
        this.glsks = glskDocument;
    }

    public RaoResult run(Network network, RaoParameters parameters) {
        if (crac == null) {
            throw new PowsyblException("Providing a crac source is mandatory to run a Rao.");
        }
        RaoInput.RaoInputBuilder inputBuilder = RaoInput.build(network, crac);
        if (glsks != null) {
            inputBuilder.withGlskProvider(glsks.getZonalGlsks(network));
        }
        RaoResult raoResult = Rao.run(inputBuilder.build(), parameters);
        // perform round trip on RAO result to always use the same implementation
        return roundTrip(raoResult);
    }

    private RaoResult roundTrip(RaoResult raoResult) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        raoResult.write("JSON", crac, properties, outputStream);
        try {
            return RaoResult.read(new ByteArrayInputStream(outputStream.toByteArray()), crac);
        } catch (IOException e) {
            throw new OpenRaoException("An error occurred during the round trip on RAO Result.");
        }
    }

    public RaoResultWithVoltageMonitoring runVoltageMonitoring(Network network, RaoResult resultIn, String provider, LoadFlowParameters parameters) {
        Monitoring raoMonitoring = new Monitoring(provider, parameters);
        MonitoringInput inputs = MonitoringInput.buildWithVoltage(network, crac, resultIn).build();
        MonitoringResult monitoringResult = raoMonitoring.runMonitoring(inputs, 1);
        return new RaoResultWithVoltageMonitoring(resultIn, monitoringResult);
    }

    public RaoResultWithAngleMonitoring runAngleMonitoring(Network network, RaoResult resultIn, String provider, LoadFlowParameters parameters) {
        Monitoring raoMonitoring = new Monitoring(provider, parameters);
        MonitoringInput inputs = MonitoringInput.buildWithAngle(network, crac, resultIn, glsks.getZonalScalable(network)).build();
        MonitoringResult monitoringResult = raoMonitoring.runMonitoring(inputs, 1);
        return new RaoResultWithAngleMonitoring(resultIn, monitoringResult);
    }

    public Crac getCrac() {
        return crac;
    }

    public GlskDocument getGlsks() {
        return glsks;
    }
}
