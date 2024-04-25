/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "pypowsybl.h"
#include "pylogging.h"
#include <iostream>

namespace pypowsybl {

graal_isolate_t* isolate = nullptr;

void init() {
    graal_isolatethread_t* thread = nullptr;

    int c = graal_create_isolate(nullptr, &isolate, &thread);
    if (c != 0) {
        throw std::runtime_error("graal_create_isolate error: " + std::to_string(c));
    }
}

GraalVmGuard::GraalVmGuard() {
   if (!isolate) {
       throw std::runtime_error("isolate has not been created");
   }
   //if thread already attached to the isolate,
   //we assume it's a nested call --> do nothing

   thread_ = graal_get_current_thread(isolate);
   if (thread_ == nullptr) {
       int c = graal_attach_thread(isolate, &thread_);
       if (c != 0) {
           throw std::runtime_error("graal_attach_thread error: " + std::to_string(c));
       }
       shouldDetach = true;
  }
}

template<>
std::vector<std::string> toVector(array* arrayPtr) {
    std::vector<std::string> strings;
    strings.reserve(arrayPtr->length);
    for (int i = 0; i < arrayPtr->length; i++) {
        char** ptr = (char**) arrayPtr->ptr + i;
        std::string str = *ptr ? *ptr : "";
        strings.emplace_back(str);
    }
    return strings;
}

//Explicitly update log level on java side
void setLogLevelFromPythonLogger(graal_isolatethread_t* thread, exception_handler* exc) {
    py::object logger = CppToPythonLogger::get()->getLogger();
    if (!logger.is_none()) {
        py::gil_scoped_acquire acquire;
        py::object level = logger.attr("level");
        ::setLogLevel(thread, level.cast<int>(), exc);
     }
}

::zone* createZone(const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys) {
    auto z = new ::zone;
    z->id = copyStringToCharPtr(id);
    z->length = injectionsIds.size();
    z->injections_ids = new char*[injectionsIds.size()];
    for (int i = 0; i < injectionsIds.size(); i++) {
        z->injections_ids[i] = copyStringToCharPtr(injectionsIds[i]);
    }
    z->injections_shift_keys = new double[injectionsShiftKeys.size()];
    for (int i = 0; i < injectionsIds.size(); i++) {
        z->injections_shift_keys[i] = injectionsShiftKeys[i];
    }
    return z;
}

void deleteZone(::zone* z) {
    delete[] z->id;
    for (int i = 0; i < z->length; i++) {
        delete[] z->injections_ids[i];
    }
    delete[] z->injections_ids;
    delete[] z->injections_shift_keys;
}

std::map<std::string, std::string> convertMapStructToStdMap(string_map* map) {
    std::map<std::string, std::string> stdStringMap;
    for (int i = 0; i < map->length; i++) {
        char** keyPtr = (char**) map->keys + i;
        char** valuePtr = (char**) map->values + i;
        // ternary is to protect from UB with nullptr
        stdStringMap.emplace(std::string(*keyPtr ? *keyPtr : ""), std::string(*valuePtr ? *valuePtr : ""));
    }
    return stdStringMap;
}

char* copyStringToCharPtr(const std::string& str) {
    char* c = new char[str.size() + 1];
    str.copy(c, str.size());
    c[str.size()] = '\0';
    return c;
}

char** copyVectorStringToCharPtrPtr(const std::vector<std::string>& strings) {
    char** charPtrPtr = new char*[strings.size()];
    for (int i = 0; i < strings.size(); i++) {
        charPtrPtr[i] = copyStringToCharPtr(strings[i]);
    }
    return charPtrPtr;
}

int* copyVectorInt(const std::vector<int>& ints) {
    int* intPtr = new int[ints.size()];
    std::copy(ints.begin(), ints.end(), intPtr);
    return intPtr;
}

double* copyVectorDouble(const std::vector<double>& doubles) {
    double* doublePtr = new double[doubles.size()];
    std::copy(doubles.begin(), doubles.end(), doublePtr);
    return doublePtr;
}

void deleteCharPtrPtr(char** charPtrPtr, int length) {
    for (int i = 0; i < length; i++) {
        delete[] charPtrPtr[i];
    }
    delete[] charPtrPtr;
}

void freeCString(char* str) {
    directCallJava(::freeString, str);
}

//copies to string and frees memory allocated by java
std::string toString(char* cstring) {
    std::string res = cstring;
    freeCString(cstring);
    return res;
}

void copyCharPtrPtrToVector(char** src, int count, std::vector<std::string>& dest) {
    dest.clear();
    std::copy(src, src + count, std::back_inserter(dest));
}

void deleteLoadFlowParameters(loadflow_parameters* ptr) {
    pypowsybl::deleteCharPtrPtr(ptr->countries_to_balance, ptr->countries_to_balance_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_keys, ptr->provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_values, ptr->provider_parameters_values_count);
}

LoadFlowParameters::LoadFlowParameters(loadflow_parameters* src) {
    voltage_init_mode = static_cast<VoltageInitMode>(src->voltage_init_mode);
    transformer_voltage_control_on = (bool) src->transformer_voltage_control_on;
    use_reactive_limits = (bool) src->use_reactive_limits;
    phase_shifter_regulation_on = (bool) src->phase_shifter_regulation_on;
    twt_split_shunt_admittance = (bool) src->twt_split_shunt_admittance;
    shunt_compensator_voltage_control_on = (bool) src->shunt_compensator_voltage_control_on;
    read_slack_bus = (bool) src->read_slack_bus;
    write_slack_bus = (bool) src->write_slack_bus;
    distributed_slack = (bool) src->distributed_slack;
    balance_type = static_cast<BalanceType>(src->balance_type);
    dc_use_transformer_ratio = (bool) src->dc_use_transformer_ratio;
    connected_component_mode = static_cast<ConnectedComponentMode>(src->connected_component_mode);
    copyCharPtrPtrToVector(src->countries_to_balance, src->countries_to_balance_count, countries_to_balance);
    copyCharPtrPtrToVector(src->provider_parameters_keys, src->provider_parameters_keys_count, provider_parameters_keys);
    copyCharPtrPtrToVector(src->provider_parameters_values, src->provider_parameters_values_count, provider_parameters_values);
}

void LoadFlowParameters::load_to_c_struct(loadflow_parameters& res) const {
    res.voltage_init_mode = voltage_init_mode;
    res.transformer_voltage_control_on = (unsigned char) transformer_voltage_control_on;
    res.use_reactive_limits = (unsigned char) use_reactive_limits;
    res.phase_shifter_regulation_on = (unsigned char) phase_shifter_regulation_on;
    res.twt_split_shunt_admittance = (unsigned char) twt_split_shunt_admittance;
    res.shunt_compensator_voltage_control_on = (unsigned char) shunt_compensator_voltage_control_on;
    res.read_slack_bus = (unsigned char) read_slack_bus;
    res.write_slack_bus = (unsigned char) write_slack_bus;
    res.distributed_slack = (unsigned char) distributed_slack;
    res.balance_type = balance_type;
    res.dc_use_transformer_ratio = (unsigned char) dc_use_transformer_ratio;
    res.connected_component_mode = connected_component_mode;
    res.countries_to_balance = pypowsybl::copyVectorStringToCharPtrPtr(countries_to_balance);
    res.countries_to_balance_count = countries_to_balance.size();
    res.provider_parameters_keys = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_keys);
    res.provider_parameters_keys_count = provider_parameters_keys.size();
    res.provider_parameters_values = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_values);
    res.provider_parameters_values_count = provider_parameters_values.size();
}

std::shared_ptr<loadflow_parameters> LoadFlowParameters::to_c_struct() const {
    loadflow_parameters* res = new loadflow_parameters();
    load_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<loadflow_parameters>(res, [](loadflow_parameters* ptr){
        deleteLoadFlowParameters(ptr);
        delete ptr;
    });
}

void deleteLoadFlowValidationParameters(loadflow_validation_parameters* ptr) {
    deleteLoadFlowParameters(&ptr->loadflow_parameters);
}

LoadFlowValidationParameters::LoadFlowValidationParameters(loadflow_validation_parameters* src):
    loadflow_parameters(&src->loadflow_parameters)
{
    threshold = (double) src->threshold;
    verbose = (bool) src->verbose;
    loadflow_name = toString(src->loadflow_name);
    epsilon_x = (double) src->epsilon_x;
    apply_reactance_correction = (bool) src->apply_reactance_correction;
    ok_missing_values = (bool) src->ok_missing_values;
    no_requirement_if_reactive_bound_inversion = (bool) src->no_requirement_if_reactive_bound_inversion;
    compare_results = (bool) src->compare_results;
    check_main_component_only = (bool) src->check_main_component_only;
    no_requirement_if_setpoint_outside_power_bounds = (bool) src->no_requirement_if_setpoint_outside_power_bounds;
}

void LoadFlowValidationParameters::load_to_c_struct(loadflow_validation_parameters& res) const {
    res.threshold = threshold;
    res.verbose = (unsigned char) verbose;
    res.loadflow_name = copyStringToCharPtr(loadflow_name);
    res.epsilon_x = epsilon_x;
    res.apply_reactance_correction = (unsigned char) apply_reactance_correction;
    res.ok_missing_values = (unsigned char) ok_missing_values;
    res.no_requirement_if_reactive_bound_inversion = (unsigned char) no_requirement_if_reactive_bound_inversion;
    res.compare_results = (unsigned char) compare_results;
    res.check_main_component_only = (unsigned char) check_main_component_only;
    res.no_requirement_if_setpoint_outside_power_bounds = (unsigned char) no_requirement_if_setpoint_outside_power_bounds;
}

std::shared_ptr<loadflow_validation_parameters> LoadFlowValidationParameters::to_c_struct() const {
    loadflow_validation_parameters* res = new loadflow_validation_parameters();
    load_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<loadflow_validation_parameters>(res, [](loadflow_validation_parameters* ptr){
        deleteLoadFlowValidationParameters(ptr);
        delete ptr;
    });
}

void deleteSecurityAnalysisParameters(security_analysis_parameters* ptr) {
    deleteLoadFlowParameters(&ptr->loadflow_parameters);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_keys, ptr->provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_values, ptr->provider_parameters_values_count);
}

SecurityAnalysisParameters::SecurityAnalysisParameters(security_analysis_parameters* src):
    loadflow_parameters(&src->loadflow_parameters)
{
    flow_proportional_threshold = (double) src->flow_proportional_threshold;
    low_voltage_proportional_threshold = (double) src->low_voltage_proportional_threshold;
    low_voltage_absolute_threshold = (double) src->low_voltage_absolute_threshold;
    high_voltage_proportional_threshold = (double) src->high_voltage_proportional_threshold;
    high_voltage_absolute_threshold = (double) src->high_voltage_absolute_threshold;
    copyCharPtrPtrToVector(src->provider_parameters_keys, src->provider_parameters_keys_count, provider_parameters_keys);
    copyCharPtrPtrToVector(src->provider_parameters_values, src->provider_parameters_values_count, provider_parameters_values);
}

std::shared_ptr<security_analysis_parameters> SecurityAnalysisParameters::to_c_struct() const {
    security_analysis_parameters* res = new security_analysis_parameters();
    loadflow_parameters.load_to_c_struct(res->loadflow_parameters);
    res->flow_proportional_threshold = (double) flow_proportional_threshold;
    res->low_voltage_proportional_threshold = (double) low_voltage_proportional_threshold;
    res->low_voltage_absolute_threshold = (double) low_voltage_absolute_threshold;
    res->high_voltage_proportional_threshold = (double) high_voltage_proportional_threshold;
    res->high_voltage_absolute_threshold = (double) high_voltage_absolute_threshold;
    res->provider_parameters_keys = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_keys);
    res->provider_parameters_keys_count = provider_parameters_keys.size();
    res->provider_parameters_values = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_values);
    res->provider_parameters_values_count = provider_parameters_values.size();
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<security_analysis_parameters>(res, [](security_analysis_parameters* ptr){
        deleteSecurityAnalysisParameters(ptr);
        delete ptr;
    });
}

void deleteSensitivityAnalysisParameters(sensitivity_analysis_parameters* ptr) {
    deleteLoadFlowParameters(&ptr->loadflow_parameters);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_keys, ptr->provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_values, ptr->provider_parameters_values_count);
}

SensitivityAnalysisParameters::SensitivityAnalysisParameters(sensitivity_analysis_parameters* src):
    loadflow_parameters(&src->loadflow_parameters)
{
    copyCharPtrPtrToVector(src->provider_parameters_keys, src->provider_parameters_keys_count, provider_parameters_keys);
    copyCharPtrPtrToVector(src->provider_parameters_values, src->provider_parameters_values_count, provider_parameters_values);
}

std::shared_ptr<sensitivity_analysis_parameters> SensitivityAnalysisParameters::to_c_struct() const {
    sensitivity_analysis_parameters* res = new sensitivity_analysis_parameters();
    loadflow_parameters.load_to_c_struct(res->loadflow_parameters);
    res->provider_parameters_keys = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_keys);
    res->provider_parameters_keys_count = provider_parameters_keys.size();
    res->provider_parameters_values = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_values);
    res->provider_parameters_values_count = provider_parameters_values.size();
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<sensitivity_analysis_parameters>(res, [](sensitivity_analysis_parameters* ptr){
        deleteSensitivityAnalysisParameters(ptr);
        delete ptr;
    });
}

FlowDecompositionParameters::FlowDecompositionParameters(flow_decomposition_parameters* src) {
    enable_losses_compensation = (bool) src->enable_losses_compensation;
    losses_compensation_epsilon = (float) src->losses_compensation_epsilon;
    sensitivity_epsilon = (float) src->sensitivity_epsilon;
    rescale_enabled = (bool) src->rescale_enabled;
    dc_fallback_enabled_after_ac_divergence = (bool) src->dc_fallback_enabled_after_ac_divergence;
    sensitivity_variable_batch_size = (int) src->sensitivity_variable_batch_size;
}

std::shared_ptr<flow_decomposition_parameters> FlowDecompositionParameters::to_c_struct() const {
    flow_decomposition_parameters* res = new flow_decomposition_parameters();
    res->enable_losses_compensation = (unsigned char) enable_losses_compensation;
    res->losses_compensation_epsilon = losses_compensation_epsilon;
    res->sensitivity_epsilon = sensitivity_epsilon;
    res->rescale_enabled = (unsigned char) rescale_enabled;
    res->dc_fallback_enabled_after_ac_divergence = (unsigned char) dc_fallback_enabled_after_ac_divergence;
    res->sensitivity_variable_batch_size = (int) sensitivity_variable_batch_size;
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<flow_decomposition_parameters>(res, [](flow_decomposition_parameters* ptr){
        delete ptr;
    });
}

void deleteShortCircuitAnalysisParameters(shortcircuit_analysis_parameters* ptr) {
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_keys, ptr->provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters_values, ptr->provider_parameters_values_count);
}

ShortCircuitAnalysisParameters::ShortCircuitAnalysisParameters(shortcircuit_analysis_parameters* src)
{
    with_feeder_result = (bool) src->with_feeder_result;
    with_limit_violations = (bool) src->with_limit_violations;
    study_type = static_cast<ShortCircuitStudyType>(src->study_type);
    with_fortescue_result = (bool) src->with_fortescue_result;
    with_voltage_result = (bool) src->with_voltage_result;
    min_voltage_drop_proportional_threshold = (double) src->min_voltage_drop_proportional_threshold;

    copyCharPtrPtrToVector(src->provider_parameters_keys, src->provider_parameters_keys_count, provider_parameters_keys);
    copyCharPtrPtrToVector(src->provider_parameters_values, src->provider_parameters_values_count, provider_parameters_values);
}

std::shared_ptr<shortcircuit_analysis_parameters> ShortCircuitAnalysisParameters::to_c_struct() const {
    shortcircuit_analysis_parameters* res = new shortcircuit_analysis_parameters();
    res->with_voltage_result = (bool) with_voltage_result;
    res->with_feeder_result = (bool) with_feeder_result;
    res->with_limit_violations = (bool) with_limit_violations;
    res->study_type = study_type;
    res->with_fortescue_result = (bool) with_fortescue_result;
    res->min_voltage_drop_proportional_threshold = min_voltage_drop_proportional_threshold;

    res->provider_parameters_keys = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_keys);
    res->provider_parameters_keys_count = provider_parameters_keys.size();
    res->provider_parameters_values = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_values);
    res->provider_parameters_values_count = provider_parameters_values.size();

    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<shortcircuit_analysis_parameters>(res, [](shortcircuit_analysis_parameters* ptr){
        deleteShortCircuitAnalysisParameters(ptr);
        delete ptr;
    });
}


SldParameters::SldParameters(sld_parameters* src) {
    use_name = (bool) src->use_name;
    center_name = (bool) src->center_name;
    diagonal_label = (bool) src->diagonal_label;
    nodes_infos = (bool) src->nodes_infos;
    tooltip_enabled = (bool) src->tooltip_enabled;
    topological_coloring = (bool) src->topological_coloring;
    component_library = toString(src->component_library);
}

NadParameters::NadParameters(nad_parameters* src) {
    edge_name_displayed = (bool) src->edge_name_displayed;
    edge_info_along_edge = (bool) src->edge_info_along_edge;
    id_displayed = (bool) src->id_displayed;
    power_value_precision = src->power_value_precision;
    current_value_precision = src->current_value_precision;
    angle_value_precision = src->angle_value_precision;
    voltage_value_precision = src->voltage_value_precision;
    substation_description_displayed = src->substation_description_displayed;
    bus_legend = src->bus_legend;
}

void SldParameters::sld_to_c_struct(sld_parameters& res) const {
    res.use_name = (unsigned char) use_name;
    res.center_name = (unsigned char) center_name;
    res.diagonal_label = (unsigned char) diagonal_label;
    res.nodes_infos = (unsigned char) nodes_infos;
    res.tooltip_enabled = (unsigned char) tooltip_enabled;
    res.topological_coloring = (unsigned char) topological_coloring;
    res.component_library = copyStringToCharPtr(component_library);
}

void NadParameters::nad_to_c_struct(nad_parameters& res) const {
    res.edge_name_displayed = (unsigned char) edge_name_displayed;
    res.edge_info_along_edge = (unsigned char) edge_info_along_edge;
    res.id_displayed = (unsigned char) id_displayed;
    res.power_value_precision = power_value_precision;
    res.current_value_precision = current_value_precision;
    res.angle_value_precision = angle_value_precision;
    res.voltage_value_precision = voltage_value_precision;
    res.substation_description_displayed = substation_description_displayed;
    res.bus_legend = bus_legend;
}

std::shared_ptr<sld_parameters> SldParameters::to_c_struct() const {
    sld_parameters* res = new sld_parameters();
    sld_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<sld_parameters>(res, [](sld_parameters* ptr){
        delete ptr;
    });
}

std::shared_ptr<nad_parameters> NadParameters::to_c_struct() const {
    nad_parameters* res = new nad_parameters();
    nad_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<nad_parameters>(res, [](nad_parameters* ptr){
        delete ptr;
    });
}

std::vector<SeriesMetadata> convertDataframeMetadata(dataframe_metadata* dataframeMetadata) {
    std::vector<SeriesMetadata> res;
    for (int i = 0; i < dataframeMetadata->attributes_count; i++) {
        const series_metadata& series = dataframeMetadata->attributes_metadata[i];
        res.push_back(SeriesMetadata(series.name, series.type, series.is_index, series.is_modifiable, series.is_default));
    }
    return res;
}

std::shared_ptr<dataframe_array> createDataframeArray(const std::vector<dataframe*>& dataframes) {
    std::shared_ptr<dataframe_array> dataframeArray(new dataframe_array(), [](dataframe_array* dataframeToDestroy){
        delete[] dataframeToDestroy->dataframes;
        delete dataframeToDestroy;
    });
    dataframe* dataframesFinal = new dataframe[dataframes.size()];
    for (int indice = 0 ; indice < dataframes.size() ; indice ++) {
        dataframesFinal[indice] = *dataframes[indice];
    }
    dataframeArray->dataframes = dataframesFinal;
    dataframeArray->dataframes_count = dataframes.size();
    return dataframeArray;
}

}
