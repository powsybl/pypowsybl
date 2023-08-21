/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.shortcircuit.adders.ShortCircuitFaultAdderFactory;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframeMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ShortCircuitFaultType;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.network.NetworkCFunctions;
import com.powsybl.shortcircuit.*;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.powsybl.python.commons.Util.createCharPtrArray;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * C functions related to short-circuit analysis.
 *
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@CContext(Directives.class)
public final class ShortCircuitAnalysisCFunctions {

    private ShortCircuitAnalysisCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(ShortCircuitAnalysisCFunctions.class);
    }

    @CEntryPoint(name = "getShortCircuitAnalysisProviderNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getShortCircuitAnalysisProviderNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(ShortCircuitAnalysisProvider.findAll()
                .stream().map(ShortCircuitAnalysisProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "setDefaultShortCircuitAnalysisProvider")
    public static void setDefaultShortCircuitAnalysisProvider(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultShortCircuitAnalysisProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultShortCircuitAnalysisProvider")
    public static CCharPointer getDefaultShortCircuitAnalysisProvider(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultShortCircuitAnalysisProvider()));
    }

    @CEntryPoint(name = "createShortCircuitAnalysis")
    public static ObjectHandle createShortCircuitAnalysis(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new ShortCircuitAnalysisContext()));
    }

    private static ShortCircuitAnalysisProvider getProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultShortCircuitAnalysisProvider() : name;
        return ShortCircuitAnalysisProvider.findAll().stream()
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No short-circuit analysis provider for name '" + actualName + "'"));
    }

    @CEntryPoint(name = "runShortCircuitAnalysis")
    public static ObjectHandle runShortCircuitAnalysis(IsolateThread thread, ObjectHandle shortCircuitAnalysisContextHandle,
                                                   ObjectHandle networkHandle, ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer,
                                                   CCharPointer providerName, ObjectHandle reporterHandle,
                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ShortCircuitAnalysisContext analysisContext = ObjectHandles.getGlobal().get(shortCircuitAnalysisContextHandle);

            Network network = ObjectHandles.getGlobal().get(networkHandle);

            ShortCircuitAnalysisProvider provider = getProvider(CTypeUtil.toString(providerName));
            logger().info("Short-circuit analysis provider used for short-circuit analysis is : {}", provider.getName());
            ShortCircuitParameters shortCircuitAnalysisParameters = ShortCircuitAnalysisCUtils.createShortCircuitAnalysisParameters(shortCircuitAnalysisParametersPointer, provider);

            ReporterModel reporter = ObjectHandles.getGlobal().get(reporterHandle);
            ShortCircuitAnalysisResult results = analysisContext.run(network, shortCircuitAnalysisParameters, provider.getName(), reporter);
            return ObjectHandles.getGlobal().create(results);
        });
    }

    @CEntryPoint(name = "freeShortCircuitAnalysisParameters")
    public static void freeShortCircuitAnalysisParameters(IsolateThread thread, ShortCircuitAnalysisParametersPointer parameters,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            UnmanagedMemory.free(parameters);
        });
    }

    @CEntryPoint(name = "createShortCircuitAnalysisParameters")
    public static ShortCircuitAnalysisParametersPointer createShortCircuitAnalysisParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToShortCircuitAnalysisParametersPointer(ShortCircuitAnalysisCUtils.createShortCircuitAnalysisParameters()));
    }

    private static ShortCircuitAnalysisParametersPointer convertToShortCircuitAnalysisParametersPointer(ShortCircuitParameters parameters) {
        ShortCircuitAnalysisParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(ShortCircuitAnalysisParametersPointer.class));
        paramsPtr.setWithVoltageResult(parameters.isWithVoltageResult());
        paramsPtr.setWithFeederResult(parameters.isWithFeederResult());
        paramsPtr.setWithLimitViolations(parameters.isWithLimitViolations());
        paramsPtr.setWithFortescueResult(parameters.isWithFortescueResult());
        paramsPtr.setMinVoltageDropProportionalThreshold(parameters.getMinVoltageDropProportionalThreshold());
        paramsPtr.setStudyType(parameters.getStudyType().ordinal());
        paramsPtr.setProviderParametersValuesCount(0);
        paramsPtr.setProviderParametersKeysCount(0);
        return paramsPtr;
    }

    static List<String> getSpecificParametersNames(ShortCircuitAnalysisProvider provider) {
        // currently, the short-circuit APIs do not have this method.
        // there is a List<Parameter> getSpecificParameters(), but its semantic must be checked
        return Collections.emptyList();
    }

    @CEntryPoint(name = "getShortCircuitAnalysisProviderParametersNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String providerStr = CTypeUtil.toString(provider);
            return Util.createCharPtrArray(getSpecificParametersNames(ShortCircuitAnalysisCUtils.getShortCircuitAnalysisProvider(providerStr)));
        });
    }

    @CEntryPoint(name = "getFaultsDataframeMetaData")
    public static DataframeMetadataPointer getFaultsDataframeMetaData(IsolateThread thread,
                                                                      ShortCircuitFaultType mappingType,
                                                                    PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            return CTypeUtil.createSeriesMetadata(ShortCircuitFaultAdderFactory.getAdder(mappingType).getMetadata());
        });
    }

    @CEntryPoint(name = "setFaults")
    public static void setFaults(IsolateThread thread, ObjectHandle contextHandle,
                                 ShortCircuitFaultType faultType,
                                 PyPowsyblApiHeader.DataframePointer cDataframe,
                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            ShortCircuitAnalysisContext context = ObjectHandles.getGlobal().get(contextHandle);
            UpdatingDataframe faultDataframe = NetworkCFunctions.createDataframe(cDataframe);
            ShortCircuitFaultAdderFactory.getAdder(faultType).addElements(context, faultDataframe);
        });
    }

    @CEntryPoint(name = "getFaultResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getFaultResults(IsolateThread thread, ObjectHandle shortCircuitAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisFaultResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "getMagnitudeFeederResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getMagnitudeFeederResults(IsolateThread thread, ObjectHandle shortCircuitAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisMagnitudeFeederResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "getLimitViolationsResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getLimitViolationsResults(IsolateThread thread, ObjectHandle shortCircuitAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisLimitViolationsResultsMapper(), result);
        });
    }
}
