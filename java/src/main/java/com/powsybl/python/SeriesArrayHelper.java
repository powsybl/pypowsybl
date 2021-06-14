/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
final class SeriesArrayHelper {

    static SeriesPointerArrayBuilder prepareData(Network network, PyPowsyblApiHeader.ElementType elementType) {
        switch (elementType) {
            case BUS:
                return prepareBusData(network);

            case LINE:
                return prepareLineData(network);

            case TWO_WINDINGS_TRANSFORMER:
                return prepareTransformers2Data(network);

            case THREE_WINDINGS_TRANSFORMER:
                return prepareTransformers3(network);

            case GENERATOR:
                return prepareGenerator(network);

            case LOAD:
                return prepareLoad(network);

            case BATTERY:
                return prepareBattery(network);

            case SHUNT_COMPENSATOR:
                return prepareShunt(network);

            case DANGLING_LINE:
                return prepareDll(network);

            case LCC_CONVERTER_STATION:
                return prepareLcc(network);

            case VSC_CONVERTER_STATION:
                return prepareVsc(network);

            case STATIC_VAR_COMPENSATOR:
                return prepareSvc(network);

            case SWITCH:
                return prepareSwitch(network);

            case VOLTAGE_LEVEL:
                return prepareVoltageLevel(network);

            case SUBSTATION:
                return prepareSubstation(network);

            case BUSBAR_SECTION:
                return prepareBusbar(network);

            case HVDC_LINE:
                return prepareHvdc(network);

            case RATIO_TAP_CHANGER_STEP:
                return prepareRtcStepsData(network);

            case PHASE_TAP_CHANGER_STEP:
                return preparePtcStepsData(network);

            case REACTIVE_CAPABILITY_CURVE_POINT:
                return prepareReactiveCapabilityCurvePoint(network);

            default:
                throw new UnsupportedOperationException("Element type not supported: " + elementType);
        }
    }

    static void updateNetworkElementsWithDoubleSeries(Network network, PyPowsyblApiHeader.ElementType elementType, int elementCount,
                                                      String seriesName, IntFunction<String> idGetter, IntToDoubleFunction valueGetter) {
        for (int i = 0; i < elementCount; i++) {
            String id = idGetter.apply(i);
            double value = valueGetter.applyAsDouble(i);
            switch (elementType) {
                case GENERATOR:
                    updateGeneratorDouble(id, network, seriesName, value);
                    break;
                case LOAD:
                    updateLoadDouble(id, network, seriesName, value);
                    break;
                case BATTERY:
                    updateBatteryDouble(id, network, seriesName, value);
                    break;
                case DANGLING_LINE:
                    updateDllDouble(id, network, seriesName, value);
                    break;
                case VSC_CONVERTER_STATION:
                    updateVscDouble(id, network, seriesName, value);
                    break;
                case STATIC_VAR_COMPENSATOR:
                    updateSvcDouble(id, network, seriesName, value);
                    break;
                case HVDC_LINE:
                    updateHvdcDouble(id, network, seriesName, value);
                    break;
                default:
                    throw new UnsupportedOperationException("Updating double series: type '" + elementType + "' field '" + seriesName + "' not supported");
            }
        }
    }

    static void updateNetworkElementsWithIntSeries(Network network, PyPowsyblApiHeader.ElementType elementType, int elementCount,
                                                   String seriesName, IntFunction<String> idGetter, IntToIntFunction valueGetter) {
        for (int i = 0; i < elementCount; i++) {
            String id = idGetter.apply(i);
            int value = valueGetter.applyAsInt(i);
            switch (elementType) {
                case SWITCH:
                    updateSwitchInt(id, network, seriesName, value);
                    break;
                case GENERATOR:
                    updateGeneratorInt(id, network, seriesName, value);
                    break;
                case VSC_CONVERTER_STATION:
                    updateVscInt(id, network, seriesName, value);
                    break;
                case TWO_WINDINGS_TRANSFORMER:
                    updateTransfo2Int(id, network, seriesName, value);
                    break;
                default:
                    throw new UnsupportedOperationException("Updating int or boolean series: type '" + elementType + "' field '" + seriesName + "' not supported");
            }
        }
    }

    static void updateNetworkElementsWithStringSeries(Network network, PyPowsyblApiHeader.ElementType elementType, int elementCount,
                                                      String seriesName, IntFunction<String> idGetter, IntFunction<String> valueGetter) {
        for (int i = 0; i < elementCount; i++) {
            String id = idGetter.apply(i);
            String value = valueGetter.apply(i);
            switch (elementType) {
                case STATIC_VAR_COMPENSATOR:
                    updateSvcString(id, network, seriesName, value);
                    break;
                case HVDC_LINE:
                    updateHvdcString(id, network, seriesName, value);
                    break;
                default:
                    throw new UnsupportedOperationException("Updating string series: type '" + elementType + "' field '" + seriesName + "' not supported");
            }
        }
    }

    private static SeriesPointerArrayBuilder prepareTransformers3(Network network) {
        List<ThreeWindingsTransformer> transformers3 = network.getThreeWindingsTransformerStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(transformers3)
                .addStringSeries("id", true, ThreeWindingsTransformer::getId)
                .addDoubleSeries("rated_u0", ThreeWindingsTransformer::getRatedU0)
                .addDoubleSeries("r1", twt -> twt.getLeg1().getR())
                .addDoubleSeries("x1", twt -> twt.getLeg1().getR())
                .addDoubleSeries("g1", twt -> twt.getLeg1().getR())
                .addDoubleSeries("b1", twt -> twt.getLeg1().getR())
                .addDoubleSeries("rated_u1", twt -> twt.getLeg1().getRatedU())
                .addDoubleSeries("rated_s1", twt -> twt.getLeg1().getRatedS())
                .addIntSeries("ratio_tap_position1", twt -> twt.getLeg1().getRatioTapChanger(), TapChanger::getTapPosition)
                .addIntSeries("phase_tap_position1", twt -> twt.getLeg1().getPhaseTapChanger(), TapChanger::getTapPosition)
                .addDoubleSeries("p1", twt -> twt.getLeg1().getTerminal().getP())
                .addDoubleSeries("q1", twt -> twt.getLeg1().getTerminal().getP())
                .addStringSeries("voltage_level1_id", twt -> twt.getLeg1().getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus1_id", twt -> getBusId(twt.getLeg1().getTerminal()))
                .addDoubleSeries("r2", twt -> twt.getLeg2().getR())
                .addDoubleSeries("x2", twt -> twt.getLeg2().getR())
                .addDoubleSeries("g2", twt -> twt.getLeg2().getR())
                .addDoubleSeries("b2", twt -> twt.getLeg2().getR())
                .addDoubleSeries("rated_u2", twt -> twt.getLeg2().getRatedU())
                .addDoubleSeries("rated_s2", twt -> twt.getLeg2().getRatedS())
                .addIntSeries("ratio_tap_position2", twt -> twt.getLeg2().getRatioTapChanger(), TapChanger::getTapPosition)
                .addIntSeries("phase_tap_position2", twt -> twt.getLeg2().getPhaseTapChanger(), TapChanger::getTapPosition)
                .addDoubleSeries("p2", twt -> twt.getLeg2().getTerminal().getP())
                .addDoubleSeries("q2", twt -> twt.getLeg2().getTerminal().getP())
                .addStringSeries("voltage_level2_id", twt -> twt.getLeg2().getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus2_id", twt -> getBusId(twt.getLeg2().getTerminal()))
                .addDoubleSeries("r3", twt -> twt.getLeg3().getR())
                .addDoubleSeries("x3", twt -> twt.getLeg3().getR())
                .addDoubleSeries("g3", twt -> twt.getLeg3().getR())
                .addDoubleSeries("b3", twt -> twt.getLeg3().getR())
                .addDoubleSeries("rated_u3", twt -> twt.getLeg3().getRatedU())
                .addDoubleSeries("rated_s3", twt -> twt.getLeg3().getRatedS())
                .addIntSeries("ratio_tap_position3", twt -> twt.getLeg3().getRatioTapChanger(), TapChanger::getTapPosition)
                .addIntSeries("phase_tap_position3", twt -> twt.getLeg3().getPhaseTapChanger(), TapChanger::getTapPosition)
                .addDoubleSeries("p3", twt -> twt.getLeg3().getTerminal().getP())
                .addDoubleSeries("q3", twt -> twt.getLeg3().getTerminal().getP())
                .addStringSeries("voltage_level3_id", twt -> twt.getLeg3().getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus3_id", twt -> getBusId(twt.getLeg3().getTerminal())));
    }

    private static MinMaxReactiveLimits getMinMaxReactiveLimits(ReactiveLimitsHolder holder) {
        ReactiveLimits reactiveLimits = holder.getReactiveLimits();
        return reactiveLimits instanceof MinMaxReactiveLimits ? (MinMaxReactiveLimits) reactiveLimits : null;
    }

    private static SeriesPointerArrayBuilder prepareGenerator(Network network) {
        List<Generator> generators = network.getGeneratorStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(generators)
                .addStringSeries("id", true, Generator::getId)
                .addEnumSeries("energy_source", Generator::getEnergySource)
                .addDoubleSeries("target_p", Generator::getTargetP)
                .addDoubleSeries("min_p", Generator::getMinP)
                .addDoubleSeries("max_p", Generator::getMaxP)
                .addDoubleSeries("min_q", SeriesArrayHelper::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ)
                .addDoubleSeries("max_q", SeriesArrayHelper::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ)
                .addDoubleSeries("target_v", Generator::getTargetV)
                .addDoubleSeries("target_q", Generator::getTargetQ)
                .addBooleanSeries("voltage_regulator_on", Generator::isVoltageRegulatorOn)
                .addDoubleSeries("p", g -> g.getTerminal().getP())
                .addDoubleSeries("q", g -> g.getTerminal().getQ())
                .addStringSeries("voltage_level_id", g -> g.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", g -> getBusId(g.getTerminal())));
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

    private static SeriesPointerArrayBuilder prepareReactiveCapabilityCurvePoint(Network network) {
        List<Triple<String, Integer, ReactiveCapabilityCurve.Point>> indexedPoints = network.getGeneratorStream()
                .filter(g -> g.getReactiveLimits() instanceof ReactiveCapabilityCurve)
                .flatMap(g -> indexPoints(g.getId(), g).stream())
                .collect(Collectors.toList());
        indexedPoints.addAll(network.getVscConverterStationStream()
                .filter(g -> g.getReactiveLimits() instanceof ReactiveCapabilityCurve)
                .flatMap(g -> indexPoints(g.getId(), g).stream())
                .collect(Collectors.toList()));
        return new SeriesPointerArrayBuilder<>(indexedPoints)
                .addStringSeries("id", true, Triple::getLeft)
                .addIntSeries("num", true, Triple::getMiddle)
                .addDoubleSeries("p", t -> t.getRight().getP())
                .addDoubleSeries("min_p", t -> t.getRight().getMinQ())
                .addDoubleSeries("max_p", t -> t.getRight().getMaxQ());
    }

    private static SeriesPointerArrayBuilder prepareLoad(Network network) {
        List<Load> loads = network.getLoadStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(loads)
                .addStringSeries("id", true, Load::getId)
                .addEnumSeries("type", Load::getLoadType)
                .addDoubleSeries("p0", Load::getP0)
                .addDoubleSeries("q0", Load::getQ0)
                .addDoubleSeries("p", l -> l.getTerminal().getP())
                .addDoubleSeries("q", l -> l.getTerminal().getQ())
                .addStringSeries("voltage_level_id", l -> l.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", l -> getBusId(l.getTerminal())));
    }

    private static SeriesPointerArrayBuilder prepareBattery(Network network) {
        List<Battery> batteries = network.getBatteryStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(batteries)
                .addStringSeries("id", true, Battery::getId)
                .addDoubleSeries("max_p", Battery::getMaxP)
                .addDoubleSeries("min_p", Battery::getMinP)
                .addDoubleSeries("p0", Battery::getP0)
                .addDoubleSeries("q0", Battery::getQ0));
    }

    private static SeriesPointerArrayBuilder prepareShunt(Network network) {
        List<ShuntCompensator> shunts = network.getShuntCompensatorStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(shunts)
                .addStringSeries("id", true, ShuntCompensator::getId)
                .addEnumSeries("model_type", ShuntCompensator::getModelType)
                .addDoubleSeries("p", sc -> sc.getTerminal().getP())
                .addDoubleSeries("q", sc -> sc.getTerminal().getQ())
                .addStringSeries("voltage_level_id", sc -> sc.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", sc -> getBusId(sc.getTerminal())));
    }

    private static SeriesPointerArrayBuilder prepareDll(Network network) {
        List<DanglingLine> danglingLines = network.getDanglingLineStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(danglingLines)
                .addStringSeries("id", true, DanglingLine::getId)
                .addDoubleSeries("r", DanglingLine::getR)
                .addDoubleSeries("x", DanglingLine::getX)
                .addDoubleSeries("g", DanglingLine::getG)
                .addDoubleSeries("b", DanglingLine::getB)
                .addDoubleSeries("p0", DanglingLine::getP0)
                .addDoubleSeries("q0", DanglingLine::getQ0)
                .addDoubleSeries("p", dl -> dl.getTerminal().getP())
                .addDoubleSeries("q", dl -> dl.getTerminal().getQ())
                .addStringSeries("voltage_level_id", dl -> dl.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", dl -> getBusId(dl.getTerminal())));
    }

    private static SeriesPointerArrayBuilder prepareLcc(Network network) {
        List<LccConverterStation> lccStations = network.getLccConverterStationStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(lccStations)
                .addStringSeries("id", true, LccConverterStation::getId)
                .addDoubleSeries("power_factor", LccConverterStation::getPowerFactor)
                .addDoubleSeries("loss_factor", LccConverterStation::getLossFactor)
                .addDoubleSeries("p", st -> st.getTerminal().getP())
                .addDoubleSeries("q", st -> st.getTerminal().getQ())
                .addStringSeries("voltage_level_id", st -> st.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", st -> getBusId(st.getTerminal())));
    }

    private static SeriesPointerArrayBuilder prepareVsc(Network network) {
        List<VscConverterStation> vscStations = network.getVscConverterStationStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(vscStations)
                .addStringSeries("id", true, VscConverterStation::getId)
                .addDoubleSeries("voltage_setpoint", VscConverterStation::getVoltageSetpoint)
                .addDoubleSeries("reactive_power_setpoint", VscConverterStation::getReactivePowerSetpoint)
                .addDoubleSeries("min_q", SeriesArrayHelper::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMinQ)
                .addDoubleSeries("max_q", SeriesArrayHelper::getMinMaxReactiveLimits, MinMaxReactiveLimits::getMaxQ)
                .addBooleanSeries("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn)
                .addDoubleSeries("p", st -> st.getTerminal().getP())
                .addDoubleSeries("q", st -> st.getTerminal().getQ())
                .addStringSeries("voltage_level_id", st -> st.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", st -> getBusId(st.getTerminal())));
    }

    private static SeriesPointerArrayBuilder prepareSvc(Network network) {
        List<StaticVarCompensator> svcs = network.getStaticVarCompensatorStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(svcs)
                .addStringSeries("id", true, StaticVarCompensator::getId)
                .addDoubleSeries("voltage_setpoint", StaticVarCompensator::getVoltageSetpoint)
                .addDoubleSeries("reactive_power_setpoint", StaticVarCompensator::getReactivePowerSetpoint)
                .addEnumSeries("regulation_mode", StaticVarCompensator::getRegulationMode)
                .addDoubleSeries("p", svc -> svc.getTerminal().getP())
                .addDoubleSeries("q", svc -> svc.getTerminal().getQ())
                .addStringSeries("voltage_level_id", svc -> svc.getTerminal().getVoltageLevel().getId())
                .addStringSeries("bus_id", svc -> getBusId(svc.getTerminal())));
    }

    private static SeriesPointerArrayBuilder prepareSwitch(Network network) {
        List<Switch> switches = network.getSwitchStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(switches)
                .addStringSeries("id", true, Switch::getId)
                .addEnumSeries("kind", Switch::getKind)
                .addBooleanSeries("open", Switch::isOpen)
                .addBooleanSeries("retained", Switch::isRetained)
                .addStringSeries("voltage_level_id", sw -> sw.getVoltageLevel().getId()));
    }

    private static SeriesPointerArrayBuilder prepareVoltageLevel(Network network) {
        List<VoltageLevel> voltageLevels = network.getVoltageLevelStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(voltageLevels)
                .addStringSeries("id", true, VoltageLevel::getId)
                .addStringSeries("substation_id", vl -> vl.getSubstation().getId())
                .addDoubleSeries("nominal_v", VoltageLevel::getNominalV)
                .addDoubleSeries("high_voltage_limit", VoltageLevel::getHighVoltageLimit)
                .addDoubleSeries("low_voltage_limit", VoltageLevel::getLowVoltageLimit));
    }

    private static SeriesPointerArrayBuilder prepareSubstation(Network network) {
        List<Substation> substations = network.getSubstationStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(substations))
                .addStringSeries("id", true, Identifiable::getId)
                .addStringSeries("TSO", Substation::getTso)
                .addStringSeries("geo_tags", substation -> String.join(",", substation.getGeographicalTags()))
                .addStringSeries("country", substation -> substation.getCountry().map(Country::toString).orElse(""));
    }

    private static SeriesPointerArrayBuilder prepareBusbar(Network network) {
        List<BusbarSection> busbarSections = network.getBusbarSectionStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(busbarSections))
                .addStringSeries("id", true, BusbarSection::getId)
                .addDoubleSeries("v", BusbarSection::getV)
                .addDoubleSeries("angle", BusbarSection::getAngle)
                .addStringSeries("voltage_level_id", bbs -> bbs.getTerminal().getVoltageLevel().getId());

    }

    private static SeriesPointerArrayBuilder prepareHvdc(Network network) {
        List<HvdcLine> hvdcLines = network.getHvdcLineStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(hvdcLines)
                .addStringSeries("id", true, HvdcLine::getId)
                .addEnumSeries("converters_mode", HvdcLine::getConvertersMode)
                .addDoubleSeries("active_power_setpoint", HvdcLine::getActivePowerSetpoint)
                .addDoubleSeries("max_p", HvdcLine::getMaxP)
                .addDoubleSeries("nominal_v", HvdcLine::getNominalV)
                .addDoubleSeries("r", HvdcLine::getR)
                .addStringSeries("converter_station1_id", l -> l.getConverterStation1().getId())
                .addStringSeries("converter_station2_id", l -> l.getConverterStation2().getId()));
    }

    private static SeriesPointerArrayBuilder prepareRtcStepsData(Network network) {
        List<Triple<String, RatioTapChanger, Integer>> ratioTapChangerSteps = network.getTwoWindingsTransformerStream()
                .filter(twt -> twt.getRatioTapChanger() != null)
                .flatMap(twt -> twt.getRatioTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getRatioTapChanger(), position)))
                .collect(Collectors.toList());
        return new SeriesPointerArrayBuilder<>(ratioTapChangerSteps)
                .addStringSeries("id", true, Triple::getLeft)
                .addIntSeries("position", true, Triple::getRight)
                .addDoubleSeries("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
                .addDoubleSeries("r", p -> p.getMiddle().getStep(p.getRight()).getR())
                .addDoubleSeries("x", p -> p.getMiddle().getStep(p.getRight()).getX())
                .addDoubleSeries("g", p -> p.getMiddle().getStep(p.getRight()).getG())
                .addDoubleSeries("b", p -> p.getMiddle().getStep(p.getRight()).getB());
    }

    private static SeriesPointerArrayBuilder preparePtcStepsData(Network network) {
        List<Triple<String, PhaseTapChanger, Integer>> phaseTapChangerSteps = network.getTwoWindingsTransformerStream()
                .filter(twt -> twt.getPhaseTapChanger() != null)
                .flatMap(twt -> twt.getPhaseTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getPhaseTapChanger(), position)))
                .collect(Collectors.toList());
        return new SeriesPointerArrayBuilder<>(phaseTapChangerSteps)
                .addStringSeries("id", true, Triple::getLeft)
                .addIntSeries("position", true, Triple::getRight)
                .addDoubleSeries("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
                .addDoubleSeries("alpha", p -> p.getMiddle().getStep(p.getRight()).getAlpha())
                .addDoubleSeries("r", p -> p.getMiddle().getStep(p.getRight()).getR())
                .addDoubleSeries("x", p -> p.getMiddle().getStep(p.getRight()).getX())
                .addDoubleSeries("g", p -> p.getMiddle().getStep(p.getRight()).getG())
                .addDoubleSeries("b", p -> p.getMiddle().getStep(p.getRight()).getB());
    }

    private static SeriesPointerArrayBuilder prepareTransformers2Data(Network network) {
        List<TwoWindingsTransformer> transformers2 = network.getTwoWindingsTransformerStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(transformers2)
                .addStringSeries("id", true, TwoWindingsTransformer::getId)
                .addDoubleSeries("r", TwoWindingsTransformer::getR)
                .addDoubleSeries("x", TwoWindingsTransformer::getX)
                .addDoubleSeries("g", TwoWindingsTransformer::getG)
                .addDoubleSeries("b", TwoWindingsTransformer::getB)
                .addDoubleSeries("rated_u1", TwoWindingsTransformer::getRatedU1)
                .addDoubleSeries("rated_u2", TwoWindingsTransformer::getRatedU2)
                .addDoubleSeries("rated_s", TwoWindingsTransformer::getRatedS)
                .addIntSeries("ratio_tap_position", TwoWindingsTransformer::getRatioTapChanger, TapChanger::getTapPosition)
                .addIntSeries("phase_tap_position", TwoWindingsTransformer::getPhaseTapChanger, TapChanger::getTapPosition)
                .addDoubleSeries("p1", twt -> twt.getTerminal1().getP())
                .addDoubleSeries("q1", twt -> twt.getTerminal1().getQ())
                .addDoubleSeries("p2", twt -> twt.getTerminal2().getP())
                .addDoubleSeries("q2", twt -> twt.getTerminal2().getQ())
                .addStringSeries("voltage_level1_id", twt -> twt.getTerminal1().getVoltageLevel().getId())
                .addStringSeries("voltage_level2_id", twt -> twt.getTerminal2().getVoltageLevel().getId())
                .addStringSeries("bus1_id", twt -> getBusId(twt.getTerminal1()))
                .addStringSeries("bus2_id", twt -> getBusId(twt.getTerminal2())));
    }

    private static SeriesPointerArrayBuilder prepareBusData(Network network) {
        List<Bus> buses = network.getBusView().getBusStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(buses)
                .addStringSeries("id", true, Bus::getId)
                .addDoubleSeries("v_mag", Bus::getV)
                .addDoubleSeries("v_angle", Bus::getAngle));
    }

    private static SeriesPointerArrayBuilder prepareLineData(Network network) {
        List<Line> lines = network.getLineStream().collect(Collectors.toList());
        return addProperties(new SeriesPointerArrayBuilder<>(lines)
                .addStringSeries("id", true, Line::getId)
                .addDoubleSeries("r", Line::getR)
                .addDoubleSeries("x", Line::getX)
                .addDoubleSeries("g1", Line::getG1)
                .addDoubleSeries("b1", Line::getB1)
                .addDoubleSeries("g2", Line::getG2)
                .addDoubleSeries("b2", Line::getB2)
                .addDoubleSeries("p1", l -> l.getTerminal1().getP())
                .addDoubleSeries("q1", l -> l.getTerminal1().getQ())
                .addDoubleSeries("p2", l -> l.getTerminal2().getP())
                .addDoubleSeries("q2", l -> l.getTerminal2().getQ())
                .addStringSeries("voltage_level1_id", l -> l.getTerminal1().getVoltageLevel().getId())
                .addStringSeries("voltage_level2_id", l -> l.getTerminal2().getVoltageLevel().getId())
                .addStringSeries("bus1_id", l -> getBusId(l.getTerminal1()))
                .addStringSeries("bus2_id", l -> getBusId(l.getTerminal2())));
    }

    private static void updateGeneratorDouble(String id, Network network, String seriesName, double value) {
        Generator g = getGeneratorOrThrowsException(id, network);
        switch (seriesName) {
            case "target_p":
                g.setTargetP(value);
                break;
            case "target_q":
                g.setTargetQ(value);
                break;
            case "target_v":
                g.setTargetV(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for generate elements: " + seriesName);
        }
    }

    private static void updateLoadDouble(String id, Network network, String seriesName, double value) {
        Load l = getLoadOrThrowsException(id, network);
        switch (seriesName) {
            case "p0":
                l.setP0(value);
                break;
            case "q0":
                l.setQ0(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for load elements: " + seriesName);
        }
    }

    private static void updateBatteryDouble(String id, Network network, String seriesName, double value) {
        Battery b = getBatteryOrThrowsException(id, network);
        switch (seriesName) {
            case "p0":
                b.setP0(value);
                break;
            case "q0":
                b.setQ0(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for battery elements: " + seriesName);
        }
    }

    private static void updateDllDouble(String id, Network network, String seriesName, double value) {
        DanglingLine dll = getDanglingLineOrThrowsException(id, network);
        switch (seriesName) {
            case "p0":
                dll.setP0(value);
                break;
            case "q0":
                dll.setQ0(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for dangling line elements: " + seriesName);
        }
    }

    private static void updateVscDouble(String id, Network network, String seriesName, double value) {
        VscConverterStation vsc = getVscOrThrowsException(id, network);
        switch (seriesName) {
            case "voltage_setpoint":
                vsc.setVoltageSetpoint(value);
                break;
            case "reactive_power_setpoint":
                vsc.setReactivePowerSetpoint(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for vsc elements: " + seriesName);
        }
    }

    private static void updateSvcDouble(String id, Network network, String seriesName, double value) {
        StaticVarCompensator svc = getSvcOrThrowsException(id, network);
        switch (seriesName) {
            case "voltage_setpoint":
                svc.setVoltageSetpoint(value);
                break;
            case "reactive_power_setpoint":
                svc.setReactivePowerSetpoint(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for svc elements: " + seriesName);
        }
    }

    private static void updateHvdcDouble(String id, Network network, String seriesName, double value) {
        HvdcLine hvdc = getHvdcOrThrowsException(id, network);
        switch (seriesName) {
            case "active_power_setpoint":
                hvdc.setActivePowerSetpoint(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for hvdc line elements: " + seriesName);
        }
    }

    private static void updateSwitchInt(String id, Network network, String seriesName, int value) {
        Switch sw = getSwitchOrThrowsException(id, network);
        switch (seriesName) {
            case "open":
                sw.setOpen(value == 1);
                break;
            case "retained":
                sw.setRetained(value == 1);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for switch elements: " + seriesName);
        }
    }

    private static void updateGeneratorInt(String id, Network network, String seriesName, int value) {
        Generator g = getGeneratorOrThrowsException(id, network);
        switch (seriesName) {
            case "voltage_regulator_on":
                g.setVoltageRegulatorOn(value == 1);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for generate elements: " + seriesName);
        }
    }

    private static void updateVscInt(String id, Network network, String seriesName, int value) {
        VscConverterStation vsc = getVscOrThrowsException(id, network);
        switch (seriesName) {
            case "voltage_regulator_on":
                vsc.setVoltageRegulatorOn(value == 1);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for vsc elements: " + seriesName);
        }
    }

    private static void updateTransfo2Int(String id, Network network, String seriesName, int value) {
        TwoWindingsTransformer twt = getTransformerOrThrowsException(id, network);
        switch (seriesName) {
            case "ratio_tap_position":
                getRatioTapChanger(twt).setTapPosition(value);
                break;
            case "phase_tap_position":
                getPhaseTapChanger(twt).setTapPosition(value);
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for 2 windings transformer elements: " + seriesName);
        }
    }

    private static void updateHvdcString(String id, Network network, String seriesName, String value) {
        HvdcLine hvdc = getHvdcOrThrowsException(id, network);
        switch (seriesName) {
            case "converters_mode":
                hvdc.setConvertersMode(HvdcLine.ConvertersMode.valueOf(value.toUpperCase()));
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for hvdc elements: " + seriesName);
        }
    }

    private static void updateSvcString(String id, Network network, String seriesName, String value) {
        StaticVarCompensator svc = getSvcOrThrowsException(id, network);
        switch (seriesName) {
            case "regulation_mode":
                svc.setRegulationMode(StaticVarCompensator.RegulationMode.valueOf(value.toUpperCase()));
                break;
            default:
                throw new UnsupportedOperationException("Series name not supported for svc elements: " + seriesName);
        }
    }

    static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> writeToCStruct(SeriesPointerArrayBuilder builder) {
        return builder.build();
    }

    private static String getBusId(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null ? bus.getId() : "";
    }

    private static <T extends Identifiable<T>> SeriesPointerArrayBuilder<T> addProperties(SeriesPointerArrayBuilder<T> builder) {
        Set<String> propertyNames = builder.getElements().stream()
                .filter(Identifiable::hasProperty)
                .flatMap(e -> e.getPropertyNames().stream())
                .collect(Collectors.toSet());
        for (String propertyName : propertyNames) {
            builder.addStringSeries(propertyName, t -> t.getProperty(propertyName));
        }
        return builder;
    }

    private static TwoWindingsTransformer getTransformerOrThrowsException(String id, Network network) {
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
        if (twt == null) {
            throw new PowsyblException("Two windings transformer '" + id + "' not found");
        }
        return twt;
    }

    private static RatioTapChanger getRatioTapChanger(TwoWindingsTransformer twt) {
        RatioTapChanger rtc = twt.getRatioTapChanger();
        if (rtc == null) {
            throw new PowsyblException("Two windings transformer '" + twt.getId() + "' does not have a ratio tap changer");
        }
        return rtc;
    }

    private static PhaseTapChanger getPhaseTapChanger(TwoWindingsTransformer twt) {
        PhaseTapChanger ptc = twt.getPhaseTapChanger();
        if (ptc == null) {
            throw new PowsyblException("Two windings transformer '" + twt.getId() + "' does not have a phase tap changer");
        }
        return ptc;
    }

    private static Generator getGeneratorOrThrowsException(String id, Network network) {
        Generator g = network.getGenerator(id);
        if (g == null) {
            throw new PowsyblException("Generator '" + id + "' not found");
        }
        return g;
    }

    private static Switch getSwitchOrThrowsException(String id, Network network) {
        Switch sw = network.getSwitch(id);
        if (sw == null) {
            throw new PowsyblException("Switch '" + id + "' not found");
        }
        return sw;
    }

    private static Load getLoadOrThrowsException(String id, Network network) {
        Load load = network.getLoad(id);
        if (load == null) {
            throw new PowsyblException("Load '" + id + "' not found");
        }
        return load;
    }

    private static Battery getBatteryOrThrowsException(String id, Network network) {
        Battery b = network.getBattery(id);
        if (b == null) {
            throw new PowsyblException("Battery '" + id + "' not found");
        }
        return b;
    }

    private static DanglingLine getDanglingLineOrThrowsException(String id, Network network) {
        DanglingLine l = network.getDanglingLine(id);
        if (l == null) {
            throw new PowsyblException("DanglingLine '" + id + "' not found");
        }
        return l;
    }

    private static VscConverterStation getVscOrThrowsException(String id, Network network) {
        VscConverterStation vsc = network.getVscConverterStation(id);
        if (vsc == null) {
            throw new PowsyblException("VscConverterStation '" + id + "' not found");
        }
        return vsc;
    }

    private static StaticVarCompensator getSvcOrThrowsException(String id, Network network) {
        StaticVarCompensator svc = network.getStaticVarCompensator(id);
        if (svc == null) {
            throw new PowsyblException("StaticVarCompensator '" + id + "' not found");
        }
        return svc;
    }

    private static HvdcLine getHvdcOrThrowsException(String id, Network network) {
        HvdcLine hvdc = network.getHvdcLine(id);
        if (hvdc == null) {
            throw new PowsyblException("HvdcLine '" + id + "' not found");
        }
        return hvdc;
    }

    private SeriesArrayHelper() {
    }
}
