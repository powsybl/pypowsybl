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
import com.powsybl.dataframe.*;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.iidm.parameters.Parameter;
import com.powsybl.iidm.parameters.ParameterType;
import com.powsybl.iidm.reducer.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.sensi.SensitivityVariableSet;
import com.powsybl.openloadflow.sensi.WeightedSensitivityVariable;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.results.PostContingencyResult;
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

    private static final DataframeMapper<Importer> PARAMETERS_MAPPER = new DataframeMapperBuilder<Importer, Parameter>()
            .itemsProvider(Importer::getParameters)
            .stringsIndex("name", Parameter::getName)
            .strings("description", Parameter::getDescription)
            .enums("type", ParameterType.class, Parameter::getType)
            .strings("default", p -> Objects.toString(p.getDefaultValue(), ""))
            .build();

    @CEntryPoint(name = "createImporterParametersSeriesArray")
    static ArrayPointer<SeriesPointer> createImporterParametersSeriesArray(IsolateThread thread, CCharPointer formatPtr,
                                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String format = CTypeUtil.toString(formatPtr);
            Importer importer = Importers.getImporter(format);
            if (importer == null) {
                throw new PowsyblException("Format '" + format + "' not supported");
            }
            return CDataframeHandler.createCDataframe(PARAMETERS_MAPPER, importer);
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
        setSecurityAnalysisResultPointer(contingencyPtr, "", result.getPreContingencyResult().getLimitViolationsResult());
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
                List<String> injectionsIds = CTypeUtil.toStringList(zonePtrI.getInjectionsIds(), zonePtrI.getLength());
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

    @CEntryPoint(name = "setBranchFlowFactorMatrix")
    public static void setBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = CTypeUtil.toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = CTypeUtil.toStringList(variableIdPtrPtr, variableIdCount);
            analysisContext.setBranchFlowFactorMatrix(branchesIds, variablesIds);
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
            NetworkDataframeMapper mapper = NetworkDataframes.getDataframeMapper(convert(elementType));
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return CDataframeHandler.createCDataframe(mapper, network);
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
            SeriesDataType type = NetworkDataframes.getDataframeMapper(convert(elementType))
                .getSeriesMetadata(seriesName)
                .getType();
            return convert(type);
        });
    }

    private static int convert(SeriesDataType type) {
        switch (type) {
            case STRING:
                return CDataframeHandler.STRING_SERIES_TYPE;
            case DOUBLE:
                return CDataframeHandler.DOUBLE_SERIES_TYPE;
            case INT:
                return CDataframeHandler.INT_SERIES_TYPE;
            case BOOLEAN:
                return CDataframeHandler.BOOLEAN_SERIES_TYPE;
            default:
                throw new IllegalStateException("Unexpected series type: " + type);
        }
    }

    @CEntryPoint(name = "updateNetworkElementsWithIntSeries")
    public static void updateNetworkElementsWithIntSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                          ElementType elementType, CCharPointer seriesNamePtr,
                                                          CCharPointerPointer elementIdPtrPtr, CIntPointer valuePtr,
                                                          int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateIntSeries(network, seriesName, createIntSeries(elementIdPtrPtr, valuePtr, elementCount));
        });
    }

    private static IntIndexedSeries createIntSeries(CCharPointerPointer elementIdPtrPtr, CIntPointer valuePtr, int elementCount) {
        return new IntIndexedSeries() {
            @Override
            public int getSize() {
                return elementCount;
            }

            @Override
            public String getId(int index) {
                return CTypeUtil.toString(elementIdPtrPtr.read(index));
            }

            @Override
            public int getValue(int index) {
                return valuePtr.read(index);
            }
        };
    }

    @CEntryPoint(name = "updateNetworkElementsWithDoubleSeries")
    public static void updateNetworkElementsWithDoubleSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                             ElementType elementType, CCharPointer seriesNamePtr,
                                                             CCharPointerPointer elementIdPtrPtr, CDoublePointer valuePtr,
                                                             int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateDoubleSeries(network, seriesName, createDoubleSeries(elementIdPtrPtr, valuePtr, elementCount));
        });
    }

    private static DoubleIndexedSeries createDoubleSeries(CCharPointerPointer elementIdPtrPtr, CDoublePointer valuePtr, int elementCount) {
        return new DoubleIndexedSeries() {
            @Override
            public int getSize() {
                return elementCount;
            }

            @Override
            public String getId(int index) {
                return CTypeUtil.toString(elementIdPtrPtr.read(index));
            }

            @Override
            public double getValue(int index) {
                return valuePtr.read(index);
            }
        };
    }

    @CEntryPoint(name = "updateNetworkElementsWithStringSeries")
    public static void updateNetworkElementsWithStringSeries(IsolateThread thread, ObjectHandle networkHandle,
                                                             ElementType elementType, CCharPointer seriesNamePtr,
                                                             CCharPointerPointer elementIdPtrPtr, CCharPointerPointer valuePtr,
                                                             int elementCount, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String seriesName = CTypeUtil.toString(seriesNamePtr);
            NetworkDataframes.getDataframeMapper(convert(elementType))
                .updateStringSeries(network, seriesName, createStringSeries(elementIdPtrPtr, valuePtr, elementCount));
        });
    }

    private static IndexedSeries<String> createStringSeries(CCharPointerPointer elementIdPtrPtr, CCharPointerPointer valuePtr, int elementCount) {
        return new IndexedSeries<>() {
            @Override
            public int getSize() {
                return elementCount;
            }

            @Override
            public String getId(int index) {
                return CTypeUtil.toString(elementIdPtrPtr.read(index));
            }

            @Override
            public String getValue(int index) {
                return CTypeUtil.toString(valuePtr.read(index));
            }
        };
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().destroy(objectHandle));
    }

    private static ElementType convert(DataframeElementType type) {
        switch (type) {
            case BUS:
                return ElementType.BUS;
            case LINE:
                return ElementType.LINE;
            case TWO_WINDINGS_TRANSFORMER:
                return ElementType.TWO_WINDINGS_TRANSFORMER;
            case THREE_WINDINGS_TRANSFORMER:
                return ElementType.THREE_WINDINGS_TRANSFORMER;
            case GENERATOR:
                return ElementType.GENERATOR;
            case LOAD:
                return ElementType.LOAD;
            case BATTERY:
                return ElementType.BATTERY;
            case SHUNT_COMPENSATOR:
                return ElementType.SHUNT_COMPENSATOR;
            case DANGLING_LINE:
                return ElementType.DANGLING_LINE;
            case LCC_CONVERTER_STATION:
                return ElementType.LCC_CONVERTER_STATION;
            case VSC_CONVERTER_STATION:
                return ElementType.VSC_CONVERTER_STATION;
            case STATIC_VAR_COMPENSATOR:
                return ElementType.STATIC_VAR_COMPENSATOR;
            case SWITCH:
                return ElementType.SWITCH;
            case VOLTAGE_LEVEL:
                return ElementType.VOLTAGE_LEVEL;
            case SUBSTATION:
                return ElementType.SUBSTATION;
            case BUSBAR_SECTION:
                return ElementType.BUSBAR_SECTION;
            case HVDC_LINE:
                return ElementType.HVDC_LINE;
            case RATIO_TAP_CHANGER_STEP:
                return ElementType.RATIO_TAP_CHANGER_STEP;
            case PHASE_TAP_CHANGER_STEP:
                return ElementType.PHASE_TAP_CHANGER_STEP;
            case RATIO_TAP_CHANGER:
                return ElementType.RATIO_TAP_CHANGER;
            case PHASE_TAP_CHANGER:
                return ElementType.PHASE_TAP_CHANGER;
            case REACTIVE_CAPABILITY_CURVE_POINT:
                return ElementType.REACTIVE_CAPABILITY_CURVE_POINT;
            default:
                throw new PowsyblException("Unknown element type : " + type);
        }
    }

    private static DataframeElementType convert(ElementType type) {
        switch (type) {
            case BUS:
                return DataframeElementType.BUS;
            case LINE:
                return DataframeElementType.LINE;
            case TWO_WINDINGS_TRANSFORMER:
                return DataframeElementType.TWO_WINDINGS_TRANSFORMER;
            case THREE_WINDINGS_TRANSFORMER:
                return DataframeElementType.THREE_WINDINGS_TRANSFORMER;
            case GENERATOR:
                return DataframeElementType.GENERATOR;
            case LOAD:
                return DataframeElementType.LOAD;
            case BATTERY:
                return DataframeElementType.BATTERY;
            case SHUNT_COMPENSATOR:
                return DataframeElementType.SHUNT_COMPENSATOR;
            case DANGLING_LINE:
                return DataframeElementType.DANGLING_LINE;
            case LCC_CONVERTER_STATION:
                return DataframeElementType.LCC_CONVERTER_STATION;
            case VSC_CONVERTER_STATION:
                return DataframeElementType.VSC_CONVERTER_STATION;
            case STATIC_VAR_COMPENSATOR:
                return DataframeElementType.STATIC_VAR_COMPENSATOR;
            case SWITCH:
                return DataframeElementType.SWITCH;
            case VOLTAGE_LEVEL:
                return DataframeElementType.VOLTAGE_LEVEL;
            case SUBSTATION:
                return DataframeElementType.SUBSTATION;
            case BUSBAR_SECTION:
                return DataframeElementType.BUSBAR_SECTION;
            case HVDC_LINE:
                return DataframeElementType.HVDC_LINE;
            case RATIO_TAP_CHANGER_STEP:
                return DataframeElementType.RATIO_TAP_CHANGER_STEP;
            case PHASE_TAP_CHANGER_STEP:
                return DataframeElementType.PHASE_TAP_CHANGER_STEP;
            case RATIO_TAP_CHANGER:
                return DataframeElementType.RATIO_TAP_CHANGER;
            case PHASE_TAP_CHANGER:
                return DataframeElementType.PHASE_TAP_CHANGER;
            case REACTIVE_CAPABILITY_CURVE_POINT:
                return DataframeElementType.REACTIVE_CAPABILITY_CURVE_POINT;
            default:
                throw new PowsyblException("Unknown element type : " + type);
        }
    }

    @CEntryPoint(name = "getWorkingVariantId")
    public static CCharPointer getWorkingVariantId(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return CTypeUtil.toCharPtr(network.getVariantManager().getWorkingVariantId());
        });

    }

    @CEntryPoint(name = "cloneVariant")
    public static void cloneVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer src, CCharPointer variant, boolean mayOverwrite, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().cloneVariant(CTypeUtil.toString(src), CTypeUtil.toString(variant), mayOverwrite);
        });
    }

    @CEntryPoint(name = "setWorkingVariant")
    public static void setWorkingVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer variant, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().setWorkingVariant(CTypeUtil.toString(variant));
        });
    }

    @CEntryPoint(name = "removeVariant")
    public static void removeVariant(IsolateThread thread, ObjectHandle networkHandle, CCharPointer variant, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            network.getVariantManager().removeVariant(CTypeUtil.toString(variant));
        });
    }

    @CEntryPoint(name = "getVariantsIds")
    public static ArrayPointer<CCharPointerPointer> getVariantsIds(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            return createCharPtrArray(List.copyOf(network.getVariantManager().getVariantIds()));
        });
    }
}
