/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.commons;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.DataframeNetworkModificationType;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.iidm.network.ValidationLevel;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.dataframe.CDataframeHandler;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

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
        CCharPointerPointer stringListPtr = UnmanagedMemory.calloc(stringList.size() * SizeOf.get(CCharPointerPointer.class));
        for (int i = 0; i < stringList.size(); i++) {
            stringListPtr.addressOf(i).write(CTypeUtil.toCharPtr(stringList.get(i)));
        }
        return allocArrayPointer(stringListPtr, stringList.size());
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

    public static ContingencyContextType convert(PyPowsyblApiHeader.RawContingencyContextType type) {
        switch (type) {
            case ALL:
                return ContingencyContextType.ALL;
            case NONE:
                return ContingencyContextType.NONE;
            case SPECIFIC:
                return ContingencyContextType.SPECIFIC;
            default:
                throw new PowsyblException("Unknown contingency context type : " + type);
        }
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

    public static DataframeNetworkModificationType convert(PyPowsyblApiHeader.NetworkModificationType networkModificationType) {
        switch (networkModificationType) {
            case VOLTAGE_LEVEL_TOPOLOGY_CREATION:
                return DataframeNetworkModificationType.VOLTAGE_LEVEL_TOPOLOGY_CREATION;
            case CREATE_FEEDER_BAY:
                return DataframeNetworkModificationType.CREATE_FEEDER_BAY;
            case CREATE_LINE_FEEDER:
                return DataframeNetworkModificationType.CREATE_LINE_FEEDER;
            case CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER:
                return DataframeNetworkModificationType.CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER;
            default:
                throw new PowsyblException("Unknown network modification type: " + networkModificationType);
        }
    }

}
