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
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.monitoring.Monitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.marmot.Marmot;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
public class RaoContext {

    public RaoContext() {
    }

    private Crac crac;

    private GlskDocument loopFlowGlsk;

    private GlskDocument monitoringGlsk;

    public void setCrac(Crac crac) {
        this.crac = crac;
    }

    public void setLoopFlowGlsk(GlskDocument glskDocument) {
        this.loopFlowGlsk = glskDocument;
    }

    public void setMonitoringGlsk(GlskDocument glskDocument) {
        this.monitoringGlsk = glskDocument;
    }

    public RaoResult run(Network network, RaoParameters parameters, String raoProvider) {
        if (crac == null) {
            throw new PowsyblException("Providing a crac source is mandatory to run a Rao.");
        }
        RaoInput.RaoInputBuilder inputBuilder = RaoInput.build(network, crac);
        if (loopFlowGlsk != null) {
            inputBuilder.withGlskProvider(loopFlowGlsk.getZonalGlsks(network));
        }
        return Rao.find(raoProvider).run(inputBuilder.build(), parameters);
    }

    public static RaoResult runMarmot(List<OffsetDateTime> timestamps, List<Network> networks, List<Crac> cracs, RaoParameters parameters, TimeCoupledConstraints timeCoupledConstraints) {
        Map<OffsetDateTime, RaoInput> inputs = new HashMap<>();
        for (int i = 0; i < timestamps.size(); ++i) {
            Network n = networks.get(i);
            Crac crac = cracs.get(i);
            inputs.put(timestamps.get(i), RaoInput.build(n, crac).build());
        }
        TimeCoupledRaoInput timeCoupledInput = new TimeCoupledRaoInput(new TemporalDataImpl<>(inputs), timeCoupledConstraints);
        return new Marmot().run(timeCoupledInput, parameters).join();
    }

    public RaoResultWithVoltageMonitoring runVoltageMonitoring(Network network, RaoResult resultIn, String provider, LoadFlowParameters parameters) {
        Monitoring raoMonitoring = new Monitoring(provider, parameters);
        MonitoringInput inputs = MonitoringInput.buildWithVoltage(network, crac, resultIn).build();
        MonitoringResult monitoringResult = raoMonitoring.runMonitoring(inputs, 1);
        return new RaoResultWithVoltageMonitoring(resultIn, monitoringResult);
    }

    public RaoResultWithAngleMonitoring runAngleMonitoring(Network network, RaoResult resultIn, String provider, LoadFlowParameters parameters) {
        Monitoring raoMonitoring = new Monitoring(provider, parameters);
        MonitoringInput inputs;
        if (monitoringGlsk != null) {
            inputs = MonitoringInput.buildWithAngle(network, crac, resultIn, monitoringGlsk.getZonalScalable(network)).build();
        } else {
            throw new PowsyblException("Providing glsk for angle monitoring is mandatory.");
        }
        MonitoringResult monitoringResult = raoMonitoring.runMonitoring(inputs, 1);
        return new RaoResultWithAngleMonitoring(resultIn, monitoringResult);
    }

    public Crac getCrac() {
        return crac;
    }
}
