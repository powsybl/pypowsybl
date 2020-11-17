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
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
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

import java.util.Collections;
import java.util.List;

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
        LoadFlowResultPointer resultPointer = UnmanagedMemory.calloc(SizeOf.get(LoadFlowResultPointer.class));
        resultPointer.setOk(result.isOk());
        return resultPointer;
    }

    @CEntryPoint(name = "runLoadFlow")
    public static LoadFlowResultPointer runLoadFlow(IsolateThread thread, ObjectHandle networkHandle) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowResult result = LoadFlow.run(network);
        LoadFlowResultPointer resultPointer = createPointer(result);
        System.out.println(result.getMetrics());
        return resultPointer;
    }

    @CEntryPoint(name = "freeLoadFlowResultPointer")
    public static void freeLoadFlowResultPointer(IsolateThread thread, LoadFlowResultPointer resultPointer) {
        UnmanagedMemory.free(resultPointer);
    }

    @CEntryPoint(name = "updateSwitchPosition")
    public static void updateSwitchPosition(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean open) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        String idStr = CTypeConversion.toJavaString(id);
        Switch sw = network.getSwitch(idStr);
        if (sw == null) {
            throw new PowsyblException("Switch '" + idStr + "' not found");
        }
        sw.setOpen(open);
    }

    @CEntryPoint(name = "updateConnectableStatus")
    public static void updateConnectableStatus(IsolateThread thread, ObjectHandle networkHandle, CCharPointer id, boolean connected) {
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
                injection.getTerminal().connect();
            } else {
                injection.getTerminal().disconnect();
            }
        } else if (equipment instanceof Branch) {
            Branch<?> branch = (Branch<?>) equipment;
            if (connected) {
                branch.getTerminal1().connect();
                branch.getTerminal2().connect();
            } else {
                branch.getTerminal1().disconnect();
                branch.getTerminal2().disconnect();
            }
        }
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
