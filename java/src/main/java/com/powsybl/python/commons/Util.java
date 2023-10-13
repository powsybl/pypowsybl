/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.commons;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.CompressionFormat;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.ValidationLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerObjective;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerStatus;
import com.powsybl.python.dataframe.CDataframeHandler;
import com.powsybl.sensitivity.SensitivityFunctionType;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public final class Util {

    private Util() {

    }

    public static void setException(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, Throwable t) {
        LoggerFactory.getLogger(CommonCFunctions.class).debug(t.getMessage(), t);
        // we need to create a non null message as on C++ side a null message is considered as non exception to rethrow
        // typically a NullPointerException has a null message and an empty string message need to be set in order to
        // correctly handle the exception on C++ side
        String nonNullMessage = Objects.toString(t.getMessage(), "");
        exceptionHandlerPtr.setMessage(CTypeUtil.toCharPtr(nonNullMessage));
    }

    public static void doCatch(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, Runnable runnable) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            runnable.run();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
        }
    }

    public static boolean doCatch(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, BooleanSupplier supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.getAsBoolean();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return false;
        }
    }

    public static int doCatch(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, IntSupplier supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return -1;
        }
    }

    public static long doCatch(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, LongSupplier supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.getAsLong();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return 0;
        }
    }

    public static <T extends Enum<?>> T doCatch(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr,
                                                Supplier<T> supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.get();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return null;
        }
    }

    public interface PointerProvider<T extends WordBase> {

        T get();
    }

    public static <T extends WordBase> T doCatch(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, PointerProvider<T> supplier) {
        exceptionHandlerPtr.setMessage(WordFactory.nullPointer());
        try {
            return supplier.get();
        } catch (Throwable t) {
            setException(exceptionHandlerPtr, t);
            return WordFactory.zero();
        }
    }

    public static ArrayPointer<CCharPointerPointer> createCharPtrArray(List<String> stringList) {
        return allocArrayPointer(getStringListAsPtr(stringList), stringList.size());
    }

    /**
     * Unsafe to use without an indicator of size !
     *
     * @param stringList the string list to transform into a pointer
     */
    public static CCharPointerPointer getStringListAsPtr(List<String> stringList) {
        CCharPointerPointer stringListPtr = UnmanagedMemory.calloc(stringList.size() * SizeOf.get(CCharPointerPointer.class));
        for (int i = 0; i < stringList.size(); i++) {
            stringListPtr.addressOf(i).write(CTypeUtil.toCharPtr(stringList.get(i)));
        }
        return stringListPtr;
    }

    public static ArrayPointer<CDoublePointer> createDoubleArray(List<Double> doubleList) {
        CDoublePointer doubleListPtr = UnmanagedMemory.calloc(doubleList.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < doubleList.size(); i++) {
            doubleListPtr.write(i, doubleList.get(i));
        }
        return allocArrayPointer(doubleListPtr, doubleList.size());
    }

    public static ArrayPointer<CIntPointer> createIntegerArray(List<Integer> integerList) {
        CIntPointer intListPtr = UnmanagedMemory.calloc(integerList.size() * SizeOf.get(CIntPointer.class));
        for (int i = 0; i < integerList.size(); i++) {
            intListPtr.write(i, integerList.get(i));
        }
        return allocArrayPointer(intListPtr, integerList.size());
    }

    public static int convert(SeriesDataType type) {
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

    public static PyPowsyblApiHeader.ElementType convert(DataframeElementType type) {
        switch (type) {
            case BUS:
                return PyPowsyblApiHeader.ElementType.BUS;
            case LINE:
                return PyPowsyblApiHeader.ElementType.LINE;
            case TWO_WINDINGS_TRANSFORMER:
                return PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER;
            case THREE_WINDINGS_TRANSFORMER:
                return PyPowsyblApiHeader.ElementType.THREE_WINDINGS_TRANSFORMER;
            case GENERATOR:
                return PyPowsyblApiHeader.ElementType.GENERATOR;
            case LOAD:
                return PyPowsyblApiHeader.ElementType.LOAD;
            case BATTERY:
                return PyPowsyblApiHeader.ElementType.BATTERY;
            case SHUNT_COMPENSATOR:
                return PyPowsyblApiHeader.ElementType.SHUNT_COMPENSATOR;
            case DANGLING_LINE:
                return PyPowsyblApiHeader.ElementType.DANGLING_LINE;
            case TIE_LINE:
                return PyPowsyblApiHeader.ElementType.TIE_LINE;
            case LCC_CONVERTER_STATION:
                return PyPowsyblApiHeader.ElementType.LCC_CONVERTER_STATION;
            case VSC_CONVERTER_STATION:
                return PyPowsyblApiHeader.ElementType.VSC_CONVERTER_STATION;
            case STATIC_VAR_COMPENSATOR:
                return PyPowsyblApiHeader.ElementType.STATIC_VAR_COMPENSATOR;
            case SWITCH:
                return PyPowsyblApiHeader.ElementType.SWITCH;
            case VOLTAGE_LEVEL:
                return PyPowsyblApiHeader.ElementType.VOLTAGE_LEVEL;
            case SUBSTATION:
                return PyPowsyblApiHeader.ElementType.SUBSTATION;
            case BUSBAR_SECTION:
                return PyPowsyblApiHeader.ElementType.BUSBAR_SECTION;
            case HVDC_LINE:
                return PyPowsyblApiHeader.ElementType.HVDC_LINE;
            case RATIO_TAP_CHANGER_STEP:
                return PyPowsyblApiHeader.ElementType.RATIO_TAP_CHANGER_STEP;
            case PHASE_TAP_CHANGER_STEP:
                return PyPowsyblApiHeader.ElementType.PHASE_TAP_CHANGER_STEP;
            case RATIO_TAP_CHANGER:
                return PyPowsyblApiHeader.ElementType.RATIO_TAP_CHANGER;
            case PHASE_TAP_CHANGER:
                return PyPowsyblApiHeader.ElementType.PHASE_TAP_CHANGER;
            case REACTIVE_CAPABILITY_CURVE_POINT:
                return PyPowsyblApiHeader.ElementType.REACTIVE_CAPABILITY_CURVE_POINT;
            case NON_LINEAR_SHUNT_COMPENSATOR_SECTION:
                return PyPowsyblApiHeader.ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION;
            case LINEAR_SHUNT_COMPENSATOR_SECTION:
                return PyPowsyblApiHeader.ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION;
            case OPERATIONAL_LIMITS:
                return PyPowsyblApiHeader.ElementType.OPERATIONAL_LIMITS;
            case MINMAX_REACTIVE_LIMITS:
                return PyPowsyblApiHeader.ElementType.MINMAX_REACTIVE_LIMITS;
            case ALIAS:
                return PyPowsyblApiHeader.ElementType.ALIAS;
            case TERMINAL:
                return PyPowsyblApiHeader.ElementType.TERMINAL;
            case INJECTION:
                return PyPowsyblApiHeader.ElementType.INJECTION;
            case BRANCH:
                return PyPowsyblApiHeader.ElementType.BRANCH;
            case IDENTIFIABLE:
                return PyPowsyblApiHeader.ElementType.IDENTIFIABLE;
            default:
                throw new PowsyblException("Unknown element type : " + type);
        }
    }

    public static DataframeElementType convert(PyPowsyblApiHeader.ElementType type) {
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
            case TIE_LINE:
                return DataframeElementType.TIE_LINE;
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
            case NON_LINEAR_SHUNT_COMPENSATOR_SECTION:
                return DataframeElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION;
            case LINEAR_SHUNT_COMPENSATOR_SECTION:
                return DataframeElementType.LINEAR_SHUNT_COMPENSATOR_SECTION;
            case OPERATIONAL_LIMITS:
                return DataframeElementType.OPERATIONAL_LIMITS;
            case MINMAX_REACTIVE_LIMITS:
                return DataframeElementType.MINMAX_REACTIVE_LIMITS;
            case ALIAS:
                return DataframeElementType.ALIAS;
            case TERMINAL:
                return DataframeElementType.TERMINAL;
            case INJECTION:
                return DataframeElementType.INJECTION;
            case BRANCH:
                return DataframeElementType.BRANCH;
            case IDENTIFIABLE:
                return DataframeElementType.IDENTIFIABLE;
            default:
                throw new PowsyblException("Unknown element type : " + type);
        }
    }

    public static SensitivityFunctionType convert(PyPowsyblApiHeader.SensitivityFunctionType type) {
        return switch (type) {
            case BRANCH_ACTIVE_POWER_1 -> SensitivityFunctionType.BRANCH_ACTIVE_POWER_1;
            case BRANCH_CURRENT_1 -> SensitivityFunctionType.BRANCH_CURRENT_1;
            case BRANCH_REACTIVE_POWER_1 -> SensitivityFunctionType.BRANCH_REACTIVE_POWER_1;
            case BRANCH_ACTIVE_POWER_2 -> SensitivityFunctionType.BRANCH_ACTIVE_POWER_2;
            case BRANCH_CURRENT_2 -> SensitivityFunctionType.BRANCH_CURRENT_2;
            case BRANCH_REACTIVE_POWER_2 -> SensitivityFunctionType.BRANCH_REACTIVE_POWER_2;
            case BRANCH_ACTIVE_POWER_3 -> SensitivityFunctionType.BRANCH_ACTIVE_POWER_3;
            case BRANCH_CURRENT_3 -> SensitivityFunctionType.BRANCH_CURRENT_3;
            case BRANCH_REACTIVE_POWER_3 -> SensitivityFunctionType.BRANCH_REACTIVE_POWER_3;
        };
    }

    public static ContingencyContextType convert(PyPowsyblApiHeader.RawContingencyContextType type) {
        return switch (type) {
            case ALL -> ContingencyContextType.ALL;
            case NONE -> ContingencyContextType.NONE;
            case SPECIFIC -> ContingencyContextType.SPECIFIC;
        };
    }

    public static PyPowsyblApiHeader.ValidationLevelType convert(ValidationLevel level) {
        switch (level) {
            case EQUIPMENT:
                return PyPowsyblApiHeader.ValidationLevelType.EQUIPMENT;
            case STEADY_STATE_HYPOTHESIS:
                return PyPowsyblApiHeader.ValidationLevelType.STEADY_STATE_HYPOTHESIS;
            default:
                throw new PowsyblException("Unknown element type : " + level);
        }
    }

    public static ValidationLevel convert(PyPowsyblApiHeader.ValidationLevelType levelType) {
        switch (levelType) {
            case EQUIPMENT:
                return ValidationLevel.EQUIPMENT;
            case STEADY_STATE_HYPOTHESIS:
                return ValidationLevel.STEADY_STATE_HYPOTHESIS;
            default:
                throw new PowsyblException("Unknown element type : " + levelType);
        }
    }

    public static Branch.Side convert(PyPowsyblApiHeader.BranchSide side) {
        switch (side) {
            case ONE:
                return Branch.Side.ONE;
            case TWO:
                return Branch.Side.TWO;
            default:
                throw new PowsyblException("Unknown element type : " + side);
        }
    }

    public static DataframeNetworkModificationType convert(PyPowsyblApiHeader.NetworkModificationType networkModificationType) {
        switch (networkModificationType) {
            case VOLTAGE_LEVEL_TOPOLOGY_CREATION:
                return DataframeNetworkModificationType.VOLTAGE_LEVEL_TOPOLOGY_CREATION;
            case CREATE_COUPLING_DEVICE:
                return DataframeNetworkModificationType.CREATE_COUPLING_DEVICE;
            case CREATE_FEEDER_BAY:
                return DataframeNetworkModificationType.CREATE_FEEDER_BAY;
            case CREATE_LINE_FEEDER:
                return DataframeNetworkModificationType.CREATE_LINE_FEEDER;
            case CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER:
                return DataframeNetworkModificationType.CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER;
            case CREATE_LINE_ON_LINE:
                return DataframeNetworkModificationType.CREATE_LINE_ON_LINE;
            case REVERT_CREATE_LINE_ON_LINE:
                return DataframeNetworkModificationType.REVERT_CREATE_LINE_ON_LINE;
            case CONNECT_VOLTAGE_LEVEL_ON_LINE:
                return DataframeNetworkModificationType.CONNECT_VOLTAGE_LEVEL_ON_LINE;
            case REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE:
                return DataframeNetworkModificationType.REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE;
            case REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE:
                return DataframeNetworkModificationType.REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE;
            default:
                throw new PowsyblException("Unknown network modification type: " + networkModificationType);
        }
    }

    public static VoltageInitializerStatus convert(OpenReacStatus status) {
        return status == OpenReacStatus.OK ? VoltageInitializerStatus.OK : VoltageInitializerStatus.NOT_OK;
    }

    public static OpenReacOptimisationObjective convert(VoltageInitializerObjective obj) {
        return switch (obj) {
            case MIN_GENERATION -> OpenReacOptimisationObjective.MIN_GENERATION;
            case BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT -> OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT;
            case SPECIFIC_VOLTAGE_PROFILE -> OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE;
        };
    }

    public static byte[] binaryBufferToBytes(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return buffer.array();
        } else {
            byte[] byteBuffer = new byte[buffer.remaining()];
            buffer.get(byteBuffer, 0, buffer.remaining());
            return byteBuffer;
        }
    }

    private static final byte[] ZIP_SIGNATURE = new byte[]{0x50, 0x4B, 0x03, 0x04};
    private static final byte[] GZIP_SIGNATURE = new byte[]{0x1F, (byte) 0x8B};
    private static final byte[] XZ_SIGNATURE = new byte[]{(byte) 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00};
    private static final byte[] BZIP2_SIGNATURE = new byte[]{0x42, 0x5A, 0x68};
    private static final byte[] ZSTD_SIGNATURE = new byte[]{0x28, (byte) 0xB5, 0x2F, (byte) 0xFD};

    private static boolean compareSignature(ByteBuffer buffer, byte[] signature) {
        byte[] header = new byte[signature.length];
        buffer.mark();
        buffer.get(header, 0, signature.length);
        buffer.reset();
        return Arrays.equals(signature, header);
    }

    public static Optional<CompressionFormat> detectCompressionFormat(ByteBuffer buffer) {
        if (compareSignature(buffer, ZIP_SIGNATURE)) {
            return Optional.of(CompressionFormat.ZIP);
        } else if (compareSignature(buffer, GZIP_SIGNATURE)) {
            return Optional.of(CompressionFormat.GZIP);
        } else if (compareSignature(buffer, XZ_SIGNATURE)) {
            return Optional.of(CompressionFormat.XZ);
        } else if (compareSignature(buffer, BZIP2_SIGNATURE)) {
            return Optional.of(CompressionFormat.BZIP2);
        } else if (compareSignature(buffer, ZSTD_SIGNATURE)) {
            return Optional.of(CompressionFormat.ZSTD);
        } else {
            return Optional.empty();
        }
    }
}
