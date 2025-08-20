package com.powsybl.python.rao;

import com.powsybl.contingency.Contingency;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.MinOrMax;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final DataframeMapper<Crac, RaoResult> FLOW_CNEC_RESULT_MAPPER = createFlowCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> ANGLE_CNEC_RESULT_MAPPER = createAngleCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> VOLTAGE_CNEC_RESULT_MAPPER = createVoltageCnecResultMapper();
    private static final DataframeMapper<Crac, RaoResult> RA_RESULT_MAPPER = createRemedialActionResultMapper();
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

    public static DataframeMapper<Crac, RaoResult> raResultMapper() {
        return RA_RESULT_MAPPER;
    }

    public static DataframeMapper<Crac, RaoResult> costResultMapper() {
        return COST_RESULT_MAPPER;
    }

    public record FlowCnecResult(String cnecId, Instant instant, String contingency, TwoSides side, double flow, double margin, double relativeMargin, double commercialFlow, double loopFlow, double ptdfZonalSum) { }

    public record AngleCnecResult(String cnecId, Instant instant, String contingency, double angle, double margin) { }

    public record VoltageCnecResult(String cnecId, Instant instant, String contingency, TwoSides side, double minVoltage, double maxVoltage, double margin) { }

    public record ActivatedRemedialActionResult(String remedialActionId, Instant instant, String contingency, double optimizedTap, double optimizedSetPoint) { }

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
                result.getVoltage(instant, cnec, MinOrMax.MIN, Unit.KILOVOLT),
                result.getVoltage(instant, cnec, MinOrMax.MAX, Unit.KILOVOLT),
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
                    Pair<Double, Double> optimizedValues = getActivatedRangeAction(ra, state, raoResult);
                    double optimizedTap = optimizedValues.getLeft();
                    double optimizedSetPoint = optimizedValues.getRight();
                    // Network actions will have optimizedTap and optimizedSetpoint set to NaN
                    ActivatedRemedialActionResult result = new ActivatedRemedialActionResult(
                        ra.getId(),
                        state.getInstant(),
                        contingencyOpt.isPresent() ? contingencyOpt.get().getId() : "",
                        optimizedTap,
                        optimizedSetPoint
                    );
                    results.add(result);
                }
            }
        }
        return results;
    }

    private static Pair<Double, Double> getActivatedRangeAction(RemedialAction<?> ra, State state, RaoResult raoResult) {
        double optimizedTap = Double.NaN; // Use a double for tap so we can set it to NaN when not relevant
        double optimizedSetPoint = Double.NaN;
        if (ra instanceof RangeAction<?> rangeAction) {
            // Pst range actions will have optimizedSetpoint set to NaN
            if (rangeAction instanceof PstRangeAction pstRangeAction) {
                optimizedTap = raoResult.getOptimizedTapOnState(state, pstRangeAction);
                // Other than Pst range actions will have optimizedTap set to NaN
            } else {
                optimizedSetPoint = raoResult.getOptimizedSetPointOnState(state, rangeAction);
            }
        }
        return Pair.of(optimizedTap, optimizedSetPoint);
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
            .strings("remedial_action_id", ActivatedRemedialActionResult::remedialActionId)
            .strings(OPTIMIZED_INSTANT, r -> r.instant() != null ? r.instant().getId() : INITIAL_INSTANT)
            .strings(CONTINGENCY, ActivatedRemedialActionResult::contingency)
            .doubles("optimized_tap", ActivatedRemedialActionResult::optimizedTap)
            .doubles("optimized_set_point", ActivatedRemedialActionResult::optimizedSetPoint)
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
}
