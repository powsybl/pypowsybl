/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.loadflow.validation;

import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.util.TwtData;
import com.powsybl.loadflow.validation.io.ValidationWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class InMemoryValidationWriter implements ValidationWriter {

    private final List<BranchValidationData> branchData = new ArrayList<>();
    private final List<GeneratorValidationData> generatorData = new ArrayList<>();
    private final List<BusValidationData> busData = new ArrayList<>();
    private final List<ShuntValidationData> shuntData = new ArrayList<>();
    private final List<SvcValidationData> svcData = new ArrayList<>();
    private final List<TwtValidationData> twtData = new ArrayList<>();
    private final List<T3wtValidationData> t3wtData = new ArrayList<>();

    public List<BranchValidationData> getBranchData() {
        return branchData;
    }

    public List<GeneratorValidationData> getGeneratorData() {
        return generatorData;
    }

    public List<BusValidationData> getBusData() {
        return busData;
    }

    public List<ShuntValidationData> getShuntData() {
        return shuntData;
    }

    public List<SvcValidationData> getSvcData() {
        return svcData;
    }

    public List<TwtValidationData> getTwtData() {
        return twtData;
    }

    public List<T3wtValidationData> getT3wtData() {
        return t3wtData;
    }

    @Override
    public void write(String branchId, double p1, double p1Calc, double q1, double q1Calc, double p2, double p2Calc, double q2, double q2Calc, double r, double x, double g1, double g2, double b1, double b2, double rho1, double rho2, double alpha1, double alpha2, double u1, double u2, double theta1, double theta2, double z, double y, double ksi, int phaseAngleClock, boolean connected1, boolean connected2, boolean mainComponent1, boolean mainComponent2, boolean validated) throws IOException {
        branchData.add(new BranchValidationData(branchId, p1, p1Calc, q1, q1Calc, p2, p2Calc, q2, q2Calc, r, x, g1, g2, b1, b2, rho1, rho2, alpha1, alpha2, u1, u2, theta1, theta2, z, y, ksi, phaseAngleClock, connected1, connected2, mainComponent1, mainComponent2, validated));
    }

    @Override
    public void write(String id, double p, double q, double v, double targetP, double targetQ, double targetV, double expectedP, boolean connected, boolean voltageRegulatorOn, double minP, double maxP, double minQ, double maxQ, boolean mainComponent, boolean validated) throws IOException {
        generatorData.add(new GeneratorValidationData(id, p, q, v, targetP, targetQ, targetV, expectedP, connected, voltageRegulatorOn, minP, maxP, minQ, maxQ, mainComponent, validated));
    }

    @Override
    public void write(String busId, double incomingP, double incomingQ, double loadP, double loadQ, double genP, double genQ, double batP, double batQ, double shuntP, double shuntQ, double svcP, double svcQ, double vscCSP, double vscCSQ, double lineP, double lineQ, double danglingLineP, double danglingLineQ, double twtP, double twtQ, double tltP, double tltQ, boolean mainComponent, boolean validated) throws IOException {
        busData.add(new BusValidationData(busId, incomingP, incomingQ, loadP, loadQ, genP, genQ, batP, batQ, shuntP, shuntQ, svcP, svcQ, vscCSP, vscCSQ, lineP, lineQ, danglingLineP, danglingLineQ, twtP, twtQ, tltP, tltQ, mainComponent, validated));
    }

    @Override
    public void write(String svcId, double p, double q, double vControlled, double vController, double nominalVcontroller, double reactivePowerSetpoint, double voltageSetpoint, boolean connected, StaticVarCompensator.RegulationMode regulationMode, double bMin, double bMax, boolean mainComponent, boolean validated) throws IOException {
        svcData.add(new SvcValidationData(svcId, p, q, vControlled, vController, nominalVcontroller, reactivePowerSetpoint, voltageSetpoint, connected, regulationMode, bMin, bMax, mainComponent, validated));
    }

    @Override
    public void write(String shuntId, double q, double expectedQ, double p, int currentSectionCount, int maximumSectionCount, double bPerSection, double v, boolean connected, double qMax, double nominalV, boolean mainComponent, boolean validated) throws IOException {
        shuntData.add(new ShuntValidationData(shuntId, q, expectedQ, p, currentSectionCount, maximumSectionCount, bPerSection, v, connected, qMax, nominalV, mainComponent, validated));
    }

    @Override
    public void write(String twtId, double error, double upIncrement, double downIncrement, double rho, double rhoPreviousStep, double rhoNextStep, int tapPosition, int lowTapPosition, int highTapPosition, double targetV, TwoSides regulatedSide, double v, boolean connected, boolean mainComponent, boolean validated) throws IOException {
        twtData.add(new TwtValidationData(twtId, error, upIncrement, downIncrement, rho, rhoPreviousStep, rhoNextStep, tapPosition, lowTapPosition, highTapPosition, targetV, regulatedSide, v, connected, mainComponent, validated));
    }

    @Override
    public void write(String twtId, TwtData twtData, boolean validated) throws IOException {
        t3wtData.add(new T3wtValidationData(twtId, twtData, validated));
    }

    @Override
    public void setValidationCompleted() {
        //noop
    }

    @Override
    public void close() throws IOException {
        //noop
    }
}
