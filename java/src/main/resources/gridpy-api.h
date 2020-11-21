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
