/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.BooleanSeriesMapper;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.DoubleSeriesMapper;
import com.powsybl.dataframe.network.extensions.NetworkExtensions;
import com.powsybl.dataframe.network.extensions.ExtensionDataframeKey;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.network.NetworkUtil;
import com.powsybl.python.network.SideEnum;
import com.powsybl.python.network.TemporaryLimitData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static com.powsybl.dataframe.MappingUtils.*;
import static com.powsybl.dataframe.network.PerUnitUtil.*;

/**
 * Main user entry point of the package :
 * defines the mappings for all elements of the network.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class NetworkDataframes {

    private static final Map<DataframeElementType, NetworkDataframeMapper> MAPPERS = createMappers();

    private static final Map<ExtensionDataframeKey, NetworkDataframeMapper> EXTENSIONS_MAPPERS = NetworkExtensions.createExtensionsMappers();

    private static final String DEFAULT_OPERATIONAL_LIMIT_GROUP_ID = "DEFAULT";

    private NetworkDataframes() {
    }

    public static NetworkDataframeMapper getDataframeMapper(DataframeElementType type) {
        return MAPPERS.get(type);
    }

    private static Map<DataframeElementType, NetworkDataframeMapper> createMappers() {
        Map<DataframeElementType, NetworkDataframeMapper> mappers = new EnumMap<>(DataframeElementType.class);
        mappers.put(DataframeElementType.SUB_NETWORK, subNetworks());
        mappers.put(DataframeElementType.AREA, areas());
        mappers.put(DataframeElementType.AREA_VOLTAGE_LEVELS, areaVoltageLevels());
        mappers.put(DataframeElementType.AREA_BOUNDARIES, areaBoundaries());
        mappers.put(DataframeElementType.BUS, buses(false));
        mappers.put(DataframeElementType.BUS_FROM_BUS_BREAKER_VIEW, buses(true));
        mappers.put(DataframeElementType.LINE, lines());
        mappers.put(DataframeElementType.TWO_WINDINGS_TRANSFORMER, twoWindingTransformers());
        mappers.put(DataframeElementType.THREE_WINDINGS_TRANSFORMER, threeWindingTransformers());
        mappers.put(DataframeElementType.GENERATOR, generators());
        mappers.put(DataframeElementType.LOAD, loads());
        mappers.put(DataframeElementType.BATTERY, batteries());
        mappers.put(DataframeElementType.SHUNT_COMPENSATOR, shunts());
        mappers.put(DataframeElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, shuntsNonLinear());
        mappers.put(DataframeElementType.LINEAR_SHUNT_COMPENSATOR_SECTION, linearShuntsSections());
        mappers.put(DataframeElementType.DANGLING_LINE, danglingLines());
        mappers.put(DataframeElementType.TIE_LINE, tieLines());
        mappers.put(DataframeElementType.LCC_CONVERTER_STATION, lccs());
        mappers.put(DataframeElementType.VSC_CONVERTER_STATION, vscs());
        mappers.put(DataframeElementType.STATIC_VAR_COMPENSATOR, svcs());
        mappers.put(DataframeElementType.SWITCH, switches());
        mappers.put(DataframeElementType.VOLTAGE_LEVEL, voltageLevels());
        mappers.put(DataframeElementType.SUBSTATION, substations());
        mappers.put(DataframeElementType.BUSBAR_SECTION, busbarSections());
        mappers.put(DataframeElementType.HVDC_LINE, hvdcs());
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER_STEP, rtcSteps());
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER_STEP, ptcSteps());
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER, rtcs());
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER, ptcs());
        mappers.put(DataframeElementType.REACTIVE_CAPABILITY_CURVE_POINT, reactiveCapabilityCurves());
        mappers.put(DataframeElementType.OPERATIONAL_LIMITS, operationalLimits(false));
        mappers.put(DataframeElementType.SELECTED_OPERATIONAL_LIMITS, operationalLimits(true));
        mappers.put(DataframeElementType.ALIAS, aliases());
        mappers.put(DataframeElementType.IDENTIFIABLE, identifiables());
        mappers.put(DataframeElementType.INJECTION, injections());
        mappers.put(DataframeElementType.BRANCH, branches());
        mappers.put(DataframeElementType.TERMINAL, terminals());
        return Collections.unmodifiableMap(mappers);
    }

    static <U extends Injection<?>> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitP() {
        return (inj, context) -> perUnitPQ(context, inj.getTerminal().getP());
    }

    static <U extends Injection<?>> ToDoubleFunction<U> getOppositeP() {
        return inj -> -inj.getTerminal().getP();
    }

    static <U extends Injection<?>> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitQ() {
        return (inj, context) -> perUnitPQ(context, inj.getTerminal().getQ());
    }

    static <U extends Injection<?>> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitP() {
        return (inj, p, context) -> inj.getTerminal().setP(unPerUnitPQ(context, p));
    }

    static <U extends Injection<?>> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitQ() {
        return (inj, q, context) -> inj.getTerminal().setQ(unPerUnitPQ(context, q));
    }

    static <U extends Branch<?>> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitP1() {
        return (b, context) -> perUnitPQ(context, b.getTerminal1().getP());
    }

    static <U extends Branch<?>> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitQ1() {
        return (b, context) -> perUnitPQ(context, b.getTerminal1().getQ());
    }

    static <U extends Branch<?>> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitP1() {
        return (b, p, context) -> b.getTerminal1().setP(unPerUnitPQ(context, p));
    }

    static <U extends Branch<?>> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitQ1() {
        return (b, q, context) -> b.getTerminal1().setQ(unPerUnitPQ(context, q));
    }

    static <U extends Branch<?>> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitP2() {
        return (b, context) -> perUnitPQ(context, b.getTerminal2().getP());
    }

    static <U extends Branch<?>> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitQ2() {
        return (b, context) -> perUnitPQ(context, b.getTerminal2().getQ());
    }

    static <U extends Branch<?>> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitP2() {
        return (b, p, context) -> b.getTerminal2().setP(unPerUnitPQ(context, p));
    }

    static <U extends Branch<?>> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitQ2() {
        return (b, q, context) -> b.getTerminal2().setQ(unPerUnitPQ(context, q));
    }

    static <U extends Injection<U>> Function<U, String> getVoltageLevelId() {
        return inj -> inj.getTerminal().getVoltageLevel().getId();
    }

    private static MinMaxReactiveLimits getMinMaxReactiveLimits(ReactiveLimitsHolder holder) {
        ReactiveLimits reactiveLimits = holder.getReactiveLimits();
        return reactiveLimits instanceof MinMaxReactiveLimits ? (MinMaxReactiveLimits) reactiveLimits : null;
    }

    static <U extends ReactiveLimitsHolder> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitMinQ(ToDoubleFunction<U> pGetter) {
        return (g, context) -> {
            ReactiveLimits reactiveLimits = g.getReactiveLimits();
            return (reactiveLimits == null) ? Double.NaN : perUnitPQ(context, reactiveLimits.getMinQ(pGetter.applyAsDouble(g)));
        };
    }

    static <U extends ReactiveLimitsHolder> ToDoubleBiFunction<U, NetworkDataframeContext> getPerUnitMaxQ(ToDoubleFunction<U> pGetter) {
        return (g, context) -> {
            ReactiveLimits reactiveLimits = g.getReactiveLimits();
            return (reactiveLimits == null) ? Double.NaN : perUnitPQ(context, reactiveLimits.getMaxQ(pGetter.applyAsDouble(g)));
        };
    }

    static <U extends ReactiveLimitsHolder> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitMinQ() {
        return (g, minQ, context) -> {
            MinMaxReactiveLimits minMaxReactiveLimits = getMinMaxReactiveLimits(g);
            if (minMaxReactiveLimits != null) {
                g.newMinMaxReactiveLimits().setMinQ(unPerUnitPQ(context, minQ))
                    .setMaxQ(minMaxReactiveLimits.getMaxQ()).add();
            } else {
                throw new UnsupportedOperationException("Cannot update minQ to " + minQ +
                    ": Min-Max reactive limits do not exist.");
            }
        };
    }

    static <U extends ReactiveLimitsHolder> DoubleSeriesMapper.DoubleUpdater<U, NetworkDataframeContext> setPerUnitMaxQ() {
        return (g, maxQ, context) -> {
            MinMaxReactiveLimits minMaxReactiveLimits = getMinMaxReactiveLimits(g);
            if (minMaxReactiveLimits != null) {
                g.newMinMaxReactiveLimits().setMaxQ(unPerUnitPQ(context, maxQ))
                    .setMinQ(minMaxReactiveLimits.getMinQ()).add();
            } else {
                throw new UnsupportedOperationException("Cannot update maxQ to " + maxQ +
                    ": Min-Max reactive limits do not exist.");
            }
        };
    }

    private static String getReactiveLimitsKind(ReactiveLimitsHolder holder) {
        ReactiveLimits reactiveLimits = holder.getReactiveLimits();
        return (reactiveLimits == null) ? "NONE"
                : reactiveLimits.getKind().name();
    }

    private static <U extends Injection<?>> BooleanSeriesMapper.BooleanUpdater<U> connectInjection() {
        return (g, b) -> {
            Boolean res = b ? g.getTerminal().connect() : g.getTerminal().disconnect();
        };
    }

    private static <U extends Branch<U>> BooleanSeriesMapper.BooleanUpdater<U> connectBranchSide1() {
        return (g, b) -> {
            Boolean res = b ? g.getTerminal1().connect() : g.getTerminal1().disconnect();
        };
    }

    private static <U extends Branch<U>> BooleanSeriesMapper.BooleanUpdater<U> connectBranchSide2() {
        return (g, b) -> {
            Boolean res = b ? g.getTerminal2().connect() : g.getTerminal2().disconnect();
        };
    }

    private static <U extends ThreeWindingsTransformer> BooleanSeriesMapper.BooleanUpdater<U> connectLeg1() {
        return (g, b) -> {
            Boolean res = b ? g.getLeg1().getTerminal().connect() : g.getLeg1().getTerminal().disconnect();
        };
    }

    private static <U extends ThreeWindingsTransformer> BooleanSeriesMapper.BooleanUpdater<U> connectLeg2() {
        return (g, b) -> {
            Boolean res = b ? g.getLeg2().getTerminal().connect() : g.getLeg2().getTerminal().disconnect();
        };
    }

    private static <U extends ThreeWindingsTransformer> BooleanSeriesMapper.BooleanUpdater<U> connectLeg3() {
        return (g, b) -> {
            Boolean res = b ? g.getLeg3().getTerminal().connect() : g.getLeg3().getTerminal().disconnect();
        };
    }

    private static BooleanSeriesMapper.BooleanUpdater<HvdcLine> connectHvdcStation1() {
        return (g, b) -> {
            Boolean res = b ? g.getConverterStation1().getTerminal().connect() : g.getConverterStation1().getTerminal().disconnect();

        };
    }

    private static BooleanSeriesMapper.BooleanUpdater<HvdcLine> connectHvdcStation2() {
        return (g, b) -> {
            Boolean res = b ? g.getConverterStation2().getTerminal().connect() : g.getConverterStation2().getTerminal().disconnect();
        };
    }

    public static <T, U> ToDoubleBiFunction<T, NetworkDataframeContext> ifExistsDoublePerUnitPQ(Function<T, U> objectGetter, ToDoubleFunction<U> valueGetter) {
        return (item, context) -> {
            U object = objectGetter.apply(item);
            return object != null ? PerUnitUtil.perUnitPQ(context, valueGetter.applyAsDouble(object)) : Double.NaN;
        };
    }

    public static <T, U> ToDoubleBiFunction<T, NetworkDataframeContext> ifExistsDoublePerUnitAngle(Function<T, U> objectGetter, ToDoubleFunction<U> valueGetter) {
        return (item, context) -> {
            U object = objectGetter.apply(item);
            return object != null ? PerUnitUtil.perUnitAngle(context, valueGetter.applyAsDouble(object)) : Double.NaN;
        };
    }

    static NetworkDataframeMapper generators() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getGeneratorStream, getOrThrow(Network::getGenerator, "Generator"))
                .stringsIndex("id", Generator::getId)
                .strings("name", g -> g.getOptionalName().orElse(""), Identifiable::setName)
                .enums("energy_source", EnergySource.class, Generator::getEnergySource, Generator::setEnergySource)
                .doubles("target_p", (g, context) -> perUnitPQ(context, g.getTargetP()), (g, targetP, context) -> g.setTargetP(unPerUnitPQ(context, targetP)))
                .doubles("min_p", (g, context) -> perUnitPQ(context, g.getMinP()), (g, minP, context) -> g.setMinP(unPerUnitPQ(context, minP)))
                .doubles("max_p", (g, context) -> perUnitPQ(context, g.getMaxP()), (g, maxP, context) -> g.setMaxP(unPerUnitPQ(context, maxP)))
                .doubles("min_q", ifExistsDoublePerUnitPQ(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ),
                    setPerUnitMinQ())
                .doubles("max_q", ifExistsDoublePerUnitPQ(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ),
                    setPerUnitMaxQ())
                .doubles("min_q_at_target_p", getPerUnitMinQ(Generator::getTargetP), false)
                .doubles("max_q_at_target_p", getPerUnitMaxQ(Generator::getTargetP), false)
                .doubles("min_q_at_p", getPerUnitMinQ(getOppositeP()), false)
                .doubles("max_q_at_p", getPerUnitMaxQ(getOppositeP()), false)
                .doubles("rated_s", (g, context) -> g.getRatedS(), (g, ratedS, context) -> g.setRatedS(ratedS))
                .strings("reactive_limits_kind", NetworkDataframes::getReactiveLimitsKind)
                .doubles("target_v", (g, context) -> perUnitTargetV(context, g.getTargetV(), g.getRegulatingTerminal(), g.getTerminal()),
                    (g, v, context) -> g.setTargetV(unPerUnitTargetV(context, v, g.getRegulatingTerminal(), g.getTerminal())))
                .doubles("target_q", (g, context) -> perUnitPQ(context, g.getTargetQ()), (g, q, context) -> g.setTargetQ(unPerUnitPQ(context, q)))
                .booleans("voltage_regulator_on", Generator::isVoltageRegulatorOn, Generator::setVoltageRegulatorOn)
                .strings("regulated_element_id", generator -> NetworkUtil.getRegulatedElementId(generator::getRegulatingTerminal),
                        (generator, elementId) -> NetworkUtil.setRegulatingTerminal(generator::setRegulatingTerminal, generator.getNetwork(), elementId))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (g, context) -> perUnitI(context, g.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", g -> getBusId(g.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", g -> getNode(g.getTerminal()), false)
                .booleans("connected", g -> g.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper subNetworks() {
        return NetworkDataframeMapperBuilder.ofStream(n -> n.getSubnetworks().stream(),
                        getOrThrow(Network::getSubnetwork, "SubNetwork"))
                .stringsIndex("id", Identifiable::getId)
                .build();
    }

    private static NetworkDataframeMapper areas() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getAreaStream,
                        getOrThrow(Network::getArea, "Area"))
                .stringsIndex("id", Identifiable::getId)
                .strings("name", a -> a.getOptionalName().orElse(""), Identifiable::setName)
                .strings("area_type", Area::getAreaType)
                .doubles("interchange_target", (a, context) -> perUnitPQ(context, a.getInterchangeTarget().orElse(Double.NaN)), (a, p, context) -> a.setInterchangeTarget(unPerUnitPQ(context, p)))
                .doubles("interchange", (a, context) -> perUnitPQ(context, a.getInterchange()))
                .doubles("ac_interchange", (a, context) -> perUnitPQ(context, a.getAcInterchange()))
                .doubles("dc_interchange", (a, context) -> perUnitPQ(context, a.getDcInterchange()))
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper buses(boolean busBreakerView) {
        var builder = NetworkDataframeMapperBuilder.ofStream(n -> busBreakerView ? n.getBusBreakerView().getBusStream() : n.getBusView().getBusStream(),
                        getOrThrow((b, id) -> b.getBusView().getBus(id), "Bus"))
                .stringsIndex("id", Bus::getId)
                .strings("name", b -> b.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("v_mag", (b, context) -> perUnitV(context, b.getV(), b),
                    (b, v, context) -> b.setV(unPerUnitV(context, v, b)))
                .doubles("v_angle", (b, context) -> perUnitAngle(context, b.getAngle()), (b, vAngle, context) -> b.setAngle(unPerUnitAngle(context, vAngle)))
                .ints("connected_component", ifExistsInt(Bus::getConnectedComponent, Component::getNum))
                .ints("synchronous_component", ifExistsInt(Bus::getSynchronousComponent, Component::getNum))
                .strings("voltage_level_id", b -> b.getVoltageLevel().getId());
        if (busBreakerView) {
            builder.strings("bus_id", NetworkDataframes::getBusViewBusId);
        }
        return builder.booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    /**
     * @param b bus in Bus/Breaker view
     * @return ID of the bus in bus view containing b, or empty string if none.
     */
    private static String getBusViewBusId(Bus b) {
        // First we try the fast and easy way using connected terminals. Works for the vast majority of buses.
        String busIdInBusView = b.getConnectedTerminalStream().map(t -> t.getBusView().getBus())
                .filter(Objects::nonNull)
                .map(Identifiable::getId)
                .findFirst()
                .orElse(null);
        if (busIdInBusView != null) {
            return busIdInBusView;
        }
        // Didn't find using connected terminals. There is the possibility that the bus has zero connected terminal
        // on its own but is still part of a Merged Bus via a closed retained switch. We examine this case below.
        VoltageLevel voltageLevel = b.getVoltageLevel();
        if (voltageLevel.getTopologyKind() == TopologyKind.BUS_BREAKER) {
            // Bus/Breaker. There is an easy method directly available.
            Bus busInBusView = voltageLevel.getBusView().getMergedBus(b.getId());
            if (busInBusView != null) {
                return busInBusView.getId();
            } else {
                return "";
            }
        } else {
            // Node/Breaker. We should probably build something more efficient on powsybl-core side to avoid having
            // to loop over all buses in the voltage level.
            for (Bus bus : voltageLevel.getBusView().getBuses()) {
                boolean found = voltageLevel.getBusBreakerView().getBusStreamFromBusViewBusId(bus.getId())
                        .anyMatch(b2 -> b.getId().equals(b2.getId()));
                if (found) {
                    return bus.getId();
                }
            }
            return "";
        }
    }

    static NetworkDataframeMapper loads() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getLoadStream, getOrThrow(Network::getLoad, "Load"))
                .stringsIndex("id", Load::getId)
                .strings("name", l -> l.getOptionalName().orElse(""), Identifiable::setName)
                .enums("type", LoadType.class, Load::getLoadType)
                .doubles("p0", (l, context) -> perUnitPQ(context, l.getP0()), (l, p, context) -> l.setP0(unPerUnitPQ(context, p)))
                .doubles("q0", (l, context) -> perUnitPQ(context, l.getQ0()), (l, q, context) -> l.setQ0(unPerUnitPQ(context, q)))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (l, context) -> perUnitI(context, l.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", l -> getBusId(l.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", l -> getNode(l.getTerminal()), false)
                .booleans("connected", l -> l.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper batteries() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getBatteryStream, getOrThrow(Network::getBattery, "Battery"))
                .stringsIndex("id", Battery::getId)
                .strings("name", b -> b.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("max_p", (b, context) -> perUnitPQ(context, b.getMaxP()), (b, maxP, context) -> b.setMaxP(unPerUnitPQ(context, maxP)))
                .doubles("min_p", (b, context) -> perUnitPQ(context, b.getMinP()), (b, minP, context) -> b.setMinP(unPerUnitPQ(context, minP)))
                .doubles("min_q", ifExistsDoublePerUnitPQ(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ),
                    setPerUnitMinQ())
                .doubles("max_q", ifExistsDoublePerUnitPQ(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ),
                    setPerUnitMaxQ())
                .strings("reactive_limits_kind", NetworkDataframes::getReactiveLimitsKind)
                .doubles("target_p", (b, context) -> perUnitPQ(context, b.getTargetP()), (b, targetP, context) -> b.setTargetP(unPerUnitPQ(context, targetP)))
                .doubles("target_q", (b, context) -> perUnitPQ(context, b.getTargetQ()), (b, targetQ, context) -> b.setTargetQ(unPerUnitPQ(context, targetQ)))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (b, context) -> perUnitI(context, b.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", b -> getBusId(b.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", b -> getNode(b.getTerminal()), false)
                .booleans("connected", b -> b.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper shunts() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getShuntCompensatorStream, getOrThrow(Network::getShuntCompensator, "Shunt compensator"))
                .stringsIndex("id", ShuntCompensator::getId)
                .strings("name", sc -> sc.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("g", (shunt, context) -> perUnitG(context, shunt))
                .doubles("b", (shunt, context) -> perUnitB(context, shunt))
                .enums("model_type", ShuntCompensatorModelType.class, ShuntCompensator::getModelType)
                .ints("max_section_count", ShuntCompensator::getMaximumSectionCount)
                .ints("section_count", ShuntCompensator::getSectionCount, ShuntCompensator::setSectionCount)
                .booleans("voltage_regulation_on", ShuntCompensator::isVoltageRegulatorOn, ShuntCompensator::setVoltageRegulatorOn)
                .doubles("target_v", (sc, context) -> perUnitTargetV(context, sc.getTargetV(), sc.getRegulatingTerminal(), sc.getTerminal()),
                    (sc, v, context) -> sc.setTargetV(unPerUnitTargetV(context, v, sc.getRegulatingTerminal(), sc.getTerminal())))
                .doubles("target_deadband", (sc, context) -> perUnitV(context, sc.getTargetDeadband(), sc.getRegulatingTerminal()),
                    (sc, tb, context) -> sc.setTargetDeadband(unPerUnitV(context, tb, sc.getRegulatingTerminal())))
                .strings("regulating_bus_id", sc -> getBusId(sc.getRegulatingTerminal()))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (sc, context) -> perUnitI(context, sc.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", sc -> getBusId(sc.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", sc -> getNode(sc.getTerminal()), false)
                .booleans("connected", sc -> sc.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper shuntsNonLinear() {
        Function<Network, Stream<Triple<ShuntCompensator, ShuntCompensatorNonLinearModel.Section, Integer>>> nonLinearShunts = network ->
                network.getShuntCompensatorStream()
                        .filter(sc -> sc.getModelType() == ShuntCompensatorModelType.NON_LINEAR)
                        .flatMap(shuntCompensator -> {
                            ShuntCompensatorNonLinearModel model = (ShuntCompensatorNonLinearModel) shuntCompensator.getModel();
                            return model.getAllSections().stream().map(section -> Triple.of(shuntCompensator, section, model.getAllSections().indexOf(section)));
                        });
        return NetworkDataframeMapperBuilder.ofStream(nonLinearShunts, NetworkDataframes::getShuntSectionNonlinear)
                .stringsIndex("id", triple -> triple.getLeft().getId())
                .intsIndex("section", Triple::getRight)
                .doubles("g", (p, context) -> perUnitG(context, p.getMiddle(), p.getLeft()),
                    (p, g, context) -> p.getMiddle().setG(unPerUnitBG(context, p.getLeft(), g)))
                .doubles("b", (p, context) -> perUnitB(context, p.getMiddle(), p.getLeft()),
                    (p, b, context) -> p.getMiddle().setB(unPerUnitBG(context, p.getLeft(), b)))
                .build();
    }

    static Triple<ShuntCompensator, ShuntCompensatorNonLinearModel.Section, Integer> getShuntSectionNonlinear(Network network, UpdatingDataframe dataframe, int index) {
        ShuntCompensator shuntCompensator = network.getShuntCompensator(dataframe.getStringValue("id", index)
                .orElseThrow(() -> new PowsyblException("id is missing")));
        if (!(shuntCompensator.getModel() instanceof ShuntCompensatorNonLinearModel shuntNonLinear)) {
            throw new PowsyblException("shunt with id " + shuntCompensator.getId() + "has not a non linear model");
        } else {
            int section = dataframe.getIntValue("section", index)
                    .orElseThrow(() -> new PowsyblException("section is missing"));
            return Triple.of(shuntCompensator, shuntNonLinear.getAllSections().get(section), section);
        }
    }

    static NetworkDataframeMapper linearShuntsSections() {
        Function<Network, Stream<Pair<ShuntCompensator, ShuntCompensatorLinearModel>>> linearShunts = network ->
                network.getShuntCompensatorStream()
                        .filter(sc -> sc.getModelType() == ShuntCompensatorModelType.LINEAR)
                        .map(shuntCompensator -> Pair.of(shuntCompensator, (ShuntCompensatorLinearModel) shuntCompensator.getModel()));
        return NetworkDataframeMapperBuilder.ofStream(linearShunts, (net, s) -> Pair.of(checkShuntNonNull(net, s), checkLinearModel(net, s)))
                .stringsIndex("id", p -> p.getLeft().getId())
                .doubles("g_per_section", (p, context) -> perUnitBG(context, p.getRight().getGPerSection(), p.getLeft().getTerminal().getVoltageLevel().getNominalV()),
                    (p, g, context) -> p.getRight().setGPerSection(unPerUnitBG(context, g, p.getLeft().getTerminal().getVoltageLevel().getNominalV())))
                .doubles("b_per_section", (p, context) -> perUnitBG(context, p.getRight().getBPerSection(), p.getLeft().getTerminal().getVoltageLevel().getNominalV()),
                    (p, b, context) -> p.getRight().setBPerSection(unPerUnitBG(context, b, p.getLeft().getTerminal().getVoltageLevel().getNominalV())))
                .ints("max_section_count", p -> p.getLeft().getMaximumSectionCount(), (p, s) -> p.getRight().setMaximumSectionCount(s))
                .build();
    }

    private static ShuntCompensator checkShuntNonNull(Network network, String id) {
        ShuntCompensator shuntCompensator = network.getShuntCompensator(id);
        if (shuntCompensator == null) {
            throw new PowsyblException("ShuntCompensator '" + id + "' not found");
        }
        return shuntCompensator;
    }

    private static ShuntCompensatorLinearModel checkLinearModel(Network network, String id) {
        ShuntCompensator shuntCompensator = network.getShuntCompensator(id);
        if (shuntCompensator.getModelType() != ShuntCompensatorModelType.LINEAR) {
            throw new PowsyblException("ShuntCompensator '" + id + "' is not linear");
        }
        return (ShuntCompensatorLinearModel) shuntCompensator.getModel();
    }

    static NetworkDataframeMapper lines() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getLineStream, getOrThrow(Network::getLine, "Line"))
                .stringsIndex("id", Line::getId)
                .strings("name", l -> l.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("r", (line, context) -> perUnitR(context, line),
                    (line, r, context) -> line.setR(unPerUnitRX(context, line, r)))
                .doubles("x", (line, context) -> PerUnitUtil.perUnitX(context, line),
                    (line, x, context) -> line.setX(unPerUnitRX(context, line, x)))
                .doubles("g1", (line, context) -> perUnitGSide1(context, line),
                    (line, g, context) -> line.setG1(unPerUnitGSide1(context, line, g)))
                .doubles("b1", (line, context) -> perUnitBSide1(context, line),
                    (line, b, context) -> line.setB1(unPerUnitBSide1(context, line, b)))
                .doubles("g2", (line, context) -> perUnitGSide2(context, line),
                    (line, g, context) -> line.setG2(unPerUnitGSide2(context, line, g)))
                .doubles("b2", (line, context) -> perUnitBSide2(context, line),
                    (line, b, context) -> line.setB2(unPerUnitBSide2(context, line, b)))
                .doubles("p1", getPerUnitP1(), setPerUnitP1())
                .doubles("q1", getPerUnitQ1(), setPerUnitQ1())
                .doubles("i1", (line, context) -> perUnitI(context, line.getTerminal1()))
                .doubles("p2", getPerUnitP2(), setPerUnitP2())
                .doubles("q2", getPerUnitQ2(), setPerUnitQ2())
                .doubles("i2", (line, context) -> perUnitI(context, line.getTerminal2()))
                .strings("voltage_level1_id", l -> l.getTerminal1().getVoltageLevel().getId())
                .strings("voltage_level2_id", l -> l.getTerminal2().getVoltageLevel().getId())
                .strings("bus1_id", l -> getBusId(l.getTerminal1()))
                .strings("bus_breaker_bus1_id", l -> getBusBreakerViewBusId(l.getTerminal1()), (l, id) -> setBusBreakerViewBusId(l.getTerminal1(), id), false)
                .ints("node1", l -> getNode(l.getTerminal1()), false)
                .strings("bus2_id", l -> getBusId(l.getTerminal2()))
                .strings("bus_breaker_bus2_id", l -> getBusBreakerViewBusId(l.getTerminal2()), (l, id) -> setBusBreakerViewBusId(l.getTerminal2(), id), false)
                .ints("node2", l -> getNode(l.getTerminal2()), false)
                .booleans("connected1", l -> l.getTerminal1().isConnected(), connectBranchSide1())
                .booleans("connected2", l -> l.getTerminal2().isConnected(), connectBranchSide2())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .strings("selected_limits_group_1", line -> line.getSelectedOperationalLimitsGroupId1().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        Line::setSelectedOperationalLimitsGroup1, false)
                .strings("selected_limits_group_2", line -> line.getSelectedOperationalLimitsGroupId2().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        Line::setSelectedOperationalLimitsGroup2, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper twoWindingTransformers() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getTwoWindingsTransformerStream, getOrThrow(Network::getTwoWindingsTransformer, "Two windings transformer"))
                .stringsIndex("id", TwoWindingsTransformer::getId)
                .strings("name", twt -> twt.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("r", (twt, context) -> perUnitRX(context, twt.getR(), twt), (twt, r, context) -> twt.setR(unPerUnitRX(context, twt, r)))
                .doubles("x", (twt, context) -> perUnitRX(context, twt.getX(), twt), (twt, x, context) -> twt.setX(unPerUnitRX(context, twt, x)))
                .doubles("g", (twt, context) -> perUnitBG(context, twt, twt.getG()), (twt, g, context) -> twt.setG(unPerUnitBG(context, twt, g)))
                .doubles("b", (twt, context) -> perUnitBG(context, twt, twt.getB()), (twt, b, context) -> twt.setB(unPerUnitBG(context, twt, b)))
                .doubles("rated_u1", (twt, context) -> perUnitV(context, twt.getRatedU1(), twt.getTerminal1()),
                    (twt, ratedV1, context) -> twt.setRatedU1(unPerUnitV(context, ratedV1, twt.getTerminal1())))
                .doubles("rated_u2", (twt, context) -> perUnitV(context, twt.getRatedU2(), twt.getTerminal2()),
                    (twt, ratedV2, context) -> twt.setRatedU2(unPerUnitV(context, ratedV2, twt.getTerminal2())))
                .doubles("rated_s", (twt, context) -> twt.getRatedS(), (twt, ratedS, context) -> twt.setRatedS(ratedS))
                .doubles("p1", getPerUnitP1(), setPerUnitP1())
                .doubles("q1", getPerUnitQ1(), setPerUnitQ1())
                .doubles("i1", (twt, context) -> perUnitI(context, twt.getTerminal1()))
                .doubles("p2", getPerUnitP2(), setPerUnitP2())
                .doubles("q2", getPerUnitQ2(), setPerUnitQ2())
                .doubles("i2", (twt, context) -> perUnitI(context, twt.getTerminal2()))
                .strings("voltage_level1_id", twt -> twt.getTerminal1().getVoltageLevel().getId())
                .strings("voltage_level2_id", twt -> twt.getTerminal2().getVoltageLevel().getId())
                .strings("bus1_id", twt -> getBusId(twt.getTerminal1()))
                .strings("bus_breaker_bus1_id", twt -> getBusBreakerViewBusId(twt.getTerminal1()), (twt, id) -> setBusBreakerViewBusId(twt.getTerminal1(), id), false)
                .ints("node1", twt -> getNode(twt.getTerminal1()), false)
                .strings("bus2_id", twt -> getBusId(twt.getTerminal2()))
                .strings("bus_breaker_bus2_id", twt -> getBusBreakerViewBusId(twt.getTerminal2()), (twt, id) -> setBusBreakerViewBusId(twt.getTerminal2(), id), false)
                .ints("node2", twt -> getNode(twt.getTerminal2()), false)
                .booleans("connected1", twt -> twt.getTerminal1().isConnected(), connectBranchSide1())
                .booleans("connected2", twt -> twt.getTerminal2().isConnected(), connectBranchSide2())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .strings("selected_limits_group_1", twt -> twt.getSelectedOperationalLimitsGroupId1().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        TwoWindingsTransformer::setSelectedOperationalLimitsGroup1, false)
                .strings("selected_limits_group_2", twt -> twt.getSelectedOperationalLimitsGroupId2().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        TwoWindingsTransformer::setSelectedOperationalLimitsGroup2, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper threeWindingTransformers() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getThreeWindingsTransformerStream, getOrThrow(Network::getThreeWindingsTransformer, "Three windings transformer"))
                .stringsIndex("id", ThreeWindingsTransformer::getId)
                .strings("name", twt -> twt.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("rated_u0", (twt, context) -> context.isPerUnit() ? 1 : twt.getRatedU0())
                .doubles("r1", (twt, context) -> perUnitRX(context, twt.getLeg1().getR(), twt), (twt, r1, context) -> twt.getLeg1().setR(unPerUnitRX(context, twt, r1)))
                .doubles("x1", (twt, context) -> perUnitRX(context, twt.getLeg1().getX(), twt), (twt, x1, context) -> twt.getLeg1().setX(unPerUnitRX(context, twt, x1)))
                .doubles("g1", (twt, context) -> perUnitBG(context, twt.getLeg1().getG(), twt), (twt, g1, context) -> twt.getLeg1().setG(unPerUnitBG(context, twt, g1)))
                .doubles("b1", (twt, context) -> perUnitBG(context, twt.getLeg1().getB(), twt), (twt, b1, context) -> twt.getLeg1().setB(unPerUnitBG(context, twt, b1)))
                .doubles("rated_u1", (twt, context) -> perUnitV(context, twt.getLeg1()), (twt, ratedU1, context) -> twt.getLeg1().setRatedU(unPerUnitV(context, ratedU1, twt.getLeg1())))
                .doubles("rated_s1", (twt, context) -> twt.getLeg1().getRatedS(), (twt, ratedS1, context) -> twt.getLeg1().setRatedS(ratedS1))
                .ints("ratio_tap_position1", getRatioTapPosition(ThreeWindingsTransformer::getLeg1), (t, v) -> setTapPosition(t.getLeg1().getRatioTapChanger(), v))
                .ints("phase_tap_position1", getPhaseTapPosition(ThreeWindingsTransformer::getLeg1), (t, v) -> setTapPosition(t.getLeg1().getPhaseTapChanger(), v))
                .doubles("p1", (twt, context) -> perUnitP(context, twt.getLeg1()), (twt, p1, context) -> twt.getLeg1().getTerminal().setP(unPerUnitPQ(context, p1)))
                .doubles("q1", (twt, context) -> perUnitQ(context, twt.getLeg1()), (twt, q1, context) -> twt.getLeg1().getTerminal().setQ(unPerUnitPQ(context, q1)))
                .doubles("i1", (twt, context) -> perUnitI(context, twt.getLeg1().getTerminal()))
                .strings("voltage_level1_id", twt -> twt.getLeg1().getTerminal().getVoltageLevel().getId())
                .strings("bus1_id", twt -> getBusId(twt.getLeg1().getTerminal()))
                .strings("bus_breaker_bus1_id", twt -> getBusBreakerViewBusId(twt.getLeg1().getTerminal()), (twt, id) -> setBusBreakerViewBusId(twt.getLeg1().getTerminal(), id), false)
                .ints("node1", twt -> getNode(twt.getLeg1().getTerminal()), false)
                .booleans("connected1", g -> g.getLeg1().getTerminal().isConnected(), connectLeg1())
                .strings("selected_limits_group_1", twt -> twt.getLeg1().getSelectedOperationalLimitsGroupId().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        (twt, groupId) -> twt.getLeg1().setSelectedOperationalLimitsGroup(groupId), false)
                .doubles("r2", (twt, context) -> perUnitRX(context, twt.getLeg2().getR(), twt), (twt, r2, context) -> twt.getLeg2().setR(unPerUnitRX(context, twt, r2)))
                .doubles("x2", (twt, context) -> perUnitRX(context, twt.getLeg2().getX(), twt), (twt, x2, context) -> twt.getLeg2().setX(unPerUnitRX(context, twt, x2)))
                .doubles("g2", (twt, context) -> perUnitBG(context, twt.getLeg2().getG(), twt), (twt, g2, context) -> twt.getLeg2().setG(unPerUnitBG(context, twt, g2)))
                .doubles("b2", (twt, context) -> perUnitBG(context, twt.getLeg2().getB(), twt), (twt, b2, context) -> twt.getLeg2().setB(unPerUnitBG(context, twt, b2)))
                .doubles("rated_u2", (twt, context) -> perUnitV(context, twt.getLeg2()), (twt, ratedU2, context) -> twt.getLeg2().setRatedU(unPerUnitV(context, ratedU2, twt.getLeg2())))
                .doubles("rated_s2", (twt, context) -> twt.getLeg2().getRatedS(), (twt, v, context) -> twt.getLeg2().setRatedS(v))
                .ints("ratio_tap_position2", getRatioTapPosition(ThreeWindingsTransformer::getLeg2), (t, v) -> setTapPosition(t.getLeg2().getRatioTapChanger(), v))
                .ints("phase_tap_position2", getPhaseTapPosition(ThreeWindingsTransformer::getLeg2), (t, v) -> setTapPosition(t.getLeg2().getPhaseTapChanger(), v))
                .doubles("p2", (twt, context) -> perUnitP(context, twt.getLeg2()), (twt, p2, context) -> twt.getLeg2().getTerminal().setP(unPerUnitPQ(context, p2)))
                .doubles("q2", (twt, context) -> perUnitQ(context, twt.getLeg2()), (twt, q2, context) -> twt.getLeg2().getTerminal().setQ(unPerUnitPQ(context, q2)))
                .doubles("i2", (twt, context) -> perUnitI(context, twt.getLeg2().getTerminal()))
                .strings("voltage_level2_id", twt -> twt.getLeg2().getTerminal().getVoltageLevel().getId())
                .strings("bus2_id", twt -> getBusId(twt.getLeg2().getTerminal()))
                .strings("bus_breaker_bus2_id", twt -> getBusBreakerViewBusId(twt.getLeg2().getTerminal()), (twt, id) -> setBusBreakerViewBusId(twt.getLeg2().getTerminal(), id), false)
                .ints("node2", twt -> getNode(twt.getLeg2().getTerminal()), false)
                .booleans("connected2", g -> g.getLeg2().getTerminal().isConnected(), connectLeg2())
                .strings("selected_limits_group_2", twt -> twt.getLeg2().getSelectedOperationalLimitsGroupId().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        (twt, groupId) -> twt.getLeg2().setSelectedOperationalLimitsGroup(groupId), false)
                .doubles("r3", (twt, context) -> perUnitRX(context, twt.getLeg3().getR(), twt), (twt, r3, context) -> twt.getLeg3().setR(unPerUnitRX(context, twt, r3)))
                .doubles("x3", (twt, context) -> perUnitRX(context, twt.getLeg3().getX(), twt), (twt, x3, context) -> twt.getLeg3().setX(unPerUnitRX(context, twt, x3)))
                .doubles("g3", (twt, context) -> perUnitBG(context, twt.getLeg3().getG(), twt), (twt, g3, context) -> twt.getLeg3().setG(unPerUnitBG(context, twt, g3)))
                .doubles("b3", (twt, context) -> perUnitBG(context, twt.getLeg3().getB(), twt), (twt, b3, context) -> twt.getLeg3().setB(unPerUnitBG(context, twt, b3)))
                .doubles("rated_u3", (twt, context) -> perUnitV(context, twt.getLeg3()), (twt, ratedU3, context) -> twt.getLeg3().setRatedU(unPerUnitV(context, ratedU3, twt.getLeg3())))
                .doubles("rated_s3", (twt, context) -> twt.getLeg3().getRatedS(), (twt, v, context) -> twt.getLeg3().setRatedS(v))
                .ints("ratio_tap_position3", getRatioTapPosition(ThreeWindingsTransformer::getLeg3), (t, v) -> setTapPosition(t.getLeg3().getRatioTapChanger(), v))
                .ints("phase_tap_position3", getPhaseTapPosition(ThreeWindingsTransformer::getLeg3), (t, v) -> setTapPosition(t.getLeg3().getPhaseTapChanger(), v))
                .doubles("p3", (twt, context) -> perUnitP(context, twt.getLeg3()), (twt, p3, context) -> twt.getLeg3().getTerminal().setP(unPerUnitPQ(context, p3)))
                .doubles("q3", (twt, context) -> perUnitQ(context, twt.getLeg3()), (twt, q3, context) -> twt.getLeg3().getTerminal().setQ(unPerUnitPQ(context, q3)))
                .doubles("i3", (twt, context) -> perUnitI(context, twt.getLeg3().getTerminal()))
                .strings("voltage_level3_id", twt -> twt.getLeg3().getTerminal().getVoltageLevel().getId())
                .strings("bus3_id", twt -> getBusId(twt.getLeg3().getTerminal()))
                .strings("bus_breaker_bus3_id", twt -> getBusBreakerViewBusId(twt.getLeg3().getTerminal()), (twt, id) -> setBusBreakerViewBusId(twt.getLeg3().getTerminal(), id), false)
                .ints("node3", twt -> getNode(twt.getLeg3().getTerminal()), false)
                .booleans("connected3", twt -> twt.getLeg3().getTerminal().isConnected(), connectLeg3())
                .strings("selected_limits_group_3", twt -> twt.getLeg3().getSelectedOperationalLimitsGroupId().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        (twt, groupId) -> twt.getLeg3().setSelectedOperationalLimitsGroup(groupId), false)
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper danglingLines() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getDanglingLineStream(), getOrThrow(Network::getDanglingLine, "Dangling line"))
                .stringsIndex("id", DanglingLine::getId)
                .strings("name", dl -> dl.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("r", (dl, context) -> perUnitRX(context, dl.getR(), dl.getTerminal()), (dl, r, context) -> dl.setR(unPerUnitRX(context, dl.getTerminal(), r)))
                .doubles("x", (dl, context) -> perUnitRX(context, dl.getX(), dl.getTerminal()), (dl, x, context) -> dl.setX(unPerUnitRX(context, dl.getTerminal(), x)))
                .doubles("g", (dl, context) -> perUnitG(context, dl), (dl, g, context) -> dl.setG(unPerUnitG(context, dl, g)))
                .doubles("b", (dl, context) -> perUnitB(context, dl), (dl, b, context) -> dl.setB(unPerUnitB(context, dl, b)))
                .doubles("p0", (dl, context) -> perUnitPQ(context, dl.getP0()), (dl, p0, context) -> dl.setP0(unPerUnitPQ(context, p0)))
                .doubles("q0", (dl, context) -> perUnitPQ(context, dl.getQ0()), (dl, q0, context) -> dl.setQ0(unPerUnitPQ(context, q0)))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (dl, context) -> perUnitI(context, dl.getTerminal()))
                .doubles("boundary_p", (dl, context) -> perUnitPQ(context, dl.getBoundary().getP()), false)
                .doubles("boundary_q", (dl, context) -> perUnitPQ(context, dl.getBoundary().getQ()), false)
                .doubles("boundary_v_mag", (dl, context) -> perUnitV(context, dl.getBoundary().getV(), dl.getTerminal()), false)
                .doubles("boundary_v_angle", (dl, context) -> perUnitAngle(context, dl.getBoundary().getAngle()), false)
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", dl -> getBusId(dl.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", dl -> getNode(dl.getTerminal()), false)
                .booleans("connected", dl -> dl.getTerminal().isConnected(), connectInjection())
                .strings("pairing_key", dl -> Objects.toString(dl.getPairingKey(), ""), DanglingLine::setPairingKey)
                .strings("ucte_xnode_code", dl -> Objects.toString(dl.getPairingKey(), ""))
                .booleans("paired", DanglingLine::isPaired)
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .strings("tie_line_id", dl -> dl.getTieLine().map(Identifiable::getId).orElse(""))
                .strings("selected_limits_group", dl -> dl.getSelectedOperationalLimitsGroupId().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        DanglingLine::setSelectedOperationalLimitsGroup, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper tieLines() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getTieLineStream, getOrThrow(Network::getTieLine, "Tie line"))
                .stringsIndex("id", TieLine::getId)
                .strings("name", tl -> tl.getOptionalName().orElse(""), Identifiable::setName)
                .strings("dangling_line1_id", tl -> tl.getDanglingLine1().getId())
                .strings("dangling_line2_id", tl -> tl.getDanglingLine2().getId())
                .strings("pairing_key", tl -> Objects.toString(tl.getPairingKey(), ""))
                .strings("ucte_xnode_code", tl -> Objects.toString(tl.getPairingKey(), ""))
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper lccs() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getLccConverterStationStream, getOrThrow(Network::getLccConverterStation, "LCC converter station"))
                .stringsIndex("id", LccConverterStation::getId)
                .strings("name", st -> st.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("power_factor", (st, context) -> st.getPowerFactor(), (lcc, v, context) -> lcc.setPowerFactor((float) v))
                .doubles("loss_factor", (st, context) -> st.getLossFactor(), (lcc, v, context) -> lcc.setLossFactor((float) v))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (st, context) -> perUnitI(context, st.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", st -> getBusId(st.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", st -> getNode(st.getTerminal()), false)
                .booleans("connected", st -> st.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper vscs() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getVscConverterStationStream, getOrThrow(Network::getVscConverterStation, "VSC converter station"))
                .stringsIndex("id", VscConverterStation::getId)
                .strings("name", st -> st.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("loss_factor", (vsc, context) -> vsc.getLossFactor(), (vscConverterStation, lf, context) -> vscConverterStation.setLossFactor((float) lf))
                .doubles("min_q", ifExistsDoublePerUnitPQ(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ), setPerUnitMinQ())
                .doubles("max_q", ifExistsDoublePerUnitPQ(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ), setPerUnitMaxQ())
                .doubles("min_q_at_p", getPerUnitMinQ(getOppositeP()), false)
                .doubles("max_q_at_p", getPerUnitMaxQ(getOppositeP()), false)
                .strings("reactive_limits_kind", NetworkDataframes::getReactiveLimitsKind)
                .doubles("target_v", (vsc, context) -> perUnitTargetV(context, vsc.getVoltageSetpoint(), vsc.getRegulatingTerminal(), vsc.getTerminal()),
                    (vsc, targetV, context) -> vsc.setVoltageSetpoint(unPerUnitTargetV(context, targetV, vsc.getRegulatingTerminal(), vsc.getTerminal())))
                .doubles("target_q", (vsc, context) -> perUnitPQ(context, vsc.getReactivePowerSetpoint()),
                    (vsc, targetQ, context) -> vsc.setReactivePowerSetpoint(unPerUnitPQ(context, targetQ)))
                .booleans("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn, VscConverterStation::setVoltageRegulatorOn)
                .strings("regulated_element_id", vsc -> NetworkUtil.getRegulatedElementId(vsc::getRegulatingTerminal),
                        (vsc, elementId) -> NetworkUtil.setRegulatingTerminal(vsc::setRegulatingTerminal, vsc.getNetwork(), elementId))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (st, context) -> perUnitI(context, st.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", st -> getBusId(st.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", st -> getNode(st.getTerminal()), false)
                .booleans("connected", st -> st.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper svcs() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getStaticVarCompensatorStream, getOrThrow(Network::getStaticVarCompensator, "Static var compensator"))
                .stringsIndex("id", StaticVarCompensator::getId)
                .strings("name", svc -> svc.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("b_min", (svc, context) -> svc.getBmin(), (svc, bMin, context) -> svc.setBmin(bMin))
                .doubles("b_max", (svc, context) -> svc.getBmax(), (svc, bMax, context) -> svc.setBmax(bMax))
                .doubles("target_v", (svc, context) -> perUnitTargetV(context, svc.getVoltageSetpoint(), svc.getRegulatingTerminal(), svc.getTerminal()),
                    (svc, targetV, context) -> svc.setVoltageSetpoint(unPerUnitTargetV(context, targetV, svc.getRegulatingTerminal(), svc.getTerminal())))
                .doubles("target_q", (svc, context) -> perUnitPQ(context, svc.getReactivePowerSetpoint()),
                    (svc, targetQ, context) -> svc.setReactivePowerSetpoint(unPerUnitPQ(context, targetQ)))
                .enums("regulation_mode", StaticVarCompensator.RegulationMode.class,
                        StaticVarCompensator::getRegulationMode, StaticVarCompensator::setRegulationMode)
                .strings("regulated_element_id", svc -> NetworkUtil.getRegulatedElementId(svc::getRegulatingTerminal),
                        (svc, elementId) -> NetworkUtil.setRegulatingTerminal(svc::setRegulatingTerminal, svc.getNetwork(), elementId))
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (st, context) -> perUnitI(context, st.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", svc -> getBusId(svc.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", svc -> getNode(svc.getTerminal()), false)
                .booleans("connected", svc -> svc.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static String getBusBreakerBus1Id(Switch s) {
        VoltageLevel vl = s.getVoltageLevel();
        if (!s.isRetained()) {
            return "";
        }
        Bus bus = vl.getBusBreakerView().getBus1(s.getId());
        return bus != null ? bus.getId() : "";
    }

    private static String getBusBreakerBus2Id(Switch s) {
        VoltageLevel vl = s.getVoltageLevel();
        if (!s.isRetained()) {
            return "";
        }
        Bus bus = vl.getBusBreakerView().getBus2(s.getId());
        return bus != null ? bus.getId() : "";
    }

    private static int getNode1(Switch s) {
        VoltageLevel vl = s.getVoltageLevel();
        if (vl.getTopologyKind() != TopologyKind.NODE_BREAKER) {
            return -1;
        }
        return vl.getNodeBreakerView().getNode1(s.getId());
    }

    private static int getNode2(Switch s) {
        VoltageLevel vl = s.getVoltageLevel();
        if (vl.getTopologyKind() != TopologyKind.NODE_BREAKER) {
            return -1;
        }
        return vl.getNodeBreakerView().getNode2(s.getId());
    }

    private static NetworkDataframeMapper switches() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getSwitchStream, getOrThrow(Network::getSwitch, "Switch"))
                .stringsIndex("id", Switch::getId)
                .strings("name", s -> s.getOptionalName().orElse(""), Identifiable::setName)
                .enums("kind", SwitchKind.class, Switch::getKind)
                .booleans("open", Switch::isOpen, Switch::setOpen)
                .booleans("retained", Switch::isRetained, Switch::setRetained)
                .strings("voltage_level_id", s -> s.getVoltageLevel().getId())
                .strings("bus_breaker_bus1_id", NetworkDataframes::getBusBreakerBus1Id, false)
                .strings("bus_breaker_bus2_id", NetworkDataframes::getBusBreakerBus2Id, false)
                .ints("node1", NetworkDataframes::getNode1, false)
                .ints("node2", NetworkDataframes::getNode2, false)
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper voltageLevels() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getVoltageLevelStream, getOrThrow(Network::getVoltageLevel, "Voltage level"))
                .stringsIndex("id", VoltageLevel::getId)
                .strings("name", vl -> vl.getOptionalName().orElse(""), Identifiable::setName)
                .strings("substation_id", vl -> vl.getSubstation().map(Identifiable::getId).orElse(""))
                .doubles("nominal_v", (vl, context) -> vl.getNominalV(), (vl, nominalV, context) -> vl.setNominalV(nominalV))
                .doubles("high_voltage_limit", (vl, context) -> perUnitV(context, vl.getHighVoltageLimit(), vl.getNominalV()),
                    (vl, hvl, context) -> vl.setHighVoltageLimit(unPerUnitV(context, hvl, vl.getNominalV())))
                .doubles("low_voltage_limit", (vl, context) -> perUnitV(context, vl.getLowVoltageLimit(), vl.getNominalV()),
                    (vl, lvl, context) -> vl.setLowVoltageLimit(unPerUnitV(context, lvl, vl.getNominalV())))
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .strings("topology_kind", vl -> vl.getTopologyKind().toString(), false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper substations() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getSubstationStream, getOrThrow(Network::getSubstation, "Substation"))
                .stringsIndex("id", Identifiable::getId)
                .strings("name", s -> s.getOptionalName().orElse(""), Identifiable::setName)
                .strings("TSO", Substation::getTso, Substation::setTso)
                .strings("geo_tags", substation -> String.join(",", substation.getGeographicalTags()))
                .enums("country", Country.class, s -> s.getCountry().orElse(null), Substation::setCountry)
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper busbarSections() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getBusbarSectionStream, getOrThrow(Network::getBusbarSection, "Bus bar section"))
                .stringsIndex("id", BusbarSection::getId)
                .strings("name", bbs -> bbs.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("v", (busbar, context) -> perUnitV(context, busbar.getV(), busbar.getTerminal()))
                .doubles("angle", (busbar, context) -> perUnitAngle(context, busbar.getAngle()))
                .strings("voltage_level_id", bbs -> bbs.getTerminal().getVoltageLevel().getId())
                .strings("bus_id", bbs -> getBusId(bbs.getTerminal()))
                .booleans("connected", bbs -> bbs.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper hvdcs() {

        return NetworkDataframeMapperBuilder.ofStream(Network::getHvdcLineStream, getOrThrow(Network::getHvdcLine, "HVDC line"))
                .stringsIndex("id", HvdcLine::getId)
                .strings("name", l -> l.getOptionalName().orElse(""), Identifiable::setName)
                .enums("converters_mode", HvdcLine.ConvertersMode.class, HvdcLine::getConvertersMode, HvdcLine::setConvertersMode)
                .doubles("target_p", (hvdc, context) -> perUnitPQ(context, hvdc.getActivePowerSetpoint()),
                    (hvdc, aps, context) -> hvdc.setActivePowerSetpoint(unPerUnitPQ(context, aps)))
                .doubles("max_p", (hvdc, context) -> perUnitPQ(context, hvdc.getMaxP()), (hvdc, maxP, context) -> hvdc.setMaxP(unPerUnitPQ(context, maxP)))
                .doubles("nominal_v", (hvdc, context) -> hvdc.getNominalV(), (hvdc, nominalV, context) -> hvdc.setNominalV(nominalV))
                .doubles("r", (hvdc, context) -> perUnitRX(context, hvdc.getR(), hvdc.getNominalV(), hvdc.getNominalV()),
                    (hvdc, r, context) -> hvdc.setR(unPerUnitRX(context, r, hvdc.getNominalV(), hvdc.getNominalV())))
                .strings("converter_station1_id", l -> l.getConverterStation1().getId())
                .strings("converter_station2_id", l -> l.getConverterStation2().getId())
                .booleans("connected1", l -> l.getConverterStation1().getTerminal().isConnected(), connectHvdcStation1())
                .booleans("connected2", l -> l.getConverterStation2().getTerminal().isConnected(), connectHvdcStation2())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper rtcSteps() {
        Function<Network, Stream<Triple<TwoWindingsTransformer, RatioTapChanger, Integer>>> ratioTapChangerSteps = network ->
                network.getTwoWindingsTransformerStream()
                        .filter(twt -> twt.getRatioTapChanger() != null)
                        .flatMap(twt -> twt.getRatioTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt, twt.getRatioTapChanger(), position)));
        return NetworkDataframeMapperBuilder.ofStream(ratioTapChangerSteps, NetworkDataframes::getRatioTapChangers)
                .stringsIndex("id", triple -> triple.getLeft().getId())
                .intsIndex("position", Triple::getRight)
                .doubles("rho", (p, context) -> p.getMiddle().getStep(p.getRight()).getRho(),
                    (p, rho, context) -> p.getMiddle().getStep(p.getRight()).setRho(rho))
                .doubles("r", (p, context) -> p.getMiddle().getStep(p.getRight()).getR(),
                    (p, r, context) -> p.getMiddle().getStep(p.getRight()).setR(r))
                .doubles("x", (p, context) -> p.getMiddle().getStep(p.getRight()).getX(),
                    (p, x, context) -> p.getMiddle().getStep(p.getRight()).setX(x))
                .doubles("g", (p, context) -> p.getMiddle().getStep(p.getRight()).getG(),
                    (p, g, context) -> p.getMiddle().getStep(p.getRight()).setG(g))
                .doubles("b", (p, context) -> p.getMiddle().getStep(p.getRight()).getB(),
                    (p, b, context) -> p.getMiddle().getStep(p.getRight()).setB(b))
                .build();
    }

    static Triple<TwoWindingsTransformer, RatioTapChanger, Integer> getRatioTapChangers(Network network, UpdatingDataframe dataframe, int index) {
        String id = dataframe.getStringValue("id", index)
                .orElseThrow(() -> new IllegalArgumentException("id column is missing"));
        int position = dataframe.getIntValue("position", index)
                .orElseThrow(() -> new IllegalArgumentException("position column is missing"));
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
        return Triple.of(twt, twt.getRatioTapChanger(), position);
    }

    private static NetworkDataframeMapper ptcSteps() {
        Function<Network, Stream<Triple<TwoWindingsTransformer, PhaseTapChanger, Integer>>> phaseTapChangerSteps = network ->
                network.getTwoWindingsTransformerStream()
                        .filter(twt -> twt.getPhaseTapChanger() != null)
                        .flatMap(twt -> twt.getPhaseTapChanger().getAllSteps().keySet()
                            .stream().map(position -> Triple.of(twt, twt.getPhaseTapChanger(), position)));
        return NetworkDataframeMapperBuilder.ofStream(phaseTapChangerSteps, NetworkDataframes::getPhaseTapChangers)
                .stringsIndex("id", triple -> triple.getLeft().getId())
                .intsIndex("position", Triple::getRight)
                .doubles("rho", (p, context) -> p.getMiddle().getStep(p.getRight()).getRho(),
                    (p, rho, context) -> p.getMiddle().getStep(p.getRight()).setRho(rho))
                .doubles("alpha", (p, context) -> perUnitAngle(context, p.getMiddle().getStep(p.getRight()).getAlpha()),
                    (p, alpha, context) -> p.getMiddle().getStep(p.getRight()).setAlpha(unPerUnitAngle(context, alpha)))
                .doubles("r", (p, context) -> p.getMiddle().getStep(p.getRight()).getR(),
                    (p, r, context) -> p.getMiddle().getStep(p.getRight()).setR(r))
                .doubles("x", (p, context) -> p.getMiddle().getStep(p.getRight()).getX(),
                    (p, x, context) -> p.getMiddle().getStep(p.getRight()).setX(x))
                .doubles("g", (p, context) -> p.getMiddle().getStep(p.getRight()).getG(),
                    (p, g, context) -> p.getMiddle().getStep(p.getRight()).setG(g))
                .doubles("b", (p, context) -> p.getMiddle().getStep(p.getRight()).getB(),
                    (p, b, context) -> p.getMiddle().getStep(p.getRight()).setB(b))
                .build();
    }

    static Triple<TwoWindingsTransformer, PhaseTapChanger, Integer> getPhaseTapChangers(Network network, UpdatingDataframe dataframe, int index) {
        String id = dataframe.getStringValue("id", index)
                .orElseThrow(() -> new IllegalArgumentException("id column is missing"));
        int position = dataframe.getIntValue("position", index)
                .orElseThrow(() -> new IllegalArgumentException("position column is missing"));
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(id);
        return Triple.of(twoWindingsTransformer, twoWindingsTransformer.getPhaseTapChanger(), position);
    }

    private static NetworkDataframeMapper rtcs() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getTwoWindingsTransformerStream()
                        .filter(t -> t.getRatioTapChanger() != null), NetworkDataframes::getT2OrThrow)
                .stringsIndex("id", TwoWindingsTransformer::getId)
                .ints("tap", t -> t.getRatioTapChanger().getTapPosition(), (t, p) -> t.getRatioTapChanger().setTapPosition(p))
                .ints("low_tap", t -> t.getRatioTapChanger().getLowTapPosition())
                .ints("high_tap", t -> t.getRatioTapChanger().getHighTapPosition())
                .ints("step_count", t -> t.getRatioTapChanger().getStepCount())
                .booleans("on_load", t -> t.getRatioTapChanger().hasLoadTapChangingCapabilities(), (t, v) -> t.getRatioTapChanger().setLoadTapChangingCapabilities(v))
                .booleans("regulating", t -> t.getRatioTapChanger().isRegulating(), (t, v) -> t.getRatioTapChanger().setRegulating(v))
                .doubles("target_v", (t, context) -> perUnitTargetV(context, t.getRatioTapChanger().getTargetV(), t.getRatioTapChanger().getRegulationTerminal(), t.getTerminal2()),
                    (t, v, context) -> t.getRatioTapChanger().setTargetV(unPerUnitTargetV(context, v, t.getRatioTapChanger().getRegulationTerminal(), t.getTerminal2())))
                .doubles("target_deadband", (t, context) -> t.getRatioTapChanger().getTargetDeadband(),
                    (t, v, context) -> t.getRatioTapChanger().setTargetDeadband(v))
                .strings("regulating_bus_id", t -> getBusId(t.getRatioTapChanger().getRegulationTerminal()))
                .doubles("rho", (twt, context) -> perUnitRho(context, twt, NetworkDataframes.computeRho(twt)))
                .doubles("alpha", ifExistsDoublePerUnitAngle(TwoWindingsTransformer::getPhaseTapChanger, pc -> pc.getCurrentStep().getAlpha()))
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .strings("regulated_side", NetworkDataframes::getRatioTapChangerRegulatedSide, NetworkDataframes::setRatioTapChangerRegulatedSide, false)
                .build();
    }

    private static String getRatioTapChangerRegulatedSide(TwoWindingsTransformer transformer) {
        return getTerminalSideStr(transformer, transformer.getRatioTapChanger().getRegulationTerminal());
    }

    private static void setRatioTapChangerRegulatedSide(TwoWindingsTransformer transformer, String side) {
        transformer.getRatioTapChanger().setRegulationTerminal(getBranchTerminal(transformer, side));
    }

    private static double computeRho(TwoWindingsTransformer twoWindingsTransformer) {
        return twoWindingsTransformer.getRatedU2() / twoWindingsTransformer.getRatedU1()
                * (twoWindingsTransformer.getRatioTapChanger() != null ? twoWindingsTransformer.getRatioTapChanger().getCurrentStep().getRho() : 1)
                * (twoWindingsTransformer.getPhaseTapChanger() != null ? twoWindingsTransformer.getPhaseTapChanger().getCurrentStep().getRho() : 1);
    }

    private static NetworkDataframeMapper ptcs() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getTwoWindingsTransformerStream()
                        .filter(t -> t.getPhaseTapChanger() != null), NetworkDataframes::getT2OrThrow)
                .stringsIndex("id", TwoWindingsTransformer::getId)
                .ints("tap", t -> t.getPhaseTapChanger().getTapPosition(), (t, v) -> t.getPhaseTapChanger().setTapPosition(v))
                .ints("low_tap", t -> t.getPhaseTapChanger().getLowTapPosition())
                .ints("high_tap", t -> t.getPhaseTapChanger().getHighTapPosition())
                .ints("step_count", t -> t.getPhaseTapChanger().getStepCount())
                .booleans("regulating", t -> t.getPhaseTapChanger().isRegulating(), (t, v) -> t.getPhaseTapChanger().setRegulating(v))
                .enums("regulation_mode", PhaseTapChanger.RegulationMode.class, t -> t.getPhaseTapChanger().getRegulationMode(), (t, v) -> t.getPhaseTapChanger().setRegulationMode(v))
                .doubles("regulation_value", (t, context) -> t.getPhaseTapChanger().getRegulationValue(),
                    (t, v, context) -> t.getPhaseTapChanger().setRegulationValue(v))
                .doubles("target_deadband", (t, context) -> t.getPhaseTapChanger().getTargetDeadband(),
                    (t, v, context) -> t.getPhaseTapChanger().setTargetDeadband(v))
                .strings("regulating_bus_id", t -> getBusId(t.getPhaseTapChanger().getRegulationTerminal()))
                .strings("regulated_side", NetworkDataframes::getPhaseTapChangerRegulatedSide, NetworkDataframes::setPhaseTapChangerRegulatedSide, false)
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .build();
    }

    static String getPhaseTapChangerRegulatedSide(TwoWindingsTransformer transformer) {
        return getTerminalSideStr(transformer, transformer.getPhaseTapChanger().getRegulationTerminal());
    }

    static NetworkDataframeMapper identifiables() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getIdentifiables().stream(),
                        getOrThrow(Network::getIdentifiable, "Identifiable"))
                .stringsIndex("id", Identifiable::getId)
                .strings("type", identifiable -> identifiable.getType().toString())
                .build();
    }

    static NetworkDataframeMapper injections() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getConnectableStream()
                                .filter(Injection.class::isInstance)
                                .map(connectable -> (Injection<?>) connectable),
                        NetworkDataframes::getInjectionOrThrow)
                .stringsIndex("id", Injection::getId)
                .strings("type", injection -> injection.getType().toString())
                .strings("voltage_level_id", injection -> injection.getTerminal().getVoltageLevel().getId())
                .ints("node", g -> getNode(g.getTerminal()), false)
                .strings("bus_breaker_bus_id", injection -> getBusBreakerViewBusId(injection.getTerminal()), (injection, id) -> setBusBreakerViewBusId(injection.getTerminal(), id), false)
                .booleans("connected", injection -> injection.getTerminal().isConnected(), connectInjection())
                .strings("bus_id", injection -> injection.getTerminal().getBusView().getBus() == null ? "" :
                        injection.getTerminal().getBusView().getBus().getId())
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (l, context) -> perUnitI(context, l.getTerminal()))
                .build();
    }

    static NetworkDataframeMapper branches() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getBranchStream, getOrThrow(Network::getBranch, "Branch"))
                .stringsIndex("id", Branch::getId)
                .strings("type", branch -> branch.getType().toString())
                .strings("voltage_level1_id", branch -> branch.getTerminal1().getVoltageLevel().getId())
                .ints("node1", g -> getNode(g.getTerminal1()), false)
                .strings("bus_breaker_bus1_id", branch -> getBusBreakerViewBusId(branch.getTerminal1()), (branch, id) -> setBusBreakerViewBusId(branch.getTerminal1(), id), false)
                .strings("bus1_id", branch -> branch.getTerminal1().getBusView().getBus() == null ? "" :
                        branch.getTerminal1().getBusView().getBus().getId())
                .booleans("connected1", branch -> branch.getTerminal1().isConnected(),
                    (branch, connected) -> setConnected(branch.getTerminal1(), connected))
                .strings("voltage_level2_id", branch -> branch.getTerminal2().getVoltageLevel().getId())
                .ints("node2", g -> getNode(g.getTerminal2()), false)
                .strings("bus_breaker_bus2_id", branch -> getBusBreakerViewBusId(branch.getTerminal2()), (branch, id) -> setBusBreakerViewBusId(branch.getTerminal2(), id), false)
                .strings("bus2_id", branch -> branch.getTerminal2().getBusView().getBus() == null ? "" :
                        branch.getTerminal2().getBusView().getBus().getId())
                .booleans("connected2", branch -> branch.getTerminal2().isConnected(),
                    (branch, connected) -> setConnected(branch.getTerminal2(), connected))
                .doubles("p1", getPerUnitP1(), setPerUnitP1())
                .doubles("q1", getPerUnitQ1(), setPerUnitQ1())
                .doubles("i1", (branch, context) -> perUnitI(context, branch.getTerminal1()))
                .doubles("p2", getPerUnitP2(), setPerUnitP2())
                .doubles("q2", getPerUnitQ2(), setPerUnitQ2())
                .doubles("i2", (branch, context) -> perUnitI(context, branch.getTerminal2()))
                .strings("selected_limits_group_1", branch -> (String) branch.getSelectedOperationalLimitsGroupId1().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        Branch::setSelectedOperationalLimitsGroup1, false)
                .strings("selected_limits_group_2", branch -> (String) branch.getSelectedOperationalLimitsGroupId2().orElse(DEFAULT_OPERATIONAL_LIMIT_GROUP_ID),
                        Branch::setSelectedOperationalLimitsGroup2, false)
                .build();
    }

    static NetworkDataframeMapper terminals() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getConnectableStream()
                                .flatMap(connectable -> (Stream<Terminal>) connectable.getTerminals().stream()),
                        NetworkDataframes::getTerminal)
                .stringsIndex("element_id", terminal -> terminal.getConnectable().getId())
                .strings("voltage_level_id", terminal -> terminal.getVoltageLevel().getId())
                .strings("bus_id", terminal -> terminal.getBusView().getBus() == null ? "" :
                        terminal.getBusView().getBus().getId())
                .strings("element_side", terminal -> terminal.getConnectable() instanceof Branch ?
                                ((Branch<?>) terminal.getConnectable()).getSide(terminal).toString() : "",
                    (terminal, element_side) -> Function.identity())
                .booleans("connected", Terminal::isConnected, NetworkDataframes::setConnected)
                .build();
    }

    private static Terminal getTerminal(Network network, UpdatingDataframe dataframe, int index) {
        String id = dataframe.getStringValue("element_id", index)
                .orElseThrow(() -> new IllegalArgumentException("element_id column is missing"));
        Connectable<?> connectable = network.getConnectable(id);
        if (connectable == null) {
            throw new PowsyblException("connectable " + id + " not found");
        }
        String sideStr = dataframe.getStringValue("element_side", index).orElse(null);
        if (sideStr == null) {
            if (connectable instanceof Branch || connectable instanceof ThreeWindingsTransformer) {
                throw new PowsyblException("side must be provided for this element : " + id);
            }
            return connectable.getTerminals().get(0);
        }
        SideEnum side = SideEnum.valueOf(sideStr);
        switch (side) {
            case ONE -> {
                if (connectable instanceof Branch) {
                    return ((Branch<?>) connectable).getTerminal(TwoSides.ONE);
                } else if (connectable instanceof ThreeWindingsTransformer twt) {
                    return twt.getTerminal(ThreeSides.ONE);
                } else {
                    throw new PowsyblException("no side ONE for this element");
                }
            }
            case TWO -> {
                if (connectable instanceof Branch) {
                    return ((Branch<?>) connectable).getTerminal(TwoSides.TWO);
                } else if (connectable instanceof ThreeWindingsTransformer twt) {
                    return twt.getTerminal(ThreeSides.TWO);
                } else {
                    throw new PowsyblException("no side TWO for this element");
                }
            }
            case THREE -> {
                if (connectable instanceof ThreeWindingsTransformer twt) {
                    return twt.getTerminal(ThreeSides.THREE);
                } else {
                    throw new PowsyblException("no side THREE for this element");
                }
            }
            default -> throw new PowsyblException("side must be ONE, TWO or THREE");
        }
    }

    private static void setConnected(Terminal terminal, boolean connected) {
        if (connected) {
            terminal.connect();
        } else {
            terminal.disconnect();
        }
    }

    private static Terminal getBranchTerminal(Branch<?> branch, String side) {
        if (side.isEmpty()) {
            return null;
        } else if (side.equals(TwoSides.ONE.name())) {
            return branch.getTerminal1();
        } else if (side.equals(TwoSides.TWO.name())) {
            return branch.getTerminal2();
        } else {
            throw new PowsyblException("Transformer side must be ONE or TWO");
        }
    }

    private static String getTerminalSideStr(Branch<?> branch, Terminal terminal) {
        if (terminal == branch.getTerminal1()) {
            return TwoSides.ONE.name();
        } else if (terminal == branch.getTerminal2()) {
            return TwoSides.TWO.name();
        }
        return "";
    }

    private static String getTerminalSideStr(ThreeWindingsTransformer t3wt, Terminal terminal) {
        if (terminal == t3wt.getLeg1().getTerminal()) {
            return ThreeSides.ONE.name();
        } else if (terminal == t3wt.getLeg2().getTerminal()) {
            return ThreeSides.TWO.name();
        } else if (terminal == t3wt.getLeg3().getTerminal()) {
            return ThreeSides.THREE.name();
        }
        return "";
    }

    private static String getTerminalSideStr(Identifiable<?> identifiable, Terminal terminal) {
        if (identifiable instanceof Branch<?> branch) {
            return getTerminalSideStr(branch, terminal);
        } else if (identifiable instanceof ThreeWindingsTransformer t3wt) {
            return getTerminalSideStr(t3wt, terminal);
        }
        return "";
    }

    static void setPhaseTapChangerRegulatedSide(TwoWindingsTransformer transformer, String side) {
        transformer.getPhaseTapChanger().setRegulationTerminal(getBranchTerminal(transformer, side));
    }

    private static NetworkDataframeMapper operationalLimits(boolean onlyActive) {
        return NetworkDataframeMapperBuilder.ofStream(onlyActive ? NetworkUtil::getSelectedLimits : NetworkUtil::getLimits)
                .stringsIndex("element_id", TemporaryLimitData::getId)
                .enums("element_type", IdentifiableType.class, TemporaryLimitData::getElementType)
                .enums("side", TemporaryLimitData.Side.class, TemporaryLimitData::getSide)
                .strings("name", TemporaryLimitData::getName)
                .enums("type", LimitType.class, TemporaryLimitData::getType)
                .doubles("value", TemporaryLimitData::getValue)
                .ints("acceptable_duration", TemporaryLimitData::getAcceptableDuration)
                .booleans("fictitious", TemporaryLimitData::isFictitious, false)
                .strings("group_name", TemporaryLimitData::getGroupId, false)
                .booleans("selected", TemporaryLimitData::isSelected, false)
                .build();
    }

    private static Stream<Pair<String, ReactiveLimitsHolder>> streamReactiveLimitsHolder(Network network) {
        return Stream.concat(Stream.concat(network.getGeneratorStream().map(g -> Pair.of(g.getId(), g)),
                        network.getVscConverterStationStream().map(g -> Pair.of(g.getId(), g))),
                network.getBatteryStream().map(g -> Pair.of(g.getId(), g)));
    }

    private static Stream<Triple<String, ReactiveCapabilityCurve.Point, Integer>> streamPoints(Network network) {
        return streamReactiveLimitsHolder(network)
                .filter(p -> p.getRight().getReactiveLimits() instanceof ReactiveCapabilityCurve)
                .flatMap(p -> indexPoints(p.getLeft(), p.getRight()).stream());
    }

    private static List<Triple<String, ReactiveCapabilityCurve.Point, Integer>> indexPoints(String id, ReactiveLimitsHolder holder) {
        ReactiveCapabilityCurve curve = (ReactiveCapabilityCurve) holder.getReactiveLimits();
        List<Triple<String, ReactiveCapabilityCurve.Point, Integer>> values = new ArrayList<>(curve.getPointCount());
        int num = 0;
        for (ReactiveCapabilityCurve.Point point : curve.getPoints()) {
            values.add(Triple.of(id, point, num));
            num++;
        }
        return values;
    }

    private static NetworkDataframeMapper reactiveCapabilityCurves() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkDataframes::streamPoints)
                .stringsIndex("id", Triple::getLeft)
                .intsIndex("num", Triple::getRight)
                .doubles("p", (t, context) -> perUnitPQ(context, t.getMiddle().getP()))
                .doubles("min_q", (t, context) -> perUnitPQ(context, t.getMiddle().getMinQ()))
                .doubles("max_q", (t, context) -> perUnitPQ(context, t.getMiddle().getMaxQ()))
                .build();
    }

    private static <T> ToIntFunction<T> getRatioTapPosition(Function<T, RatioTapChangerHolder> getter) {
        return ifExistsInt(t -> getter.apply(t).getRatioTapChanger(), RatioTapChanger::getTapPosition);
    }

    private static <T> ToIntFunction<T> getPhaseTapPosition(Function<T, PhaseTapChangerHolder> getter) {
        return ifExistsInt(t -> getter.apply(t).getPhaseTapChanger(), PhaseTapChanger::getTapPosition);
    }

    private static void setTapPosition(TapChanger<?, ?, ?, ?> tapChanger, int position) {
        if (tapChanger != null) {
            tapChanger.setTapPosition(position);
        }
    }

    private static String getBusId(Terminal t) {
        if (t == null) {
            return "";
        } else {
            Bus bus = t.getBusView().getBus();
            return bus != null ? bus.getId() : "";
        }
    }

    private static String getBusBreakerViewBusId(Terminal t) {
        if (t == null) {
            return "";
        } else {
            Bus bus = t.isConnected() ? t.getBusBreakerView().getBus() : t.getBusBreakerView().getConnectableBus();
            return bus != null ? bus.getId() : "";
        }
    }

    private static <T extends Injection<T>> Function<T, String> getBusBreakerViewBusId() {
        return i -> getBusBreakerViewBusId(i.getTerminal());
    }

    private static void setBusBreakerViewBusId(Terminal t, String busId) {
        Objects.requireNonNull(t).getBusBreakerView().setConnectableBus(busId);
    }

    private static <T extends Injection<T>> void setBusBreakerViewBusId(T i, String busId) {
        setBusBreakerViewBusId(i.getTerminal(), busId);
    }

    private static int getNode(Terminal t) {
        if (t.getVoltageLevel().getTopologyKind().equals(TopologyKind.NODE_BREAKER)) {
            return t.getNodeBreakerView().getNode();
        } else {
            return -1;
        }
    }

    /**
     * Wraps equipment getter to throw if not found
     */
    private static <T> BiFunction<Network, String, T> getOrThrow(BiFunction<Network, String, T> getter, String type) {
        return (network, id) -> {
            T equipment = getter.apply(network, id);
            if (equipment == null) {
                throw new PowsyblException(type + " '" + id + "' not found");
            }
            return equipment;
        };
    }

    private static TwoWindingsTransformer getT2OrThrow(Network network, String id) {
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
        if (twt == null) {
            throw new PowsyblException("Two windings transformer '" + id + "' not found");
        }
        return twt;
    }

    private static Injection<?> getInjectionOrThrow(Network network, String id) {
        Connectable<?> c = network.getConnectable(id);
        if (!(c instanceof Injection<?>)) {
            throw new PowsyblException("Injection '" + id + "' not found");
        }
        return (Injection<?>) c;
    }

    public static NetworkDataframeMapper getExtensionDataframeMapper(String extensionName, String tableName) {
        return EXTENSIONS_MAPPERS.get(new ExtensionDataframeKey(extensionName, tableName));
    }

    private static NetworkDataframeMapper aliases() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkDataframes::getAliasesData)
                .stringsIndex("id", pair -> pair.getLeft().getId())
                .strings("alias", Pair::getRight)
                .strings("alias_type", pair -> pair.getLeft().getAliasType(pair.getRight()).orElse(""))
                .build();
    }

    private static Stream<Pair<Identifiable<?>, String>> getAliasesData(Network network) {
        return network.getIdentifiables().stream()
                .flatMap(identifiable -> identifiable.getAliases().stream()
                        .map(alias -> Pair.of(identifiable, alias)));
    }

    private static NetworkDataframeMapper areaVoltageLevels() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkDataframes::areaVoltageLevelsData)
                .stringsIndex("id", pair -> pair.getLeft().getId())
                .strings("voltage_level_id", pair -> pair.getRight().getId())
                .build();
    }

    private static Stream<Pair<Area, VoltageLevel>> areaVoltageLevelsData(Network network) {
        return network.getAreaStream()
                .flatMap(area -> area.getVoltageLevelStream()
                        .map(voltageLevel -> Pair.of(area, voltageLevel)));
    }

    private static NetworkDataframeMapper areaBoundaries() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkDataframes::areaBoundariesData)
                .stringsIndex("id", pair -> pair.getLeft().getId())
                .strings("boundary_type", pair -> getAreaBoundaryType(pair.getRight()), false)
                .strings("element", pair -> getAreaBoundaryElement(pair.getRight()))
                .strings("side", pair -> getAreaBoundarySide(pair.getRight()), false)
                .booleans("ac", pair -> pair.getRight().isAc())
                .doubles("p", (pair, context) -> perUnitPQ(context, pair.getRight().getP()))
                .doubles("q", (pair, context) -> perUnitPQ(context, pair.getRight().getQ()))
                .build();
    }

    private static String getAreaBoundaryType(AreaBoundary areaBoundary) {
        Objects.requireNonNull(areaBoundary);
        return areaBoundary.getBoundary().map(
                b -> PyPowsyblApiHeader.ElementType.DANGLING_LINE.name()
        ).orElse(PyPowsyblApiHeader.ElementType.TERMINAL.name());
    }

    private static String getAreaBoundaryElement(AreaBoundary areaBoundary) {
        Objects.requireNonNull(areaBoundary);
        return areaBoundary.getBoundary().map(
                b -> b.getDanglingLine().getId()
        ).orElseGet(() -> areaBoundary.getTerminal().orElseThrow().getConnectable().getId());
    }

    private static String getAreaBoundarySide(AreaBoundary areaBoundary) {
        Objects.requireNonNull(areaBoundary);
        if (areaBoundary.getBoundary().isPresent()) {
            return "";
        }
        Terminal terminal = areaBoundary.getTerminal().orElseThrow();
        return getTerminalSideStr(terminal.getConnectable(), terminal);
    }

    private static Stream<Pair<Area, AreaBoundary>> areaBoundariesData(Network network) {
        return network.getAreaStream()
                .flatMap(area -> area.getAreaBoundaryStream()
                        .map(areaBoundary -> Pair.of(area, areaBoundary)));
    }

}

