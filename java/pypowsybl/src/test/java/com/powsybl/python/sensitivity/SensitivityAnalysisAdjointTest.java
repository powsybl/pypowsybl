/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JVM-level validation of the pypowsybl adjoint plumbing ({@link SensitivityAnalysisContext#runAdjoint}):
 * that it builds the factors + cotangent map from its factor matrices, dispatches to the OpenLoadFlow
 * {@code runAdjoint}, and maps the result back correctly. Self-consistent check: with a unit cotangent on a
 * single monitored function, {@code θ̄ = Sᵀ·e = S}, so the adjoint gradient must equal the (pypowsybl)
 * forward sensitivity computed from the same declared factor matrix. Runs entirely in the JVM — it does not
 * touch the native (createGradient/UnmanagedMemory) path.
 */
class SensitivityAnalysisAdjointTest {

    @Test
    void runAdjointReproducesTheForwardSensitivityOnIeee14() {
        Network network = IeeeCdfNetworkFactory.create14();
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);

        // scaling-free dP_branch1/dP_injection factors (function base == variable base) on IEEE-14
        String branch = "L1-2-1";
        List<String> gens = List.of("B2-G", "B3-G", "B6-G");
        SensitivityAnalysisContext ctx = new SensitivityAnalysisContext();
        ctx.addFactorMatrix("m", List.of(branch), gens, List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.INJECTION_ACTIVE_POWER);

        // warm the OpenLoadFlow network cache that runAdjoint reuses
        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());

        // reverse mode: unit cotangent on the single monitored branch flow ⇒ θ̄ = dP_branch1/dP_gen
        double[] thetaBar = ctx.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow")
                .gradientValues("m");

        // forward mode through the same context: S row is baseCaseValues (rows = gens, single column)
        double[] forwardS = ctx.run(network, parameters, "OpenLoadFlow", ReportNode.NO_OP).getBaseCaseValues();

        double maxAbs = 0;
        for (int i = 0; i < gens.size(); i++) {
            double s = forwardS[i];
            double theta = thetaBar[i];
            assertEquals(s, theta, 1e-4 * (Math.abs(s) + 1e-3), "runAdjoint vs forward S for " + gens.get(i));
            maxAbs = Math.max(maxAbs, Math.abs(theta));
        }
        assertTrue(maxAbs > 0.1, "gradient must be non-trivial, got max |θ̄| = " + maxAbs);
    }

    /**
     * One BUS_VOLTAGE column monitored under {@code busId}, differentiated against a generator target voltage:
     * returns the adjoint gradient (unit cotangent) and the forward sensitivity from the SAME context, so the
     * two must agree whatever id convention {@code busId} uses.
     */
    private static double[] adjointAndForwardBusVoltage(Network network, String busId, String generatorId) {
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);

        SensitivityAnalysisContext ctx = new SensitivityAnalysisContext();
        ctx.addFactorMatrix("m", List.of(busId), List.of(generatorId), List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.BUS_TARGET_VOLTAGE);

        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());
        double theta = ctx.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow").gradientValues("m")[0];
        assertFalse(Double.isNaN(theta), "no gradient for " + generatorId + " monitored under bus id '" + busId + "'");
        double forward = ctx.run(network, parameters, "OpenLoadFlow", ReportNode.NO_OP).getBaseCaseValues()[0];
        return new double[] {theta, forward};
    }

    @Test
    void runAdjointAcceptsBusBreakerBusIdsLikeTheForwardPath() {
        // The forward run() resolves a BUS_VOLTAGE function id (bus-view, bus-breaker or busbar-section id) to the
        // bus-view bus before handing it to OpenLoadFlow. runAdjoint must do the same on BOTH sides of the OLF
        // boundary (block function list AND cotangent key): keying the cotangent by the resolved id while the
        // block carried the caller's id made every bus-breaker id an orphan cotangent ("key no monitored function").
        // IEEE-14: bus-breaker id "B10" and bus-view id "VL10_0" are the same bus.
        double[] viaBusBreakerId = adjointAndForwardBusVoltage(IeeeCdfNetworkFactory.create14(), "B10", "B6-G");
        double[] viaBusViewId = adjointAndForwardBusVoltage(IeeeCdfNetworkFactory.create14(), "VL10_0", "B6-G");
        assertEquals(viaBusViewId[0], viaBusBreakerId[0], 1e-12, "adjoint gradient must not depend on the bus id convention");
        assertEquals(viaBusBreakerId[1], viaBusBreakerId[0], 1e-4 * (Math.abs(viaBusBreakerId[1]) + 1e-3), "adjoint vs forward, bus-breaker id");
        assertTrue(Math.abs(viaBusBreakerId[0]) > 1e-3, "gradient must be non-trivial, got " + viaBusBreakerId[0]);
    }

    @Test
    void runAdjointAcceptsNodeBreakerBusbarSectionIdsLikeTheForwardPath() {
        // Node-breaker topology: a busbar section id ("S2VL1_BBS") resolves to its bus-view bus ("S2VL1_0").
        // Function and variable both sit in the main synchronous component (S2/S3/S4, the S1 side is DC-coupled);
        // S2VL1's own generator GTH1 is not voltage-regulating, so its voltage does move with GTH2's target on S3VL1.
        double[] viaBusbarSectionId = adjointAndForwardBusVoltage(FourSubstationsNodeBreakerFactory.create(), "S2VL1_BBS", "GTH2");
        double[] viaBusViewId = adjointAndForwardBusVoltage(FourSubstationsNodeBreakerFactory.create(), "S2VL1_0", "GTH2");
        assertEquals(viaBusViewId[0], viaBusbarSectionId[0], 1e-12, "adjoint gradient must not depend on the bus id convention");
        assertEquals(viaBusbarSectionId[1], viaBusbarSectionId[0], 1e-4 * (Math.abs(viaBusbarSectionId[1]) + 1e-3), "adjoint vs forward, busbar section id");
        assertTrue(Math.abs(viaBusbarSectionId[0]) > 1e-3, "gradient must be non-trivial, got " + viaBusbarSectionId[0]);
    }

    /**
     * Declares {@code line} as a lever under each of {@code variableTypes} — one factor matrix per type, all
     * monitoring the same branch flow — and returns each matrix's single gradient value, read back through
     * the per-matrix row keys (the mapping {@code createGradient} uses).
     */
    private static double[] adjointPerVariableType(String line, List<SensitivityVariableType> variableTypes) {
        Network network = IeeeCdfNetworkFactory.create14();
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);

        String monitored = "L1-2-1";
        SensitivityAnalysisContext ctx = new SensitivityAnalysisContext();
        for (SensitivityVariableType variableType : variableTypes) {
            ctx.addFactorMatrix(variableType.name(), List.of(monitored), List.of(line), List.of(),
                    ContingencyContextType.NONE, SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, variableType);
        }

        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());

        // one cotangent slot per (matrix, column): the same monitored function declared by every matrix, so
        // state the real value once and 0 elsewhere (the agreeing-slots convention of putFunctionCotangent)
        double[] cotangents = new double[variableTypes.size()];
        cotangents[0] = 1.0;
        SensitivityAnalysisAdjointResultContext result = ctx.runAdjoint(network, cotangents, parameters, "OpenLoadFlow");

        double[] perType = new double[variableTypes.size()];
        for (int i = 0; i < variableTypes.size(); i++) {
            double[] column = result.gradientValues(variableTypes.get(i).name());
            assertEquals(1, column.length);
            perType[i] = column[0];
        }
        return perType;
    }

    @Test
    void runAdjointAcceptsRepeatedCotangentsThatAgreeToRoundingButRejectsRealDisagreement() {
        // Two factor matrices declaring the SAME monitored function against their own lever families: ȳ is a
        // property of the function, so the repeated slots must state one number. A caller that recomputes its
        // cotangent per matrix does the same arithmetic twice and can land on values differing in the last
        // bits — the same number stated twice, which must not be rejected — whereas a genuine disagreement is
        // two different answers about one number and cannot be silently blended.
        Network network = IeeeCdfNetworkFactory.create14();
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);

        SensitivityAnalysisContext ctx = new SensitivityAnalysisContext();
        ctx.addFactorMatrix("gens", List.of("L1-2-1"), List.of("B2-G"), List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        ctx.addFactorMatrix("shunts", List.of("L1-2-1"), List.of("B9-SH"), List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.SHUNT_COMPENSATOR_SUSCEPTANCE);

        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());

        double stated = 0.7;
        double roundingApart = Math.nextAfter(Math.nextAfter(stated, 1.0), 1.0); // a few ulps away, same number
        double once = ctx.runAdjoint(network, new double[] {stated, 0.0}, parameters, "OpenLoadFlow")
                .gradientValues("gens")[0];
        double twice = ctx.runAdjoint(network, new double[] {stated, roundingApart}, parameters, "OpenLoadFlow")
                .gradientValues("gens")[0];
        assertEquals(once, twice, 1e-12 * Math.abs(once) + 1e-13, "slots agreeing to rounding must be accepted, used once");
        assertTrue(Math.abs(once) > 1e-3, "gradient must be non-trivial, got " + once);

        PowsyblException e = assertThrows(PowsyblException.class,
            () -> ctx.runAdjoint(network, new double[] {stated, 2 * stated}, parameters, "OpenLoadFlow"));
        assertTrue(e.getMessage().contains("Conflicting cotangents"), e.getMessage());
    }

    @Test
    void runAdjointRefusesDeclaredContingenciesAndNamesThem() {
        // Reverse mode is base case only, so a declared contingency changes nothing in the answer. Returning
        // the base-case gradient anyway is indistinguishable from the post-contingency one the caller asked
        // for, so it is refused and the contingency is named.
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);
        Network network = IeeeCdfNetworkFactory.create14();
        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());

        // declared on the analysis itself
        SensitivityAnalysisContext onAnalysis = new SensitivityAnalysisContext();
        onAnalysis.addFactorMatrix("m", List.of("L1-2-1"), List.of("B2-G"), List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        onAnalysis.addContingency("lostLine", List.of("L2-3-1"));
        PowsyblException e1 = assertThrows(PowsyblException.class,
            () -> onAnalysis.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow"));
        assertTrue(e1.getMessage().contains("lostLine"), e1.getMessage());

        // declared on a factor matrix
        SensitivityAnalysisContext onMatrix = new SensitivityAnalysisContext();
        onMatrix.addFactorMatrix("postContingency", List.of("L1-2-1"), List.of("B2-G"), List.of("lostLine"),
                ContingencyContextType.SPECIFIC, SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
                SensitivityVariableType.INJECTION_ACTIVE_POWER);
        PowsyblException e2 = assertThrows(PowsyblException.class,
            () -> onMatrix.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow"));
        assertTrue(e2.getMessage().contains("postContingency"), e2.getMessage());

        // and the recommended shape is untouched: ALL context with nothing declared is all of none
        SensitivityAnalysisContext allContext = new SensitivityAnalysisContext();
        allContext.addFactorMatrix("m", List.of("L1-2-1"), List.of("B2-G"), List.of(), ContingencyContextType.ALL,
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        double theta = allContext.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow")
                .gradientValues("m")[0];
        assertTrue(Math.abs(theta) > 1e-3, "gradient must be non-trivial, got " + theta);
    }

    @Test
    void runAdjointInfersUndeclaredVariableTypesAndResolvesVariableSets() {
        // A matrix declared with no variable type (AUTO_DETECT on the Python side) leaves each lever's type
        // to be inferred: from the network element, or — when the id is not an element at all — from the
        // declared SensitivityVariableSets. Every other adjoint test states the type, so both branches and
        // the "not found" failure are otherwise unreachable here.
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);

        Network network = IeeeCdfNetworkFactory.create14();
        SensitivityAnalysisContext ctx = new SensitivityAnalysisContext();
        ctx.setVariableSets(List.of(new SensitivityVariableSet("glsk",
                List.of(new WeightedSensitivityVariable("B2-G", 0.5f), new WeightedSensitivityVariable("B6-G", 0.5f)))));
        // a generator (injection), a shunt (susceptance) and a variable-set id, none of them typed here
        List<String> levers = List.of("B3-G", "B9-SH", "glsk");
        ctx.addFactorMatrix("m", List.of("L1-2-1"), levers, List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, null);
        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());

        double[] thetaBar = ctx.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow").gradientValues("m");
        double[] forwardS = ctx.run(network, parameters, "OpenLoadFlow", ReportNode.NO_OP).getBaseCaseValues();

        for (int i = 0; i < levers.size(); i++) {
            assertEquals(forwardS[i], thetaBar[i], 1e-4 * (Math.abs(forwardS[i]) + 1e-3),
                    "adjoint vs forward for the inferred lever " + levers.get(i));
        }
        assertTrue(Math.abs(thetaBar[2]) > 1e-3, "the variable-set lever is trivially zero: " + thetaBar[2]);

        // an id that is neither a network element nor a declared variable set is named, not silently dropped
        SensitivityAnalysisContext unknown = new SensitivityAnalysisContext();
        unknown.addFactorMatrix("m", List.of("L1-2-1"), List.of("NOT_AN_ELEMENT"), List.of(),
                ContingencyContextType.NONE, SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, null);
        PowsyblException e = assertThrows(PowsyblException.class,
            () -> unknown.runAdjoint(network, new double[] {1.0}, parameters, "OpenLoadFlow"));
        assertTrue(e.getMessage().contains("'NOT_AN_ELEMENT' not found"), e.getMessage());
    }

    @Test
    void runAdjointTreatsTwoIdsForOneBusAsOneMonitoredFunction() {
        // Why the repeated-slot merge lives HERE and not in the Python caller: two different ids can name
        // the SAME monitored function, and only this side knows it, because the answer comes from resolving
        // the id against the bus view. IEEE-14's bus-breaker id "B10" and bus-view id "VL10_0" are one bus,
        // so two factor matrices declaring one each declare one function — which must be stated once, and
        // once only, whichever id is used.
        LoadFlowParameters lfParams = new LoadFlowParameters().setDc(false).setDistributedSlack(false);
        OpenLoadFlowParameters.create(lfParams).setNetworkCacheEnabled(true);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);

        Network network = IeeeCdfNetworkFactory.create14();
        SensitivityAnalysisContext ctx = new SensitivityAnalysisContext();
        ctx.addFactorMatrix("byBusBreakerId", List.of("B10"), List.of("B6-G"), List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.BUS_TARGET_VOLTAGE);
        ctx.addFactorMatrix("byBusViewId", List.of("VL10_0"), List.of("B6-G"), List.of(), ContingencyContextType.NONE,
                SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.BUS_TARGET_VOLTAGE);
        assertTrue(LoadFlow.find("OpenLoadFlow").run(network, lfParams).isFullyConverged());

        // stated once under each id, agreeing: used once, NOT summed — the same answer as stating it alone
        double viaBothIds = ctx.runAdjoint(network, new double[] {1.0, 1.0}, parameters, "OpenLoadFlow")
                .gradientValues("byBusBreakerId")[0];
        double viaOneId = ctx.runAdjoint(network, new double[] {1.0, 0.0}, parameters, "OpenLoadFlow")
                .gradientValues("byBusBreakerId")[0];
        assertEquals(viaOneId, viaBothIds, 1e-12 * Math.abs(viaOneId) + 1e-13,
                "one function declared under two ids must be counted once, not summed");
        assertTrue(Math.abs(viaBothIds) > 1e-3, "gradient must be non-trivial, got " + viaBothIds);

        // contradicting each other: rejected, and the message names both factor matrices
        PowsyblException e = assertThrows(PowsyblException.class,
            () -> ctx.runAdjoint(network, new double[] {1.0, 2.0}, parameters, "OpenLoadFlow"));
        assertTrue(e.getMessage().contains("byBusBreakerId") && e.getMessage().contains("byBusViewId"), e.getMessage());
    }

    @Test
    void runAdjointKeepsVariableTypesDistinctOnSharedElementId() {
        // One element is a legitimate lever under several variable types at once: a line's admittance and its
        // reactance are different derivatives of the same branch. OpenLoadFlow keys θ̄ by (variableType,
        // variableId) for exactly that reason, so the caller must read each row back under ITS OWN type —
        // reading by the bare id collapsed the two, and the second matrix got another type's value or NaN.
        String line = "L2-3-1";
        List<SensitivityVariableType> types = List.of(SensitivityVariableType.BRANCH_ADMITTANCE,
                SensitivityVariableType.BRANCH_REACTANCE);
        double[] fused = adjointPerVariableType(line, types);

        for (int i = 0; i < types.size(); i++) {
            double alone = adjointPerVariableType(line, List.of(types.get(i)))[0];
            assertEquals(alone, fused[i], 1e-12 * Math.abs(alone) + 1e-13,
                    "fused run must give " + types.get(i) + " its own gradient");
            assertTrue(Double.isFinite(fused[i]) && Math.abs(fused[i]) > 1e-6,
                    "gradient for " + types.get(i) + " must be non-trivial, got " + fused[i]);
        }
        // and the comparison must not be vacuous: reporting one type under the other would otherwise pass
        assertTrue(Math.abs(fused[0] - fused[1]) > 1e-6,
                "the two variable types must have genuinely different gradients, got " + fused[0] + " and " + fused[1]);
    }
}
