/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.commons;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.struct.*;
import org.graalvm.nativeimage.c.type.*;
import org.graalvm.word.PointerBase;

/**
 * Defines java mapping with C structs defined in powsybl-api.h header.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class PyPowsyblApiHeader {

    private PyPowsyblApiHeader() {
    }

    @CStruct("exception_handler")
    public interface ExceptionHandlerPointer extends PointerBase {

        @CField("message")
        CCharPointer geMessage();

        @CField("message")
        void setMessage(CCharPointer message);
    }

    /**
     * Structure containing a pointer to a C array + its length.
     */
    @CStruct("array")
    public interface ArrayPointer<T extends PointerBase> extends PointerBase {

        @CField("ptr")
        T getPtr();

        @CField("ptr")
        void setPtr(T ptr);

        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);

        ArrayPointer addressOf(int index);

    }

    public static <T extends PointerBase> ArrayPointer<T> allocArrayPointer(T ptr, int length) {
        ArrayPointer<T> arrayPtr = UnmanagedMemory.calloc(SizeOf.get(ArrayPointer.class));
        arrayPtr.setPtr(ptr);
        arrayPtr.setLength(length);
        return arrayPtr;
    }

    public static <T extends PointerBase> void freeArrayPointer(ArrayPointer<T> arrayPointer) {
        UnmanagedMemory.free(arrayPointer.getPtr());
        UnmanagedMemory.free(arrayPointer);
    }

    @CStruct("string_map")
    public interface StringMap extends PointerBase {
        @CField("length")
        int getLength();

        @CField("length")
        void setLength(int length);

        @CField("keys")
        CCharPointerPointer getKeys();

        @CField("keys")
        void setKeys(CCharPointerPointer keys);

        @CField("values")
        CCharPointerPointer getValues();

        @CField("values")
        void setValues(CCharPointerPointer values);
    }

    @CStruct("network_metadata")
    public interface NetworkMetadataPointer extends PointerBase {

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

    @CStruct("slack_bus_result")
    public interface SlackBusResultPointer extends PointerBase {

        @CField("id")
        CCharPointer getId();

        @CField("id")
        void setId(CCharPointer id);

        @CField("active_power_mismatch")
        double getActivePowerMismatch();

        @CField("active_power_mismatch")
        void setActivePowerMismatch(double activePowerMismatch);

        SlackBusResultPointer addressOf(int index);
    }

    @CStruct("loadflow_component_result")
    public interface LoadFlowComponentResultPointer extends PointerBase {

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

        @CField("status_text")
        CCharPointer getStatusText();

        @CField("status_text")
        void setStatusText(CCharPointer statusText);

        @CField("iteration_count")
        int getIterationCount();

        @CField("iteration_count")
        void setIterationCount(int iterationCount);

        @CField("reference_bus_id")
        CCharPointer getReferenceBusId();

        @CField("reference_bus_id")
        void setReferenceBusId(CCharPointer referenceBusId);

        @CFieldAddress("slack_bus_results")
        ArrayPointer<SlackBusResultPointer> slackBusResults();

        @CField("distributed_active_power")
        double getDistributedActivePower();

        @CField("distributed_active_power")
        void setDistributedActivePower(double distributedActivePower);

        LoadFlowComponentResultPointer addressOf(int index);
    }

    @CStruct("provider_parameters")
    public interface ProviderParameters extends PointerBase {

        @CField("provider_parameters_keys")
        void setProviderParametersKeys(CCharPointerPointer providerParametersKeys);

        @CField("provider_parameters_keys")
        CCharPointerPointer getProviderParametersKeys();

        @CField("provider_parameters_keys_count")
        int getProviderParametersKeysCount();

        @CField("provider_parameters_keys_count")
        void setProviderParametersKeysCount(int providerParametersKeysCount);

        @CField("provider_parameters_values")
        void setProviderParametersValues(CCharPointerPointer providerParametersValues);

        @CField("provider_parameters_values")
        CCharPointerPointer getProviderParametersValues();

        @CField("provider_parameters_values_count")
        int getProviderParametersValuesCount();

        @CField("provider_parameters_values_count")
        void setProviderParametersValuesCount(int providerParametersKeysCount);
    }

    @CStruct("loadflow_parameters")
    public interface LoadFlowParametersPointer extends PointerBase {

        @CFieldAddress("provider_parameters")
        ProviderParameters getProviderParameters();

        @CField("voltage_init_mode")
        int getVoltageInitMode();

        @CField("voltage_init_mode")
        void setVoltageInitMode(int voltageInitMode);

        @CField("transformer_voltage_control_on")
        boolean isTransformerVoltageControlOn();

        @CField("transformer_voltage_control_on")
        void setTransformerVoltageControlOn(boolean transformerVoltageControlOn);

        @CField("use_reactive_limits")
        boolean isUseReactiveLimits();

        @CField("use_reactive_limits")
        void setUseReactiveLimits(boolean useReactiveLimits);

        @CField("phase_shifter_regulation_on")
        boolean isPhaseShifterRegulationOn();

        @CField("phase_shifter_regulation_on")
        void setPhaseShifterRegulationOn(boolean phaseShifterRegulationOn);

        @CField("twt_split_shunt_admittance")
        boolean isTwtSplitShuntAdmittance();

        @CField("twt_split_shunt_admittance")
        void setTwtSplitShuntAdmittance(boolean twtSplitShuntAdmittance);

        @CField("shunt_compensator_voltage_control_on")
        boolean isShuntCompensatorVoltageControlOn();

        @CField("shunt_compensator_voltage_control_on")
        void setShuntCompensatorVoltageControlOn(boolean shuntCompensatorVoltageControlOn);

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

        @CField("hvdc_ac_emulation")
        boolean isHvdcAcEmulation();

        @CField("hvdc_ac_emulation")
        void setHvdcAcEmulation(boolean hvdcAcEmulation);

        @CField("dc_power_factor")
        double getDcPowerFactor();

        @CField("dc_power_factor")
        void setDcPowerFactor(double dcPowerFactor);
    }

    @CStruct("loadflow_validation_parameters")
    public interface LoadFlowValidationParametersPointer extends PointerBase {

        @CField("threshold")
        double getThreshold();

        @CField("threshold")
        void setThreshold(double threshold);

        @CField("verbose")
        boolean isVerbose();

        @CField("verbose")
        void setVerbose(boolean verbose);

        @CField("loadflow_name")
        CCharPointer getLoadFlowName();

        @CField("loadflow_name")
        void setLoadFlowName(CCharPointer loadFlowName);

        @CField("epsilon_x")
        double getEpsilonX();

        @CField("epsilon_x")
        void setEpsilonX(double epsilonX);

        @CField("apply_reactance_correction")
        boolean isApplyReactanceCorrection();

        @CField("apply_reactance_correction")
        void setApplyReactanceCorrection(boolean applyReactanceCorrection);

        @CFieldAddress("loadflow_parameters")
        LoadFlowParametersPointer getLoadFlowParameters();

        @CField("ok_missing_values")
        boolean isOkMissingValues();

        @CField("ok_missing_values")
        void setOkMissingValues(boolean okMissingValues);

        @CField("no_requirement_if_reactive_bound_inversion")
        boolean isNoRequirementIfReactiveBoundInversion();

        @CField("no_requirement_if_reactive_bound_inversion")
        void setNoRequirementIfReactiveBoundInversion(boolean noRequirementIfReactiveBoundInversion);

        @CField("compare_results")
        boolean isCompareResults();

        @CField("compare_results")
        void setCompareResults(boolean compareResults);

        @CField("check_main_component_only")
        boolean isCheckMainComponentOnly();

        @CField("check_main_component_only")
        void setCheckMainComponentOnly(boolean checkMainComponentOnly);

        @CField("no_requirement_if_setpoint_outside_power_bounds")
        boolean isNoRequirementIfSetpointOutsidePowerBounds();

        @CField("no_requirement_if_setpoint_outside_power_bounds")
        void setNoRequirementIfSetpointOutsidePowerBounds(boolean noRequirementIfSetpointOutsidePowerBounds);
    }

    @CStruct("security_analysis_parameters")
    public interface SecurityAnalysisParametersPointer extends PointerBase {

        @CFieldAddress("provider_parameters")
        ProviderParameters getProviderParameters();

        @CFieldAddress("loadflow_parameters")
        LoadFlowParametersPointer getLoadFlowParameters();

        @CField("flow_proportional_threshold")
        double getFlowProportionalThreshold();

        @CField("flow_proportional_threshold")
        void setFlowProportionalThreshold(double flowProportionalThreshold);

        @CField("low_voltage_proportional_threshold")
        double getLowVoltageProportionalThreshold();

        @CField("low_voltage_proportional_threshold")
        void setLowVoltageProportionalThreshold(double lowVoltageProportionalThreshold);

        @CField("low_voltage_absolute_threshold")
        double getLowVoltageAbsoluteThreshold();

        @CField("low_voltage_absolute_threshold")
        void setLowVoltageAbsoluteThreshold(double lowVoltageAbsoluteThreshold);

        @CField("high_voltage_proportional_threshold")
        double getHighVoltageProportionalThreshold();

        @CField("high_voltage_proportional_threshold")
        void setHighVoltageProportionalThreshold(double highVoltageProportionalThreshold);

        @CField("high_voltage_absolute_threshold")
        double getHighVoltageAbsoluteThreshold();

        @CField("high_voltage_absolute_threshold")
        void setHighVoltageAbsoluteThreshold(double highVoltageAbsoluteThreshold);
    }

    @CStruct("sensitivity_analysis_parameters")
    public interface SensitivityAnalysisParametersPointer extends PointerBase {

        @CFieldAddress("provider_parameters")
        ProviderParameters getProviderParameters();

        @CFieldAddress("loadflow_parameters")
        LoadFlowParametersPointer getLoadFlowParameters();
    }

    @CStruct("limit_violation")
    public interface LimitViolationPointer extends PointerBase {

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
        double getLimitReduction();

        @CField("limit_reduction")
        void setLimitReduction(double limitReduction);

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

    @CStruct("pre_contingency_result")
    public interface PreContingencyResultPointer extends PointerBase {

        @CField("status")
        int getStatus();

        @CField("status")
        void setStatus(int status);

        @CFieldAddress("limit_violations")
        ArrayPointer<LimitViolationPointer> limitViolations();
    }

    @CStruct("post_contingency_result")
    public interface PostContingencyResultPointer extends PointerBase {

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

        PostContingencyResultPointer addressOf(int index);
    }

    @CStruct("operator_strategy_result")
    public interface OperatorStrategyResultPointer extends PointerBase {

        @CField("operator_strategy_id")
        CCharPointer getOperatorStrategyId();

        @CField("operator_strategy_id")
        void setOperatorStrategyId(CCharPointer contingencyId);

        @CField("status")
        int getStatus();

        @CField("status")
        void setStatus(int status);

        @CFieldAddress("limit_violations")
        ArrayPointer<LimitViolationPointer> limitViolations();

        OperatorStrategyResultPointer addressOf(int index);
    }

    @CEnum("element_type")
    public enum ElementType {
        BUS,
        BUS_FROM_BUS_BREAKER_VIEW,
        LINE,
        TWO_WINDINGS_TRANSFORMER,
        THREE_WINDINGS_TRANSFORMER,
        GENERATOR,
        LOAD,
        GROUND,
        BATTERY,
        SHUNT_COMPENSATOR,
        NON_LINEAR_SHUNT_COMPENSATOR_SECTION,
        LINEAR_SHUNT_COMPENSATOR_SECTION,
        DANGLING_LINE,
        DANGLING_LINE_GENERATION,
        TIE_LINE,
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
        OPERATIONAL_LIMITS,
        SELECTED_OPERATIONAL_LIMITS,
        MINMAX_REACTIVE_LIMITS,
        ALIAS,
        IDENTIFIABLE,
        INJECTION,
        BRANCH,
        TERMINAL,
        SUB_NETWORK,
        AREA,
        AREA_VOLTAGE_LEVELS,
        AREA_BOUNDARIES,
        INTERNAL_CONNECTION,
        PROPERTIES;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ElementType fromCValue(int value);
    }

    @CEnum("validation_type")
    public enum ValidationType {
        FLOWS,
        GENERATORS,
        BUSES,
        SVCS,
        SHUNTS,
        TWTS,
        TWTS3W;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ValidationType fromCValue(int value);
    }

    @CEnum("network_modification_type")
    public enum NetworkModificationType {
        VOLTAGE_LEVEL_TOPOLOGY_CREATION,
        CREATE_COUPLING_DEVICE,
        CREATE_FEEDER_BAY,
        CREATE_LINE_FEEDER,
        CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER,
        CREATE_LINE_ON_LINE,
        REVERT_CREATE_LINE_ON_LINE,
        CONNECT_VOLTAGE_LEVEL_ON_LINE,
        REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE,
        REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native NetworkModificationType fromCValue(int value);
    }

    @CEnum("violation_type")
    public enum LimitViolationType {
        ACTIVE_POWER,
        APPARENT_POWER,
        CURRENT,
        LOW_VOLTAGE,
        HIGH_VOLTAGE,
        LOW_SHORT_CIRCUIT_CURRENT,
        HIGH_SHORT_CIRCUIT_CURRENT,
        OTHER;
        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native LimitViolationType fromCValue(int value);
    }

    @CEnum("remove_modification_type")
    public enum RemoveModificationType {
        REMOVE_FEEDER,
        REMOVE_VOLTAGE_LEVEL,
        REMOVE_HVDC_LINE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native RemoveModificationType fromCValue(int value);
    }

    @CEnum("sensitivity_function_type")
    public enum SensitivityFunctionType {
        BRANCH_ACTIVE_POWER_1,
        BRANCH_CURRENT_1,
        BRANCH_REACTIVE_POWER_1,
        BRANCH_ACTIVE_POWER_2,
        BRANCH_CURRENT_2,
        BRANCH_REACTIVE_POWER_2,
        BRANCH_ACTIVE_POWER_3,
        BRANCH_CURRENT_3,
        BRANCH_REACTIVE_POWER_3,
        BUS_REACTIVE_POWER,
        BUS_VOLTAGE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native SensitivityFunctionType fromCValue(int value);
    }

    @CEnum("sensitivity_variable_type")
    public enum SensitivityVariableType {
        AUTO_DETECT,
        INJECTION_ACTIVE_POWER,
        INJECTION_REACTIVE_POWER,
        TRANSFORMER_PHASE,
        BUS_TARGET_VOLTAGE,
        HVDC_LINE_ACTIVE_POWER,
        TRANSFORMER_PHASE_1,
        TRANSFORMER_PHASE_2,
        TRANSFORMER_PHASE_3;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native SensitivityVariableType fromCValue(int value);
    }

    @CStruct("matrix")
    public interface MatrixPointer extends PointerBase {

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
    public interface SeriesPointer extends PointerBase {

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

    @CStruct("dataframe")
    public interface DataframePointer extends PointerBase {

        @CField("series")
        SeriesPointer getSeries();

        @CField("series")
        void setSeries(SeriesPointer series);

        @CField("series_count")
        int getSeriesCount();

        @CField("series_count")
        void setSeriesCount(int count);

        DataframePointer addressOf(int index);
    }

    @CStruct("dataframe_array")
    public interface DataframeArrayPointer extends PointerBase {

        @CField("dataframes")
        DataframePointer getDataframes();

        @CField("dataframes")
        void setDataframes(DataframePointer series);

        @CField("dataframes_count")
        int getDataframesCount();

        @CField("dataframes_count")
        void setDataframesCount(int count);
    }

    /*
    typedef struct series_metadata_struct {
        char* name;
        int type;
        unsigned char  is_index;
        unsigned char  is_modifiable;
    } series_metadata;
     */
    @CStruct("series_metadata")
    public interface SeriesMetadataPointer extends PointerBase {

        @CField("name")
        CCharPointer getName();

        @CField("name")
        void setName(CCharPointer name);

        @CField("type")
        int getType();

        @CField("type")
        void setType(int type);

        @CField("is_index")
        boolean isIndex();

        @CField("is_index")
        void setIndex(boolean index);

        @CField("is_modifiable")
        boolean isModifiable();

        @CField("is_modifiable")
        void setModifiable(boolean index);

        @CField("is_default")
        boolean isDefault();

        @CField("is_default")
        void setDefault(boolean attDefault);

        SeriesMetadataPointer addressOf(int index);
    }

    @CStruct("dataframe_metadata")
    public interface DataframeMetadataPointer extends PointerBase {

        @CField("attributes_count")
        int getAttributesCount();

        @CField("attributes_count")
        void setAttributesCount(int count);

        @CField("attributes_metadata")
        SeriesMetadataPointer getAttributesMetadata();

        @CField("attributes_metadata")
        void setAttributesMetadata(SeriesMetadataPointer metadata);

        /**
         * When used as a C array, get the struct at i-th position.
         */
        DataframeMetadataPointer addressOf(int index);
    }

    @CStruct("dataframes_metadata")
    public interface DataframesMetadataPointer extends PointerBase {

        @CField("dataframes_count")
        int getDataframesCount();

        @CField("dataframes_count")
        void setDataframesCount(int count);

        @CField("dataframes_metadata")
        DataframeMetadataPointer getDataframesMetadata();

        @CField("dataframes_metadata")
        void setDataframesMetadata(DataframeMetadataPointer metadata);
    }

    @CStruct("zone")
    public interface ZonePointer extends PointerBase {

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
    public interface ZonePointerPointer extends PointerBase {

        ZonePointer read(int index);
    }

    @CPointerTo(VoidPointer.class)
    public interface VoidPointerPointer extends PointerBase {

        ObjectHandle read(int index);
    }

    @CEnum("contingency_context_type")
    public enum RawContingencyContextType {

        ALL,
        NONE,
        SPECIFIC,
        ONLY_CONTINGENCIES;

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

    @CEnum("validation_level_type")
    public enum ValidationLevelType {
        EQUIPMENT,
        STEADY_STATE_HYPOTHESIS;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ValidationLevelType fromCValue(int value);
    }

    @CStruct("flow_decomposition_parameters")
    public interface FlowDecompositionParametersPointer extends PointerBase {
        @CField("enable_losses_compensation")
        boolean isLossesCompensationEnabled();

        @CField("enable_losses_compensation")
        void setEnableLossesCompensation(boolean enableLossesCompensation);

        @CField("losses_compensation_epsilon")
        double getLossesCompensationEpsilon();

        @CField("losses_compensation_epsilon")
        void setLossesCompensationEpsilon(double lossesCompensationEpsilon);

        @CField("sensitivity_epsilon")
        double getSensitivityEpsilon();

        @CField("sensitivity_epsilon")
        void setSensitivityEpsilon(double sensitivityEpsilon);

        @CField("rescale_mode")
        int getRescaleMode();

        @CField("rescale_mode")
        void setRescaleMode(int rescaleMode);

        @CField("dc_fallback_enabled_after_ac_divergence")
        boolean isDcFallbackEnabledAfterAcDivergence();

        @CField("dc_fallback_enabled_after_ac_divergence")
        void setDcFallbackEnabledAfterAcDivergence(boolean dcFallbackEnabledAfterAcDivergence);

        @CField("sensitivity_variable_batch_size")
        int getSensitivityVariableBatchSize();

        @CField("sensitivity_variable_batch_size")
        void setSensitivityVariableBatchSize(int sensitivityVariableBatchSize);
    }

    @CStruct("sld_parameters")
    public interface SldParametersPointer extends PointerBase {

        @CField("use_name")
        boolean isUseName();

        @CField("use_name")
        void setUseName(boolean useName);

        @CField("center_name")
        boolean isCenterName();

        @CField("center_name")
        void setCenterName(boolean centerName);

        @CField("diagonal_label")
        boolean isDiagonalLabel();

        @CField("diagonal_label")
        void setDiagonalLabel(boolean diagonalLabel);

        @CField("nodes_infos")
        boolean isBusesLegendAdded();

        @CField("nodes_infos")
        void setBusesLegendAdded(boolean addNodeInfos);

        @CField("tooltip_enabled")
        void setTooltipEnabled(boolean tooltipEnabled);

        @CField("tooltip_enabled")
        boolean getTooltipEnabled();

        @CField("topological_coloring")
        boolean isTopologicalColoring();

        @CField("topological_coloring")
        void setTopologicalColoring(boolean topologicalColoring);

        @CField("component_library")
        CCharPointer getComponentLibrary();

        @CField("component_library")
        void setComponentLibrary(CCharPointer componentLibrary);

        @CField("display_current_feeder_info")
        boolean isDisplayCurrentFeederInfo();

        @CField("display_current_feeder_info")
        void setDisplayCurrentFeederInfo(boolean displayCurrentInfo);

        @CField("active_power_unit")
        CCharPointer getActivePowerUnit();

        @CField("active_power_unit")
        void setActivePowerUnit(CCharPointer activePowerUnit);

        @CField("reactive_power_unit")
        CCharPointer getReactivePowerUnit();

        @CField("reactive_power_unit")
        void setReactivePowerUnit(CCharPointer reactivePowerUnit);

        @CField("current_unit")
        CCharPointer getCurrentUnit();

        @CField("current_unit")
        void setCurrentUnit(CCharPointer currentUnit);

    }

    @CStruct("nad_parameters")
    public interface NadParametersPointer extends PointerBase {
        @CField("edge_name_displayed")
        void setEdgeNameDisplayed(boolean edgeNameDisplayed);

        @CField("edge_name_displayed")
        boolean isEdgeNameDisplayed();

        @CField("id_displayed")
        void setIdDisplayed(boolean idDisplayed);

        @CField("id_displayed")
        boolean isIdDisplayed();

        @CField("edge_info_along_edge")
        void setEdgeInfoAlongEdge(boolean edgeInfoAlongEdge);

        @CField("edge_info_along_edge")
        boolean isEdgeInfoAlongEdge();

        @CField("power_value_precision")
        void setPowerValuePrecision(int powerValuePrecision);

        @CField("power_value_precision")
        int getPowerValuePrecision();

        @CField("current_value_precision")
        void setCurrentValuePrecision(int currentValuePrecision);

        @CField("current_value_precision")
        int getCurrentValuePrecision();

        @CField("angle_value_precision")
        void setAngleValuePrecision(int angleValuePrecision);

        @CField("angle_value_precision")
        int getAngleValuePrecision();

        @CField("voltage_value_precision")
        void setVoltageValuePrecision(int voltageValuePrecision);

        @CField("voltage_value_precision")
        int getVoltageValuePrecision();

        @CField("bus_legend")
        void setBusLegend(boolean busLegend);

        @CField("bus_legend")
        boolean isBusLegend();

        @CField("substation_description_displayed")
        void setSubstationDescriptionDisplayed(boolean substationDescriptionDisplayed);

        @CField("substation_description_displayed")
        boolean isSubstationDescriptionDisplayed();

        @CField("layout_type")
        void setLayoutType(int layoutType);

        @CField("layout_type")
        int getLayoutType();

        @CField("scaling_factor")
        int getScalingFactor();

        @CField("scaling_factor")
        void setScalingFactor(int scalingFactor);

        @CField("radius_factor")
        double getRadiusFactor();

        @CField("radius_factor")
        void setRadiusFactor(double radiusFactor);

        @CField("edge_info_displayed")
        void setEdgeInfoDisplayed(int edgeInfoDisplayed);

        @CField("edge_info_displayed")
        int getEdgeInfoDisplayed();

        @CField("voltage_level_details")
        void setVoltageLevelDetails(boolean isVoltageLevelDetails);

        @CField("voltage_level_details")
        boolean isVoltageLevelDetails();
    }

    @CEnum("DynamicMappingType")
    public enum DynamicMappingType {
        BASE_LOAD,
        LOAD_ONE_TRANSFORMER,
        LOAD_ONE_TRANSFORMER_TAP_CHANGER,
        LOAD_TWO_TRANSFORMERS,
        LOAD_TWO_TRANSFORMERS_TAP_CHANGERS,
        BASE_GENERATOR,
        SYNCHRONIZED_GENERATOR,
        SYNCHRONOUS_GENERATOR,
        WECC,
        GRID_FORMING_CONVERTER,
        SIGNAL_N_GENERATOR,
        HVDC_P,
        HVDC_VSC,
        BASE_TRANSFORMER,
        BASE_STATIC_VAR_COMPENSATOR,
        BASE_LINE,
        BASE_BUS,
        INFINITE_BUS,
        OVERLOAD_MANAGEMENT_SYSTEM,
        TWO_LEVEL_OVERLOAD_MANAGEMENT_SYSTEM,
        UNDER_VOLTAGE,
        PHASE_SHIFTER_I,
        PHASE_SHIFTER_P,
        PHASE_SHIFTER_BLOCKING_I,
        TAP_CHANGER,
        TAP_CHANGER_BLOCKING;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native DynamicMappingType fromCValue(int value);
    }

    @CEnum("EventMappingType")
    public enum EventMappingType {
        DISCONNECT,
        NODE_FAULT,
        ACTIVE_POWER_VARIATION;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native EventMappingType fromCValue(int value);
    }

    @CEnum("OutputVariableType")
    public enum OutputVariableType {
        CURVE,
        FINAL_STATE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native OutputVariableType fromCValue(int value);
    }

    @CEnum("DynamicSimulationStatus")
    public enum DynamicSimulationStatus {
        DYNAMIC_SIMULATION_SUCCESS,
        DYNAMIC_SIMULATION_FAILURE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native DynamicSimulationStatus fromCValue(int value);
    }

    @CEnum("ThreeSide")
    public enum ThreeSideType {
        UNDEFINED,
        ONE,
        TWO,
        THREE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ThreeSideType fromCValue(int value);
    }

    @CEnum("condition_type")
    public enum ConditionType {
        TRUE_CONDITION,
        ALL_VIOLATION_CONDITION,
        ANY_VIOLATION_CONDITION,
        AT_LEAST_ONE_VIOLATION_CONDITION;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native ConditionType fromCValue(int value);
    }

    @CStruct("shortcircuit_analysis_parameters")
    public interface ShortCircuitAnalysisParametersPointer extends PointerBase {

        @CFieldAddress("provider_parameters")
        ProviderParameters getProviderParameters();

        @CField("with_voltage_result")
        boolean isWithVoltageResult();

        @CField("with_voltage_result")
        void setWithVoltageResult(boolean withVoltageResult);

        @CField("with_feeder_result")
        boolean isWithFeederResult();

        @CField("with_feeder_result")
        void setWithFeederResult(boolean withFeederResult);

        @CField("with_limit_violations")
        boolean isWithLimitViolations();

        @CField("with_limit_violations")
        void setWithLimitViolations(boolean withLimitViolations);

        @CField("study_type")
        int getStudyType();

        @CField("study_type")
        void setStudyType(int studyType);

        @CField("with_fortescue_result")
        boolean isWithFortescueResult();

        @CField("with_fortescue_result")
        void setWithFortescueResult(boolean withFortescueResult);

        @CField("min_voltage_drop_proportional_threshold")
        double getMinVoltageDropProportionalThreshold();

        @CField("min_voltage_drop_proportional_threshold")
        void setMinVoltageDropProportionalThreshold(double minVoltageDropProportionalThreshold);

        @CField("initial_voltage_profile_mode")
        int getInitialVoltageProfileMode();

        @CField("initial_voltage_profile_mode")
        void setInitialVoltageProfileMode(int initialVoltageProfileMode);
    }

    @CEnum("VoltageInitializerObjective")
    public enum VoltageInitializerObjective {
        MIN_GENERATION,
        BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT,
        SPECIFIC_VOLTAGE_PROFILE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native VoltageInitializerObjective fromCValue(int value);
    }

    @CEnum("VoltageInitializerLogLevelAmpl")
    public enum VoltageInitializerLogLevelAmpl {
        LOG_AMPL_DEBUG,
        LOG_AMPL_INFO,
        LOG_AMPL_WARNING,
        LOG_AMPL_ERROR;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native VoltageInitializerLogLevelAmpl fromCValue(int value);
    }

    @CEnum("VoltageInitializerLogLevelSolver")
    public enum VoltageInitializerLogLevelSolver {
        NOTHING,
        ONLY_RESULTS,
        EVERYTHING;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native VoltageInitializerLogLevelSolver fromCValue(int value);
    }

    @CEnum("VoltageInitializerReactiveSlackBusesMode")
    public enum VoltageInitializerReactiveSlackBusesMode {
        CONFIGURED,
        NO_GENERATION,
        ALL_BUSES;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native VoltageInitializerReactiveSlackBusesMode fromCValue(int value);
    }

    @CEnum("VoltageInitializerStatus")
    public enum VoltageInitializerStatus {
        OK,
        NOT_OK;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native VoltageInitializerStatus fromCValue(int value);
    }

    @CEnum("RaoComputationStatus")
    public enum RaoComputationStatus {
        DEFAULT,
        FAILURE,
        PARTIAL_FAILURE;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native RaoComputationStatus fromCValue(int value);
    }

    @CStruct("rao_parameters")
    public interface RaoParametersPointer extends PointerBase {

        // Objective function parameters
        @CFieldAddress("provider_parameters")
        ProviderParameters getProviderParameters();

        @CField("objective_function_type")
        int getObjectiveFunctionType();

        @CField("objective_function_type")
        void setObjectiveFunctionType(int type);

        @CField("enforce_curative_security")
        boolean getEnforceCurativeSecurity();

        @CField("enforce_curative_security")
        void setEnforceCurativeSecurity(boolean enforceCurativeSecurity);

        @CField("unit")
        int getUnit();

        @CField("unit")
        void setUnit(int unit);

        @CField("curative_min_obj_improvement")
        double getCurativeMinObjImprovement();

        @CField("curative_min_obj_improvement")
        void setCurativeMinObjImprovement(double curativeMinObjImprovement);

        // Range action optim parameters (solver)
        @CField("solver")
        int getSolver();

        @CField("solver")
        void setSolver(int solver);

        @CField("relative_mip_gap")
        double getRelativeMipGap();

        @CField("relative_mip_gap")
        void setRelativeMipGap(double relativeMipGap);

        @CField("solver_specific_parameters")
        CCharPointer getSolverSpecificParameters();

        @CField("solver_specific_parameters")
        void setSolverSpecificParameters(CCharPointer solverSpecificParameters);

        // Range action optim parameters
        @CField("pst_ra_min_impact_threshold")
        double getPstRaMinImpactThreshold();

        @CField("pst_ra_min_impact_threshold")
        void setPstRaMinImpactThreshold(double pstRaMinImpactThreshold);

        @CField("hvdc_ra_min_impact_threshold")
        double getHvdcRaMinImpactThreshold();

        @CField("hvdc_ra_min_impact_threshold")
        void setHvdcRaMinImpactThreshold(double hvdcRaMinImpactThreshold);

        @CField("injection_ra_min_impact_threshold")
        double getInjectionRaMinImpactThreshold();

        @CField("injection_ra_min_impact_threshold")
        void setInjectionRaMinImpactThreshold(double injectionRaMinImpactThreshold);

        @CField("max_mip_iterations")
        int getMaxMipIterations();

        @CField("max_mip_iterations")
        void setMaxMipIterations(int maxMipIterations);

        @CField("pst_sensitivity_threshold")
        double getPstSensitivityThreshold();

        @CField("pst_sensitivity_threshold")
        void setPstSensitivityThreshold(double pstSensitivityThreshold);

        @CField("hvdc_sensitivity_threshold")
        double getHvdcSensitivityThreshold();

        @CField("hvdc_sensitivity_threshold")
        void setHvdcSensitivityThreshold(double pstSensitivityThreshold);

        @CField("pst_model")
        int getPstModel();

        @CField("pst_model")
        void setPstModel(int pstModel);

        @CField("injection_ra_sensitivity_threshold")
        double getInjectionRaSensitivityThreshold();

        @CField("injection_ra_sensitivity_threshold")
        void setInjectionRaSensitivityThreshold(double injectionRaSensitivityThreshold);

        @CField("ra_range_shrinking")
        int getRaRangeShrinking();

        @CField("ra_range_shrinking")
        void setRaRangeShrinking(int raRangeShrinking);

        // Topo optimization parameters
        @CField("relative_min_impact_threshold")
        double getRelativeMinImpactThreshold();

        @CField("relative_min_impact_threshold")
        void setRelativeMinImpactThreshold(double threshold);

        @CField("absolute_min_impact_threshold")
        double getAbsoluteMinImpactThreshold();

        @CField("absolute_min_impact_threshold")
        void setAbsoluteMinImpactThreshold(double threshold);

        @CField("max_preventive_search_tree_depth")
        int getMaxPreventiveSearchTreeDepth();

        @CField("max_preventive_search_tree_depth")
        void setMaxPreventiveSearchTreeDepth(int depth);

        @CField("max_curative_search_tree_depth")
        int getMaxCurativeSearchTreeDepth();

        @CField("max_curative_search_tree_depth")
        void setMaxCurativeSearchTreeDepth(int depth);

        @CFieldAddress("predefined_combinations")
        ArrayPointer<ArrayPointer<CCharPointerPointer>> getPredefinedCombinations();

        @CField("skip_actions_far_from_most_limiting_element")
        boolean getSkipActionsFarFromMostLimitingElement();

        @CField("skip_actions_far_from_most_limiting_element")
        void setSkipActionsFarFromMostLimitingElement(boolean skip);

        @CField("max_number_of_boundaries_for_skipping_actions")
        int getMaxNumberOfBoundariesForSkippingActions();

        @CField("max_number_of_boundaries_for_skipping_actions")
        void setMaxNumberOfBoundariesForSkippingActions(int boundaries);

        // Multithreading parameters
        @CField("available_cpus")
        int getAvailableCpus();

        @CField("available_cpus")
        void setAvailableCpus(int availableCpus);

        // Second preventive parameters
        @CField("execution_condition")
        int getExecutionCondition();

        @CField("execution_condition")
        void setExecutionCondition(int executionCondition);

        @CField("re_optimize_curative_range_actions")
        boolean getReOptimizeCurativeRangeActions();

        @CField("re_optimize_curative_range_actions")
        void setReOptimizeCurativeRangeActions(boolean reOptimizeCurativeRangeActions);

        @CField("hint_from_first_preventive_rao")
        boolean getHintFromFirstPreventiveRao();

        @CField("hint_from_first_preventive_rao")
        void setHintFromFirstPreventiveRao(boolean hintFromFirstPreventiveRao);

        @CField("do_not_optimize_curative_cnecs_for_tsos_without_cras")
        boolean getDoNotOptimizeCurativeCnecsForTsosWithoutCras();

        @CField("do_not_optimize_curative_cnecs_for_tsos_without_cras")
        void setDoNotOptimizeCurativeCnecsForTsosWithoutCras(boolean doNotOptimizeCurativeCnecsForTsosWithoutCras);

        @CField("load_flow_provider")
        CCharPointer getLoadFlowProvider();

        @CField("load_flow_provider")
        void setLoadFlowProvider(CCharPointer loadFlowProvider);

        @CField("sensitivity_provider")
        CCharPointer getSensitivityProvider();

        @CField("sensitivity_provider")
        void setSensitivityProvider(CCharPointer sensitivityProvider);

        @CField("sensitivity_parameters")
        SensitivityAnalysisParametersPointer getSensitivityParameters();

        @CField("sensitivity_parameters")
        void setSensitivityParameters(SensitivityAnalysisParametersPointer sensitivityParameters);

        @CField("sensitivity_failure_overcost")
        double getSensitivityFailureOvercost();

        @CField("sensitivity_failure_overcost")
        void setSensitivityFailureOvercost(double sensitivityFailureOvercost);
    }

}
