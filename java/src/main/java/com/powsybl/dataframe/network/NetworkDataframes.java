/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.BooleanSeriesMapper;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.DoubleSeriesMapper.DoubleUpdater;
import com.powsybl.iidm.network.*;
import com.powsybl.python.NetworkUtil;
import com.powsybl.python.TemporaryLimitContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import static com.powsybl.dataframe.MappingUtils.ifExistsDouble;
import static com.powsybl.dataframe.MappingUtils.ifExistsInt;

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
    private static final Map<DataframeElementType, NetworkDataframeMapper> BUS_BREAKER_VIEW_MAPPERS = createMappersBusBreakerView();

    private NetworkDataframes() {
    }

    public static NetworkDataframeMapper getDataframeMapper(DataframeElementType type) {
        return getDataframeMapper(type, false);
    }

    public static NetworkDataframeMapper getDataframeMapper(DataframeElementType type, boolean busBreakerView) {
        return busBreakerView ? BUS_BREAKER_VIEW_MAPPERS.get(type) : MAPPERS.get(type);
    }

    private static Map<DataframeElementType, NetworkDataframeMapper> createMappers() {
        Map<DataframeElementType, NetworkDataframeMapper> mappers = new EnumMap<>(DataframeElementType.class);
        mappers.put(DataframeElementType.BUS, buses(false));
        mappers.put(DataframeElementType.LINE, lines(false));
        mappers.put(DataframeElementType.TWO_WINDINGS_TRANSFORMER, twoWindingTransformers(false));
        mappers.put(DataframeElementType.THREE_WINDINGS_TRANSFORMER, threeWindingTransformers(false));
        mappers.put(DataframeElementType.GENERATOR, generators(false));
        mappers.put(DataframeElementType.LOAD, loads(false));
        mappers.put(DataframeElementType.BATTERY, batteries(false));
        mappers.put(DataframeElementType.SHUNT_COMPENSATOR, shunts(false));
        mappers.put(DataframeElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, shuntsNonLinear());
        mappers.put(DataframeElementType.DANGLING_LINE, danglingLines(false));
        mappers.put(DataframeElementType.LCC_CONVERTER_STATION, lccs(false));
        mappers.put(DataframeElementType.VSC_CONVERTER_STATION, vscs(false));
        mappers.put(DataframeElementType.STATIC_VAR_COMPENSATOR, svcs(false));
        mappers.put(DataframeElementType.SWITCH, switches());
        mappers.put(DataframeElementType.VOLTAGE_LEVEL, voltageLevels());
        mappers.put(DataframeElementType.SUBSTATION, substations());
        mappers.put(DataframeElementType.BUSBAR_SECTION, busBars());
        mappers.put(DataframeElementType.HVDC_LINE, hvdcs());
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER_STEP, rtcSteps());
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER_STEP, ptcSteps());
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER, rtcs(false));
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER, ptcs(false));
        mappers.put(DataframeElementType.CURRENT_LIMITS, currentLimits());
        mappers.put(DataframeElementType.REACTIVE_CAPABILITY_CURVE_POINT, reactiveCapabilityCurves());
        return Collections.unmodifiableMap(mappers);
    }

    private static Map<DataframeElementType, NetworkDataframeMapper> createMappersBusBreakerView() {
        Map<DataframeElementType, NetworkDataframeMapper> mappers = new EnumMap<>(DataframeElementType.class);
        mappers.put(DataframeElementType.BUS, buses(true));
        mappers.put(DataframeElementType.LINE, lines(true));
        mappers.put(DataframeElementType.TWO_WINDINGS_TRANSFORMER, twoWindingTransformers(true));
        mappers.put(DataframeElementType.THREE_WINDINGS_TRANSFORMER, threeWindingTransformers(true));
        mappers.put(DataframeElementType.GENERATOR, generators(true));
        mappers.put(DataframeElementType.LOAD, loads(true));
        mappers.put(DataframeElementType.BATTERY, batteries(true));
        mappers.put(DataframeElementType.SHUNT_COMPENSATOR, shunts(true));
        mappers.put(DataframeElementType.DANGLING_LINE, danglingLines(true));
        mappers.put(DataframeElementType.LCC_CONVERTER_STATION, lccs(true));
        mappers.put(DataframeElementType.VSC_CONVERTER_STATION, vscs(true));
        mappers.put(DataframeElementType.STATIC_VAR_COMPENSATOR, svcs(true));
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER, rtcs(true));
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER, ptcs(true));
        return Collections.unmodifiableMap(mappers);
    }

    static <U extends Injection<U>> ToDoubleFunction<U> getP() {
        return inj -> inj.getTerminal().getP();
    }

    static <U extends Injection<U>> ToDoubleFunction<U> getQ() {
        return inj -> inj.getTerminal().getQ();
    }

    static <U extends Injection<U>> DoubleUpdater<U> setP() {
        return (inj, p) -> inj.getTerminal().setP(p);
    }

    static <U extends Injection<U>> DoubleUpdater<U> setQ() {
        return (inj, q) -> inj.getTerminal().setQ(q);
    }

    static <U extends Branch<U>> ToDoubleFunction<U> getP1() {
        return b -> b.getTerminal1().getP();
    }

    static <U extends Branch<U>> ToDoubleFunction<U> getQ1() {
        return b -> b.getTerminal1().getQ();
    }

    static <U extends Branch<U>> DoubleUpdater<U> setP1() {
        return (b, p) -> b.getTerminal1().setP(p);
    }

    static <U extends Branch<U>> DoubleUpdater<U> setQ1() {
        return (b, q) -> b.getTerminal1().setQ(q);
    }

    static <U extends Branch<U>> ToDoubleFunction<U> getP2() {
        return b -> b.getTerminal2().getP();
    }

    static <U extends Branch<U>> ToDoubleFunction<U> getQ2() {
        return b -> b.getTerminal2().getQ();
    }

    static <U extends Branch<U>> DoubleUpdater<U> setP2() {
        return (b, p) -> b.getTerminal2().setP(p);
    }

    static <U extends Branch<U>> DoubleUpdater<U> setQ2() {
        return (b, q) -> b.getTerminal2().setQ(q);
    }

    static <U extends Injection<U>> Function<U, String> getVoltageLevelId() {
        return inj -> inj.getTerminal().getVoltageLevel().getId();
    }

    private static MinMaxReactiveLimits getMinMaxReactiveLimits(ReactiveLimitsHolder holder) {
        ReactiveLimits reactiveLimits = holder.getReactiveLimits();
        return reactiveLimits instanceof MinMaxReactiveLimits ? (MinMaxReactiveLimits) reactiveLimits : null;
    }

    private static <U extends Injection<U>> BooleanSeriesMapper.BooleanUpdater<U> connectInjection() {
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

    protected static NetworkDataframeMapper generators(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getGeneratorStream, getOrThrow(Network::getGenerator, "Generator"))
                .stringsIndex("id", Generator::getId)
                .enums("energy_source", EnergySource.class, Generator::getEnergySource)
                .doubles("target_p", Generator::getTargetP, Generator::setTargetP)
                .doubles("min_p", Generator::getMinP, Generator::setMinP)
                .doubles("max_p", Generator::getMaxP, Generator::setMaxP)
                .doubles("min_q", ifExistsDouble(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ))
                .doubles("max_q", ifExistsDouble(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ))
                .doubles("target_v", Generator::getTargetV, Generator::setTargetV)
                .doubles("target_q", Generator::getTargetQ, Generator::setTargetQ)
                .booleans("voltage_regulator_on", Generator::isVoltageRegulatorOn, Generator::setVoltageRegulatorOn)
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", g -> g.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", g -> busIdGetter(g.getTerminal(), busBreakerView))
                .booleans("connected", g -> g.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper buses(boolean busBreakerView) {
        Function<Network, Stream<Bus>> busesFunction = busBreakerView ? n -> n.getBusBreakerView().getBusStream() : n -> n.getBusView().getBusStream();
        BiFunction<Network, String, Bus> getter = busBreakerView ? (n, id) -> n.getBusBreakerView().getBus(id) : (n, id) -> n.getBusView().getBus(id);
        return NetworkDataframeMapperBuilder.ofStream(busesFunction,
                getOrThrow(getter, "Bus"))
                .stringsIndex("id", Bus::getId)
                .doubles("v_mag", Bus::getV, Bus::setV)
                .doubles("v_angle", Bus::getAngle, Bus::setAngle)
                .ints("connected_component", ifExistsInt(Bus::getConnectedComponent, Component::getNum))
                .ints("synchronous_component", ifExistsInt(Bus::getSynchronousComponent, Component::getNum))
                .strings("voltage_level_id", b -> b.getVoltageLevel().getId())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper loads(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getLoadStream, getOrThrow(Network::getLoad, "Load"))
                .stringsIndex("id", Load::getId)
                .enums("type", LoadType.class, Load::getLoadType)
                .doubles("p0", Load::getP0, Load::setP0)
                .doubles("q0", Load::getQ0, Load::setQ0)
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", l -> l.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", l -> busIdGetter(l.getTerminal(), busBreakerView))
                .booleans("connected", l -> l.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper batteries(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getBatteryStream, getOrThrow(Network::getBattery, "Battery"))
                .stringsIndex("id", Battery::getId)
                .doubles("max_p", Battery::getMaxP, Battery::setMaxP)
                .doubles("min_p", Battery::getMinP, Battery::setMinP)
                .doubles("p0", Battery::getP0, Battery::setP0)
                .doubles("q0", Battery::getQ0, Battery::setQ0)
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", b -> b.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", b -> busIdGetter(b.getTerminal(), busBreakerView))
                .booleans("connected", b -> b.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper shunts(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getShuntCompensatorStream, getOrThrow(Network::getShuntCompensator, "Shunt compensator"))
                .stringsIndex("id", ShuntCompensator::getId)
                .doubles("g", ShuntCompensator::getG)
                .doubles("b", ShuntCompensator::getB)
                .enums("model_type", ShuntCompensatorModelType.class, ShuntCompensator::getModelType)
                .ints("max_section_count", ShuntCompensator::getMaximumSectionCount)
                .ints("section_count", ShuntCompensator::getSectionCount, ShuntCompensator::setSectionCount)
                .booleans("voltage_regulation_on", ShuntCompensator::isVoltageRegulatorOn, ShuntCompensator::setVoltageRegulatorOn)
                .doubles("target_v", ShuntCompensator::getTargetV, ShuntCompensator::setTargetV)
                .doubles("target_deadband", ShuntCompensator::getTargetDeadband, ShuntCompensator::setTargetDeadband)
                .strings("regulating_bus_id", s -> busIdGetter(s.getRegulatingTerminal(), busBreakerView))
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", s -> s.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", s -> busIdGetter(s.getTerminal(), busBreakerView))
                .booleans("connected", s -> s.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper shuntsNonLinear() {
        Function<Network, Stream<Triple<String, ShuntCompensatorNonLinearModel.Section, Integer>>> nonLinearShunts = network ->
                network.getShuntCompensatorStream()
                        .filter(sc -> sc.getModelType() == ShuntCompensatorModelType.NON_LINEAR)
                        .flatMap(shuntCompensator -> {
                            ShuntCompensatorNonLinearModel model = (ShuntCompensatorNonLinearModel) shuntCompensator.getModel();
                            return model.getAllSections().stream().map(section -> Triple.of(shuntCompensator.getId(), section, model.getAllSections().indexOf(section)));
                        });
        return NetworkDataframeMapperBuilder.ofStream(nonLinearShunts)
                .stringsIndex("id", Triple::getLeft)
                .intsIndex("section", Triple::getRight)
                .doubles("g", snl -> snl.getMiddle().getG())
                .doubles("b", snl -> snl.getMiddle().getB())
                .build();
    }

    private static NetworkDataframeMapper lines(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getLineStream, getOrThrow(Network::getLine, "Line"))
                .stringsIndex("id", Line::getId)
                .doubles("r", Line::getR, Line::setR)
                .doubles("x", Line::getX, Line::setX)
                .doubles("g1", Line::getG1, Line::setG1)
                .doubles("b1", Line::getB1, Line::setB1)
                .doubles("g2", Line::getG2, Line::setG2)
                .doubles("b2", Line::getB2, Line::setB2)
                .doubles("p1", getP1(), setP1())
                .doubles("q1", getQ1(), setQ1())
                .doubles("i1", l -> l.getTerminal1().getI())
                .doubles("p2", getP2(), setP2())
                .doubles("q2", getQ2(), setQ2())
                .doubles("i2", l -> l.getTerminal2().getI())
                .strings("voltage_level1_id", l -> l.getTerminal1().getVoltageLevel().getId())
                .strings("voltage_level2_id", l -> l.getTerminal2().getVoltageLevel().getId())
                .strings("bus1_id", l -> busIdGetter(l.getTerminal1(), busBreakerView))
                .strings("bus2_id", l -> busIdGetter(l.getTerminal2(), busBreakerView))
                .booleans("connected1", l -> l.getTerminal1().isConnected(), connectBranchSide1())
                .booleans("connected2", l -> l.getTerminal2().isConnected(), connectBranchSide2())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper twoWindingTransformers(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getTwoWindingsTransformerStream, getOrThrow(Network::getTwoWindingsTransformer, "Two windings transformer"))
                .stringsIndex("id", TwoWindingsTransformer::getId)
                .doubles("r", TwoWindingsTransformer::getR, TwoWindingsTransformer::setR)
                .doubles("x", TwoWindingsTransformer::getX, TwoWindingsTransformer::setX)
                .doubles("g", TwoWindingsTransformer::getG, TwoWindingsTransformer::setG)
                .doubles("b", TwoWindingsTransformer::getB, TwoWindingsTransformer::setB)
                .doubles("rated_u1", TwoWindingsTransformer::getRatedU1, TwoWindingsTransformer::setRatedU1)
                .doubles("rated_u2", TwoWindingsTransformer::getRatedU2, TwoWindingsTransformer::setRatedU2)
                .doubles("rated_s", TwoWindingsTransformer::getRatedS, TwoWindingsTransformer::setRatedS)
                .doubles("p1", getP1(), setP1())
                .doubles("q1", getQ1(), setQ1())
                .doubles("i1", twt -> twt.getTerminal1().getI())
                .doubles("p2", getP2(), setP2())
                .doubles("q2", getQ2(), setQ2())
                .doubles("i2", twt -> twt.getTerminal2().getI())
                .strings("voltage_level1_id", twt -> twt.getTerminal1().getVoltageLevel().getId())
                .strings("voltage_level2_id", twt -> twt.getTerminal2().getVoltageLevel().getId())
                .strings("bus1_id", twt -> busIdGetter(twt.getTerminal1(), busBreakerView))
                .strings("bus2_id", twt -> busIdGetter(twt.getTerminal2(), busBreakerView))
                .booleans("connected1", twt -> twt.getTerminal1().isConnected(), connectBranchSide1())
                .booleans("connected2", twt -> twt.getTerminal2().isConnected(), connectBranchSide2())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper threeWindingTransformers(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getThreeWindingsTransformerStream, getOrThrow(Network::getThreeWindingsTransformer, "Three windings transformer"))
                .stringsIndex("id", ThreeWindingsTransformer::getId)
                .doubles("rated_u0", ThreeWindingsTransformer::getRatedU0)
                .doubles("r1", twt -> twt.getLeg1().getR(), (twt, v) -> twt.getLeg1().setR(v))
                .doubles("x1", twt -> twt.getLeg1().getX(), (twt, v) -> twt.getLeg1().setX(v))
                .doubles("g1", twt -> twt.getLeg1().getG(), (twt, v) -> twt.getLeg1().setG(v))
                .doubles("b1", twt -> twt.getLeg1().getB(), (twt, v) -> twt.getLeg1().setB(v))
                .doubles("rated_u1", twt -> twt.getLeg1().getRatedU(), (twt, v) -> twt.getLeg1().setRatedU(v))
                .doubles("rated_s1", twt -> twt.getLeg1().getRatedS(), (twt, v) -> twt.getLeg1().setRatedS(v))
                .ints("ratio_tap_position1", getRatioTapPosition(t -> t.getLeg1()), (t, v) -> setTapPosition(t.getLeg1().getRatioTapChanger(), v))
                .ints("phase_tap_position1", getPhaseTapPosition(t -> t.getLeg1()), (t, v) -> setTapPosition(t.getLeg1().getPhaseTapChanger(), v))
                .doubles("p1", twt -> twt.getLeg1().getTerminal().getP(), (twt, v) -> twt.getLeg1().getTerminal().setP(v))
                .doubles("q1", twt -> twt.getLeg1().getTerminal().getQ(), (twt, v) -> twt.getLeg1().getTerminal().setQ(v))
                .doubles("i1", twt -> twt.getLeg1().getTerminal().getI())
                .strings("voltage_level1_id", twt -> twt.getLeg1().getTerminal().getVoltageLevel().getId())
                .strings("bus1_id", twt -> busIdGetter(twt.getLeg1().getTerminal(), busBreakerView))
                .booleans("connected1", twt -> twt.getLeg1().getTerminal().isConnected())
                .doubles("r2", twt -> twt.getLeg2().getR(), (twt, v) -> twt.getLeg2().setR(v))
                .doubles("x2", twt -> twt.getLeg2().getX(), (twt, v) -> twt.getLeg2().setX(v))
                .doubles("g2", twt -> twt.getLeg2().getG(), (twt, v) -> twt.getLeg2().setG(v))
                .doubles("b2", twt -> twt.getLeg2().getB(), (twt, v) -> twt.getLeg2().setB(v))
                .doubles("rated_u2", twt -> twt.getLeg2().getRatedU(), (twt, v) -> twt.getLeg2().setRatedU(v))
                .doubles("rated_s2", twt -> twt.getLeg2().getRatedS(), (twt, v) -> twt.getLeg2().setRatedS(v))
                .ints("ratio_tap_position2", getRatioTapPosition(t -> t.getLeg2()), (t, v) -> setTapPosition(t.getLeg2().getRatioTapChanger(), v))
                .ints("phase_tap_position2", getPhaseTapPosition(t -> t.getLeg2()), (t, v) -> setTapPosition(t.getLeg2().getPhaseTapChanger(), v))
                .doubles("p2", twt -> twt.getLeg2().getTerminal().getP(), (twt, v) -> twt.getLeg2().getTerminal().setP(v))
                .doubles("q2", twt -> twt.getLeg2().getTerminal().getQ(), (twt, v) -> twt.getLeg2().getTerminal().setQ(v))
                .doubles("i2", twt -> twt.getLeg2().getTerminal().getI())
                .strings("voltage_level2_id", twt -> twt.getLeg2().getTerminal().getVoltageLevel().getId())
                .strings("bus2_id", twt -> busIdGetter(twt.getLeg2().getTerminal(), busBreakerView))
                .booleans("connected2", twt -> twt.getLeg2().getTerminal().isConnected())
                .doubles("r3", twt -> twt.getLeg3().getR(), (twt, v) -> twt.getLeg3().setR(v))
                .doubles("x3", twt -> twt.getLeg3().getX(), (twt, v) -> twt.getLeg3().setX(v))
                .doubles("g3", twt -> twt.getLeg3().getG(), (twt, v) -> twt.getLeg3().setG(v))
                .doubles("b3", twt -> twt.getLeg3().getB(), (twt, v) -> twt.getLeg3().setB(v))
                .doubles("rated_u3", twt -> twt.getLeg3().getRatedU(), (twt, v) -> twt.getLeg3().setRatedU(v))
                .doubles("rated_s3", twt -> twt.getLeg3().getRatedS(), (twt, v) -> twt.getLeg3().setRatedS(v))
                .ints("ratio_tap_position3", getRatioTapPosition(t -> t.getLeg3()), (t, v) -> setTapPosition(t.getLeg3().getRatioTapChanger(), v))
                .ints("phase_tap_position3", getPhaseTapPosition(t -> t.getLeg3()), (t, v) -> setTapPosition(t.getLeg3().getPhaseTapChanger(), v))
                .doubles("p3", twt -> twt.getLeg3().getTerminal().getP(), (twt, v) -> twt.getLeg3().getTerminal().setP(v))
                .doubles("q3", twt -> twt.getLeg3().getTerminal().getQ(), (twt, v) -> twt.getLeg3().getTerminal().setQ(v))
                .doubles("i3", twt -> twt.getLeg3().getTerminal().getI())
                .strings("voltage_level3_id", twt -> twt.getLeg3().getTerminal().getVoltageLevel().getId())
                .strings("bus3_id", twt -> busIdGetter(twt.getLeg3().getTerminal(), busBreakerView))
                .booleans("connected3", twt -> twt.getLeg3().getTerminal().isConnected())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper danglingLines(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getDanglingLineStream, getOrThrow(Network::getDanglingLine, "Dangling line"))
                .stringsIndex("id", DanglingLine::getId)
                .doubles("r", DanglingLine::getR, DanglingLine::setR)
                .doubles("x", DanglingLine::getX, DanglingLine::setX)
                .doubles("g", DanglingLine::getG, DanglingLine::setG)
                .doubles("b", DanglingLine::getB, DanglingLine::setB)
                .doubles("p0", DanglingLine::getP0, DanglingLine::setP0)
                .doubles("q0", DanglingLine::getQ0, DanglingLine::setQ0)
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", dl -> dl.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", dl -> busIdGetter(dl.getTerminal(), busBreakerView))
                .booleans("connected", dl -> dl.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper lccs(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getLccConverterStationStream, getOrThrow(Network::getLccConverterStation, "LCC converter station"))
                .stringsIndex("id", LccConverterStation::getId)
                .doubles("power_factor", LccConverterStation::getPowerFactor, (lcc, v) -> lcc.setPowerFactor((float) v))
                .doubles("loss_factor", LccConverterStation::getLossFactor, (lcc, v) -> lcc.setLossFactor((float) v))
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", st -> st.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", st -> busIdGetter(st.getTerminal(), busBreakerView))
                .booleans("connected", st -> st.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper vscs(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getVscConverterStationStream, getOrThrow(Network::getVscConverterStation, "VSC converter station"))
                .stringsIndex("id", VscConverterStation::getId)
                .doubles("voltage_setpoint", VscConverterStation::getVoltageSetpoint, VscConverterStation::setVoltageSetpoint)
                .doubles("reactive_power_setpoint", VscConverterStation::getReactivePowerSetpoint, VscConverterStation::setReactivePowerSetpoint)
                .booleans("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn, VscConverterStation::setVoltageRegulatorOn)
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", vsc -> vsc.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", vsc -> busIdGetter(vsc.getTerminal(), busBreakerView))
                .booleans("connected", vsc -> vsc.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    private static NetworkDataframeMapper svcs(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(Network::getStaticVarCompensatorStream, getOrThrow(Network::getStaticVarCompensator, "Static var compensator"))
                .stringsIndex("id", StaticVarCompensator::getId)
                .doubles("voltage_setpoint", StaticVarCompensator::getVoltageSetpoint, StaticVarCompensator::setVoltageSetpoint)
                .doubles("reactive_power_setpoint", StaticVarCompensator::getReactivePowerSetpoint, StaticVarCompensator::setReactivePowerSetpoint)
                .enums("regulation_mode", StaticVarCompensator.RegulationMode.class,
                        StaticVarCompensator::getRegulationMode, StaticVarCompensator::setRegulationMode)
                .doubles("p", getP(), setP())
                .doubles("q", getQ(), setQ())
                .doubles("i", st -> st.getTerminal().getI())
                .strings("voltage_level_id", getVoltageLevelId())
                .strings("bus_id", svc -> busIdGetter(svc.getTerminal(), busBreakerView))
                .booleans("connected", svc -> svc.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper switches() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getSwitchStream, getOrThrow(Network::getSwitch, "Switch"))
                .stringsIndex("id", Switch::getId)
                .enums("kind", SwitchKind.class, Switch::getKind)
                .booleans("open", Switch::isOpen, Switch::setOpen)
                .booleans("retained", Switch::isRetained, Switch::setRetained)
                .strings("voltage_level_id", s -> s.getVoltageLevel().getId())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper voltageLevels() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getVoltageLevelStream, getOrThrow(Network::getVoltageLevel, "Voltage level"))
                .stringsIndex("id", VoltageLevel::getId)
                .strings("substation_id", vl -> vl.getSubstation().getId())
                .doubles("nominal_v", VoltageLevel::getNominalV, VoltageLevel::setNominalV)
                .doubles("high_voltage_limit", VoltageLevel::getHighVoltageLimit, VoltageLevel::setHighVoltageLimit)
                .doubles("low_voltage_limit", VoltageLevel::getLowVoltageLimit, VoltageLevel::setLowVoltageLimit)
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper substations() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getSubstationStream, getOrThrow(Network::getSubstation, "Substation"))
                .stringsIndex("id", Identifiable::getId)
                .strings("TSO", Substation::getTso, Substation::setTso)
                .strings("geo_tags", substation -> String.join(",", substation.getGeographicalTags()))
                .enums("country", Country.class, s -> s.getCountry().orElse(null), Substation::setCountry)
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper busBars() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getBusbarSectionStream, getOrThrow(Network::getBusbarSection, "Bus bar section"))
                .stringsIndex("id", BusbarSection::getId)
                .booleans("fictitious", BusbarSection::isFictitious, BusbarSection::setFictitious)
                .doubles("v", BusbarSection::getV)
                .doubles("angle", BusbarSection::getAngle)
                .strings("voltage_level_id", bbs -> bbs.getTerminal().getVoltageLevel().getId())
                .booleans("connected", g -> g.getTerminal().isConnected(), connectInjection())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper hvdcs() {
        return NetworkDataframeMapperBuilder.ofStream(Network::getHvdcLineStream, getOrThrow(Network::getHvdcLine, "HVDC line"))
                .stringsIndex("id", HvdcLine::getId)
                .enums("converters_mode", HvdcLine.ConvertersMode.class, HvdcLine::getConvertersMode, HvdcLine::setConvertersMode)
                .doubles("active_power_setpoint", HvdcLine::getActivePowerSetpoint, HvdcLine::setActivePowerSetpoint)
                .doubles("max_p", HvdcLine::getMaxP, HvdcLine::setMaxP)
                .doubles("nominal_v", HvdcLine::getNominalV, HvdcLine::setNominalV)
                .doubles("r", HvdcLine::getR, HvdcLine::setR)
                .strings("converter_station1_id", l -> l.getConverterStation1().getId())
                .strings("converter_station2_id", l -> l.getConverterStation2().getId())
                .booleans("connected1", g -> g.getConverterStation1().getTerminal().isConnected(), connectHvdcStation1())
                .booleans("connected2", g -> g.getConverterStation2().getTerminal().isConnected(), connectHvdcStation2())
                .addProperties()
                .build();
    }

    protected static NetworkDataframeMapper rtcSteps() {
        Function<Network, Stream<Triple<String, RatioTapChanger, Integer>>> ratioTapChangerSteps = network ->
                network.getTwoWindingsTransformerStream()
                        .filter(twt -> twt.getRatioTapChanger() != null)
                        .flatMap(twt -> twt.getRatioTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getRatioTapChanger(), position)));
        return NetworkDataframeMapperBuilder.ofStream(ratioTapChangerSteps)
                .stringsIndex("id", Triple::getLeft)
                .intsIndex("position", Triple::getRight)
                .doubles("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
                .doubles("r", p -> p.getMiddle().getStep(p.getRight()).getR())
                .doubles("x", p -> p.getMiddle().getStep(p.getRight()).getX())
                .doubles("g", p -> p.getMiddle().getStep(p.getRight()).getG())
                .doubles("b", p -> p.getMiddle().getStep(p.getRight()).getB())
                .build();
    }

    protected static NetworkDataframeMapper ptcSteps() {
        Function<Network, Stream<Triple<String, PhaseTapChanger, Integer>>> phaseTapChangerSteps = network ->
                network.getTwoWindingsTransformerStream()
                        .filter(twt -> twt.getPhaseTapChanger() != null)
                        .flatMap(twt -> twt.getPhaseTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getPhaseTapChanger(), position)));
        return NetworkDataframeMapperBuilder.ofStream(phaseTapChangerSteps)
                .stringsIndex("id", Triple::getLeft)
                .intsIndex("position", Triple::getRight)
                .doubles("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
                .doubles("alpha", p -> p.getMiddle().getStep(p.getRight()).getAlpha())
                .doubles("r", p -> p.getMiddle().getStep(p.getRight()).getR())
                .doubles("x", p -> p.getMiddle().getStep(p.getRight()).getX())
                .doubles("g", p -> p.getMiddle().getStep(p.getRight()).getG())
                .doubles("b", p -> p.getMiddle().getStep(p.getRight()).getB())
                .build();
    }

    private static NetworkDataframeMapper rtcs(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getTwoWindingsTransformerStream()
                .filter(t -> t.getRatioTapChanger() != null), NetworkDataframes::getT2OrThrow)
                .stringsIndex("id", TwoWindingsTransformer::getId)
                .ints("tap", t -> t.getRatioTapChanger().getTapPosition(), (t, p) -> t.getRatioTapChanger().setTapPosition(p))
                .ints("low_tap", t -> t.getRatioTapChanger().getLowTapPosition())
                .ints("high_tap", t -> t.getRatioTapChanger().getHighTapPosition())
                .ints("step_count", t -> t.getRatioTapChanger().getStepCount())
                .booleans("on_load", t -> t.getRatioTapChanger().hasLoadTapChangingCapabilities(), (t, v) -> t.getRatioTapChanger().setLoadTapChangingCapabilities(v))
                .booleans("regulating", t -> t.getRatioTapChanger().isRegulating(), (t, v) -> t.getRatioTapChanger().setRegulating(v))
                .doubles("target_v", t -> t.getRatioTapChanger().getTargetV(), (t, v) -> t.getRatioTapChanger().setTargetV(v))
                .doubles("target_deadband", t -> t.getRatioTapChanger().getTargetDeadband(), (t, v) -> t.getRatioTapChanger().setTargetDeadband(v))
                .strings("regulating_bus_id", t -> busIdGetter(t.getRatioTapChanger().getRegulationTerminal(), busBreakerView))
                .doubles("rho", NetworkDataframes::computeRho)
                .doubles("alpha", ifExistsDouble(TwoWindingsTransformer::getPhaseTapChanger, pc -> pc.getCurrentStep().getAlpha()))
                .build();
    }

    private static double computeRho(TwoWindingsTransformer twoWindingsTransformer) {
        return twoWindingsTransformer.getRatedU2() / twoWindingsTransformer.getRatedU1()
                * (twoWindingsTransformer.getRatioTapChanger() != null ? twoWindingsTransformer.getRatioTapChanger().getCurrentStep().getRho() : 1)
                * (twoWindingsTransformer.getPhaseTapChanger() != null ? twoWindingsTransformer.getPhaseTapChanger().getCurrentStep().getRho() : 1);
    }

    private static NetworkDataframeMapper ptcs(boolean busBreakerView) {
        return NetworkDataframeMapperBuilder.ofStream(network -> network.getTwoWindingsTransformerStream()
                .filter(t -> t.getPhaseTapChanger() != null), NetworkDataframes::getT2OrThrow)
                .stringsIndex("id", TwoWindingsTransformer::getId)
                .ints("tap", t -> t.getPhaseTapChanger().getTapPosition(), (t, v) -> t.getPhaseTapChanger().setTapPosition(v))
                .ints("low_tap", t -> t.getPhaseTapChanger().getLowTapPosition())
                .ints("high_tap", t -> t.getPhaseTapChanger().getHighTapPosition())
                .ints("step_count", t -> t.getPhaseTapChanger().getStepCount())
                .booleans("regulating", t -> t.getPhaseTapChanger().isRegulating(), (t, v) -> t.getPhaseTapChanger().setRegulating(v))
                .enums("regulation_mode", PhaseTapChanger.RegulationMode.class, t -> t.getPhaseTapChanger().getRegulationMode(), (t, v) -> t.getPhaseTapChanger().setRegulationMode(v))
                .doubles("regulation_value", t -> t.getPhaseTapChanger().getRegulationValue(), (t, v) -> t.getPhaseTapChanger().setRegulationValue(v))
                .doubles("target_deadband", t -> t.getPhaseTapChanger().getTargetDeadband(), (t, v) -> t.getPhaseTapChanger().setTargetDeadband(v))
                .strings("regulating_bus_id", t -> busIdGetter(t.getPhaseTapChanger().getRegulationTerminal(), busBreakerView))
                .build();
    }

    protected static NetworkDataframeMapper currentLimits() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkUtil::getCurrentLimits)
                .stringsIndex("branch_id", TemporaryLimitContext::getBranchId)
                .stringsIndex("name", TemporaryLimitContext::getName)
                .enums("side", Branch.Side.class, TemporaryLimitContext::getSide)
                .doubles("value", TemporaryLimitContext::getValue)
                .ints("acceptable_duration", TemporaryLimitContext::getAcceptableDuration)
                .booleans("is_fictitious", TemporaryLimitContext::isFictitious)
                .build();
    }

    private static Stream<Pair<String, ReactiveLimitsHolder>> streamReactiveLimitsHolder(Network network) {
        return Stream.concat(network.getGeneratorStream().map(g -> Pair.of(g.getId(), g)),
                network.getVscConverterStationStream().map(g -> Pair.of(g.getId(), g)));
    }

    private static Stream<Triple<String, Integer, ReactiveCapabilityCurve.Point>> streamPoints(Network network) {
        return streamReactiveLimitsHolder(network)
                .filter(p -> p.getRight().getReactiveLimits() instanceof ReactiveCapabilityCurve)
                .flatMap(p -> indexPoints(p.getLeft(), p.getRight()).stream());
    }

    private static List<Triple<String, Integer, ReactiveCapabilityCurve.Point>> indexPoints(String id, ReactiveLimitsHolder holder) {
        ReactiveCapabilityCurve curve = (ReactiveCapabilityCurve) holder.getReactiveLimits();
        List<Triple<String, Integer, ReactiveCapabilityCurve.Point>> values = new ArrayList<>(curve.getPointCount());
        int num = 0;
        for (ReactiveCapabilityCurve.Point point : curve.getPoints()) {
            values.add(Triple.of(id, num, point));
            num++;
        }
        return values;
    }

    private static NetworkDataframeMapper reactiveCapabilityCurves() {
        return NetworkDataframeMapperBuilder.ofStream(NetworkDataframes::streamPoints)
                .stringsIndex("id", Triple::getLeft)
                .intsIndex("num", Triple::getMiddle)
                .doubles("p", t -> t.getRight().getP())
                .doubles("min_q", t -> t.getRight().getMinQ())
                .doubles("max_q", t -> t.getRight().getMaxQ())
                .build();
    }

    private static <T> ToIntFunction<T> getRatioTapPosition(Function<T, RatioTapChangerHolder> getter) {
        return ifExistsInt(t -> getter.apply(t).getRatioTapChanger(), RatioTapChanger::getTapPosition);
    }

    private static <T> ToIntFunction<T> getPhaseTapPosition(Function<T, PhaseTapChangerHolder> getter) {
        return ifExistsInt(t -> getter.apply(t).getPhaseTapChanger(), PhaseTapChanger::getTapPosition);
    }

    private static void setTapPosition(TapChanger<?, ?> tapChanger, int position) {
        if (tapChanger != null) {
            tapChanger.setTapPosition(position);
        }
    }

    private static String busIdGetter(Terminal t, boolean busBreakerView) {
        if (t == null) {
            return "";
        } else {
            Bus bus = busBreakerView ? t.getBusBreakerView().getBus() : t.getBusView().getBus();
            return bus != null ? bus.getId() : "";
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
}

