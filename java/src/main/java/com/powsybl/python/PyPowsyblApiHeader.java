/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.struct.*;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
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

    @CStruct("bus")
    interface BusPointer extends PointerBase {

        @CField("id")
        CCharPointer getId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("v_magnitude")
        double getVoltageMagnitude();

        @CField("v_magnitude")
        void setVoltageMagnitude(double voltageMagnitude);

        @CField("v_angle")
        double getVoltageAngle();

        @CField("v_angle")
        void setVoltageAngle(double voltageAngle);

        @CField("component_num")
        int geComponentNum();

        @CField("component_num")
        void setComponentNum(int componentNum);

        BusPointer addressOf(int index);
    }

    @CStruct("generator")
    interface GeneratorPointer extends PointerBase {

        @CField("id")
        CCharPointer getId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("target_p")
        double getTargetP();

        @CField("target_p")
        void setTargetP(double targetP);

        @CField("min_p")
        double getMinP();

        @CField("min_p")
        void setMinP(double minP);

        @CField("max_p")
        double getMaxP();

        @CField("max_p")
        void setMaxP(double maxP);

        @CField("nominal_voltage")
        double getNominalVoltage();

        @CField("nominal_voltage")
        void setNominalVoltage(double nominalVoltage);

        @CField("country")
        CCharPointer getCountry();

        @CField("country")
        void setCountry(CCharPointer country);

        @CField("bus_")
        BusPointer getBus();

        @CField("bus_")
        void setBus(BusPointer bus);

        GeneratorPointer addressOf(int index);
    }

    @CStruct("load")
    interface LoadPointer extends PointerBase {

        @CField("id")
        CCharPointer getId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("p0")
        double getP0();

        @CField("p0")
        void setP0(double p0);

        @CField("nominal_voltage")
        double getNominalVoltage();

        @CField("nominal_voltage")
        void setNominalVoltage(double nominalVoltage);

        @CField("country")
        CCharPointer getCountry();

        @CField("country")
        void setCountry(CCharPointer country);

        @CField("bus_")
        BusPointer getBus();

        @CField("bus_")
        void setBus(BusPointer bus);

        LoadPointer addressOf(int index);
    }

    @CStruct("load_flow_component_result")
    interface LoadFlowComponentResultPointer extends PointerBase {

        @CField("component_num")
        int geComponentNum();

        @CField("component_num")
        void setComponentNum(int componentNum);

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
    enum ElementType {
        BUS,
        LINE,
        TWO_WINDINGS_TRANSFORMER,
        THREE_WINDINGS_TRANSFORMER,
        GENERATOR,
        LOAD,
        SHUNT_COMPENSATOR,
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
        PHASE_TAP_CHANGER_STEP;

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
        void setInjectionIds(CCharPointerPointer injectionsIds);

        @CField("injections_weights")
        CDoublePointer getInjectionsWeights();

        @CField("injections_weights")
        void setInjectionsWeights(CDoublePointer injectionsWeights);

        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);

        ZonePointer addressOf(int index);
    }

    @CPointerTo(ZonePointer.class)
    interface ZonePointerPointer extends PointerBase {

        void write(ZonePointer value);
    }
}
