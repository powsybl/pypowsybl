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
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.powsybl.python.rao.RaoConstants.*;

public final class RaoDataframes {

    private RaoDataframes() {
    }

    private static final DataframeMapper<Crac, RaoResult> FLOW_CNEC_RESULT_MAPPER = createFlowCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> ANGLE_CNEC_RESULT_MAPPER = createAngleCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> VOLTAGE_CNEC_RESULT_MAPPER = createVoltageCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> REMEDIAL_ACTION_RESULT_MAPPER = createRemedialActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> NETWORK_ACTION_RESULT_MAPPER = createNetworkActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> PST_RANGE_ACTION_RESULT_MAPPER = createPstRangeActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> RANGE_ACTION_RESULT_MAPPER = createRangeActionResultMapper();
    private static final DataframeMapper<Crac, RaoResult> COST_RESULT_MAPPER = createCostResultMapper();
    private static final DataframeMapper<Crac, TimeCoupledRaoResult> GLOBAL_COST_RESULT_MAPPER = createGlobalCostResultMapper();

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

    public static DataframeMapper<Crac, TimeCoupledRaoResult> globalCostResultMapper() {
        return GLOBAL_COST_RESULT_MAPPER;
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

    private static List<CostResult> getGlobalCostResults(Crac crac, TimeCoupledRaoResult raoResult) {
        List<CostResult> results = new ArrayList<>();
        results.add(new CostResult(null,
                raoResult.getGlobalFunctionalCost(null),
                raoResult.getGlobalVirtualCost(null),
                raoResult.getGlobalCost(null)));
        for (var instant : crac.getSortedInstants()) {
            results.add(new CostResult(instant,
                    raoResult.getGlobalFunctionalCost(instant),
                    raoResult.getGlobalVirtualCost(instant),
                    raoResult.getGlobalCost(instant)));
        }
        return results;
    }

    private static List<CostResult> getCostResults(Crac crac, TimeCoupledRaoResult raoResult, OffsetDateTime timestamp) {
        List<CostResult> results = new ArrayList<>();
        results.add(new CostResult(null,
                raoResult.getFunctionalCost(null, timestamp),
                raoResult.getVirtualCost(null, timestamp),
                raoResult.getCost(null, timestamp)));
        for (var instant : crac.getSortedInstants()) {
            results.add(new CostResult(instant,
                    raoResult.getFunctionalCost(instant, timestamp),
                    raoResult.getVirtualCost(instant, timestamp),
                    raoResult.getCost(instant, timestamp)));
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
            .doubles(FLOW, FlowCnecResult::flow)
            .doubles(MARGIN, FlowCnecResult::margin)
            .doubles(RELATIVE_MARGIN, FlowCnecResult::relativeMargin)
            .doubles(COMMERCIAL_FLOW, FlowCnecResult::commercialFlow)
            .doubles(LOOP_FLOW, FlowCnecResult::loopFlow)
            .doubles(PTDF_ZONAL_SUM, FlowCnecResult::ptdfZonalSum)
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
            .doubles(ANGLE, AngleCnecResult::angle)
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
            .doubles(MIN_VOLTAGE, VoltageCnecResult::minVoltage)
            .doubles(MAX_VOLTAGE, VoltageCnecResult::maxVoltage)
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
            .ints(OPTIMIZED_TAP, ActivatedPstRangeActionResult::optimizedTap)
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
            .doubles(OPTIMIZED_SET_POINT, ActivatedRangeActionResult::optimizedSetPoint)
            .build();
    }

    private static DataframeMapper<Crac, RaoResult> createCostResultMapper() {
        return new DataframeMapperBuilder<Crac, CostResult, RaoResult>()
            .itemsProvider(RaoDataframes::getCostResults)
            .stringsIndex(OPTIMIZED_INSTANT, c -> c.instant() != null ? c.instant().getId() : INITIAL_INSTANT)
            .doubles(FUNCTIONAL_COST, CostResult::functionalCost)
            .doubles(VIRTUAL_COST, CostResult::virtualCost)
            .doubles(COST, CostResult::cost)
            .build();
    }

    private static DataframeMapper<Crac, TimeCoupledRaoResult> createGlobalCostResultMapper() {
        return new DataframeMapperBuilder<Crac, CostResult, TimeCoupledRaoResult>()
                .itemsProvider(RaoDataframes::getGlobalCostResults)
                .stringsIndex(OPTIMIZED_INSTANT, c -> c.instant() != null ? c.instant().getId() : INITIAL_INSTANT)
                .doubles(GLOBAL_FUNCTIONAL_COST, CostResult::functionalCost)
                .doubles(GLOBAL_VIRTUAL_COST, CostResult::virtualCost)
                .doubles(GLOBAL_COST, CostResult::cost)
                .build();
    }

    public static DataframeMapper<Crac, TimeCoupledRaoResult> createCostResultMapper(OffsetDateTime timestamp) {
        return new DataframeMapperBuilder<Crac, CostResult, TimeCoupledRaoResult>()
                .itemsProvider((crac, raoResult) -> RaoDataframes.getCostResults(crac, raoResult, timestamp))
                .stringsIndex(OPTIMIZED_INSTANT, c -> c.instant() != null ? c.instant().getId() : INITIAL_INSTANT)
                .doubles(FUNCTIONAL_COST, CostResult::functionalCost)
                .doubles(VIRTUAL_COST, CostResult::virtualCost)
                .doubles(COST, CostResult::cost)
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
            .stringsIndex(ID, Identifiable::getId)
            .strings(KIND, instant -> String.valueOf(instant.getKind()))
            .ints(ORDER, Instant::getOrder)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracRemedialActionsUsageLimits() {
        return new DataframeMapperBuilder<Crac, Map.Entry<Instant, RaUsageLimits>, Void>()
            .itemsProvider(crac -> crac.getRaUsageLimitsPerInstant().entrySet().stream().toList())
            .stringsIndex(INSTANT, e -> e.getKey().getId())
            .ints(VALUE, e -> e.getValue().getMaxRa())
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
            .stringsIndex(INSTANT, e -> e.getLeft().getId())
            .stringsIndex(TSO, Triple::getMiddle)
            .ints(VALUE, Triple::getRight)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracContingencies() {
        return new DataframeMapperBuilder<Crac, Contingency, Void>()
            .itemsProvider(crac -> crac.getContingencies().stream().toList())
            .stringsIndex(ID, Contingency::getId)
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
            .stringsIndex(ID, Pair::getFirst)
            .stringsIndex(NETWORK_ELEMENT_ID, Pair::getSecond)
            .build();
    }

    /**
     * TODO check NetworkElement vs NetworkElements ?
     */
    public static DataframeMapper<Crac, Void> cracFlowCnecs() {
        return new DataframeMapperBuilder<Crac, FlowCnec, Void>()
            .itemsProvider(crac ->
                crac.getFlowCnecs().stream().toList())
            .stringsIndex(ID, FlowCnec::getId)
            .strings(NAME, FlowCnec::getName)
            .strings(NETWORK_ELEMENT_ID, cnec -> cnec.getNetworkElement().getId())
            .strings(OPERATOR, FlowCnec::getOperator)
            .strings(BORDER, FlowCnec::getBorder)
            .strings(INSTANT, cnec -> Optional.of(cnec.getState().getInstant()).map(Instant::getId).orElse(""))
            .strings(CONTINGENCY_ID, cnec -> cnec.getState().getContingency().map(Contingency::getId).orElse(""))
            .booleans(OPTIMIZED, FlowCnec::isOptimized)
            .booleans(MONITORED, FlowCnec::isMonitored)
            .doubles(RELIABILITY_MARGIN, FlowCnec::getReliabilityMargin)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracAngleCnecs() {
        return new DataframeMapperBuilder<Crac, AngleCnec, Void>()
            .itemsProvider(crac ->
                crac.getAngleCnecs().stream().toList())
            .stringsIndex(ID, AngleCnec::getId)
            .strings(NAME, AngleCnec::getName)
            .strings(EXPORTING_NETWORK_ELEMENT_ID, cnec -> cnec.getExportingNetworkElement().getId())
            .strings(IMPORTING_NETWORK_ELEMENT_ID, cnec -> cnec.getImportingNetworkElement().getId())
            .strings(OPERATOR, AngleCnec::getOperator)
            .strings(BORDER, AngleCnec::getBorder)
            .strings(INSTANT, cnec -> Optional.of(cnec.getState().getInstant()).map(Instant::getId).orElse(""))
            .strings(CONTINGENCY_ID, cnec -> cnec.getState().getContingency().map(Contingency::getId).orElse(""))
            .booleans(OPTIMIZED, AngleCnec::isOptimized)
            .booleans(MONITORED, AngleCnec::isMonitored)
            .doubles(RELIABILITY_MARGIN, AngleCnec::getReliabilityMargin)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracVoltageCnecs() {
        return new DataframeMapperBuilder<Crac, VoltageCnec, Void>()
            .itemsProvider(crac ->
                crac.getVoltageCnecs().stream().toList())
            .stringsIndex(ID, VoltageCnec::getId)
            .strings(NAME, VoltageCnec::getName)
            .strings(NETWORK_ELEMENT_ID, cnec -> cnec.getNetworkElement().getId())
            .strings(OPERATOR, VoltageCnec::getOperator)
            .strings(BORDER, VoltageCnec::getBorder)
            .strings(INSTANT, cnec -> Optional.of(cnec.getState().getInstant()).map(Instant::getId).orElse(""))
            .strings(CONTINGENCY_ID, cnec -> cnec.getState().getContingency().map(Contingency::getId).orElse(""))
            .booleans(OPTIMIZED, VoltageCnec::isOptimized)
            .booleans(MONITORED, VoltageCnec::isMonitored)
            .doubles(RELIABILITY_MARGIN, VoltageCnec::getReliabilityMargin)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracThresholds() {
        return new DataframeMapperBuilder<Crac, Triple<String, Threshold, String>, Void>()
            .itemsProvider(RaoDataframes::gatherThresholds)
            .stringsIndex(ID, Triple::getLeft)
            .optionalDoubles(MIN, t -> t.getMiddle().min().map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .optionalDoubles(MAX, t -> t.getMiddle().max().map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .strings(UNIT, t -> t.getMiddle().getUnit().name())
            .strings(SIDE, Triple::getRight)
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

    public static DataframeMapper<Crac, Void> cracPstRangeActions(Function<Crac, List<PstRangeAction>> actionsSupplier) {
        return new DataframeMapperBuilder<Crac, PstRangeAction, Void>()
            .itemsProvider(actionsSupplier)
            .stringsIndex(ID, PstRangeAction::getId)
            .strings(NAME, PstRangeAction::getName)
            .strings(OPERATOR, PstRangeAction::getOperator)
            .strings(NETWORK_ELEMENT_ID, a -> a.getNetworkElements().stream().toList().getFirst().getId())
            .strings(GROUP_ID, a -> a.getGroupId().orElse(""))
            .optionalInts(SPEED, a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .strings(ACTIVATION_COST, a -> a.getActivationCost().map(Object::toString).orElse(""))
            .strings(VARIATION_COST_UP, a -> a.getVariationCost(VariationDirection.UP).map(Object::toString).orElse(""))
            .strings(VARIATION_COST_DOWN, a -> a.getVariationCost(VariationDirection.DOWN).map(Object::toString).orElse(""))
            .ints(INITIAL_TAP, PstRangeAction::getInitialTap)
            .build();
    }

    private static double getInitialSetpointOrNaN(StandardRangeAction<?> rangeAction) {
        // initial setpoint is undefined for range actions created without one (e.g. from the python API)
        Double initialSetpoint = rangeAction.getInitialSetpoint();
        return initialSetpoint != null ? initialSetpoint : Double.NaN;
    }

    public static DataframeMapper<Crac, Void> cracHvdcRangeActions(Function<Crac, List<HvdcRangeAction>> actionsSupplier) {
        return new DataframeMapperBuilder<Crac, HvdcRangeAction, Void>()
            .itemsProvider(actionsSupplier)
            .stringsIndex(ID, HvdcRangeAction::getId)
            .strings(NAME, HvdcRangeAction::getName)
            .strings(OPERATOR, HvdcRangeAction::getOperator)
            .strings(NETWORK_ELEMENT_ID, a -> a.getNetworkElements().stream().toList().getFirst().getId())
            .strings(GROUP_ID, a -> a.getGroupId().orElse(""))
            .optionalInts(SPEED, a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .strings(ACTIVATION_COST, a -> a.getActivationCost().map(Object::toString).orElse(""))
            .strings(VARIATION_COST_UP, a -> a.getVariationCost(VariationDirection.UP).map(Object::toString).orElse(""))
            .strings(VARIATION_COST_DOWN, a -> a.getVariationCost(VariationDirection.DOWN).map(Object::toString).orElse(""))
            .doubles(INITIAL_SET_POINT, RaoDataframes::getInitialSetpointOrNaN)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracInjectionRangeActions() {
        return new DataframeMapperBuilder<Crac, InjectionRangeAction, Void>()
            .itemsProvider(crac -> crac.getInjectionRangeActions().stream().toList())
            .stringsIndex(ID, InjectionRangeAction::getId)
            .strings(NAME, InjectionRangeAction::getName)
            .strings(OPERATOR, InjectionRangeAction::getOperator)
            .strings(GROUP_ID, a -> a.getGroupId().orElse(""))
            .optionalInts(SPEED, a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .optionalDoubles(ACTIVATION_COST, a -> a.getActivationCost().map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .optionalDoubles(VARIATION_COST_UP, a -> a.getVariationCost(VariationDirection.UP).map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .optionalDoubles(VARIATION_COST_DOWN, a -> a.getVariationCost(VariationDirection.DOWN).map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .doubles(INITIAL_SET_POINT, RaoDataframes::getInitialSetpointOrNaN)
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
            .stringsIndex(ID, e -> e.getLeft().getId())
            .strings(NETWORK_ELEMENT_ID, e -> e.getMiddle().getId())
            .doubles(DISTRIBUTION_KEY, Triple::getRight)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracCounterTradeRangeActions() {
        return new DataframeMapperBuilder<Crac, CounterTradeRangeAction, Void>()
            .itemsProvider(crac -> crac.getCounterTradeRangeActions().stream().toList())
            .stringsIndex(ID, CounterTradeRangeAction::getId)
            .strings(NAME, CounterTradeRangeAction::getName)
            .strings(OPERATOR, CounterTradeRangeAction::getOperator)
            .strings(EXPORTING_AREA, CounterTradeRangeAction::getExportingArea)
            .strings(IMPORTING_AREA, CounterTradeRangeAction::getImportingArea)
            .strings(GROUP_ID, a -> a.getGroupId().orElse(""))
            .optionalInts(SPEED, a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .optionalDoubles(ACTIVATION_COST, a -> a.getActivationCost().map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .optionalDoubles(VARIATION_COST_UP, a -> a.getVariationCost(VariationDirection.UP).map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .optionalDoubles(VARIATION_COST_DOWN, a -> a.getVariationCost(VariationDirection.DOWN).map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .doubles(INITIAL_SET_POINT, RaoDataframes::getInitialSetpointOrNaN)
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
            .stringsIndex(ID, Range::raId)
            .doubles(MIN, Range::min)
            .doubles(MAX, Range::max)
            .strings(RANGE_TYPE, r -> r.rangeType().name())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracNetworkActions() {
        return new DataframeMapperBuilder<Crac, NetworkAction, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().toList())
            .stringsIndex(ID, NetworkAction::getId)
            .strings(NAME, NetworkAction::getName)
            .strings(OPERATOR, NetworkAction::getOperator)
            .optionalInts(SPEED, a -> a.getSpeed().map(OptionalInt::of).orElse(OptionalInt.empty()))
            .optionalDoubles(ACTIVATION_COST, a -> a.getActivationCost().map(OptionalDouble::of).orElseGet(OptionalDouble::empty))
            .build();
    }

    public static DataframeMapper<Crac, Void> cracTerminalConnectionActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, TerminalsConnectionAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                    .filter(TerminalsConnectionAction.class::isInstance)
                    .map(elementary -> Pair.create(a, (TerminalsConnectionAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getElementId())
            .strings(ACTION_TYPE, pair -> pair.getSecond().isOpen() ? OPEN : CLOSE)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracPstTapPositionActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, PhaseTapChangerTapPositionAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(PhaseTapChangerTapPositionAction.class::isInstance)
                .map(elementary -> Pair.create(a, (PhaseTapChangerTapPositionAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getTransformerId())
            .ints(TAP_POSITION, pair -> pair.getSecond().getTapPosition())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracGeneratorActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, GeneratorAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(GeneratorAction.class::isInstance)
                .map(elementary -> Pair.create(a, (GeneratorAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getGeneratorId())
            .optionalDoubles(ACTIVE_POWER_VALUE, pair -> pair.getSecond().getActivePowerValue())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracLoadActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, LoadAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(LoadAction.class::isInstance)
                .map(elementary -> Pair.create(a, (LoadAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getLoadId())
            .optionalDoubles(ACTIVE_POWER_VALUE, pair -> pair.getSecond().getActivePowerValue())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracBoundaryLineActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, BoundaryLineAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(BoundaryLineAction.class::isInstance)
                .map(elementary -> Pair.create(a, (BoundaryLineAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getBoundaryLineId())
            .optionalDoubles(ACTIVE_POWER_VALUE, pair -> pair.getSecond().getActivePowerValue())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracShuntCompensatorPositionActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, ShuntCompensatorPositionAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(ShuntCompensatorPositionAction.class::isInstance)
                .map(elementary -> Pair.create(a, (ShuntCompensatorPositionAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getShuntCompensatorId())
            .ints(SECTION_COUNT, pair -> pair.getSecond().getSectionCount())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracSwitchActions() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, SwitchAction>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(SwitchAction.class::isInstance)
                .map(elementary -> Pair.create(a, (SwitchAction) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(NETWORK_ELEMENT_ID, pair -> pair.getSecond().getSwitchId())
            .strings(ACTION_TYPE, pair -> pair.getSecond().isOpen() ? OPEN : CLOSE)
            .build();
    }

    public static DataframeMapper<Crac, Void> cracSwitchPairs() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, SwitchPair>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getElementaryActions().stream()
                .filter(SwitchPair.class::isInstance)
                .map(elementary -> Pair.create(a, (SwitchPair) elementary))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(OPEN, pair -> pair.getSecond().getSwitchToOpen().getId())
            .strings(CLOSE, pair -> pair.getSecond().getSwitchToClose().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnInstantUsageRules() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, UsageRule>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream().map(rule -> Pair.create(a, rule))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(INSTANT, pair -> pair.getSecond().getInstant().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnContingencyStateUsageRules() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, OnContingencyState>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream()
                .filter(OnContingencyState.class::isInstance).map(rule -> Pair.create(a, (OnContingencyState) rule))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(INSTANT, pair -> pair.getSecond().getInstant().getId())
            .strings(CONTINGENCY_ID, pair -> pair.getSecond().getContingency().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnConstraintUsageRules() {
        return new DataframeMapperBuilder<Crac, Pair<NetworkAction, OnConstraint<Cnec<?>>>, Void>()
            .itemsProvider(crac -> crac.getNetworkActions().stream().flatMap(a -> a.getUsageRules().stream()
                .filter(OnConstraint.class::isInstance).map(rule -> Pair.create(a, (OnConstraint<Cnec<?>>) rule))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(INSTANT, pair -> pair.getSecond().getInstant().getId())
            .strings(CNEC_ID, pair -> pair.getSecond().getCnec().getId())
            .build();
    }

    public static DataframeMapper<Crac, Void> cracOnFlowConstraintInCountryUsageRules() {
        return new DataframeMapperBuilder<Crac, Pair<RemedialAction, OnFlowConstraintInCountry>, Void>()
            .itemsProvider(crac -> crac.getRemedialActions().stream().flatMap(a -> a.getUsageRules().stream()
                .filter(OnFlowConstraintInCountry.class::isInstance).map(rule -> Pair.create((RemedialAction) a, (OnFlowConstraintInCountry) rule))).toList())
            .stringsIndex(ID, pair -> pair.getFirst().getId())
            .strings(INSTANT, pair -> pair.getSecond().getInstant().getId())
            .strings(CONTINGENCY_ID, pair -> pair.getSecond().getContingency().map(Contingency::getId).orElse(""))
            .strings(COUNTRY, pair -> pair.getSecond().getCountry().name())
            .build();
    }
}
