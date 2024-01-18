/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.commons.parameters.Parameter;
import com.powsybl.commons.parameters.ParameterType;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.flow_decomposition.FlowDecompositionResults;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.dataframe.CDataframeHandler;
import com.powsybl.python.flow_decomposition.XnecWithDecompositionContext;
import com.powsybl.python.security.BranchResultContext;
import com.powsybl.python.security.BusResultContext;
import com.powsybl.python.security.LimitViolationContext;
import com.powsybl.python.security.ThreeWindingsTransformerResultContext;
import com.powsybl.python.shortcircuit.*;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.shortcircuit.*;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Integer.MIN_VALUE;

/**
 * Mappers to dataframes.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class Dataframes {

    private static final DataframeMapper<Importer> IMPORTER_PARAMETERS_MAPPER = new DataframeMapperBuilder<Importer, Parameter>()
            .itemsProvider(Importer::getParameters)
            .stringsIndex("name", Parameter::getName)
            .strings("description", Parameter::getDescription)
            .enums("type", ParameterType.class, Parameter::getType)
            .strings("default", p -> Objects.toString(p.getDefaultValue(), ""))
            .strings("possible_values", p -> p.getPossibleValues() == null ? "" : p.getPossibleValues().toString())
            .build();

    private static final DataframeMapper<Exporter> EXPORTER_PARAMETERS_MAPPER = new DataframeMapperBuilder<Exporter, Parameter>()
            .itemsProvider(Exporter::getParameters)
            .stringsIndex("name", Parameter::getName)
            .strings("description", Parameter::getDescription)
            .enums("type", ParameterType.class, Parameter::getType)
            .strings("default", p -> Objects.toString(p.getDefaultValue(), ""))
            .strings("possible_values", p -> p.getPossibleValues() == null ? "" : p.getPossibleValues().toString())
            .build();

    private static final DataframeMapper<SecurityAnalysisResult> BRANCH_RESULTS_MAPPER = createBranchResultsMapper();
    private static final DataframeMapper<SecurityAnalysisResult> T3WT_RESULTS_MAPPER = createThreeWindingsTransformersResults();
    private static final DataframeMapper<SecurityAnalysisResult> BUS_RESULTS_MAPPER = createBusResultsMapper();
    private static final DataframeMapper<SecurityAnalysisResult> LIMIT_VIOLATIONS_MAPPER = createLimitViolationsMapper();
    private static final DataframeMapper<VoltageLevel.NodeBreakerView> NODE_BREAKER_VIEW_SWITCHES_MAPPER = createNodeBreakerViewSwitchesMapper();
    private static final DataframeMapper<VoltageLevel.NodeBreakerView> NODE_BREAKER_VIEW_NODES_MAPPER = createNodeBreakerViewNodes();
    private static final DataframeMapper<VoltageLevel.NodeBreakerView> NODE_BREAKER_VIEW_INTERNAL_CONNECTION_MAPPER = createNodeBreakerViewInternalConnections();
    private static final DataframeMapper<VoltageLevel.BusBreakerView> BUS_BREAKER_VIEW_SWITCHES_MAPPER = createBusBreakerViewSwitchesMapper();
    private static final DataframeMapper<VoltageLevel> BUS_BREAKER_VIEW_BUSES_MAPPER = createBusBreakerViewBuses();
    private static final DataframeMapper<VoltageLevel> BUS_BREAKER_VIEW_ELEMENTS_MAPPER = createBusBreakerViewElements();

    private static final DataframeMapper<Map<String, List<ConnectablePosition.Feeder>>> FEEDER_MAP_MAPPER = createFeederMapDataframe();

    private Dataframes() {
    }

    /**
     * Maps an object to a C struct using the provided mapper.
     */
    public static <T> ArrayPointer<SeriesPointer> createCDataframe(DataframeMapper<T> mapper, T object) {
        return createCDataframe(mapper, object, new DataframeFilter());
    }

    public static <T> ArrayPointer<SeriesPointer> createCDataframe(DataframeMapper<T> mapper, T object, DataframeFilter dataframeFilter) {
        CDataframeHandler handler = new CDataframeHandler();
        mapper.createDataframe(object, handler, dataframeFilter);
        return handler.getDataframePtr();
    }

    /**
     * Maps an object to java series
     */
    public static <T> List<Series> createSeries(DataframeMapper<T> mapper, T object) {
        List<Series> series = new ArrayList<>();
        mapper.createDataframe(object, new DefaultDataframeHandler(series::add), new DataframeFilter());
        return List.copyOf(series);
    }

    /**
     * A mapper which maps an importer to a dataframe containing its parameters.
     */
    static DataframeMapper<com.powsybl.iidm.network.Importer> importerParametersMapper() {
        return IMPORTER_PARAMETERS_MAPPER;
    }

    static DataframeMapper<Exporter> exporterParametersMapper() {
        return EXPORTER_PARAMETERS_MAPPER;
    }

    public static DataframeMapper<SecurityAnalysisResult> branchResultsMapper() {
        return BRANCH_RESULTS_MAPPER;
    }

    public static DataframeMapper<SecurityAnalysisResult> busResultsMapper() {
        return BUS_RESULTS_MAPPER;
    }

    public static DataframeMapper<SecurityAnalysisResult> threeWindingsTransformerResultsMapper() {
        return T3WT_RESULTS_MAPPER;
    }

    public static DataframeMapper<SecurityAnalysisResult> limitViolationsMapper() {
        return LIMIT_VIOLATIONS_MAPPER;
    }

    public static DataframeMapper<VoltageLevel.NodeBreakerView> nodeBreakerViewSwitches() {
        return NODE_BREAKER_VIEW_SWITCHES_MAPPER;
    }

    public static DataframeMapper<VoltageLevel.NodeBreakerView> nodeBreakerViewNodes() {
        return NODE_BREAKER_VIEW_NODES_MAPPER;
    }

    public static DataframeMapper<VoltageLevel.NodeBreakerView> nodeBreakerViewInternalConnection() {
        return NODE_BREAKER_VIEW_INTERNAL_CONNECTION_MAPPER;
    }

    public static DataframeMapper<VoltageLevel.BusBreakerView> busBreakerViewSwitches() {
        return BUS_BREAKER_VIEW_SWITCHES_MAPPER;
    }

    public static DataframeMapper<VoltageLevel> busBreakerViewBuses() {
        return BUS_BREAKER_VIEW_BUSES_MAPPER;
    }

    public static DataframeMapper<VoltageLevel> busBreakerViewElements() {
        return BUS_BREAKER_VIEW_ELEMENTS_MAPPER;
    }

    public static DataframeMapper<Map<String, List<ConnectablePosition.Feeder>>> feederMapMapper() {
        return FEEDER_MAP_MAPPER;
    }

    private static List<BranchResultContext> getBranchResults(SecurityAnalysisResult result) {
        List<BranchResultContext> branchResults = result.getPreContingencyResult().getNetworkResult()
                .getBranchResults().stream()
                .map(branchResult -> new BranchResultContext(branchResult, null))
                .collect(Collectors.toList());
        result.getPostContingencyResults().forEach(postContingencyResult ->
            postContingencyResult.getNetworkResult().getBranchResults()
                    .forEach(branchResult -> branchResults.add(new BranchResultContext(branchResult, postContingencyResult.getContingency().getId()))));
        return branchResults;
    }

    private static DataframeMapper<SecurityAnalysisResult> createBranchResultsMapper() {
        return new DataframeMapperBuilder<SecurityAnalysisResult, BranchResultContext>()
                .itemsProvider(Dataframes::getBranchResults)
                .stringsIndex("contingency_id", BranchResultContext::getContingencyId)
                .stringsIndex("branch_id", BranchResultContext::getBranchId)
                .doubles("p1", BranchResultContext::getP1)
                .doubles("q1", BranchResultContext::getQ1)
                .doubles("i1", BranchResultContext::getI1)
                .doubles("p2", BranchResultContext::getP2)
                .doubles("q2", BranchResultContext::getQ2)
                .doubles("i2", BranchResultContext::getI2)
                .doubles("flow_transfer", BranchResultContext::getFlowTransfer)
                .build();
    }

    private static List<BusResultContext> getBusResults(SecurityAnalysisResult result) {
        List<BusResultContext> busResults = result.getPreContingencyResult()
                .getNetworkResult()
                .getBusResults().stream()
                .map(busResult -> new BusResultContext(busResult, null))
                .collect(Collectors.toList());
        result.getPostContingencyResults().forEach(postContingencyResult ->
            postContingencyResult.getNetworkResult().getBusResults()
                    .forEach(busResult -> busResults.add(new BusResultContext(busResult, postContingencyResult.getContingency().getId()))));
        return busResults;
    }

    private static DataframeMapper<SecurityAnalysisResult> createBusResultsMapper() {
        return new DataframeMapperBuilder<SecurityAnalysisResult, BusResultContext>()
                .itemsProvider(Dataframes::getBusResults)
                .stringsIndex("contingency_id", BusResultContext::getContingencyId)
                .stringsIndex("voltage_level_id", BusResultContext::getVoltageLevelId)
                .stringsIndex("bus_id", BusResultContext::getBusId)
                .doubles("v_mag", BusResultContext::getV)
                .doubles("v_angle", BusResultContext::getAngle)
                .build();
    }

    private static List<ThreeWindingsTransformerResultContext> getThreeWindingsTransformerResults(SecurityAnalysisResult result) {
        List<ThreeWindingsTransformerResultContext> threeWindingsTransformerResults = result.getPreContingencyResult()
                .getNetworkResult().getThreeWindingsTransformerResults().stream()
                .map(threeWindingsTransformerResult -> new ThreeWindingsTransformerResultContext(threeWindingsTransformerResult, null))
                .collect(Collectors.toList());
        result.getPostContingencyResults().forEach(postContingencyResult ->
            postContingencyResult.getNetworkResult().getThreeWindingsTransformerResults()
                    .forEach(threeWindingsTransformerResult ->
                            threeWindingsTransformerResults.add(new ThreeWindingsTransformerResultContext(threeWindingsTransformerResult,
                                    postContingencyResult.getContingency().getId()))));
        return threeWindingsTransformerResults;
    }

    private static DataframeMapper<SecurityAnalysisResult> createThreeWindingsTransformersResults() {
        return new DataframeMapperBuilder<SecurityAnalysisResult, ThreeWindingsTransformerResultContext>()
                .itemsProvider(Dataframes::getThreeWindingsTransformerResults)
                .stringsIndex("contingency_id", ThreeWindingsTransformerResultContext::getContingencyId)
                .stringsIndex("transformer_id", ThreeWindingsTransformerResultContext::getThreeWindingsTransformerId)
                .doubles("p1", ThreeWindingsTransformerResultContext::getP1)
                .doubles("q1", ThreeWindingsTransformerResultContext::getQ1)
                .doubles("i1", ThreeWindingsTransformerResultContext::getI1)
                .doubles("p2", ThreeWindingsTransformerResultContext::getP2)
                .doubles("q2", ThreeWindingsTransformerResultContext::getQ2)
                .doubles("i2", ThreeWindingsTransformerResultContext::getI2)
                .doubles("p3", ThreeWindingsTransformerResultContext::getP3)
                .doubles("q3", ThreeWindingsTransformerResultContext::getQ3)
                .doubles("i3", ThreeWindingsTransformerResultContext::getI3)
                .build();
    }

    private static List<LimitViolationContext> getLimitViolations(SecurityAnalysisResult result) {
        List<LimitViolationContext> limitViolations = result.getPreContingencyResult().getLimitViolationsResult().getLimitViolations()
                .stream().map(limitViolation -> new LimitViolationContext("", limitViolation)).collect(Collectors.toList());
        result.getPostContingencyResults()
                .forEach(postContingencyResult -> limitViolations.addAll(postContingencyResult.getLimitViolationsResult()
                        .getLimitViolations().stream()
                        .map(limitViolation -> new LimitViolationContext(postContingencyResult.getContingency().getId(), limitViolation))
                        .toList()));
        return limitViolations;
    }

    private static DataframeMapper<SecurityAnalysisResult> createLimitViolationsMapper() {
        return new DataframeMapperBuilder<SecurityAnalysisResult, LimitViolationContext>()
                .itemsProvider(Dataframes::getLimitViolations)
                .stringsIndex("contingency_id", LimitViolationContext::getContingencyId)
                .stringsIndex("subject_id", LimitViolation::getSubjectId)
                .strings("subject_name", p -> Objects.toString(p.getSubjectName(), ""))
                .enums("limit_type", LimitViolationType.class, LimitViolation::getLimitType)
                .strings("limit_name", p -> Objects.toString(p.getLimitName(), ""))
                .doubles("limit", LimitViolation::getLimit)
                .ints("acceptable_duration", LimitViolation::getAcceptableDuration)
                .doubles("limit_reduction", LimitViolation::getLimitReduction)
                .doubles("value", LimitViolation::getValue)
                .strings("side", p -> Objects.toString(p.getSide(), ""))
                .build();
    }

    private static List<NodeBreakerViewSwitchContext> getNodeBreakerViewSwitches(VoltageLevel.NodeBreakerView nodeBreakerView) {
        return IteratorUtils.toList(nodeBreakerView.getSwitches().iterator()).stream().map(switchContext ->
                        new NodeBreakerViewSwitchContext(switchContext,
                                nodeBreakerView.getNode1(switchContext.getId()),
                                nodeBreakerView.getNode2(switchContext.getId()))).toList();
    }

    private static DataframeMapper<VoltageLevel.NodeBreakerView> createNodeBreakerViewSwitchesMapper() {
        return new DataframeMapperBuilder<VoltageLevel.NodeBreakerView, NodeBreakerViewSwitchContext>()
                .itemsProvider(Dataframes::getNodeBreakerViewSwitches)
                .stringsIndex("id", nodeBreakerViewSwitchContext -> nodeBreakerViewSwitchContext.getSwitchContext().getId())
                .strings("name", nodeBreakerViewSwitchContext -> nodeBreakerViewSwitchContext.getSwitchContext().getOptionalName().orElse(""))
                .enums("kind", SwitchKind.class, nodeBreakerViewSwitchContext -> nodeBreakerViewSwitchContext.getSwitchContext().getKind())
                .booleans("open", nodeBreakerViewSwitchContext -> nodeBreakerViewSwitchContext.getSwitchContext().isOpen())
                .booleans("retained", nodeBreakerViewSwitchContext -> nodeBreakerViewSwitchContext.getSwitchContext().isRetained())
                .ints("node1", NodeBreakerViewSwitchContext::getNode1)
                .ints("node2", NodeBreakerViewSwitchContext::getNode2)
                .build();
    }

    private static List<NodeContext> getNodeBreakerViewNodes(VoltageLevel.NodeBreakerView nodeBreakerView) {
        List<Integer> nodes = Arrays.stream(nodeBreakerView.getNodes()).boxed().toList();
        return nodes.stream().map(node -> {
            Terminal terminal = nodeBreakerView.getTerminal(node);
            if (terminal == null) {
                return new NodeContext(node, null);
            } else {
                return new NodeContext(node, terminal.getConnectable().getId());
            }
        }).toList();
    }

    private static DataframeMapper<VoltageLevel.NodeBreakerView> createNodeBreakerViewNodes() {
        return new DataframeMapperBuilder<VoltageLevel.NodeBreakerView, NodeContext>()
                .itemsProvider(Dataframes::getNodeBreakerViewNodes)
                .intsIndex("node", NodeContext::getNode)
                .strings("connectable_id", node -> Objects.toString(node.getConnectableId(), ""))
                .build();
    }

    private static List<InternalConnectionContext> getNodeBreakerViewInternalConnections(VoltageLevel.NodeBreakerView nodeBreakerView) {
        List<VoltageLevel.NodeBreakerView.InternalConnection> internalConnectionContextList = IteratorUtils
                .toList(nodeBreakerView.getInternalConnections().iterator());
        return internalConnectionContextList.stream()
                .map(internalConnection ->
                        new InternalConnectionContext(internalConnection, internalConnectionContextList.indexOf(internalConnection)))
                .toList();
    }

    private static DataframeMapper<VoltageLevel.NodeBreakerView> createNodeBreakerViewInternalConnections() {
        return new DataframeMapperBuilder<VoltageLevel.NodeBreakerView, InternalConnectionContext>()
                .itemsProvider(Dataframes::getNodeBreakerViewInternalConnections)
                .intsIndex("id", InternalConnectionContext::getIndex)
                .ints("node1", internalConnectionContext -> internalConnectionContext.getInternalConnection().getNode1())
                .ints("node2", internalConnectionContext -> internalConnectionContext.getInternalConnection().getNode2())
                .build();
    }

    private static List<BusBreakerViewSwitchContext> getBusBreakerViewSwitches(VoltageLevel.BusBreakerView busBreakerView) {
        return IteratorUtils.toList(busBreakerView.getSwitches().iterator()).
                stream()
                .map(switchDevice -> new BusBreakerViewSwitchContext(switchDevice,
                        busBreakerView.getBus1(switchDevice.getId()).getId(), busBreakerView.getBus2(switchDevice.getId()).getId())).toList();
    }

    private static DataframeMapper<VoltageLevel.BusBreakerView> createBusBreakerViewSwitchesMapper() {
        return new DataframeMapperBuilder<VoltageLevel.BusBreakerView, BusBreakerViewSwitchContext>()
                .itemsProvider(Dataframes::getBusBreakerViewSwitches)
                .stringsIndex("id", context -> context.getSwitchContext().getId())
                .enums("kind", SwitchKind.class, context -> context.getSwitchContext().getKind())
                .booleans("open", context -> context.getSwitchContext().isOpen())
                .strings("bus1_id", BusBreakerViewSwitchContext::getBusId1)
                .strings("bus2_id", BusBreakerViewSwitchContext::getBusId2)
                .build();
    }

    private static List<BusBreakerViewBusData> getBusBreakerViewBuses(VoltageLevel voltageLevel) {
        return voltageLevel.getBusBreakerView().getBusStream().map(bus -> {
            Bus busViewBus = bus.getConnectedTerminalStream()
                    .map(t -> t.getBusView().getBus())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            return new BusBreakerViewBusData(bus, busViewBus);
        }).toList();

    }

    private static DataframeMapper<VoltageLevel> createBusBreakerViewBuses() {
        return new DataframeMapperBuilder<VoltageLevel, BusBreakerViewBusData>()
                .itemsProvider(Dataframes::getBusBreakerViewBuses)
                .stringsIndex("id", BusBreakerViewBusData::getId)
                .strings("name", BusBreakerViewBusData::getName)
                .strings("bus_id", BusBreakerViewBusData::getBusViewBusId)
                .build();
    }

    private static List<Pair<String, ConnectablePosition.Feeder>> getPositions(Map<String, List<ConnectablePosition.Feeder>> map) {
        Set<Map.Entry<String, List<ConnectablePosition.Feeder>>> entriesSet = map.entrySet();
        List<Pair<String, ConnectablePosition.Feeder>> entriesList = new ArrayList<>(entriesSet.size());
        entriesSet.forEach(e -> e.getValue().forEach(feeder -> entriesList.add(Pair.of(e.getKey(), feeder))));
        return entriesList;
    }

    private static DataframeMapper<Map<String, List<ConnectablePosition.Feeder>>> createFeederMapDataframe() {
        return new DataframeMapperBuilder<Map<String, List<ConnectablePosition.Feeder>>, Pair<String, ConnectablePosition.Feeder>>()
                .itemsProvider(Dataframes::getPositions)
                .stringsIndex("connectable_id", Pair::getLeft)
                .ints("order_position", pair -> pair.getRight().getOrder().orElse(MIN_VALUE))
                .strings("extension_name", pair -> pair.getRight().getName().orElse(null))
                .build();
    }

    private static String getBusBreakerBusId(Terminal terminal) {
        Bus bus = terminal.getBusBreakerView().getBus();
        return bus != null ? bus.getId() : "";
    }

    private static void addConnectableData(VoltageLevel voltageLevel, Connectable<?> connectable, Consumer<BusBreakerViewElementData> consumer) {
        if (connectable instanceof Injection<?> inj) {
            String busId = getBusBreakerBusId(inj.getTerminal());
            consumer.accept(new BusBreakerViewElementData(connectable.getType(), busId, connectable.getId()));
        } else if (connectable instanceof Branch<?> branch) {
            if (branch.getTerminal1().getVoltageLevel() == voltageLevel) {
                String busId = getBusBreakerBusId(branch.getTerminal1());
                consumer.accept(new BusBreakerViewElementData(connectable.getType(), busId, connectable.getId(), SideEnum.ONE));
            }
            if (branch.getTerminal2().getVoltageLevel() == voltageLevel) {
                String busId = getBusBreakerBusId(branch.getTerminal2());
                consumer.accept(new BusBreakerViewElementData(connectable.getType(), busId, connectable.getId(), SideEnum.TWO));
            }
        } else if (connectable instanceof ThreeWindingsTransformer transfo) {
            if (transfo.getLeg1().getTerminal().getVoltageLevel() == voltageLevel) {
                String busId = getBusBreakerBusId(transfo.getLeg1().getTerminal());
                consumer.accept(new BusBreakerViewElementData(connectable.getType(), busId, transfo.getId(), SideEnum.ONE));
            }
            if (transfo.getLeg2().getTerminal().getVoltageLevel() == voltageLevel) {
                String busId = getBusBreakerBusId(transfo.getLeg2().getTerminal());
                consumer.accept(new BusBreakerViewElementData(connectable.getType(), busId, transfo.getId(), SideEnum.TWO));
            }
            if (transfo.getLeg3().getTerminal().getVoltageLevel() == voltageLevel) {
                String busId = getBusBreakerBusId(transfo.getLeg3().getTerminal());
                consumer.accept(new BusBreakerViewElementData(connectable.getType(), busId, transfo.getId(), SideEnum.THREE));
            }
        }
    }

    private static List<BusBreakerViewElementData> getBusBreakerViewElements(VoltageLevel voltageLevel) {
        List<BusBreakerViewElementData> result = new ArrayList<>();
        voltageLevel.getConnectableStream()
                .forEach(connectable -> addConnectableData(voltageLevel, connectable, result::add));
        return result;
    }

    private static DataframeMapper<VoltageLevel> createBusBreakerViewElements() {
        return new DataframeMapperBuilder<VoltageLevel, BusBreakerViewElementData>()
                .itemsProvider(Dataframes::getBusBreakerViewElements)
                .stringsIndex("id", BusBreakerViewElementData::getElementId)
                .strings("type", elementContext -> elementContext.getType().toString())
                .strings("bus_id", BusBreakerViewElementData::getBusId)
                .strings("side", elementContext -> elementContext.getSide().isPresent() ? elementContext.getSide().get().toString() : "")
                .build();
    }

    public static DataframeMapper<FlowDecompositionResults> flowDecompositionMapper(Set<Country> zoneSet) {
        return new DataframeMapperBuilder<FlowDecompositionResults, XnecWithDecompositionContext>()
                .itemsProvider(Dataframes::getXnecWithDecompositions)
                .stringsIndex("xnec_id", XnecWithDecompositionContext::getId)
                .strings("branch_id", XnecWithDecompositionContext::getBranchId)
                .strings("contingency_id", XnecWithDecompositionContext::getContingencyId)
                .strings("country1", XnecWithDecompositionContext::getCountry1String)
                .strings("country2", XnecWithDecompositionContext::getCountry2String)
                .doubles("ac_reference_flow", XnecWithDecompositionContext::getAcReferenceFlow)
                .doubles("dc_reference_flow", XnecWithDecompositionContext::getDcReferenceFlow)
                .doubles("commercial_flow", XnecWithDecompositionContext::getAllocatedFlow)
                .doubles("x_node_flow", XnecWithDecompositionContext::getXNodeFlow)
                .doubles("pst_flow", XnecWithDecompositionContext::getPstFlow)
                .doubles("internal_flow", XnecWithDecompositionContext::getInternalFlow)
                .doubles(XnecWithDecompositionContext.getLoopFlowsFunctionMap(zoneSet))
                .build();
    }

    private static List<XnecWithDecompositionContext> getXnecWithDecompositions(FlowDecompositionResults flowDecompositionResults) {
        return flowDecompositionResults.getDecomposedFlowMap().values().stream()
                .map(XnecWithDecompositionContext::new)
                .sorted(Comparator.comparing(XnecWithDecompositionContext::getId))
                .toList();
    }

    // shortcircuit
    public static DataframeMapper<ShortCircuitAnalysisResult> shortCircuitAnalysisFaultResultsMapper(boolean withFortescueResult) {
        return withFortescueResult ? SHORT_CIRCUIT_FORTESCUE_RESULTS_MAPPER : SHORT_CIRCUIT_MAGNITUDE_RESULTS_MAPPER;
    }

    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_MAGNITUDE_RESULTS_MAPPER = createMagnitudeFaultResultsMapper();
    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_FORTESCUE_RESULTS_MAPPER = createFortescueFaultResultsMapper();

    private static List<MagnitudeFaultResult> getMagnitudeFaultResults(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream().filter(MagnitudeFaultResult.class::isInstance)
                .map(f -> (MagnitudeFaultResult) f)
                .toList();
    }

    private static List<FortescueFaultResult> getFortescueFaultResults(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream().filter(FortescueFaultResult.class::isInstance)
                .map(f -> (FortescueFaultResult) f)
                .toList();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createMagnitudeFaultResultsMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, MagnitudeFaultResult>()
                .itemsProvider(Dataframes::getMagnitudeFaultResults)
                .stringsIndex("id", f -> f.getFault().getId())
                .enums("status", FaultResult.Status.class, f -> f.getStatus())
                .doubles("short_circuit_power", MagnitudeFaultResult::getShortCircuitPower)
                .strings("time_constant", f -> f.getTimeConstant() != null ? f.getTimeConstant().toString() : null)
                .doubles("current", MagnitudeFaultResult::getCurrent)
                .doubles("voltage", MagnitudeFaultResult::getVoltage)
                .build();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createFortescueFaultResultsMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, FortescueFaultResult>()
                .itemsProvider(Dataframes::getFortescueFaultResults)
                .stringsIndex("id", f -> f.getFault().getId())
                .enums("status", FaultResult.Status.class, f -> f.getStatus())
                .doubles("short_circuit_power", FortescueFaultResult::getShortCircuitPower)
                .strings("time_constant", f -> f.getTimeConstant() != null ? f.getTimeConstant().toString() : null)
                .doubles("current_negative_angle", fortescueFaultResult -> fortescueFaultResult.getCurrent().getNegativeAngle())
                .doubles("current_positive_angle", fortescueFaultResult -> fortescueFaultResult.getCurrent().getPositiveAngle())
                .doubles("current_negative_magnitude", fortescueFaultResult -> fortescueFaultResult.getCurrent().getNegativeMagnitude())
                .doubles("current_positive_magnitude", fortescueFaultResult -> fortescueFaultResult.getCurrent().getPositiveMagnitude())
                .doubles("current_zero_magnitude", fortescueFaultResult -> fortescueFaultResult.getCurrent().getZeroMagnitude())
                .doubles("current_zero_angle", fortescueFaultResult -> fortescueFaultResult.getCurrent().getZeroAngle())
                .doubles("voltage_negative_angle", fortescueFaultResult -> fortescueFaultResult.getVoltage().getNegativeAngle())
                .doubles("voltage_positive_angle", fortescueFaultResult -> fortescueFaultResult.getVoltage().getPositiveAngle())
                .doubles("voltage_negative_magnitude", fortescueFaultResult -> fortescueFaultResult.getVoltage().getNegativeMagnitude())
                .doubles("voltage_positive_magnitude", fortescueFaultResult -> fortescueFaultResult.getVoltage().getPositiveMagnitude())
                .doubles("voltage_zero_magnitude", fortescueFaultResult -> fortescueFaultResult.getVoltage().getZeroMagnitude())
                .doubles("voltage_zero_angle", fortescueFaultResult -> fortescueFaultResult.getVoltage().getZeroAngle())
                .build();
    }

    public static DataframeMapper<ShortCircuitAnalysisResult> shortCircuitAnalysisLimitViolationsResultsMapper() {
        return SHORT_CIRCUIT_LIMIT_VIOLATIONS_RESULTS_MAPPER;
    }

    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_LIMIT_VIOLATIONS_RESULTS_MAPPER = createLimitViolationsFaultMapper();

    public static List<LimitViolationFaultContext> getFaultLimitViolations(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream()
                .flatMap(a -> a.getLimitViolations()
                        .stream()
                        .map(ss -> new LimitViolationFaultContext(a.getFault().getId(), ss)))
                .toList();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createLimitViolationsFaultMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, LimitViolationFaultContext>()
                .itemsProvider(Dataframes::getFaultLimitViolations)
                .stringsIndex("id", LimitViolationFaultContext::getFaultId)
                .stringsIndex("subject_id", LimitViolation::getSubjectId)
                .strings("subject_name", p -> Objects.toString(p.getSubjectName(), ""))
                .enums("limit_type", LimitViolationType.class, LimitViolation::getLimitType)
                .strings("limit_name", p -> Objects.toString(p.getLimitName(), ""))
                .doubles("limit", LimitViolation::getLimit)
                .ints("acceptable_duration", LimitViolation::getAcceptableDuration)
                .doubles("limit_reduction", LimitViolation::getLimitReduction)
                .doubles("value", LimitViolation::getValue)
                .strings("side", p -> Objects.toString(p.getSide(), ""))
                .build();
    }

    public static DataframeMapper<ShortCircuitAnalysisResult> shortCircuitAnalysisMagnitudeFeederResultsMapper(boolean withFortescueResult) {
        return withFortescueResult ? SHORT_CIRCUIT_FORTESCUE_FEEDER_RESULTS_MAPPER : SHORT_CIRCUIT_MAGNITUDE_FEEDER_RESULTS_MAPPER;
    }

    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_MAGNITUDE_FEEDER_RESULTS_MAPPER = createMagnitudeFeederMapper();
    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_FORTESCUE_FEEDER_RESULTS_MAPPER = createFortescueFeederMapper();

    public static List<MagnitudeFeederResultContext> getMagnitudeFeederResultContexts(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream()
                .flatMap(a -> a.getFeederResults()
                        .stream()
                        .map(ss -> new MagnitudeFeederResultContext(a.getFault().getId(), (MagnitudeFeederResult) ss)))
                .toList();
    }

    public static List<FortescueFeederResultContext> getFortescueFeederResultContexts(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream()
                .flatMap(a -> a.getFeederResults()
                        .stream()
                        .map(ss -> new FortescueFeederResultContext(a.getFault().getId(), (FortescueFeederResult) ss)))
                .toList();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createMagnitudeFeederMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, MagnitudeFeederResultContext>()
                .itemsProvider(Dataframes::getMagnitudeFeederResultContexts)
                .stringsIndex("id", MagnitudeFeederResultContext::getFaultId)
                .stringsIndex("connectable_id", MagnitudeFeederResultContext::getConnectableId)
                .doubles("current", MagnitudeFeederResultContext::getCurrent)
                .build();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createFortescueFeederMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, FortescueFeederResultContext>()
                .itemsProvider(Dataframes::getFortescueFeederResultContexts)
                .stringsIndex("id", FortescueFeederResultContext::getFaultId)
                .stringsIndex("connectable_id", FortescueFeederResultContext::getConnectableId)
                .doubles("current_negative_angle", fortescueFeederResultContext ->
                        fortescueFeederResultContext.getCurrent().getNegativeAngle())
                .doubles("current_positive_angle", fortescueFeederResultContext ->
                        fortescueFeederResultContext.getCurrent().getPositiveAngle())
                .doubles("current_negative_magnitude", fortescueFeederResultContext ->
                        fortescueFeederResultContext.getCurrent().getNegativeMagnitude())
                .doubles("current_positive_magnitude", fortescueFeederResultContext ->
                        fortescueFeederResultContext.getCurrent().getPositiveMagnitude())
                .doubles("current_zero_magnitude", fortescueFeederResultContext ->
                        fortescueFeederResultContext.getCurrent().getZeroMagnitude())
                .doubles("current_zero_angle", fortescueFeederResultContext ->
                        fortescueFeederResultContext.getCurrent().getZeroAngle())
                .build();
    }

    public static DataframeMapper<ShortCircuitAnalysisResult> shortCircuitAnalysisMagnitudeBusResultsMapper(boolean withFortescueResult) {
        return withFortescueResult ? SHORT_CIRCUIT_FORTESCUE_BUS_RESULTS_MAPPER : SHORT_CIRCUIT_MAGNITUDE_BUS_RESULTS_MAPPER;
    }

    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_MAGNITUDE_BUS_RESULTS_MAPPER = createMagnitudeBusResultsFaultMapper();
    private static final DataframeMapper<ShortCircuitAnalysisResult> SHORT_CIRCUIT_FORTESCUE_BUS_RESULTS_MAPPER = createFortescueBusResultsFaultMapper();

    public static List<MagnitudeBusResultsContext> getMagnitudeBusResultsContexts(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream()
                .flatMap(a -> a.getShortCircuitBusResults()
                        .stream()
                        .map(ss -> new MagnitudeBusResultsContext(a.getFault().getId(), (MagnitudeShortCircuitBusResults) ss)))
                .toList();
    }

    public static List<FortescueBusResultsContext> getFortescueBusResultsContexts(ShortCircuitAnalysisResult result) {
        return result.getFaultResults().stream()
                .flatMap(a -> a.getShortCircuitBusResults()
                        .stream()
                        .map(ss -> new FortescueBusResultsContext(a.getFault().getId(), (FortescueShortCircuitBusResults) ss)))
                .toList();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createMagnitudeBusResultsFaultMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, MagnitudeBusResultsContext>()
                .itemsProvider(Dataframes::getMagnitudeBusResultsContexts)
                .stringsIndex("id", MagnitudeBusResultsContext::getFaultId)
                .stringsIndex("voltage_level_id", MagnitudeBusResultsContext::getVoltageLevelId)
                .stringsIndex("bus_id", MagnitudeBusResultsContext::getBusId)
                .doubles("initial_voltage_magnitude", MagnitudeBusResultsContext::getInitialVoltageMagnitude)
                .doubles("voltage_drop_proportional", MagnitudeBusResultsContext::getVoltageDropProportional)
                .doubles("voltage", MagnitudeBusResultsContext::getVoltage)
                .build();
    }

    private static DataframeMapper<ShortCircuitAnalysisResult> createFortescueBusResultsFaultMapper() {
        return new DataframeMapperBuilder<ShortCircuitAnalysisResult, FortescueBusResultsContext>()
                .itemsProvider(Dataframes::getFortescueBusResultsContexts)
                .stringsIndex("id", FortescueBusResultsContext::getFaultId)
                .stringsIndex("voltage_level_id", FortescueBusResultsContext::getVoltageLevelId)
                .stringsIndex("bus_id", FortescueBusResultsContext::getBusId)
                .doubles("initial_voltage_magnitude", FortescueBusResultsContext::getInitialVoltageMagnitude)
                .doubles("voltage_drop_proportional", FortescueBusResultsContext::getVoltageDropProportional)
                .doubles("voltage_negative_angle", fortescueBusResultsContext
                        -> fortescueBusResultsContext.getVoltage().getNegativeAngle())
                .doubles("voltage_positive_angle", fortescueBusResultsContext
                        -> fortescueBusResultsContext.getVoltage().getPositiveAngle())
                .doubles("voltage_negative_magnitude", fortescueBusResultsContext
                        -> fortescueBusResultsContext.getVoltage().getNegativeMagnitude())
                .doubles("voltage_positive_magnitude", fortescueBusResultsContext
                        -> fortescueBusResultsContext.getVoltage().getPositiveMagnitude())
                .doubles("voltage_zero_magnitude", fortescueBusResultsContext
                        -> fortescueBusResultsContext.getVoltage().getZeroMagnitude())
                .doubles("voltage_zero_angle", fortescueBusResultsContext
                        -> fortescueBusResultsContext.getVoltage().getZeroAngle())
                .build();
    }
}
