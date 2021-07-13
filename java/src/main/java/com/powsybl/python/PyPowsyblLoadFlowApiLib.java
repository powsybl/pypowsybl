/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.loadflow.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import static com.powsybl.python.PyPowsyblApiHeader.*;
import static com.powsybl.python.Util.doCatch;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
@CContext(Directives.class)
public final class PyPowsyblLoadFlowApiLib {

    private PyPowsyblLoadFlowApiLib() {
    }

    @CEntryPoint(name = "runLoadFlowValidation")
    public static ArrayPointer<SeriesPointer> createLoadFlowValidationSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                                  ElementType elementType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createLoadFlowValidationSeriesArray(network, elementType);
        });
    }

    static ArrayPointer<SeriesPointer> createLoadFlowValidationSeriesArray(Network network, ElementType elementType) {
        DefaultInMemoryValidationWriter writer = createLoadFlowValidationWriter(network, elementType);
        return createCDataFrame(writer, elementType);
    }

    // package scope for unit testing
    static DefaultInMemoryValidationWriter createLoadFlowValidationWriter(Network network, ElementType elementType) {
        ValidationConfig validationConfig = ValidationConfig.load();
        DefaultInMemoryValidationWriter writer;
        switch (elementType) {
            case BRANCH:
                writer = new BranchValidationWriter();
                ValidationType.FLOWS.check(network, validationConfig, writer);
                break;
            case BUS:
                writer = new BusValidationWriter();
                ValidationType.BUSES.check(network, validationConfig, writer);
                break;
            case GENERATOR:
                writer = new GeneratorValidationWriter();
                ValidationType.GENERATORS.check(network, validationConfig, writer);
                break;
            case STATIC_VAR_COMPENSATOR:
                writer = new SvcValidationWriter();
                ValidationType.SVCS.check(network, validationConfig, writer);
                break;
            case SHUNT_COMPENSATOR:
                writer = new ShuntValidationWriter();
                ValidationType.SHUNTS.check(network, validationConfig, writer);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                writer = new TwtValidationWriter();
                ValidationType.TWTS.check(network, validationConfig, writer);
                break;
            case THREE_WINDINGS_TRANSFORMER:
                writer = new Twt3wValidationWriter();
                ValidationType.TWTS3W.check(network, validationConfig, writer);
                break;
            default:
                throw new PowsyblException("Validation '" + elementType + "' not supported");
        }
        return writer;
    }

    private static ArrayPointer<SeriesPointer> createCDataFrame(DefaultInMemoryValidationWriter validationWriter, ElementType elementType) {
        switch (elementType) {
            case BRANCH:
                return Dataframes.createCDataframe(Validations.branchValidationsMapper(), (BranchValidationWriter) validationWriter);
            case BUS:
                return Dataframes.createCDataframe(Validations.busValidationsMapper(), (BusValidationWriter) validationWriter);
            case GENERATOR:
                return Dataframes.createCDataframe(Validations.generatorValidationsMapper(), (GeneratorValidationWriter) validationWriter);
            case STATIC_VAR_COMPENSATOR:
                return Dataframes.createCDataframe(Validations.svcsValidationMapper(), (SvcValidationWriter) validationWriter);
            case SHUNT_COMPENSATOR:
                return Dataframes.createCDataframe(Validations.shuntsValidationMapper(), (ShuntValidationWriter) validationWriter);
            case TWO_WINDINGS_TRANSFORMER:
                return Dataframes.createCDataframe(Validations.twtsValidationMapper(), (TwtValidationWriter) validationWriter);
            case THREE_WINDINGS_TRANSFORMER:
                return Dataframes.createCDataframe(Validations.twt3wsValidationMapper(), (Twt3wValidationWriter) validationWriter);
            default:
                throw new PowsyblException("Validation '" + elementType + "' not supported");
        }
    }
}
