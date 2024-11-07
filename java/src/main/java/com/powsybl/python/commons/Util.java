/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.commons;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.CompressionFormat;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType;
import com.powsybl.iidm.network.ThreeSides;
import com.powsybl.iidm.network.ValidationLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.input.algo.OpenReacAmplLogLevel;
import com.powsybl.openreac.parameters.input.algo.OpenReacSolverLogLevel;
import com.powsybl.openreac.parameters.input.algo.ReactiveSlackBusesMode;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerObjective;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerStatus;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerLogLevelAmpl;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerLogLevelSolver;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerReactiveSlackBusesMode;
import com.powsybl.python.dataframe.CDataframeHandler;
import com.powsybl.security.LimitViolationType;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public final class Util {

    private Util() {

    }

    public static void setException(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, Throwable t) {
        LoggerFactory.getLogger(CommonCFunctions.class).debug(t.getMessage(), t);
        // we need to create a non null message as on C++ side a null message is considered as non exception to rethrow
        // typically a NullPointerException has a null message and an empty string message need to be set in order to
        // correctly handle the exception on C++ side
        String message = t.getMessage();
        String nonNullMessage = message == null || message.isEmpty() ? t.toString() : message;
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

        T get() throws IOException;
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

    public static ArrayPointer<CCharPointer> createByteArray(byte[] bytes) {
        return allocArrayPointer(CTypeUtil.toBytePtr(bytes), bytes.length);
    }

    public static int convert(SeriesDataType type) {
        return switch (type) {
            case STRING -> CDataframeHandler.STRING_SERIES_TYPE;
            case DOUBLE -> CDataframeHandler.DOUBLE_SERIES_TYPE;
            case INT -> CDataframeHandler.INT_SERIES_TYPE;
            case BOOLEAN -> CDataframeHandler.BOOLEAN_SERIES_TYPE;
        };
    }

    public static PyPowsyblApiHeader.ElementType convert(DataframeElementType type) {
        return switch (type) {
            case BUS -> PyPowsyblApiHeader.ElementType.BUS;
            case BUS_FROM_BUS_BREAKER_VIEW -> PyPowsyblApiHeader.ElementType.BUS_FROM_BUS_BREAKER_VIEW;
            case LINE -> PyPowsyblApiHeader.ElementType.LINE;
            case TWO_WINDINGS_TRANSFORMER -> PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER;
            case THREE_WINDINGS_TRANSFORMER -> PyPowsyblApiHeader.ElementType.THREE_WINDINGS_TRANSFORMER;
            case GENERATOR -> PyPowsyblApiHeader.ElementType.GENERATOR;
            case LOAD -> PyPowsyblApiHeader.ElementType.LOAD;
            case BATTERY -> PyPowsyblApiHeader.ElementType.BATTERY;
            case SHUNT_COMPENSATOR -> PyPowsyblApiHeader.ElementType.SHUNT_COMPENSATOR;
            case DANGLING_LINE -> PyPowsyblApiHeader.ElementType.DANGLING_LINE;
            case TIE_LINE -> PyPowsyblApiHeader.ElementType.TIE_LINE;
            case LCC_CONVERTER_STATION -> PyPowsyblApiHeader.ElementType.LCC_CONVERTER_STATION;
            case VSC_CONVERTER_STATION -> PyPowsyblApiHeader.ElementType.VSC_CONVERTER_STATION;
            case STATIC_VAR_COMPENSATOR -> PyPowsyblApiHeader.ElementType.STATIC_VAR_COMPENSATOR;
            case SWITCH -> PyPowsyblApiHeader.ElementType.SWITCH;
            case VOLTAGE_LEVEL -> PyPowsyblApiHeader.ElementType.VOLTAGE_LEVEL;
            case SUBSTATION -> PyPowsyblApiHeader.ElementType.SUBSTATION;
            case BUSBAR_SECTION -> PyPowsyblApiHeader.ElementType.BUSBAR_SECTION;
            case HVDC_LINE -> PyPowsyblApiHeader.ElementType.HVDC_LINE;
            case RATIO_TAP_CHANGER_STEP -> PyPowsyblApiHeader.ElementType.RATIO_TAP_CHANGER_STEP;
            case PHASE_TAP_CHANGER_STEP -> PyPowsyblApiHeader.ElementType.PHASE_TAP_CHANGER_STEP;
            case RATIO_TAP_CHANGER -> PyPowsyblApiHeader.ElementType.RATIO_TAP_CHANGER;
            case PHASE_TAP_CHANGER -> PyPowsyblApiHeader.ElementType.PHASE_TAP_CHANGER;
            case REACTIVE_CAPABILITY_CURVE_POINT -> PyPowsyblApiHeader.ElementType.REACTIVE_CAPABILITY_CURVE_POINT;
            case NON_LINEAR_SHUNT_COMPENSATOR_SECTION ->
                    PyPowsyblApiHeader.ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION;
            case LINEAR_SHUNT_COMPENSATOR_SECTION -> PyPowsyblApiHeader.ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION;
            case OPERATIONAL_LIMITS -> PyPowsyblApiHeader.ElementType.OPERATIONAL_LIMITS;
            case SELECTED_OPERATIONAL_LIMITS -> PyPowsyblApiHeader.ElementType.SELECTED_OPERATIONAL_LIMITS;
            case MINMAX_REACTIVE_LIMITS -> PyPowsyblApiHeader.ElementType.MINMAX_REACTIVE_LIMITS;
            case ALIAS -> PyPowsyblApiHeader.ElementType.ALIAS;
            case TERMINAL -> PyPowsyblApiHeader.ElementType.TERMINAL;
            case INJECTION -> PyPowsyblApiHeader.ElementType.INJECTION;
            case BRANCH -> PyPowsyblApiHeader.ElementType.BRANCH;
            case IDENTIFIABLE -> PyPowsyblApiHeader.ElementType.IDENTIFIABLE;
            case SUB_NETWORK -> PyPowsyblApiHeader.ElementType.SUB_NETWORK;
            case AREA -> PyPowsyblApiHeader.ElementType.AREA;
            case AREA_VOLTAGE_LEVELS -> PyPowsyblApiHeader.ElementType.AREA_VOLTAGE_LEVELS;
            case AREA_BOUNDARIES -> PyPowsyblApiHeader.ElementType.AREA_BOUNDARIES;
        };
    }

    public static DataframeElementType convert(PyPowsyblApiHeader.ElementType type) {
        return switch (type) {
            case BUS -> DataframeElementType.BUS;
            case BUS_FROM_BUS_BREAKER_VIEW -> DataframeElementType.BUS_FROM_BUS_BREAKER_VIEW;
            case LINE -> DataframeElementType.LINE;
            case TWO_WINDINGS_TRANSFORMER -> DataframeElementType.TWO_WINDINGS_TRANSFORMER;
            case THREE_WINDINGS_TRANSFORMER -> DataframeElementType.THREE_WINDINGS_TRANSFORMER;
            case GENERATOR -> DataframeElementType.GENERATOR;
            case LOAD -> DataframeElementType.LOAD;
            case BATTERY -> DataframeElementType.BATTERY;
            case SHUNT_COMPENSATOR -> DataframeElementType.SHUNT_COMPENSATOR;
            case DANGLING_LINE -> DataframeElementType.DANGLING_LINE;
            case TIE_LINE -> DataframeElementType.TIE_LINE;
            case LCC_CONVERTER_STATION -> DataframeElementType.LCC_CONVERTER_STATION;
            case VSC_CONVERTER_STATION -> DataframeElementType.VSC_CONVERTER_STATION;
            case STATIC_VAR_COMPENSATOR -> DataframeElementType.STATIC_VAR_COMPENSATOR;
            case SWITCH -> DataframeElementType.SWITCH;
            case VOLTAGE_LEVEL -> DataframeElementType.VOLTAGE_LEVEL;
            case SUBSTATION -> DataframeElementType.SUBSTATION;
            case BUSBAR_SECTION -> DataframeElementType.BUSBAR_SECTION;
            case HVDC_LINE -> DataframeElementType.HVDC_LINE;
            case RATIO_TAP_CHANGER_STEP -> DataframeElementType.RATIO_TAP_CHANGER_STEP;
            case PHASE_TAP_CHANGER_STEP -> DataframeElementType.PHASE_TAP_CHANGER_STEP;
            case RATIO_TAP_CHANGER -> DataframeElementType.RATIO_TAP_CHANGER;
            case PHASE_TAP_CHANGER -> DataframeElementType.PHASE_TAP_CHANGER;
            case REACTIVE_CAPABILITY_CURVE_POINT -> DataframeElementType.REACTIVE_CAPABILITY_CURVE_POINT;
            case NON_LINEAR_SHUNT_COMPENSATOR_SECTION -> DataframeElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION;
            case LINEAR_SHUNT_COMPENSATOR_SECTION -> DataframeElementType.LINEAR_SHUNT_COMPENSATOR_SECTION;
            case OPERATIONAL_LIMITS -> DataframeElementType.OPERATIONAL_LIMITS;
            case SELECTED_OPERATIONAL_LIMITS -> DataframeElementType.SELECTED_OPERATIONAL_LIMITS;
            case MINMAX_REACTIVE_LIMITS -> DataframeElementType.MINMAX_REACTIVE_LIMITS;
            case ALIAS -> DataframeElementType.ALIAS;
            case TERMINAL -> DataframeElementType.TERMINAL;
            case INJECTION -> DataframeElementType.INJECTION;
            case BRANCH -> DataframeElementType.BRANCH;
            case IDENTIFIABLE -> DataframeElementType.IDENTIFIABLE;
            case SUB_NETWORK -> DataframeElementType.SUB_NETWORK;
            case AREA -> DataframeElementType.AREA;
            case AREA_VOLTAGE_LEVELS -> DataframeElementType.AREA_VOLTAGE_LEVELS;
            case AREA_BOUNDARIES -> DataframeElementType.AREA_BOUNDARIES;
        };
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
            case BUS_VOLTAGE -> SensitivityFunctionType.BUS_VOLTAGE;
            case BUS_REACTIVE_POWER -> SensitivityFunctionType.BUS_REACTIVE_POWER;
        };
    }

    public static SensitivityVariableType convert(PyPowsyblApiHeader.SensitivityVariableType type) {
        return switch (type) {
            case AUTO_DETECT -> null;
            case INJECTION_ACTIVE_POWER -> SensitivityVariableType.INJECTION_ACTIVE_POWER;
            case INJECTION_REACTIVE_POWER -> SensitivityVariableType.INJECTION_REACTIVE_POWER;
            case TRANSFORMER_PHASE -> SensitivityVariableType.TRANSFORMER_PHASE;
            case BUS_TARGET_VOLTAGE -> SensitivityVariableType.BUS_TARGET_VOLTAGE;
            case HVDC_LINE_ACTIVE_POWER -> SensitivityVariableType.HVDC_LINE_ACTIVE_POWER;
            case TRANSFORMER_PHASE_1 -> SensitivityVariableType.TRANSFORMER_PHASE_1;
            case TRANSFORMER_PHASE_2 -> SensitivityVariableType.TRANSFORMER_PHASE_2;
            case TRANSFORMER_PHASE_3 -> SensitivityVariableType.TRANSFORMER_PHASE_3;
        };
    }

    public static ContingencyContextType convert(PyPowsyblApiHeader.RawContingencyContextType type) {
        return switch (type) {
            case ALL -> ContingencyContextType.ALL;
            case NONE -> ContingencyContextType.NONE;
            case SPECIFIC -> ContingencyContextType.SPECIFIC;
            case ONLY_CONTINGENCIES -> ContingencyContextType.ONLY_CONTINGENCIES;
        };
    }

    public static PyPowsyblApiHeader.ValidationLevelType convert(ValidationLevel level) {
        return switch (level) {
            case EQUIPMENT -> PyPowsyblApiHeader.ValidationLevelType.EQUIPMENT;
            case STEADY_STATE_HYPOTHESIS -> PyPowsyblApiHeader.ValidationLevelType.STEADY_STATE_HYPOTHESIS;
        };
    }

    public static ValidationLevel convert(PyPowsyblApiHeader.ValidationLevelType levelType) {
        return switch (levelType) {
            case EQUIPMENT -> ValidationLevel.EQUIPMENT;
            case STEADY_STATE_HYPOTHESIS -> ValidationLevel.STEADY_STATE_HYPOTHESIS;
        };
    }

    public static DataframeNetworkModificationType convert(PyPowsyblApiHeader.NetworkModificationType networkModificationType) {
        return switch (networkModificationType) {
            case VOLTAGE_LEVEL_TOPOLOGY_CREATION -> DataframeNetworkModificationType.VOLTAGE_LEVEL_TOPOLOGY_CREATION;
            case CREATE_COUPLING_DEVICE -> DataframeNetworkModificationType.CREATE_COUPLING_DEVICE;
            case CREATE_FEEDER_BAY -> DataframeNetworkModificationType.CREATE_FEEDER_BAY;
            case CREATE_LINE_FEEDER -> DataframeNetworkModificationType.CREATE_LINE_FEEDER;
            case CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER ->
                    DataframeNetworkModificationType.CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER;
            case CREATE_LINE_ON_LINE -> DataframeNetworkModificationType.CREATE_LINE_ON_LINE;
            case REVERT_CREATE_LINE_ON_LINE -> DataframeNetworkModificationType.REVERT_CREATE_LINE_ON_LINE;
            case CONNECT_VOLTAGE_LEVEL_ON_LINE -> DataframeNetworkModificationType.CONNECT_VOLTAGE_LEVEL_ON_LINE;
            case REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE ->
                    DataframeNetworkModificationType.REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE;
            case REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE ->
                    DataframeNetworkModificationType.REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE;
        };
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

    public static OpenReacAmplLogLevel convert(VoltageInitializerLogLevelAmpl obj) {
        return switch (obj) {
            case LOG_AMPL_DEBUG -> OpenReacAmplLogLevel.DEBUG;
            case LOG_AMPL_INFO -> OpenReacAmplLogLevel.INFO;
            case LOG_AMPL_WARNING -> OpenReacAmplLogLevel.WARNING;
            case LOG_AMPL_ERROR -> OpenReacAmplLogLevel.ERROR;
        };
    }

    public static OpenReacSolverLogLevel convert(VoltageInitializerLogLevelSolver obj) {
        return switch (obj) {
            case NOTHING -> OpenReacSolverLogLevel.NOTHING;
            case ONLY_RESULTS -> OpenReacSolverLogLevel.ONLY_RESULTS;
            case EVERYTHING -> OpenReacSolverLogLevel.EVERYTHING;
        };
    }

    public static ReactiveSlackBusesMode convert(VoltageInitializerReactiveSlackBusesMode obj) {
        return switch (obj) {
            case CONFIGURED -> ReactiveSlackBusesMode.CONFIGURED;
            case NO_GENERATION -> ReactiveSlackBusesMode.NO_GENERATION;
            case ALL_BUSES -> ReactiveSlackBusesMode.ALL;
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

    public static LimitViolationType convert(PyPowsyblApiHeader.LimitViolationType violationType) {
        return switch (violationType) {
            case ACTIVE_POWER -> LimitViolationType.ACTIVE_POWER;
            case APPARENT_POWER -> LimitViolationType.APPARENT_POWER;
            case CURRENT -> LimitViolationType.CURRENT;
            case LOW_VOLTAGE -> LimitViolationType.LOW_VOLTAGE;
            case HIGH_VOLTAGE -> LimitViolationType.HIGH_VOLTAGE;
            case LOW_SHORT_CIRCUIT_CURRENT -> LimitViolationType.LOW_SHORT_CIRCUIT_CURRENT;
            case HIGH_SHORT_CIRCUIT_CURRENT -> LimitViolationType.HIGH_SHORT_CIRCUIT_CURRENT;
            case OTHER -> LimitViolationType.OTHER;
            default -> throw new PowsyblException("Unknown limit violation type: " + violationType);
        };
    }

    public static ThreeSides convert(PyPowsyblApiHeader.ThreeSideType side) {
        return switch (side.getCValue()) {
            case 0 -> ThreeSides.ONE;
            case 1 -> ThreeSides.TWO;
            case 2 -> ThreeSides.THREE;
            default -> null;
        };
    }
}
