/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.loadflow.validation;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.network.ThreeSides;

import java.util.List;
import java.util.function.Function;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class Validations {

    public static DataframeMapper<List<BranchValidationData>, Void> branchValidationsMapper() {
        return new DataframeMapperBuilder<List<BranchValidationData>, BranchValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", BranchValidationData::getBranchId)
                .doubles("p1", BranchValidationData::getP1)
                .doubles("p1_calc", BranchValidationData::getP1Calc)
                .doubles("q1", BranchValidationData::getQ1)
                .doubles("q1_calc", BranchValidationData::getQ1Calc)
                .doubles("p2", BranchValidationData::getP2)
                .doubles("p2_calc", BranchValidationData::getP2Calc)
                .doubles("q2", BranchValidationData::getQ2)
                .doubles("q2_calc", BranchValidationData::getQ2Calc)
                .doubles("r", BranchValidationData::getR)
                .doubles("x", BranchValidationData::getX)
                .doubles("g1", BranchValidationData::getG1)
                .doubles("g2", BranchValidationData::getG2)
                .doubles("b1", BranchValidationData::getB1)
                .doubles("b2", BranchValidationData::getB2)
                .doubles("rho1", BranchValidationData::getRho1)
                .doubles("rho2", BranchValidationData::getRho2)
                .doubles("alpha1", BranchValidationData::getAlpha1)
                .doubles("alpha2", BranchValidationData::getAlpha2)
                .doubles("u1", BranchValidationData::getU1)
                .doubles("u2", BranchValidationData::getU2)
                .doubles("theta1", BranchValidationData::getTheta1)
                .doubles("theta2", BranchValidationData::getTheta2)
                .doubles("z", BranchValidationData::getZ)
                .doubles("y", BranchValidationData::getY)
                .doubles("ksi", BranchValidationData::getKsi)
                .ints("phase_angle_clock", BranchValidationData::getPhaseAngleClock)
                .booleans("connected1", BranchValidationData::isConnected1)
                .booleans("connected2", BranchValidationData::isConnected2)
                .booleans("main_component1", BranchValidationData::isMainComponent1)
                .booleans("main_component2", BranchValidationData::isMainComponent2)
                .booleans("validated", BranchValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<List<BusValidationData>, Void> busValidationsMapper() {
        return new DataframeMapperBuilder<List<BusValidationData>, BusValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", BusValidationData::getId)
                .doubles("incoming_p", BusValidationData::getIncomingP)
                .doubles("incoming_q", BusValidationData::getIncomingQ)
                .doubles("load_p", BusValidationData::getLoadP)
                .doubles("load_q", BusValidationData::getLoadQ)
                .doubles("gen_p", BusValidationData::getGenP)
                .doubles("gen_q", BusValidationData::getGenQ)
                .doubles("bat_p", BusValidationData::getBatP)
                .doubles("bat_q", BusValidationData::getBatQ)
                .doubles("shunt_p", BusValidationData::getShuntP)
                .doubles("shunt_q", BusValidationData::getShuntQ)
                .doubles("svc_p", BusValidationData::getSvcP)
                .doubles("svc_q", BusValidationData::getSvcQ)
                .doubles("vsc_p", BusValidationData::getVscCSP)
                .doubles("vsc_q", BusValidationData::getVscCSQ)
                .doubles("line_p", BusValidationData::getLineP)
                .doubles("line_q", BusValidationData::getLineQ)
                .doubles("dangling_line_p", BusValidationData::getDanglingLineP)
                .doubles("dangling_line_q", BusValidationData::getDanglingLineQ)
                .doubles("twt_p", BusValidationData::getTwtP)
                .doubles("twt_q", BusValidationData::getTwtQ)
                .doubles("tlt_p", BusValidationData::getTltP)
                .doubles("tlt_q", BusValidationData::getTltQ)
                .booleans("main_component", BusValidationData::isMainComponent)
                .booleans("validated", BusValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<List<GeneratorValidationData>, Void> generatorValidationsMapper() {
        return new DataframeMapperBuilder<List<GeneratorValidationData>, GeneratorValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", GeneratorValidationData::getId)
                .doubles("p", GeneratorValidationData::getP)
                .doubles("q", GeneratorValidationData::getQ)
                .doubles("v", GeneratorValidationData::getV)
                .doubles("target_p", GeneratorValidationData::getTargetP)
                .doubles("target_q", GeneratorValidationData::getTargetQ)
                .doubles("target_v", GeneratorValidationData::getTargetV)
                .doubles("expected_p", GeneratorValidationData::getExpectedP)
                .booleans("connected", GeneratorValidationData::isConnected)
                .booleans("voltage_regulator_on", GeneratorValidationData::isVoltageRegulatorOn)
                .doubles("min_p", GeneratorValidationData::getMinP)
                .doubles("max_p", GeneratorValidationData::getMaxP)
                .doubles("min_q", GeneratorValidationData::getMinQ)
                .doubles("max_q", GeneratorValidationData::getMaxQ)
                .booleans("main_component", GeneratorValidationData::isMainComponent)
                .booleans("validated", GeneratorValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<List<SvcValidationData>, Void> svcsValidationMapper() {
        return new DataframeMapperBuilder<List<SvcValidationData>, SvcValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", SvcValidationData::getSvcId)
                .doubles("p", SvcValidationData::getP)
                .doubles("q", SvcValidationData::getQ)
                .doubles("v_controlled", SvcValidationData::getvControlled)
                .doubles("v_controller", SvcValidationData::getvController)
                .doubles("nominal_v_controller", SvcValidationData::getNominalVcontroller)
                .doubles("reactive_power_setpoint", SvcValidationData::getReactivePowerSetpoint)
                .doubles("voltage_setpoint", SvcValidationData::getVoltageSetpoint)
                .booleans("connected", SvcValidationData::isConnected)
                .strings("mode", data -> data.getRegulationMode().name())
                .doubles("b_min", SvcValidationData::getbMin)
                .doubles("b_max", SvcValidationData::getbMax)
                .booleans("main_component", SvcValidationData::isMainComponent)
                .booleans("validated", SvcValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<List<ShuntValidationData>, Void> shuntsValidationMapper() {
        return new DataframeMapperBuilder<List<ShuntValidationData>, ShuntValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", ShuntValidationData::getShuntId)
                .doubles("q", ShuntValidationData::getQ)
                .doubles("expected_q", ShuntValidationData::getExpectedQ)
                .doubles("p", ShuntValidationData::getP)
                .ints("current_section_count", ShuntValidationData::getCurrentSectionCount)
                .ints("maximum_section_count", ShuntValidationData::getMaximumSectionCount)
                .doubles("b_per_section", ShuntValidationData::getbPerSection)
                .doubles("v", ShuntValidationData::getV)
                .booleans("connected", ShuntValidationData::isConnected)
                .doubles("q_max", ShuntValidationData::getqMax)
                .doubles("nominal_v", ShuntValidationData::getNominalV)
                .booleans("main_component", ShuntValidationData::isMainComponent)
                .booleans("validated", ShuntValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<List<TwtValidationData>, Void> twtsValidationMapper() {
        return new DataframeMapperBuilder<List<TwtValidationData>, TwtValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", TwtValidationData::getId)
                .doubles("error", TwtValidationData::getError)
                .doubles("up_increment", TwtValidationData::getUpIncrement)
                .doubles("down_increment", TwtValidationData::getDownIncrement)
                .doubles("rho", TwtValidationData::getRho)
                .doubles("rho_previous_step", TwtValidationData::getRhoPreviousStep)
                .doubles("rho_next_step", TwtValidationData::getRhoNextStep)
                .ints("tap_position", TwtValidationData::getTapPosition)
                .ints("low_tap_position", TwtValidationData::getLowTapPosition)
                .ints("high_tap_position", TwtValidationData::getHighTapPosition)
                .doubles("target_v", TwtValidationData::getTargetV)
                .strings("regulated_side", d -> d.getRegulatedSide().map(Enum::name).orElse(""))
                .doubles("v", TwtValidationData::getV)
                .booleans("connected", TwtValidationData::isConnected)
                .booleans("main_component", TwtValidationData::isMainComponent)
                .booleans("validated", TwtValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<List<T3wtValidationData>, Void> twt3wsValidationMapper() {
        return new DataframeMapperBuilder<List<T3wtValidationData>, T3wtValidationData, Void>()
                .itemsProvider(Function.identity())
                .stringsIndex("id", T3wtValidationData::getId)
                .doubles("p1", d -> d.getTwtData().getP(ThreeSides.ONE))
                .doubles("p2", d -> d.getTwtData().getP(ThreeSides.TWO))
                .doubles("p3", d -> d.getTwtData().getP(ThreeSides.THREE))
                .doubles("q1", d -> d.getTwtData().getQ(ThreeSides.ONE))
                .doubles("q2", d -> d.getTwtData().getQ(ThreeSides.TWO))
                .doubles("q3", d -> d.getTwtData().getQ(ThreeSides.THREE))
                .doubles("u1", d -> d.getTwtData().getU(ThreeSides.ONE))
                .doubles("u2", d -> d.getTwtData().getU(ThreeSides.TWO))
                .doubles("u3", d -> d.getTwtData().getU(ThreeSides.THREE))
                .doubles("theta1", d -> d.getTwtData().getTheta(ThreeSides.ONE))
                .doubles("theta2", d -> d.getTwtData().getTheta(ThreeSides.TWO))
                .doubles("theta3", d -> d.getTwtData().getTheta(ThreeSides.THREE))
                .doubles("r1", d -> d.getTwtData().getR(ThreeSides.ONE))
                .doubles("r2", d -> d.getTwtData().getR(ThreeSides.TWO))
                .doubles("r3", d -> d.getTwtData().getR(ThreeSides.THREE))
                .doubles("x1", d -> d.getTwtData().getX(ThreeSides.ONE))
                .doubles("x2", d -> d.getTwtData().getX(ThreeSides.TWO))
                .doubles("x3", d -> d.getTwtData().getX(ThreeSides.THREE))
                .doubles("g11", d -> d.getTwtData().getG1(ThreeSides.ONE))
                .doubles("g21", d -> d.getTwtData().getG1(ThreeSides.TWO))
                .doubles("g31", d -> d.getTwtData().getG1(ThreeSides.THREE))
                .doubles("b11", d -> d.getTwtData().getB1(ThreeSides.ONE))
                .doubles("b21", d -> d.getTwtData().getB1(ThreeSides.TWO))
                .doubles("b31", d -> d.getTwtData().getB1(ThreeSides.THREE))
                .doubles("g12", d -> d.getTwtData().getG2(ThreeSides.ONE))
                .doubles("g22", d -> d.getTwtData().getG2(ThreeSides.TWO))
                .doubles("g32", d -> d.getTwtData().getG2(ThreeSides.THREE))
                .doubles("b12", d -> d.getTwtData().getB2(ThreeSides.ONE))
                .doubles("b22", d -> d.getTwtData().getB2(ThreeSides.TWO))
                .doubles("b32", d -> d.getTwtData().getB2(ThreeSides.THREE))
                .doubles("computed_p1", d -> d.getTwtData().getComputedP(ThreeSides.ONE))
                .doubles("computed_p2", d -> d.getTwtData().getComputedP(ThreeSides.TWO))
                .doubles("computed_p3", d -> d.getTwtData().getComputedP(ThreeSides.THREE))
                .doubles("computed_q1", d -> d.getTwtData().getComputedQ(ThreeSides.ONE))
                .doubles("computed_q2", d -> d.getTwtData().getComputedQ(ThreeSides.TWO))
                .doubles("computed_q3", d -> d.getTwtData().getComputedQ(ThreeSides.THREE))
                .doubles("star_u", d -> d.getTwtData().getStarU())
                .doubles("star_theta", d -> d.getTwtData().getStarTheta())
                .booleans("validated", T3wtValidationData::isValidated)
                .build();
    }

    private Validations() {
    }
}
