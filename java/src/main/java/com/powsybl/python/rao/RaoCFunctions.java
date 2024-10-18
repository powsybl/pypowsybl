package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.raoresultjson.RaoResultJsonExporter;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.powsybl.python.commons.Util.binaryBufferToBytes;
import static com.powsybl.python.commons.Util.doCatch;

@CContext(Directives.class)
public final class RaoCFunctions {

    private RaoCFunctions() {
    }

    @CEntryPoint(name = "createRao")
    public static ObjectHandle createRao(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new RaoContext()));
    }

    @CEntryPoint(name = "runRaoFromStreamedInput")
    public static void runRaoFromStreamedInput(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle,
                              CCharPointer cracBuffer, int cracBufferSize,
                              CCharPointer parametersBuffer, int paramersBufferSize,
                              CCharPointer glsksBuffer, int glsksBufferSize,
                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            System.out.println("Running rao with streamed inputs !!");
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);

            ByteBuffer bufferCrac = CTypeConversion.asByteBuffer(cracBuffer, cracBufferSize);
            ByteBuffer bufferParameters = CTypeConversion.asByteBuffer(parametersBuffer, paramersBufferSize);
            ByteBuffer bufferGlsks = CTypeConversion.asByteBuffer(glsksBuffer, glsksBufferSize);

            InputStream streamedCrac = new ByteArrayInputStream(binaryBufferToBytes(bufferCrac));
            InputStream streamedParameters = new ByteArrayInputStream(binaryBufferToBytes(bufferParameters));
            InputStream streamedGlsks = new ByteArrayInputStream(binaryBufferToBytes(bufferGlsks));

            raoContext.run(network, streamedCrac, streamedParameters, streamedGlsks);
        });
    }

    @CEntryPoint(name = "runRao")
    public static void runRao(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle,
                              CCharPointer cracFile, CCharPointer parametersFile, CCharPointer glsksFile,
                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {

        doCatch(exceptionHandlerPtr, () -> {
            System.out.println("Running rao with path inputs !!");
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);

            String cracStr = CTypeUtil.toString(cracFile);
            String parametersStr = CTypeUtil.toString(parametersFile);
            String glsksStr = CTypeUtil.toString(glsksFile);

            InputStream cracStream = createStreamOrThrow(Path.of(cracStr),
                "Cannot read provided crac data");
            InputStream parametersStream = createStreamOrThrow(Path.of(parametersStr),
                "Cannot read provided rao parameters data");
            InputStream glsksStream = createStreamOrThrow(Path.of(glsksStr),
                "Cannot read provided glsks data");

            raoContext.run(network, cracStream, parametersStream, glsksStream);
        });
    }

    @CEntryPoint(name = "serializeRaoResults")
    public static void serializeRaoResults(IsolateThread thread, ObjectHandle raoContextHandle, CCharPointer resultsFilePath, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String filePath = CTypeUtil.toString(resultsFilePath);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            try (OutputStream output = Files.newOutputStream(Path.of(filePath))) {
                new RaoResultJsonExporter().exportData(
                    raoContext.getResults(), raoContext.getCrac(), Set.of(Unit.MEGAWATT), output);
            } catch (IOException e) {
                throw new PowsyblException("Coule not serialize rao results : " + e.getMessage());
            }
        });
    }

    @CEntryPoint(name = "serializeRaoResultsToBuffer")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointer> serializeRaoResultsToBuffer(IsolateThread thread, ObjectHandle raoContextHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                new RaoResultJsonExporter().exportData(
                    raoContext.getResults(), raoContext.getCrac(), Set.of(Unit.MEGAWATT), output);
                return Util.createByteArray(output.toByteArray());
            } catch (IOException e) {
                throw new PowsyblException("Coule not serialize rao results : " + e.getMessage());
            }
        });
    }

    static InputStream createStreamOrThrow(Path filePath, String exceptionMsg) {
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new PowsyblException(exceptionMsg + " (provided path " + filePath + ") : " + e.getMessage());
        }
    }
}
