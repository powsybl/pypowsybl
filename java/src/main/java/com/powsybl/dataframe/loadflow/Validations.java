/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class Validations {

    public static DataframeMapper<DefaultInMemoryValidationWriter<BranchValidationWriter.ValidationData>> branchValidationsMapper() {
        return new DataframeMapperBuilder<DefaultInMemoryValidationWriter<BranchValidationWriter.ValidationData>, BranchValidationWriter.ValidationData>()
                .itemsProvider(DefaultInMemoryValidationWriter::getList)
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

    public static DataframeMapper<DefaultInMemoryValidationWriter<BusValidationWriter.ValidationData>> busValidationsMapper() {
        return new DataframeMapperBuilder<DefaultInMemoryValidationWriter<BusValidationWriter.ValidationData>, BusValidationWriter.ValidationData>()
                .itemsProvider(DefaultInMemoryValidationWriter::getList)
                .stringsIndex("id", BusValidationWriter.ValidationData::getId)
                .doubles("incoming_p", BusValidationWriter.ValidationData::getIncomingP)
                .build();
    }

    public static DataframeMapper<DefaultInMemoryValidationWriter<GeneratorValidationWriter.ValidationData>> generatorValidationsMapper() {
        return new DataframeMapperBuilder<DefaultInMemoryValidationWriter<GeneratorValidationWriter.ValidationData>, GeneratorValidationWriter.ValidationData>()
                .itemsProvider(DefaultInMemoryValidationWriter::getList)
                .stringsIndex("id", GeneratorValidationWriter.ValidationData::getId)
                .doubles("p", GeneratorValidationWriter.ValidationData::getP)
                .build();
    }

    private Validations() {
    }
}
