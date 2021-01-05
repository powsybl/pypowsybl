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
    char* status;
    int iteration_count;
    char* slack_bus_id;
    double slack_bus_active_power_mismatch;
} load_flow_component_result;

typedef struct limit_violation_struct {
    char* subject_id;
    double limit;
    double value;
} limit_violation;

typedef struct security_analysis_result_struct {
    char* contingency_id;
    char* status;
    array limit_violations;
} security_analysis_result;
