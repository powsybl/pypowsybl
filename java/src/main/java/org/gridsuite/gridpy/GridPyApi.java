/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.oracle.svm.core.c.ProjectHeaderFile;
import com.powsybl.commons.PowsyblException;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
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
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@CContext(GridPyApi.Directives.class)
public final class GridPyApi {

    static class Directives implements CContext.Directives {

        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve("org.gridsuite.gridpy", "gridpy-api.h"));
        }
    }

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

    @CEntryPoint(name = "loadNetwork")
    public static ObjectHandle loadNetwork(IsolateThread thread, CCharPointer file) {
        String fileStr = CTypeConversion.toJavaString(file);
        Network network = Importers.loadNetwork(fileStr);
        return ObjectHandles.getGlobal().create(network);
    }

    @CStruct("load_flow_result")
    interface LoadFlowResultPointer extends PointerBase {

        @CField("ok")
        boolean isOk();

        @CField("ok")
        void setOk(boolean ok);
    }

    static LoadFlowResultPointer createPointer(LoadFlowResult result) {
        LoadFlowResultPointer resultPtr = UnmanagedMemory.calloc(SizeOf.get(LoadFlowResultPointer.class));
        resultPtr.setOk(result.isOk());
        return resultPtr;
    }

    @CEntryPoint(name = "runLoadFlow")
    public static LoadFlowResultPointer runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean distributedSlack,
                                                    boolean dc) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowParameters parameters = LoadFlowParameters.load()
                .setDistributedSlack(distributedSlack)
                .setDc(dc);
        LoadFlowResult result = LoadFlow.run(network, parameters);
        LoadFlowResultPointer resultPtr = createPointer(result);
        System.out.println(result.getMetrics());
        return resultPtr;
    }

    @CEntryPoint(name = "freeLoadFlowResultPointer")
    public static void freeLoadFlowResultPointer(IsolateThread thread, LoadFlowResultPointer resultPointer) {
        UnmanagedMemory.free(resultPointer);
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

    @CStruct("bus_array")
    interface BusArrayPointer extends PointerBase {

        @CField("ptr")
        BusPointer getPtr();

        @CField("ptr")
        void setPtr(BusPointer ptr);

        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);
    }

    @CEntryPoint(name = "getBusArray")
    public static BusArrayPointer getBusArray(IsolateThread thread, ObjectHandle networkHandle, boolean busBreakerView) {
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
        BusArrayPointer busArrayPtr = UnmanagedMemory.calloc(SizeOf.get(BusArrayPointer.class));
        busArrayPtr.setPtr(busesPtr);
        busArrayPtr.setLength(buses.size());
        return busArrayPtr;
    }

    @CEntryPoint(name = "freeBusArray")
    public static void freeBusArray(IsolateThread thread, BusArrayPointer busArrayPointer) {
        // don't need to free char* from id field as it is done by python
        UnmanagedMemory.free(busArrayPointer.getPtr());
        UnmanagedMemory.free(busArrayPointer);
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

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
