/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
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
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.loadflow.LoadFlowCFunctions;

import java.util.Optional;

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
                                                                                  PyPowsyblApiHeader.ValidationType validationType,
                                                                                  PyPowsyblApiHeader.LoadFlowValidationParametersPointer loadFlowValidationParametersPtr,
                                                                                  ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createLoadFlowValidationSeriesArray(network, validationType, loadFlowValidationParametersPtr);
        });
    }

    static ArrayPointer<SeriesPointer> createLoadFlowValidationSeriesArray(Network network, PyPowsyblApiHeader.ValidationType validationType,
                                                                           PyPowsyblApiHeader.LoadFlowValidationParametersPointer loadFlowValidationParametersPtr) {
        ValidationConfig validationConfig = createValidationConfig(loadFlowValidationParametersPtr);
        InMemoryValidationWriter writer = createLoadFlowValidationWriter(network, validationType, validationConfig);
        return createCDataFrame(writer, validationType);
    }

    private static ValidationConfig defaultValidationConfig() {
        return new ValidationConfig(ValidationConfig.THRESHOLD_DEFAULT, ValidationConfig.VERBOSE_DEFAULT, null,
                ValidationConfig.TABLE_FORMATTER_FACTORY_DEFAULT, ValidationConfig.EPSILON_X_DEFAULT, ValidationConfig.APPLY_REACTANCE_CORRECTION_DEFAULT,
                ValidationConfig.VALIDATION_OUTPUT_WRITER_DEFAULT, new LoadFlowParameters(), ValidationConfig.OK_MISSING_VALUES_DEFAULT,
                ValidationConfig.NO_REQUIREMENT_IF_REACTIVE_BOUND_INVERSION_DEFAULT, ValidationConfig.COMPARE_RESULTS_DEFAULT,
                ValidationConfig.CHECK_MAIN_COMPONENT_ONLY_DEFAULT, ValidationConfig.NO_REQUIREMENT_IF_SETPOINT_OUTSIDE_POWERS_BOUNDS);
    }

    static InMemoryValidationWriter createLoadFlowValidationWriter(Network network, PyPowsyblApiHeader.ValidationType validationType) {
        ValidationConfig validationConfig = defaultValidationConfig();
        return createLoadFlowValidationWriter(network, validationType, validationConfig);
    }

    // package scope for unit testing
    static InMemoryValidationWriter createLoadFlowValidationWriter(Network network, PyPowsyblApiHeader.ValidationType validationType,
                                                                   ValidationConfig validationConfig) {
        InMemoryValidationWriter writer = new InMemoryValidationWriter();
        switch (validationType) {
            case FLOWS -> ValidationType.FLOWS.check(network, validationConfig, writer);
            case BUSES -> ValidationType.BUSES.check(network, validationConfig, writer);
            case GENERATORS -> ValidationType.GENERATORS.check(network, validationConfig, writer);
            case SVCS -> ValidationType.SVCS.check(network, validationConfig, writer);
            case SHUNTS -> ValidationType.SHUNTS.check(network, validationConfig, writer);
            case TWTS -> ValidationType.TWTS.check(network, validationConfig, writer);
            case TWTS3W -> ValidationType.TWTS3W.check(network, validationConfig, writer);
            default -> throw new PowsyblException("Validation '" + validationType + "' not supported");
        }
        return writer;
    }

    private static ArrayPointer<SeriesPointer> createCDataFrame(InMemoryValidationWriter validationWriter, PyPowsyblApiHeader.ValidationType validationType) {
        return switch (validationType) {
            case FLOWS ->
                    Dataframes.createCDataframe(Validations.branchValidationsMapper(), validationWriter.getBranchData());
            case BUSES ->
                    Dataframes.createCDataframe(Validations.busValidationsMapper(), validationWriter.getBusData());
            case GENERATORS ->
                    Dataframes.createCDataframe(Validations.generatorValidationsMapper(), validationWriter.getGeneratorData());
            case SVCS -> Dataframes.createCDataframe(Validations.svcsValidationMapper(), validationWriter.getSvcData());
            case SHUNTS ->
                    Dataframes.createCDataframe(Validations.shuntsValidationMapper(), validationWriter.getShuntData());
            case TWTS -> Dataframes.createCDataframe(Validations.twtsValidationMapper(), validationWriter.getTwtData());
            case TWTS3W ->
                    Dataframes.createCDataframe(Validations.twt3wsValidationMapper(), validationWriter.getT3wtData());
        };
    }

    @CEntryPoint(name = "createValidationConfig")
    public static LoadFlowValidationParametersPointer createValidationConfig(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToLoadFlowValidationParametersPointer(createValidationConfig()));
    }

    public static void copyToCLoadFlowValidationParameters(ValidationConfig parameters, LoadFlowValidationParametersPointer cParameters) {
        cParameters.setThreshold(parameters.getThreshold());
        cParameters.setVerbose(parameters.isVerbose());
        Optional<String> optionalName = parameters.getLoadFlowName();
        if (optionalName.isPresent()) {
            cParameters.setLoadFlowName(CTypeUtil.toCharPtr(optionalName.get()));
        } else {
            cParameters.setLoadFlowName(CTypeUtil.toCharPtr(""));
        }
        cParameters.setEpsilonX(parameters.getEpsilonX());
        cParameters.setApplyReactanceCorrection(parameters.applyReactanceCorrection());
        LoadFlowCFunctions.copyToCLoadFlowParameters(parameters.getLoadFlowParameters(), cParameters.getLoadFlowParameters());
        cParameters.setOkMissingValues(parameters.areOkMissingValues());
        cParameters.setNoRequirementIfReactiveBoundInversion(parameters.isNoRequirementIfReactiveBoundInversion());
        cParameters.setCompareResults(parameters.isCompareResults());
        cParameters.setCheckMainComponentOnly(parameters.isCheckMainComponentOnly());
        cParameters.setNoRequirementIfSetpointOutsidePowerBounds(parameters.isNoRequirementIfSetpointOutsidePowerBounds());
    }

    public static LoadFlowValidationParametersPointer convertToLoadFlowValidationParametersPointer(ValidationConfig parameters) {
        LoadFlowValidationParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(LoadFlowValidationParametersPointer.class));
        copyToCLoadFlowValidationParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    @CEntryPoint(name = "freeValidationConfig")
    public static void freeValidationConfig(IsolateThread thread, LoadFlowValidationParametersPointer loadFlowValidationParametersPtr,
                                            ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeLoadFlowValidationParametersPointer(loadFlowValidationParametersPtr));
    }

    public static void freeLoadFlowValidationParametersPointer(LoadFlowValidationParametersPointer loadFlowValidationParametersPtr) {
        LoadFlowCUtils.freeLoadFlowParametersContent(loadFlowValidationParametersPtr.getLoadFlowParameters());
        UnmanagedMemory.free(loadFlowValidationParametersPtr);
    }

    public static ValidationConfig createValidationConfig() {
        return PyPowsyblConfiguration.isReadConfig() ? ValidationConfig.load() : defaultValidationConfig();
    }

    private static ValidationConfig createValidationConfig(PyPowsyblApiHeader.LoadFlowValidationParametersPointer loadFlowValidationParametersPtr) {
        ValidationConfig validationConfig = createValidationConfig();
        validationConfig.setThreshold(loadFlowValidationParametersPtr.getThreshold());
        validationConfig.setVerbose(loadFlowValidationParametersPtr.isVerbose());
        validationConfig.setLoadFlowName(CTypeUtil.toString(loadFlowValidationParametersPtr.getLoadFlowName()));
        validationConfig.setEpsilonX(loadFlowValidationParametersPtr.getEpsilonX());
        validationConfig.setApplyReactanceCorrection(loadFlowValidationParametersPtr.isApplyReactanceCorrection());
        validationConfig.setLoadFlowParameters(LoadFlowCUtils.convertLoadFlowParameters(false, loadFlowValidationParametersPtr.getLoadFlowParameters()));
        validationConfig.setOkMissingValues(loadFlowValidationParametersPtr.isOkMissingValues());
        validationConfig.setNoRequirementIfReactiveBoundInversion(loadFlowValidationParametersPtr.isNoRequirementIfReactiveBoundInversion());
        validationConfig.setCompareResults(loadFlowValidationParametersPtr.isCompareResults());
        validationConfig.setCheckMainComponentOnly(loadFlowValidationParametersPtr.isCheckMainComponentOnly());
        validationConfig.setNoRequirementIfSetpointOutsidePowerBounds(loadFlowValidationParametersPtr.isNoRequirementIfSetpointOutsidePowerBounds());
        return validationConfig;
    }
}
