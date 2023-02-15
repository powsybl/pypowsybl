/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

typedef struct exception_handler_struct {
    char* message;
} exception_handler;

/**
 * Weakly typed array of data.
 */
typedef struct array_struct {
    void* ptr;
    int length;
} array;

typedef struct network_metadata_struct {
    char* id;
    char* name;
    double case_date; //seconds since epoch
    char* source_format;
    int forecast_distance;
} network_metadata;

typedef struct loadflow_component_result_struct {
    int connected_component_num;
    int synchronous_component_num;
    int status;
    int iteration_count;
    char* slack_bus_id;
    double slack_bus_active_power_mismatch;
    double distributed_active_power;
} loadflow_component_result;

typedef struct loadflow_parameters_struct {
    int voltage_init_mode;
    unsigned char transformer_voltage_control_on;
    unsigned char no_generator_reactive_limits;
    unsigned char phase_shifter_regulation_on;
    unsigned char twt_split_shunt_admittance;
    unsigned char simul_shunt;
    unsigned char read_slack_bus;
    unsigned char write_slack_bus;
    unsigned char distributed_slack;
    int balance_type;
    unsigned char dc_use_transformer_ratio;
    char** countries_to_balance;
    int countries_to_balance_count;
    int connected_component_mode;
    char** provider_parameters_keys;
    int provider_parameters_keys_count;
    char** provider_parameters_values;
    int provider_parameters_values_count;
} loadflow_parameters;

typedef struct loadflow_validation_parameters_struct {
    double threshold;
    double epsilon_x;
    unsigned char verbose;
    char* loadflow_name;
    struct loadflow_parameters_struct loadflow_parameters;
    unsigned char apply_reactance_correction;
    unsigned char ok_missing_values;
    unsigned char no_requirement_if_reactive_bound_inversion;
    unsigned char compare_results;
    unsigned char check_main_component_only;
    unsigned char no_requirement_if_setpoint_outside_power_bounds;
} loadflow_validation_parameters;

typedef struct security_analysis_parameters_struct {
    struct loadflow_parameters_struct loadflow_parameters;
    double flow_proportional_threshold;
    double low_voltage_proportional_threshold;
    double low_voltage_absolute_threshold;
    double high_voltage_proportional_threshold;
    double high_voltage_absolute_threshold;
    char** provider_parameters_keys;
    int provider_parameters_keys_count;
    char** provider_parameters_values;
    int provider_parameters_values_count;
} security_analysis_parameters;

typedef struct sensitivity_analysis_parameters_struct {
    struct loadflow_parameters_struct loadflow_parameters;
    char** provider_parameters_keys;
    int provider_parameters_keys_count;
    char** provider_parameters_values;
    int provider_parameters_values_count;
} sensitivity_analysis_parameters;

typedef struct limit_violation_struct {
    char* subject_id;
    char* subject_name;
    int limit_type;
    double limit;
    char* limit_name;
    int acceptable_duration;
    float limit_reduction;
    double value;
    int side;
} limit_violation;

typedef struct post_contingency_result_struct {
    char* contingency_id;
    int status;
    array limit_violations;
} post_contingency_result;

typedef struct pre_contingency_result_struct {
    int status;
    array limit_violations;
} pre_contingency_result;

typedef enum {
    BUS = 0,
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
    OPERATIONAL_LIMITS,
    MINMAX_REACTIVE_LIMITS,
    ALIAS,
    IDENTIFIABLE,
    INJECTION,
    BRANCH,
    TERMINAL,
} element_type;

typedef enum {
    FLOWS = 0,
    GENERATORS,
    BUSES,
    SVCS,
    SHUNTS,
    TWTS,
    TWTS3W,
} validation_type;

typedef enum {
    EQUIPMENT = 0,
    STEADY_STATE_HYPOTHESIS,
} validation_level_type;

typedef enum {
    ALL = 0,
    NONE,
    SPECIFIC,
} contingency_context_type;

typedef enum {
    VOLTAGE_LEVEL_TOPOLOGY_CREATION = 0,
    CREATE_COUPLING_DEVICE,
} network_modification_type;

typedef struct matrix_struct {
    int row_count;
    int column_count;
    double* values;
} matrix;

typedef struct series_struct {
    char* name;
    unsigned char index;
    int type;
    array data;
} series;

/**
 * A dataframe: simply an array of series.
 */
typedef struct dataframe_struct {
    struct series_struct* series;
    int series_count;
} dataframe;

/**
 * An array of dataframes.
 */
typedef struct {
    dataframe* dataframes;
    int dataframes_count;
} dataframe_array;

/**
 * Metadata about one attribute.
 */
typedef struct series_metadata_struct {
    char* name;
    int type;
    unsigned char  is_index;
    unsigned char  is_modifiable;
    unsigned char  is_default;
} series_metadata;

/**
 * Metadata for one dataframe : simply a list of attributes metadata.
 */
typedef struct {
    series_metadata* attributes_metadata;
    int attributes_count;
} dataframe_metadata;

/**
 * Metadata for a list of dataframes.
 */
typedef struct {
    dataframe_metadata* dataframes_metadata;
    int dataframes_count;
} dataframes_metadata;

typedef struct zone_struct {
    char* id;
    char** injections_ids;
    double* injections_shift_keys;
    int length;
} zone;

typedef enum {
    ALL_ATTRIBUTES = 0,
    DEFAULT_ATTRIBUTES,
    SELECTION_ATTRIBUTES
} filter_attributes_type;

typedef struct flow_decomposition_parameters_struct {
    unsigned char enable_losses_compensation;
    double losses_compensation_epsilon;
    double sensitivity_epsilon;
    unsigned char rescale_enabled;
    unsigned char dc_fallback_enabled_after_ac_divergence;
    int sensitivity_variable_batch_size;
} flow_decomposition_parameters;

typedef struct layout_parameters_struct {
    unsigned char use_name;
    unsigned char center_name;
    unsigned char diagonal_label;
    unsigned char topological_coloring;
    unsigned char nodes_infos;
} layout_parameters;

typedef enum {
    ALPHA_BETA_LOAD = 0,
    ONE_TRANSFORMER_LOAD,
    CURRENT_LIMIT_AUTOMATON,
    GENERATOR_SYNCHRONOUS,
    GENERATOR_SYNCHRONOUS_THREE_WINDINGS,
    GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS,
    GENERATOR_SYNCHRONOUS_FOUR_WINDINGS,
    GENERATOR_SYNCHRONOUS_FOUR_WINDINGS_PROPORTIONAL_REGULATIONS,
} DynamicMappingType;

typedef enum {
    ONE = 0,
    TWO,
} BranchSide;
