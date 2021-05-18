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
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
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
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
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
        String nonNullMessage = Objects.toString(t.getMessage(), "");
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
                .setBalanceType(LoadFlowParameters.BalanceType.values()[loadFlowParametersPtr.getBalanceType()])
                .setDcUseTransformerRatio(loadFlowParametersPtr.isDcUseTransformerRatio())
                .setCountriesToBalance(CTypeUtil.toStringList(loadFlowParametersPtr.getCountriesToBalance(), loadFlowParametersPtr.getCountriesToBalanceCount())
                        .stream().map(Country::valueOf).collect(Collectors.toSet()))
                .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.values()[loadFlowParametersPtr.getConnectedComponentMode()]);
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

    @CEntryPoint(name = "createNetworkElementsSeriesArray")
    public static ArrayPointer<SeriesPointer> createNetworkElementsSeriesArray(IsolateThread thread, ObjectHandle networkHandle,
                                                                               ElementType elementType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, elementType);
            return SeriesArrayHelper.writeToCStruct(builder);
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
                case BATTERY:
                    seriesTypes = SeriesDataTypeConstants.BATTERY_MAP;
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
            IntFunction<String> idGetter = i -> CTypeUtil.toString(elementIdPtrPtr.read(i));
            IntToIntFunction valueGetter = i -> valuePtr.read(i);
            SeriesArrayHelper.updateNetworkElementsWithIntSeries(network, elementType, elementCount, seriesName,
                    idGetter, valueGetter);
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
            IntFunction<String> idGetter = i -> CTypeUtil.toString(elementIdPtrPtr.read(i));
            IntToDoubleFunction valueGetter = i -> valuePtr.read(i);
            SeriesArrayHelper.updateNetworkElementsWithDoubleSeries(network, elementType, elementCount, seriesName,
                    idGetter, valueGetter);
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
            IntFunction<String> idGetter = i -> CTypeUtil.toString(elementIdPtrPtr.read(i));
            IntFunction<String> valueGetter = i -> CTypeUtil.toString(valuePtr.read(i));
            SeriesArrayHelper.updateNetworkElementsWithStringSeries(network, elementType, elementCount, seriesName,
                    idGetter, valueGetter);
        });
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().destroy(objectHandle));
    }

}
