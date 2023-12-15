/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.*;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.svg.SvgParameters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, Writer writer,
                         double highNominalVoltageBound, double lowNominalVoltageBound, NadParameters nadParameters) {

        Predicate<VoltageLevel> filter = !voltageLevelIds.isEmpty()
                ? getNominalVoltageFilter(network, voltageLevelIds, lowNominalVoltageBound, highNominalVoltageBound, depth)
                : VoltageLevelFilter.NO_FILTER;
        NetworkAreaDiagram.draw(network, writer, nadParameters, filter);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, NadParameters nadParameters) {
        return getSvg(network, voltageLevelIds, 0, -1, -1, nadParameters);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, int depth,
                         double highNominalVoltageBound, double lowNominalVoltageBound, NadParameters nadParameters) {
        try (StringWriter writer = new StringWriter()) {
            writeSvg(network, voltageLevelIds, depth, writer, highNominalVoltageBound, lowNominalVoltageBound, nadParameters);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, String svgFile,
                         Double highNominalVoltageBound, Double lowNominalVoltageBound, NadParameters nadParameters) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile), StandardCharsets.UTF_8)) {
            writeSvg(network, voltageLevelIds, depth, writer, highNominalVoltageBound, lowNominalVoltageBound, nadParameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static NadParameters createNadParameters() {
        SvgParameters svgParameters = new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setFixedWidth(800)
                .setFixedHeight(600);
        return new NadParameters()
                .setSvgParameters(svgParameters);
    }

    static VoltageLevelFilter getNominalVoltageFilter(Network network, List<String> voltageLevelIds, double lowNominalVoltageBound, double highNominalVoltageBound, int depth) {
        VoltageLevelFilter voltageLevelFilter = VoltageLevelFilter.NO_FILTER;
        if (lowNominalVoltageBound >= 0 && highNominalVoltageBound >= 0) {
            voltageLevelFilter = VoltageLevelFilter.createNominalVoltageFilter(network, voltageLevelIds, lowNominalVoltageBound, highNominalVoltageBound, depth);
        } else if (lowNominalVoltageBound < 0 && highNominalVoltageBound >= 0) {
            voltageLevelFilter = VoltageLevelFilter.createNominalVoltageUpperBoundFilter(network, voltageLevelIds, highNominalVoltageBound, depth);
        } else if (lowNominalVoltageBound >= 0 && highNominalVoltageBound < 0) {
            voltageLevelFilter = VoltageLevelFilter.createNominalVoltageLowerBoundFilter(network, voltageLevelIds, lowNominalVoltageBound, depth);
        } else {
            voltageLevelFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, voltageLevelIds, depth);
        }
    }

    public static List<String> getDisplayedVoltageLevels(Network network, List<String> voltageLevelIds, int depth) {
        return NetworkAreaDiagram.getDisplayedVoltageLevels(network, voltageLevelIds, depth);
    }
<<<<<<< HEAD

    private static void traverseVoltageLevels(Set<VoltageLevel> voltageLevelsDepth, int depth, Set<VoltageLevel> visitedVoltageLevels,
                                              double highNominalVoltageBound, double lowNominalVoltageBound) {
        if (depth >= 0) {
            Set<VoltageLevel> nextDepthVoltageLevels = new HashSet<>();
            for (VoltageLevel vl : voltageLevelsDepth) {
                if (!visitedVoltageLevels.contains(vl)) {
                    if (highNominalVoltageBound > 0 && lowNominalVoltageBound > 0) {
                        if (vl.getNominalV() >= lowNominalVoltageBound
                                && vl.getNominalV() <= highNominalVoltageBound) {
                            traverseVoltageLevel(visitedVoltageLevels, nextDepthVoltageLevels, vl);
                        }
                    } else if (highNominalVoltageBound > 0) {
                        if (vl.getNominalV() <= highNominalVoltageBound) {
                            traverseVoltageLevel(visitedVoltageLevels, nextDepthVoltageLevels, vl);
                        }
                    } else if (lowNominalVoltageBound > 0) {
                        if (vl.getNominalV() >= lowNominalVoltageBound) {
                            traverseVoltageLevel(visitedVoltageLevels, nextDepthVoltageLevels, vl);
                        }
                    } else {
                        traverseVoltageLevel(visitedVoltageLevels, nextDepthVoltageLevels, vl);
                    }
                }
            }
            traverseVoltageLevels(nextDepthVoltageLevels, depth - 1, visitedVoltageLevels, highNominalVoltageBound, lowNominalVoltageBound);
        }
    }

    private static void traverseVoltageLevel(Set<VoltageLevel> visitedVoltageLevels, Set<VoltageLevel> nextDepthVoltageLevels, VoltageLevel vl) {
        visitedVoltageLevels.add(vl);
        vl.visitEquipments(new VlVisitor(nextDepthVoltageLevels, visitedVoltageLevels));
    }

    private static class VlVisitor extends DefaultTopologyVisitor {
        private final Set<VoltageLevel> nextDepthVoltageLevels;
        private final Set<VoltageLevel> visitedVoltageLevels;

        public VlVisitor(Set<VoltageLevel> nextDepthVoltageLevels, Set<VoltageLevel> visitedVoltageLevels) {
            this.nextDepthVoltageLevels = nextDepthVoltageLevels;
            this.visitedVoltageLevels = visitedVoltageLevels;
        }

        public void visitLine(Line line, TwoSides side) {
            this.visitBranch(line, side);
        }

        public void visitTwoWindingsTransformer(TwoWindingsTransformer twt, TwoSides side) {
            this.visitBranch(twt, side);
        }

        public void visitThreeWindingsTransformer(ThreeWindingsTransformer twt, ThreeSides side) {
            if (side == ThreeSides.ONE) {
                this.visitTerminal(twt.getTerminal(ThreeSides.TWO));
                this.visitTerminal(twt.getTerminal(ThreeSides.THREE));
            } else if (side == ThreeSides.TWO) {
                this.visitTerminal(twt.getTerminal(ThreeSides.ONE));
                this.visitTerminal(twt.getTerminal(ThreeSides.THREE));
            } else {
                this.visitTerminal(twt.getTerminal(ThreeSides.ONE));
                this.visitTerminal(twt.getTerminal(ThreeSides.TWO));
            }

        }

        public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
            converterStation.getOtherConverterStation().ifPresent(c -> this.visitTerminal(c.getTerminal()));
        }

        private void visitBranch(Branch<?> branch, TwoSides side) {
            this.visitTerminal(branch.getTerminal(IidmUtils.getOpposite(side)));
        }

        private void visitTerminal(Terminal terminal) {
            VoltageLevel voltageLevel = terminal.getVoltageLevel();
            if (!this.visitedVoltageLevels.contains(voltageLevel)) {
                this.nextDepthVoltageLevels.add(voltageLevel);
            }

        }

        public void visitDanglingLine(DanglingLine danglingLine) {
            if (danglingLine.isPaired()) {
                danglingLine.getTieLine().ifPresent(tieline -> visitBranch(tieline, tieline.getSide(danglingLine.getTerminal())));
            }
        }
    }
}
