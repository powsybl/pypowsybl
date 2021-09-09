package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.SeriesDataType;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static com.powsybl.python.PyPowsyblApiHeader.allocArrayPointer;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public final class Util {

    private Util() {

    }

    public static void setException(PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr, Throwable t) {
        LoggerFactory.getLogger(PyPowsyblApiLib.class).debug(t.getMessage(), t);
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

    interface PointerProvider<T extends WordBase> {

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

    static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> createCharPtrArray(List<String> stringList) {
        CCharPointerPointer stringListPtr = UnmanagedMemory.calloc(stringList.size() * SizeOf.get(CCharPointerPointer.class));
        for (int i = 0; i < stringList.size(); i++) {
            stringListPtr.addressOf(i).write(CTypeUtil.toCharPtr(stringList.get(i)));
        }
        return allocArrayPointer(stringListPtr, stringList.size());
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
            case CURRENT_LIMITS:
                return PyPowsyblApiHeader.ElementType.CURRENT_LIMITS;
            case NON_LINEAR_SHUNT_COMPENSATOR_SECTION:
                return PyPowsyblApiHeader.ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION;
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
            case CURRENT_LIMITS:
                return DataframeElementType.CURRENT_LIMITS;
            case NON_LINEAR_SHUNT_COMPENSATOR_SECTION:
                return DataframeElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION;
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
}
