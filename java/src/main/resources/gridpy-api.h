typedef struct load_flow_result_struct {
    unsigned char ok;
} load_flow_result;

typedef struct bus_struct {
    char* id;
    double v_magnitude;
    double v_angle;
} bus;

typedef struct bus_array_struct {
    bus* ptr;
    int length;
} bus_array;

typedef struct load_flow_component_result_struct {
    int component_num;
    char* status;
    int iteration_count;
    char* slack_bus_id;
    double slack_bus_active_power_mismatch;
} load_flow_component_result;

typedef struct load_flow_component_result_array_struct {
    load_flow_component_result* ptr;
    int length;
} load_flow_component_result_array;
