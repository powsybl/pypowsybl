/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import ch.qos.logback.classic.Logger;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ValidationLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.SecurityAnalysisProvider;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.results.PostContingencyResult;
import com.powsybl.sensitivity.SensitivityAnalysisProvider;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import com.powsybl.tools.Version;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.python.CTypeUtil.toStringList;
import static com.powsybl.python.PyPowsyblApiHeader.*;
import static com.powsybl.python.Util.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class PyPowsyblApiLib {

    public static CFunctionPointer loggerCallback;

    private PyPowsyblApiLib() {
    }

    @CEntryPoint(name = "setJavaLibraryPath")
    public static void setJavaLibraryPath(IsolateThread thread, CCharPointer javaLibraryPath, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            System.setProperty("java.library.path", CTypeUtil.toString(javaLibraryPath));
        });
    }

    @CEntryPoint(name = "setConfigRead")
    public static void setConfigRead(IsolateThread thread, boolean read, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setReadConfig(read);
        });
    }

    @CEntryPoint(name = "setDefaultLoadFlowProvider")
    public static void setDefaultLoadFlowProvider(IsolateThread thread, CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultLoadFlowProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultLoadFlowProvider")
    public static CCharPointer getDefaultLoadFlowProvider(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultLoadFlowProvider()));
    }

    @CEntryPoint(name = "setDefaultSecurityAnalysisProvider")
    public static void setDefaultSecurityAnalysisProvider(IsolateThread thread, CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultSecurityAnalysisProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultSecurityAnalysisProvider")
    public static CCharPointer getDefaultSecurityAnalysisProvider(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultSecurityAnalysisProvider()));
    }

    @CEntryPoint(name = "setDefaultSensitivityAnalysisProvider")
    public static void setDefaultSensitivityAnalysisProvider(IsolateThread thread, CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultSensitivityAnalysisProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultSensitivityAnalysisProvider")
    public static CCharPointer getDefaultSensitivityAnalysisProvider(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider()));
    }

    @CEntryPoint(name = "isConfigRead")
    public static boolean isConfigRead(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, PyPowsyblConfiguration::isReadConfig);
    }

    @CEntryPoint(name = "getVersionTable")
    public static CCharPointer getVersionTable(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(Version.getTableString()));
    }

    @CEntryPoint(name = "getNetworkImportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkImportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ArrayList<>(Importers.getFormats())));
    }

    @CEntryPoint(name = "getNetworkExportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkExportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ArrayList<>(Exporters.getFormats())));
    }

    @CEntryPoint(name = "getLoadFlowProviderNames")
    public static ArrayPointer<CCharPointerPointer> getLoadFlowProviderNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(LoadFlowProvider.class).getServices()
                .stream().map(LoadFlowProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "getSecurityAnalysisProviderNames")
    public static ArrayPointer<CCharPointerPointer> getSecurityAnalysisProviderNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(SecurityAnalysisProvider.class).getServices()
                .stream().map(SecurityAnalysisProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "getSensitivityAnalysisProviderNames")
    public static ArrayPointer<CCharPointerPointer> getSensitivityAnalysisProviderNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(SensitivityAnalysisProvider.class).getServices()
                .stream().map(SensitivityAnalysisProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "freeStringArray")
    public static void freeStringArray(IsolateThread thread, ArrayPointer<CCharPointerPointer> arrayPtr,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeArrayContent(arrayPtr));
    }

    @CEntryPoint(name = "createImporterParametersSeriesArray")
    static ArrayPointer<SeriesPointer> createImporterParametersSeriesArray(IsolateThread thread, CCharPointer formatPtr,
                                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String format = CTypeUtil.toString(formatPtr);
            Importer importer = Importers.getImporter(format);
            if (importer == null) {
                throw new PowsyblException("Format '" + format + "' not supported");
            }
            return Dataframes.createCDataframe(Dataframes.importerParametersMapper(), importer);
        });
    }

    @CEntryPoint(name = "createExporterParametersSeriesArray")
    static ArrayPointer<SeriesPointer> createExporterParametersSeriesArray(IsolateThread thread, CCharPointer formatPtr,
                                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String format = CTypeUtil.toString(formatPtr);
            var exporter = Exporters.getExporter(format);
            if (exporter == null) {
                throw new PowsyblException("Format '" + format + "' not supported");
            }
            return Dataframes.createCDataframe(Dataframes.exporterParametersMapper(), exporter);
        });
    }

    static ArrayPointer<LoadFlowComponentResultPointer> createLoadFlowComponentResultArrayPointer(LoadFlowResult result) {
        List<LoadFlowResult.ComponentResult> componentResults = result.getComponentResults();
        LoadFlowComponentResultPointer componentResultPtr = UnmanagedMemory.calloc(componentResults.size() * SizeOf.get(LoadFlowComponentResultPointer.class));
        for (int index = 0; index < componentResults.size(); index++) {
            LoadFlowResult.ComponentResult componentResult = componentResults.get(index);
            LoadFlowComponentResultPointer ptr = componentResultPtr.addressOf(index);
            ptr.setConnectedComponentNum(componentResult.getConnectedComponentNum());
            ptr.setSynchronousComponentNum(componentResult.getSynchronousComponentNum());
            ptr.setStatus(componentResult.getStatus().ordinal());
            ptr.setIterationCount(componentResult.getIterationCount());
            ptr.setSlackBusId(CTypeUtil.toCharPtr(componentResult.getSlackBusId()));
            ptr.setSlackBusActivePowerMismatch(componentResult.getSlackBusActivePowerMismatch());
        }
        return allocArrayPointer(componentResultPtr, componentResults.size());
    }

    private static LoadFlowParameters createLoadFlowParameters(boolean dc, LoadFlowParametersPointer loadFlowParametersPtr) {
        return createLoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.values()[loadFlowParametersPtr.getVoltageInitMode()])
                .setTransformerVoltageControlOn(loadFlowParametersPtr.isTransformerVoltageControlOn())
                .setNoGeneratorReactiveLimits(loadFlowParametersPtr.isNoGeneratorReactiveLimits())
                .setPhaseShifterRegulationOn(loadFlowParametersPtr.isPhaseShifterRegulationOn())
                .setTwtSplitShuntAdmittance(loadFlowParametersPtr.isTwtSplitShuntAdmittance())
                .setShuntCompensatorVoltageControlOn(loadFlowParametersPtr.isSimulShunt())
                .setReadSlackBus(loadFlowParametersPtr.isReadSlackBus())
                .setWriteSlackBus(loadFlowParametersPtr.isWriteSlackBus())
                .setDistributedSlack(loadFlowParametersPtr.isDistributedSlack())
                .setDc(dc)
                .setBalanceType(LoadFlowParameters.BalanceType.values()[loadFlowParametersPtr.getBalanceType()])
                .setDcUseTransformerRatio(loadFlowParametersPtr.isDcUseTransformerRatio())
                .setCountriesToBalance(CTypeUtil.toStringList(loadFlowParametersPtr.getCountriesToBalance(), loadFlowParametersPtr.getCountriesToBalanceCount())
                        .stream().map(Country::valueOf).collect(Collectors.toSet()))
                .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.values()[loadFlowParametersPtr.getConnectedComponentMode()]);
    }

    @CEntryPoint(name = "createLoadFlowParameters")
    public static LoadFlowParametersPointer createLoadFlowParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            return convertToLoadFlowParametersPointer(createLoadFlowParameters());
        });
    }

    private static LoadFlowParameters createLoadFlowParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? LoadFlowParameters.load() : new LoadFlowParameters();
    }

    @CEntryPoint(name = "freeLoadFlowParameters")
    public static void freeLoadFlowParameters(IsolateThread thread, LoadFlowParametersPointer loadFlowParametersPtr,
                                              ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < loadFlowParametersPtr.getCountriesToBalanceCount(); i++) {
                UnmanagedMemory.free(loadFlowParametersPtr.getCountriesToBalance().read(i));
            }
            UnmanagedMemory.free(loadFlowParametersPtr.getCountriesToBalance());
            UnmanagedMemory.free(loadFlowParametersPtr);
        });
    }

    private static LoadFlowParametersPointer convertToLoadFlowParametersPointer(LoadFlowParameters parameters) {
        LoadFlowParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(LoadFlowParametersPointer.class));
        paramsPtr.setVoltageInitMode(parameters.getVoltageInitMode().ordinal());
        paramsPtr.setTransformerVoltageControlOn(parameters.isTransformerVoltageControlOn());
        paramsPtr.setNoGeneratorReactiveLimits(parameters.isNoGeneratorReactiveLimits());
        paramsPtr.setPhaseShifterRegulationOn(parameters.isPhaseShifterRegulationOn());
        paramsPtr.setTwtSplitShuntAdmittance(parameters.isTwtSplitShuntAdmittance());
        paramsPtr.setSimulShunt(parameters.isSimulShunt());
        paramsPtr.setReadSlackBus(parameters.isReadSlackBus());
        paramsPtr.setWriteSlackBus(parameters.isWriteSlackBus());
        paramsPtr.setDistributedSlack(parameters.isDistributedSlack());
        paramsPtr.setBalanceType(parameters.getBalanceType().ordinal());
        paramsPtr.setReadSlackBus(parameters.isReadSlackBus());
        paramsPtr.setBalanceType(parameters.getBalanceType().ordinal());
        paramsPtr.setDcUseTransformerRatio(parameters.isDcUseTransformerRatio());
        CCharPointerPointer calloc = UnmanagedMemory.calloc(parameters.getCountriesToBalance().size() * SizeOf.get(CCharPointerPointer.class));
        ArrayList<Country> countries = new ArrayList<>(parameters.getCountriesToBalance());
        for (int i = 0; i < parameters.getCountriesToBalance().size(); i++) {
            calloc.write(i, CTypeUtil.toCharPtr(countries.get(i).toString()));
        }
        paramsPtr.setCountriesToBalance(calloc);
        paramsPtr.setCountriesToBalanceCount(countries.size());
        paramsPtr.setConnectedComponentMode(parameters.getConnectedComponentMode().ordinal());
        return paramsPtr;
    }

    @CEntryPoint(name = "runLoadFlow")
    public static ArrayPointer<LoadFlowComponentResultPointer> runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean dc,
                                                                           LoadFlowParametersPointer loadFlowParametersPtr,
                                                                           CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters parameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
            String providerStr = CTypeUtil.toString(provider);
            if (providerStr.equals("")) {
                providerStr = PyPowsyblConfiguration.getDefaultLoadFlowProvider();
            }
            Logger rootLogger = (Logger) LoggerFactory.getLogger(PyPowsyblApiLib.class);
            rootLogger.info("loadflow provider used is : {}", providerStr);
            LoadFlow.Runner runner = LoadFlow.find(providerStr);
            LoadFlowResult result = runner.run(network, parameters);
            return createLoadFlowComponentResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "freeLoadFlowComponentResultPointer")
    public static void freeLoadFlowComponentResultPointer(IsolateThread thread, ArrayPointer<LoadFlowComponentResultPointer> componentResultArrayPtr,
                                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < componentResultArrayPtr.getLength(); i++) {
                UnmanagedMemory.free(componentResultArrayPtr.getPtr().addressOf(i).getSlackBusId());
            }
            freeArrayPointer(componentResultArrayPtr);
        });
    }

    @CEntryPoint(name = "updateSwitchPosition")
    public static boolean updateSwitchPosition(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean open,
                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String idStr = CTypeUtil.toString(id);
            return NetworkUtil.updateSwitchPosition(network, idStr, open);
        });
    }

    @CEntryPoint(name = "updateConnectableStatus")
    public static boolean updateConnectableStatus(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean connected,
                                                  ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String idStr = CTypeUtil.toString(id);
            return NetworkUtil.updateConnectableStatus(network, idStr, connected);
        });
    }

    @CEntryPoint(name = "writeSingleLineDiagramSvg")
    public static void writeSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                 CCharPointer svgFile, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            String svgFileStr = CTypeUtil.toString(svgFile);
            SingleLineDiagramUtil.writeSvg(network, containerIdStr, svgFileStr);
        });
    }

    @CEntryPoint(name = "getSingleLineDiagramSvg")
    public static CCharPointer getSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String containerIdStr = CTypeUtil.toString(containerId);
            String svg = SingleLineDiagramUtil.getSvg(network, containerIdStr);
            return CTypeUtil.toCharPtr(svg);
        });
    }

    @CEntryPoint(name = "writeNetworkAreaDiagramSvg")
    public static void writeNetworkAreaDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer svgFile,
                                                  CCharPointerPointer voltageLevelIdsPointer, int voltageLevelIdCount, int depth, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String svgFileStr = CTypeUtil.toString(svgFile);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            NetworkAreaDiagramUtil.writeSvg(network, voltageLevelIds, depth, svgFileStr);
        });
    }

    @CEntryPoint(name = "getNetworkAreaDiagramSvg")
    public static CCharPointer getNetworkAreaDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointerPointer voltageLevelIdsPointer,
                                                        int voltageLevelIdCount, int depth, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> voltageLevelIds = toStringList(voltageLevelIdsPointer, voltageLevelIdCount);
            String svg = NetworkAreaDiagramUtil.getSvg(network, voltageLevelIds, depth);
            return CTypeUtil.toCharPtr(svg);
        });
    }

    @CEntryPoint(name = "createSecurityAnalysis")
    public static ObjectHandle createSecurityAnalysis(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new SecurityAnalysisContext()));
    }

    @CEntryPoint(name = "addContingency")
    public static void addContingency(IsolateThread thread, ObjectHandle contingencyContainerHandle, CCharPointer contingencyIdPtr,
                                      CCharPointerPointer elementIdPtrPtr, int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            ContingencyContainer contingencyContainer = ObjectHandles.getGlobal().get(contingencyContainerHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            List<String> elementIds = toStringList(elementIdPtrPtr, elementCount);
            contingencyContainer.addContingency(contingencyId, elementIds);
        });
    }

    private static LoadFlowResult.ComponentResult.Status getStatus(LimitViolationsResult result) {
        return result.isComputationOk() ? LoadFlowResult.ComponentResult.Status.CONVERGED : LoadFlowResult.ComponentResult.Status.FAILED;
    }

    private static void setSecurityAnalysisResultPointer(ContingencyResultPointer contingencyPtr, String contingencyId, LimitViolationsResult limitViolationsResult) {
        contingencyPtr.setContingencyId(CTypeUtil.toCharPtr(contingencyId));
        contingencyPtr.setStatus(getStatus(limitViolationsResult).ordinal());
        List<LimitViolation> limitViolations = limitViolationsResult.getLimitViolations();
        LimitViolationPointer limitViolationPtr = UnmanagedMemory.calloc(limitViolations.size() * SizeOf.get(LimitViolationPointer.class));
        for (int i = 0; i < limitViolations.size(); i++) {
            LimitViolation limitViolation = limitViolations.get(i);
            LimitViolationPointer limitViolationPtrPlus = limitViolationPtr.addressOf(i);
            limitViolationPtrPlus.setSubjectId(CTypeUtil.toCharPtr(limitViolation.getSubjectId()));
            limitViolationPtrPlus.setSubjectName(CTypeUtil.toCharPtr(Objects.toString(limitViolation.getSubjectName(), "")));
            limitViolationPtrPlus.setLimitType(limitViolation.getLimitType().ordinal());
            limitViolationPtrPlus.setLimit(limitViolation.getLimit());
            limitViolationPtrPlus.setLimitName(CTypeUtil.toCharPtr(Objects.toString(limitViolation.getLimitName(), "")));
            limitViolationPtrPlus.setAcceptableDuration(limitViolation.getAcceptableDuration());
            limitViolationPtrPlus.setLimitReduction(limitViolation.getLimitReduction());
            limitViolationPtrPlus.setValue(limitViolation.getValue());
            limitViolationPtrPlus.setSide(limitViolation.getSide() != null ? limitViolation.getSide().ordinal() : -1);
        }
        contingencyPtr.limitViolations().setLength(limitViolations.size());
        contingencyPtr.limitViolations().setPtr(limitViolationPtr);
    }

    private static ArrayPointer<ContingencyResultPointer> createContingencyResultArrayPointer(SecurityAnalysisResult result) {
        int resultCount = result.getPostContingencyResults().size() + 1; // + 1 for pre-contingency result
        ContingencyResultPointer contingencyPtr = UnmanagedMemory.calloc(resultCount * SizeOf.get(ContingencyResultPointer.class));
        setSecurityAnalysisResultPointer(contingencyPtr, "", result.getPreContingencyResult().getLimitViolationsResult());
        for (int i = 0; i < result.getPostContingencyResults().size(); i++) {
            PostContingencyResult postContingencyResult = result.getPostContingencyResults().get(i);
            ContingencyResultPointer contingencyPtrPlus = contingencyPtr.addressOf(i + 1);
            setSecurityAnalysisResultPointer(contingencyPtrPlus, postContingencyResult.getContingency().getId(), postContingencyResult.getLimitViolationsResult());
        }
        return allocArrayPointer(contingencyPtr, resultCount);
    }

    @CEntryPoint(name = "runSecurityAnalysis")
    public static ObjectHandle runSecurityAnalysis(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                   ObjectHandle networkHandle, LoadFlowParametersPointer loadFlowParametersPtr,
                                                   CCharPointer provider, boolean dc, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters loadFlowParameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
            String providerStr = CTypeUtil.toString(provider);
            if (providerStr.equals("")) {
                providerStr = PyPowsyblConfiguration.getDefaultSecurityAnalysisProvider();
            }
            Logger logger = (Logger) LoggerFactory.getLogger(PyPowsyblApiLib.class);
            logger.info("loadflow provider used for security analysis is : {}", providerStr);
            SecurityAnalysisResult result = analysisContext.run(network, loadFlowParameters, providerStr);
            return ObjectHandles.getGlobal().create(result);
        });
    }

    @CEntryPoint(name = "getSecurityAnalysisResult")
    public static ArrayPointer<ContingencyResultPointer> getSecurityAnalysisResult(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return createContingencyResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "getLimitViolations")
    public static ArrayPointer<SeriesPointer> getLimitViolations(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return Dataframes.createCDataframe(Dataframes.limitViolationsMapper(), result);
        });
    }

    @CEntryPoint(name = "freeContingencyResultArrayPointer")
    public static void freeContingencyResultArrayPointer(IsolateThread thread, ArrayPointer<ContingencyResultPointer> contingencyResultArrayPtr,
                                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < contingencyResultArrayPtr.getLength(); i++) {
                ContingencyResultPointer contingencyResultPtrPlus = contingencyResultArrayPtr.getPtr().addressOf(i);
                UnmanagedMemory.free(contingencyResultPtrPlus.getContingencyId());
                for (int l = 0; l < contingencyResultPtrPlus.limitViolations().getLength(); l++) {
                    LimitViolationPointer violation = contingencyResultPtrPlus.limitViolations().getPtr().addressOf(l);
                    UnmanagedMemory.free(violation.getSubjectId());
                    UnmanagedMemory.free(violation.getSubjectName());
                    UnmanagedMemory.free(violation.getLimitName());
                }
                UnmanagedMemory.free(contingencyResultPtrPlus.limitViolations().getPtr());
            }
            freeArrayPointer(contingencyResultArrayPtr);
        });
    }

    @CEntryPoint(name = "createSensitivityAnalysis")
    public static ObjectHandle createSensitivityAnalysis(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new SensitivityAnalysisContext()));
    }

    @CEntryPoint(name = "setZones")
    public static void setZones(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                ZonePointerPointer zonePtrPtr, int zoneCount,
                                ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<SensitivityVariableSet> variableSets = new ArrayList<>(zoneCount);
            for (int zoneIndex = 0; zoneIndex < zoneCount; zoneIndex++) {
                PyPowsyblApiHeader.ZonePointer zonePtrI = zonePtrPtr.read(zoneIndex);
                String zoneId = CTypeUtil.toString(zonePtrI.getId());
                List<String> injectionsIds = toStringList(zonePtrI.getInjectionsIds(), zonePtrI.getLength());
                List<Double> injectionsShiftKeys = CTypeUtil.toDoubleList(zonePtrI.getinjectionsShiftKeys(), zonePtrI.getLength());
                List<WeightedSensitivityVariable> variables = new ArrayList<>(injectionsIds.size());
                for (int injectionIndex = 0; injectionIndex < injectionsIds.size(); injectionIndex++) {
                    variables.add(new WeightedSensitivityVariable(injectionsIds.get(injectionIndex), injectionsShiftKeys.get(injectionIndex)));
                }
                variableSets.add(new SensitivityVariableSet(zoneId, variables));
            }
            analysisContext.setVariableSets(variableSets);
        });
    }

    @CEntryPoint(name = "addBranchFlowFactorMatrix")
    public static void addBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                 CCharPointer matrixIdPtr,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = toStringList(variableIdPtrPtr, variableIdCount);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            analysisContext.addBranchFlowFactorMatrix(matrixId, branchesIds, variablesIds);
        });
    }

    @CEntryPoint(name = "addPreContingencyBranchFlowFactorMatrix")
    public static void addPreContingencyBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                               CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                               CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                               CCharPointer matrixIdPtr,
                                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = toStringList(variableIdPtrPtr, variableIdCount);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            analysisContext.addPreContingencyBranchFlowFactorMatrix(matrixId, branchesIds, variablesIds);
        });
    }

    @CEntryPoint(name = "addPostContingencyBranchFlowFactorMatrix")
    public static void addPostContingencyBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                                CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                                CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                                CCharPointerPointer contingenciesIdPtrPtr, int contingenciesIdCount,
                                                                CCharPointer matrixIdPtr,
                                                                ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = toStringList(variableIdPtrPtr, variableIdCount);
            List<String> contingencies = toStringList(contingenciesIdPtrPtr, contingenciesIdCount);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            analysisContext.addPostContingencyBranchFlowFactorMatrix(matrixId, branchesIds, variablesIds, contingencies);
        });
    }

    @CEntryPoint(name = "setBusVoltageFactorMatrix")
    public static void setBusVoltageFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer busVoltageIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer targetVoltageIdPtrPtr, int injectionOrTransfoIdCount,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> busVoltageIds = toStringList(busVoltageIdPtrPtr, branchIdCount);
            List<String> targetVoltageIds = toStringList(targetVoltageIdPtrPtr, injectionOrTransfoIdCount);
            analysisContext.setBusVoltageFactorMatrix(busVoltageIds, targetVoltageIds);
        });
    }

    @CEntryPoint(name = "runSensitivityAnalysis")
    public static ObjectHandle runSensitivityAnalysis(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                      ObjectHandle networkHandle, boolean dc, LoadFlowParametersPointer loadFlowParametersPtr,
                                                      CCharPointer provider,
                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters loadFlowParameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
            String providerStr = CTypeUtil.toString(provider);
            if (providerStr.equals("")) {
                providerStr = PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider();
            }
            Logger logger = (Logger) LoggerFactory.getLogger(PyPowsyblApiLib.class);
            logger.info("loadflow provider used for sensitivity analysis is : {}", providerStr);
            SensitivityAnalysisResultContext resultContext = analysisContext.run(network, loadFlowParameters, providerStr);
            return ObjectHandles.getGlobal().create(resultContext);
        });
    }

    @CEntryPoint(name = "getBranchFlowsSensitivityMatrix")
    public static MatrixPointer getBranchFlowsSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                CCharPointer matrixIdPtr, CCharPointer contingencyIdPtr,
                                                                ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            return resultContext.createBranchFlowsSensitivityMatrix(matrixId, contingencyId);
        });
    }

    @CEntryPoint(name = "getBusVoltagesSensitivityMatrix")
    public static MatrixPointer getBusVoltagesSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createBusVoltagesSensitivityMatrix(contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceFlows")
    public static MatrixPointer getReferenceFlows(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                    CCharPointer matrixIdPtr, CCharPointer contingencyIdPtr,
                                                    ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            return resultContext.createReferenceFlowsActivePower(matrixId, contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceVoltages")
    public static MatrixPointer getReferenceVoltages(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                     CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createReferenceVoltages(contingencyId);
        });
    }

    @CEntryPoint(name = "freeArray")
    public static <T extends PointerBase> void freeArray(IsolateThread thread, ArrayPointer<T> arrayPointer,
                                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        UnmanagedMemory.free(arrayPointer.getPtr());
        UnmanagedMemory.free(arrayPointer);
    }

    @CEntryPoint(name = "freeSeriesArray")
    public static void freeSeriesArray(IsolateThread thread, ArrayPointer<SeriesPointer> seriesPtrArrayPtr,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < seriesPtrArrayPtr.getLength(); i++) {
                freeSeries(seriesPtrArrayPtr.getPtr().addressOf(i));
            }
            freeArrayPointer(seriesPtrArrayPtr);
        });
    }

    private static void freeSeries(SeriesPointer seriesPointer) {
        if (seriesPointer.getType() == CDataframeHandler.STRING_SERIES_TYPE) {
            freeArrayContent(seriesPointer.data());
        }
        UnmanagedMemory.free(seriesPointer.data().getPtr());
        UnmanagedMemory.free(seriesPointer.getName());
    }

    /**
     * Frees C strings memory
     *
     * @param array
     */
    private static void freeArrayContent(ArrayPointer<CCharPointerPointer> array) {
        for (int i = 0; i < array.getLength(); i++) {
            UnmanagedMemory.free(array.getPtr().read(i));
        }
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().destroy(objectHandle));
    }

    @CEntryPoint(name = "getWorkingVariantId")
    public static CCharPointer getWorkingVariantId(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return CTypeUtil.toCharPtr(network.getVariantManager().getWorkingVariantId());
        });
    }

    @CEntryPoint(name = "addMonitoredElements")
    public static void addMonitoredElements(IsolateThread thread, ObjectHandle securityAnalysisContextHandle, RawContingencyContextType contingencyContextType,
                                            CCharPointerPointer branchIds, int branchIdsCount,
                                            CCharPointerPointer voltageLevelIds, int voltageLevelIdCount,
                                            CCharPointerPointer threeWindingsTransformerIds, int threeWindingsTransformerIdsCount,
                                            CCharPointerPointer contingencyIds, int contingencyIdsCount,
                                            ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            List<String> contingencies = toStringList(contingencyIds, contingencyIdsCount);
            contingencies.forEach(contingency -> {
                if (contingency.equals("")) {
                    contingency = null;
                }
                analysisContext.addMonitor(new StateMonitor(new ContingencyContext(contingency, convert(contingencyContextType)),
                        Set.copyOf(toStringList(branchIds, branchIdsCount)), Set.copyOf(toStringList(voltageLevelIds, voltageLevelIdCount)),
                        Set.copyOf(toStringList(threeWindingsTransformerIds, threeWindingsTransformerIdsCount))));
            });
        });
    }

    @CEntryPoint(name = "getBranchResults")
    public static ArrayPointer<SeriesPointer> getBranchResults(IsolateThread thread, ObjectHandle securityAnalysisResult, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.branchResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "getBusResults")
    public static ArrayPointer<SeriesPointer> getBusResults(IsolateThread thread, ObjectHandle securityAnalysisResult, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.busResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "getThreeWindingsTransformerResults")
    public static ArrayPointer<SeriesPointer> getThreeWindingsTransformerResults(IsolateThread thread, ObjectHandle securityAnalysisResult, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.threeWindingsTransformerResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "freeString")
    public static void freeString(IsolateThread thread, CCharPointer string, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(string));
    }

    @CEntryPoint(name = "getValidationLevel")
    public static ValidationLevelType getValidationLevel(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return Util.convert(network.getValidationLevel());
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return Util.convert(ValidationLevel.MINIMUM_VALUE);
        }
    }

    @CEntryPoint(name = "validate")
    public static ValidationLevelType validate(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return Util.convert(network.runValidationChecks());
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return Util.convert(ValidationLevel.MINIMUM_VALUE);
        }
    }

    @CEntryPoint(name = "setMinValidationLevel")
    public static void setMinValidationLevel(IsolateThread thread, ObjectHandle networkHandle,
                                     ValidationLevelType levelType,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.setMinimumAcceptableValidationLevel(Util.convert(levelType));
        });
    }

    interface Callback extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(int level, long timestamp, CCharPointer loggerName, CCharPointer message);
    }

    @CEntryPoint(name = "setupCallback")
    public static void setupCallback(IsolateThread thread, Callback fpointer, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            loggerCallback = fpointer;
        });
    }

    @CEntryPoint(name = "setLogLevel")
    public static void setLogLevel(IsolateThread thread, int logLevel, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(PyLoggingUtil.pythonLevelToLogbackLevel(logLevel));
        });
    }
}
