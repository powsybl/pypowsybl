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

typedef struct string_map_struct {
    int length;
    char** keys;
    char** values;
} string_map;

typedef struct network_metadata_struct {
    char* id;
    char* name;
    double case_date; //seconds since epoch
    char* source_format;
    int forecast_distance;
} network_metadata;

typedef struct slack_bus_result_struct {
    char* id;
    double active_power_mismatch;
} slack_bus_result;

typedef struct loadflow_component_result_struct {
    int connected_component_num;
    int synchronous_component_num;
    int status;
    char* status_text;
    int iteration_count;
    char* reference_bus_id;
    array slack_bus_results;
    double distributed_active_power;
} loadflow_component_result;

typedef struct loadflow_parameters_struct {
    int voltage_init_mode;
    unsigned char transformer_voltage_control_on;
    unsigned char use_reactive_limits;
    unsigned char phase_shifter_regulation_on;
    unsigned char twt_split_shunt_admittance;
    unsigned char shunt_compensator_voltage_control_on;
    unsigned char read_slack_bus;
    unsigned char write_slack_bus;
    unsigned char distributed_slack;
    int balance_type;
    unsigned char dc_use_transformer_ratio;
    char** countries_to_balance;
    int countries_to_balance_count;
    int connected_component_mode;
    double dc_power_factor;
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
    double limit_reduction;
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

typedef struct operator_strategy_result_struct {
    char* operator_strategy_id;
    int status;
    array limit_violations;
} operator_strategy_result;

typedef enum {
    BUS = 0,
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
    PROPERTIES
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
    ACTIVE_POWER = 0,
    APPARENT_POWER,
    CURRENT,
    LOW_VOLTAGE,
    HIGH_VOLTAGE,
    LOW_SHORT_CIRCUIT_CURRENT,
    HIGH_SHORT_CIRCUIT_CURRENT,
    OTHER,
} violation_type;

typedef enum {
    TRUE_CONDITION = 0,
    ALL_VIOLATION_CONDITION,
    ANY_VIOLATION_CONDITION,
    AT_LEAST_ONE_VIOLATION_CONDITION,
} condition_type;

typedef enum {
    EQUIPMENT = 0,
    STEADY_STATE_HYPOTHESIS,
} validation_level_type;

typedef enum {
    ALL = 0,
    NONE,
    SPECIFIC,
    ONLY_CONTINGENCIES,
} contingency_context_type;

typedef enum {
    BRANCH_ACTIVE_POWER_1=0,
    BRANCH_CURRENT_1,
    BRANCH_REACTIVE_POWER_1,
    BRANCH_ACTIVE_POWER_2,
    BRANCH_CURRENT_2,
    BRANCH_REACTIVE_POWER_2,
    BRANCH_ACTIVE_POWER_3,
    BRANCH_CURRENT_3,
    BRANCH_REACTIVE_POWER_3,
    BUS_REACTIVE_POWER,
    BUS_VOLTAGE,
} sensitivity_function_type;

typedef enum {
    AUTO_DETECT=0,
    INJECTION_ACTIVE_POWER,
    INJECTION_REACTIVE_POWER,
    TRANSFORMER_PHASE,
    BUS_TARGET_VOLTAGE,
    HVDC_LINE_ACTIVE_POWER,
    TRANSFORMER_PHASE_1,
    TRANSFORMER_PHASE_2,
    TRANSFORMER_PHASE_3,
} sensitivity_variable_type;

typedef enum {
    VOLTAGE_LEVEL_TOPOLOGY_CREATION = 0,
    CREATE_COUPLING_DEVICE,
    CREATE_FEEDER_BAY,
    CREATE_LINE_FEEDER,
    CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER,
    CREATE_LINE_ON_LINE,
    REVERT_CREATE_LINE_ON_LINE,
    CONNECT_VOLTAGE_LEVEL_ON_LINE,
    REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE,
    REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE,
} network_modification_type;

typedef enum {
    REMOVE_FEEDER = 0,
    REMOVE_VOLTAGE_LEVEL,
    REMOVE_HVDC_LINE,
} remove_modification_type;

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
    int rescale_mode;
    unsigned char dc_fallback_enabled_after_ac_divergence;
    int sensitivity_variable_batch_size;
} flow_decomposition_parameters;

typedef struct sld_parameters_struct {
    unsigned char use_name;
    unsigned char center_name;
    unsigned char diagonal_label;
    unsigned char nodes_infos;
    unsigned char tooltip_enabled;
    unsigned char topological_coloring;
    char* component_library;
    unsigned char display_current_feeder_info;
    char* active_power_unit;
    char* reactive_power_unit;
    char* current_unit;
} sld_parameters;

typedef struct nad_parameters_struct {
    unsigned char edge_name_displayed;
    unsigned char edge_info_along_edge;
    unsigned char id_displayed;
    int power_value_precision;
    int current_value_precision;
    int angle_value_precision;
    int voltage_value_precision;
    unsigned char substation_description_displayed;
    unsigned char bus_legend;
    int layout_type;
    int scaling_factor;
    double radius_factor;
    int edge_info_displayed;
} nad_parameters;

typedef enum {
    BASE_LOAD = 0,
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
    TAP_CHANGER_BLOCKING,
} DynamicMappingType;

typedef enum {
    DISCONNECT = 0,
    NODE_FAULT,
    ACTIVE_POWER_VARIATION,
} EventMappingType;

typedef enum {
    CURVE = 0,
    FINAL_STATE,
} OutputVariableType;

typedef enum {
    DYNAMIC_SIMULATION_SUCCESS = 0,
    DYNAMIC_SIMULATION_FAILURE,
} DynamicSimulationStatus;

typedef enum {
    UNDEFINED = -1,
    ONE,
    TWO,
    THREE,
} ThreeSide;

typedef struct shortcircuit_analysis_parameters_struct {
    unsigned char with_voltage_result;
    unsigned char with_feeder_result;
    unsigned char with_limit_violations;
    int study_type;
    unsigned char with_fortescue_result;
    double min_voltage_drop_proportional_threshold;
    int initial_voltage_profile_mode;
    char** provider_parameters_keys;
    int provider_parameters_keys_count;
    char** provider_parameters_values;
    int provider_parameters_values_count;
} shortcircuit_analysis_parameters;

typedef enum {
    OK = 0,
    NOT_OK,
} VoltageInitializerStatus;

typedef enum {
    MIN_GENERATION = 0,
    BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT,
    SPECIFIC_VOLTAGE_PROFILE,
} VoltageInitializerObjective;

typedef enum {
    LOG_AMPL_DEBUG = 0,
    LOG_AMPL_INFO,
    LOG_AMPL_WARNING,
    LOG_AMPL_ERROR,
} VoltageInitializerLogLevelAmpl;

typedef enum {
    NOTHING = 0,
    ONLY_RESULTS,
    EVERYTHING,
} VoltageInitializerLogLevelSolver;

typedef enum {
    CONFIGURED = 0,
    NO_GENERATION,
    ALL_BUSES,
} VoltageInitializerReactiveSlackBusesMode;

typedef enum {
    DEFAULT = 0,
    FAILURE,
} RaoComputationStatus;

typedef enum {
    VOLTAGE_LEVEL_NAME = 0,
    LOAD_NAME,
    GENERATOR_NAME,
    SHUNT_NAME,
    BRANCH_NAME,
} Grid2opStringValueType;

typedef enum {
    LOAD_VOLTAGE_LEVEL_NUM = 0,
    GENERATOR_VOLTAGE_LEVEL_NUM,
    SHUNT_VOLTAGE_LEVEL_NUM,
    BRANCH_VOLTAGE_LEVEL_NUM_1,
    BRANCH_VOLTAGE_LEVEL_NUM_2,
    SHUNT_LOCAL_BUS,
    TOPO_VECT,
} Grid2opIntegerValueType;

typedef enum {
    LOAD_P = 0,
    LOAD_Q,
    LOAD_V,
    LOAD_ANGLE,
    GENERATOR_P,
    GENERATOR_Q,
    GENERATOR_V,
    GENERATOR_ANGLE,
    SHUNT_P,
    SHUNT_Q,
    SHUNT_V,
    SHUNT_ANGLE,
    BRANCH_P1,
    BRANCH_P2,
    BRANCH_Q1,
    BRANCH_Q2,
    BRANCH_V1,
    BRANCH_V2,
    BRANCH_ANGLE1,
    BRANCH_ANGLE2,
    BRANCH_I1,
    BRANCH_I2,
    BRANCH_PERMANENT_LIMIT_A,
} Grid2opDoubleValueType;

typedef enum {
    UPDATE_LOAD_P = 0,
    UPDATE_LOAD_Q,
    UPDATE_GENERATOR_P,
    UPDATE_GENERATOR_V,
} Grid2opUpdateDoubleValueType;


typedef enum {
    UPDATE_LOAD_BUS = 0,
    UPDATE_GENERATOR_BUS,
    UPDATE_SHUNT_BUS,
    UPDATE_BRANCH_BUS1,
    UPDATE_BRANCH_BUS2,
} Grid2opUpdateIntegerValueType;
