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

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkAreaDiagramUtil {

    private NetworkAreaDiagramUtil() {
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

    public record CustomLabelData(Map<String, CustomLabelProvider.BranchLabels> branchLabels, Map<String, CustomLabelProvider.ThreeWtLabels> threeWtLabels,
                                  Map<String, String> busDescriptions, Map<String, List<String>> vlDescriptions, Map<String, List<String>> vlDetails) {
    }

    public static Map<String, CustomLabelProvider.BranchLabels> getBranchLabelsMap(Graph graph, LabelProvider labelProvider) {

        return graph.getBranchEdgeStream()
                .filter(be -> !(BranchEdge.DANGLING_LINE_EDGE.equals(be.getType())))
                .collect(Collectors.toMap(Identifiable::getEquipmentId, branchEdge -> {
                    String branchId = branchEdge.getEquipmentId();
                    String type = branchEdge.getType();
                    EdgeInfo e1 = labelProvider.getBranchEdgeInfo(branchId, BranchEdge.Side.ONE, type).orElse(null);
                    String side1Label = "";
                    EdgeInfo.Direction dir1 = null;
                    if (e1 != null) {
                        side1Label = e1.getLabelB().orElse("");
                        dir1 = e1.getDirection().orElse(null);

                    }

                    EdgeInfo e2 = labelProvider.getBranchEdgeInfo(branchId, BranchEdge.Side.TWO, type).orElse(null);
                    String side2Label = "";
                    EdgeInfo.Direction dir2 = null;
                    if (e2 != null) {
                        side2Label = e2.getLabelB().orElse("");
                        dir2 = e2.getDirection().orElse(null);
                    }

                    EdgeInfo eMiddle = labelProvider.getBranchEdgeInfo(branchId, type).orElse(null);
                    String edgeLabel = "";
                    if (eMiddle != null) {
                        edgeLabel = eMiddle.getLabelB().orElse("");
                    }

                    return new CustomLabelProvider.BranchLabels(side1Label, null, edgeLabel, null,
                            side2Label, null, dir1, null, dir2);
                }));
    }

    public static Map<String, CustomLabelProvider.BranchLabels> getBranchLabelsMap(Network network, SvgParameters pars) {
        LabelProvider labelProvider = new DefaultLabelProvider(network, pars);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return NetworkAreaDiagramUtil.getBranchLabelsMap(graph, labelProvider);
    }

    private static CustomLabelProvider.ThreeWtLabels buildThreeWtLabelsRecord(List<ThreeWtEdge> dataList, Graph graph, LabelProvider labelProvider) {
        String side1 = null;
        String side2 = null;
        String side3 = null;
        EdgeInfo.Direction direction1 = null;
        EdgeInfo.Direction direction2 = null;
        EdgeInfo.Direction direction3 = null;

        for (ThreeWtEdge edge : dataList) {
            ThreeWtEdge.Side side = edge.getSide();
            Optional<EdgeInfo> edgeInfo = labelProvider.getThreeWindingTransformerEdgeInfo(edge.getEquipmentId(), side);
            if (edgeInfo.isPresent()) {
                EdgeInfo.Direction dir = edgeInfo.get().getDirection().orElse(null);
                String label = edgeInfo.get().getLabelB().orElse("");

                switch (side) {
                    case ONE -> {
                        side1 = label;
                        direction1 = dir;
                    }
                    case TWO -> {
                        side2 = label;
                        direction2 = dir;
                    }
                    case THREE -> {
                        side3 = label;
                        direction3 = dir;
                    }
                }
            }
        }
        return new CustomLabelProvider.ThreeWtLabels(side1, null, side2, null, side3, null,
                direction1, direction2, direction3);
    }

    public static Map<String, CustomLabelProvider.ThreeWtLabels> getThreeWtBranchLabelsMap(Graph graph, LabelProvider labelProvider) {
        return graph.getThreeWtEdgesStream().collect(Collectors.groupingBy(
                ThreeWtEdge::getEquipmentId,
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> buildThreeWtLabelsRecord(list, graph, labelProvider)
                )
        ));
    }

    public static Map<String, CustomLabelProvider.ThreeWtLabels> getThreeWtBranchLabelsMap(Network network, SvgParameters pars) {
        LabelProvider labelProvider = new DefaultLabelProvider(network, pars);
        Graph graph = new NetworkGraphBuilder(network, new LayoutParameters()).buildGraph();
        return NetworkAreaDiagramUtil.getThreeWtBranchLabelsMap(graph, labelProvider);
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
            .strings("side1", ble -> ble.getValue().side1Internal())
            .strings("middle", ble -> ble.getValue().middle1())
            .strings("side2", ble -> ble.getValue().side2Internal())
            .strings("arrow1", ble -> getBranchArrowAsString(ble.getValue(), BranchEdge.Side.ONE))
            .strings("arrow2", ble -> getBranchArrowAsString(ble.getValue(), BranchEdge.Side.TWO))
            .build();

    public static final DataframeMapper<Map<String, CustomLabelProvider.ThreeWtLabels>, Void> TWT_LABELS_MAPPER = new DataframeMapperBuilder<Map<String, CustomLabelProvider.ThreeWtLabels>,
            Map.Entry<String, CustomLabelProvider.ThreeWtLabels>, Void>()
            .itemsProvider(c -> c.entrySet()
                    .stream()
                    .map(x -> Map.entry(x.getKey(), x.getValue()))
                    .toList())
            .stringsIndex("id", Map.Entry::getKey)
            .strings("side1", ble -> ble.getValue().side1Internal())
            .strings("side2", ble -> ble.getValue().side2Internal())
            .strings("side3", ble -> ble.getValue().side3Internal())
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
}
