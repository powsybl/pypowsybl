/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.powsybl.commons.PowsyblException;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.util.ConnectedComponents;
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
import org.graalvm.nativeimage.c.type.*;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

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

    private static void fillBus(Bus bus, BusPointer busPtr) {
        busPtr.setId(CTypeConversion.toCString(bus.getId()).get());
        busPtr.setVoltageMagnitude(bus.getV());
        busPtr.setVoltageAngle(bus.getAngle());
        busPtr.setComponentNum(ConnectedComponents.getCcNum(bus));
    }

    @CEntryPoint(name = "getBusArray")
    public static ArrayPointer<BusPointer> getBusArray(IsolateThread thread, ObjectHandle networkHandle) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        List<Bus> buses = network.getBusView().getBusStream().collect(Collectors.toList());
        BusPointer busesPtr = UnmanagedMemory.calloc(buses.size() * SizeOf.get(BusPointer.class));
        for (int index = 0; index < buses.size(); index++) {
            Bus bus = buses.get(index);
            BusPointer busPtr = busesPtr.addressOf(index);
            fillBus(bus, busPtr);
        }
        return allocArrayPointer(busesPtr, buses.size());
    }

    @CEntryPoint(name = "freeBusArray")
    public static void freeBusArray(IsolateThread thread, ArrayPointer<BusPointer> busArrayPointer) {
        // don't need to free char* from id field as it is done by python
        freeArrayPointer(busArrayPointer);
    }

    @CEntryPoint(name = "getGeneratorArray")
    public static ArrayPointer<GeneratorPointer> getGeneratorArray(IsolateThread thread, ObjectHandle networkHandle) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        List<Generator> generators = network.getGeneratorStream().collect(Collectors.toList());
        GeneratorPointer generatorPtr = UnmanagedMemory.calloc(generators.size() * SizeOf.get(GeneratorPointer.class));
        for (int index = 0; index < generators.size(); index++) {
            Generator generator = generators.get(index);
            GeneratorPointer generatorPtrI = generatorPtr.addressOf(index);
            generatorPtrI.setId(CTypeConversion.toCString(generator.getId()).get());
            generatorPtrI.setTargetP(generator.getTargetP());
            generatorPtrI.setMaxP(generator.getMaxP());
            generatorPtrI.setMinP(generator.getMinP());
            VoltageLevel vl = generator.getTerminal().getVoltageLevel();
            generatorPtrI.setNominalVoltage(vl.getNominalV());
            generatorPtrI.setCountry(CTypeConversion.toCString(vl.getSubstation().getCountry().map(Country::name).orElse(null)).get());
            Bus bus = generator.getTerminal().getBusView().getBus();
            if (bus != null) {
                BusPointer busPtr = UnmanagedMemory.calloc(SizeOf.get(BusPointer.class));
                fillBus(bus, busPtr);
                generatorPtrI.setBus(busPtr);
            }
        }
        return allocArrayPointer(generatorPtr, generators.size());
    }

    @CEntryPoint(name = "freeGeneratorArray")
    public static void freeGeneratorArray(IsolateThread thread, ArrayPointer<GeneratorPointer> generatorArrayPtr) {
        for (int index = 0; index < generatorArrayPtr.getLength(); index++) {
            GeneratorPointer generatorPtrI = generatorArrayPtr.getPtr().addressOf(index);
            // don't need to free char* from id field as it is done by python
            if (generatorPtrI.getBus().isNonNull()) {
                UnmanagedMemory.free(generatorPtrI.getBus());
            }
        }
        freeArrayPointer(generatorArrayPtr);
    }

    @CEntryPoint(name = "getLoadArray")
    public static ArrayPointer<LoadPointer> getLoadArray(IsolateThread thread, ObjectHandle networkHandle) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        List<Load> loads = network.getLoadStream().collect(Collectors.toList());
        LoadPointer loadPtr = UnmanagedMemory.calloc(loads.size() * SizeOf.get(LoadPointer.class));
        for (int index = 0; index < loads.size(); index++) {
            Load load = loads.get(index);
            LoadPointer loadPtrI = loadPtr.addressOf(index);
            loadPtrI.setId(CTypeConversion.toCString(load.getId()).get());
            loadPtrI.setP0(load.getP0());
            VoltageLevel vl = load.getTerminal().getVoltageLevel();
            loadPtrI.setNominalVoltage(vl.getNominalV());
            loadPtrI.setCountry(CTypeConversion.toCString(vl.getSubstation().getCountry().map(Country::name).orElse(null)).get());
            Bus bus = load.getTerminal().getBusView().getBus();
            if (bus != null) {
                BusPointer busPtr = UnmanagedMemory.calloc(SizeOf.get(BusPointer.class));
                fillBus(bus, busPtr);
                loadPtrI.setBus(busPtr);
            }
        }
        return allocArrayPointer(loadPtr, loads.size());
    }

    @CEntryPoint(name = "freeLoadArray")
    public static void freeLoadArray(IsolateThread thread, ArrayPointer<LoadPointer> loadArrayPtr) {
        for (int index = 0; index < loadArrayPtr.getLength(); index++) {
            LoadPointer loadPtrI = loadArrayPtr.getPtr().addressOf(index);
            // don't need to free char* from id field as it is done by python
            if (loadPtrI.getBus().isNonNull()) {
                UnmanagedMemory.free(loadPtrI.getBus());
            }
        }
        freeArrayPointer(loadArrayPtr);
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
                                                                          CDoublePointer nominalVoltagePtr, int nominalVoltageCount,
                                                                          CCharPointerPointer countryPtr, int countryCount, boolean mainCc) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        Set<Double> nominalVoltages = new HashSet<>(CTypeUtil.createDoubleList(nominalVoltagePtr, nominalVoltageCount));
        Set<String> countries = new HashSet<>(CTypeUtil.createStringList(countryPtr, countryCount));
        List<String> elementsIds = NetworkUtil.getElementsIds(network, elementType, nominalVoltages, countries, mainCc);
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
        if (sensitivityValues != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(resultContext.getRowCount() * resultContext.getColumnCount() * SizeOf.get(CDoublePointer.class));
            for (SensitivityValue sensitivityValue : sensitivityValues) {
                IndexedSensitivityFactor indexedFactor = (IndexedSensitivityFactor) sensitivityValue.getFactor();
                valuePtr.addressOf(indexedFactor.getRow() * resultContext.getColumnCount() + indexedFactor.getColumn()).write(sensitivityValue.getValue());
            }
            MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(MatrixPointer.class));
            matrixPtr.setRowCount(resultContext.getRowCount());
            matrixPtr.setColumnCount(resultContext.getColumnCount());
            matrixPtr.setValues(valuePtr);
            return matrixPtr;
        }
        return WordFactory.nullPointer();
    }

    @CEntryPoint(name = "getReferenceFlows")
    public static MatrixPointer getReferenceFlows(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                  CCharPointer contingencyIdPtr) {
        SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
        String contingencyId = CTypeConversion.toJavaString(contingencyIdPtr);
        Collection<SensitivityValue> sensitivityValues = resultContext.getSensitivityValues(contingencyId);
        if (sensitivityValues != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(resultContext.getColumnCount() * SizeOf.get(CDoublePointer.class));
            for (SensitivityValue sensitivityValue : sensitivityValues) {
                IndexedSensitivityFactor indexedFactor = (IndexedSensitivityFactor) sensitivityValue.getFactor();
                valuePtr.addressOf(indexedFactor.getColumn()).write(sensitivityValue.getFunctionReference());
            }
            MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(MatrixPointer.class));
            matrixPtr.setRowCount(1);
            matrixPtr.setColumnCount(resultContext.getColumnCount());
            matrixPtr.setValues(valuePtr);
            return matrixPtr;
        }
        return WordFactory.nullPointer();
    }

    static class ColumnPointerArrayBuilder<T> {

        private static final int STRING_COLUMN_TYPE = 0;
        private static final int DOUBLE_COLUMN_TYPE = 1;
        private static final int INT_COLUMN_TYPE = 2;
        private static final int BOOLEAN_COLUMN_TYPE = 3;

        private final List<T> elements;

        private final GridPyApiHeader.ColumnPointer columnPtr;

        private final int columnCount;

        private int columnIndex = 0;

        ColumnPointerArrayBuilder(List<T> elements, int columnCount) {
            this.elements = elements;
            this.columnCount = columnCount;
            columnPtr = UnmanagedMemory.calloc(columnCount * SizeOf.get(ColumnPointer.class));
        }

        private void checkColumnIndex() {
            if (columnIndex >= columnCount) {
                throw new PowsyblException("Not enough columns:" + columnCount);
            }
        }

        ColumnPointerArrayBuilder<T> createStringColumn(String columnName, Function<T, String> getString) {
            checkColumnIndex();
            GridPyApiHeader.ColumnPointer columnPtrI = columnPtr.addressOf(columnIndex);
            columnPtrI.setName(CTypeConversion.toCString(columnName).get());
            columnPtrI.setType(STRING_COLUMN_TYPE);
            columnPtrI.data().setLength(elements.size());
            CCharPointerPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CCharPointerPointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(CTypeConversion.toCString(getString.apply(element)).get());
            }
            columnPtrI.data().setPtr(dataPtr);
            columnIndex++;
            return this;
        }

        ColumnPointerArrayBuilder<T> createEnumColumn(String columnName, Function<T, Enum> getEnum) {
            return createStringColumn(columnName, element -> getEnum.apply(element).name());
        }

        ColumnPointerArrayBuilder<T> createDoubleColumn(String columnName, ToDoubleFunction<T> getDouble) {
            checkColumnIndex();
            GridPyApiHeader.ColumnPointer columnPtrI = columnPtr.addressOf(columnIndex);
            columnPtrI.setName(CTypeConversion.toCString(columnName).get());
            columnPtrI.setType(DOUBLE_COLUMN_TYPE);
            columnPtrI.data().setLength(elements.size());
            CDoublePointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CDoublePointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(getDouble.applyAsDouble(element));
            }
            columnPtrI.data().setPtr(dataPtr);
            columnIndex++;
            return this;
        }

        ColumnPointerArrayBuilder<T> createIntColumn(String columnName, ToIntFunction<T> getInt) {
            return createIntColumn(columnName, getInt, INT_COLUMN_TYPE);
        }

        private ColumnPointerArrayBuilder<T> createIntColumn(String columnName, ToIntFunction<T> getInt, int type) {
            checkColumnIndex();
            GridPyApiHeader.ColumnPointer columnPtrI = columnPtr.addressOf(columnIndex);
            columnPtrI.setName(CTypeConversion.toCString(columnName).get());
            columnPtrI.setType(type);
            columnPtrI.data().setLength(elements.size());
            CIntPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CIntPointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(getInt.applyAsInt(element));
            }
            columnPtrI.data().setPtr(dataPtr);
            columnIndex++;
            return this;
        }

        ColumnPointerArrayBuilder<T> createBooleanColumn(String columnName, Predicate<T> getBool) {
            return createIntColumn(columnName, element -> getBool.test(element) ? 1 : 0, BOOLEAN_COLUMN_TYPE);
        }

        ArrayPointer<ColumnPointer> build() {
            return allocArrayPointer(columnPtr, columnCount);
        }
    }

    @CEntryPoint(name = "createNetworkElementsDataFrame")
    public static ArrayPointer<ColumnPointer> createNetworkElementsDataFrame(IsolateThread thread, ObjectHandle networkHandle, ElementType elementType) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        switch (elementType) {
            case GENERATOR:
                List<Generator> generators = network.getGeneratorStream().collect(Collectors.toList());
                return new ColumnPointerArrayBuilder<>(generators, 8)
                        .createStringColumn("id", Generator::getId)
                        .createEnumColumn("energy_source", Generator::getEnergySource)
                        .createDoubleColumn("target_p", Generator::getTargetP)
                        .createDoubleColumn("max_p", Generator::getMaxP)
                        .createDoubleColumn("min_p", Generator::getMinP)
                        .createDoubleColumn("target_v", Generator::getTargetV)
                        .createDoubleColumn("target_q", Generator::getTargetQ)
                        .createBooleanColumn("voltage_regulator_on", Generator::isVoltageRegulatorOn)
                        .build();

            default:
                throw new UnsupportedOperationException("Element type not supported: " + elementType);
        }
    }

    @CEntryPoint(name = "freeNetworkElementsDataFrame")
    public static void freeNetworkElementsDataFrame(IsolateThread thread, ArrayPointer<ColumnPointer> columnPtrArray) {
        // TODO
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
