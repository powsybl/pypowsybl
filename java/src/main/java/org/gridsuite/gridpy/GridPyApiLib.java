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
import com.powsybl.iidm.reducer.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;
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
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static org.gridsuite.gridpy.GridPyApiHeader.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class GridPyApiLib {

    private GridPyApiLib() {
    }

    private static void doCatch(ExceptionHandlerPointer exceptionHandlerPtr, Runnable runnable) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            runnable.run();
        } catch (Throwable t) {
            LoggerFactory.getLogger(GridPyApiLib.class).debug(t.getMessage(), t);
            exceptionHandlerPtr.setMessage(CTypeUtil.toCharPtr(t.getMessage()));
        }
    }

    private static boolean doCatch(ExceptionHandlerPointer exceptionHandlerPtr, BooleanSupplier supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            LoggerFactory.getLogger(GridPyApiLib.class).debug(t.getMessage(), t);
            exceptionHandlerPtr.setMessage(CTypeUtil.toCharPtr(t.getMessage()));
            return false;
        }
    }

    interface PointerProvider<T extends WordBase> {

        T get();
    }

    private static <T extends WordBase> T doCatch(ExceptionHandlerPointer exceptionHandlerPtr, PointerProvider<T> supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.get();
        } catch (Throwable t) {
            LoggerFactory.getLogger(GridPyApiLib.class).debug(t.getMessage(), t);
            exceptionHandlerPtr.setMessage(CTypeUtil.toCharPtr(t.getMessage()));
            return WordFactory.zero();
        }
    }

    @CEntryPoint(name = "setDebugMode")
    public static void setDebugMode(IsolateThread thread, boolean debug) {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(debug ? Level.DEBUG : Level.ERROR);
    }

    @CEntryPoint(name = "getVersionTable")
    public static CCharPointer getVersionTable(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(Version.getTableString()));
    }

    @CEntryPoint(name = "createEmptyNetwork")
    public static ObjectHandle createEmptyNetwork(IsolateThread thread, CCharPointer id, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String idStr = CTypeUtil.toString(id);
            Network network = Network.create(idStr, "");
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createIeee14Network")
    public static ObjectHandle createIeee14Network(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = IeeeCdfNetworkFactory.create14();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "createEurostagTutorialExample1Network")
    public static ObjectHandle createEurostagTutorialExample1Network(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = EurostagTutorialExample1Factory.create();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileStr = CTypeUtil.toString(file);
            Network network = Importers.loadNetwork(fileStr);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "dumpNetwork")
    public static void dumpNetwork(IsolateThread thread, ObjectHandle networkHandle, CCharPointer file, CCharPointer format,
                                   ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String fileStr = CTypeUtil.toString(file);
            String formatStr = CTypeUtil.toString(format);
            Exporters.export(formatStr, network, null, Paths.get(fileStr));
        });
    }

    @CEntryPoint(name = "reduceNetwork")
    public static void reduceNetwork(IsolateThread thread, ObjectHandle networkHandle,
                                     double vMin, double vMax,
                                     CCharPointerPointer idsPtrPtr, int idsCount,
                                     CCharPointerPointer vlsPtrPtr, int vlsCount,
                                     CIntPointer depthsPtr, int depthsCount,
                                     boolean withDanglingLines,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReductionOptions options = new ReductionOptions();
            options.withDanglingLlines(withDanglingLines);
            List<NetworkPredicate> predicates = new ArrayList<>();
            if (vMax != Double.MAX_VALUE || vMin != 0) {
                predicates.add(new NominalVoltageNetworkPredicate(vMin, vMax));
            }
            if (idsCount != 0) {
                List<String> ids = CTypeUtil.toStringList(idsPtrPtr, idsCount);
                predicates.add(new IdentifierNetworkPredicate(ids));
            }
            if (depthsCount != 0) {
                final List<Integer> depths = CTypeUtil.toIntegerList(depthsPtr, depthsCount);
                final List<String> voltageLeveles = CTypeUtil.toStringList(vlsPtrPtr, vlsCount);
                for (int i = 0; i < depths.size(); i++) {
                    predicates.add(new SubNetworkPredicate(network.getVoltageLevel(voltageLeveles.get(i)), depths.get(i)));
                }
            }
            final OrNetworkPredicate orNetworkPredicate = new OrNetworkPredicate(predicates);
            NetworkReducer.builder()
                    .withNetworkPredicate(orNetworkPredicate)
                    .withReductionOptions(options)
                    .build()
                    .reduce(network);
        });
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
            ptr.setSlackBusId(CTypeUtil.toCharPtr(componentResult.getSlackBusId()));
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
    public static ArrayPointer<LoadFlowComponentResultPointer> runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean dc,
                                                                           LoadFlowParametersPointer loadFlowParametersPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters parameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
            LoadFlowResult result = LoadFlow.run(network, parameters);
            return createLoadFlowComponentResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "freeLoadFlowComponentResultPointer")
    public static void freeLoadFlowComponentResultPointer(IsolateThread thread, ArrayPointer<LoadFlowComponentResultPointer> componentResultArrayPtr) {
        // don't need to free char* from id field as it is done by python
        freeArrayPointer(componentResultArrayPtr);
    }

    private static void fillBus(Bus bus, BusPointer busPtr) {
        busPtr.setId(CTypeUtil.toCharPtr(bus.getId()));
        busPtr.setVoltageMagnitude(bus.getV());
        busPtr.setVoltageAngle(bus.getAngle());
        busPtr.setComponentNum(ConnectedComponents.getCcNum(bus));
    }

    @CEntryPoint(name = "getBusArray")
    public static ArrayPointer<BusPointer> getBusArray(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<Bus> buses = network.getBusView().getBusStream().collect(Collectors.toList());
            BusPointer busesPtr = UnmanagedMemory.calloc(buses.size() * SizeOf.get(BusPointer.class));
            for (int index = 0; index < buses.size(); index++) {
                Bus bus = buses.get(index);
                BusPointer busPtr = busesPtr.addressOf(index);
                fillBus(bus, busPtr);
            }
            return allocArrayPointer(busesPtr, buses.size());
        });
    }

    @CEntryPoint(name = "freeBusArray")
    public static void freeBusArray(IsolateThread thread, ArrayPointer<BusPointer> busArrayPointer) {
        // don't need to free char* from id field as it is done by python
        freeArrayPointer(busArrayPointer);
    }

    @CEntryPoint(name = "getGeneratorArray")
    public static ArrayPointer<GeneratorPointer> getGeneratorArray(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<Generator> generators = network.getGeneratorStream().collect(Collectors.toList());
            GeneratorPointer generatorPtr = UnmanagedMemory.calloc(generators.size() * SizeOf.get(GeneratorPointer.class));
            for (int index = 0; index < generators.size(); index++) {
                Generator generator = generators.get(index);
                GeneratorPointer generatorPtrI = generatorPtr.addressOf(index);
                generatorPtrI.setId(CTypeUtil.toCharPtr(generator.getId()));
                generatorPtrI.setTargetP(generator.getTargetP());
                generatorPtrI.setMaxP(generator.getMaxP());
                generatorPtrI.setMinP(generator.getMinP());
                VoltageLevel vl = generator.getTerminal().getVoltageLevel();
                generatorPtrI.setNominalVoltage(vl.getNominalV());
                generatorPtrI.setCountry(CTypeUtil.toCharPtr(vl.getSubstation().getCountry().map(Country::name).orElse(null)));
                Bus bus = generator.getTerminal().getBusView().getBus();
                if (bus != null) {
                    BusPointer busPtr = UnmanagedMemory.calloc(SizeOf.get(BusPointer.class));
                    fillBus(bus, busPtr);
                    generatorPtrI.setBus(busPtr);
                }
            }
            return allocArrayPointer(generatorPtr, generators.size());
        });
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
    public static ArrayPointer<LoadPointer> getLoadArray(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<Load> loads = network.getLoadStream().collect(Collectors.toList());
            LoadPointer loadPtr = UnmanagedMemory.calloc(loads.size() * SizeOf.get(LoadPointer.class));
            for (int index = 0; index < loads.size(); index++) {
                Load load = loads.get(index);
                LoadPointer loadPtrI = loadPtr.addressOf(index);
                loadPtrI.setId(CTypeUtil.toCharPtr(load.getId()));
                loadPtrI.setP0(load.getP0());
                VoltageLevel vl = load.getTerminal().getVoltageLevel();
                loadPtrI.setNominalVoltage(vl.getNominalV());
                loadPtrI.setCountry(CTypeUtil.toCharPtr(vl.getSubstation().getCountry().map(Country::name).orElse(null)));
                Bus bus = load.getTerminal().getBusView().getBus();
                if (bus != null) {
                    BusPointer busPtr = UnmanagedMemory.calloc(SizeOf.get(BusPointer.class));
                    fillBus(bus, busPtr);
                    loadPtrI.setBus(busPtr);
                }
            }
            return allocArrayPointer(loadPtr, loads.size());
        });
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

    @CEntryPoint(name = "getNetworkElementsIds")
    public static ArrayPointer<CCharPointerPointer> getNetworkElementsIds(IsolateThread thread, ObjectHandle networkHandle, ElementType elementType,
                                                                          CDoublePointer nominalVoltagePtr, int nominalVoltageCount,
                                                                          CCharPointerPointer countryPtr, int countryCount, boolean mainCc, boolean mainSc,
                                                                          boolean notConnectedToSameBusAtBothSides, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            Set<Double> nominalVoltages = new HashSet<>(CTypeUtil.toDoubleList(nominalVoltagePtr, nominalVoltageCount));
            Set<String> countries = new HashSet<>(CTypeUtil.toStringList(countryPtr, countryCount));
            List<String> elementsIds = NetworkUtil.getElementsIds(network, elementType, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides);
            CCharPointerPointer elementsIdsPtr = UnmanagedMemory.calloc(elementsIds.size() * SizeOf.get(CCharPointerPointer.class));
            for (int i = 0; i < elementsIds.size(); i++) {
                elementsIdsPtr.addressOf(i).write(CTypeUtil.toCharPtr(elementsIds.get(i)));
            }
            return allocArrayPointer(elementsIdsPtr, elementsIds.size());
        });
    }

    @CEntryPoint(name = "freeNetworkElementsIds")
    public static void freeNetworkElementsIds(IsolateThread thread, ArrayPointer<CCharPointerPointer> elementsIdsArrayPtr) {
        freeArrayPointer(elementsIdsArrayPtr);
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

    @CEntryPoint(name = "createSecurityAnalysis")
    public static ObjectHandle createSecurityAnalysis(IsolateThread thread) {
        return ObjectHandles.getGlobal().create(new SecurityAnalysisContext());
    }

    @CEntryPoint(name = "addContingency")
    public static void addContingency(IsolateThread thread, ObjectHandle contingencyContainerHandle, CCharPointer contingencyIdPtr,
                                      CCharPointerPointer elementIdPtrPtr, int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            ContingencyContainer contingencyContainer = ObjectHandles.getGlobal().get(contingencyContainerHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            List<String> elementIds = CTypeUtil.toStringList(elementIdPtrPtr, elementCount);
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
                                                                             ObjectHandle networkHandle, LoadFlowParametersPointer loadFlowParametersPtr,
                                                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters loadFlowParameters = createLoadFlowParameters(false, loadFlowParametersPtr);
            SecurityAnalysisResult result = analysisContext.run(network, loadFlowParameters);
            return createContingencyResultArrayPointer(result);
        });
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
                                       CCharPointerPointer injectionOrTransfoIdPtrPtr, int injectionOrTransfoIdCount,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchsIds = CTypeUtil.toStringList(branchIdPtrPtr, branchIdCount);
            List<String> injectionsOrTransfosIds = CTypeUtil.toStringList(injectionOrTransfoIdPtrPtr, injectionOrTransfoIdCount);
            analysisContext.setFactorMatrix(branchsIds, injectionsOrTransfosIds);
        });
    }

    @CEntryPoint(name = "runSensitivityAnalysis")
    public static ObjectHandle runSensitivityAnalysis(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                      ObjectHandle networkHandle, LoadFlowParametersPointer loadFlowParametersPtr,
                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters loadFlowParameters = createLoadFlowParameters(true, loadFlowParametersPtr);
            SensitivityAnalysisResultContext resultContext = analysisContext.runV2(network, loadFlowParameters);
            return ObjectHandles.getGlobal().create(resultContext);
        });
    }

    @CEntryPoint(name = "getSensitivityMatrix")
    public static MatrixPointer getSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                     CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createSensitivityMatrix(contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceFlows")
    public static MatrixPointer getReferenceFlows(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                  CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createReferenceFlows(contingencyId);
        });
    }

    private static String getBusId(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null ? bus.getId() : "";
    }

    private static <T extends Identifiable<T>> SeriesPointerArrayBuilder<T> addProperties(SeriesPointerArrayBuilder<T> builder) {
        Set<String> propertyNames = builder.getElements().stream()
                .filter(Identifiable::hasProperty)
                .flatMap(e -> e.getPropertyNames().stream())
                .collect(Collectors.toSet());
        for (String propertyName : propertyNames) {
            builder.addStringSeries(propertyName, t -> t.getProperty(propertyName));
        }
        return builder;
    }

    @CEntryPoint(name = "createNetworkElementsSeriesArray")
    public static ArrayPointer<SeriesPointer> createNetworkElementsSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                               ElementType elementType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            switch (elementType) {
                case BUS:
                    List<Bus> buses = network.getBusView().getBusStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(buses)
                            .addStringSeries("id", Bus::getId)
                            .addDoubleSeries("v_mag", Bus::getV)
                            .addDoubleSeries("v_angle", Bus::getAngle))
                            .build();

                case LINE:
                    List<Line> lines = network.getLineStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(lines)
                            .addStringSeries("id", Line::getId)
                            .addDoubleSeries("r", Line::getR)
                            .addDoubleSeries("x", Line::getX)
                            .addDoubleSeries("g1", Line::getG1)
                            .addDoubleSeries("b1", Line::getB1)
                            .addDoubleSeries("g2", Line::getG2)
                            .addDoubleSeries("b2", Line::getB2)
                            .addDoubleSeries("p1", l -> l.getTerminal1().getP())
                            .addDoubleSeries("q1", l -> l.getTerminal1().getQ())
                            .addDoubleSeries("p2", l -> l.getTerminal2().getP())
                            .addDoubleSeries("q2", l -> l.getTerminal2().getQ())
                            .addStringSeries("bus1_id", l -> getBusId(l.getTerminal1()))
                            .addStringSeries("bus2_id", l -> getBusId(l.getTerminal2())))
                            .build();

                case TWO_WINDINGS_TRANSFORMER:
                    List<TwoWindingsTransformer> transformers2 = network.getTwoWindingsTransformerStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(transformers2)
                            .addStringSeries("id", TwoWindingsTransformer::getId)
                            .addDoubleSeries("r", TwoWindingsTransformer::getR)
                            .addDoubleSeries("x", TwoWindingsTransformer::getX)
                            .addDoubleSeries("g", TwoWindingsTransformer::getG)
                            .addDoubleSeries("b", TwoWindingsTransformer::getB)
                            .addDoubleSeries("rated_u1", TwoWindingsTransformer::getRatedU1)
                            .addDoubleSeries("rated_u2", TwoWindingsTransformer::getRatedU2)
                            .addDoubleSeries("rated_s", TwoWindingsTransformer::getRatedS)
                            .addDoubleSeries("p1", twt -> twt.getTerminal1().getP())
                            .addDoubleSeries("q1", twt -> twt.getTerminal1().getQ())
                            .addDoubleSeries("p2", twt -> twt.getTerminal2().getP())
                            .addDoubleSeries("q2", twt -> twt.getTerminal2().getQ())
                            .addStringSeries("bus1_id", twt -> getBusId(twt.getTerminal1()))
                            .addStringSeries("bus2_id", twt -> getBusId(twt.getTerminal2())))
                            .build();

                case THREE_WINDINGS_TRANSFORMER:
                    List<ThreeWindingsTransformer> transformers3 = network.getThreeWindingsTransformerStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(transformers3)
                            .addStringSeries("id", ThreeWindingsTransformer::getId)
                            .addDoubleSeries("rated_u0", ThreeWindingsTransformer::getRatedU0)
                            .addDoubleSeries("r1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("x1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("g1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("b1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("rated_u1", twt -> twt.getLeg1().getRatedU())
                            .addDoubleSeries("rated_s1", twt -> twt.getLeg1().getRatedS())
                            .addDoubleSeries("p1", twt -> twt.getLeg1().getTerminal().getP())
                            .addDoubleSeries("q1", twt -> twt.getLeg1().getTerminal().getP())
                            .addStringSeries("bus1_id", twt -> getBusId(twt.getLeg1().getTerminal()))
                            .addDoubleSeries("r2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("x2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("g2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("b2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("rated_u2", twt -> twt.getLeg2().getRatedU())
                            .addDoubleSeries("rated_s2", twt -> twt.getLeg2().getRatedS())
                            .addDoubleSeries("p2", twt -> twt.getLeg2().getTerminal().getP())
                            .addDoubleSeries("q2", twt -> twt.getLeg2().getTerminal().getP())
                            .addStringSeries("bus2_id", twt -> getBusId(twt.getLeg2().getTerminal()))
                            .addDoubleSeries("r3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("x3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("g3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("b3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("rated_u3", twt -> twt.getLeg3().getRatedU())
                            .addDoubleSeries("rated_s3", twt -> twt.getLeg3().getRatedS())
                            .addDoubleSeries("p3", twt -> twt.getLeg3().getTerminal().getP())
                            .addDoubleSeries("q3", twt -> twt.getLeg3().getTerminal().getP())
                            .addStringSeries("bus3_id", twt -> getBusId(twt.getLeg3().getTerminal())))
                            .build();

                case GENERATOR:
                    List<Generator> generators = network.getGeneratorStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(generators)
                            .addStringSeries("id", Generator::getId)
                            .addEnumSeries("energy_source", Generator::getEnergySource)
                            .addDoubleSeries("target_p", Generator::getTargetP)
                            .addDoubleSeries("max_p", Generator::getMaxP)
                            .addDoubleSeries("min_p", Generator::getMinP)
                            .addDoubleSeries("target_v", Generator::getTargetV)
                            .addDoubleSeries("target_q", Generator::getTargetQ)
                            .addBooleanSeries("voltage_regulator_on", Generator::isVoltageRegulatorOn)
                            .addDoubleSeries("p", g -> g.getTerminal().getP())
                            .addDoubleSeries("q", g -> g.getTerminal().getQ())
                            .addStringSeries("bus_id", g -> getBusId(g.getTerminal())))
                            .build();

                case LOAD:
                    List<Load> loads = network.getLoadStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(loads)
                            .addStringSeries("id", Load::getId)
                            .addEnumSeries("type", Load::getLoadType)
                            .addDoubleSeries("p0", Load::getP0)
                            .addDoubleSeries("q0", Load::getQ0)
                            .addDoubleSeries("p", l -> l.getTerminal().getP())
                            .addDoubleSeries("q", l -> l.getTerminal().getQ())
                            .addStringSeries("bus_id", l -> getBusId(l.getTerminal())))
                            .build();

                case SHUNT_COMPENSATOR:
                    List<ShuntCompensator> shunts = network.getShuntCompensatorStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(shunts)
                            .addStringSeries("id", ShuntCompensator::getId)
                            .addEnumSeries("model_type", ShuntCompensator::getModelType)
                            .addDoubleSeries("p", g -> g.getTerminal().getP())
                            .addDoubleSeries("q", g -> g.getTerminal().getQ())
                            .addStringSeries("bus_id", g -> getBusId(g.getTerminal())))
                            .build();

                case DANGLING_LINE:
                    List<DanglingLine> danglingLines = network.getDanglingLineStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(danglingLines)
                            .addStringSeries("id", DanglingLine::getId)
                            .addDoubleSeries("r", DanglingLine::getR)
                            .addDoubleSeries("x", DanglingLine::getX)
                            .addDoubleSeries("g", DanglingLine::getG)
                            .addDoubleSeries("b", DanglingLine::getB)
                            .addDoubleSeries("p0", DanglingLine::getP0)
                            .addDoubleSeries("q0", DanglingLine::getQ0)
                            .addDoubleSeries("p", dl -> dl.getTerminal().getP())
                            .addDoubleSeries("q", dl -> dl.getTerminal().getQ())
                            .addStringSeries("bus_id", dl -> getBusId(dl.getTerminal())))
                            .build();

                case LCC_CONVERTER_STATION:
                    List<LccConverterStation> lccStations = network.getLccConverterStationStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(lccStations)
                            .addStringSeries("id", LccConverterStation::getId)
                            .addDoubleSeries("power_factor", LccConverterStation::getPowerFactor)
                            .addDoubleSeries("loss_factor", LccConverterStation::getLossFactor)
                            .addDoubleSeries("p", st -> st.getTerminal().getP())
                            .addDoubleSeries("q", st -> st.getTerminal().getQ())
                            .addStringSeries("bus_id", st -> getBusId(st.getTerminal())))
                            .build();

                case VSC_CONVERTER_STATION:
                    List<VscConverterStation> vscStations = network.getVscConverterStationStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(vscStations)
                            .addStringSeries("id", VscConverterStation::getId)
                            .addDoubleSeries("voltage_setpoint", VscConverterStation::getVoltageSetpoint)
                            .addDoubleSeries("reactive_power_setpoint", VscConverterStation::getReactivePowerSetpoint)
                            .addBooleanSeries("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn)
                            .addDoubleSeries("p", st -> st.getTerminal().getP())
                            .addDoubleSeries("q", st -> st.getTerminal().getQ())
                            .addStringSeries("bus_id", st -> getBusId(st.getTerminal())))
                            .build();

                case STATIC_VAR_COMPENSATOR:
                    List<StaticVarCompensator> svcs = network.getStaticVarCompensatorStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(svcs)
                            .addStringSeries("id", StaticVarCompensator::getId)
                            .addDoubleSeries("voltage_setpoint", StaticVarCompensator::getVoltageSetpoint)
                            .addDoubleSeries("reactive_power_setpoint", StaticVarCompensator::getReactivePowerSetpoint)
                            .addEnumSeries("regulation_mode", StaticVarCompensator::getRegulationMode)
                            .addDoubleSeries("p", svc -> svc.getTerminal().getP())
                            .addDoubleSeries("q", svc -> svc.getTerminal().getQ())
                            .addStringSeries("bus_id", svc -> getBusId(svc.getTerminal())))
                            .build();

                case SWITCH:
                    List<Switch> switches = network.getSwitchStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(switches)
                            .addStringSeries("id", Switch::getId)
                            .addEnumSeries("kind", Switch::getKind)
                            .addBooleanSeries("open", Switch::isOpen))
                            .build();

                default:
                    throw new UnsupportedOperationException("Element type not supported: " + elementType);
            }
        });
    }

    @CEntryPoint(name = "freeNetworkElementsSeriesArray")
    public static void freeNetworkElementsSeriesArray(IsolateThread thread, ArrayPointer<SeriesPointer> seriesPtrArrayPtr) {
        // don't need to free char* from id field as it is done by python
        for (int i = 0; i < seriesPtrArrayPtr.getLength(); i++) {
            SeriesPointer seriesPtrPlus = seriesPtrArrayPtr.getPtr().addressOf(i);
            UnmanagedMemory.free(seriesPtrPlus.data().getPtr());
        }
        freeArrayPointer(seriesPtrArrayPtr);
    }

    @CEntryPoint(name = "updateNetworkElementsWithIntSeries")
    public static void updateNetworkElementsWithIntSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                          ElementType elementType, CCharPointer seriesNamePtr,
                                                          CCharPointerPointer elementIdPtrPtr, CIntPointer valuePtr,
                                                          int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            for (int i = 0; i < elementCount; i++) {
                CCharPointer elementIdPtr = elementIdPtrPtr.read(i);
                String id = CTypeUtil.toString(elementIdPtr);
                int value = valuePtr.read(i);
                switch (elementType) {
                    case SWITCH:
                        Switch sw = network.getSwitch(id);
                        if (sw == null) {
                            throw new PowsyblException("Switch '" + id + "' not found");
                        }
                        switch (seriesName) {
                            case "open":
                                sw.setOpen(value == 1);
                                break;

                            default:
                                throw new UnsupportedOperationException("Series name not supported for switch elements: " + seriesName);
                        }
                        break;

                    default:
                        throw new UnsupportedOperationException("Element type not supported: " + elementType);
                }
            }
        });
    }

    @CEntryPoint(name = "updateNetworkElementsWithDoubleSeries")
    public static void updateNetworkElementsWithDoubleSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                             ElementType elementType, CCharPointer seriesNamePtr,
                                                             CCharPointerPointer elementIdPtrPtr, CDoublePointer valuePtr,
                                                             int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            for (int i = 0; i < elementCount; i++) {
                CCharPointer elementIdPtr = elementIdPtrPtr.read(i);
                String id = CTypeUtil.toString(elementIdPtr);
                double value = valuePtr.read(i);
                switch (elementType) {
                    case GENERATOR:
                        Generator g = network.getGenerator(id);
                        if (g == null) {
                            throw new PowsyblException("Generator '" + id + "' not found");
                        }
                        switch (seriesName) {
                            case "target_p":
                                g.setTargetP(value);
                                break;

                            default:
                                throw new UnsupportedOperationException("Series name not supported for switch elements: " + seriesName);
                        }
                        break;

                    default:
                        throw new UnsupportedOperationException("Element type not supported: " + elementType);
                }
            }
        });
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
