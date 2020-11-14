/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.tools.Version;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
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

    @CEntryPoint(name = "runLoadFlow")
    public static ObjectHandle runLoadFlow(IsolateThread thread, ObjectHandle networkHandle) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        LoadFlowResult result = LoadFlow.run(network);
        System.out.println(result.getMetrics());
        return ObjectHandles.getGlobal().create(result);
    }

    @CEntryPoint(name = "isLoadFlowResultOk")
    public static boolean isLoadFlowResultOk(IsolateThread thread, ObjectHandle loadFlowResultHandle) {
        LoadFlowResult loadFlowResult = ObjectHandles.getGlobal().get(loadFlowResultHandle);
        return loadFlowResult.isOk();
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle) {
        ObjectHandles.getGlobal().destroy(objectHandle);
    }
}
