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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.powsybl.dataframe.MappingUtils.*;
import static com.powsybl.dataframe.network.PerUnitUtil.*;

/**
 * Main user entry point of the package :
 * defines the mappings for all elements of the network.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public final class NetworkDataframes {

    private static final Map<DataframeElementType, NetworkDataframeMapper> MAPPERS = createMappers();

    private static final Map<ExtensionDataframeKey, NetworkDataframeMapper> EXTENSIONS_MAPPERS = NetworkExtensions.createExtensionsMappers();

    private static final String DEFAULT_OPERATIONAL_LIMIT_GROUP_ID = "DEFAULT";
    private static final String MIN_Q_AT_TARGET_P = "min_q_at_target_p";
    private static final String MAX_Q_AT_TARGET_P = "max_q_at_target_p";
    private static final String MIN_Q_AT_P = "min_q_at_p";
    private static final String MAX_Q_AT_P = "max_q_at_p";
    private static final String REGULATED_BUS_ID = "regulated_bus_id";
    private static final String REGULATED_BUS_BREAKER_BUS_ID = "regulated_bus_breaker_bus_id";
    private static final String REGULATING = "regulating";

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
        mappers.put(DataframeElementType.GROUND, grounds());
        mappers.put(DataframeElementType.SHUNT_COMPENSATOR, shunts());
        mappers.put(DataframeElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, shuntsNonLinear());
        mappers.put(DataframeElementType.LINEAR_SHUNT_COMPENSATOR_SECTION, linearShuntsSections());
        mappers.put(DataframeElementType.DANGLING_LINE, danglingLines());
        mappers.put(DataframeElementType.DANGLING_LINE_GENERATION, danglingLinesGeneration());
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
        mappers.put(DataframeElementType.PROPERTIES, properties());
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
        return reactiveLimits instanceof MinMaxReactiveLimits minMaxReactiveLimits ? minMaxReactiveLimits : null;
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
                .doubles(MIN_Q_AT_TARGET_P, getPerUnitMinQ(Generator::getTargetP), false)
                .doubles(MAX_Q_AT_TARGET_P, getPerUnitMaxQ(Generator::getTargetP), false)
                .doubles(MIN_Q_AT_P, getPerUnitMinQ(getOppositeP()), false)
                .doubles(MAX_Q_AT_P, getPerUnitMaxQ(getOppositeP()), false)
                .doubles("rated_s", (g, context) -> g.getRatedS(), (g, ratedS, context) -> g.setRatedS(ratedS))
                .strings("reactive_limits_kind", NetworkDataframes::getReactiveLimitsKind)
                .doubles("target_v", (g, context) -> perUnitTargetV(context, g.getTargetV(), g.getRegulatingTerminal(), g.getTerminal()),
                    (g, v, context) -> g.setTargetV(unPerUnitTargetV(context, v, g.getRegulatingTerminal(), g.getTerminal())))
                .doubles("target_q", (g, context) -> perUnitPQ(context, g.getTargetQ()), (g, q, context) -> g.setTargetQ(unPerUnitPQ(context, q)))
                .booleans("voltage_regulator_on", Generator::isVoltageRegulatorOn, Generator::setVoltageRegulatorOn)
                .strings("regulated_element_id", generator -> NetworkUtil.getRegulatedElementId(generator::getRegulatingTerminal),
                        (generator, elementId) -> NetworkUtil.setRegulatingTerminal(generator::setRegulatingTerminal, generator.getNetwork(), elementId))
                .strings(REGULATED_BUS_ID, generator -> getBusId(generator.getRegulatingTerminal()), false)
                .strings(REGULATED_BUS_BREAKER_BUS_ID, generator -> getBusBreakerViewBusId(generator.getRegulatingTerminal()), false)
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (g, context) -> perUnitI(context, g.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", g -> getBusId(g.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", g -> getNode(g.getTerminal()), false)
                .booleans("condenser", Generator::isCondenser, false)
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
            builder.strings("bus_id", b -> NetworkUtil.getBusViewBus(b).map(Bus::getId).orElse(""));
        }
        return builder.booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .addProperties()
                .build();
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
                .doubles(MIN_Q_AT_TARGET_P, getPerUnitMinQ(Battery::getTargetP), false)
                .doubles(MAX_Q_AT_TARGET_P, getPerUnitMaxQ(Battery::getTargetP), false)
                .doubles(MIN_Q_AT_P, getPerUnitMinQ(getOppositeP()), false)
                .doubles(MAX_Q_AT_P, getPerUnitMaxQ(getOppositeP()), false)
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

    static NetworkDataframeMapper grounds() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getGroundStream, getOrThrow(Network::getGround, "Ground"))
                .stringsIndex("id", Ground::getId)
                .strings("name", b -> b.getOptionalName().orElse(""), Identifiable::setName)
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", b -> getBusId(b.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", b -> getNode(b.getTerminal()), false)
                .booleans("connected", b -> b.getTerminal().isConnected(), connectInjection())
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
                            // careful: shunt section number starts at 1, but position in array starts at 0
                            var allSections = model.getAllSections();
                            return IntStream.range(0, allSections.size()).mapToObj(i -> Triple.of(shuntCompensator, allSections.get(i), i + 1));
                        });
        return NetworkDataframeMapperBuilder.ofStream(nonLinearShunts, NetworkDataframes::getShuntSectionNonlinear)
                .stringsIndex("id", triple -> triple.getLeft().getId())
                .intsIndex("section", Triple::getRight)
                .doubles("g", (p, context) -> perUnitG(context, p.getMiddle(), p.getLeft()),
                    (p, g, context) -> p.getMiddle().setG(unPerUnitGB(context, p.getLeft(), g)))
                .doubles("b", (p, context) -> perUnitB(context, p.getMiddle(), p.getLeft()),
                    (p, b, context) -> p.getMiddle().setB(unPerUnitGB(context, p.getLeft(), b)))
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
            // careful: shunt section number starts at 1, but position in array starts at 0
            List<ShuntCompensatorNonLinearModel.Section> allSections = shuntNonLinear.getAllSections();
            if (section < 1 || section > allSections.size()) {
                throw new PowsyblException(String.format("Section number must be between 1 and %d, inclusive", allSections.size()));
            }
            return Triple.of(shuntCompensator, allSections.get(section - 1), section);
        }
    }

    static NetworkDataframeMapper linearShuntsSections() {
        Function<Network, Stream<Pair<ShuntCompensator, ShuntCompensatorLinearModel>>> linearShunts = network ->
                network.getShuntCompensatorStream()
                        .filter(sc -> sc.getModelType() == ShuntCompensatorModelType.LINEAR)
                        .map(shuntCompensator -> Pair.of(shuntCompensator, (ShuntCompensatorLinearModel) shuntCompensator.getModel()));
        return NetworkDataframeMapperBuilder.ofStream(linearShunts, (net, s) -> Pair.of(checkShuntNonNull(net, s), checkLinearModel(net, s)))
                .stringsIndex("id", p -> p.getLeft().getId())
                .doubles("g_per_section", (p, context) -> perUnitGB(context, p.getRight().getGPerSection(), p.getLeft().getTerminal().getVoltageLevel().getNominalV()),
                    (p, g, context) -> p.getRight().setGPerSection(unPerUnitGB(context, g, p.getLeft().getTerminal().getVoltageLevel().getNominalV())))
                .doubles("b_per_section", (p, context) -> perUnitGB(context, p.getRight().getBPerSection(), p.getLeft().getTerminal().getVoltageLevel().getNominalV()),
                    (p, b, context) -> p.getRight().setBPerSection(unPerUnitGB(context, b, p.getLeft().getTerminal().getVoltageLevel().getNominalV())))
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
                .doubles("r", (twt, context) -> perUnitRX(context, twt, twt.getR()), (twt, r, context) -> twt.setR(unPerUnitRX(context, twt, r)))
                .doubles("x", (twt, context) -> perUnitRX(context, twt, twt.getX()), (twt, x, context) -> twt.setX(unPerUnitRX(context, twt, x)))
                .doubles("g", (twt, context) -> perUnitGB(context, twt, twt.getG()), (twt, g, context) -> twt.setG(unPerUnitGB(context, twt, g)))
                .doubles("b", (twt, context) -> perUnitGB(context, twt, twt.getB()), (twt, b, context) -> twt.setB(unPerUnitGB(context, twt, b)))
                .doubles("rated_u1", (twt, context) -> twt.getRatedU1(), (twt, ratedV1, context) -> twt.setRatedU1(ratedV1))
                .doubles("rated_u2", (twt, context) -> twt.getRatedU2(), (twt, ratedV2, context) -> twt.setRatedU2(ratedV2))
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
                .doubles("rho", (twt, context) -> perUnitRho(context, twt, NetworkDataframes.computeRho(twt)), false)
                .doubles("alpha", (twt, context) -> perUnitAngle(context, NetworkDataframes.computeAlpha(twt)), false)
                .doubles("r_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeR(twt)), false)
                .doubles("x_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeX(twt)), false)
                .doubles("g_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeG(twt)), false)
                .doubles("b_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeB(twt)), false)
                .addProperties()
                .build();
    }

    static NetworkDataframeMapper threeWindingTransformers() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getThreeWindingsTransformerStream, getOrThrow(Network::getThreeWindingsTransformer, "Three windings transformer"))
                .stringsIndex("id", ThreeWindingsTransformer::getId)
                .strings("name", twt -> twt.getOptionalName().orElse(""), Identifiable::setName)
                .doubles("rated_u0", (twt, context) -> twt.getRatedU0(), (twt, ratedU0, context) -> twt.setRatedU0(ratedU0))
                .doubles("r1", (twt, context) -> perUnitRX(context, twt, twt.getLeg1().getR()), (twt, r1, context) -> twt.getLeg1().setR(unPerUnitRX(context, twt, r1)))
                .doubles("x1", (twt, context) -> perUnitRX(context, twt, twt.getLeg1().getX()), (twt, x1, context) -> twt.getLeg1().setX(unPerUnitRX(context, twt, x1)))
                .doubles("g1", (twt, context) -> perUnitGB(context, twt, twt.getLeg1().getG()), (twt, g1, context) -> twt.getLeg1().setG(unPerUnitGB(context, twt, g1)))
                .doubles("b1", (twt, context) -> perUnitGB(context, twt, twt.getLeg1().getB()), (twt, b1, context) -> twt.getLeg1().setB(unPerUnitGB(context, twt, b1)))
                .doubles("rated_u1", (twt, context) -> twt.getLeg1().getRatedU(), (twt, ratedU1, context) -> twt.getLeg1().setRatedU(ratedU1))
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
                .doubles("rho1", (twt, context) -> perUnitRho(context, twt, ThreeSides.ONE, NetworkDataframes.computeRho(twt, ThreeSides.ONE)), false)
                .doubles("alpha1", (twt, context) -> perUnitAngle(context, NetworkDataframes.computeAlpha(twt, ThreeSides.ONE)), false)
                .doubles("r1_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeR(twt, ThreeSides.ONE)), false)
                .doubles("x1_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeX(twt, ThreeSides.ONE)), false)
                .doubles("g1_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeG(twt, ThreeSides.ONE)), false)
                .doubles("b1_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeB(twt, ThreeSides.ONE)), false)
                .doubles("r2", (twt, context) -> perUnitRX(context, twt, twt.getLeg2().getR()), (twt, r2, context) -> twt.getLeg2().setR(unPerUnitRX(context, twt, r2)))
                .doubles("x2", (twt, context) -> perUnitRX(context, twt, twt.getLeg2().getX()), (twt, x2, context) -> twt.getLeg2().setX(unPerUnitRX(context, twt, x2)))
                .doubles("g2", (twt, context) -> perUnitGB(context, twt, twt.getLeg2().getG()), (twt, g2, context) -> twt.getLeg2().setG(unPerUnitGB(context, twt, g2)))
                .doubles("b2", (twt, context) -> perUnitGB(context, twt, twt.getLeg2().getB()), (twt, b2, context) -> twt.getLeg2().setB(unPerUnitGB(context, twt, b2)))
                .doubles("rated_u2", (twt, context) -> twt.getLeg2().getRatedU(), (twt, ratedU2, context) -> twt.getLeg2().setRatedU(ratedU2))
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
                .doubles("rho2", (twt, context) -> perUnitRho(context, twt, ThreeSides.TWO, NetworkDataframes.computeRho(twt, ThreeSides.TWO)), false)
                .doubles("alpha2", (twt, context) -> perUnitAngle(context, NetworkDataframes.computeAlpha(twt, ThreeSides.TWO)), false)
                .doubles("r2_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeR(twt, ThreeSides.TWO)), false)
                .doubles("x2_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeX(twt, ThreeSides.TWO)), false)
                .doubles("g2_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeG(twt, ThreeSides.TWO)), false)
                .doubles("b2_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeB(twt, ThreeSides.TWO)), false)
                .doubles("r3", (twt, context) -> perUnitRX(context, twt, twt.getLeg3().getR()), (twt, r3, context) -> twt.getLeg3().setR(unPerUnitRX(context, twt, r3)))
                .doubles("x3", (twt, context) -> perUnitRX(context, twt, twt.getLeg3().getX()), (twt, x3, context) -> twt.getLeg3().setX(unPerUnitRX(context, twt, x3)))
                .doubles("g3", (twt, context) -> perUnitGB(context, twt, twt.getLeg3().getG()), (twt, g3, context) -> twt.getLeg3().setG(unPerUnitGB(context, twt, g3)))
                .doubles("b3", (twt, context) -> perUnitGB(context, twt, twt.getLeg3().getB()), (twt, b3, context) -> twt.getLeg3().setB(unPerUnitGB(context, twt, b3)))
                .doubles("rated_u3", (twt, context) -> twt.getLeg3().getRatedU(), (twt, ratedU3, context) -> twt.getLeg3().setRatedU(ratedU3))
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
                .doubles("rho3", (twt, context) -> perUnitRho(context, twt, ThreeSides.THREE, NetworkDataframes.computeRho(twt, ThreeSides.THREE)), false)
                .doubles("alpha3", (twt, context) -> perUnitAngle(context, NetworkDataframes.computeAlpha(twt, ThreeSides.THREE)), false)
                .doubles("r3_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeR(twt, ThreeSides.THREE)), false)
                .doubles("x3_at_current_tap", (twt, context) -> perUnitRX(context, twt, NetworkDataframes.computeX(twt, ThreeSides.THREE)), false)
                .doubles("g3_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeG(twt, ThreeSides.THREE)), false)
                .doubles("b3_at_current_tap", (twt, context) -> perUnitGB(context, twt, NetworkDataframes.computeB(twt, ThreeSides.THREE)), false)
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
                .doubles("boundary_i", (dl, context) -> perUnitI(context, dl.getBoundary().getI(), dl.getTerminal().getVoltageLevel().getNominalV()), false)
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

    static NetworkDataframeMapper danglingLinesGeneration() {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getDanglingLineStream().filter(dl -> Optional.ofNullable(dl.getGeneration()).isPresent()),
                        getOrThrow(Network::getDanglingLine, "Dangling line with generation"))
                .stringsIndex("id", DanglingLine::getId)
                .doubles("min_p", (dl, context) -> perUnitPQ(context, dl.getGeneration().getMinP()),
                        (dl, minP, context) -> dl.getGeneration().setMinP(unPerUnitPQ(context, minP)))
                .doubles("max_p", (dl, context) -> perUnitPQ(context, dl.getGeneration().getMaxP()),
                        (dl, maxP, context) -> dl.getGeneration().setMaxP(unPerUnitPQ(context, maxP)))
                .doubles("target_p", (dl, context) -> perUnitPQ(context, dl.getGeneration().getTargetP()),
                        (dl, targetP, context) -> dl.getGeneration().setTargetP(unPerUnitPQ(context, targetP)))
                .doubles("target_q", (dl, context) -> perUnitPQ(context, dl.getGeneration().getTargetQ()),
                        (dl, targetQ, context) -> dl.getGeneration().setTargetQ(unPerUnitPQ(context, targetQ)))
                .doubles("target_v", (dl, context) -> perUnitV(context, dl.getGeneration().getTargetV(), dl.getTerminal()),
                        (dl, targetV, context) -> dl.getGeneration().setTargetV(unPerUnitV(context, targetV, dl.getTerminal())))
                .booleans("voltage_regulator_on", dl -> dl.getGeneration().isVoltageRegulationOn(),
                        (dl, voltageRegulatorOn) -> dl.getGeneration().setVoltageRegulationOn(voltageRegulatorOn))
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
                .strings("hvdc_line_id", station -> Optional.ofNullable(station.getHvdcLine()).map(Identifiable::getId).orElse(""))
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
                .doubles(MIN_Q_AT_TARGET_P, getPerUnitMinQ(vsc -> vsc.getHvdcLine().getActivePowerSetpoint()), false)
                .doubles(MAX_Q_AT_TARGET_P, getPerUnitMaxQ(vsc -> vsc.getHvdcLine().getActivePowerSetpoint()), false)
                .doubles(MIN_Q_AT_P, getPerUnitMinQ(getOppositeP()), false)
                .doubles(MAX_Q_AT_P, getPerUnitMaxQ(getOppositeP()), false)
                .strings("reactive_limits_kind", NetworkDataframes::getReactiveLimitsKind)
                .doubles("target_v", (vsc, context) -> perUnitTargetV(context, vsc.getVoltageSetpoint(), vsc.getRegulatingTerminal(), vsc.getTerminal()),
                    (vsc, targetV, context) -> vsc.setVoltageSetpoint(unPerUnitTargetV(context, targetV, vsc.getRegulatingTerminal(), vsc.getTerminal())))
                .doubles("target_q", (vsc, context) -> perUnitPQ(context, vsc.getReactivePowerSetpoint()),
                    (vsc, targetQ, context) -> vsc.setReactivePowerSetpoint(unPerUnitPQ(context, targetQ)))
                .booleans("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn, VscConverterStation::setVoltageRegulatorOn)
                .strings("regulated_element_id", vsc -> NetworkUtil.getRegulatedElementId(vsc::getRegulatingTerminal),
                        (vsc, elementId) -> NetworkUtil.setRegulatingTerminal(vsc::setRegulatingTerminal, vsc.getNetwork(), elementId))
                .strings(REGULATED_BUS_ID, vsc -> getBusId(vsc.getRegulatingTerminal()), false)
                .strings(REGULATED_BUS_BREAKER_BUS_ID, vsc -> getBusBreakerViewBusId(vsc.getRegulatingTerminal()), false)
                .doubles("p", getPerUnitP(), setPerUnitP())
                .doubles("q", getPerUnitQ(), setPerUnitQ())
                .doubles("i", (st, context) -> perUnitI(context, st.getTerminal()))
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", st -> getBusId(st.getTerminal()))
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", st -> getNode(st.getTerminal()), false)
                .booleans("connected", st -> st.getTerminal().isConnected(), connectInjection())
                .booleans("fictitious", Identifiable::isFictitious, Identifiable::setFictitious, false)
                .strings("hvdc_line_id", station -> Optional.ofNullable(station.getHvdcLine()).map(Identifiable::getId).orElse(""))
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
                .booleans(REGULATING, StaticVarCompensator::isRegulating, StaticVarCompensator::setRegulating)
                .strings("regulated_element_id", svc -> NetworkUtil.getRegulatedElementId(svc::getRegulatingTerminal),
                        (svc, elementId) -> NetworkUtil.setRegulatingTerminal(svc::setRegulatingTerminal, svc.getNetwork(), elementId))
                .strings(REGULATED_BUS_ID, svc -> getBusId(svc.getRegulatingTerminal()), false)
                .strings(REGULATED_BUS_BREAKER_BUS_ID, svc -> getBusBreakerViewBusId(svc.getRegulatingTerminal()), false)
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
                .strings("topology_kind", vl -> vl.getTopologyKind().name(), (voltageLevel, topologyKindStr) -> {
                    TopologyKind topologyKind = TopologyKind.valueOf(topologyKindStr);
                    voltageLevel.convertToTopology(topologyKind);
                }, false)
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
                .strings("bus_breaker_bus_id", getBusBreakerViewBusId(), NetworkDataframes::setBusBreakerViewBusId, false)
                .ints("node", bbs -> getNode(bbs.getTerminal()), false)
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

    interface TapChangerStepRow {
        String getId();

        String getSide();

        RatioTapChangerStep getRtcStep();

        PhaseTapChangerStep getPtcStep();

        int getPosition();
    }

    record TapChangerStepRow2(TwoWindingsTransformer twt, int position) implements TapChangerStepRow {
        @Override
        public String getId() {
            return twt.getId();
        }

        @Override
        public String getSide() {
            return "";
        }

        @Override
        public RatioTapChangerStep getRtcStep() {
            return twt.getRatioTapChanger().getStep(position);
        }

        @Override
        public PhaseTapChangerStep getPtcStep() {
            return twt.getPhaseTapChanger().getStep(position);
        }

        @Override
        public int getPosition() {
            return position;
        }
    }

    record TapChangerStepRow3(ThreeWindingsTransformer twt, ThreeSides side, int position) implements TapChangerStepRow {
        @Override
        public String getId() {
            return twt.getId();
        }

        @Override
        public String getSide() {
            return side.name();
        }

        @Override
        public RatioTapChangerStep getRtcStep() {
            return twt.getLeg(side).getRatioTapChanger().getStep(position);
        }

        @Override
        public PhaseTapChangerStep getPtcStep() {
            return twt.getLeg(side).getPhaseTapChanger().getStep(position);
        }

        @Override
        public int getPosition() {
            return position;
        }
    }

    private static Stream<TapChangerStepRow> getTapChangerStepRows(Network network, TapChangerType type) {
        List<TapChangerStepRow> rows = new ArrayList<>();
        for (TwoWindingsTransformer twt : network.getTwoWindingsTransformers()) {
            switch (type) {
                case RATIO -> {
                    RatioTapChanger rtc = twt.getRatioTapChanger();
                    if (rtc != null) {
                        rows.addAll(rtc.getAllSteps().keySet().stream().map(position -> new TapChangerStepRow2(twt, position)).toList());
                    }
                }
                case PHASE -> {
                    PhaseTapChanger ptc = twt.getPhaseTapChanger();
                    if (ptc != null) {
                        rows.addAll(ptc.getAllSteps().keySet().stream().map(position -> new TapChangerStepRow2(twt, position)).toList());
                    }
                }
            }
        }
        for (ThreeWindingsTransformer twt : network.getThreeWindingsTransformers()) {
            for (ThreeWindingsTransformer.Leg leg : twt.getLegs()) {
                switch (type) {
                    case RATIO -> {
                        RatioTapChanger rtc = leg.getRatioTapChanger();
                        if (rtc != null) {
                            rows.addAll(rtc.getAllSteps().keySet().stream().map(position -> new TapChangerStepRow3(twt, leg.getSide(), position)).toList());
                        }
                    }
                    case PHASE -> {
                        PhaseTapChanger ptc = leg.getPhaseTapChanger();
                        if (ptc != null) {
                            rows.addAll(ptc.getAllSteps().keySet().stream().map(position -> new TapChangerStepRow3(twt, leg.getSide(), position)).toList());
                        }
                    }
                }
            }
        }
        return rows.stream();
    }

    static TapChangerStepRow getTapChangerStepRow(Network network, UpdatingDataframe dataframe, int index) {
        String id = dataframe.getStringValue("id", index)
                .orElseThrow(() -> new IllegalArgumentException("id column is missing"));
        int position = dataframe.getIntValue("position", index)
                .orElseThrow(() -> new IllegalArgumentException("position column is missing"));
        ThreeSides side = dataframe.getStringValue("side", index).stream()
                .map(ThreeSides::valueOf)
                .findFirst()
                .orElse(null);
        if (side == null) {
            TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
            if (twt == null) {
                throw new PowsyblException("2 windings transformer '" + id + "' does not exist.");
            }
            return new TapChangerStepRow2(twt, position);
        }
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(id);
        if (twt == null) {
            throw new PowsyblException("3 windings transformer '" + id + "' does not exist.");
        }
        return new TapChangerStepRow3(twt, side, position);
    }

    private static NetworkDataframeMapper rtcSteps() {
        return NetworkDataframeMapperBuilder.ofStream(network -> NetworkDataframes.getTapChangerStepRows(network, TapChangerType.RATIO), NetworkDataframes::getTapChangerStepRow)
                .stringsIndex("id", TapChangerStepRow::getId)
                .intsIndex("position", TapChangerStepRow::getPosition)
                .strings("side", TapChangerStepRow::getSide)
                .doubles("rho", (row, context) -> row.getRtcStep().getRho(),
                    (row, rho, context) -> row.getRtcStep().setRho(rho))
                .doubles("r", (row, context) -> row.getRtcStep().getR(),
                    (row, r, context) -> row.getRtcStep().setR(r))
                .doubles("x", (row, context) -> row.getRtcStep().getX(),
                    (row, x, context) -> row.getRtcStep().setX(x))
                .doubles("g", (row, context) -> row.getRtcStep().getG(),
                    (row, g, context) -> row.getRtcStep().setG(g))
                .doubles("b", (row, context) -> row.getRtcStep().getB(),
                    (row, b, context) -> row.getRtcStep().setB(b))
                .build();
    }

    private static NetworkDataframeMapper ptcSteps() {
        return NetworkDataframeMapperBuilder.ofStream(network -> NetworkDataframes.getTapChangerStepRows(network, TapChangerType.PHASE), NetworkDataframes::getTapChangerStepRow)
                .stringsIndex("id", TapChangerStepRow::getId)
                .intsIndex("position", TapChangerStepRow::getPosition)
                .strings("side", TapChangerStepRow::getSide)
                .doubles("rho", (row, context) -> row.getPtcStep().getRho(),
                    (row, rho, context) -> row.getPtcStep().setRho(rho))
                .doubles("alpha", (row, context) -> perUnitAngle(context, row.getPtcStep().getAlpha()),
                    (row, alpha, context) -> row.getPtcStep().setAlpha(unPerUnitAngle(context, alpha)))
                .doubles("r", (row, context) -> row.getPtcStep().getR(),
                    (row, r, context) -> row.getPtcStep().setR(r))
                .doubles("x", (row, context) -> row.getPtcStep().getX(),
                    (row, x, context) -> row.getPtcStep().setX(x))
                .doubles("g", (row, context) -> row.getPtcStep().getG(),
                    (row, g, context) -> row.getPtcStep().setG(g))
                .doubles("b", (row, context) -> row.getPtcStep().getB(),
                    (row, b, context) -> row.getPtcStep().setB(b))
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

    interface TapChangerRow {
        String getId();

        String getSide();

        RatioTapChanger getRtc();

        PhaseTapChanger getPtc();

        String getRtcRegulatedSide();

        void setRtcRegulatedSide(String regulatedSide);

        String getPtcRegulatedSide();

        void setPtcRegulatedSide(String regulatedSide);
    }

    record TapChangerRow2(TwoWindingsTransformer twt) implements TapChangerRow {
        @Override
        public String getId() {
            return twt.getId();
        }

        @Override
        public String getSide() {
            return null;
        }

        @Override
        public RatioTapChanger getRtc() {
            return twt.getRatioTapChanger();
        }

        @Override
        public PhaseTapChanger getPtc() {
            return twt.getPhaseTapChanger();
        }

        @Override
        public String getRtcRegulatedSide() {
            return NetworkDataframes.getTapChangerRegulatedSide(twt, TwoWindingsTransformer::getRatioTapChanger);
        }

        @Override
        public void setRtcRegulatedSide(String regulatedSide) {
            NetworkDataframes.setTapChangerRegulatedSide(twt, regulatedSide, TwoWindingsTransformer::getRatioTapChanger);
        }

        @Override
        public String getPtcRegulatedSide() {
            return NetworkDataframes.getTapChangerRegulatedSide(twt, TwoWindingsTransformer::getPhaseTapChanger);
        }

        @Override
        public void setPtcRegulatedSide(String regulatedSide) {
            NetworkDataframes.setTapChangerRegulatedSide(twt, regulatedSide, TwoWindingsTransformer::getPhaseTapChanger);
        }
    }

    record TapChangerRow3(ThreeWindingsTransformer twt, ThreeSides side) implements TapChangerRow {
        @Override
        public String getId() {
            return twt.getId();
        }

        @Override
        public String getSide() {
            return side.name();
        }

        @Override
        public RatioTapChanger getRtc() {
            return twt.getLeg(side).getRatioTapChanger();
        }

        @Override
        public PhaseTapChanger getPtc() {
            return twt.getLeg(side).getPhaseTapChanger();
        }

        @Override
        public String getRtcRegulatedSide() {
            return NetworkDataframes.getTapChangerRegulatedSide(twt, side, ThreeWindingsTransformer.Leg::getRatioTapChanger);
        }

        @Override
        public void setRtcRegulatedSide(String regulatedSide) {
            NetworkDataframes.setTapChangerRegulatedSide(twt, side, regulatedSide, ThreeWindingsTransformer.Leg::getRatioTapChanger);
        }

        @Override
        public String getPtcRegulatedSide() {
            return NetworkDataframes.getTapChangerRegulatedSide(twt, side, ThreeWindingsTransformer.Leg::getPhaseTapChanger);
        }

        @Override
        public void setPtcRegulatedSide(String regulatedSide) {
            NetworkDataframes.setTapChangerRegulatedSide(twt, side, regulatedSide, ThreeWindingsTransformer.Leg::getPhaseTapChanger);
        }
    }

    enum TapChangerType {
        RATIO,
        PHASE
    }

    private static Stream<TapChangerRow> getTapChangerRows(Network network, TapChangerType type) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(type);
        List<TapChangerRow> rows = new ArrayList<>();
        for (TwoWindingsTransformer twt : network.getTwoWindingsTransformers()) {
            switch (type) {
                case RATIO -> {
                    if (twt.getRatioTapChanger() != null) {
                        rows.add(new TapChangerRow2(twt));
                    }
                }
                case PHASE -> {
                    if (twt.getPhaseTapChanger() != null) {
                        rows.add(new TapChangerRow2(twt));
                    }
                }
            }

        }
        for (ThreeWindingsTransformer twt : network.getThreeWindingsTransformers()) {
            for (ThreeWindingsTransformer.Leg leg : twt.getLegs()) {
                switch (type) {
                    case RATIO -> {
                        if (leg.getRatioTapChanger() != null) {
                            rows.add(new TapChangerRow3(twt, leg.getSide()));
                        }
                    }
                    case PHASE -> {
                        if (leg.getPhaseTapChanger() != null) {
                            rows.add(new TapChangerRow3(twt, leg.getSide()));
                        }
                    }
                }
            }
        }
        return rows.stream();
    }

    static TapChangerRow getTapChangerRow(Network network, UpdatingDataframe dataframe, int index) {
        String id = dataframe.getStringValue("id", index)
                .orElseThrow(() -> new IllegalArgumentException("id column is missing"));
        ThreeSides side = dataframe.getStringValue("side", index).stream()
                .map(ThreeSides::valueOf)
                .findFirst()
                .orElse(null);
        if (side == null) {
            TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
            if (twt == null) {
                throw new PowsyblException("2 windings transformer '" + id + "' does not exist.");
            }
            return new TapChangerRow2(twt);
        }
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(id);
        if (twt == null) {
            throw new PowsyblException("3 windings transformer '" + id + "' does not exist.");
        }
        return new TapChangerRow3(twt, side);
    }

    private static NetworkDataframeMapper rtcs() {
        return NetworkDataframeMapperBuilder.ofStream(network -> NetworkDataframes.getTapChangerRows(network, TapChangerType.RATIO), NetworkDataframes::getTapChangerRow)
                .stringsIndex("id", TapChangerRow::getId)
                .strings("side", TapChangerRow::getSide)
                .ints("tap", row -> row.getRtc().getTapPosition(), (row, p) -> row.getRtc().setTapPosition(p))
                .ints("low_tap", row -> row.getRtc().getLowTapPosition())
                .ints("high_tap", row -> row.getRtc().getHighTapPosition())
                .ints("step_count", row -> row.getRtc().getStepCount())
                .booleans("on_load", row -> row.getRtc().hasLoadTapChangingCapabilities(), (row, v) -> row.getRtc().setLoadTapChangingCapabilities(v))
                .booleans(REGULATING, row -> row.getRtc().isRegulating(), (row, v) -> row.getRtc().setRegulating(v))
                .doubles("target_v", (row, context) -> getTransformerTargetV(row.getRtc(), context),
                        (row, targetV, context) -> setTransformerTargetV(row.getRtc(), targetV, context))
                .doubles("target_deadband", (row, context) -> row.getRtc().getTargetDeadband(),
                    (row, v, context) -> row.getRtc().setTargetDeadband(v))
                .strings("regulating_bus_id", row -> getBusId(row.getRtc().getRegulationTerminal()))
                .strings("regulated_side", TapChangerRow::getRtcRegulatedSide, TapChangerRow::setRtcRegulatedSide, false)
                .build();
    }

    private static void setTransformerTargetV(RatioTapChanger rtc, double targetV, NetworkDataframeContext context) {
        if (context.isPerUnit()) {
            if (rtc.getRegulationTerminal() != null) {
                rtc.setTargetV(unPerUnitV(context, targetV, rtc.getRegulationTerminal()));
            }
            // we are not able to per unit the target voltage as we don't know where is the regulated point and
            // so on it nominal voltage
        } else {
            rtc.setTargetV(targetV);
        }
    }

    private static double getTransformerTargetV(RatioTapChanger rtc, NetworkDataframeContext context) {
        if (context.isPerUnit()) {
            if (rtc.getRegulationTerminal() != null) {
                return perUnitV(context, rtc.getTargetV(), rtc.getRegulationTerminal());
            }
            // we are not able to per unit the target voltage as we don't know where is the regulated point and
            // so on it nominal voltage
            return Double.NaN;
        } else {
            return rtc.getTargetV();
        }
    }

    private static <T extends TapChanger<?, ?, ?, ?>> String getTapChangerRegulatedSide(TwoWindingsTransformer transformer, Function<TwoWindingsTransformer, T> tapChangerGetter) {
        return getTerminalSideStr(transformer, tapChangerGetter.apply(transformer).getRegulationTerminal());
    }

    private static <T extends TapChanger<?, ?, ?, ?>> void setTapChangerRegulatedSide(TwoWindingsTransformer transformer, String side, Function<TwoWindingsTransformer, T> tapChangerGetter) {
        tapChangerGetter.apply(transformer).setRegulationTerminal(getBranchTerminal(transformer, side));
    }

    private static <T extends TapChanger<?, ?, ?, ?>> String getTapChangerRegulatedSide(ThreeWindingsTransformer transformer, ThreeSides side, Function<ThreeWindingsTransformer.Leg, T> tapChangerGetter) {
        var rtc = tapChangerGetter.apply(transformer.getLeg(side));
        return getTerminalSideStr(transformer, rtc.getRegulationTerminal());
    }

    private static <T extends TapChanger<?, ?, ?, ?>> void setTapChangerRegulatedSide(ThreeWindingsTransformer transformer, ThreeSides side, String regulatedSide, Function<ThreeWindingsTransformer.Leg, T> tapChangerGetter) {
        var rtc = tapChangerGetter.apply(transformer.getLeg(side));
        if (regulatedSide.isEmpty()) {
            rtc.setRegulationTerminal(null);
        }
        rtc.setRegulationTerminal(transformer.getTerminal(ThreeSides.valueOf(regulatedSide)));
    }

    private static double computeRho(TwoWindingsTransformer twt) {
        return twt.getRatedU2() / twt.getRatedU1()
                * (twt.getRatioTapChanger() != null ? twt.getRatioTapChanger().getCurrentStep().getRho() : 1)
                * (twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getRho() : 1);
    }

    private static double computeRho(ThreeWindingsTransformer twt, ThreeSides side) {
        ThreeWindingsTransformer.Leg leg = twt.getLeg(side);
        return twt.getRatedU0() / leg.getRatedU()
                * (leg.getRatioTapChanger() != null ? leg.getRatioTapChanger().getCurrentStep().getRho() : 1)
                * (leg.getPhaseTapChanger() != null ? leg.getPhaseTapChanger().getCurrentStep().getRho() : 1);
    }

    private static double computeAlpha(TwoWindingsTransformer twt) {
        return twt.getPhaseTapChanger() != null ? twt.getPhaseTapChanger().getCurrentStep().getAlpha() : 0;
    }

    private static double computeAlpha(ThreeWindingsTransformer twt, ThreeSides side) {
        ThreeWindingsTransformer.Leg leg = twt.getLeg(side);
        return leg.getPhaseTapChanger() != null ? leg.getPhaseTapChanger().getCurrentStep().getAlpha() : 0;
    }

    private static double computeR(TwoWindingsTransformer twt) {
        return twt.getR()
                * (twt.getRatioTapChanger() != null ? (1 + twt.getRatioTapChanger().getCurrentStep().getR() / 100.0) : 1)
                * (twt.getPhaseTapChanger() != null ? (1 + twt.getPhaseTapChanger().getCurrentStep().getR() / 100.0) : 1);
    }

    private static double computeX(TwoWindingsTransformer twt) {
        return twt.getX()
                * (twt.getRatioTapChanger() != null ? (1 + twt.getRatioTapChanger().getCurrentStep().getX() / 100.0) : 1)
                * (twt.getPhaseTapChanger() != null ? (1 + twt.getPhaseTapChanger().getCurrentStep().getX() / 100.0) : 1);
    }

    private static double computeG(TwoWindingsTransformer twt) {
        return twt.getG()
                * (twt.getRatioTapChanger() != null ? (1 + twt.getRatioTapChanger().getCurrentStep().getG() / 100.0) : 1)
                * (twt.getPhaseTapChanger() != null ? (1 + twt.getPhaseTapChanger().getCurrentStep().getG() / 100.0) : 1);
    }

    private static double computeB(TwoWindingsTransformer twt) {
        return twt.getB()
                * (twt.getRatioTapChanger() != null ? (1 + twt.getRatioTapChanger().getCurrentStep().getB() / 100.0) : 1)
                * (twt.getPhaseTapChanger() != null ? (1 + twt.getPhaseTapChanger().getCurrentStep().getB() / 100.0) : 1);
    }

    private static double computeR(ThreeWindingsTransformer twt, ThreeSides side) {
        ThreeWindingsTransformer.Leg leg = twt.getLeg(side);
        return leg.getR()
                * (leg.getRatioTapChanger() != null ? (1 + leg.getRatioTapChanger().getCurrentStep().getR() / 100.0) : 1)
                * (leg.getPhaseTapChanger() != null ? (1 + leg.getPhaseTapChanger().getCurrentStep().getR() / 100.0) : 1);
    }

    private static double computeX(ThreeWindingsTransformer twt, ThreeSides side) {
        ThreeWindingsTransformer.Leg leg = twt.getLeg(side);
        return leg.getX()
                * (leg.getRatioTapChanger() != null ? (1 + leg.getRatioTapChanger().getCurrentStep().getX() / 100.0) : 1)
                * (leg.getPhaseTapChanger() != null ? (1 + leg.getPhaseTapChanger().getCurrentStep().getX() / 100.0) : 1);
    }

    private static double computeG(ThreeWindingsTransformer twt, ThreeSides side) {
        ThreeWindingsTransformer.Leg leg = twt.getLeg(side);
        return leg.getG()
                * (leg.getRatioTapChanger() != null ? (1 + leg.getRatioTapChanger().getCurrentStep().getG() / 100.0) : 1)
                * (leg.getPhaseTapChanger() != null ? (1 + leg.getPhaseTapChanger().getCurrentStep().getG() / 100.0) : 1);
    }

    private static double computeB(ThreeWindingsTransformer twt, ThreeSides side) {
        ThreeWindingsTransformer.Leg leg = twt.getLeg(side);
        return leg.getB()
                * (leg.getRatioTapChanger() != null ? (1 + leg.getRatioTapChanger().getCurrentStep().getB() / 100.0) : 1)
                * (leg.getPhaseTapChanger() != null ? (1 + leg.getPhaseTapChanger().getCurrentStep().getB() / 100.0) : 1);
    }

    private static NetworkDataframeMapper ptcs() {
        return NetworkDataframeMapperBuilder.ofStream(network -> NetworkDataframes.getTapChangerRows(network, TapChangerType.PHASE), NetworkDataframes::getTapChangerRow)
                .stringsIndex("id", TapChangerRow::getId)
                .strings("side", TapChangerRow::getSide)
                .ints("tap", t -> t.getPtc().getTapPosition(), (t, v) -> t.getPtc().setTapPosition(v))
                .ints("low_tap", t -> t.getPtc().getLowTapPosition())
                .ints("high_tap", t -> t.getPtc().getHighTapPosition())
                .ints("step_count", t -> t.getPtc().getStepCount())
                .booleans(REGULATING, t -> t.getPtc().isRegulating(), (t, v) -> t.getPtc().setRegulating(v))
                .enums("regulation_mode", PhaseTapChanger.RegulationMode.class, t -> t.getPtc().getRegulationMode(), (t, v) -> t.getPtc().setRegulationMode(v))
                .doubles("regulation_value", (t, context) -> t.getPtc().getRegulationValue(),
                    (t, v, context) -> t.getPtc().setRegulationValue(v))
                .doubles("target_deadband", (t, context) -> t.getPtc().getTargetDeadband(),
                    (t, v, context) -> t.getPtc().setTargetDeadband(v))
                .strings("regulating_bus_id", t -> getBusId(t.getPtc().getRegulationTerminal()))
                .strings("regulated_side", TapChangerRow::getPtcRegulatedSide, TapChangerRow::setPtcRegulatedSide, false)
                .build();
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
                .doubles("value", (limit, context) -> perUnitLimitValue(context, limit))
                .ints("acceptable_duration", TemporaryLimitData::getAcceptableDuration)
                .booleans("fictitious", TemporaryLimitData::isFictitious, false)
                .strings("group_name", TemporaryLimitData::getGroupId, false)
                .booleans("selected", TemporaryLimitData::isSelected, false)
                .build();
    }

    private static double perUnitLimitValue(NetworkDataframeContext context, TemporaryLimitData limit) {
        return switch (limit.getType()) {
            case CURRENT ->
                perUnitI(context, limit.getValue(), limit.getPerUnitingNominalV());
            case ACTIVE_POWER, APPARENT_POWER ->
                perUnitPQ(context, limit.getValue());
            case VOLTAGE ->
                perUnitV(context, limit.getValue(), limit.getPerUnitingNominalV());
            case VOLTAGE_ANGLE ->
                perUnitAngle(context, limit.getValue());
        };
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
        if (!busId.isEmpty()) {
            Objects.requireNonNull(t).getBusBreakerView().setConnectableBus(busId);
        }
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
                .strings("type", pair -> pair.getLeft().getType().toString())
                .strings("alias", Pair::getRight)
                .strings("alias_type", pair -> pair.getLeft().getAliasType(pair.getRight()).orElse(""))
                .build();
    }

    private static Stream<Pair<Identifiable<?>, String>> getAliasesData(Network network) {
        return network.getIdentifiables().stream()
                .flatMap(identifiable -> identifiable.getAliases().stream()
                        .map(alias -> Pair.of(identifiable, alias)));
    }

    private static NetworkDataframeMapper properties() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkDataframes::getPropertiesData)
                .stringsIndex("id", pair -> pair.getLeft().getId())
                .strings("type", pair -> pair.getLeft().getType().toString())
                .strings("key", Pair::getRight)
                .strings("value", pair -> pair.getLeft().getProperty(pair.getRight()))
                .build();
    }

    private static Stream<Pair<Identifiable<?>, String>> getPropertiesData(Network network) {
        return network.getIdentifiables().stream()
                .flatMap(identifiable -> identifiable.getPropertyNames().stream()
                        .map(prop -> Pair.of(identifiable, prop)));
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

