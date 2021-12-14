/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.struct.*;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.word.PointerBase;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class PyPowsyblApiHeader {

    private PyPowsyblApiHeader() {
    }

    @CStruct("exception_handler")
    interface ExceptionHandlerPointer extends PointerBase {

        @CField("message")
        CCharPointer geMessage();

        @CField("message")
        void setMessage(CCharPointer message);
    }

    @CStruct("array")
    interface ArrayPointer<T extends PointerBase> extends PointerBase {

        @CField("ptr")
        T getPtr();

        @CField("ptr")
        void setPtr(T ptr);

        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);
    }

    static <T extends PointerBase> ArrayPointer<T> allocArrayPointer(T ptr, int length) {
        ArrayPointer<T> arrayPtr = UnmanagedMemory.calloc(SizeOf.get(ArrayPointer.class));
        arrayPtr.setPtr(ptr);
        arrayPtr.setLength(length);
        return arrayPtr;
    }

    static <T extends PointerBase> void freeArrayPointer(ArrayPointer<T> arrayPointer) {
        UnmanagedMemory.free(arrayPointer.getPtr());
        UnmanagedMemory.free(arrayPointer);
    }

    @CStruct("network_metadata")
    interface NetworkMetadataPointer extends PointerBase {

        @CField("id")
        CCharPointer getId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("name")
        CCharPointer getName();

        @CField("name")
        void setName(CCharPointer name);

        @CField("case_date")
        double getCaseDate();

        @CField("case_date")
        void setCaseDate(double millis);

        @CField("source_format")
        CCharPointer getSourceFormat();

        @CField("source_format")
        void setSourceFormat(CCharPointer sourceFormat);

        @CField("forecast_distance")
        int getForecastDistance();

        @CField("forecast_distance")
        void setForecastDistance(int forecastDistance);
    }

    @CStruct("load_flow_component_result")
    interface LoadFlowComponentResultPointer extends PointerBase {

        @CField("connected_component_num")
        int getConnectedComponentNum();

        @CField("connected_component_num")
        void setConnectedComponentNum(int connectedComponentNum);

        @CField("synchronous_component_num")
        int getSynchronousComponentNum();

        @CField("synchronous_component_num")
        void setSynchronousComponentNum(int synchronousComponentNum);

        @CField("status")
        int getStatus();

        @CField("status")
        void setStatus(int status);

        @CField("iteration_count")
        int getIterationCount();

        @CField("iteration_count")
        void setIterationCount(int iterationCount);

        @CField("slack_bus_id")
        CCharPointer getSlackBusId();

        @CField("slack_bus_id")
        void setSlackBusId(CCharPointer slackBusId);

        @CField("slack_bus_active_power_mismatch")
        double getSlackBusActivePowerMismatch();

        @CField("slack_bus_active_power_mismatch")
        void setSlackBusActivePowerMismatch(double slackBusActivePowerMismatch);

        LoadFlowComponentResultPointer addressOf(int index);
    }

    @CStruct("load_flow_parameters")
    interface LoadFlowParametersPointer extends PointerBase {

        @CField("voltage_init_mode")
        int getVoltageInitMode();

        @CField("voltage_init_mode")
        void setVoltageInitMode(int voltageInitMode);

        @CField("transformer_voltage_control_on")
        boolean isTransformerVoltageControlOn();

        @CField("transformer_voltage_control_on")
        void setTransformerVoltageControlOn(boolean transformerVoltageControlOn);

        @CField("no_generator_reactive_limits")
        boolean isNoGeneratorReactiveLimits();

        @CField("no_generator_reactive_limits")
        void setNoGeneratorReactiveLimits(boolean noGeneratorReactiveLimits);

        @CField("phase_shifter_regulation_on")
        boolean isPhaseShifterRegulationOn();

        @CField("phase_shifter_regulation_on")
        void setPhaseShifterRegulationOn(boolean phaseShifterRegulationOn);

        @CField("twt_split_shunt_admittance")
        boolean isTwtSplitShuntAdmittance();

        @CField("twt_split_shunt_admittance")
        void setTwtSplitShuntAdmittance(boolean twtSplitShuntAdmittance);

        @CField("simul_shunt")
        boolean isSimulShunt();

        @CField("simul_shunt")
        void setSimulShunt(boolean simulShunt);

        @CField("read_slack_bus")
        boolean isReadSlackBus();

        @CField("read_slack_bus")
        void setReadSlackBus(boolean readSlackBus);

        @CField("write_slack_bus")
        boolean isWriteSlackBus();

        @CField("write_slack_bus")
        void setWriteSlackBus(boolean writeSlackBus);

        @CField("distributed_slack")
        boolean isDistributedSlack();

        @CField("distributed_slack")
        void setDistributedSlack(boolean distributedSlack);

        @CField("balance_type")
        int getBalanceType();

        @CField("balance_type")
        void setBalanceType(int balanceType);

        @CField("dc_use_transformer_ratio")
        boolean isDcUseTransformerRatio();

        @CField("dc_use_transformer_ratio")
        void setDcUseTransformerRatio(boolean dcUseTransformerRatio);

        @CField("countries_to_balance")
        CCharPointerPointer getCountriesToBalance();

        @CField("countries_to_balance")
        void setCountriesToBalance(CCharPointerPointer countriesToBalance);

        @CField("countries_to_balance_count")
        int getCountriesToBalanceCount();

        @CField("countries_to_balance_count")
        void setCountriesToBalanceCount(int countriesToBalanceCount);

        @CField("connected_component_mode")
        int getConnectedComponentMode();

        @CField("connected_component_mode")
        void setConnectedComponentMode(int connectedComponentMode);
    }

    @CStruct("limit_violation")
    interface LimitViolationPointer extends PointerBase {

        @CField("subject_id")
        CCharPointer getSubjectId();

        @CField("subject_id")
        void setSubjectId(CCharPointer subjectId);

        @CField("subject_name")
        CCharPointer getSubjectName();

        @CField("subject_name")
        void setSubjectName(CCharPointer subjectName);

        @CField("limit_type")
        int getLimitType();

        @CField("limit_type")
        void setLimitType(int limitType);

        @CField("limit")
        double getLimit();

        @CField("limit")
        void setLimit(double limit);

        @CField("limit_name")
        CCharPointer getLimitName();

        @CField("limit_name")
        void setLimitName(CCharPointer limitName);

        @CField("acceptable_duration")
        int getAcceptableDuration();

        @CField("acceptable_duration")
        void setAcceptableDuration(int acceptableDuration);

        @CField("limit_reduction")
        float getLimitReduction();

        @CField("limit_reduction")
        void setLimitReduction(float limitReduction);

        @CField("value")
        double getValue();

        @CField("value")
        void setValue(double value);

        @CField("side")
        int getSide();

        @CField("side")
        void setSide(int side);

        LimitViolationPointer addressOf(int index);
    }

    @CStruct("contingency_result")
    interface ContingencyResultPointer extends PointerBase {

        @CField("contingency_id")
        CCharPointer getContingencyId();

        @CField("contingency_id")
        void setContingencyId(CCharPointer contingencyId);

        @CField("status")
        int getStatus();

        @CField("status")
        void setStatus(int status);

        @CFieldAddress("limit_violations")
        ArrayPointer<LimitViolationPointer> limitViolations();

        ContingencyResultPointer addressOf(int index);
    }

    @CEnum("element_type")
    public enum ElementType {
        BUS,
        LINE,
        TWO_WINDINGS_TRANSFORMER,
        THREE_WINDINGS_TRANSFORMER,
        GENERATOR,
        LOAD,
        BATTERY,
        SHUNT_COMPENSATOR,
        NON_LINEAR_SHUNT_COMPENSATOR_SECTION,
        LINEAR_SHUNT_COMPENSATOR_SECTION,
        DANGLING_LINE,
        LCC_CONVERTER_STATION,
        VSC_CONVERTER_STATION,
        STATIC_VAR_COMPENSATOR,
        SWITCH,
        VOLTAGE_LEVEL,
        SUBSTATION,
        BUSBAR_SECTION,
        HVDC_LINE,
        RATIO_TAP_CHANGER_STEP,
        PHASE_TAP_CHANGER_STEP,
        RATIO_TAP_CHANGER,
        PHASE_TAP_CHANGER,
        REACTIVE_CAPABILITY_CURVE_POINT,
        CURRENT_LIMITS;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ElementType fromCValue(int value);
    }

    @CStruct("matrix")
    interface MatrixPointer extends PointerBase {

        @CField("values")
        CDoublePointer getValues();

        @CField("values")
        void setValues(CDoublePointer values);

        @CField("row_count")
        int getRowCount();

        @CField("row_count")
        void setRowCount(int rowCount);

        @CField("column_count")
        int getColumnCount();

        @CField("column_count")
        void setColumnCount(int columnCount);
    }

    @CStruct("series")
    interface SeriesPointer extends PointerBase {

        @CField("name")
        CCharPointer getName();

        @CField("name")
        void setName(CCharPointer name);

        @CField("index")
        boolean isIndex();

        @CField("index")
        void setIndex(boolean index);

        @CField("type")
        int getType();

        @CField("type")
        void setType(int type);

        @CFieldAddress("data")
        <T extends PointerBase> ArrayPointer<T> data();

        SeriesPointer addressOf(int index);
    }

    @CStruct("zone")
    interface ZonePointer extends PointerBase {

        @CField("id")
        CCharPointer getId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("injections_ids")
        CCharPointerPointer getInjectionsIds();

        @CField("injections_ids")
        void setInjectionsIds(CCharPointerPointer injectionsIds);

        @CField("injections_shift_keys")
        CDoublePointer getinjectionsShiftKeys();

        @CField("injections_shift_keys")
        void setinjectionsShiftKeys(CDoublePointer injectionsShiftKeys);

        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);

        ZonePointer addressOf(int index);
    }

    @CPointerTo(ZonePointer.class)
    interface ZonePointerPointer extends PointerBase {

        ZonePointer read(int index);
    }

    @CPointerTo(VoidPointer.class)
    interface VoidPointerPointer extends PointerBase {

        ObjectHandle read(int index);
    }


    @CEnum("contingency_context_type")
    public enum RawContingencyContextType {

        ALL,
        NONE,
        SPECIFIC;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native RawContingencyContextType fromCValue(int value);
    }

    @CEnum("filter_attributes_type")
    public enum FilterAttributesType {
        ALL_ATTRIBUTES,
        DEFAULT_ATTRIBUTES,
        SELECTION_ATTRIBUTES;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native FilterAttributesType fromCValue(int value);
    }
}
