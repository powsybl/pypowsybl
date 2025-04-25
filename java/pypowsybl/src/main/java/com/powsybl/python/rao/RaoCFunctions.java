/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Properties;

import static com.powsybl.python.commons.Util.binaryBufferToBytes;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
@CContext(Directives.class)
public final class RaoCFunctions {

    private RaoCFunctions() {
    }

    @CEntryPoint(name = "createRao")
    public static ObjectHandle createRao(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new RaoContext()));
    }

    @CEntryPoint(name = "createDefaultRaoParameters")
    public static ObjectHandle createDefaultRaoParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new RaoParameters()));
    }

    @CEntryPoint(name = "loadRaoParameters")
    public static ObjectHandle loadRaoParameters(IsolateThread thread, CCharPointer parametersBuffer, int paramersBufferSize, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ByteBuffer bufferParameters = CTypeConversion.asByteBuffer(parametersBuffer, paramersBufferSize);
            InputStream streamedParameters = new ByteArrayInputStream(binaryBufferToBytes(bufferParameters));
            return ObjectHandles.getGlobal().create(JsonRaoParameters.read(streamedParameters));
        });
    }

    @CEntryPoint(name = "serializeRaoParameters")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointer> serializeRaoParameters(IsolateThread thread, ObjectHandle raoParameters,
                                                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoParameters parameters = ObjectHandles.getGlobal().get(raoParameters);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                JsonRaoParameters.write(parameters, output);
                return Util.createByteArray(output.toByteArray());
            } catch (IOException e) {
                throw new PowsyblException("Could not serialize rao parameters : " + e.getMessage());
            }
        });
    }

    @CEntryPoint(name = "setCracBufferedSource")
    public static void setCracBufferedSource(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle, CCharPointer cracBuffer, int cracBufferSize, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);

            ByteBuffer bufferCrac = CTypeConversion.asByteBuffer(cracBuffer, cracBufferSize);
            InputStream streamedCrac = new ByteArrayInputStream(binaryBufferToBytes(bufferCrac));
            try {
                Crac crac = Crac.read("crac.json", streamedCrac, network);
                if (crac != null) {
                    raoContext.setCrac(crac);
                } else {
                    throw new PowsyblException("Error while reading json crac, please enable detailed log for more information.");
                }
            } catch (IOException e) {
                throw new PowsyblException("Cannot read provided crac data : " + e.getMessage());
            }
        });
    }

    @CEntryPoint(name = "setGlskBufferedSource")
    public static void setGlskBufferedSource(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle, CCharPointer glsksBuffer, int glsksBufferSize, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            ByteBuffer bufferGlsks = CTypeConversion.asByteBuffer(glsksBuffer, glsksBufferSize);

            InputStream glsksStream = new ByteArrayInputStream(binaryBufferToBytes(bufferGlsks));
            ZonalData<SensitivityVariableSet> glsks = GlskDocumentImporters.importGlsk(glsksStream)
                .getZonalGlsks(network);
            raoContext.setGlsks(glsks);
        });
    }

    @CEntryPoint(name = "runRao")
    public static void runRao(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle,
                              ObjectHandle raoParametersHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            RaoParameters raoParameters = ObjectHandles.getGlobal().get(raoParametersHandle);
            raoContext.run(network, raoParameters);
        });
    }

    @CEntryPoint(name = "serializeRaoResultsToBuffer")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointer> serializeRaoResultsToBuffer(IsolateThread thread, ObjectHandle raoResultHandle,
        ObjectHandle cracHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoResult raoResult = ObjectHandles.getGlobal().get(raoResultHandle);
            Crac crac = ObjectHandles.getGlobal().get(cracHandle);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Properties properties = new Properties();
                properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
                properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
                raoResult.write("JSON", crac, properties, output);
                return Util.createByteArray(output.toByteArray());
            } catch (IOException e) {
                throw new PowsyblException("Could not serialize rao results : " + e.getMessage());
            }
        });
    }

    @CEntryPoint(name = "getRaoResultStatus")
    public static PyPowsyblApiHeader.RaoComputationStatus getRaoResultStatus(IsolateThread thread, ObjectHandle raoResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
            switch (result.getComputationStatus()) {
                case DEFAULT -> {
                    return PyPowsyblApiHeader.RaoComputationStatus.DEFAULT;
                }
                case FAILURE -> {
                    return PyPowsyblApiHeader.RaoComputationStatus.FAILURE;
                }
                default -> throw new PowsyblException("Unexpected computation status : " + result.getComputationStatus());
            }
        });
    }

    @CEntryPoint(name = "getCrac")
    public static ObjectHandle getCrac(IsolateThread thread, ObjectHandle raoContextHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            return ObjectHandles.getGlobal().create(raoContext.getCrac());
        });
    }

    @CEntryPoint(name = "getRaoResult")
    public static ObjectHandle getRaoResult(IsolateThread thread, ObjectHandle raoContextHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            return ObjectHandles.getGlobal().create(raoContext.getResults());
        });
    }
}
