typedef struct array_struct {
    void* ptr;
    int length;
} array;

typedef struct bus_struct {
    char* id;
    double v_magnitude;
    double v_angle;
} bus;

typedef struct load_flow_component_result_struct {
    int component_num;
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
    LINE = 0,
    TWO_WINDINGS_TRANSFORMER,
    GENERATOR,
} element_type;

typedef struct matrix_struct {
    int row_count;
    int column_count;
    double* values;
} matrix;
