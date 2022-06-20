/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.glsk;

import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;

import java.time.Instant;
import java.util.List;

import static com.powsybl.python.commons.Util.*;

/**
 * C functions for GLSK processing
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class GlskCFunctions {

    private GlskCFunctions() {
    }

    @CEntryPoint(name = "createGLSKdocument")
    public static ObjectHandle createGLSKdocument(IsolateThread thread, CCharPointer fileNamePtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            GlskDocumentContext importer = new GlskDocumentContext();
            String filename = CTypeUtil.toString(fileNamePtr);
            importer.load(filename);
            return ObjectHandles.getGlobal().create(importer);
        });
    }

    @CEntryPoint(name = "getGLSKinjectionkeys")
    public static ArrayPointer<CCharPointerPointer> getGLSKinjectionkeys(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle importerHandle, CCharPointer countryPtr, long instant, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            GlskDocumentContext importer = ObjectHandles.getGlobal().get(importerHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String country = CTypeUtil.toString(countryPtr);
            return createCharPtrArray(importer.getInjectionIdForCountry(network, country, Instant.ofEpochSecond(instant)));
        });
    }

    @CEntryPoint(name = "getGLSKcountries")
    public static ArrayPointer<CCharPointerPointer> getGLSKcountries(IsolateThread thread, ObjectHandle importerHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            GlskDocumentContext importer = ObjectHandles.getGlobal().get(importerHandle);
            return createCharPtrArray(importer.getCountries());
        });
    }

    @CEntryPoint(name = "getInjectionFactor")
    public static ArrayPointer<CDoublePointer> getInjectionFactor(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle importerHandle, CCharPointer countryPtr, long instant, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            GlskDocumentContext importer = ObjectHandles.getGlobal().get(importerHandle);
            String country = CTypeUtil.toString(countryPtr);
            List<Double> values = importer.getInjectionFactorForCountryTimeinterval(network, country, Instant.ofEpochSecond(instant));
            return createDoubleArray(values);
        });
    }

    @CEntryPoint(name = "getInjectionFactorStartTimestamp")
    public static long getInjectionFactorStartTimestamp(IsolateThread thread, ObjectHandle importerHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            GlskDocumentContext importer = ObjectHandles.getGlobal().get(importerHandle);
            return importer.getInjectionFactorStart().getEpochSecond();
        });
    }

    @CEntryPoint(name = "getInjectionFactorEndTimestamp")
    public static long getInjectionFactorEndTimestamp(IsolateThread thread, ObjectHandle importerHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            GlskDocumentContext importer = ObjectHandles.getGlobal().get(importerHandle);
            return importer.getInjectionFactorEnd().getEpochSecond();
        });
    }
}
