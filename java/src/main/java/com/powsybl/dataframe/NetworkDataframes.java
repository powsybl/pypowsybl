/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.DoubleSeriesMapper.DoubleUpdater;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.*;
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

    private static final Map<DataframeElementType, DataframeMapper> MAPPERS = createMappers();

    private NetworkDataframes() {
    }

    public static DataframeMapper getDataframeMapper(DataframeElementType type) {
        return MAPPERS.get(type);
    }

    private static Map<DataframeElementType, DataframeMapper> createMappers() {
        Map<DataframeElementType, DataframeMapper> mappers = new EnumMap<>(DataframeElementType.class);
        mappers.put(DataframeElementType.BUS, buses());
        mappers.put(DataframeElementType.LINE, lines());
        mappers.put(DataframeElementType.TWO_WINDINGS_TRANSFORMER, twoWindingTransformers());
        mappers.put(DataframeElementType.THREE_WINDINGS_TRANSFORMER, threeWindingTransformers());
        mappers.put(DataframeElementType.GENERATOR, generators());
        mappers.put(DataframeElementType.LOAD, loads());
        mappers.put(DataframeElementType.BATTERY, batteries());
        mappers.put(DataframeElementType.SHUNT_COMPENSATOR, shunts());
        mappers.put(DataframeElementType.DANGLING_LINE, danglingLines());
        mappers.put(DataframeElementType.LCC_CONVERTER_STATION, lccs());
        mappers.put(DataframeElementType.VSC_CONVERTER_STATION, vscs());
        mappers.put(DataframeElementType.STATIC_VAR_COMPENSATOR, svcs());
        mappers.put(DataframeElementType.SWITCH, switches());
        mappers.put(DataframeElementType.VOLTAGE_LEVEL, voltageLevels());
        mappers.put(DataframeElementType.SUBSTATION, substations());
        mappers.put(DataframeElementType.BUSBAR_SECTION, busBars());
        mappers.put(DataframeElementType.HVDC_LINE, hvdcs());
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER_STEP, rtcSteps());
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER_STEP, ptcSteps());
        mappers.put(DataframeElementType.RATIO_TAP_CHANGER, rtcs());
        mappers.put(DataframeElementType.PHASE_TAP_CHANGER, ptcs());
        mappers.put(DataframeElementType.REACTIVE_CAPABILITY_CURVE_POINT, reactiveCapabilityCurves());
        return Collections.unmodifiableMap(mappers);
    }

    static <U extends Injection<U>> ToDoubleFunction<U> getP() {
        return inj -> inj.getTerminal().getP();
    }

    static <U extends InjectionAdder<U>> BiConsumer<U, String> setConnectableBus() {
        return InjectionAdder::setConnectableBus;
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
        return b -> b.getTerminal1().getP();
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
        return b -> b.getTerminal2().getP();
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

    static DataframeMapper generators() {
        return DataframeMapperBuilder.ofStream(Network::getGeneratorStream, getOrThrow(Network::getGenerator, "Generator"))
            .stringsIndex("id", Generator::getId)
            .enums("energy_source", EnergySource.class, Generator::getEnergySource)
            .doubles("target_p", Generator::getTargetP, Generator::setTargetP)
            .doubles("min_p", Generator::getMinP, Generator::setMinP)
            .doubles("max_p", Generator::getMaxP, Generator::setMaxP)
            .doubles("min_q", ifExistsDouble(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ))
            .doubles("max_q", ifExistsDouble(NetworkDataframes::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ))
            .doubles("target_v", Generator::getTargetV, Generator::setTargetV)
            .doubles("target_q", Generator::getTargetQ, Generator::setTargetQ)
            .doubles("rated_s", Generator::getRatedS, Generator::setRatedS)
            .booleans("voltage_regulator_on", Generator::isVoltageRegulatorOn, Generator::setVoltageRegulatorOn)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id", getVoltageLevelId())
            .strings("bus_id", g -> getBusId(g.getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper buses() {
        return DataframeMapperBuilder.ofStream(n -> n.getBusView().getBusStream())
            .stringsIndex("id", Bus::getId)
            .doubles("v_mag", Bus::getV, Bus::setV)
            .doubles("v_angle", Bus::getAngle, Bus::setAngle)
            .addProperties()
            .build();
    }

    static DataframeMapper loads() {
        return DataframeMapperBuilder.ofStream(Network::getLoadStream, getOrThrow(Network::getLoad, "Load"))
            .stringsIndex("id", Load::getId)
            .enums("type", LoadType.class, Load::getLoadType)
            .doubles("p0", Load::getP0, Load::setP0)
            .doubles("q0", Load::getQ0, Load::setQ0)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id", getVoltageLevelId())
            .strings("bus_id", g -> getBusId(g.getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper batteries() {
        return DataframeMapperBuilder.ofStream(Network::getBatteryStream, getOrThrow(Network::getBattery, "Battery"))
            .stringsIndex("id", Battery::getId)
            .doubles("max_p", Battery::getMaxP, Battery::setMaxP)
            .doubles("min_p", Battery::getMinP, Battery::setMinP)
            .doubles("p0", Battery::getP0, Battery::setP0)
            .doubles("q0", Battery::getQ0, Battery::setQ0)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id",  getVoltageLevelId())
            .strings("bus_id",  g -> getBusId(g.getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper shunts() {
        return DataframeMapperBuilder.ofStream(Network::getShuntCompensatorStream, getOrThrow(Network::getShuntCompensator, "Shunt compensator"))
            .stringsIndex("id", ShuntCompensator::getId)
            .enums("model_type", ShuntCompensatorModelType.class, ShuntCompensator::getModelType)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id",  getVoltageLevelId())
            .strings("bus_id",  g -> getBusId(g.getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper lines() {
        return DataframeMapperBuilder.ofStream(Network::getLineStream, getOrThrow(Network::getLine, "Line"))
            .stringsIndex("id", Line::getId)
            .doubles("r", Line::getR, Line::setR)
            .doubles("x", Line::getX, Line::setX)
            .doubles("g1", Line::getG1, Line::setG1)
            .doubles("b1", Line::getB1, Line::setB1)
            .doubles("g2", Line::getG2, Line::setG2)
            .doubles("b2", Line::getB2, Line::setB2)
            .doubles("p1", getP1(), setP1())
            .doubles("q1", getQ1(), setQ1())
            .doubles("p2", getP2(), setP2())
            .doubles("q2", getQ2(), setQ2())
            .strings("voltage_level1_id", l -> l.getTerminal1().getVoltageLevel().getId())
            .strings("voltage_level2_id", l -> l.getTerminal2().getVoltageLevel().getId())
            .strings("bus1_id", l -> getBusId(l.getTerminal1()))
            .strings("bus2_id", l -> getBusId(l.getTerminal2()))
            .addProperties()
            .build();
    }

    static DataframeMapper twoWindingTransformers() {
        return DataframeMapperBuilder.ofStream(Network::getTwoWindingsTransformerStream, getOrThrow(Network::getTwoWindingsTransformer, "Two windings transformer"))
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
            .doubles("p2", getP2(), setP2())
            .doubles("q2", getQ2(), setQ2())
            .strings("voltage_level1_id", twt -> twt.getTerminal1().getVoltageLevel().getId())
            .strings("voltage_level2_id", twt -> twt.getTerminal2().getVoltageLevel().getId())
            .strings("bus1_id", twt -> getBusId(twt.getTerminal1()))
            .strings("bus2_id", twt -> getBusId(twt.getTerminal2()))
            .addProperties()
            .build();
    }

    static DataframeMapper threeWindingTransformers() {
        return DataframeMapperBuilder.ofStream(Network::getThreeWindingsTransformerStream, getOrThrow(Network::getThreeWindingsTransformer, "Three windings transformer"))
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
            .doubles("q1", twt -> twt.getLeg1().getTerminal().getP(), (twt, v) -> twt.getLeg1().getTerminal().setQ(v))
            .strings("voltage_level1_id", twt -> twt.getLeg1().getTerminal().getVoltageLevel().getId())
            .strings("bus1_id", twt -> getBusId(twt.getLeg1().getTerminal()))
            .doubles("r2", twt -> twt.getLeg2().getR(), (twt, v) -> twt.getLeg2().setR(v))
            .doubles("x2", twt -> twt.getLeg2().getX(), (twt, v) -> twt.getLeg2().setX(v))
            .doubles("g2", twt -> twt.getLeg2().getG(), (twt, v) -> twt.getLeg2().setG(v))
            .doubles("b2", twt -> twt.getLeg2().getB(), (twt, v) -> twt.getLeg2().setB(v))
            .doubles("rated_u2", twt -> twt.getLeg2().getRatedU(), (twt, v) -> twt.getLeg2().setRatedU(v))
            .doubles("rated_s2", twt -> twt.getLeg2().getRatedS(), (twt, v) -> twt.getLeg2().setRatedS(v))
            .ints("ratio_tap_position2", getRatioTapPosition(t -> t.getLeg2()), (t, v) -> setTapPosition(t.getLeg2().getRatioTapChanger(), v))
            .ints("phase_tap_position2", getPhaseTapPosition(t -> t.getLeg2()), (t, v) -> setTapPosition(t.getLeg2().getPhaseTapChanger(), v))
            .doubles("p2", twt -> twt.getLeg2().getTerminal().getP(), (twt, v) -> twt.getLeg2().getTerminal().setP(v))
            .doubles("q2", twt -> twt.getLeg2().getTerminal().getP(), (twt, v) -> twt.getLeg2().getTerminal().setQ(v))
            .strings("voltage_level2_id", twt -> twt.getLeg2().getTerminal().getVoltageLevel().getId())
            .strings("bus2_id", twt -> getBusId(twt.getLeg2().getTerminal()))
            .doubles("r3", twt -> twt.getLeg3().getR(), (twt, v) -> twt.getLeg3().setR(v))
            .doubles("x3", twt -> twt.getLeg3().getX(), (twt, v) -> twt.getLeg3().setX(v))
            .doubles("g3", twt -> twt.getLeg3().getG(), (twt, v) -> twt.getLeg3().setG(v))
            .doubles("b3", twt -> twt.getLeg3().getB(), (twt, v) -> twt.getLeg3().setB(v))
            .doubles("rated_u3", twt -> twt.getLeg3().getRatedU(), (twt, v) -> twt.getLeg3().setRatedU(v))
            .doubles("rated_s3", twt -> twt.getLeg3().getRatedS(), (twt, v) -> twt.getLeg3().setRatedS(v))
            .ints("ratio_tap_position3", getRatioTapPosition(t -> t.getLeg3()), (t, v) -> setTapPosition(t.getLeg3().getRatioTapChanger(), v))
            .ints("phase_tap_position3", getPhaseTapPosition(t -> t.getLeg3()), (t, v) -> setTapPosition(t.getLeg3().getPhaseTapChanger(), v))
            .doubles("p3", twt -> twt.getLeg3().getTerminal().getP(), (twt, v) -> twt.getLeg3().getTerminal().setP(v))
            .doubles("q3", twt -> twt.getLeg3().getTerminal().getP(), (twt, v) -> twt.getLeg3().getTerminal().setQ(v))
            .strings("voltage_level3_id", twt -> twt.getLeg3().getTerminal().getVoltageLevel().getId())
            .strings("bus3_id", twt -> getBusId(twt.getLeg3().getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper danglingLines() {
        return DataframeMapperBuilder.ofStream(Network::getDanglingLineStream, getOrThrow(Network::getDanglingLine, "Dangling line"))
            .stringsIndex("id", DanglingLine::getId)
            .doubles("r", DanglingLine::getR, DanglingLine::setR)
            .doubles("x", DanglingLine::getX, DanglingLine::setX)
            .doubles("g", DanglingLine::getG, DanglingLine::setG)
            .doubles("b", DanglingLine::getB, DanglingLine::setB)
            .doubles("p0", DanglingLine::getP0, DanglingLine::setQ0)
            .doubles("q0", DanglingLine::getQ0, DanglingLine::setP0)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id", getVoltageLevelId())
            .strings("bus_id", dl -> getBusId(dl.getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper lccs() {
        return DataframeMapperBuilder.ofStream(Network::getLccConverterStationStream, getOrThrow(Network::getLccConverterStation, "LCC converter station"))
            .stringsIndex("id", LccConverterStation::getId)
            .doubles("power_factor", LccConverterStation::getPowerFactor, (lcc, v) -> lcc.setPowerFactor((float) v))
            .doubles("loss_factor", LccConverterStation::getLossFactor, (lcc, v) -> lcc.setLossFactor((float) v))
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id", getVoltageLevelId())
            .strings("bus_id", st -> getBusId(st.getTerminal()))
            .addProperties()
            .build();
    }

    static DataframeMapper vscs() {
        return DataframeMapperBuilder.ofStream(Network::getVscConverterStationStream, getOrThrow(Network::getVscConverterStation, "VSC converter station"))
            .stringsIndex("id", VscConverterStation::getId)
            .doubles("voltage_setpoint", VscConverterStation::getVoltageSetpoint, VscConverterStation::setVoltageSetpoint)
            .doubles("reactive_power_setpoint", VscConverterStation::getReactivePowerSetpoint, VscConverterStation::setReactivePowerSetpoint)
            .booleans("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn, VscConverterStation::setVoltageRegulatorOn)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id", getVoltageLevelId())
            .strings("bus_id", st -> getBusId(st.getTerminal()))
            .addProperties()
            .build();
    }

    private static DataframeMapper svcs() {
        return DataframeMapperBuilder.ofStream(Network::getStaticVarCompensatorStream, getOrThrow(Network::getStaticVarCompensator, "Static var compensator"))
            .stringsIndex("id", StaticVarCompensator::getId)
            .doubles("voltage_setpoint", StaticVarCompensator::getVoltageSetpoint, StaticVarCompensator::setVoltageSetpoint)
            .doubles("reactive_power_setpoint", StaticVarCompensator::getReactivePowerSetpoint, StaticVarCompensator::setReactivePowerSetpoint)
            .enums("regulation_mode", StaticVarCompensator.RegulationMode.class,
                   StaticVarCompensator::getRegulationMode, StaticVarCompensator::setRegulationMode)
            .doubles("p", getP(), setP())
            .doubles("q", getQ(), setQ())
            .strings("voltage_level_id", getVoltageLevelId())
            .strings("bus_id", svc -> getBusId(svc.getTerminal()))
            .addProperties()
            .build();
    }

    private static DataframeMapper switches() {
        return DataframeMapperBuilder.ofStream(Network::getSwitchStream, getOrThrow(Network::getSwitch, "Switch"))
            .stringsIndex("id", Switch::getId)
            .enums("kind", SwitchKind.class, Switch::getKind)
            .booleans("open", Switch::isOpen, Switch::setOpen)
            .booleans("retained", Switch::isRetained, Switch::setRetained)
            .strings("voltage_level_id", s -> s.getVoltageLevel().getId())
            .addProperties()
            .build();
    }

    private static DataframeMapper voltageLevels() {
        return DataframeMapperBuilder.ofStream(Network::getVoltageLevelStream, getOrThrow(Network::getVoltageLevel, "Voltage level"))
            .stringsIndex("id", VoltageLevel::getId)
            .strings("substation_id", vl -> vl.getSubstation().getId())
            .doubles("nominal_v", VoltageLevel::getNominalV, VoltageLevel::setNominalV)
            .doubles("high_voltage_limit", VoltageLevel::getHighVoltageLimit, VoltageLevel::setHighVoltageLimit)
            .doubles("low_voltage_limit", VoltageLevel::getLowVoltageLimit, VoltageLevel::setLowVoltageLimit)
            .addProperties()
            .build();
    }

    private static DataframeMapper substations() {
        return DataframeMapperBuilder.ofStream(Network::getSubstationStream, getOrThrow(Network::getSubstation, "Substation"))
            .stringsIndex("id", Identifiable::getId)
            .strings("TSO", Substation::getTso, Substation::setTso)
            .strings("geo_tags", substation -> String.join(",", substation.getGeographicalTags()))
            .enums("country", Country.class, s -> s.getCountry().orElse(null), Substation::setCountry)
            .addProperties()
            .build();
    }

    private static DataframeMapper busBars() {
        return DataframeMapperBuilder.ofStream(Network::getBusbarSectionStream, getOrThrow(Network::getBusbarSection, "Bus bar section"))
            .stringsIndex("id", BusbarSection::getId)
            .booleans("fictitious", BusbarSection::isFictitious, BusbarSection::setFictitious)
            .doubles("v", BusbarSection::getV)
            .doubles("angle", BusbarSection::getAngle)
            .strings("voltage_level_id", bbs -> bbs.getTerminal().getVoltageLevel().getId())
            .addProperties()
            .build();
    }

    private static DataframeMapper hvdcs() {
        return DataframeMapperBuilder.ofStream(Network::getHvdcLineStream, getOrThrow(Network::getHvdcLine, "HVDC line"))
            .stringsIndex("id", HvdcLine::getId)
            .enums("converters_mode", HvdcLine.ConvertersMode.class, HvdcLine::getConvertersMode, HvdcLine::setConvertersMode)
            .doubles("active_power_setpoint", HvdcLine::getActivePowerSetpoint, HvdcLine::setActivePowerSetpoint)
            .doubles("max_p", HvdcLine::getMaxP, HvdcLine::setMaxP)
            .doubles("nominal_v", HvdcLine::getNominalV, HvdcLine::setNominalV)
            .doubles("r", HvdcLine::getR, HvdcLine::setR)
            .strings("converter_station1_id", l -> l.getConverterStation1().getId())
            .strings("converter_station2_id", l -> l.getConverterStation2().getId())
            .addProperties()
            .build();
    }

    private static DataframeMapper rtcSteps() {
        Function<Network, Stream<Triple<String, RatioTapChanger, Integer>>> ratioTapChangerSteps = network ->
            network.getTwoWindingsTransformerStream()
                .filter(twt -> twt.getRatioTapChanger() != null)
                .flatMap(twt -> twt.getRatioTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getRatioTapChanger(), position)));
        return DataframeMapperBuilder.ofStream(ratioTapChangerSteps)
            .stringsIndex("id", Triple::getLeft)
            .intsIndex("position", Triple::getRight)
            .doubles("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
            .doubles("r", p -> p.getMiddle().getStep(p.getRight()).getR())
            .doubles("x", p -> p.getMiddle().getStep(p.getRight()).getX())
            .doubles("g", p -> p.getMiddle().getStep(p.getRight()).getG())
            .doubles("b", p -> p.getMiddle().getStep(p.getRight()).getB())
            .build();
    }

    private static DataframeMapper ptcSteps() {
        Function<Network, Stream<Triple<String, PhaseTapChanger, Integer>>>  phaseTapChangerSteps = network ->
            network.getTwoWindingsTransformerStream()
                .filter(twt -> twt.getPhaseTapChanger() != null)
                .flatMap(twt -> twt.getPhaseTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getPhaseTapChanger(), position)));
        return DataframeMapperBuilder.ofStream(phaseTapChangerSteps)
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

    private static DataframeMapper rtcs() {
        return DataframeMapperBuilder.ofStream(
            network -> network.getTwoWindingsTransformerStream()
                .filter(t -> t.getRatioTapChanger() != null),
            NetworkDataframes::getT2OrThrow
        )
            .stringsIndex("id", TwoWindingsTransformer::getId)
            .ints("tap", t -> t.getRatioTapChanger().getTapPosition(), (t, p) -> t.getRatioTapChanger().setTapPosition(p))
            .ints("low_tap", t -> t.getRatioTapChanger().getLowTapPosition())
            .ints("high_tap", t -> t.getRatioTapChanger().getHighTapPosition())
            .ints("step_count", t -> t.getRatioTapChanger().getStepCount())
            .booleans("on_load", t -> t.getRatioTapChanger().hasLoadTapChangingCapabilities(), (t, v) -> t.getRatioTapChanger().setLoadTapChangingCapabilities(v))
            .booleans("regulating", t -> t.getRatioTapChanger().isRegulating(), (t, v) -> t.getRatioTapChanger().setRegulating(v))
            .doubles("target_v", t -> t.getRatioTapChanger().getTargetV(), (t, v) -> t.getRatioTapChanger().setTargetV(v))
            .doubles("target_deadband", t -> t.getRatioTapChanger().getTargetDeadband(), (t, v) -> t.getRatioTapChanger().setTargetDeadband(v))
            .build();
    }

    private static DataframeMapper ptcs() {
        return DataframeMapperBuilder.ofStream(
            network -> network.getTwoWindingsTransformerStream()
                .filter(t -> t.getPhaseTapChanger() != null),
            NetworkDataframes::getT2OrThrow
        )
            .stringsIndex("id", TwoWindingsTransformer::getId)
            .ints("tap", t -> t.getPhaseTapChanger().getTapPosition(), (t, v) -> t.getPhaseTapChanger().setTapPosition(v))
            .ints("low_tap", t -> t.getPhaseTapChanger().getLowTapPosition())
            .ints("high_tap", t -> t.getPhaseTapChanger().getHighTapPosition())
            .ints("step_count", t -> t.getPhaseTapChanger().getStepCount())
            .booleans("regulating", t -> t.getPhaseTapChanger().isRegulating(), (t, v) -> t.getPhaseTapChanger().setRegulating(v))
            .enums("regulation_mode", PhaseTapChanger.RegulationMode.class, t -> t.getPhaseTapChanger().getRegulationMode(), (t, v) -> t.getPhaseTapChanger().setRegulationMode(v))
            .doubles("regulation_value", t -> t.getPhaseTapChanger().getRegulationValue(), (t, v) -> t.getPhaseTapChanger().setRegulationValue(v))
            .doubles("target_deadband", t -> t.getPhaseTapChanger().getTargetDeadband(), (t, v) -> t.getPhaseTapChanger().setTargetDeadband(v))
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

    private static DataframeMapper reactiveCapabilityCurves() {
        return DataframeMapperBuilder.ofStream(NetworkDataframes::streamPoints)
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

    private static String getBusId(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null ? bus.getId() : "";
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
