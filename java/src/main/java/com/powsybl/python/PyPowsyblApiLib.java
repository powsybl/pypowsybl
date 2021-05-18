/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.iidm.network.util.ConnectedComponents;
import com.powsybl.iidm.parameters.Parameter;
import com.powsybl.iidm.reducer.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.tools.Version;
import org.apache.commons.lang3.tuple.Triple;
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
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import static com.powsybl.python.PyPowsyblApiHeader.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class PyPowsyblApiLib {

    private PyPowsyblApiLib() {
    }

    private static void setException(ExceptionHandlerPointer exceptionHandlerPtr, Throwable t) {
        LoggerFactory.getLogger(PyPowsyblApiLib.class).debug(t.getMessage(), t);
        // we need to create a non null message as on C++ side a null message is considered as non exception to rethrow
        // typically a NullPointerException has a null message and an empty string message need to be set in order to
        // correctly handle the exception on C++ side
        String nonNullMessage = Objects.requireNonNull(t.getMessage(), "");
        exceptionHandlerPtr.setMessage(CTypeUtil.toCharPtr(nonNullMessage));
    }

    private static void doCatch(ExceptionHandlerPointer exceptionHandlerPtr, Runnable runnable) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            runnable.run();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
        }
    }

    private static boolean doCatch(ExceptionHandlerPointer exceptionHandlerPtr, BooleanSupplier supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return false;
        }
    }

    private static int doCatch(ExceptionHandlerPointer exceptionHandlerPtr, IntSupplier supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return -1;
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
            setException(exceptionHandlerPtr, t);
            return WordFactory.zero();
        }
    }

    @CEntryPoint(name = "setDebugMode")
    public static void setDebugMode(IsolateThread thread, boolean debug, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(debug ? Level.DEBUG : Level.ERROR);
        });
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

    @CEntryPoint(name = "createIeeeNetwork")
    public static ObjectHandle createIeeeNetwork(IsolateThread thread, int busCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network;
            switch (busCount) {
                case 9:
                    network = IeeeCdfNetworkFactory.create9();
                    break;
                case 14:
                    network = IeeeCdfNetworkFactory.create14();
                    break;
                case 30:
                    network = IeeeCdfNetworkFactory.create30();
                    break;
                case 57:
                    network = IeeeCdfNetworkFactory.create57();
                    break;
                case 118:
                    network = IeeeCdfNetworkFactory.create118();
                    break;
                case 300:
                    network = IeeeCdfNetworkFactory.create300();
                    break;
                default:
                    throw new PowsyblException("IEEE " + busCount + " buses case not found");
            }
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

    static ArrayPointer<CCharPointerPointer> createCharPtrArray(List<String> stringList) {
        CCharPointerPointer stringListPtr = UnmanagedMemory.calloc(stringList.size() * SizeOf.get(CCharPointerPointer.class));
        for (int i = 0; i < stringList.size(); i++) {
            stringListPtr.addressOf(i).write(CTypeUtil.toCharPtr(stringList.get(i)));
        }
        return allocArrayPointer(stringListPtr, stringList.size());
    }

    @CEntryPoint(name = "getNetworkImportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkImportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ArrayList<>(Importers.getFormats())));
    }

    @CEntryPoint(name = "getNetworkExportFormats")
    public static ArrayPointer<CCharPointerPointer> getNetworkExportFormats(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ArrayList<>(Exporters.getFormats())));
    }

    @CEntryPoint(name = "freeStringArray")
    public static void freeStringArray(IsolateThread thread, ArrayPointer<?> arrayPtr,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeArrayPointer(arrayPtr));
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
            List<Parameter> parameters = importer.getParameters();
            return new SeriesPointerArrayBuilder<>(parameters)
                    .addStringSeries("name", true, Parameter::getName)
                    .addStringSeries("description", Parameter::getDescription)
                    .addEnumSeries("type", Parameter::getType)
                    .addStringSeries("default", p -> Objects.toString(p.getDefaultValue(), ""))
                    .build();
        });
    }

    private static Properties createParameters(CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                               CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount) {
        List<String> parameterNames = CTypeUtil.toStringList(parameterNamesPtrPtr, parameterNamesCount);
        List<String> parameterValues = CTypeUtil.toStringList(parameterValuesPtrPtr, parameterValuesCount);
        Properties parameters = new Properties();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.setProperty(parameterNames.get(i), parameterValues.get(i));
        }
        return parameters;
    }

    @CEntryPoint(name = "createFourSubstationsNodeBreakerNetwork")
    public static ObjectHandle createFourSubstationsNodeBreakerNetwork(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = FourSubstationsNodeBreakerFactory.create();
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file, CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                           CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String fileStr = CTypeUtil.toString(file);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Network network = Importers.loadNetwork(Paths.get(fileStr), LocalComputationManager.getDefault(), ImportConfig.load(), parameters);
            return ObjectHandles.getGlobal().create(network);
        });
    }

    @CEntryPoint(name = "dumpNetwork")
    public static void dumpNetwork(IsolateThread thread, ObjectHandle networkHandle, CCharPointer file, CCharPointer format,
                                   CCharPointerPointer parameterNamesPtrPtr, int parameterNamesCount,
                                   CCharPointerPointer parameterValuesPtrPtr, int parameterValuesCount,
                                   ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String fileStr = CTypeUtil.toString(file);
            String formatStr = CTypeUtil.toString(format);
            Properties parameters = createParameters(parameterNamesPtrPtr, parameterNamesCount, parameterValuesPtrPtr, parameterValuesCount);
            Exporters.export(formatStr, network, parameters, Paths.get(fileStr));
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
                                                                           LoadFlowParametersPointer loadFlowParametersPtr,
                                                                           CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters parameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
            String providerStr = CTypeUtil.toString(provider);
            LoadFlow.Runner runner = LoadFlow.find(providerStr);
            LoadFlowResult result = runner.run(network, parameters);
            return createLoadFlowComponentResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "freeLoadFlowComponentResultPointer")
    public static void freeLoadFlowComponentResultPointer(IsolateThread thread, ArrayPointer<LoadFlowComponentResultPointer> componentResultArrayPtr,
                                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            // don't need to free char* from id field as it is done by python
            freeArrayPointer(componentResultArrayPtr);
        });
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
    public static void freeBusArray(IsolateThread thread, ArrayPointer<BusPointer> busArrayPointer, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            // don't need to free char* from id field as it is done by python
            freeArrayPointer(busArrayPointer);
        });
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
    public static void freeGeneratorArray(IsolateThread thread, ArrayPointer<GeneratorPointer> generatorArrayPtr,
                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int index = 0; index < generatorArrayPtr.getLength(); index++) {
                GeneratorPointer generatorPtrI = generatorArrayPtr.getPtr().addressOf(index);
                // don't need to free char* from id field as it is done by python
                if (generatorPtrI.getBus().isNonNull()) {
                    UnmanagedMemory.free(generatorPtrI.getBus());
                }
            }
            freeArrayPointer(generatorArrayPtr);
        });
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
    public static void freeLoadArray(IsolateThread thread, ArrayPointer<LoadPointer> loadArrayPtr,
                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int index = 0; index < loadArrayPtr.getLength(); index++) {
                LoadPointer loadPtrI = loadArrayPtr.getPtr().addressOf(index);
                // don't need to free char* from id field as it is done by python
                if (loadPtrI.getBus().isNonNull()) {
                    UnmanagedMemory.free(loadPtrI.getBus());
                }
            }
            freeArrayPointer(loadArrayPtr);
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
            return createCharPtrArray(elementsIds);
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
                                                                             CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters loadFlowParameters = createLoadFlowParameters(false, loadFlowParametersPtr);
            String providerStr = CTypeUtil.toString(provider);
            SecurityAnalysisResult result = analysisContext.run(network, loadFlowParameters, providerStr);
            return createContingencyResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "freeContingencyResultArrayPointer")
    public static void freeContingencyResultArrayPointer(IsolateThread thread, ArrayPointer<ContingencyResultPointer> contingencyResultArrayPtr,
                                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            // don't need to free char* from id field as it is done by python
            for (int i = 0; i < contingencyResultArrayPtr.getLength(); i++) {
                ContingencyResultPointer contingencyResultPtrPlus = contingencyResultArrayPtr.getPtr().addressOf(i);
                UnmanagedMemory.free(contingencyResultPtrPlus.limitViolations().getPtr());
            }
            freeArrayPointer(contingencyResultArrayPtr);
        });
    }

    @CEntryPoint(name = "createSensitivityAnalysis")
    public static ObjectHandle createSensitivityAnalysis(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new SensitivityAnalysisContext()));
    }

    @CEntryPoint(name = "setBranchFlowFactorMatrix")
    public static void setBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer injectionOrTransfoIdPtrPtr, int injectionOrTransfoIdCount,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchsIds = CTypeUtil.toStringList(branchIdPtrPtr, branchIdCount);
            List<String> injectionsOrTransfosIds = CTypeUtil.toStringList(injectionOrTransfoIdPtrPtr, injectionOrTransfoIdCount);
            analysisContext.setBranchFlowFactorMatrix(branchsIds, injectionsOrTransfosIds);
        });
    }

    @CEntryPoint(name = "setBusVoltageFactorMatrix")
    public static void setBusVoltageFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer busVoltageIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer targetVoltageIdPtrPtr, int injectionOrTransfoIdCount,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> busVoltageIds = CTypeUtil.toStringList(busVoltageIdPtrPtr, branchIdCount);
            List<String> targetVoltageIds = CTypeUtil.toStringList(targetVoltageIdPtrPtr, injectionOrTransfoIdCount);
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
            SensitivityAnalysisResultContext resultContext = analysisContext.run(network, loadFlowParameters, providerStr);
            return ObjectHandles.getGlobal().create(resultContext);
        });
    }

    @CEntryPoint(name = "getBranchFlowsSensitivityMatrix")
    public static MatrixPointer getBranchFlowsSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createBranchFlowsSensitivityMatrix(contingencyId);
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
                                                  CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createReferenceFlows(contingencyId);
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
                            .addStringSeries("id", true, Bus::getId)
                            .addDoubleSeries("v_mag", Bus::getV)
                            .addDoubleSeries("v_angle", Bus::getAngle))
                            .build();

                case LINE:
                    List<Line> lines = network.getLineStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(lines)
                            .addStringSeries("id", true, Line::getId)
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
                            .addStringSeries("voltage_level1_id", l -> l.getTerminal1().getVoltageLevel().getId())
                            .addStringSeries("voltage_level2_id", l -> l.getTerminal2().getVoltageLevel().getId())
                            .addStringSeries("bus1_id", l -> getBusId(l.getTerminal1()))
                            .addStringSeries("bus2_id", l -> getBusId(l.getTerminal2())))
                            .build();

                case TWO_WINDINGS_TRANSFORMER:
                    List<TwoWindingsTransformer> transformers2 = network.getTwoWindingsTransformerStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(transformers2)
                            .addStringSeries("id", true, TwoWindingsTransformer::getId)
                            .addDoubleSeries("r", TwoWindingsTransformer::getR)
                            .addDoubleSeries("x", TwoWindingsTransformer::getX)
                            .addDoubleSeries("g", TwoWindingsTransformer::getG)
                            .addDoubleSeries("b", TwoWindingsTransformer::getB)
                            .addDoubleSeries("rated_u1", TwoWindingsTransformer::getRatedU1)
                            .addDoubleSeries("rated_u2", TwoWindingsTransformer::getRatedU2)
                            .addDoubleSeries("rated_s", TwoWindingsTransformer::getRatedS)
                            .addIntSeries("ratio_tap_position", TwoWindingsTransformer::getRatioTapChanger, TapChanger::getTapPosition)
                            .addIntSeries("phase_tap_position", TwoWindingsTransformer::getPhaseTapChanger, TapChanger::getTapPosition)
                            .addDoubleSeries("p1", twt -> twt.getTerminal1().getP())
                            .addDoubleSeries("q1", twt -> twt.getTerminal1().getQ())
                            .addDoubleSeries("p2", twt -> twt.getTerminal2().getP())
                            .addDoubleSeries("q2", twt -> twt.getTerminal2().getQ())
                            .addStringSeries("voltage_level1_id", twt -> twt.getTerminal1().getVoltageLevel().getId())
                            .addStringSeries("voltage_level2_id", twt -> twt.getTerminal2().getVoltageLevel().getId())
                            .addStringSeries("bus1_id", twt -> getBusId(twt.getTerminal1()))
                            .addStringSeries("bus2_id", twt -> getBusId(twt.getTerminal2())))
                            .build();

                case THREE_WINDINGS_TRANSFORMER:
                    List<ThreeWindingsTransformer> transformers3 = network.getThreeWindingsTransformerStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(transformers3)
                            .addStringSeries("id", true, ThreeWindingsTransformer::getId)
                            .addDoubleSeries("rated_u0", ThreeWindingsTransformer::getRatedU0)
                            .addDoubleSeries("r1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("x1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("g1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("b1", twt -> twt.getLeg1().getR())
                            .addDoubleSeries("rated_u1", twt -> twt.getLeg1().getRatedU())
                            .addDoubleSeries("rated_s1", twt -> twt.getLeg1().getRatedS())
                            .addIntSeries("ratio_tap_position1", twt -> twt.getLeg1().getRatioTapChanger(), TapChanger::getTapPosition)
                            .addIntSeries("phase_tap_position1", twt -> twt.getLeg1().getPhaseTapChanger(), TapChanger::getTapPosition)
                            .addDoubleSeries("p1", twt -> twt.getLeg1().getTerminal().getP())
                            .addDoubleSeries("q1", twt -> twt.getLeg1().getTerminal().getP())
                            .addStringSeries("voltage_level1_id", twt -> twt.getLeg1().getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus1_id", twt -> getBusId(twt.getLeg1().getTerminal()))
                            .addDoubleSeries("r2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("x2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("g2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("b2", twt -> twt.getLeg2().getR())
                            .addDoubleSeries("rated_u2", twt -> twt.getLeg2().getRatedU())
                            .addDoubleSeries("rated_s2", twt -> twt.getLeg2().getRatedS())
                            .addIntSeries("ratio_tap_position2", twt -> twt.getLeg2().getRatioTapChanger(), TapChanger::getTapPosition)
                            .addIntSeries("phase_tap_position2", twt -> twt.getLeg2().getPhaseTapChanger(), TapChanger::getTapPosition)
                            .addDoubleSeries("p2", twt -> twt.getLeg2().getTerminal().getP())
                            .addDoubleSeries("q2", twt -> twt.getLeg2().getTerminal().getP())
                            .addStringSeries("voltage_level2_id", twt -> twt.getLeg2().getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus2_id", twt -> getBusId(twt.getLeg2().getTerminal()))
                            .addDoubleSeries("r3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("x3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("g3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("b3", twt -> twt.getLeg3().getR())
                            .addDoubleSeries("rated_u3", twt -> twt.getLeg3().getRatedU())
                            .addDoubleSeries("rated_s3", twt -> twt.getLeg3().getRatedS())
                            .addIntSeries("ratio_tap_position3", twt -> twt.getLeg3().getRatioTapChanger(), TapChanger::getTapPosition)
                            .addIntSeries("phase_tap_position3", twt -> twt.getLeg3().getPhaseTapChanger(), TapChanger::getTapPosition)
                            .addDoubleSeries("p3", twt -> twt.getLeg3().getTerminal().getP())
                            .addDoubleSeries("q3", twt -> twt.getLeg3().getTerminal().getP())
                            .addStringSeries("voltage_level3_id", twt -> twt.getLeg3().getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus3_id", twt -> getBusId(twt.getLeg3().getTerminal())))
                            .build();

                case GENERATOR:
                    List<Generator> generators = network.getGeneratorStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(generators)
                            .addStringSeries("id", true, Generator::getId)
                            .addEnumSeries("energy_source", Generator::getEnergySource)
                            .addDoubleSeries("target_p", Generator::getTargetP)
                            .addDoubleSeries("max_p", Generator::getMaxP)
                            .addDoubleSeries("min_p", Generator::getMinP)
                            .addDoubleSeries("target_v", Generator::getTargetV)
                            .addDoubleSeries("target_q", Generator::getTargetQ)
                            .addBooleanSeries("voltage_regulator_on", Generator::isVoltageRegulatorOn)
                            .addDoubleSeries("p", g -> g.getTerminal().getP())
                            .addDoubleSeries("q", g -> g.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", g -> g.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", g -> getBusId(g.getTerminal())))
                            .build();

                case LOAD:
                    List<Load> loads = network.getLoadStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(loads)
                            .addStringSeries("id", true, Load::getId)
                            .addEnumSeries("type", Load::getLoadType)
                            .addDoubleSeries("p0", Load::getP0)
                            .addDoubleSeries("q0", Load::getQ0)
                            .addDoubleSeries("p", l -> l.getTerminal().getP())
                            .addDoubleSeries("q", l -> l.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", l -> l.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", l -> getBusId(l.getTerminal())))
                            .build();

                case SHUNT_COMPENSATOR:
                    List<ShuntCompensator> shunts = network.getShuntCompensatorStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(shunts)
                            .addStringSeries("id", true, ShuntCompensator::getId)
                            .addEnumSeries("model_type", ShuntCompensator::getModelType)
                            .addDoubleSeries("p", sc -> sc.getTerminal().getP())
                            .addDoubleSeries("q", sc -> sc.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", sc -> sc.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", sc -> getBusId(sc.getTerminal())))
                            .build();

                case DANGLING_LINE:
                    List<DanglingLine> danglingLines = network.getDanglingLineStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(danglingLines)
                            .addStringSeries("id", true, DanglingLine::getId)
                            .addDoubleSeries("r", DanglingLine::getR)
                            .addDoubleSeries("x", DanglingLine::getX)
                            .addDoubleSeries("g", DanglingLine::getG)
                            .addDoubleSeries("b", DanglingLine::getB)
                            .addDoubleSeries("p0", DanglingLine::getP0)
                            .addDoubleSeries("q0", DanglingLine::getQ0)
                            .addDoubleSeries("p", dl -> dl.getTerminal().getP())
                            .addDoubleSeries("q", dl -> dl.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", dl -> dl.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", dl -> getBusId(dl.getTerminal())))
                            .build();

                case LCC_CONVERTER_STATION:
                    List<LccConverterStation> lccStations = network.getLccConverterStationStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(lccStations)
                            .addStringSeries("id", true, LccConverterStation::getId)
                            .addDoubleSeries("power_factor", LccConverterStation::getPowerFactor)
                            .addDoubleSeries("loss_factor", LccConverterStation::getLossFactor)
                            .addDoubleSeries("p", st -> st.getTerminal().getP())
                            .addDoubleSeries("q", st -> st.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", st -> st.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", st -> getBusId(st.getTerminal())))
                            .build();

                case VSC_CONVERTER_STATION:
                    List<VscConverterStation> vscStations = network.getVscConverterStationStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(vscStations)
                            .addStringSeries("id", true, VscConverterStation::getId)
                            .addDoubleSeries("voltage_setpoint", VscConverterStation::getVoltageSetpoint)
                            .addDoubleSeries("reactive_power_setpoint", VscConverterStation::getReactivePowerSetpoint)
                            .addBooleanSeries("voltage_regulator_on", VscConverterStation::isVoltageRegulatorOn)
                            .addDoubleSeries("p", st -> st.getTerminal().getP())
                            .addDoubleSeries("q", st -> st.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", st -> st.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", st -> getBusId(st.getTerminal())))
                            .build();

                case STATIC_VAR_COMPENSATOR:
                    List<StaticVarCompensator> svcs = network.getStaticVarCompensatorStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(svcs)
                            .addStringSeries("id", true, StaticVarCompensator::getId)
                            .addDoubleSeries("voltage_setpoint", StaticVarCompensator::getVoltageSetpoint)
                            .addDoubleSeries("reactive_power_setpoint", StaticVarCompensator::getReactivePowerSetpoint)
                            .addEnumSeries("regulation_mode", StaticVarCompensator::getRegulationMode)
                            .addDoubleSeries("p", svc -> svc.getTerminal().getP())
                            .addDoubleSeries("q", svc -> svc.getTerminal().getQ())
                            .addStringSeries("voltage_level_id", svc -> svc.getTerminal().getVoltageLevel().getId())
                            .addStringSeries("bus_id", svc -> getBusId(svc.getTerminal())))
                            .build();

                case SWITCH:
                    List<Switch> switches = network.getSwitchStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(switches)
                            .addStringSeries("id", true, Switch::getId)
                            .addEnumSeries("kind", Switch::getKind)
                            .addBooleanSeries("open", Switch::isOpen)
                            .addBooleanSeries("retained", Switch::isRetained)
                            .addStringSeries("voltage_level_id", sw -> sw.getVoltageLevel().getId()))
                            .build();

                case VOLTAGE_LEVEL:
                    List<VoltageLevel> voltageLevels = network.getVoltageLevelStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(voltageLevels)
                            .addStringSeries("id", true, VoltageLevel::getId)
                            .addStringSeries("substation_id", vl -> vl.getSubstation().getId())
                            .addDoubleSeries("nominal_v", VoltageLevel::getNominalV)
                            .addDoubleSeries("high_voltage_limit", VoltageLevel::getHighVoltageLimit)
                            .addDoubleSeries("low_voltage_limit", VoltageLevel::getLowVoltageLimit))
                            .build();

                case SUBSTATION:
                    List<Substation> substations = network.getSubstationStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(substations))
                            .addStringSeries("id", true, Identifiable::getId)
                            .addStringSeries("TSO", Substation::getTso)
                            .addStringSeries("geo_tags", substation -> String.join(",", substation.getGeographicalTags()))
                            .addStringSeries("country", substation -> substation.getCountry().map(Country::toString).orElse(""))
                            .build();

                case BUSBAR_SECTION:
                    List<BusbarSection> busbarSections = network.getBusbarSectionStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(busbarSections))
                            .addStringSeries("id", true, BusbarSection::getId)
                            .addDoubleSeries("v", BusbarSection::getV)
                            .addDoubleSeries("angle", BusbarSection::getAngle)
                            .addStringSeries("voltage_level_id", bbs -> bbs.getTerminal().getVoltageLevel().getId())
                            .build();

                case HVDC_LINE:
                    List<HvdcLine> hvdcLines = network.getHvdcLineStream().collect(Collectors.toList());
                    return addProperties(new SeriesPointerArrayBuilder<>(hvdcLines)
                            .addStringSeries("id", true, HvdcLine::getId)
                            .addEnumSeries("converters_mode", HvdcLine::getConvertersMode)
                            .addDoubleSeries("active_power_setpoint", HvdcLine::getActivePowerSetpoint)
                            .addDoubleSeries("max_p", HvdcLine::getMaxP)
                            .addDoubleSeries("nominal_v", HvdcLine::getNominalV)
                            .addDoubleSeries("r", HvdcLine::getR)
                            .addStringSeries("converter_station1_id", l -> l.getConverterStation1().getId())
                            .addStringSeries("converter_station2_id", l -> l.getConverterStation2().getId()))
                            .build();

                case RATIO_TAP_CHANGER_STEP:
                    List<Triple<String, RatioTapChanger, Integer>> ratioTapChangerSteps = network.getTwoWindingsTransformerStream()
                            .filter(twt -> twt.getRatioTapChanger() != null)
                            .flatMap(twt -> twt.getRatioTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getRatioTapChanger(), position)))
                            .collect(Collectors.toList());
                    return new SeriesPointerArrayBuilder<>(ratioTapChangerSteps)
                            .addStringSeries("id", true, Triple::getLeft)
                            .addIntSeries("position", true, Triple::getRight)
                            .addDoubleSeries("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
                            .addDoubleSeries("r", p -> p.getMiddle().getStep(p.getRight()).getR())
                            .addDoubleSeries("x", p -> p.getMiddle().getStep(p.getRight()).getX())
                            .addDoubleSeries("g", p -> p.getMiddle().getStep(p.getRight()).getG())
                            .addDoubleSeries("b", p -> p.getMiddle().getStep(p.getRight()).getB())
                            .build();

                case PHASE_TAP_CHANGER_STEP:
                    List<Triple<String, PhaseTapChanger, Integer>> phaseTapChangerSteps = network.getTwoWindingsTransformerStream()
                            .filter(twt -> twt.getPhaseTapChanger() != null)
                            .flatMap(twt -> twt.getPhaseTapChanger().getAllSteps().keySet().stream().map(position -> Triple.of(twt.getId(), twt.getPhaseTapChanger(), position)))
                            .collect(Collectors.toList());
                    return new SeriesPointerArrayBuilder<>(phaseTapChangerSteps)
                            .addStringSeries("id", true, Triple::getLeft)
                            .addIntSeries("position", true, Triple::getRight)
                            .addDoubleSeries("rho", p -> p.getMiddle().getStep(p.getRight()).getRho())
                            .addDoubleSeries("alpha", p -> p.getMiddle().getStep(p.getRight()).getAlpha())
                            .addDoubleSeries("r", p -> p.getMiddle().getStep(p.getRight()).getR())
                            .addDoubleSeries("x", p -> p.getMiddle().getStep(p.getRight()).getX())
                            .addDoubleSeries("g", p -> p.getMiddle().getStep(p.getRight()).getG())
                            .addDoubleSeries("b", p -> p.getMiddle().getStep(p.getRight()).getB())
                            .build();

                default:
                    throw new UnsupportedOperationException("Element type not supported: " + elementType);
            }
        });
    }

    @CEntryPoint(name = "freeSeriesArray")
    public static void freeSeriesArray(IsolateThread thread, ArrayPointer<SeriesPointer> seriesPtrArrayPtr,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            // don't need to free char* from id field as it is done by python
            for (int i = 0; i < seriesPtrArrayPtr.getLength(); i++) {
                SeriesPointer seriesPtrPlus = seriesPtrArrayPtr.getPtr().addressOf(i);
                UnmanagedMemory.free(seriesPtrPlus.data().getPtr());
            }
            freeArrayPointer(seriesPtrArrayPtr);
        });
    }

    @CEntryPoint(name = "getSeriesType")
    public static int getSeriesType(IsolateThread thread, ElementType elementType, CCharPointer seriesNamePtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            Map<String, Integer> seriesTypes;
            switch (elementType) {
                case SWITCH:
                    seriesTypes = SeriesDataTypeConstants.SWITCH_MAP;
                    break;
                case GENERATOR:
                    seriesTypes = SeriesDataTypeConstants.GENERATOR_MAP;
                    break;
                case HVDC_LINE:
                    seriesTypes = SeriesDataTypeConstants.HVDC_LINE_MAP;
                    break;
                case LOAD:
                    seriesTypes = SeriesDataTypeConstants.LOAD_MAP;
                    break;
                case DANGLING_LINE:
                    seriesTypes = SeriesDataTypeConstants.DANGLING_LINE_MAP;
                    break;
                case VSC_CONVERTER_STATION:
                    seriesTypes = SeriesDataTypeConstants.VSC_CONVERTER_STATION_MAP;
                    break;
                case STATIC_VAR_COMPENSATOR:
                    seriesTypes = SeriesDataTypeConstants.STATIC_VAR_COMPENSATOR_MAP;
                    break;
                case TWO_WINDINGS_TRANSFORMER:
                    seriesTypes = SeriesDataTypeConstants.TWO_WINDINGS_TRANSFORMER_MAP;
                    break;
                default:
                    throw new UnsupportedOperationException("Element type not supported: " + elementType);
            }
            Integer type = seriesTypes.get(seriesName);
            if (type == null) {
                throw new PowsyblException("Series '" + seriesName + "' not found for element type " + elementType);
            }
            return type;
        });
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
                        Switch sw = getSwitchOrThrowsException(id, network);
                        switch (seriesName) {
                            case "open":
                                sw.setOpen(value == 1);
                                break;
                            case "retained":
                                sw.setRetained(value == 1);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for switch elements: " + seriesName);
                        }
                        break;
                    case GENERATOR:
                        Generator g = getGeneratorOrThrowsException(id, network);
                        switch (seriesName) {
                            case "voltage_regulator_on":
                                g.setVoltageRegulatorOn(value == 1);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for generate elements: " + seriesName);
                        }
                        break;
                    case VSC_CONVERTER_STATION:
                        VscConverterStation vsc = getVscOrThrowsException(id, network);
                        switch (seriesName) {
                            case "voltage_regulator_on":
                                vsc.setVoltageRegulatorOn(value == 1);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for vsc elements: " + seriesName);
                        }
                        break;
                    case TWO_WINDINGS_TRANSFORMER:
                        TwoWindingsTransformer twt = getTransformerOrThrowsException(id, network);
                        switch (seriesName) {
                            case "ratio_tap_position":
                                getRatioTapChanger(twt).setTapPosition(value);
                                break;
                            case "phase_tap_position":
                                getPhaseTapChanger(twt).setTapPosition(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for 2 windings transformer elements: " + seriesName);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Updating int or boolean series: type '" + elementType + "' field '" + seriesName + "' not supported");
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
                        Generator g = getGeneratorOrThrowsException(id, network);
                        switch (seriesName) {
                            case "target_p":
                                g.setTargetP(value);
                                break;
                            case "target_q":
                                g.setTargetQ(value);
                                break;
                            case "target_v":
                                g.setTargetV(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for generate elements: " + seriesName);
                        }
                        break;
                    case LOAD:
                        Load l = getLoadOrThrowsException(id, network);
                        switch (seriesName) {
                            case "p0":
                                l.setP0(value);
                                break;
                            case "q0":
                                l.setQ0(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for load elements: " + seriesName);
                        }
                        break;
                    case DANGLING_LINE:
                        DanglingLine dll = getDanglingLineOrThrowsException(id, network);
                        switch (seriesName) {
                            case "p0":
                                dll.setP0(value);
                                break;
                            case "q0":
                                dll.setQ0(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for dangling line elements: " + seriesName);
                        }
                        break;
                    case VSC_CONVERTER_STATION:
                        VscConverterStation vsc = getVscOrThrowsException(id, network);
                        switch (seriesName) {
                            case "voltage_setpoint":
                                vsc.setVoltageSetpoint(value);
                                break;
                            case "reactive_power_setpoint":
                                vsc.setReactivePowerSetpoint(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for vsc elements: " + seriesName);
                        }
                        break;
                    case STATIC_VAR_COMPENSATOR:
                        StaticVarCompensator svc = getSvcOrThrowsException(id, network);
                        switch (seriesName) {
                            case "voltage_setpoint":
                                svc.setVoltageSetpoint(value);
                                break;
                            case "reactive_power_setpoint":
                                svc.setReactivePowerSetpoint(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for svc elements: " + seriesName);
                        }
                        break;
                    case HVDC_LINE:
                        HvdcLine hvdc = getHvdcOrThrowsException(id, network);
                        switch (seriesName) {
                            case "active_power_setpoint":
                                hvdc.setActivePowerSetpoint(value);
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for hvdc line elements: " + seriesName);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Updating double series: type '" + elementType + "' field '" + seriesName + "' not supported");
                }
            }
        });
    }

    @CEntryPoint(name = "updateNetworkElementsWithStringSeries")
    public static void updateNetworkElementsWithStringSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                             ElementType elementType, CCharPointer seriesNamePtr,
                                                             CCharPointerPointer elementIdPtrPtr, CCharPointerPointer valuePtr,
                                                             int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            for (int i = 0; i < elementCount; i++) {
                CCharPointer elementIdPtr = elementIdPtrPtr.read(i);
                String id = CTypeUtil.toString(elementIdPtr);
                String value = CTypeUtil.toString(valuePtr.read(i));
                switch (elementType) {
                    case STATIC_VAR_COMPENSATOR:
                        StaticVarCompensator svc = getSvcOrThrowsException(id, network);
                        switch (seriesName) {
                            case "regulation_mode":
                                svc.setRegulationMode(StaticVarCompensator.RegulationMode.valueOf(value.toUpperCase()));
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for svc elements: " + seriesName);
                        }
                        break;
                    case HVDC_LINE:
                        HvdcLine hvdc = getHvdcOrThrowsException(id, network);
                        switch (seriesName) {
                            case "converters_mode":
                                hvdc.setConvertersMode(HvdcLine.ConvertersMode.valueOf(value.toUpperCase()));
                                break;
                            default:
                                throw new UnsupportedOperationException("Series name not supported for hvdc elements: " + seriesName);
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Updating string series: type '" + elementType + "' field '" + seriesName + "' not supported");
                }
            }
        });
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().destroy(objectHandle));
    }

    private static TwoWindingsTransformer getTransformerOrThrowsException(String id, Network network) {
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
        if (twt == null) {
            throw new PowsyblException("Two windings transformer '" + id + "' not found");
        }
        return twt;
    }

    private static RatioTapChanger getRatioTapChanger(TwoWindingsTransformer twt) {
        RatioTapChanger rtc = twt.getRatioTapChanger();
        if (rtc == null) {
            throw new PowsyblException("Two windings transformer '" + twt.getId() + "' does not have a ratio tap changer");
        }
        return rtc;
    }

    private static PhaseTapChanger getPhaseTapChanger(TwoWindingsTransformer twt) {
        PhaseTapChanger ptc = twt.getPhaseTapChanger();
        if (ptc == null) {
            throw new PowsyblException("Two windings transformer '" + twt.getId() + "' does not have a phase tap changer");
        }
        return ptc;
    }

    private static Generator getGeneratorOrThrowsException(String id, Network network) {
        Generator g = network.getGenerator(id);
        if (g == null) {
            throw new PowsyblException("Generator '" + id + "' not found");
        }
        return g;
    }

    private static Switch getSwitchOrThrowsException(String id, Network network) {
        Switch sw = network.getSwitch(id);
        if (sw == null) {
            throw new PowsyblException("Switch '" + id + "' not found");
        }
        return sw;
    }

    private static Load getLoadOrThrowsException(String id, Network network) {
        Load load = network.getLoad(id);
        if (load == null) {
            throw new PowsyblException("Load '" + id + "' not found");
        }
        return load;
    }

    private static DanglingLine getDanglingLineOrThrowsException(String id, Network network) {
        DanglingLine l = network.getDanglingLine(id);
        if (l == null) {
            throw new PowsyblException("DanglingLine '" + id + "' not found");
        }
        return l;
    }

    private static VscConverterStation getVscOrThrowsException(String id, Network network) {
        VscConverterStation vsc = network.getVscConverterStation(id);
        if (vsc == null) {
            throw new PowsyblException("VscConverterStation '" + id + "' not found");
        }
        return vsc;
    }

    private static StaticVarCompensator getSvcOrThrowsException(String id, Network network) {
        StaticVarCompensator svc = network.getStaticVarCompensator(id);
        if (svc == null) {
            throw new PowsyblException("StaticVarCompensator '" + id + "' not found");
        }
        return svc;
    }

    private static HvdcLine getHvdcOrThrowsException(String id, Network network) {
        HvdcLine hvdc = network.getHvdcLine(id);
        if (hvdc == null) {
            throw new PowsyblException("HvdcLine '" + id + "' not found");
        }
        return hvdc;
    }
}
