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

typedef struct security_analysis_result_struct {
} security_analysis_result;
