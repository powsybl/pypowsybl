/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.loadflow;

import com.powsybl.dataframe.network.NetworkDataframeContext;
import com.powsybl.dataframe.network.PerUnitUtil;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.test.BoundaryLineNetworkFactory;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.iidm.network.test.ThreeWindingsTransformerNetworkFactory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Samuel Monroe {@literal <smn at n-side.com>}
 */
class DcLoadFlowResultsTest {

    private static final double EPS = 1e-9;

    private static LoadFlowParameters dc() {
        return new LoadFlowParameters().setDc(true);
    }

    /**
     * Runs a load flow like {@link LoadFlowCFunctions} does.
     */
    private static LoadFlowResult run(Network network, LoadFlowParameters parameters) {
        String variantId = network.getVariantManager().getWorkingVariantId();
        DcLoadFlowResults.clear(network, variantId);
        LoadFlowResult result = LoadFlow.find("OpenLoadFlow").run(network, parameters);
        DcLoadFlowResults.record(network, variantId, parameters, result);
        return result;
    }

    private static double amperes(double apparentPower, Terminal terminal) {
        return apparentPower * 1000 / (Math.sqrt(3) * terminal.getVoltageLevel().getNominalV());
    }

    private static double estimate(Terminal terminal, double powerFactor) {
        return amperes(Math.abs(terminal.getP()) / powerFactor, terminal);
    }

    private static void assertNoEstimate(Terminal terminal) {
        assertTrue(Double.isNaN(DcLoadFlowResults.current(terminal)));
        assertTrue(Double.isNaN(DcLoadFlowResults.voltage(terminal.getBusView().getBus())));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0.9})
    void branchCurrentsAndBusVoltages(double powerFactor) {
        Network network = EurostagTutorialExample1Factory.create();
        run(network, dc().setDcPowerFactor(powerFactor));

        for (Bus bus : network.getBusView().getBuses()) {
            assertTrue(Double.isNaN(bus.getV()));
            assertEquals(bus.getVoltageLevel().getNominalV(), DcLoadFlowResults.voltage(bus));
        }
        Terminal line1 = network.getLine("NHV1_NHV2_1").getTerminal1();
        assertEquals(300, line1.getP(), 1e-6);
        assertTrue(Double.isNaN(line1.getQ()));
        assertTrue(Double.isNaN(line1.getI()));
        assertEquals(300 * 1000 / (Math.sqrt(3) * 380 * powerFactor), DcLoadFlowResults.current(line1), 1e-6);
        network.getBranches().forEach(branch -> {
            assertEquals(estimate(branch.getTerminal1(), powerFactor), DcLoadFlowResults.current(branch.getTerminal1()), EPS);
            assertEquals(estimate(branch.getTerminal2(), powerFactor), DcLoadFlowResults.current(branch.getTerminal2()), EPS);
        });
    }

    @Test
    void perUnitValues() {
        Network network = EurostagTutorialExample1Factory.create();
        run(network, dc().setDcPowerFactor(0.9));

        Terminal terminal = network.getLine("NHV1_NHV2_1").getTerminal1();
        assertEquals(estimate(terminal, 0.9), PerUnitUtil.perUnitI(NetworkDataframeContext.DEFAULT, terminal), EPS);
        assertEquals(Math.abs(terminal.getP()) / (100 * 0.9),
                PerUnitUtil.perUnitI(new NetworkDataframeContext(true, 100), terminal), EPS);
    }

    @Test
    void threeWindingsTransformerLegsAndBoundaryLines() {
        Network network = ThreeWindingsTransformerNetworkFactory.create();
        run(network, dc().setDcPowerFactor(0.9));
        ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer("3WT");
        for (ThreeWindingsTransformer.Leg leg : transformer.getLegs()) {
            assertTrue(Double.isFinite(leg.getTerminal().getP()));
            assertEquals(estimate(leg.getTerminal(), 0.9), DcLoadFlowResults.current(leg.getTerminal()), EPS);
        }

        network = BoundaryLineNetworkFactory.create();
        run(network, dc().setDcPowerFactor(0.9));
        BoundaryLine boundaryLine = network.getBoundaryLine("BL");
        Terminal terminal = boundaryLine.getTerminal();
        assertEquals(50, terminal.getP(), 1e-6);
        assertEquals(estimate(terminal, 0.9), DcLoadFlowResults.current(terminal), EPS);
        assertTrue(Double.isNaN(boundaryLine.getBoundary().getI()));
        assertEquals(amperes(Math.abs(boundaryLine.getBoundary().getP()) / 0.9, terminal), DcLoadFlowResults.boundaryCurrent(boundaryLine), EPS);
        assertEquals(DcLoadFlowResults.current(terminal), DcLoadFlowResults.boundaryCurrent(boundaryLine), 1e-6);
        assertTrue(Double.isNaN(boundaryLine.getBoundary().getV()));
        assertEquals(terminal.getVoltageLevel().getNominalV(), DcLoadFlowResults.boundaryVoltage(boundaryLine));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0.9})
    void injectionCurrentsFollowTheSameRule(double powerFactor) {
        Network network = EurostagTutorialExample1Factory.create();
        run(network, dc().setDcPowerFactor(powerFactor));

        // the reactive power which is defined at the terminal of the load is not used
        Terminal load = network.getLoad("LOAD").getTerminal();
        assertEquals(600, load.getP(), EPS);
        assertEquals(200, load.getQ(), EPS);
        assertEquals(600 * 1000 / (Math.sqrt(3) * 150 * powerFactor), DcLoadFlowResults.current(load), 1e-6);
        Terminal generator = network.getGenerator("GEN").getTerminal();
        assertTrue(Math.abs(generator.getP()) > 0);
        assertEquals(estimate(generator, powerFactor), DcLoadFlowResults.current(generator), EPS);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0.9})
    void currentsAreBalancedAtEachBus(double powerFactor) {
        for (Network network : List.of(EurostagTutorialExample1Factory.create(), FourSubstationsNodeBreakerFactory.create())) {
            run(network, dc().setDcPowerFactor(powerFactor));
            for (Bus bus : network.getBusView().getBuses()) {
                double incoming = 0;
                double outgoing = 0;
                for (Terminal terminal : bus.getConnectedTerminals()) {
                    double current = DcLoadFlowResults.current(terminal);
                    if (terminal.getP() > 0) {
                        outgoing += current;
                    } else if (terminal.getP() < 0) {
                        incoming += current;
                    }
                }
                assertTrue(outgoing > 0, bus.getId());
                assertEquals(incoming, outgoing, 1e-6, bus.getId());
            }
        }
    }

    @Test
    void reactiveOnlyInjectionsHaveNoCurrentEstimate() {
        Network network = SvcTestCaseFactory.create();
        run(network, dc());

        Terminal svc = network.getStaticVarCompensator("SVC2").getTerminal();
        assertEquals(svc.getVoltageLevel().getNominalV(), DcLoadFlowResults.voltage(svc.getBusView().getBus()));
        assertEquals(0, svc.getP());
        assertTrue(Double.isNaN(DcLoadFlowResults.current(svc)));

        network = FourSubstationsNodeBreakerFactory.create();
        run(network, dc());
        Terminal shunt = network.getShuntCompensator("SHUNT").getTerminal();
        assertEquals(shunt.getVoltageLevel().getNominalV(), DcLoadFlowResults.voltage(shunt.getBusView().getBus()));
        assertEquals(0, shunt.getP());
        assertTrue(Double.isNaN(DcLoadFlowResults.current(shunt)));
    }

    @Test
    void busbarSectionVoltages() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        run(network, dc());
        for (BusbarSection busbarSection : network.getBusbarSections()) {
            assertTrue(Double.isNaN(busbarSection.getV()));
            assertEquals(busbarSection.getTerminal().getVoltageLevel().getNominalV(), DcLoadFlowResults.voltage(busbarSection));
        }

        run(network, new LoadFlowParameters());
        for (BusbarSection busbarSection : network.getBusbarSections()) {
            assertFalse(Double.isNaN(busbarSection.getV()));
            assertEquals(busbarSection.getV(), DcLoadFlowResults.voltage(busbarSection));
        }
    }

    @Test
    void networkValuesTakePrecedence() {
        Network network = EurostagTutorialExample1Factory.create();
        run(network, dc());
        Terminal terminal = network.getLine("NHV1_NHV2_1").getTerminal1();
        Bus bus = terminal.getBusView().getBus();

        // a voltage which is written on the network is not a DC result anymore
        bus.setV(385);
        assertEquals(385, DcLoadFlowResults.voltage(bus));
        assertTrue(Double.isNaN(DcLoadFlowResults.current(terminal)));

        // nor is the result of an AC load flow, even when it has not been recorded
        LoadFlow.find("OpenLoadFlow").run(network, new LoadFlowParameters());
        assertFalse(Double.isNaN(terminal.getI()));
        assertEquals(terminal.getI(), DcLoadFlowResults.current(terminal));
        assertEquals(bus.getV(), DcLoadFlowResults.voltage(bus));
    }

    @Test
    void loadFlowWithoutConvergedComponentForgetsDcResults() {
        Network network = EurostagTutorialExample1Factory.create();
        LoadFlowParameters parameters = dc();
        run(network, parameters);
        Terminal terminal = network.getLine("NHV1_NHV2_1").getTerminal1();
        assertFalse(Double.isNaN(DcLoadFlowResults.current(terminal)));

        DcLoadFlowResults.record(network, VariantManagerConstants.INITIAL_VARIANT_ID, parameters,
                new LoadFlowResultImpl(false, Map.of(), "", List.of()));
        assertNoEstimate(terminal);
    }

    @Test
    void variants() {
        Network network = EurostagTutorialExample1Factory.create();
        VariantManager variantManager = network.getVariantManager();
        String initial = VariantManagerConstants.INITIAL_VARIANT_ID;
        run(network, dc());
        Terminal terminal = network.getLine("NHV1_NHV2_1").getTerminal1();
        double expected = estimate(terminal, 1);

        // the interpretation follows the cloned variants
        variantManager.cloneVariant(initial, "copy");
        variantManager.setWorkingVariant("copy");
        assertEquals(expected, DcLoadFlowResults.current(terminal), EPS);

        // and is specific to each variant
        run(network, new LoadFlowParameters());
        assertEquals(terminal.getI(), DcLoadFlowResults.current(terminal));
        variantManager.setWorkingVariant(initial);
        assertEquals(expected, DcLoadFlowResults.current(terminal), EPS);

        // an overwritten variant takes the interpretation of its source, whatever it is
        variantManager.cloneVariant("copy", initial, true);
        assertEquals(terminal.getI(), DcLoadFlowResults.current(terminal));
        variantManager.setWorkingVariant("copy");
        run(network, dc());
        variantManager.cloneVariant("copy", initial, true);
        variantManager.setWorkingVariant(initial);
        assertEquals(expected, DcLoadFlowResults.current(terminal), EPS);
        variantManager.removeVariant("copy");
        assertEquals(expected, DcLoadFlowResults.current(terminal), EPS);
    }

    @Test
    void componentsWhichAreNotSolvedHaveNoEstimate() {
        Network network = Network.merge("merged", EurostagTutorialExample1Factory.create(), FourSubstationsNodeBreakerFactory.create());
        // only the main connected component is solved
        LoadFlowResult result = run(network, dc());
        assertEquals(List.of(0), result.getComponentResults().stream().map(LoadFlowResult.ComponentResult::getConnectedComponentNum).distinct().toList());

        Terminal solved = network.getLine("LINE_S2S3").getTerminal1();
        assertEquals(0, solved.getBusView().getBus().getConnectedComponent().getNum());
        assertEquals(estimate(solved, 1), DcLoadFlowResults.current(solved), EPS);
        assertEquals(400, DcLoadFlowResults.voltage(solved.getBusView().getBus()));

        Terminal notSolved = network.getLine("NHV1_NHV2_1").getTerminal1();
        assertEquals(1, notSolved.getBusView().getBus().getConnectedComponent().getNum());
        assertNoEstimate(notSolved);
    }

    @Test
    void subnetwork() {
        Network network = Network.merge("merged", EurostagTutorialExample1Factory.create(), FourSubstationsNodeBreakerFactory.create());
        Network subnetwork = network.getSubnetwork("sim1");
        LoadFlowParameters parameters = dc().setComponentMode(LoadFlowParameters.ComponentMode.ALL_CONNECTED);
        LoadFlowResult result = assertDoesNotThrow(() -> run(subnetwork, parameters));
        assertEquals(1, result.getComponentResults().size());

        Terminal terminal = network.getLine("NHV1_NHV2_1").getTerminal1();
        assertEquals(300, terminal.getP(), 1e-6);
        assertEquals(estimate(terminal, 1), DcLoadFlowResults.current(terminal), EPS);
        assertEquals(380, DcLoadFlowResults.voltage(terminal.getBusView().getBus()));
        // the other subnetwork has not been solved: it still holds the state of its test case
        Terminal other = network.getLine("LINE_S2S3").getTerminal1();
        assertFalse(Double.isNaN(other.getI()));
        assertEquals(other.getI(), DcLoadFlowResults.current(other));
        assertEquals(other.getBusView().getBus().getV(), DcLoadFlowResults.voltage(other.getBusView().getBus()));
    }

    @Test
    void networkCanStillBeExportedStrictly(@TempDir Path directory) {
        Network network = EurostagTutorialExample1Factory.create();
        run(network, dc());

        // nothing is attached to the network, which would have to be exported with it
        Properties parameters = new Properties();
        parameters.setProperty("iidm.export.xml.throw-exception-if-extension-not-found", "true");
        assertDoesNotThrow(() -> network.write("XIIDM", parameters, directory.resolve("network")));
    }
}
