/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.shortcircuit.adders.FaultDataframeAdder;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframeMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer;
import com.powsybl.python.commons.Util.PointerProvider;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.network.NetworkCFunctions;
import com.powsybl.shortcircuit.ShortCircuitAnalysisProvider;
import com.powsybl.shortcircuit.ShortCircuitAnalysisResult;
import com.powsybl.shortcircuit.ShortCircuitParameters;
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
@SuppressWarnings({"java:S1602", "java:S1604"})
@CContext(Directives.class)
public final class ShortCircuitAnalysisCFunctions {

    private ShortCircuitAnalysisCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(ShortCircuitAnalysisCFunctions.class);
    }

    @CEntryPoint(name = "getShortCircuitAnalysisProviderNames")
    public static ArrayPointer<CCharPointerPointer> getShortCircuitAnalysisProviderNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<CCharPointerPointer>>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                return createCharPtrArray(ShortCircuitAnalysisProvider.findAll()
                        .stream().map(ShortCircuitAnalysisProvider::getName).collect(Collectors.toList()));
            }
        });
    }

    @CEntryPoint(name = "setDefaultShortCircuitAnalysisProvider")
    public static void setDefaultShortCircuitAnalysisProvider(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                PyPowsyblConfiguration.setDefaultShortCircuitAnalysisProvider(CTypeUtil.toString(provider));
            }
        });
    }

    @CEntryPoint(name = "getDefaultShortCircuitAnalysisProvider")
    public static CCharPointer getDefaultShortCircuitAnalysisProvider(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<CCharPointer>() {
            @Override
            public CCharPointer get() {
                return CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultShortCircuitAnalysisProvider());
            }
        });
    }

    @CEntryPoint(name = "createShortCircuitAnalysis")
    public static ObjectHandle createShortCircuitAnalysis(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                return ObjectHandles.getGlobal().create(new ShortCircuitAnalysisContext());
            }
        });
    }

    @CEntryPoint(name = "runShortCircuitAnalysis")
    public static ObjectHandle runShortCircuitAnalysis(IsolateThread thread, ObjectHandle shortCircuitAnalysisContextHandle,
                                                       ObjectHandle networkHandle, ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer,
                                                       CCharPointer providerName, ObjectHandle reportNodeHandle,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                ShortCircuitAnalysisContext analysisContext = ObjectHandles.getGlobal().get(shortCircuitAnalysisContextHandle);

                Network network = ObjectHandles.getGlobal().get(networkHandle);

                ShortCircuitAnalysisProvider provider = ShortCircuitAnalysisCUtils.getShortCircuitAnalysisProvider(CTypeUtil.toString(providerName));
                logger().info("Short-circuit analysis provider used for short-circuit analysis is : {}", provider.getName());
                ShortCircuitParameters shortCircuitAnalysisParameters = ShortCircuitAnalysisCUtils.createShortCircuitAnalysisParameters(shortCircuitAnalysisParametersPointer, provider);

                ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
                ShortCircuitAnalysisResult results = analysisContext.run(network, shortCircuitAnalysisParameters, provider.getName(), reportNode);
                return ObjectHandles.getGlobal().create(results);
            }
        });
    }

    @CEntryPoint(name = "freeShortCircuitAnalysisParameters")
    public static void freeShortCircuitAnalysisParameters(IsolateThread thread, ShortCircuitAnalysisParametersPointer parameters,
                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                UnmanagedMemory.free(parameters);
            }
        });
    }

    @CEntryPoint(name = "createShortCircuitAnalysisParameters")
    public static ShortCircuitAnalysisParametersPointer createShortCircuitAnalysisParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ShortCircuitAnalysisParametersPointer>() {
            @Override
            public ShortCircuitAnalysisParametersPointer get() {
                return convertToShortCircuitAnalysisParametersPointer(ShortCircuitAnalysisCUtils.createShortCircuitAnalysisParameters());
            }
        });
    }

    private static ShortCircuitAnalysisParametersPointer convertToShortCircuitAnalysisParametersPointer(ShortCircuitParameters parameters) {
        ShortCircuitAnalysisParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(ShortCircuitAnalysisParametersPointer.class));
        paramsPtr.setWithVoltageResult(parameters.isWithVoltageResult());
        paramsPtr.setWithFeederResult(parameters.isWithFeederResult());
        paramsPtr.setWithLimitViolations(parameters.isWithLimitViolations());
        paramsPtr.setWithFortescueResult(parameters.isWithFortescueResult());
        paramsPtr.setMinVoltageDropProportionalThreshold(parameters.getMinVoltageDropProportionalThreshold());
        paramsPtr.setStudyType(parameters.getStudyType().ordinal());
        paramsPtr.setInitialVoltageProfileMode(parameters.getInitialVoltageProfileMode().ordinal());
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
    public static ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<CCharPointerPointer>>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                String providerStr = CTypeUtil.toString(provider);
                return Util.createCharPtrArray(getSpecificParametersNames(ShortCircuitAnalysisCUtils.getShortCircuitAnalysisProvider(providerStr)));
            }
        });
    }

    @CEntryPoint(name = "getFaultsDataframeMetaData")
    public static DataframeMetadataPointer getFaultsDataframeMetaData(IsolateThread thread,
                                                                      PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<DataframeMetadataPointer>() {
            @Override
            public DataframeMetadataPointer get() {
                return CTypeUtil.createSeriesMetadata(new FaultDataframeAdder().getMetadata());
            }
        });
    }

    @CEntryPoint(name = "setFaults")
    public static void setFaults(IsolateThread thread, ObjectHandle contextHandle,
                                 PyPowsyblApiHeader.DataframePointer cDataframe,
                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                ShortCircuitAnalysisContext context = ObjectHandles.getGlobal().get(contextHandle);
                UpdatingDataframe faultDataframe = NetworkCFunctions.createDataframe(cDataframe);
                new FaultDataframeAdder().addElements(context, faultDataframe);
            }
        });
    }

    @CEntryPoint(name = "getShortCircuitAnalysisFaultResults")
    public static ArrayPointer<SeriesPointer> getShortCircuitAnalysisFaultResults(IsolateThread thread,
                                                                                  ObjectHandle shortCircuitAnalysisResult,
                                                                                  boolean withFortescueResult,
                                                                                  PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
                return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisFaultResultsMapper(withFortescueResult), result);
            }
        });
    }

    @CEntryPoint(name = "getShortCircuitAnalysisFeederResults")
    public static ArrayPointer<SeriesPointer> getShortCircuitAnalysisFeederResults(IsolateThread thread,
                                                                                   ObjectHandle shortCircuitAnalysisResult,
                                                                                   boolean withFortescueResult,
                                                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
                return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisMagnitudeFeederResultsMapper(withFortescueResult), result);
            }
        });
    }

    @CEntryPoint(name = "getShortCircuitAnalysisLimitViolationsResults")
    public static ArrayPointer<SeriesPointer> getShortCircuitAnalysisLimitViolationsResults(IsolateThread thread, ObjectHandle shortCircuitAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
                return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisLimitViolationsResultsMapper(), result);
            }
        });
    }

    @CEntryPoint(name = "getShortCircuitAnalysisBusResults")
    public static ArrayPointer<SeriesPointer> getShortCircuitAnalysisBusResults(IsolateThread thread,
                                                                                ObjectHandle shortCircuitAnalysisResult,
                                                                                boolean withFortescueResult,
                                                                                PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                ShortCircuitAnalysisResult result = ObjectHandles.getGlobal().get(shortCircuitAnalysisResult);
                return Dataframes.createCDataframe(Dataframes.shortCircuitAnalysisMagnitudeBusResultsMapper(withFortescueResult), result);
            }
        });
    }
}
