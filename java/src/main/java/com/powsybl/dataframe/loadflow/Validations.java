/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class Validations {

    public static DataframeMapper<BranchValidationWriter> branchValidationsMapper() {
        return new DataframeMapperBuilder<BranchValidationWriter, BranchValidationWriter.ValidationData>()
                .itemsProvider(BranchValidationWriter::getList)
                .stringsIndex("id", BranchValidationWriter.ValidationData::getBranchId)
                .doubles("p1", BranchValidationWriter.ValidationData::getP1)
                .doubles("p1Calc", BranchValidationWriter.ValidationData::getP1Calc)
                .doubles("q1", BranchValidationWriter.ValidationData::getQ1)
                .doubles("q1Calc", BranchValidationWriter.ValidationData::getQ1Calc)
                .doubles("p2", BranchValidationWriter.ValidationData::getP2)
                .doubles("p2Calc", BranchValidationWriter.ValidationData::getP2Calc)
                .doubles("q2", BranchValidationWriter.ValidationData::getQ2)
                .doubles("q2Calc", BranchValidationWriter.ValidationData::getQ2Calc)
                .doubles("r", BranchValidationWriter.ValidationData::getR)
                .doubles("x", BranchValidationWriter.ValidationData::getX)
                .doubles("g1", BranchValidationWriter.ValidationData::getG1)
                .doubles("g2", BranchValidationWriter.ValidationData::getG2)
                .doubles("b1", BranchValidationWriter.ValidationData::getB1)
                .doubles("b2", BranchValidationWriter.ValidationData::getB2)
                .doubles("rho1", BranchValidationWriter.ValidationData::getRho1)
                .doubles("rho2", BranchValidationWriter.ValidationData::getRho2)
                .doubles("alpha1", BranchValidationWriter.ValidationData::getAlpha1)
                .doubles("alpha2", BranchValidationWriter.ValidationData::getAlpha2)
                .doubles("u1", BranchValidationWriter.ValidationData::getU1)
                .doubles("u2", BranchValidationWriter.ValidationData::getU2)
                .doubles("theta1", BranchValidationWriter.ValidationData::getTheta1)
                .doubles("theta2", BranchValidationWriter.ValidationData::getTheta2)
                .doubles("z", BranchValidationWriter.ValidationData::getZ)
                .doubles("y", BranchValidationWriter.ValidationData::getY)
                .doubles("ksi", BranchValidationWriter.ValidationData::getKsi)
                .ints("phaseAngleClock", BranchValidationWriter.ValidationData::getPhaseAngleClock)
                .booleans("connected1", BranchValidationWriter.ValidationData::isConnected1)
                .booleans("connected2", BranchValidationWriter.ValidationData::isConnected2)
                .booleans("mainComponent1", BranchValidationWriter.ValidationData::isMainComponent1)
                .booleans("mainComponent2", BranchValidationWriter.ValidationData::isMainComponent2)
                .booleans("validated", BranchValidationWriter.ValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<BusValidationWriter> busValidationsMapper() {
        return new DataframeMapperBuilder<BusValidationWriter, BusValidationWriter.ValidationData>()
                .itemsProvider(BusValidationWriter::getList)
                .stringsIndex("id", BusValidationWriter.ValidationData::getId)
                .doubles("incoming_p", BusValidationWriter.ValidationData::getIncomingP)
                .doubles("incoming_q", BusValidationWriter.ValidationData::getIncomingQ)
                .doubles("loadP", BusValidationWriter.ValidationData::getLoadP)
                .doubles("loadQ", BusValidationWriter.ValidationData::getLoadQ)
                .doubles("genP", BusValidationWriter.ValidationData::getGenP)
                .doubles("genQ", BusValidationWriter.ValidationData::getGenQ)
                .doubles("batP", BusValidationWriter.ValidationData::getBatP)
                .doubles("batQ", BusValidationWriter.ValidationData::getBatQ)
                .doubles("shuntP", BusValidationWriter.ValidationData::getShuntP)
                .doubles("shuntQ", BusValidationWriter.ValidationData::getShuntQ)
                .doubles("svcP", BusValidationWriter.ValidationData::getSvcP)
                .doubles("svcQ", BusValidationWriter.ValidationData::getSvcQ)
                .doubles("vscCSP", BusValidationWriter.ValidationData::getVscCSP)
                .doubles("vscCSQ", BusValidationWriter.ValidationData::getVscCSQ)
                .doubles("lineP", BusValidationWriter.ValidationData::getLineP)
                .doubles("lineQ", BusValidationWriter.ValidationData::getLineQ)
                .doubles("danglingLineP", BusValidationWriter.ValidationData::getDanglingLineP)
                .doubles("danglingLineQ", BusValidationWriter.ValidationData::getDanglingLineQ)
                .doubles("twtP", BusValidationWriter.ValidationData::getTwtP)
                .doubles("twtQ", BusValidationWriter.ValidationData::getTwtQ)
                .doubles("tltP", BusValidationWriter.ValidationData::getTltP)
                .doubles("tltQ", BusValidationWriter.ValidationData::getTltQ)
                .booleans("mainComponent", BusValidationWriter.ValidationData::isMainComponent)
                .booleans("validated", BusValidationWriter.ValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<GeneratorValidationWriter> generatorValidationsMapper() {
        return new DataframeMapperBuilder<GeneratorValidationWriter, GeneratorValidationWriter.ValidationData>()
                .itemsProvider(GeneratorValidationWriter::getList)
                .stringsIndex("id", GeneratorValidationWriter.ValidationData::getId)
                .doubles("p", GeneratorValidationWriter.ValidationData::getP)
                .doubles("q", GeneratorValidationWriter.ValidationData::getQ)
                .doubles("v", GeneratorValidationWriter.ValidationData::getV)
                .doubles("targetP", GeneratorValidationWriter.ValidationData::getTargetP)
                .doubles("targetQ", GeneratorValidationWriter.ValidationData::getTargetQ)
                .doubles("targetV", GeneratorValidationWriter.ValidationData::getTargetV)
                .doubles("expectedP", GeneratorValidationWriter.ValidationData::getExpectedP)
                .booleans("connected", GeneratorValidationWriter.ValidationData::isConnected)
                .booleans("voltageRegulatorOn", GeneratorValidationWriter.ValidationData::isVoltageRegulatorOn)
                .doubles("minP", GeneratorValidationWriter.ValidationData::getMinP)
                .doubles("maxP", GeneratorValidationWriter.ValidationData::getMaxP)
                .doubles("minQ", GeneratorValidationWriter.ValidationData::getMinQ)
                .doubles("maxQ", GeneratorValidationWriter.ValidationData::getMaxQ)
                .booleans("mainComponent", GeneratorValidationWriter.ValidationData::isMainComponent)
                .booleans("validated", GeneratorValidationWriter.ValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<SvcValidationWriter> svcsValidationMapper() {
        return new DataframeMapperBuilder<SvcValidationWriter, SvcValidationWriter.ValidationData>()
                .itemsProvider(SvcValidationWriter::getList)
                .stringsIndex("id", SvcValidationWriter.ValidationData::getSvcId)
                .doubles("p", SvcValidationWriter.ValidationData::getP)
                .doubles("q", SvcValidationWriter.ValidationData::getQ)
                .doubles("vControlled", SvcValidationWriter.ValidationData::getvControlled)
                .doubles("vController", SvcValidationWriter.ValidationData::getvController)
                .doubles("nominalVcontroller", SvcValidationWriter.ValidationData::getNominalVcontroller)
                .doubles("reactivePowerSetpoint", SvcValidationWriter.ValidationData::getReactivePowerSetpoint)
                .doubles("voltageSetpoint", SvcValidationWriter.ValidationData::getVoltageSetpoint)
                .booleans("connected", SvcValidationWriter.ValidationData::isConnected)
                .strings("mode", data -> data.getRegulationMode().name())
                .doubles("bMin", SvcValidationWriter.ValidationData::getbMin)
                .doubles("bMax", SvcValidationWriter.ValidationData::getbMax)
                .booleans("mainComponent", SvcValidationWriter.ValidationData::isMainComponent)
                .booleans("validated", SvcValidationWriter.ValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<ShuntValidationWriter> shuntsValidationMapper() {
        return new DataframeMapperBuilder<ShuntValidationWriter, ShuntValidationWriter.ValidationData>()
                .itemsProvider(ShuntValidationWriter::getList)
                .stringsIndex("id", ShuntValidationWriter.ValidationData::getShuntId)
                .doubles("q", ShuntValidationWriter.ValidationData::getQ)
                .doubles("expectedQ", ShuntValidationWriter.ValidationData::getExpectedQ)
                .doubles("p", ShuntValidationWriter.ValidationData::getP)
                .ints("currentSectionCount", ShuntValidationWriter.ValidationData::getCurrentSectionCount)
                .ints("maximumSectionCount", ShuntValidationWriter.ValidationData::getMaximumSectionCount)
                .doubles("bPerSection", ShuntValidationWriter.ValidationData::getbPerSection)
                .doubles("v", ShuntValidationWriter.ValidationData::getV)
                .booleans("connected", ShuntValidationWriter.ValidationData::isConnected)
                .doubles("qMax", ShuntValidationWriter.ValidationData::getqMax)
                .doubles("nominalV", ShuntValidationWriter.ValidationData::getNominalV)
                .booleans("mainComponent", ShuntValidationWriter.ValidationData::isMainComponent)
                .booleans("validated", ShuntValidationWriter.ValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<TwtValidationWriter> twtsValidationMapper() {
        return new DataframeMapperBuilder<TwtValidationWriter, TwtValidationWriter.ValidationData>()
                .itemsProvider(TwtValidationWriter::getList)
                .stringsIndex("id", TwtValidationWriter.ValidationData::getId)
                .doubles("error", TwtValidationWriter.ValidationData::getError)
                .doubles("upIncrement", TwtValidationWriter.ValidationData::getUpIncrement)
                .doubles("downIncrement", TwtValidationWriter.ValidationData::getDownIncrement)
                .doubles("rho", TwtValidationWriter.ValidationData::getRho)
                .doubles("rhoPreviousStep", TwtValidationWriter.ValidationData::getRhoPreviousStep)
                .doubles("rhoNextStep", TwtValidationWriter.ValidationData::getRhoNextStep)
                .ints("tapPosition", TwtValidationWriter.ValidationData::getTapPosition)
                .ints("lowTapPosition", TwtValidationWriter.ValidationData::getLowTapPosition)
                .ints("highTapPosition", TwtValidationWriter.ValidationData::getHighTapPosition)
                .doubles("targetV", TwtValidationWriter.ValidationData::getTargetV)
                .strings("regulatedSide", d -> d.getRegulatedSide().name())
                .doubles("v", TwtValidationWriter.ValidationData::getV)
                .booleans("connected", TwtValidationWriter.ValidationData::isConnected)
                .booleans("mainComponent", TwtValidationWriter.ValidationData::isMainComponent)
                .booleans("validated", TwtValidationWriter.ValidationData::isValidated)
                .build();
    }

    public static DataframeMapper<Twt3wValidationWriter> twt3wsValidationMapper() {
        return new DataframeMapperBuilder<Twt3wValidationWriter, Twt3wValidationWriter.ValidationData>()
                .itemsProvider(Twt3wValidationWriter::getList)
                .stringsIndex("id", Twt3wValidationWriter.ValidationData::getId)
                .doubles("p1", d -> d.getTwtData().getP(ThreeWindingsTransformer.Side.ONE))
                .doubles("p2", d -> d.getTwtData().getP(ThreeWindingsTransformer.Side.TWO))
                .doubles("p3", d -> d.getTwtData().getP(ThreeWindingsTransformer.Side.THREE))
                .doubles("q1", d -> d.getTwtData().getQ(ThreeWindingsTransformer.Side.ONE))
                .doubles("q2", d -> d.getTwtData().getQ(ThreeWindingsTransformer.Side.TWO))
                .doubles("q3", d -> d.getTwtData().getQ(ThreeWindingsTransformer.Side.THREE))
                .doubles("u1", d -> d.getTwtData().getU(ThreeWindingsTransformer.Side.ONE))
                .doubles("u2", d -> d.getTwtData().getU(ThreeWindingsTransformer.Side.TWO))
                .doubles("u3", d -> d.getTwtData().getU(ThreeWindingsTransformer.Side.THREE))
                .doubles("theta1", d -> d.getTwtData().getTheta(ThreeWindingsTransformer.Side.ONE))
                .doubles("theta2", d -> d.getTwtData().getTheta(ThreeWindingsTransformer.Side.TWO))
                .doubles("theta3", d -> d.getTwtData().getTheta(ThreeWindingsTransformer.Side.THREE))
                .doubles("r1", d -> d.getTwtData().getR(ThreeWindingsTransformer.Side.ONE))
                .doubles("r2", d -> d.getTwtData().getR(ThreeWindingsTransformer.Side.TWO))
                .doubles("r3", d -> d.getTwtData().getR(ThreeWindingsTransformer.Side.THREE))
                .doubles("x1", d -> d.getTwtData().getX(ThreeWindingsTransformer.Side.ONE))
                .doubles("x2", d -> d.getTwtData().getX(ThreeWindingsTransformer.Side.TWO))
                .doubles("x3", d -> d.getTwtData().getX(ThreeWindingsTransformer.Side.THREE))
                .doubles("g11", d -> d.getTwtData().getG1(ThreeWindingsTransformer.Side.ONE))
                .doubles("g21", d -> d.getTwtData().getG1(ThreeWindingsTransformer.Side.TWO))
                .doubles("g31", d -> d.getTwtData().getG1(ThreeWindingsTransformer.Side.THREE))
                .doubles("b11", d -> d.getTwtData().getB1(ThreeWindingsTransformer.Side.ONE))
                .doubles("b21", d -> d.getTwtData().getB1(ThreeWindingsTransformer.Side.TWO))
                .doubles("b31", d -> d.getTwtData().getB1(ThreeWindingsTransformer.Side.THREE))
                .doubles("g12", d -> d.getTwtData().getG2(ThreeWindingsTransformer.Side.ONE))
                .doubles("g22", d -> d.getTwtData().getG2(ThreeWindingsTransformer.Side.TWO))
                .doubles("g32", d -> d.getTwtData().getG2(ThreeWindingsTransformer.Side.THREE))
                .doubles("b12", d -> d.getTwtData().getB2(ThreeWindingsTransformer.Side.ONE))
                .doubles("b22", d -> d.getTwtData().getB2(ThreeWindingsTransformer.Side.TWO))
                .doubles("b32", d -> d.getTwtData().getB2(ThreeWindingsTransformer.Side.THREE))
                .doubles("computed_p1", d -> d.getTwtData().getComputedP(ThreeWindingsTransformer.Side.ONE))
                .doubles("computed_p2", d -> d.getTwtData().getComputedP(ThreeWindingsTransformer.Side.TWO))
                .doubles("computed_p3", d -> d.getTwtData().getComputedP(ThreeWindingsTransformer.Side.THREE))
                .doubles("computed_q1", d -> d.getTwtData().getComputedQ(ThreeWindingsTransformer.Side.ONE))
                .doubles("computed_q2", d -> d.getTwtData().getComputedQ(ThreeWindingsTransformer.Side.TWO))
                .doubles("computed_q3", d -> d.getTwtData().getComputedQ(ThreeWindingsTransformer.Side.THREE))
                .doubles("starU", d -> d.getTwtData().getStarU())
                .doubles("starTheta", d -> d.getTwtData().getStarTheta())
                .booleans("validated", Twt3wValidationWriter.ValidationData::isValidated)
                .build();
    }

    private Validations() {
    }
}
