/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RaUsageLimitsAdder;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.ContingencyAdder;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.CnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRangeAdder;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.*;
import com.powsybl.openrao.data.crac.api.threshold.AngleThresholdAdder;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.crac.api.threshold.VoltageThresholdAdder;

import java.util.*;
import java.util.function.ObjIntConsumer;

import static com.powsybl.python.rao.RaoConstants.*;

/**
 * Builds CRAC objects (instants, contingencies, CNECs, remedial actions) from dataframes sent by Python.
 * <p>
 * Each object is described by a one-row "attributes" dataframe, in which optional attributes are simply
 * missing columns (or empty strings / NaN), and by additional dataframes for its sub-elements.
 *
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
final class CracCreationUtil {

    private CracCreationUtil() {
    }

    static void addInstant(Crac crac, String id, String kind) {
        crac.newInstant(id, toEnum(InstantKind.class, kind));
    }

    static void addRaUsageLimits(Crac crac, UpdatingDataframe attributes, UpdatingDataframe perTsoLimits) {
        String instantId = getString(attributes, INSTANT, 0);
        RaUsageLimitsAdder adder = crac.newRaUsageLimits(instantId);
        getOptionalInt(attributes, MAX_REMEDIAL_ACTIONS, 0).ifPresent(adder::withMaxRa);
        if (getOptionalInt(attributes, MAX_TSOS, 0).isPresent()) {
            throw new PowsyblException("Limiting the maximum number of TSOs is not supported by this version of OpenRAO");
        }
        Map<String, Map<String, Integer>> limits = new HashMap<>();
        applyOnEachRow(perTsoLimits, (df, row) -> limits
            .computeIfAbsent(getString(df, LIMIT_TYPE, row), k -> new HashMap<>())
            .put(getString(df, TSO, row), getInt(df, VALUE, row)));
        for (Map.Entry<String, Map<String, Integer>> e : limits.entrySet()) {
            switch (e.getKey()) {
                case MAX_TOPOLOGICAL_ACTIONS_PER_TSO -> adder.withMaxTopoPerTso(e.getValue());
                case MAX_PSTS_PER_TSO -> adder.withMaxPstPerTso(e.getValue());
                case MAX_REMEDIAL_ACTIONS_PER_TSO -> adder.withMaxRaPerTso(e.getValue());
                case MAX_ELEMENTARY_ACTIONS_PER_TSO -> adder.withMaxElementaryActionPerTso(e.getValue());
                default -> throw new PowsyblException("Unknown usage limit type: " + e.getKey());
            }
        }
        adder.add();
    }

    static void addContingency(Crac crac, Network network, String id, List<String> networkElementIds) {
        ContingencyAdder adder = crac.newContingency().withId(id);
        for (String elementId : networkElementIds) {
            Identifiable<?> identifiable = network.getIdentifiable(elementId);
            if (identifiable == null) {
                throw new PowsyblException("Network element '" + elementId + "' of contingency '" + id + "' not found in network");
            }
            ContingencyElementType type;
            try {
                type = ContingencyElementType.valueOf(identifiable.getType().name());
            } catch (IllegalArgumentException e) {
                throw new PowsyblException("Network element '" + elementId + "' of type " + identifiable.getType() + " cannot be used in a contingency");
            }
            adder.withContingencyElement(elementId, type);
        }
        adder.add();
    }

    // ---- CNECs ----

    private static <T extends CnecAdder<T>> T fillCnec(T adder, UpdatingDataframe attributes) {
        adder.withId(getString(attributes, ID, 0))
            .withInstant(getString(attributes, INSTANT, 0))
            .withOptimized(getBoolean(attributes, OPTIMIZED, 0, false))
            .withMonitored(getBoolean(attributes, MONITORED, 0, false));
        getOptionalString(attributes, NAME, 0).ifPresent(adder::withName);
        getOptionalString(attributes, OPERATOR, 0).ifPresent(adder::withOperator);
        getOptionalString(attributes, BORDER, 0).ifPresent(adder::withBorder);
        getOptionalString(attributes, CONTINGENCY_ID, 0).ifPresent(adder::withContingency);
        getOptionalDouble(attributes, RELIABILITY_MARGIN, 0).ifPresent(adder::withReliabilityMargin);
        return adder;
    }

    static void addFlowCnec(Crac crac, Network network, UpdatingDataframe attributes, UpdatingDataframe thresholds) {
        String networkElementId = getString(attributes, NETWORK_ELEMENT_ID, 0);
        FlowCnecAdder adder = fillCnec(crac.newFlowCnec(), attributes).withNetworkElement(networkElementId);
        // nominal voltages and iMax are needed for thresholds in AMPERE or PERCENT_IMAX
        // TODO Check if nominal voltages and iMax should be provided as attributes in the dataframe rather that being queried on the network
        if (network != null && network.getIdentifiable(networkElementId) instanceof Branch<?> branch) {
            for (TwoSides side : TwoSides.values()) {
                adder.withNominalVoltage(branch.getTerminal(side).getVoltageLevel().getNominalV(), side);
                branch.getCurrentLimits(side).ifPresent(limits -> adder.withIMax(limits.getPermanentLimit(), side));
            }
        }
        applyOnEachRow(thresholds, (df, row) -> {
            BranchThresholdAdder thresholdAdder = adder.newThreshold()
                .withUnit(toEnum(Unit.class, getString(df, UNIT, row)))
                .withSide(toEnum(TwoSides.class, getString(df, SIDE, row)));
            getOptionalDouble(df, LOWER_BOUND, row).ifPresent(thresholdAdder::withMin);
            getOptionalDouble(df, UPPER_BOUND, row).ifPresent(thresholdAdder::withMax);
            thresholdAdder.add();
        });
        adder.add();
    }

    static void addAngleCnec(Crac crac, UpdatingDataframe attributes, UpdatingDataframe thresholds) {
        AngleCnecAdder adder = fillCnec(crac.newAngleCnec(), attributes)
            .withExportingNetworkElement(getString(attributes, EXPORTING_NETWORK_ELEMENT_ID, 0))
            .withImportingNetworkElement(getString(attributes, IMPORTING_NETWORK_ELEMENT_ID, 0));
        applyOnEachRow(thresholds, (df, row) -> {
            AngleThresholdAdder thresholdAdder = adder.newThreshold().withUnit(toEnum(Unit.class, getString(df, UNIT, row)));
            getOptionalDouble(df, LOWER_BOUND, row).ifPresent(thresholdAdder::withMin);
            getOptionalDouble(df, UPPER_BOUND, row).ifPresent(thresholdAdder::withMax);
            thresholdAdder.add();
        });
        adder.add();
    }

    static void addVoltageCnec(Crac crac, UpdatingDataframe attributes, UpdatingDataframe thresholds) {
        VoltageCnecAdder adder = fillCnec(crac.newVoltageCnec(), attributes)
            .withNetworkElement(getString(attributes, NETWORK_ELEMENT_ID, 0));
        applyOnEachRow(thresholds, (df, row) -> {
            VoltageThresholdAdder thresholdAdder = adder.newThreshold().withUnit(toEnum(Unit.class, getString(df, UNIT, row)));
            getOptionalDouble(df, LOWER_BOUND, row).ifPresent(thresholdAdder::withMin);
            getOptionalDouble(df, UPPER_BOUND, row).ifPresent(thresholdAdder::withMax);
            thresholdAdder.add();
        });
        adder.add();
    }

    private static <T extends RemedialActionAdder<T>> T fillRemedialAction(T adder, UpdatingDataframe attributes, UpdatingDataframe usageRules) {
        adder.withId(getString(attributes, ID, 0));
        getOptionalString(attributes, NAME, 0).ifPresent(adder::withName);
        getOptionalString(attributes, OPERATOR, 0).ifPresent(adder::withOperator);
        getOptionalInt(attributes, SPEED, 0).ifPresent(adder::withSpeed);
        getOptionalDouble(attributes, ACTIVATION_COST, 0).ifPresent(adder::withActivationCost);
        addUsageRules(adder, usageRules);
        return adder;
    }

    /**
     * @param usageRules columns: usage_rule_type (on_instant, on_contingency_state, on_constraint,
     *                   on_flow_constraint_in_country), instant, contingency_id, cnec_id, country
     */
    private static <T extends RemedialActionAdder<T>> void addUsageRules(T adder, UpdatingDataframe usageRules) {
        applyOnEachRow(usageRules, (df, row) -> {
            String instant = getString(df, INSTANT, row);
            String type = getString(df, USAGE_RULE_TYPE, row);
            switch (type) {
                case ON_INSTANT -> adder.newOnInstantUsageRule().withInstant(instant).add();
                case ON_CONTINGENCY_STATE -> adder.newOnContingencyStateUsageRule()
                    .withInstant(instant)
                    .withContingency(getString(df, CONTINGENCY_ID, row))
                    .add();
                case ON_CONSTRAINT -> adder.newOnConstraintUsageRule()
                    .withInstant(instant)
                    .withCnec(getString(df, CNEC_ID, row))
                    .add();
                case ON_FLOW_CONSTRAINT_IN_COUNTRY -> {
                    var ruleAdder = adder.newOnFlowConstraintInCountryUsageRule()
                        .withInstant(instant)
                        .withCountry(toEnum(Country.class, getString(df, COUNTRY, row)));
                    getOptionalString(df, CONTINGENCY_ID, row).ifPresent(ruleAdder::withContingency);
                    ruleAdder.add();
                }
                default -> throw new PowsyblException("Unknown usage rule type: " + type);
            }
        });
    }

    private static <T extends StandardRangeActionAdder<T>> T fillStandardRangeAction(T adder, UpdatingDataframe attributes,
                                                                                     UpdatingDataframe ranges, UpdatingDataframe usageRules) {
        fillRemedialAction(adder, attributes, usageRules);
        getOptionalString(attributes, GROUP_ID, 0).ifPresent(adder::withGroupId);
        getOptionalDouble(attributes, UPWARD_VARIATION_COST, 0).ifPresent(cost -> adder.withVariationCost(cost, VariationDirection.UP));
        getOptionalDouble(attributes, DOWNWARD_VARIATION_COST, 0).ifPresent(cost -> adder.withVariationCost(cost, VariationDirection.DOWN));
        applyOnEachRow(ranges, (df, row) -> {
            StandardRangeAdder<T> rangeAdder = adder.newRange().withRangeType(toEnum(RangeType.class, getString(df, RANGE_TYPE, row)));
            getOptionalDouble(df, LOWER_BOUND, row).ifPresent(rangeAdder::withMin);
            getOptionalDouble(df, UPPER_BOUND, row).ifPresent(rangeAdder::withMax);
            rangeAdder.add();
        });
        return adder;
    }

    static void addNetworkAction(Crac crac, UpdatingDataframe attributes,
                                 UpdatingDataframe terminalConnectionActions, UpdatingDataframe pstTapPositionActions,
                                 UpdatingDataframe generatorActions, UpdatingDataframe loadActions,
                                 UpdatingDataframe boundaryLineActions, UpdatingDataframe shuntCompensatorPositionActions,
                                 UpdatingDataframe switchActions, UpdatingDataframe switchPairs,
                                 UpdatingDataframe usageRules) {
        NetworkActionAdder adder = fillRemedialAction(crac.newNetworkAction(), attributes, usageRules);
        applyOnEachRow(terminalConnectionActions, (df, row) -> adder.newTerminalsConnectionAction()
            .withNetworkElement(getString(df, TERMINAL_ID, row))
            .withActionType(toEnum(ActionType.class, getString(df, ACTION_TYPE, row)))
            .add());
        applyOnEachRow(pstTapPositionActions, (df, row) -> adder.newPhaseTapChangerTapPositionAction()
            .withNetworkElement(getString(df, PHASE_TAP_CHANGER_ID, row))
            .withTapPosition(getInt(df, TAP_POSITION, row))
            .add());
        applyOnEachRow(generatorActions, (df, row) -> adder.newGeneratorAction()
            .withNetworkElement(getString(df, GENERATOR_ID, row))
            .withActivePowerValue(getDouble(df, ACTIVE_POWER_VALUE, row))
            .add());
        applyOnEachRow(loadActions, (df, row) -> adder.newLoadAction()
            .withNetworkElement(getString(df, LOAD_ID, row))
            .withActivePowerValue(getDouble(df, ACTIVE_POWER_VALUE, row))
            .add());
        applyOnEachRow(boundaryLineActions, (df, row) -> adder.newBoundaryLineAction()
            .withNetworkElement(getString(df, DANGLING_LINE_ID, row))
            .withActivePowerValue(getDouble(df, ACTIVE_POWER_VALUE, row))
            .add());
        applyOnEachRow(shuntCompensatorPositionActions, (df, row) -> adder.newShuntCompensatorPositionAction()
            .withNetworkElement(getString(df, SHUNT_COMPENSATOR_ID, row))
            .withSectionCount(getInt(df, SECTION_COUNT, row))
            .add());
        applyOnEachRow(switchActions, (df, row) -> adder.newSwitchAction()
            .withNetworkElement(getString(df, SWITCH_ID, row))
            .withActionType(toEnum(ActionType.class, getString(df, ACTION_TYPE, row)))
            .add());
        applyOnEachRow(switchPairs, (df, row) -> adder.newSwitchPair()
            .withSwitchToOpen(getString(df, SWITCH_TO_OPEN_ID, row))
            .withSwitchToClose(getString(df, SWITCH_TO_CLOSE_ID, row))
            .add());
        adder.add();
    }

    static void addPstRangeAction(Crac crac, Network network, UpdatingDataframe attributes,
                                  UpdatingDataframe ranges, UpdatingDataframe usageRules) {
        String networkElementId = getString(attributes, NETWORK_ELEMENT_ID, 0);
        PstRangeActionAdder adder = fillRemedialAction(crac.newPstRangeAction(), attributes, usageRules)
            .withNetworkElement(networkElementId);
        getOptionalString(attributes, GROUP_ID, 0).ifPresent(adder::withGroupId);
        getOptionalDouble(attributes, UPWARD_VARIATION_COST, 0).ifPresent(cost -> adder.withVariationCost(cost, VariationDirection.UP));
        getOptionalDouble(attributes, DOWNWARD_VARIATION_COST, 0).ifPresent(cost -> adder.withVariationCost(cost, VariationDirection.DOWN));

        // initial tap and tap to angle conversion map are read from the network
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElementId);
        if (transformer == null || !transformer.hasPhaseTapChanger()) {
            throw new PowsyblException("Network element '" + networkElementId + "' is not a two windings transformer with a phase tap changer");
        }
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        Map<Integer, Double> tapToAngle = new HashMap<>();
        for (int tap = phaseTapChanger.getLowTapPosition(); tap <= phaseTapChanger.getHighTapPosition(); tap++) {
            tapToAngle.put(tap, phaseTapChanger.getStep(tap).getAlpha());
        }
        adder.withInitialTap(phaseTapChanger.getTapPosition()).withTapToAngleConversionMap(tapToAngle);

        // tap bounds are sent as doubles so that they can be undefined (NaN)
        applyOnEachRow(ranges, (df, row) -> {
            TapRangeAdder rangeAdder = adder.newTapRange().withRangeType(toEnum(RangeType.class, getString(df, RANGE_TYPE, row)));
            getOptionalDouble(df, LOWER_BOUND, row).ifPresent(min -> rangeAdder.withMinTap((int) min));
            getOptionalDouble(df, UPPER_BOUND, row).ifPresent(max -> rangeAdder.withMaxTap((int) max));
            rangeAdder.add();
        });
        adder.add();
    }

    static void addHvdcRangeAction(Crac crac, UpdatingDataframe attributes, UpdatingDataframe ranges, UpdatingDataframe usageRules) {
        fillStandardRangeAction(crac.newHvdcRangeAction(), attributes, ranges, usageRules)
            .withNetworkElement(getString(attributes, NETWORK_ELEMENT_ID, 0))
            .add();
    }

    /**
     * @param networkElementsAndKeys columns: network_element_id, key
     */
    static void addInjectionRangeAction(Crac crac, UpdatingDataframe attributes, UpdatingDataframe networkElementsAndKeys,
                                        UpdatingDataframe ranges, UpdatingDataframe usageRules) {
        InjectionRangeActionAdder adder = fillStandardRangeAction(crac.newInjectionRangeAction(), attributes, ranges, usageRules);
        applyOnEachRow(networkElementsAndKeys, (df, row) ->
            adder.withNetworkElementAndKey(getDouble(df, KEY, row), getString(df, NETWORK_ELEMENT_ID, row)));
        adder.add();
    }

    static void addCounterTradeRangeAction(Crac crac, UpdatingDataframe attributes, UpdatingDataframe ranges, UpdatingDataframe usageRules) {
        fillStandardRangeAction(crac.newCounterTradeRangeAction(), attributes, ranges, usageRules)
            .withExportingArea(getString(attributes, EXPORTING_COUNTRY, 0))
            .withImportingArea(getString(attributes, IMPORTING_COUNTRY, 0))
            .add();
    }

    static Optional<String> getOptionalString(UpdatingDataframe df, String column, int row) {
        if (df == null) {
            return Optional.empty();
        }
        StringSeries series = df.getStrings(column);
        if (series == null) {
            return Optional.empty();
        }
        String value = series.get(row);
        return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    static OptionalDouble getOptionalDouble(UpdatingDataframe df, String column, int row) {
        if (df == null) {
            return OptionalDouble.empty();
        }
        DoubleSeries series = df.getDoubles(column);
        if (series == null) {
            return OptionalDouble.empty();
        }
        double value = series.get(row);
        return Double.isNaN(value) ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    static OptionalInt getOptionalInt(UpdatingDataframe df, String column, int row) {
        if (df == null) {
            return OptionalInt.empty();
        }
        IntSeries series = df.getInts(column);
        return series == null ? OptionalInt.empty() : OptionalInt.of(series.get(row));
    }

    static String getString(UpdatingDataframe df, String column, int row) {
        return getOptionalString(df, column, row)
                .orElseThrow(() -> missingValue(column, row));
    }

    static double getDouble(UpdatingDataframe df, String column, int row) {
        return getOptionalDouble(df, column, row)
                .orElseThrow(() -> missingValue(column, row));
    }

    static int getInt(UpdatingDataframe df, String column, int row) {
        return getOptionalInt(df, column, row)
                .orElseThrow(() -> missingValue(column, row));
    }

    private static PowsyblException missingValue(String column, int row) {
        return new PowsyblException("Missing value for column '" + column + "' at row " + row);
    }

    static boolean getBoolean(UpdatingDataframe df, String column, int row, boolean defaultValue) {
        OptionalInt value = getOptionalInt(df, column, row);
        return value.isPresent() ? value.getAsInt() != 0 : defaultValue;
    }

    private static void applyOnEachRow(UpdatingDataframe df, ObjIntConsumer<UpdatingDataframe> applyOnRow) {
        if (df == null) {
            return;
        }
        for (int row = 0; row < df.getRowCount(); row++) {
            applyOnRow.accept(df, row);
        }
    }

    private static <E extends Enum<E>> E toEnum(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PowsyblException("Invalid value '" + value + "' for " + enumClass.getSimpleName()
                    + ", expected one of " + Arrays.stream(enumClass.getEnumConstants()).map(c -> c.name().toLowerCase(Locale.ROOT)).toList());
        }
    }
}
