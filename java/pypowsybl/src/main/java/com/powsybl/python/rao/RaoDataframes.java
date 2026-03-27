package com.powsybl.python.rao;

import com.powsybl.action.*;
import com.powsybl.contingency.Contingency;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.*;
import com.powsybl.openrao.data.crac.api.threshold.Threshold;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class RaoDataframes {

    private RaoDataframes() {
    }

    private static final String INDEX = "index";
    private static final String CNEC_ID = "cnec_id";
    private static final String OPTIMIZED_INSTANT = "optimized_instant";
    private static final String CONTINGENCY = "contingency";
    private static final String MARGIN = "margin";
    private static final String INITIAL_INSTANT = "initial";
    private static final String SIDE = "side";
    private static final String REMEDIAL_ACTION_ID = "remedial_action_id";

    private static final DataframeMapper<Crac, RaoResult> FLOW_CNEC_RESULT_MAPPER = createFlowCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> ANGLE_CNEC_RESULT_MAPPER = createAngleCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> VOLTAGE_CNEC_RESULT_MAPPER = createVoltageCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> REMEDIAL_ACTION_RESULT_MAPPER = createRemedialActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> NETWORK_ACTION_RESULT_MAPPER = createNetworkActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> PST_RANGE_ACTION_RESULT_MAPPER = createPstRangeActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> RANGE_ACTION_RESULT_MAPPER = createRangeActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> COST_RESULT_MAPPER = createCostResultMapper();

    public static DataframeMapper<Crac, RaoResult> flowCnecMapper() {
        return FLOW_CNEC_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> angleCnecMapper() {
        return ANGLE_CNEC_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> voltageCnecMapper() {
        return VOLTAGE_CNEC_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> remedialActionResultMapper() {
        return REMEDIAL_ACTION_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> networkActionResultMapper() {
        return NETWORK_ACTION_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> pstRangeActionResultMapper() {
        return PST_RANGE_ACTION_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> rangeActionResultMapper() {
        return RANGE_ACTION_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> costResultMapper() {
        return COST_RESULT_MAPPER;
    }

    public record FlowCnecResult(String cnecId, Instant instant, String contingency, TwoSides side, double flow, double margin, double relativeMargin, double commercialFlow, double loopFlow, double ptdfZonalSum) { }

    public record AngleCnecResult(String cnecId, Instant instant, String contingency, double angle, double margin) { }

    public record VoltageCnecResult(String cnecId, Instant instant, String contingency, TwoSides side, double minVoltage, double maxVoltage, double margin) { }

    public record ActivatedRemedialActionResult(String remedialActionId, Instant instant, String contingency) { }

    public record ActivatedPstRangeActionResult(String remedialActionId, Instant instant, String contingency, int optimizedTap) { }

    public record ActivatedRangeActionResult(String remedialActionId, Instant instant, String contingency, double optimizedSetPoint) { }

    public record CostResult(Instant instant, double functionalCost, double virtualCost, double cost) { }

    public record VirtualCostResult(Instant instant, Map<String, Double> virtualCosts) { }

    private static List<FlowCnecResult> getFlowCnecResult(Crac crac, RaoResult raoResult) {
        List<FlowCnecResult> results = new ArrayList<>();
        for (var cnec : crac.getFlowCnecs()) {
            // For each cnec output initial instant and ond cnec instant for both side
            readFlowCnecResult(raoResult, cnec, null, TwoSides.ONE).ifPresent(results::add);
            readFlowCnecResult(raoResult, cnec, null, TwoSides.TWO).ifPresent(results::add);
            readFlowCnecResult(raoResult, cnec, cnec.getState().getInstant(), TwoSides.ONE).ifPresent(results::add);
            readFlowCnecResult(raoResult, cnec, cnec.getState().getInstant(), TwoSides.TWO).ifPresent(results::add);
        }
        return results;
    }

    static Optional<FlowCnecResult> readFlowCnecResult(RaoResult result, FlowCnec cnec, Instant instant, TwoSides side) {
        Optional<Contingency> contingencyOpt = cnec.getState().getContingency();
        try {
            return Optional.of(new FlowCnecResult(
                cnec.getId(),
                instant,
                contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "",
                side,
                result.getFlow(instant, cnec, side, Unit.MEGAWATT),
                result.getMargin(instant, cnec, Unit.MEGAWATT),
                result.getRelativeMargin(instant, cnec, Unit.MEGAWATT),
                result.getCommercialFlow(instant, cnec, side, Unit.MEGAWATT),
                result.getLoopFlow(instant, cnec, side, Unit.MEGAWATT),
                result.getPtdfZonalSum(instant, cnec, side)
            ));
        } catch (OpenRaoException exception) {
            return Optional.empty();
        }
    }

    private static List<AngleCnecResult> getAngleCnecResult(Crac crac, RaoResult raoResult) {
        List<AngleCnecResult> results = new ArrayList<>();
        for (var cnec : crac.getAngleCnecs()) {
            readAngleCnecResult(raoResult, cnec, null).ifPresent(results::add);
            readAngleCnecResult(raoResult, cnec, cnec.getState().getInstant()).ifPresent(results::add);
        }
        return results;
    }

    static Optional<AngleCnecResult> readAngleCnecResult(RaoResult result, AngleCnec cnec, Instant instant) {
        Optional<Contingency> contingencyOpt = cnec.getState().getContingency();
        try {
            return Optional.of(new AngleCnecResult(
                cnec.getId(),
                instant,
                contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "",
                result.getAngle(instant, cnec, Unit.DEGREE),
                result.getMargin(instant, cnec, Unit.DEGREE)
            ));
        } catch (OpenRaoException exception) {
            return Optional.empty();
        }
    }

    private static List<VoltageCnecResult> getVoltageCnecResult(Crac crac, RaoResult raoResult) {
        List<VoltageCnecResult> results = new ArrayList<>();
        for (var cnec : crac.getVoltageCnecs()) {
            readVoltageCnecResult(raoResult, cnec, null).ifPresent(results::add);
            readVoltageCnecResult(raoResult, cnec, cnec.getState().getInstant()).ifPresent(results::add);
        }
        return results;
    }

    static Optional<VoltageCnecResult> readVoltageCnecResult(RaoResult result, VoltageCnec cnec, Instant instant) {
        Optional<Contingency> contingencyOpt = cnec.getState().getContingency();
        try {
            return Optional.of(new VoltageCnecResult(
                cnec.getId(),
                instant,
                contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "",
                TwoSides.ONE,
                result.getMinVoltage(instant, cnec, Unit.KILOVOLT),
                result.getMaxVoltage(instant, cnec, Unit.KILOVOLT),
                result.getMargin(instant, cnec, Unit.KILOVOLT)
            ));
        } catch (OpenRaoException exception) {
            return Optional.empty();
        }
    }

    private static List<ActivatedRemedialActionResult> getActivatedRemedialActions(Crac crac, RaoResult raoResult) {
        List<ActivatedRemedialActionResult> results = new ArrayList<>();
        for (RemedialAction<?> ra : crac.getRemedialActions()) {
            for (State state : crac.getStates()) {
                Optional<Contingency> contingencyOpt = state.getContingency();
                // Only go through activated remedial actions
                if (raoResult.isActivatedDuringState(state, ra)) {
                    ActivatedRemedialActionResult result = new ActivatedRemedialActionResult(ra.getId(), state.getInstant(), contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "");
                    results.add(result);
                }
            }
        }
        return results;
    }

    private static List<ActivatedRemedialActionResult> getActivatedNetworkActions(Crac crac, RaoResult raoResult) {
        List<ActivatedRemedialActionResult> results = new ArrayList<>();
        for (NetworkAction networkAction : crac.getNetworkActions()) {
            for (State state : crac.getStates()) {
                Optional<Contingency> contingencyOpt = state.getContingency();
                // Only go through activated remedial actions
                if (raoResult.isActivatedDuringState(state, networkAction)) {
                    ActivatedRemedialActionResult result = new ActivatedRemedialActionResult(networkAction.getId(), state.getInstant(), contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "");
                    results.add(result);
                }
            }
        }
        return results;
    }

    private static List<ActivatedPstRangeActionResult> getActivatedPstRangeActions(Crac crac, RaoResult raoResult) {
        List<ActivatedPstRangeActionResult> results = new ArrayList<>();
        for (PstRangeAction pstRangeAction : crac.getPstRangeActions()) {
            for (State state : crac.getStates()) {
                Optional<Contingency> contingencyOpt = state.getContingency();
                // Only go through activated remedial actions
                if (raoResult.isActivatedDuringState(state, pstRangeAction)) {
                    int optimizedTap = raoResult.getOptimizedTapOnState(state, pstRangeAction);
                    ActivatedPstRangeActionResult result = new ActivatedPstRangeActionResult(pstRangeAction.getId(), state.getInstant(), contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "", optimizedTap);
                    results.add(result);
                }
            }
        }
        return results;
    }

    private static List<ActivatedRangeActionResult> getActivatedRangeActions(Crac crac, RaoResult raoResult) {
        List<ActivatedRangeActionResult> results = new ArrayList<>();
        for (RangeAction<?> rangeAction : crac.getRangeActions()) {
            // PST range actions are dealt with separately since they rely on taps instead of set-points
            if (!(rangeAction instanceof PstRangeAction)) {
                for (State state : crac.getStates()) {
                    Optional<Contingency> contingencyOpt = state.getContingency();
                    // Only go through activated remedial actions
                    if (raoResult.isActivatedDuringState(state, rangeAction)) {
                        double optimizedSetPoint = raoResult.getOptimizedSetPointOnState(state, rangeAction);
                        ActivatedRangeActionResult result = new ActivatedRangeActionResult(rangeAction.getId(), state.getInstant(), contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "", optimizedSetPoint);
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }

    private static List<CostResult> getCostResults(Crac crac, RaoResult raoResult) {
        List<CostResult> results = new ArrayList<>();
        results.add(new CostResult(null,
            raoResult.getFunctionalCost(null),
            raoResult.getVirtualCost(null),
            raoResult.getCost(null)));
        for (var instant : crac.getSortedInstants()) {
            results.add(new CostResult(instant,
                raoResult.getFunctionalCost(instant),
                raoResult.getVirtualCost(instant),
                raoResult.getCost(instant)));
        }
        return results;
    }

    private static List<VirtualCostResult> getVirtualCost(Crac crac, RaoResult raoResult) {
        List<VirtualCostResult> results = new ArrayList<>();
        results.add(new VirtualCostResult(null, getVirtualCostMap(null, raoResult)));
        for (var instant : crac.getSortedInstants()) {
            results.add(new VirtualCostResult(instant, getVirtualCostMap(instant, raoResult)));
        }
        return results;
    }

    static Map<String, Double> getVirtualCostMap(Instant instant, RaoResult results) {
        Map<String, Double> costs = new HashMap<>();
        for (String virtualCost : results.getVirtualCostNames()) {
            costs.put(virtualCost, results.getVirtualCost(instant, virtualCost));
        }
        return costs;
    }

    private static DataframeMapper<Crac, RaoResult> createFlowCnecResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, FlowCnecResult, RaoResult>()
            .itemsProvider(RaoDataframes::getFlowCnecResult)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(CNEC_ID, FlowCnecResult::cnecId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, FlowCnecResult::contingency)
            .strings(SIDE, r -> r.side().toString())
            .doubles("flow", FlowCnecResult::flow)
            .doubles(MARGIN, FlowCnecResult::margin)
            .doubles("relative_margin", FlowCnecResult::relativeMargin)
            .doubles("commercial_flow", FlowCnecResult::commercialFlow)
            .doubles("loop_flow", FlowCnecResult::loopFlow)
            .doubles("ptdf_zonal_sum", FlowCnecResult::ptdfZonalSum)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createAngleCnecResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, AngleCnecResult, RaoResult>()
            .itemsProvider(RaoDataframes::getAngleCnecResult)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(CNEC_ID, AngleCnecResult::cnecId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, AngleCnecResult::contingency)
            .doubles("angle", AngleCnecResult::angle)
            .doubles(MARGIN, AngleCnecResult::margin)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createVoltageCnecResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, VoltageCnecResult, RaoResult>()
            .itemsProvider(RaoDataframes::getVoltageCnecResult)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(CNEC_ID, VoltageCnecResult::cnecId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, VoltageCnecResult::contingency)
            .strings(SIDE, r -> r.side().toString())
            .doubles("min_voltage", VoltageCnecResult::minVoltage)
            .doubles("max_voltage", VoltageCnecResult::maxVoltage)
            .doubles(MARGIN, VoltageCnecResult::margin)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createRemedialActionResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, ActivatedRemedialActionResult, RaoResult>()
            .itemsProvider(RaoDataframes::getActivatedRemedialActions)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(REMEDIAL_ACTION_ID, ActivatedRemedialActionResult::remedialActionId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, ActivatedRemedialActionResult::contingency)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createNetworkActionResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, ActivatedRemedialActionResult, RaoResult>()
            .itemsProvider(RaoDataframes::getActivatedNetworkActions)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(REMEDIAL_ACTION_ID, ActivatedRemedialActionResult::remedialActionId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, ActivatedRemedialActionResult::contingency)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createPstRangeActionResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, ActivatedPstRangeActionResult, RaoResult>()
            .itemsProvider(RaoDataframes::getActivatedPstRangeActions)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(REMEDIAL_ACTION_ID, ActivatedPstRangeActionResult::remedialActionId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, ActivatedPstRangeActionResult::contingency)
            .ints("optimized_tap", ActivatedPstRangeActionResult::optimizedTap)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createRangeActionResultMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<Crac, ActivatedRangeActionResult, RaoResult>()
            .itemsProvider(RaoDataframes::getActivatedRangeActions)
            .intsIndex(INDEX, e -> index.getAndIncrement())
            .strings(REMEDIAL_ACTION_ID, ActivatedRangeActionResult::remedialActionId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, ActivatedRangeActionResult::contingency)
            .doubles("optimized_set_point", ActivatedRangeActionResult::optimizedSetPoint)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createCostResultMapper() {
        return new DataframeMapperBuilder<Crac, CostResult, RaoResult>()
            .itemsProvider(RaoDataframes::getCostResults)
            .stringsIndex(OPTIMIZED_INSTANT, c -> c.instant() != null ? c.instant().getId() : INITIAL_INSTANT)
            .doubles("functional_cost", CostResult::functionalCost)
            .doubles("virtual_cost", CostResult::virtualCost)
            .doubles("cost", CostResult::cost)
            .build();
    }

    public static DataframeMapper<Crac, RaoResult> createVirtualCostResultMapper(String virtualCostName) {
        return new DataframeMapperBuilder<Crac, VirtualCostResult, RaoResult>()
            .itemsProvider(RaoDataframes::getVirtualCost)
            .stringsIndex(OPTIMIZED_INSTANT, c -> c.instant() != null ? c.instant().getId() : INITIAL_INSTANT)
            .doubles(virtualCostName, c -> c.virtualCosts().get(virtualCostName))
            .build();
    }

    public static DataframeMapper<Crac, Void> cracInstantsMapper() {
        return new DataframeMapperBuilder<Crac, Instant, Void>()
            .itemsProvider(Crac::getSortedInstants)
            .stringsIndex("id", Identifiable::getId)
            .strings("kind", instant -> String.valueOf(instant.getKind()))
            .ints("order", Instant::getOrder)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracRemedialActionsUsageLimits() {
        return new DataframeMapperBuilder<Crac, Map.Entry<Instant, RaUsageLimits>, Void>()
            .itemsProvider(crac -> crac.getRaUsageLimitsPerInstant().entrySet().stream().toList())
            .stringsIndex("instant", e -> e.getKey().getId())
            .ints("value", e -> e.getValue().getMaxRa())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracPerTsoUsageLimits(Function<RaUsageLimits, Map<String, Integer>> func) {
        return new DataframeMapperBuilder<Crac, Triple<Instant, String, Integer>, Void>()
            .itemsProvider(crac ->
                crac.getRaUsageLimitsPerInstant().entrySet().stream()
                    .flatMap(e ->
                        func.apply(e.getValue()).entrySet().stream()
                            .map(v -> Triple.of(e.getKey(), v.getKey(), v.getValue()))
                    )
                    .toList())
            .stringsIndex("instant", e -> e.getLeft().getId())
            .stringsIndex("tso", Triple::getMiddle)
            .ints("value", Triple::getRight)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracContingencies() {
        return new DataframeMapperBuilder<Crac, Contingency, Void>()
            .itemsProvider(crac -> crac.getContingencies().stream().toList())
            .stringsIndex("id", Contingency::getId)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracContingencyElements() {
        return new DataframeMapperBuilder<Crac, Pair<String, String>, Void>()
            .itemsProvider(crac ->
                crac.getContingencies().stream()
                    .flatMap(c ->
                        c.getElements().stream()
                            .map(elem -> Pair.create(c.getId(), elem.getId()))
                    )
                    .toList())
            .stringsIndex("id", Pair::getFirst)
            .stringsIndex("network_element_id", Pair::getSecond)
            .build();
    }

    /**
     * TODO check NetworkElement vs NetworkElements ?
     */
    public static DataframeMapper<Crac, Void> cracFlowCnecs() {
        return new DataframeMapperBuilder<Crac, FlowCnec, Void>()
            .itemsProvider(crac ->
                crac.getFlowCnecs().stream().toList())
            .stringsIndex("id", FlowCnec::getId)
            .strings("name", FlowCnec::getName)
            .strings("network_element_id", cnec -> cnec.getNetworkElement().getId())
            .strings("operator", FlowCnec::getOperator)
            .strings("border", FlowCnec::getBorder)
            .strings("instant", cnec -> Optional.of(cnec.getState().getInstant()).map(Instant::getId).orElse(""))
            .strings("contingency_id", cnec -> cnec.getState().getContingency().map(Contingency::getId).orElse(""))
            .booleans("optimized", FlowCnec::isOptimized)
            .booleans("monitored", FlowCnec::isMonitored)
            .doubles("reliability_margin", FlowCnec::getReliabilityMargin)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracAngleCnecs() {
        return new DataframeMapperBuilder<Crac, AngleCnec, Void>()
            .itemsProvider(crac ->
                crac.getAngleCnecs().stream().toList())
            .stringsIndex("id", AngleCnec::getId)
            .strings("name", AngleCnec::getName)
            .strings("exporting_network_element_id", cnec -> cnec.getExportingNetworkElement().getId())
            .strings("exporting_network_element_id", cnec -> cnec.getImportingNetworkElement().getId())
            .strings("operator", AngleCnec::getOperator)
            .strings("border", AngleCnec::getBorder)
            .strings("instant", cnec -> Optional.of(cnec.getState().getInstant()).map(Instant::getId).orElse(""))
            .strings("contingency_id", cnec -> cnec.getState().getContingency().map(Contingency::getId).orElse(""))
            .booleans("optimized", AngleCnec::isOptimized)
            .booleans("monitored", AngleCnec::isMonitored)
            .doubles("reliability_margin", AngleCnec::getReliabilityMargin)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracVoltageCnecs() {
        return new DataframeMapperBuilder<Crac, VoltageCnec, Void>()
            .itemsProvider(crac ->
                crac.getVoltageCnecs().stream().toList())
            .stringsIndex("id", VoltageCnec::getId)
            .strings("name", VoltageCnec::getName)
            .strings("network_element_id", cnec -> cnec.getNetworkElement().getId())
            .strings("operator", VoltageCnec::getOperator)
            .strings("border", VoltageCnec::getBorder)
            .strings("instant", cnec -> Optional.of(cnec.getState().getInstant()).map(Instant::getId).orElse(""))
            .strings("contingency_id", cnec -> cnec.getState().getContingency().map(Contingency::getId).orElse(""))
            .booleans("optimized", VoltageCnec::isOptimized)
            .booleans("monitored", VoltageCnec::isMonitored)
            .doubles("reliability_margin", VoltageCnec::getReliabilityMargin)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracThresholds() {
        return new DataframeMapperBuilder<Crac, Triple<String, Threshold, String>, Void>()
            .itemsProvider(RaoDataframes::gatherThresholds)
            .stringsIndex("id", Triple::getLeft)
            .strings("min", t -> t.getMiddle().min().isPresent() ? t.getMiddle().min().get().toString() : "")
            .strings("max", t -> t.getMiddle().max().isPresent() ? t.getMiddle().max().get().toString() : "")
            .strings("unit", t -> t.getMiddle().getUnit().name())
            .strings("side", Triple::getRight)
            .build();
    }

    public static List<Triple<String, Threshold, String>> gatherThresholds(Crac crac) {
        List<Triple<String, Threshold, String>> thresholds = new ArrayList<>();
        crac.getFlowCnecs().forEach(
            cnec -> cnec.getThresholds().forEach(
                threshold -> thresholds.add(Triple.of(cnec.getId(), threshold, threshold.getSide().name()))));
        crac.getAngleCnecs().forEach(
            cnec -> cnec.getThresholds().forEach(
                threshold -> thresholds.add(Triple.of(cnec.getId(), threshold, ""))));
        crac.getVoltageCnecs().forEach(
            cnec -> cnec.getThresholds().forEach(
                threshold -> thresholds.add(Triple.of(cnec.getId(), threshold, ""))));
        return thresholds;
    }

    public static <T extends RangeAction<?>> DataframeMapper<Crac, Void> cracRangeActions(Function<Crac, List<T>> actionsSupplier) {
        return new DataframeMapperBuilder<Crac, T, Void>()
            .itemsProvider(actionsSupplier)
            .stringsIndex("id", T::getId)
            .strings("name", T::getName)
            .strings("operator", T::getOperator)
            .strings("network_element_id", a -> a.getNetworkElements().stream().toList().getFirst().getId())
            .strings("group_id", a -> a.getGroupId().orElse(""))
            .optionalInts("speed", a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .strings("activation_cost", a -> a.getActivationCost().map(Object::toString).orElse(""))
            .strings("variation_cost_up", a -> a.getVariationCost(VariationDirection.UP).map(Object::toString).orElse(""))
            .strings("variation_cost_down", a -> a.getVariationCost(VariationDirection.DOWN).map(Object::toString).orElse(""))
            .build();
    }

    public static DataframeMapper<Crac, Void> cracInjectionRangeActions() {
        return new DataframeMapperBuilder<Crac, InjectionRangeAction, Void>()
            .itemsProvider(crac -> crac.getInjectionRangeActions().stream().toList())
            .stringsIndex("id", InjectionRangeAction::getId)
            .strings("name", InjectionRangeAction::getName)
            .strings("operator", InjectionRangeAction::getOperator)
            .strings("group_id", a -> a.getGroupId().orElse(""))
            .optionalInts("speed", a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .strings("activation_cost", a -> a.getActivationCost().map(Object::toString).orElse(""))
            .strings("variation_cost_up", a -> a.getVariationCost(VariationDirection.UP).map(Object::toString).orElse(""))
            .strings("variation_cost_down", a -> a.getVariationCost(VariationDirection.DOWN).map(Object::toString).orElse(""))
            .build();
    }

    public static DataframeMapper<Crac, Void> cracInjectionRaElements() {
        return new DataframeMapperBuilder<Crac, Triple<InjectionRangeAction, NetworkElement, Double>, Void>()
            .itemsProvider(crac ->
                crac.getInjectionRangeActions().stream()
                    .flatMap(e ->
                        e.getInjectionDistributionKeys().entrySet().stream()
                            .map(v -> Triple.of(e, v.getKey(), v.getValue()))
                    )
                    .toList())
            .stringsIndex("id", e -> e.getLeft().getId())
            .strings("network_element_id", e -> e.getMiddle().getId())
            .doubles("distribution_key", Triple::getRight)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracCounterTradeRangeActions() {
        return new DataframeMapperBuilder<Crac, CounterTradeRangeAction, Void>()
            .itemsProvider(crac -> crac.getCounterTradeRangeActions().stream().toList())
            .stringsIndex("id", CounterTradeRangeAction::getId)
            .strings("name", CounterTradeRangeAction::getName)
            .strings("operator", CounterTradeRangeAction::getOperator)
            .strings("exporting_country", a -> a.getExportingCountry().name())
            .strings("importing_country", a -> a.getImportingCountry().name())
            .strings("group_id", a -> a.getGroupId().orElse(""))
            .optionalInts("speed", a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .strings("activation_cost", a -> a.getActivationCost().map(Object::toString).orElse(""))
            .strings("variation_cost_up", a -> a.getVariationCost(VariationDirection.UP).map(Object::toString).orElse(""))
            .strings("variation_cost_down", a -> a.getVariationCost(VariationDirection.DOWN).map(Object::toString).orElse(""))
            .build();
    }

    private record Range(String raId, double min, double max, RangeType rangeType) { }

    private static List<Range> flattenRanges(Crac crac) {
        List<Range> ranges = new ArrayList<>();
        crac.getPstRangeActions().forEach(
            a -> a.getRanges().forEach(
                r -> ranges.add(new Range(a.getId(), r.getMinTap(), r.getMaxTap(), r.getRangeType()))));
        crac.getHvdcRangeActions().forEach(
            a -> a.getRanges().forEach(
                r -> ranges.add(new Range(a.getId(), r.getMin(), r.getMax(), r.getRangeType()))));
        crac.getInjectionRangeActions().forEach(
            a -> a.getRanges().forEach(
                r -> ranges.add(new Range(a.getId(), r.getMin(), r.getMax(), r.getRangeType()))));
        crac.getCounterTradeRangeActions().forEach(
            a -> a.getRanges().forEach(
                r -> ranges.add(new Range(a.getId(), r.getMin(), r.getMax(), r.getRangeType()))));
        return ranges;
    }

    public static DataframeMapper<Crac, Void> cracRanges() {
        return new DataframeMapperBuilder<Crac, Range, Void>()
            .itemsProvider(RaoDataframes::flattenRanges)
            .stringsIndex("id", Range::raId)
            .doubles("min", Range::min)
            .doubles("max", Range::max)
            .strings("range_type", r -> r.rangeType().name())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracNetworkActions() {
        return new DataframeMapperBuilder<Crac, NetworkAction, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().toList())
            .stringsIndex("id", NetworkAction::getId)
            .strings("name", NetworkAction::getName)
            .strings("operator", NetworkAction::getOperator)
            .optionalInts("speed", a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .strings("activation_cost", a -> a.getActivationCost().map(Object::toString).orElse(""))
            .build();
    }

    public static DataframeMapper<Crac, Void> cracTerminalConnectionActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, TerminalsConnectionAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                    .filter(TerminalsConnectionAction.class::isInstance)
                    .map(elementary -> Pair.create(a, (TerminalsConnectionAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getElementId())
            .strings("action_type", pair -> pair.getSecond().isOpen() ? "open" : "close")
            .build();
    }

    public static DataframeMapper<Crac, Void> cracPstTapPositionActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, PhaseTapChangerTapPositionAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(PhaseTapChangerTapPositionAction.class::isInstance)
                .map(elementary -> Pair.create(a, (PhaseTapChangerTapPositionAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getTransformerId())
            .ints("tap_position", pair -> pair.getSecond().getTapPosition())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracGeneratorActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, GeneratorAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(GeneratorAction.class::isInstance)
                .map(elementary -> Pair.create(a, (GeneratorAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getGeneratorId())
            .strings("active_power_value", pair -> pair.getSecond().getActivePowerValue().isPresent() ? String.valueOf(pair.getSecond().getActivePowerValue().getAsDouble()) : "")
            .build();
    }

    public static DataframeMapper<Crac, Void> cracLoadActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, LoadAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(LoadAction.class::isInstance)
                .map(elementary -> Pair.create(a, (LoadAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getLoadId())
            .strings("active_power_value", pair -> pair.getSecond().getActivePowerValue().isPresent() ? String.valueOf(pair.getSecond().getActivePowerValue().getAsDouble()) : "")
            .build();
    }

    public static DataframeMapper<Crac, Void> cracBoundaryLineActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, DanglingLineAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(DanglingLineAction.class::isInstance)
                .map(elementary -> Pair.create(a, (DanglingLineAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getDanglingLineId())
            .strings("active_power_value", pair -> pair.getSecond().getActivePowerValue().isPresent() ? String.valueOf(pair.getSecond().getActivePowerValue().getAsDouble()) : "")
            .build();
    }

    public static DataframeMapper<Crac, Void> cracShuntCompensatorPositionActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, ShuntCompensatorPositionAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(ShuntCompensatorPositionAction.class::isInstance)
                .map(elementary -> Pair.create(a, (ShuntCompensatorPositionAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getShuntCompensatorId())
            .ints("section_count", pair -> pair.getSecond().getSectionCount())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracSwitchActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, SwitchAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(SwitchAction.class::isInstance)
                .map(elementary -> Pair.create(a, (SwitchAction) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("network_element_id", pair -> pair.getSecond().getSwitchId())
            .strings("active_power_value", pair -> pair.getSecond().isOpen() ? "open" : "close")
            .build();
    }

    public static DataframeMapper<Crac, Void> cracSwitchPairsAction() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, SwitchPair>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(SwitchPair.class::isInstance)
                .map(elementary -> Pair.create(a, (SwitchPair) elementary))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("open", pair -> pair.getSecond().getSwitchToOpen().getId())
            .strings("close", pair -> pair.getSecond().getSwitchToClose().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnInstantUsageRule() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, UsageRule>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream().map(rule -> Pair.create(a, rule))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("instant", pair -> pair.getSecond().getInstant().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnContingencyStateUsageRule() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, OnContingencyState>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream()
                .filter(OnContingencyState.class::isInstance).map(rule -> Pair.create(a, (OnContingencyState) rule))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("instant", pair -> pair.getSecond().getInstant().getId())
            .strings("contingency_id", pair -> pair.getSecond().getContingency().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnConstraintUsageRule() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, OnConstraint<Cnec<?>>>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream()
                .filter(OnConstraint.class::isInstance).map(rule -> Pair.create(a, (OnConstraint<Cnec<?>>) rule))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("instant", pair -> pair.getSecond().getInstant().getId())
            .strings("cnec_id", pair -> pair.getSecond().getCnec().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnFlowConstraintInCountryUsageRule() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, OnFlowConstraintInCountry>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance).map(rule -> Pair.create(a, (OnFlowConstraintInCountry) rule))).toList())
            .stringsIndex("id", pair -> pair.getFirst().getId())
            .strings("instant", pair -> pair.getSecond().getInstant().getId())
            .strings("contingency_id", pair -> pair.getSecond().getContingency().map(Contingency::getId).orElse(""))
            .strings("country", pair -> pair.getSecond().getCountry().name())
            .build();
    }
}
