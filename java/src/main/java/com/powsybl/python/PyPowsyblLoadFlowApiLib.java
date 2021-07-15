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
                                                                                  PyPowsyblApiHeader.ValidationType validationType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createLoadFlowValidationSeriesArray(network, validationType);
        });
    }

    static ArrayPointer<SeriesPointer> createLoadFlowValidationSeriesArray(Network network, PyPowsyblApiHeader.ValidationType validationType) {
        DefaultInMemoryValidationWriter writer = createLoadFlowValidationWriter(network, validationType);
        return createCDataFrame(writer, validationType);
    }

    // package scope for unit testing
    static DefaultInMemoryValidationWriter createLoadFlowValidationWriter(Network network, PyPowsyblApiHeader.ValidationType validationType) {
        ValidationConfig validationConfig = ValidationConfig.load();
        DefaultInMemoryValidationWriter writer;
        switch (validationType) {
            case FLOWS:
                writer = new BranchValidationWriter();
                ValidationType.FLOWS.check(network, validationConfig, writer);
                break;
            case BUSES:
                writer = new BusValidationWriter();
                ValidationType.BUSES.check(network, validationConfig, writer);
                break;
            case GENERATORS:
                writer = new GeneratorValidationWriter();
                ValidationType.GENERATORS.check(network, validationConfig, writer);
                break;
            case SVCS:
                writer = new SvcValidationWriter();
                ValidationType.SVCS.check(network, validationConfig, writer);
                break;
            case SHUNTS:
                writer = new ShuntValidationWriter();
                ValidationType.SHUNTS.check(network, validationConfig, writer);
                break;
            case TWTS:
                writer = new TwtValidationWriter();
                ValidationType.TWTS.check(network, validationConfig, writer);
                break;
            case TWTS3W:
                writer = new Twt3wValidationWriter();
                ValidationType.TWTS3W.check(network, validationConfig, writer);
                break;
            default:
                throw new PowsyblException("Validation '" + validationType + "' not supported");
        }
        return writer;
    }

    private static ArrayPointer<SeriesPointer> createCDataFrame(DefaultInMemoryValidationWriter validationWriter, PyPowsyblApiHeader.ValidationType validationType) {
        switch (validationType) {
            case FLOWS:
                return Dataframes.createCDataframe(Validations.branchValidationsMapper(), (BranchValidationWriter) validationWriter);
            case BUSES:
                return Dataframes.createCDataframe(Validations.busValidationsMapper(), (BusValidationWriter) validationWriter);
            case GENERATORS:
                return Dataframes.createCDataframe(Validations.generatorValidationsMapper(), (GeneratorValidationWriter) validationWriter);
            case SVCS:
                return Dataframes.createCDataframe(Validations.svcsValidationMapper(), (SvcValidationWriter) validationWriter);
            case SHUNTS:
                return Dataframes.createCDataframe(Validations.shuntsValidationMapper(), (ShuntValidationWriter) validationWriter);
            case TWTS:
                return Dataframes.createCDataframe(Validations.twtsValidationMapper(), (TwtValidationWriter) validationWriter);
            case TWTS3W:
                return Dataframes.createCDataframe(Validations.twt3wsValidationMapper(), (Twt3wValidationWriter) validationWriter);
            default:
                throw new PowsyblException("Validation '" + validationType + "' not supported");
        }
    }
}
