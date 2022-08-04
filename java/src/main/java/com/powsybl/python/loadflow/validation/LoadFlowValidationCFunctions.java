/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.loadflow.validation;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.loadflow.validation.InMemoryValidationWriter;
import com.powsybl.dataframe.loadflow.validation.Validations;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.network.Dataframes;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import static com.powsybl.python.commons.PyPowsyblApiHeader.*;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * Defines C interface for loadflow validation.
 *
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
@CContext(Directives.class)
public final class LoadFlowValidationCFunctions {

    private LoadFlowValidationCFunctions() {
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
        InMemoryValidationWriter writer = createLoadFlowValidationWriter(network, validationType);
        return createCDataFrame(writer, validationType);
    }

    private static ValidationConfig defaultValidationConfig() {
        return new ValidationConfig(ValidationConfig.THRESHOLD_DEFAULT, ValidationConfig.VERBOSE_DEFAULT, null,
                ValidationConfig.TABLE_FORMATTER_FACTORY_DEFAULT, ValidationConfig.EPSILON_X_DEFAULT, ValidationConfig.APPLY_REACTANCE_CORRECTION_DEFAULT,
                ValidationConfig.VALIDATION_OUTPUT_WRITER_DEFAULT, new LoadFlowParameters(), ValidationConfig.OK_MISSING_VALUES_DEFAULT,
                ValidationConfig.NO_REQUIREMENT_IF_REACTIVE_BOUND_INVERSION_DEFAULT, ValidationConfig.COMPARE_RESULTS_DEFAULT,
                ValidationConfig.CHECK_MAIN_COMPONENT_ONLY_DEFAULT, ValidationConfig.NO_REQUIREMENT_IF_SETPOINT_OUTSIDE_POWERS_BOUNDS);
    }

    // package scope for unit testing
    static InMemoryValidationWriter createLoadFlowValidationWriter(Network network, PyPowsyblApiHeader.ValidationType validationType) {
        ValidationConfig validationConfig = PyPowsyblConfiguration.isReadConfig() ? ValidationConfig.load() : defaultValidationConfig();
        InMemoryValidationWriter writer = new InMemoryValidationWriter();
        switch (validationType) {
            case FLOWS:
                ValidationType.FLOWS.check(network, validationConfig, writer);
                break;
            case BUSES:
                ValidationType.BUSES.check(network, validationConfig, writer);
                break;
            case GENERATORS:
                ValidationType.GENERATORS.check(network, validationConfig, writer);
                break;
            case SVCS:
                ValidationType.SVCS.check(network, validationConfig, writer);
                break;
            case SHUNTS:
                ValidationType.SHUNTS.check(network, validationConfig, writer);
                break;
            case TWTS:
                ValidationType.TWTS.check(network, validationConfig, writer);
                break;
            case TWTS3W:
                ValidationType.TWTS3W.check(network, validationConfig, writer);
                break;
            default:
                throw new PowsyblException("Validation '" + validationType + "' not supported");
        }
        return writer;
    }

    private static ArrayPointer<SeriesPointer> createCDataFrame(InMemoryValidationWriter validationWriter, PyPowsyblApiHeader.ValidationType validationType) {
        switch (validationType) {
            case FLOWS:
                return Dataframes.createCDataframe(Validations.branchValidationsMapper(), validationWriter.getBranchData());
            case BUSES:
                return Dataframes.createCDataframe(Validations.busValidationsMapper(), validationWriter.getBusData());
            case GENERATORS:
                return Dataframes.createCDataframe(Validations.generatorValidationsMapper(), validationWriter.getGeneratorData());
            case SVCS:
                return Dataframes.createCDataframe(Validations.svcsValidationMapper(), validationWriter.getSvcData());
            case SHUNTS:
                return Dataframes.createCDataframe(Validations.shuntsValidationMapper(), validationWriter.getShuntData());
            case TWTS:
                return Dataframes.createCDataframe(Validations.twtsValidationMapper(), validationWriter.getTwtData());
            case TWTS3W:
                return Dataframes.createCDataframe(Validations.twt3wsValidationMapper(), validationWriter.getT3wtData());
            default:
                throw new PowsyblException("Validation '" + validationType + "' not supported");
        }
    }
}
