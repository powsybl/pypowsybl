/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.tools.Version;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gridsuite.gridpy.GridPyApiHeader.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class GridPyApi {

    private GridPyApi() {
    }

    @CEntryPoint(name = "setDebugMode")
    public static void setDebugMode(IsolateThread thread, boolean debug) {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(debug ? Level.DEBUG : Level.ERROR);
    }

    @CEntryPoint(name = "getVersionTable")
    public static CCharPointer getVersionTable(IsolateThread thread) {
        return CTypeConversion.toCString(Version.getTableString()).get();
    }

    @CEntryPoint(name = "createEmptyNetwork")
    public static ObjectHandle createEmptyNetwork(IsolateThread thread, CCharPointer id) {
        String idStr = CTypeConversion.toJavaString(id);
        Network network = Network.create(idStr, "");
        return ObjectHandles.getGlobal().create(network);
    }

    @CEntryPoint(name = "createIeee14Network")
    public static ObjectHandle createIeee14Network(IsolateThread thread) {
        Network network = IeeeCdfNetworkFactory.create14();
        return ObjectHandles.getGlobal().create(network);
    }

    @CEntryPoint(name = "createEurostagTutorialExample1Network")
    public static ObjectHandle createEurostagTutorialExample1Network(IsolateThread thread) {
        Network network = EurostagTutorialExample1Factory.create();
        return ObjectHandles.getGlobal().create(network);
    }

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file) {
        String fileStr = CTypeConversion.toJavaString(file);
        Network network = Importers.loadNetwork(fileStr);
        return ObjectHandles.getGlobal().create(network);
    }

    @CEntryPoint(name = "dumpNetwork")
    public static void dumpNetwork(IsolateThread thread, ObjectHandle networkHandle, CCharPointer file, CCharPointer format) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        String fileStr = CTypeConversion.toJavaString(file);
        String formatStr = CTypeConversion.toJavaString(format);
        Exporters.export(formatStr, network, null, Paths.get(fileStr));
    }

    static <T extends PointerBase> ArrayPointer<T> allocArrayPointer(T ptr, int length) {
        ArrayPointer<T> arrayPtr = UnmanagedMemory.calloc(SizeOf.get(ArrayPointer.class));
        arrayPtr.setPtr(ptr);
        arrayPtr.setLength(length);
        return arrayPtr;
    }

    static <T extends PointerBase> void freeArrayPointer(ArrayPointer<T> arrayPointer) {
        UnmanagedMemory.free(arrayPointer.getPtr());
        UnmanagedMemory.free(arrayPointer);
    }

    static ArrayPointer<LoadFlowComponentResultPointer> createLoadFlowComponentResultArrayPointer(LoadFlowResult result) {
        List<LoadFlowResult.ComponentResult> componentResults = result.getComponentResults();
        LoadFlowComponentResultPointer componentResultPtr = UnmanagedMemory.calloc(componentResults.size() * SizeOf.get(LoadFlowComponentResultPointer.class));
        for (int index = 0; index < componentResults.size(); index++) {
            LoadFlowResult.ComponentResult componentResult = componentResults.get(index);
            LoadFlowComponentResultPointer ptr = componentResultPtr.addressOf(index);
            ptr.setComponentNum(componentResult.getComponentNum());
            ptr.setStatus(componentResult.getStatus().ordinal());
            ptr.setIterationCount(componentResult.getIterationCount());
            ptr.setSlackBusId(CTypeConversion.toCString(componentResult.getSlackBusId()).get());
            ptr.setSlackBusActivePowerMismatch(componentResult.getSlackBusActivePowerMismatch());
        }
        return allocArrayPointer(componentResultPtr, componentResults.size());
    }

    private static LoadFlowParameters createLoadFlowParameters(boolean dc, LoadFlowParametersPointer loadFlowParametersPtr) {
        return LoadFlowParameters.load()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.values()[loadFlowParametersPtr.getVoltageInitMode()])
                .setTransformerVoltageControlOn(loadFlowParametersPtr.isTransformerVoltageControlOn())
                .setNoGeneratorReactiveLimits(loadFlowParametersPtr.isNoGeneratorReactiveLimits())
                .setPhaseShifterRegulationOn(loadFlowParametersPtr.isPhaseShifterRegulationOn())
                .setTwtSplitShuntAdmittance(loadFlowParametersPtr.isTwtSplitShuntAdmittance())
                .setSimulShunt(loadFlowParametersPtr.isSimulShunt())
                .setReadSlackBus(loadFlowParametersPtr.isReadSlackBus())
                .setWriteSlackBus(loadFlowParametersPtr.isWriteSlackBus())
                .setDistributedSlack(loadFlowParametersPtr.isDistributedSlack())
                .setDc(dc)
                .setBalanceType(LoadFlowParameters.BalanceType.values()[loadFlowParametersPtr.getBalanceType()]);
    }

    @CEntryPoint(name = "runLoadFlow")
    public static ArrayPointer<LoadFlowComponentResultPointer> runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean dc, LoadFlowParametersPointer loadFlowParametersPtr) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowParameters parameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
        LoadFlowResult result = LoadFlow.run(network, parameters);
        return createLoadFlowComponentResultArrayPointer(result);
    }

    @CEntryPoint(name = "freeLoadFlowComponentResultPointer")
    public static void freeLoadFlowComponentResultPointer(IsolateThread thread, ArrayPointer<LoadFlowComponentResultPointer> componentResultArrayPtr) {
        // don't need to free char* from id field as it is done by python
        freeArrayPointer(componentResultArrayPtr);
    }

    @CEntryPoint(name = "getBusArray")
    public static ArrayPointer<BusPointer> getBusArray(IsolateThread thread, ObjectHandle networkHandle, boolean busBreakerView) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        Stream<Bus> busStream = busBreakerView ? network.getBusBreakerView().getBusStream() : network.getBusView().getBusStream();
        List<Bus> buses = busStream.collect(Collectors.toList());
        BusPointer busesPtr = UnmanagedMemory.calloc(buses.size() * SizeOf.get(BusPointer.class));
        for (int index = 0; index < buses.size(); index++) {
            Bus bus = buses.get(index);
            BusPointer busPtr = busesPtr.addressOf(index);
            busPtr.setId(CTypeConversion.toCString(bus.getId()).get());
            busPtr.setVoltageMagnitude(bus.getV());
            busPtr.setVoltageAngle(bus.getAngle());
        }
        return allocArrayPointer(busesPtr, buses.size());
    }

    @CEntryPoint(name = "freeBusArray")
    public static void freeBusArray(IsolateThread thread, ArrayPointer<BusPointer> busArrayPointer) {
        // don't need to free char* from id field as it is done by python
        freeArrayPointer(busArrayPointer);
    }

    @CEntryPoint(name = "updateSwitchPosition")
    public static boolean updateSwitchPosition(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean open) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        String idStr = CTypeConversion.toJavaString(id);
        return NetworkUtil.updateSwitchPosition(network, idStr, open);
    }

    @CEntryPoint(name = "updateConnectableStatus")
    public static boolean updateConnectableStatus(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean connected) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        String idStr = CTypeConversion.toJavaString(id);
        return NetworkUtil.updateConnectableStatus(network, idStr, connected);
    }

    @CEntryPoint(name = "getNetworkElementsIds")
    public static ArrayPointer<CCharPointerPointer> getNetworkElementsIds(IsolateThread thread, ObjectHandle networkHandle, ElementType elementType,
                                                                                          double nominalVoltage, boolean mainCc) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        List<String> elementsIds = NetworkUtil.getElementsIds(network, elementType, nominalVoltage, mainCc);
        CCharPointerPointer elementsIdsPtr = UnmanagedMemory.calloc(elementsIds.size() * SizeOf.get(CCharPointerPointer.class));
        for (int i = 0; i < elementsIds.size(); i++) {
            elementsIdsPtr.addressOf(i).write(CTypeConversion.toCString(elementsIds.get(i)).get());
        }
        return allocArrayPointer(elementsIdsPtr, elementsIds.size());
    }

    @CEntryPoint(name = "freeNetworkElementsIds")
    public static void freeNetworkElementsIds(IsolateThread thread, ArrayPointer<CCharPointerPointer> elementsIdsArrayPtr) {
        freeArrayPointer(elementsIdsArrayPtr);
    }

    @CEntryPoint(name = "writeSingleLineDiagramSvg")
    public static void writeSingleLineDiagramSvg(IsolateThread thread, ObjectHandle networkHandle, CCharPointer containerId,
                                                 CCharPointer svgFile) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        String containerIdStr = CTypeConversion.toJavaString(containerId);
        String svgFileStr = CTypeConversion.toJavaString(svgFile);
        SingleLineDiagramUtil.writeSvg(network, containerIdStr, svgFileStr);
    }

    @CEntryPoint(name = "createSecurityAnalysis")
    public static ObjectHandle createSecurityAnalysis(IsolateThread thread) {
        return ObjectHandles.getGlobal().create(new SecurityAnalysisContext());
    }

    @CEntryPoint(name = "addContingency")
    public static void addContingency(IsolateThread thread, ObjectHandle contingencyContainerHandle, CCharPointer contingencyIdPtr,
                                      CCharPointerPointer elementIdPtrPtr, int elementCount) {
        ContingencyContainer contingencyContainer = ObjectHandles.getGlobal().get(contingencyContainerHandle);
        String contingencyId = CTypeConversion.toJavaString(contingencyIdPtr);
        List<String> elementIds = CTypeUtil.createStringList(elementIdPtrPtr, elementCount);
        contingencyContainer.addContingency(contingencyId, elementIds);
    }

    private static LoadFlowResult.ComponentResult.Status getStatus(LimitViolationsResult result) {
        return result.isComputationOk() ? LoadFlowResult.ComponentResult.Status.CONVERGED : LoadFlowResult.ComponentResult.Status.FAILED;
    }

    private static void setSecurityAnalysisResultPointer(ContingencyResultPointer contingencyPtr, String contingencyId, LimitViolationsResult limitViolationsResult) {
        contingencyPtr.setContingencyId(CTypeConversion.toCString(contingencyId).get());
        contingencyPtr.setStatus(getStatus(limitViolationsResult).ordinal());
        List<LimitViolation> limitViolations = limitViolationsResult.getLimitViolations();
        LimitViolationPointer limitViolationPtr = UnmanagedMemory.calloc(limitViolations.size() * SizeOf.get(LimitViolationPointer.class));
        for (int i = 0; i < limitViolations.size(); i++) {
            LimitViolation limitViolation = limitViolations.get(i);
            LimitViolationPointer limitViolationPtrPlus = limitViolationPtr.addressOf(i);
            limitViolationPtrPlus.setSubjectId(CTypeConversion.toCString(limitViolation.getSubjectId()).get());
            limitViolationPtrPlus.setSubjectName(CTypeConversion.toCString(Objects.toString(limitViolation.getSubjectName(), "")).get());
            limitViolationPtrPlus.setLimitType(limitViolation.getLimitType().ordinal());
            limitViolationPtrPlus.setLimit(limitViolation.getLimit());
            limitViolationPtrPlus.setLimitName(CTypeConversion.toCString(Objects.toString(limitViolation.getLimitName(), "")).get());
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
        setSecurityAnalysisResultPointer(contingencyPtr, "", result.getPreContingencyResult());
        for (int i = 0; i < result.getPostContingencyResults().size(); i++) {
            PostContingencyResult postContingencyResult = result.getPostContingencyResults().get(i);
            ContingencyResultPointer contingencyPtrPlus = contingencyPtr.addressOf(i + 1);
            setSecurityAnalysisResultPointer(contingencyPtrPlus, postContingencyResult.getContingency().getId(), postContingencyResult.getLimitViolationsResult());
        }
        return allocArrayPointer(contingencyPtr, resultCount);
    }

    @CEntryPoint(name = "runSecurityAnalysis")
    public static ArrayPointer<ContingencyResultPointer> runSecurityAnalysis(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                                             ObjectHandle networkHandle, LoadFlowParametersPointer loadFlowParametersPtr) {
        SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowParameters loadFlowParameters = createLoadFlowParameters(false, loadFlowParametersPtr);
        SecurityAnalysisResult result = analysisContext.run(network, loadFlowParameters);
        return createContingencyResultArrayPointer(result);
    }

    @CEntryPoint(name = "freeContingencyResultArrayPointer")
    public static void freeContingencyResultArrayPointer(IsolateThread thread, ArrayPointer<ContingencyResultPointer> contingencyResultArrayPtr) {
        // don't need to free char* from id field as it is done by python
        for (int i = 0; i < contingencyResultArrayPtr.getLength(); i++) {
            ContingencyResultPointer contingencyResultPtrPlus = contingencyResultArrayPtr.getPtr().addressOf(i);
            UnmanagedMemory.free(contingencyResultPtrPlus.limitViolations().getPtr());
        }
        freeArrayPointer(contingencyResultArrayPtr);
    }

    @CEntryPoint(name = "createSensitivityAnalysis")
    public static ObjectHandle createSensitivityAnalysis(IsolateThread thread) {
        return ObjectHandles.getGlobal().create(new SensitivityAnalysisContext());
    }

    @CEntryPoint(name = "setFactorMatrix")
    public static void setFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                       CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                       CCharPointerPointer injectionOrTransfoIdPtrPtr, int injectionOrTransfoIdCount) {
        SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
        List<String> branchsIds = CTypeUtil.createStringList(branchIdPtrPtr, branchIdCount);
        List<String> injectionsOrTransfosIds = CTypeUtil.createStringList(injectionOrTransfoIdPtrPtr, injectionOrTransfoIdCount);
        analysisContext.setFactorMatrix(branchsIds, injectionsOrTransfosIds);
    }

    @CEntryPoint(name = "runSensitivityAnalysis")
    public static ObjectHandle runSensitivityAnalysis(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                      ObjectHandle networkHandle, LoadFlowParametersPointer loadFlowParametersPtr) {
        SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowParameters loadFlowParameters = createLoadFlowParameters(true, loadFlowParametersPtr);
        SensitivityAnalysisResultContext resultContext = analysisContext.run(network, loadFlowParameters);
        return ObjectHandles.getGlobal().create(resultContext);
    }

    @CEntryPoint(name = "getSensitivityMatrix")
    public static MatrixPointer getSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                     CCharPointer contingencyIdPtr) {
        SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
        String contingencyId = CTypeConversion.toJavaString(contingencyIdPtr);
        Collection<SensitivityValue> sensitivityValues = resultContext.getSensitivityValues(contingencyId);
        MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(MatrixPointer.class));
        if (sensitivityValues != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(resultContext.getRowCount() * resultContext.getColumnCount() * SizeOf.get(CDoublePointer.class));
            for (SensitivityValue sensitivityValue : sensitivityValues) {
                IndexedSensitivityFactor indexedFactor = (IndexedSensitivityFactor) sensitivityValue.getFactor();
                valuePtr.addressOf(indexedFactor.getRow() * resultContext.getColumnCount() + indexedFactor.getColumn()).write(sensitivityValue.getValue());
            }
            matrixPtr.setRowCount(resultContext.getRowCount());
            matrixPtr.setColumnCount(resultContext.getColumnCount());
            matrixPtr.setValues(valuePtr);
        } else {
            matrixPtr.setRowCount(0);
            matrixPtr.setColumnCount(0);
        }
        return matrixPtr;
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
