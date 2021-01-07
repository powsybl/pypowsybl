/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.sa.OpenSecurityAnalysisFactory;
import com.powsybl.security.*;
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
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@CContext(Directives.class)
public final class GridPyApi {

    private GridPyApi() {
    }

    @CEntryPoint(name = "printVersion")
    public static void printVersion(IsolateThread thread) {
        System.out.println(Version.getTableString());
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

    @CEntryPoint(name = "runLoadFlow")
    public static ArrayPointer<LoadFlowComponentResultPointer> runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean distributedSlack, boolean dc) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowParameters parameters = LoadFlowParameters.load()
                .setDistributedSlack(distributedSlack)
                .setDc(dc);
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

    private static class SecurityAnalysisContext {

        private final Map<String, List<String>> elementIdsByContingencyId = new HashMap<>();
    }

    @CEntryPoint(name = "createSecurityAnalysis")
    public static ObjectHandle createSecurityAnalysis(IsolateThread thread) {
        return ObjectHandles.getGlobal().create(new SecurityAnalysisContext());
    }

    @CEntryPoint(name = "addContingencyToSecurityAnalysis")
    public static void addContingencyToSecurityAnalysis(IsolateThread thread, ObjectHandle securityAnalysisContextHandle, CCharPointer contingencyIdPtr,
                                                        CCharPointerPointer elementIdPtrPtr, int elementCount) {
        SecurityAnalysisContext securityAnalysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
        String contingencyId = CTypeConversion.toJavaString(contingencyIdPtr);
        List<String> elementIds = new ArrayList<>(elementCount);
        for (int i = 0; i < elementCount; i++) {
            CCharPointer elementIdPtr = elementIdPtrPtr.read(i);
            String elementId = CTypeConversion.toJavaString(elementIdPtr);
            elementIds.add(elementId);
        }
        securityAnalysisContext.elementIdsByContingencyId.put(contingencyId, elementIds);
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
        int geStatus();

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

    private static ContingencyElement createContingencyElement(Network network, String elementId) {
        Identifiable<?> identifiable = network.getIdentifiable(elementId);
        if (identifiable == null) {
            throw new PowsyblException("Element '" + elementId + "' not found");
        }
        if (identifiable instanceof Branch) {
            return new BranchContingency(elementId);
        } else {
            throw new PowsyblException("Element type not supported: " + identifiable.getClass().getSimpleName());
        }
    }

    @CEntryPoint(name = "runSecurityAnalysis")
    public static ArrayPointer<ContingencyResultPointer> runSecurityAnalysis(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                                             ObjectHandle networkHandle) {
        SecurityAnalysisContext securityAnalysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        SecurityAnalysis securityAnalysis = new OpenSecurityAnalysisFactory().create(network, LocalComputationManager.getDefault(), 0);
        SecurityAnalysisParameters securityAnalysisParameters = SecurityAnalysisParameters.load();
        List<Contingency> contingencies = new ArrayList<>(securityAnalysisContext.elementIdsByContingencyId.size());
        for (Map.Entry<String, List<String>> e : securityAnalysisContext.elementIdsByContingencyId.entrySet()) {
            String contingencyId = e.getKey();
            List<String> elementIds = e.getValue();
            List<ContingencyElement> elements = elementIds.stream()
                .map(elementId -> createContingencyElement(network, elementId))
                .collect(Collectors.toList());
            contingencies.add(new Contingency(contingencyId, elements));
        }
        SecurityAnalysisResult result = securityAnalysis
                .run(VariantManagerConstants.INITIAL_VARIANT_ID, securityAnalysisParameters, n -> contingencies)
                .join();
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

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
