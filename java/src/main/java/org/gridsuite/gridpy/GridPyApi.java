/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.SmartVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.library.ResourcesComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DefaultSVGWriter;
import com.powsybl.sld.util.TopologicalStyleProvider;
import com.powsybl.tools.Version;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@CContext(Directives.class)
public final class GridPyApi {

    private GridPyApi() {
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

    @CStruct("array")
    interface ArrayPointer<T extends PointerBase> extends PointerBase {

        @CField("ptr")
        T getPtr();

        @CField("ptr")
        void setPtr(T ptr);

        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);
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

    @CStruct("load_flow_component_result")
    interface LoadFlowComponentResultPointer extends PointerBase {

        @CField("component_num")
        int geComponentNum();

        @CField("component_num")
        void setComponentNum(int componentNum);

        @CField("status")
        int getStatus();

        @CField("status")
        void setStatus(int status);

        @CField("iteration_count")
        int getIterationCount();

        @CField("iteration_count")
        void setIterationCount(int iterationCount);

        @CField("slack_bus_id")
        CCharPointer getSlackBusId();

        @CField("slack_bus_id")
        void setSlackBusId(CCharPointer slackBusId);

        @CField("slack_bus_active_power_mismatch")
        double getSlackBusActivePowerMismatch();

        @CField("slack_bus_active_power_mismatch")
        void setSlackBusActivePowerMismatch(double slackBusActivePowerMismatch);

        LoadFlowComponentResultPointer addressOf(int index);
    }

    @CStruct("load_flow_parameters")
    interface LoadFlowParametersPointer extends PointerBase {

        @CField("voltage_init_mode")
        int getVoltageInitMode();

        @CField("voltage_init_mode")
        void setVoltageInitMode(int voltageInitMode);

        @CField("transformer_voltage_control_on")
        boolean isTransformerVoltageControlOn();

        @CField("transformer_voltage_control_on")
        void setTransformerVoltageControlOn(boolean transformerVoltageControlOn);

        @CField("no_generator_reactive_limits")
        boolean isNoGeneratorReactiveLimits();

        @CField("no_generator_reactive_limits")
        void setNoGeneratorReactiveLimits(boolean noGeneratorReactiveLimits);

        @CField("phase_shifter_regulation_on")
        boolean isPhaseShifterRegulationOn();

        @CField("phase_shifter_regulation_on")
        void setPhaseShifterRegulationOn(boolean phaseShifterRegulationOn);

        @CField("twt_split_shunt_admittance")
        boolean isTwtSplitShuntAdmittance();

        @CField("twt_split_shunt_admittance")
        void setTwtSplitShuntAdmittance(boolean twtSplitShuntAdmittance);

        @CField("simul_shunt")
        boolean isSimulShunt();

        @CField("simul_shunt")
        void setSimulShunt(boolean simulShunt);

        @CField("read_slack_bus")
        boolean isReadSlackBus();

        @CField("read_slack_bus")
        void setReadSlackBus(boolean readSlackBus);

        @CField("write_slack_bus")
        boolean isWriteSlackBus();

        @CField("write_slack_bus")
        void setWriteSlackBus(boolean writeSlackBus);

        @CField("distributed_slack")
        boolean isDistributedSlack();

        @CField("distributed_slack")
        void setDistributedSlack(boolean distributedSlack);

        @CField("balance_type")
        int getBalanceType();

        @CField("balance_type")
        void setBalanceType(int balanceType);
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

    @CStruct("bus")
    interface BusPointer extends PointerBase {

        @CField("id")
        CCharPointer geId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("v_magnitude")
        double getVoltageMagnitude();

        @CField("v_magnitude")
        void setVoltageMagnitude(double voltageMagnitude);

        @CField("v_angle")
        double getVoltageAngle();

        @CField("v_angle")
        void setVoltageAngle(double voltageAngle);

        BusPointer addressOf(int index);
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
        Switch sw = network.getSwitch(idStr);
        if (sw == null) {
            throw new PowsyblException("Switch '" + idStr + "' not found");
        }
        if (open && !sw.isOpen()) {
            sw.setOpen(true);
            return true;
        } else if (!open && sw.isOpen()) {
            sw.setOpen(false);
            return true;
        }
        return false;
    }

    @CEntryPoint(name = "updateConnectableStatus")
    public static boolean updateConnectableStatus(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean connected) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        String idStr = CTypeConversion.toJavaString(id);
        Identifiable<?> equipment = network.getIdentifiable(idStr);
        if (equipment == null) {
            throw new PowsyblException("Equipment '" + idStr + "' not found");
        }
        if (!(equipment instanceof Connectable)) {
            throw new PowsyblException("Equipment '" + idStr + "' is not a connectable");
        }
        if (equipment instanceof Injection) {
            Injection<?> injection = (Injection<?>) equipment;
            if (connected) {
                return injection.getTerminal().connect();
            } else {
                return injection.getTerminal().disconnect();
            }
        } else if (equipment instanceof Branch) {
            Branch<?> branch = (Branch<?>) equipment;
            if (connected) {
                boolean done1 = branch.getTerminal1().connect();
                boolean done2 = branch.getTerminal2().connect();
                return done1 || done2;
            } else {
                boolean done1 = branch.getTerminal1().disconnect();
                boolean done2 = branch.getTerminal2().disconnect();
                return done1 || done2;
            }
        }
        return false;
    }

    @CEnum("element_type")
    enum ElementType {
        LINE,
        TWO_WINDINGS_TRANSFORMER,
        GENERATOR;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ElementType fromCValue(int value);
    }

    private static boolean isInMainCc(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null && bus.getConnectedComponent().getNum() == ComponentConstants.MAIN_NUM;
    }

    @CEntryPoint(name = "getNetworkElementsIds")
    public static ArrayPointer<CCharPointerPointer> getNetworkElementsIds(IsolateThread thread, ObjectHandle networkHandle, ElementType elementType,
                                                                          double nominalVoltage, boolean mainCc) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        List<String> elementsIds;
        switch (elementType) {
            case LINE:
                elementsIds = network.getLineStream()
                        .filter(l -> Double.isNaN(nominalVoltage) || l.getTerminal1().getVoltageLevel().getNominalV() == nominalVoltage)
                        .filter(l -> !mainCc || (isInMainCc(l.getTerminal1()) && isInMainCc(l.getTerminal2())))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case TWO_WINDINGS_TRANSFORMER:
                elementsIds = network.getTwoWindingsTransformerStream()
                        .filter(twt -> Double.isNaN(nominalVoltage)
                                || twt.getTerminal1().getVoltageLevel().getNominalV() == nominalVoltage
                                || twt.getTerminal2().getVoltageLevel().getNominalV() == nominalVoltage)
                        .filter(l -> !mainCc || (isInMainCc(l.getTerminal1()) && isInMainCc(l.getTerminal2())))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case GENERATOR:
                elementsIds = network.getGeneratorStream()
                        .filter(g -> Double.isNaN(nominalVoltage) || g.getTerminal().getVoltageLevel().getNominalV() == nominalVoltage)
                        .filter(g -> !mainCc || isInMainCc(g.getTerminal()))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            default:
                throw new PowsyblException("Unsupported element type:" + elementType);
        }
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
        ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        if (network.getVoltageLevel(containerIdStr) != null) {
            VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
            VoltageLevelDiagram voltageLevelDiagram = VoltageLevelDiagram.build(new NetworkGraphBuilder(network), containerIdStr, voltageLevelLayoutFactory, false);
            LayoutParameters layoutParameters = new LayoutParameters()
                    .setAdaptCellHeightToContent(true);
            voltageLevelDiagram.writeSvg("",
                    new DefaultSVGWriter(componentLibrary, layoutParameters),
                    new DefaultDiagramLabelProvider(network, componentLibrary, layoutParameters),
                    new TopologicalStyleProvider(network),
                    Paths.get(svgFileStr));
        } else {
            throw new PowsyblException("Container '" + containerIdStr + "' not found");
        }
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

    @CStruct("limit_violation")
    interface LimitViolationPointer extends PointerBase {

        @CField("subject_id")
        CCharPointer getSubjectId();

        @CField("subject_id")
        void setSubjectId(CCharPointer subjectId);

        @CField("subject_name")
        CCharPointer getSubjectName();

        @CField("subject_name")
        void setSubjectName(CCharPointer subjectName);

        @CField("limit_type")
        int getLimitType();

        @CField("limit_type")
        void setLimitType(int limitType);

        @CField("limit")
        double getLimit();

        @CField("limit")
        void setLimit(double limit);

        @CField("limit_name")
        CCharPointer getLimitName();

        @CField("limit_name")
        void setLimitName(CCharPointer limitName);

        @CField("acceptable_duration")
        int getAcceptableDuration();

        @CField("acceptable_duration")
        void setAcceptableDuration(int acceptableDuration);

        @CField("limit_reduction")
        float getLimitReduction();

        @CField("limit_reduction")
        void setLimitReduction(float limitReduction);

        @CField("value")
        double getValue();

        @CField("value")
        void setValue(double value);

        @CField("side")
        int getSide();

        @CField("side")
        void setSide(int side);

        LimitViolationPointer addressOf(int index);
    }

    @CStruct("contingency_result")
    interface ContingencyResultPointer extends PointerBase {

        @CField("contingency_id")
        CCharPointer getContingencyId();

        @CField("contingency_id")
        void setContingencyId(CCharPointer contingencyId);

        @CField("status")
        int getStatus();

        @CField("status")
        void setStatus(int status);

        @CFieldAddress("limit_violations")
        ArrayPointer<LimitViolationPointer> limitViolations();

        ContingencyResultPointer addressOf(int index);
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

    @CStruct("matrix")
    interface MatrixPointer extends PointerBase {

        @CField("values")
        CDoublePointer getValues();

        @CField("values")
        void setValues(CDoublePointer values);

        @CField("row_count")
        int getRowCount();

        @CField("row_count")
        void setRowCount(int rowCount);

        @CField("column_count")
        int getColumnCount();

        @CField("column_count")
        void setColumnCount(int columnCount);
    }

    @CEntryPoint(name = "getSensitivityMatrix")
    public static MatrixPointer getSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                     CCharPointer contingencyIdPtr) {
        SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
        String contingencyId = CTypeConversion.toJavaString(contingencyIdPtr);
        Collection<SensitivityValue> sensitivityValues = resultContext.getSensitivityValues(contingencyId);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(resultContext.getRowCount() * resultContext.getColumnCount() * SizeOf.get(CDoublePointer.class));
        if (sensitivityValues != null) {
            for (SensitivityValue sensitivityValue : sensitivityValues) {
                IndexedSensitivityFactor indexedFactor = (IndexedSensitivityFactor) sensitivityValue.getFactor();
                valuePtr.addressOf(indexedFactor.getRow() * resultContext.getColumnCount() + indexedFactor.getColumn()).write(sensitivityValue.getValue());
            }
        }
        MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(MatrixPointer.class));
        matrixPtr.setRowCount(resultContext.getRowCount());
        matrixPtr.setColumnCount(resultContext.getColumnCount());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
