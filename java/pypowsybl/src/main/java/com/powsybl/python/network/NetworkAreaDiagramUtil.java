/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.model.Identifiable;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.NetworkGraphBuilder;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.model.*;
import com.powsybl.nad.svg.CustomLabelProvider;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.LabelProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.nad.svg.iidm.DefaultLabelProvider.EdgeInfoEnum.ACTIVE_POWER;
import static com.powsybl.nad.svg.iidm.DefaultLabelProvider.EdgeInfoEnum.EMPTY;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
    }

    public static int getEquivalentValueForEdgeInfo(DefaultLabelProvider.EdgeInfoEnum edgeInfoEnum) {
        return switch (edgeInfoEnum) {
            case ACTIVE_POWER -> 0;
            case REACTIVE_POWER -> 1;
            case CURRENT -> 2;
            case NAME -> 3;
            case VALUE_PERMANENT_LIMIT_PERCENTAGE -> 4;
            case EMPTY -> -1;
        };
    }

    public static DefaultLabelProvider.EdgeInfoEnum getEquivalentEdgeInfoEnum(int edgeInfoValue) {
        return switch (edgeInfoValue) {
            case 0 -> ACTIVE_POWER;
            case 1 -> DefaultLabelProvider.EdgeInfoEnum.REACTIVE_POWER;
            case 2 -> DefaultLabelProvider.EdgeInfoEnum.CURRENT;
            case 3 -> DefaultLabelProvider.EdgeInfoEnum.NAME;
            case 4 -> DefaultLabelProvider.EdgeInfoEnum.VALUE_PERMANENT_LIMIT_PERCENTAGE;
            default -> EMPTY;
        };
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, Writer writer, Writer metadataWriter,
                         double nominalVoltageUpperBound, double nominalVoltageLowerBound, NadParameters nadParameters) {

        Predicate<VoltageLevel> filter = !voltageLevelIds.isEmpty()
                ? getNominalVoltageFilter(network, voltageLevelIds, nominalVoltageLowerBound, nominalVoltageUpperBound, depth)
                : getNominalVoltageFilter(network, nominalVoltageLowerBound, nominalVoltageUpperBound);
        NetworkAreaDiagram.draw(network, writer, metadataWriter, nadParameters, filter);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, NadParameters nadParameters) {
        return getSvg(network, voltageLevelIds, 0, -1, -1, nadParameters);
    }

    static String getSvg(Network network, List<String> voltageLevelIds, int depth,
                         double nominalVoltageUpperBound, double nominalVoltageLowerBound, NadParameters nadParameters) {
        try (StringWriter writer = new StringWriter(); StringWriter metadataWriter = new StringWriter()) {
            writeSvg(network, voltageLevelIds, depth, writer, metadataWriter, nominalVoltageUpperBound,
                    nominalVoltageLowerBound, nadParameters);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> getSvgAndMetadata(Network network, List<String> voltageLevelIds, NadParameters nadParameters) {
        return getSvgAndMetadata(network, voltageLevelIds, 0, -1, -1, nadParameters);
    }

    static List<String> getSvgAndMetadata(Network network, List<String> voltageLevelIds, int depth,
                                          double nominalVoltageUpperBound, double nominalVoltageLowerBound, NadParameters nadParameters) {
        try (StringWriter writer = new StringWriter(); StringWriter metadataWriter = new StringWriter()) {
            writeSvg(network, voltageLevelIds, depth, writer, metadataWriter, nominalVoltageUpperBound,
                    nominalVoltageLowerBound, nadParameters);
            return List.of(writer.toString(), metadataWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeSvg(Network network, List<String> voltageLevelIds, int depth, String svgFile, String metadataFile,
                         Double nominalVoltageUpperBound, Double nominalVoltageLowerBound, NadParameters nadParameters) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(svgFile), StandardCharsets.UTF_8);
             Writer metadataWriter = metadataFile == null || metadataFile.isEmpty() ? new StringWriter() : Files.newBufferedWriter(Paths.get(metadataFile))) {
            writeSvg(network, voltageLevelIds, depth, writer, metadataWriter, nominalVoltageUpperBound, nominalVoltageLowerBound, nadParameters);
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

    static DefaultLabelProvider.EdgeInfoParameters createEdgeInfoParameters() {
        return new DefaultLabelProvider.EdgeInfoParameters(ACTIVE_POWER, EMPTY, EMPTY, EMPTY);
    }

    static VoltageLevelFilter getNominalVoltageFilter(Network network, List<String> voltageLevelIds, double nominalVoltageLowerBound, double nominalVoltageUpperBound, int depth) {
        if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageFilter(network, voltageLevelIds, nominalVoltageLowerBound, nominalVoltageUpperBound, depth);
        } else if (nominalVoltageLowerBound < 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageUpperBoundFilter(network, voltageLevelIds, nominalVoltageUpperBound, depth);
        } else if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound < 0) {
            return VoltageLevelFilter.createNominalVoltageLowerBoundFilter(network, voltageLevelIds, nominalVoltageLowerBound, depth);
        } else {
            return VoltageLevelFilter.createVoltageLevelsDepthFilter(network, voltageLevelIds, depth);
        }
    }

    static VoltageLevelFilter getNominalVoltageFilter(Network network, double nominalVoltageLowerBound, double nominalVoltageUpperBound) {
        if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageFilter(network, nominalVoltageLowerBound, nominalVoltageUpperBound);
        } else if (nominalVoltageLowerBound < 0 && nominalVoltageUpperBound >= 0) {
            return VoltageLevelFilter.createNominalVoltageUpperBoundFilter(network, nominalVoltageUpperBound);
        } else if (nominalVoltageLowerBound >= 0 && nominalVoltageUpperBound < 0) {
            return VoltageLevelFilter.createNominalVoltageLowerBoundFilter(network, nominalVoltageLowerBound);
        } else {
            return VoltageLevelFilter.createNominalVoltageFilterWithPredicate(network, VoltageLevelFilter.NO_FILTER);
        }
    }

    public static List<String> getDisplayedVoltageLevels(Network network, List<String> voltageLevelIds, int depth) {
        return NetworkAreaDiagram.getDisplayedVoltageLevels(network, voltageLevelIds, depth);
    }

    public record CustomLabelData(Map<String, CustomLabelProvider.BranchLabels> branchLabels,
                                  Map<String, CustomLabelProvider.ThreeWtLabels> threeWtLabels,
                                  Map<String, CustomLabelProvider.InjectionLabels> injectionLabels,
                                  Map<String, String> busDescriptions,
                                  Map<String, List<String>> vlDescriptions,
                                  Map<String, List<String>> vlDetails) {
    }

    public static Map<String, CustomLabelProvider.BranchLabels> getBranchLabelsMap(Graph graph, LabelProvider labelProvider) {

        return graph.getBranchEdgeStream()
                .filter(be -> !(BranchEdge.BOUNDARY_LINE_EDGE.equals(be.getType())))
                .collect(Collectors.toMap(Identifiable::getEquipmentId, branchEdge -> {
                    String branchId = branchEdge.getEquipmentId();
                    String type = branchEdge.getType();
                    EdgeInfo e1 = labelProvider.getBranchEdgeInfo(branchId, BranchEdge.Side.ONE, type).orElse(null);
                    EdgeInfoLabels edgeInfoLabels1 = getEdgeInfoLabels(e1);

                    EdgeInfo e2 = labelProvider.getBranchEdgeInfo(branchId, BranchEdge.Side.TWO, type).orElse(null);
                    EdgeInfoLabels edgeInfoLabels2 = getEdgeInfoLabels(e2);

                    EdgeInfo eMiddle = labelProvider.getBranchEdgeInfo(branchId, type).orElse(null);
                    EdgeInfoLabels edgeInfoLabelsMiddle = getEdgeInfoLabels(eMiddle);

                    return new CustomLabelProvider.BranchLabels(edgeInfoLabels1.labelA, edgeInfoLabels1.labelB,
                        edgeInfoLabelsMiddle.labelA, edgeInfoLabelsMiddle.labelB,
                        edgeInfoLabels2.labelA, edgeInfoLabels2.labelB,
                        edgeInfoLabels1.direction, edgeInfoLabelsMiddle.direction, edgeInfoLabels2.direction);
                }));
    }

    public static Map<String, CustomLabelProvider.BranchLabels> getBranchLabelsMap(Network network, SvgParameters pars) {
        LabelProvider labelProvider = new DefaultLabelProvider(network, pars);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return NetworkAreaDiagramUtil.getBranchLabelsMap(graph, labelProvider);
    }

    private static CustomLabelProvider.ThreeWtLabels buildThreeWtLabelsRecord(List<ThreeWtEdge> dataList, LabelProvider labelProvider) {
        EdgeInfoLabels edgeInfoLabelsSide1 = getEdgeInfoLabels(null);
        EdgeInfoLabels edgeInfoLabelsSide2 = getEdgeInfoLabels(null);
        EdgeInfoLabels edgeInfoLabelsSide3 = getEdgeInfoLabels(null);

        for (ThreeWtEdge edge : dataList) {
            ThreeWtEdge.Side side = edge.getSide();
            EdgeInfo edgeInfo = labelProvider.getThreeWindingTransformerEdgeInfo(edge.getEquipmentId(), side).orElse(null);
            switch (side) {
                case ONE -> edgeInfoLabelsSide1 = getEdgeInfoLabels(edgeInfo);
                case TWO -> edgeInfoLabelsSide2 = getEdgeInfoLabels(edgeInfo);
                case THREE -> edgeInfoLabelsSide3 = getEdgeInfoLabels(edgeInfo);
            }
        }
        return new CustomLabelProvider.ThreeWtLabels(edgeInfoLabelsSide1.labelA, edgeInfoLabelsSide1.labelB,
            edgeInfoLabelsSide2.labelA, edgeInfoLabelsSide2.labelB,
            edgeInfoLabelsSide3.labelA, edgeInfoLabelsSide3.labelB,
                edgeInfoLabelsSide1.direction, edgeInfoLabelsSide2.direction, edgeInfoLabelsSide3.direction);
    }

    public static Map<String, CustomLabelProvider.ThreeWtLabels> getThreeWtBranchLabelsMap(Graph graph, LabelProvider labelProvider) {
        return graph.getThreeWtEdgesStream().collect(Collectors.groupingBy(
                ThreeWtEdge::getEquipmentId,
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> buildThreeWtLabelsRecord(list, labelProvider)
                )
        ));
    }

    public static Map<String, CustomLabelProvider.ThreeWtLabels> getThreeWtBranchLabelsMap(Network network, SvgParameters pars) {
        LabelProvider labelProvider = new DefaultLabelProvider(network, pars);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return NetworkAreaDiagramUtil.getThreeWtBranchLabelsMap(graph, labelProvider);
    }

    public static Map<String, CustomLabelProvider.InjectionLabels> getInjectionLabelsMap(Graph graph, LabelProvider labelProvider) {

        return graph.getInjections().stream()
            .collect(Collectors.toMap(Identifiable::getEquipmentId, injection -> {
                String branchId = injection.getEquipmentId();
                EdgeInfo edgeInfo = labelProvider.getInjectionEdgeInfo(branchId).orElse(null);
                EdgeInfoLabels edgeInfoLabels = getEdgeInfoLabels(edgeInfo);
                return new CustomLabelProvider.InjectionLabels(edgeInfoLabels.labelA, edgeInfoLabels.labelB, edgeInfoLabels.direction);
            }));
    }

    public static Map<String, CustomLabelProvider.InjectionLabels> getInjectionLabelsMap(Network network, SvgParameters pars) {
        LabelProvider labelProvider = new DefaultLabelProvider(network, pars);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return NetworkAreaDiagramUtil.getInjectionLabelsMap(graph, labelProvider);
    }

    public static Map<String, String> getBusDescriptionsMap(Graph graph, LabelProvider labelProvider) {
        Map<String, String> busDescriptions = new HashMap<>();
        graph.getVoltageLevelNodesStream()
            .forEach(vlNode -> vlNode.getBusNodeStream()
                    .filter(busNode -> !(busNode instanceof BoundaryBusNode))
                    .forEach(busNode -> busDescriptions.put(busNode.getEquipmentId(),
                            labelProvider.getVoltageLevelLegend(vlNode.getEquipmentId()).getBusLegend(busNode.getEquipmentId())))
            );
        return busDescriptions;
    }

    public static Map<String, String> getBusDescriptionsMap(Network network, SvgParameters pars) {
        LabelProvider labelProvider = new DefaultLabelProvider(network, pars);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return NetworkAreaDiagramUtil.getBusDescriptionsMap(graph, labelProvider);
    }

    public static Map<String, List<String>> getVoltageLevelDescriptionsMap(Graph graph, LabelProvider labelProvider) {
        return graph.getVoltageLevelNodesStream()
                .filter(Objects::nonNull)
                .filter(VoltageLevelNode::isVisible)
                .filter(vlNode -> !(vlNode instanceof BoundaryNode))
                .collect(Collectors.toMap(
                        VoltageLevelNode::getEquipmentId,
                        vlNode -> labelProvider.getVoltageLevelLegend(vlNode.getEquipmentId()).legendHeader()
                ));
    }

    public static Map<String, List<String>> getVoltageLevelDetailsMap(Graph graph, LabelProvider labelProvider) {
        return graph.getVoltageLevelNodesStream()
                .filter(Objects::nonNull)
                .filter(VoltageLevelNode::isVisible)
                .filter(vlNode -> !(vlNode instanceof BoundaryNode))
                .collect(Collectors.filtering(
                        c1 -> {
                            List<String> details = labelProvider.getVoltageLevelLegend(c1.getEquipmentId()).legendFooter();
                            return details != null && !details.isEmpty();
                        },
                        Collectors.toMap(
                                VoltageLevelNode::getEquipmentId,
                                vlNode -> labelProvider.getVoltageLevelLegend(vlNode.getEquipmentId()).legendFooter()
                        )));
    }

    private static String getDirectionAsString(EdgeInfo.Direction dir) {
        if (dir != null) {
            return dir.name();
        } else {
            return "";
        }
    }

    private static String getBranchArrowAsString(CustomLabelProvider.BranchLabels label, BranchEdge.Side side) {
        EdgeInfo.Direction dir = switch (side) {
            case ONE -> label.arrow1();
            case TWO -> label.arrow2();
        };
        return getDirectionAsString(dir);
    }

    private static String getTwtLegArrowAsString(CustomLabelProvider.ThreeWtLabels label, ThreeWtEdge.Side side) {
        EdgeInfo.Direction dir = switch (side) {
            case ONE -> label.arrow1();
            case TWO -> label.arrow2();
            case THREE -> label.arrow3();
        };
        return getDirectionAsString(dir);
    }

    public static final DataframeMapper<Map<String, CustomLabelProvider.BranchLabels>, Void> BRANCH_LABELS_MAPPER = new DataframeMapperBuilder<Map<String, CustomLabelProvider.BranchLabels>,
            Map.Entry<String, CustomLabelProvider.BranchLabels>, Void>()
            .itemsProvider(c -> c.entrySet()
                    .stream()
                    .map(x -> Map.entry(x.getKey(), x.getValue()))
                    .toList())
            .stringsIndex("id", Map.Entry::getKey)
            .strings("side1Internal", ble -> ble.getValue().side1Internal())
            .strings("side1External", ble -> ble.getValue().side1External())
            .strings("middle1", ble -> ble.getValue().middle1())
            .strings("middle2", ble -> ble.getValue().middle2())
            .strings("side2Internal", ble -> ble.getValue().side2Internal())
            .strings("side2External", ble -> ble.getValue().side2External())
            .strings("arrow1", ble -> getBranchArrowAsString(ble.getValue(), BranchEdge.Side.ONE))
            .strings("arrowMiddle", ble -> getDirectionAsString(ble.getValue().arrowMiddle()))
            .strings("arrow2", ble -> getBranchArrowAsString(ble.getValue(), BranchEdge.Side.TWO))
            .build();

    public static final DataframeMapper<Map<String, CustomLabelProvider.InjectionLabels>, Void> INJECTION_LABELS_MAPPER = new DataframeMapperBuilder<Map<String, CustomLabelProvider.InjectionLabels>,
        Map.Entry<String, CustomLabelProvider.InjectionLabels>, Void>()
        .itemsProvider(c -> c.entrySet()
            .stream()
            .map(x -> Map.entry(x.getKey(), x.getValue()))
            .toList())
        .stringsIndex("id", Map.Entry::getKey)
        .strings("labelInternal", ble -> ble.getValue().labelInternal())
        .strings("labelExternal", ble -> ble.getValue().labelExternal())
        .strings("arrow", ble -> getDirectionAsString(ble.getValue().arrow()))
        .build();

    public static final DataframeMapper<Map<String, CustomLabelProvider.ThreeWtLabels>, Void> TWT_LABELS_MAPPER = new DataframeMapperBuilder<Map<String, CustomLabelProvider.ThreeWtLabels>,
            Map.Entry<String, CustomLabelProvider.ThreeWtLabels>, Void>()
            .itemsProvider(c -> c.entrySet()
                    .stream()
                    .map(x -> Map.entry(x.getKey(), x.getValue()))
                    .toList())
            .stringsIndex("id", Map.Entry::getKey)
            .strings("side1Internal", ble -> ble.getValue().side1Internal())
            .strings("side1External", ble -> ble.getValue().side1External())
            .strings("side2Internal", ble -> ble.getValue().side2Internal())
            .strings("side2External", ble -> ble.getValue().side2External())
            .strings("side3Internal", ble -> ble.getValue().side3Internal())
            .strings("side3External", ble -> ble.getValue().side3External())
            .strings("arrow1", ble -> getTwtLegArrowAsString(ble.getValue(), ThreeWtEdge.Side.ONE))
            .strings("arrow2", ble -> getTwtLegArrowAsString(ble.getValue(), ThreeWtEdge.Side.TWO))
            .strings("arrow3", ble -> getTwtLegArrowAsString(ble.getValue(), ThreeWtEdge.Side.THREE))
            .build();

    public static final DataframeMapper<Map<String, String>, Void> BUS_DESCRIPTIONS_MAPPER = new DataframeMapperBuilder<Map<String, String>, Map.Entry<String, String>, Void>()
            .itemsProvider(c -> c.entrySet()
                    .stream()
                    .map(x -> Map.entry(x.getKey(), x.getValue()))
                    .toList())
            .stringsIndex("id", Map.Entry::getKey)
            .strings("description", Map.Entry::getValue)
            .build();

    public record VlInfos(String id, String type, String description) {
    }

    public static List<NetworkAreaDiagramUtil.VlInfos> getVlDescriptionsWithType(Map<String, List<String>> vlDescriptionMap, Map<String, List<String>> vlDetailsMap) {
        Objects.requireNonNull(vlDescriptionMap);
        Objects.requireNonNull(vlDetailsMap);
        return Stream.concat(
                vlDescriptionMap.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .map(value -> new NetworkAreaDiagramUtil.VlInfos(entry.getKey(), "HEADER", value))),
                vlDetailsMap.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .map(value -> new NetworkAreaDiagramUtil.VlInfos(entry.getKey(), "FOOTER", value)))
        ).toList();
    }

    public static List<NetworkAreaDiagramUtil.VlInfos> getVlDescriptionsWithType(Network network, SvgParameters svgParameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(svgParameters);
        LabelProvider labelProvider = new DefaultLabelProvider(network, svgParameters);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return getVlDescriptionsWithType(NetworkAreaDiagramUtil.getVoltageLevelDescriptionsMap(graph, labelProvider), NetworkAreaDiagramUtil.getVoltageLevelDetailsMap(graph, labelProvider));

    }

    public static final DataframeMapper<List<VlInfos>, Void> VL_DESCRIPTIONS_MAPPER = new DataframeMapperBuilder<List<VlInfos>, VlInfos, Void>()
        .itemsProvider(c -> c)
        .stringsIndex("id", x -> x.id)
        .strings("type", x -> x.type)
        .strings("description", x -> x.description)
        .build();

    private static EdgeInfoLabels getEdgeInfoLabels(EdgeInfo edgeInfo) {
        String labelA = "";
        String labelB = "";
        EdgeInfo.Direction direction = null;
        if (edgeInfo != null) {
            labelA = edgeInfo.getLabelA().orElse("");
            labelB = edgeInfo.getLabelB().orElse("");
            direction = edgeInfo.getDirection().orElse(null);
        }
        return new EdgeInfoLabels(labelA, labelB, direction);
    }

    private record EdgeInfoLabels(String labelA, String labelB, EdgeInfo.Direction direction) { }
}
