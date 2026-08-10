/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.dynamic.MarginCalculationDataframeMappersUtils;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters;
import com.powsybl.dynawo.margincalculation.results.MarginCalculationResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.MarginCalculationParametersPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.commons.Util.PointerProvider;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.report.ReportCUtils;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.doCatch;
import static com.powsybl.python.commons.Util.freeProviderParameters;
import static com.powsybl.python.dynamic.MarginCalculationParametersCUtils.copyToCMarginCalculationParameters;

/**
 * C functions related to margin calculation.
 *
 * <p>Contingencies are added through the generic contingency container entry points
 * ({@code addContingency}, {@code addContingencyFromJsonFile}).
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@SuppressWarnings({"java:S1602", "java:S1604", "Convert2Lambda"})
@CContext(Directives.class)
public final class MarginCalculationCFunctions {

    private MarginCalculationCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(MarginCalculationCFunctions.class);
    }

    @CEntryPoint(name = "createMarginCalculation")
    public static ObjectHandle createMarginCalculation(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new MarginCalculationContext()));
    }

    @CEntryPoint(name = "createLoadsVariationMapping")
    public static ObjectHandle createLoadsVariationMapping(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonLoadsVariationSupplier()));
    }

    @CEntryPoint(name = "addLoadsVariation")
    public static void addLoadsVariation(IsolateThread thread, ObjectHandle loadsVariationSupplierHandle,
                                         CCharPointerPointer loadIdsPtr, int loadIdsCount, double variationValue,
                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                PythonLoadsVariationSupplier loadsVariationSupplier = ObjectHandles.getGlobal().get(loadsVariationSupplierHandle);
                List<String> loadIds = toStringList(loadIdsPtr, loadIdsCount);
                loadsVariationSupplier.addLoadsVariation(loadIds, variationValue);
            }
        });
    }

    @CEntryPoint(name = "createMarginCalculationParameters")
    public static MarginCalculationParametersPointer createMarginCalculationParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<MarginCalculationParametersPointer>() {
            @Override
            public MarginCalculationParametersPointer get() {
                MarginCalculationParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(MarginCalculationParametersPointer.class));
                copyToCMarginCalculationParameters(paramsPtr);
                return paramsPtr;
            }
        });
    }

    @CEntryPoint(name = "freeMarginCalculationParameters")
    public static void freeMarginCalculationParameters(IsolateThread thread, MarginCalculationParametersPointer parametersPtr,
                                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                // debug_dir is a char* scalar: it is freed on the C++ side when read (toString),
                // like other char* scalar fields, so it must not be freed here.
                freeProviderParameters(parametersPtr.getProviderParameters());
                UnmanagedMemory.free(parametersPtr);
            }
        });
    }

    @CEntryPoint(name = "runMarginCalculation")
    public static ObjectHandle runMarginCalculation(IsolateThread thread,
                                                    ObjectHandle marginCalculationContextHandle,
                                                    ObjectHandle networkHandle,
                                                    ObjectHandle dynamicMappingHandle,
                                                    ObjectHandle loadsVariationSupplierHandle,
                                                    MarginCalculationParametersPointer parametersPtr,
                                                    ObjectHandle reportNodeHandle,
                                                    ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                MarginCalculationContext marginCalculationContext = ObjectHandles.getGlobal().get(marginCalculationContextHandle);
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
                PythonLoadsVariationSupplier loadsVariationSupplier = ObjectHandles.getGlobal().get(loadsVariationSupplierHandle);
                ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
                MarginCalculationParameters marginCalculationParameters =
                        MarginCalculationParametersCUtils.createMarginCalculationParameters(parametersPtr);
                MarginCalculationResult result = marginCalculationContext.run(network,
                        dynamicMapping,
                        loadsVariationSupplier,
                        marginCalculationParameters,
                        reportNode);
                logger().info("Margin calculation ran successfully in java");
                return ObjectHandles.getGlobal().create(result);
            }
        });
    }

    @CEntryPoint(name = "getMarginCalculationLoadIncreaseResults")
    public static ArrayPointer<SeriesPointer> getMarginCalculationLoadIncreaseResults(IsolateThread thread, ObjectHandle resultHandle,
                                                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                MarginCalculationResult result = ObjectHandles.getGlobal().get(resultHandle);
                return Dataframes.createCDataframe(MarginCalculationDataframeMappersUtils.loadIncreaseResultsDataFrameMapper(),
                        result.getLoadIncreaseResults());
            }
        });
    }

    @CEntryPoint(name = "getMarginCalculationScenarioResults")
    public static ArrayPointer<SeriesPointer> getMarginCalculationScenarioResults(IsolateThread thread, ObjectHandle resultHandle,
                                                                                  ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                MarginCalculationResult result = ObjectHandles.getGlobal().get(resultHandle);
                return Dataframes.createCDataframe(MarginCalculationDataframeMappersUtils.scenarioResultsDataFrameMapper(),
                        result.getLoadIncreaseResults());
            }
        });
    }
}
