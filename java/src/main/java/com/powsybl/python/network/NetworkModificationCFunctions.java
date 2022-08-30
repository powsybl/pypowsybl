/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.python.commons.Util.convert;
import static com.powsybl.python.commons.Util.doCatch;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
@CContext(Directives.class)
public final class NetworkModificationCFunctions {

    private NetworkModificationCFunctions() {
    }

    @CEntryPoint(name = "createFeederBay")
    public static void createFeederBay(IsolateThread thread, ObjectHandle networkHandle, boolean throwException,
                                       ObjectHandle reporterHandle, PyPowsyblApiHeader.DataframeArrayPointer cDataframes,
                                       PyPowsyblApiHeader.ElementType elementType,
                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {

        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReporterModel reporter = ObjectHandles.getGlobal().get(reporterHandle);
            DataframeElementType type = convert(elementType);
            List<UpdatingDataframe> dataframes = new ArrayList<>();
            for (int i = 0; i < cDataframes.getDataframesCount(); i++) {
                dataframes.add(createDataframe(cDataframes.getDataframes().addressOf(i)));
            }
            NetworkElementAdders.addElementsWithBay(type, network, dataframes, throwException, reporter);
        });
    }
}
