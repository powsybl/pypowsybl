/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

typedef struct exception_handler_struct {
    char* message;
} exception_handler;

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

typedef struct load_flow_component_result_struct {
    int connected_component_num;
    int synchronous_component_num;
    int status;
    int iteration_count;
    char* slack_bus_id;
    double slack_bus_active_power_mismatch;
} load_flow_component_result;

typedef struct load_flow_parameters_struct {
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
} load_flow_parameters;

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

typedef struct contingency_result_struct {
    char* contingency_id;
    int status;
    array limit_violations;
} contingency_result;

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
} element_type;

typedef enum {
    ALL = 0,
    NONE,
    SPECIFIC,
} contingency_context_type;

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

typedef struct zone_struct {
    char* id;
    char** injections_ids;
    double* injections_shift_keys;
    int length;
} zone;
