/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.utils.iidm.IidmUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, Writer writer,
                         double highNominalVoltageBound, double lowNominalVoltageBound, boolean edgeNameDisplayed) {
        SvgParameters svgParameters = new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setFixedWidth(800)
                .setFixedHeight(600)
                .setEdgeNameDisplayed(edgeNameDisplayed);
        Predicate<VoltageLevel> filter = voltageLevelIds.size() > 0
                ? getNominalVoltageFilter(network, voltageLevelIds, highNominalVoltageBound, lowNominalVoltageBound, depth)
                : VoltageLevelFilter.NO_FILTER;
        new NetworkAreaDiagram(network, filter)
                .draw(writer, svgParameters);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, int depth, boolean edgeNameDisplayed) {
        return getSvg(network, voltageLevelIds, depth, -1, -1, edgeNameDisplayed);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, int depth,
                         double highNominalVoltageBound, double lowNominalVoltageBound, boolean edgeNameDisplayed) {
        try (StringWriter writer = new StringWriter()) {
            writeSvg(network, voltageLevelIds, depth, writer, highNominalVoltageBound, lowNominalVoltageBound, edgeNameDisplayed);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, String svgFile,
                         Double highNominalVoltageBound, Double lowNominalVoltageBound, boolean edgeNameDisplayed) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile), StandardCharsets.UTF_8)) {
            writeSvg(network, voltageLevelIds, depth, writer, highNominalVoltageBound, lowNominalVoltageBound, edgeNameDisplayed);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static VoltageLevelFilter getNominalVoltageFilter(Network network, List<String> voltageLevelIds,
                                                      double highNominalVoltageBound,
                                                      double lowNominalVoltageBound, int depth) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(voltageLevelIds);
        Set<VoltageLevel> startingSet = new HashSet<>();

        for (String voltageLevelId : voltageLevelIds) {
            VoltageLevel vl = network.getVoltageLevel(voltageLevelId);
            if (vl == null) {
                throw new PowsyblException("Unknown voltage level id '" + voltageLevelId + "'");
            }
            if (lowNominalVoltageBound > 0 && vl.getNominalV() < lowNominalVoltageBound ||
                    highNominalVoltageBound > 0 && vl.getNominalV() > highNominalVoltageBound) {
                throw new PowsyblException("vl '" + voltageLevelId +
                        "' has his nominal voltage out of the indicated thresholds");
            }
            startingSet.add(vl);
        }

        Set<VoltageLevel> voltageLevels = new HashSet<>();
        traverseVoltageLevels(startingSet, depth, voltageLevels, highNominalVoltageBound, lowNominalVoltageBound);
        return new VoltageLevelFilter(voltageLevels);
    }

    public static List<String> getDisplayedVoltageLevels(Network network, List<String> voltageLevelIds, int depth) {
        NetworkGraphBuilder builder = new NetworkGraphBuilder(network, VoltageLevelFilter.createVoltageLevelsDepthFilter(network, voltageLevelIds, depth));
        return builder.getVoltageLevels().stream()
                .map(VoltageLevel::getId)
                .sorted()
                .toList();
    }

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

        public void visitLine(Line line, Branch.Side side) {
            this.visitBranch(line, side);
        }

        public void visitTwoWindingsTransformer(TwoWindingsTransformer twt, Branch.Side side) {
            this.visitBranch(twt, side);
        }

        public void visitThreeWindingsTransformer(ThreeWindingsTransformer twt, ThreeWindingsTransformer.Side side) {
            if (side == ThreeWindingsTransformer.Side.ONE) {
                this.visitTerminal(twt.getTerminal(ThreeWindingsTransformer.Side.TWO));
                this.visitTerminal(twt.getTerminal(ThreeWindingsTransformer.Side.THREE));
            } else if (side == ThreeWindingsTransformer.Side.TWO) {
                this.visitTerminal(twt.getTerminal(ThreeWindingsTransformer.Side.ONE));
                this.visitTerminal(twt.getTerminal(ThreeWindingsTransformer.Side.THREE));
            } else {
                this.visitTerminal(twt.getTerminal(ThreeWindingsTransformer.Side.ONE));
                this.visitTerminal(twt.getTerminal(ThreeWindingsTransformer.Side.TWO));
            }

        }

        public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
            converterStation.getOtherConverterStation().ifPresent(c -> this.visitTerminal(c.getTerminal()));
        }

        private void visitBranch(Branch<?> branch, Branch.Side side) {
            this.visitTerminal(branch.getTerminal(IidmUtils.getOpposite(side)));
        }

        private void visitTerminal(Terminal terminal) {
            VoltageLevel voltageLevel = terminal.getVoltageLevel();
            if (!this.visitedVoltageLevels.contains(voltageLevel)) {
                this.nextDepthVoltageLevels.add(voltageLevel);
            }

        }
    }
}
