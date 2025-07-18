/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "powsybl-cpp.h"
#include <iostream>
#include <sstream>
#include <cstring>

namespace pypowsybl {

std::mutex PowsyblCaller::initMutex_;
PowsyblCaller *PowsyblCaller::singleton_ = nullptr;

graal_isolate_t* isolate = nullptr;
std::vector<char*> argv;

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

GraalVmGuard::~GraalVmGuard() noexcept(false) {
    if (shouldDetach) {
        int c = graal_detach_thread(thread_);
        if (c != 0) {
            throw std::runtime_error("graal_detach_thread error: " + std::to_string(c));
        }
    }
}
PowsyblCaller* PowsyblCaller::get() {
    std::lock_guard<std::mutex> guard(initMutex_);
    if (!singleton_) {
        singleton_ = new PowsyblCaller();
    }
    return singleton_;
}

void PowsyblCaller::setPreprocessingJavaCall(std::function <void(GraalVmGuard* guard, exception_handler* exc)> func) {
    beginCall_ = func;
}

void PowsyblCaller::setPostProcessingJavaCall(std::function<void()> func) {
    endCall_ = func;
}

// we need to pass arguments through GRAALVM_OPTIONS env variable like:
// GRAALVM_OPTIONS="-Xmx1G" python
void readArgvFromEnv() {
    argv.reserve(1);
    argv.push_back(strdup("from_env")); // argv[0] is expected to be the program name
    const char* env = std::getenv("GRAALVM_OPTIONS");
    if (env) {
        // parse
        std::istringstream iss(env);
        std::string token;
        while (iss >> token) {
            argv.push_back(strdup(token.c_str()));
        }
    }
}

void freeArgv() {
    for (auto& arg : argv) {
        free(arg);
    }
    argv.clear();
}

void init(std::function <void(GraalVmGuard* guard, exception_handler* exc)> preJavaCall,
          std::function <void()> postJavaCall) {
    graal_isolatethread_t* thread = nullptr;

    PowsyblCaller::get()->setPreprocessingJavaCall(preJavaCall);
    PowsyblCaller::get()->setPostProcessingJavaCall(postJavaCall);

    readArgvFromEnv();

    int argc = argv.size();
    int c;
    if (argc > 1) {
        graal_create_isolate_params_t params;
        params.version = 4;
        // theses fields are not part of the public API, so on are named reserved
        // this might fail in a coming release of GraalVM
        params._reserved_1 = argc; // argc
        params._reserved_2 = &argv[0]; // argv
        params._reserved_3 = false; // ignoreUnrecognizedArguments
        params._reserved_4 = true;  // exitWhenArgumentParsingFails
        c = graal_create_isolate(&params, &isolate, &thread);
    } else {
        c = graal_create_isolate(nullptr, &isolate, &thread);
    }

    if (c != 0) {
        throw std::runtime_error("graal_create_isolate error: " + std::to_string(c));
    }
}

//Destruction of java object when the shared_ptr has no more references
JavaHandle::JavaHandle(void* handle):
    handle_(handle, [](void* to_be_deleted) {
        if (to_be_deleted) {
            PowsyblCaller::get()->callJava<>(::destroyObjectHandle, to_be_deleted);
        }
    })
{
}

template<>
Array<loadflow_component_result>::~Array() {
    PowsyblCaller::get()->callJava<>(::freeLoadFlowComponentResultPointer, delegate_);
}

template<>
Array<slack_bus_result>::~Array() {
    // already freed by loadflow_component_result
}

template<>
Array<post_contingency_result>::~Array() {
    PowsyblCaller::get()->callJava<>(::freeContingencyResultArrayPointer, delegate_);
}

template<>
Array<operator_strategy_result>::~Array() {
    PowsyblCaller::get()->callJava<>(::freeOperatorStrategyResultArrayPointer, delegate_);
}

template<>
Array<limit_violation>::~Array() {
    // already freed by contingency_result
}

template<>
Array<series>::~Array() {
    PowsyblCaller::get()->callJava<>(::freeSeriesArray, delegate_);
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

class ToStringVector {
public:
    ToStringVector(array* arrayPtr)
        : arrayPtr_(arrayPtr) {
    }

    ~ToStringVector() {
        PowsyblCaller::get()->callJava<>(::freeStringArray, arrayPtr_);
    }

    std::vector<std::string> get() {
        return toVector<std::string>(arrayPtr_);
    }

private:
    array* arrayPtr_;
};

template<typename T>
class ToPrimitiveVector {
public:
    ToPrimitiveVector(array* arrayPtr)
        : arrayPtr_(arrayPtr) {
    }

    ~ToPrimitiveVector() {
        PowsyblCaller::get()->callJava<>(::freeArray, arrayPtr_);
    }

    std::vector<T> get() {
        return toVector<T>(arrayPtr_);
    }

private:
    array* arrayPtr_;
};


std::map<std::string, std::string> convertMapStructToStdMap(string_map* map) {
    std::map<std::string, std::string> stdStringMap;
    for (int i = 0; i < map->length; i++) {
        char** keyPtr = (char**) map->keys + i;
        char** valuePtr = (char**) map->values + i;
        // ternary is to protect from UB with nullptr
        stdStringMap.emplace(std::string(*keyPtr ? *keyPtr : ""), std::string(*valuePtr ? *valuePtr : ""));
    }
    PowsyblCaller::get()->callJava<>(::freeStringMap, map);
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
    PowsyblCaller::get()->callJava<>(::freeString, str);
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
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_keys, ptr->provider_parameters.provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_values, ptr->provider_parameters.provider_parameters_values_count);
}

void providerParametersToCStruct(provider_parameters& providerParams, std::vector<std::string> const& provider_parameters_keys,
  std::vector<std::string> const& provider_parameters_values) {
     providerParams.provider_parameters_keys = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_keys);
     providerParams.provider_parameters_keys_count = provider_parameters_keys.size();
     providerParams.provider_parameters_values = pypowsybl::copyVectorStringToCharPtrPtr(provider_parameters_values);
     providerParams.provider_parameters_values_count = provider_parameters_values.size();

}

void providerParametersFromCStruct(provider_parameters& providerParams, std::vector<std::string>& provider_parameters_keys,
  std::vector<std::string>& provider_parameters_values) {
     copyCharPtrPtrToVector(providerParams.provider_parameters_keys, providerParams.provider_parameters_keys_count, provider_parameters_keys);
     copyCharPtrPtrToVector(providerParams.provider_parameters_values, providerParams.provider_parameters_values_count, provider_parameters_values);
}

std::vector<std::vector<std::string>> arrayToStringVectorVector(array nestedStringVector) {
    std::vector<std::vector<std::string>> mainList;
    for (int i = 0; i < nestedStringVector.length; i++) {
        std::vector<std::string> subList;
        array value = *((array*) nestedStringVector.ptr + i);
        for (int j = 0; j < value.length; j++) {
            char* subValue = *((char**) value.ptr + j);
            subList.push_back(std::string(subValue));
        }
        mainList.push_back(subList);
    }
    return mainList;
}

array stringVectorVectorToArray(std::vector<std::vector<std::string>> const& nestedStringVector) {
    array mainArray;
    array* mainPtr = new array[nestedStringVector.size()];
    mainArray.length = nestedStringVector.size();
    for (int i=0; i < nestedStringVector.size(); ++i) {
        mainPtr[i].ptr = copyVectorStringToCharPtrPtr(nestedStringVector[i]);
        mainPtr[i].length =  nestedStringVector[i].size();
    }
    mainArray.ptr = mainPtr;
    return mainArray;
}

void freeStringListListArray(array mainArray) {
    for (int i=0; i < mainArray.length; ++i) {
        array* subArray = (array*) mainArray.ptr;
        deleteCharPtrPtr((char**) subArray[i].ptr, subArray[i].length);
    }
    delete[] mainArray.ptr;
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
    hvdc_ac_emulation = (bool) src->hvdc_ac_emulation;
    dc_power_factor = (double) src->dc_power_factor;
    copyCharPtrPtrToVector(src->countries_to_balance, src->countries_to_balance_count, countries_to_balance);
    providerParametersFromCStruct(src->provider_parameters, provider_parameters_keys, provider_parameters_values);
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
    res.countries_to_balance = pypowsybl::copyVectorStringToCharPtrPtr(countries_to_balance);
    res.countries_to_balance_count = countries_to_balance.size();
    res.connected_component_mode = connected_component_mode;
    res.hvdc_ac_emulation = (unsigned char) hvdc_ac_emulation;
    res.dc_power_factor = dc_power_factor;
    providerParametersToCStruct(res.provider_parameters, provider_parameters_keys, provider_parameters_values);
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

void deleteSensitivityAnalysisParameters(sensitivity_analysis_parameters* ptr) {
    deleteLoadFlowParameters(&ptr->loadflow_parameters);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_keys, ptr->provider_parameters.provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_values, ptr->provider_parameters.provider_parameters_values_count);
}

RaoParameters::RaoParameters(rao_parameters* src):
   sensitivity_parameters(src->sensitivity_parameters)
{
    objective_function_type = static_cast<ObjectiveFunctionType>(src->objective_function_type);
    unit = static_cast<Unit>(src->unit);
    curative_min_obj_improvement = src->curative_min_obj_improvement;
    enforce_curative_security = (bool) src->enforce_curative_security;

    solver = static_cast<Solver>(src->solver);
    relative_mip_gap = src->relative_mip_gap;
    if (src->solver_specific_parameters != nullptr) {
        solver_specific_parameters = toString(src->solver_specific_parameters);
    }

    // range action optimization parameters
    pst_ra_min_impact_threshold = src->pst_ra_min_impact_threshold;
    hvdc_ra_min_impact_threshold = src->hvdc_ra_min_impact_threshold;
    injection_ra_min_impact_threshold = src->injection_ra_min_impact_threshold;
    max_mip_iterations = src->max_mip_iterations;
    pst_sensitivity_threshold = src->pst_sensitivity_threshold;
    hvdc_sensitivity_threshold = src->hvdc_sensitivity_threshold;
    injection_ra_sensitivity_threshold = src->injection_ra_sensitivity_threshold;
    pst_model = static_cast<PstModel>(src->pst_model);
    ra_range_shrinking = static_cast<RaRangeShrinking>(src->ra_range_shrinking);

    // topo optimization parameters
    max_preventive_search_tree_depth = src->max_preventive_search_tree_depth;
    max_curative_search_tree_depth = src->max_curative_search_tree_depth;
    // Missing predefinedCombinations (list of list of string..)
    predefined_combinations = arrayToStringVectorVector(src->predefined_combinations);

    relative_min_impact_threshold = src->relative_min_impact_threshold;
    absolute_min_impact_threshold = src->absolute_min_impact_threshold;
    skip_actions_far_from_most_limiting_element = (bool) src->skip_actions_far_from_most_limiting_element;
    max_number_of_boundaries_for_skipping_actions = src->max_number_of_boundaries_for_skipping_actions;

    // Multithreading parameters
    available_cpus = src->available_cpus;

    // Second preventive rao parameters
    execution_condition = static_cast<ExecutionCondition>(src->execution_condition);
    re_optimize_curative_range_actions = (bool) src->re_optimize_curative_range_actions;
    hint_from_first_preventive_rao = (bool) src->hint_from_first_preventive_rao;

    // Not optimized cnec parameters
    do_not_optimize_curative_cnecs_for_tsos_without_cras = (bool) src->do_not_optimize_curative_cnecs_for_tsos_without_cras;

    // Load flow and sensitivity parameters
    load_flow_provider = toString(src->load_flow_provider);
    sensitivity_provider = toString(src->sensitivity_provider);
    sensitivity_failure_overcost = src->sensitivity_failure_overcost;

    providerParametersFromCStruct(src->provider_parameters, provider_parameters_keys, provider_parameters_values);
}

void RaoParameters::load_to_c_struct(rao_parameters& res) const {
    res.objective_function_type = objective_function_type;
    res.unit = unit;
    res.curative_min_obj_improvement = curative_min_obj_improvement;
    res.enforce_curative_security = enforce_curative_security;

    res.solver = int(solver);
    res.relative_mip_gap = relative_mip_gap;
    res.solver_specific_parameters = copyStringToCharPtr(solver_specific_parameters);

    // range action optimization parameters
    res.pst_ra_min_impact_threshold = pst_ra_min_impact_threshold;
    res.hvdc_ra_min_impact_threshold = hvdc_ra_min_impact_threshold;
    res.injection_ra_min_impact_threshold = injection_ra_min_impact_threshold;
    res.max_mip_iterations = max_mip_iterations;
    res.pst_sensitivity_threshold = pst_sensitivity_threshold;
    res.hvdc_sensitivity_threshold = hvdc_sensitivity_threshold;
    res.injection_ra_sensitivity_threshold = injection_ra_sensitivity_threshold;
    res.pst_model = int(pst_model);
    res.ra_range_shrinking = int(ra_range_shrinking);

    // topo optimization parameters
    res.max_preventive_search_tree_depth = max_preventive_search_tree_depth;
    res.max_curative_search_tree_depth = max_curative_search_tree_depth;
    // Missing predefinedCombinations (list of list of string..)
    res.predefined_combinations = stringVectorVectorToArray(predefined_combinations);
    res.relative_min_impact_threshold = relative_min_impact_threshold;
    res.absolute_min_impact_threshold = absolute_min_impact_threshold;
    res.skip_actions_far_from_most_limiting_element = skip_actions_far_from_most_limiting_element;
    res.max_number_of_boundaries_for_skipping_actions = max_number_of_boundaries_for_skipping_actions;

    // Multithreading parameters
    res.available_cpus = available_cpus;

    // Second preventive rao parameters
    res.execution_condition = int(execution_condition);
    res.re_optimize_curative_range_actions = re_optimize_curative_range_actions;
    res.hint_from_first_preventive_rao = hint_from_first_preventive_rao;

    // Not optimized cnec parameters
    res.do_not_optimize_curative_cnecs_for_tsos_without_cras = do_not_optimize_curative_cnecs_for_tsos_without_cras;

    // Load flow and sensitivity parameters
    res.load_flow_provider = copyStringToCharPtr(load_flow_provider);
    res.sensitivity_provider = copyStringToCharPtr(sensitivity_provider);
    res.sensitivity_parameters = new sensitivity_analysis_parameters();
    sensitivity_parameters.load_to_c_struct(*(res.sensitivity_parameters));
    res.sensitivity_failure_overcost = sensitivity_failure_overcost;
    providerParametersToCStruct(res.provider_parameters, provider_parameters_keys, provider_parameters_values);
}

std::shared_ptr<rao_parameters> RaoParameters::to_c_struct() const {
    rao_parameters* res = new rao_parameters();
    load_to_c_struct(*res);
    return std::shared_ptr<rao_parameters>(res, [](rao_parameters* ptr){
        deleteSensitivityAnalysisParameters(ptr->sensitivity_parameters);
        freeStringListListArray(ptr->predefined_combinations);
        delete ptr->load_flow_provider;
        delete ptr->sensitivity_provider;
        pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_keys, ptr->provider_parameters.provider_parameters_keys_count);
        pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_values, ptr->provider_parameters.provider_parameters_values_count);
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
    loadflow_parameters.load_to_c_struct(res->loadflow_parameters);
    load_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<loadflow_validation_parameters>(res, [](loadflow_validation_parameters* ptr){
        deleteLoadFlowValidationParameters(ptr);
        delete ptr;
    });
}

void deleteSecurityAnalysisParameters(security_analysis_parameters* ptr) {
    deleteLoadFlowParameters(&ptr->loadflow_parameters);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_keys, ptr->provider_parameters.provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_values, ptr->provider_parameters.provider_parameters_values_count);
}

SecurityAnalysisParameters::SecurityAnalysisParameters(security_analysis_parameters* src):
    loadflow_parameters(&src->loadflow_parameters)
{
    flow_proportional_threshold = (double) src->flow_proportional_threshold;
    low_voltage_proportional_threshold = (double) src->low_voltage_proportional_threshold;
    low_voltage_absolute_threshold = (double) src->low_voltage_absolute_threshold;
    high_voltage_proportional_threshold = (double) src->high_voltage_proportional_threshold;
    high_voltage_absolute_threshold = (double) src->high_voltage_absolute_threshold;
    providerParametersFromCStruct(src->provider_parameters, provider_parameters_keys, provider_parameters_values);
}

std::shared_ptr<security_analysis_parameters> SecurityAnalysisParameters::to_c_struct() const {
    security_analysis_parameters* res = new security_analysis_parameters();
    loadflow_parameters.load_to_c_struct(res->loadflow_parameters);
    res->flow_proportional_threshold = (double) flow_proportional_threshold;
    res->low_voltage_proportional_threshold = (double) low_voltage_proportional_threshold;
    res->low_voltage_absolute_threshold = (double) low_voltage_absolute_threshold;
    res->high_voltage_proportional_threshold = (double) high_voltage_proportional_threshold;
    res->high_voltage_absolute_threshold = (double) high_voltage_absolute_threshold;

    providerParametersToCStruct(res->provider_parameters, provider_parameters_keys, provider_parameters_values);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<security_analysis_parameters>(res, [](security_analysis_parameters* ptr){
        deleteSecurityAnalysisParameters(ptr);
        delete ptr;
    });
}

SensitivityAnalysisParameters::SensitivityAnalysisParameters(sensitivity_analysis_parameters* src):
    loadflow_parameters(&src->loadflow_parameters)
{
    providerParametersFromCStruct(src->provider_parameters, provider_parameters_keys, provider_parameters_values);
}

std::shared_ptr<sensitivity_analysis_parameters> SensitivityAnalysisParameters::to_c_struct() const {
    sensitivity_analysis_parameters* res = new sensitivity_analysis_parameters();
    load_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<sensitivity_analysis_parameters>(res, [](sensitivity_analysis_parameters* ptr){
        deleteSensitivityAnalysisParameters(ptr);
        delete ptr;
    });
}

void SensitivityAnalysisParameters::load_to_c_struct(sensitivity_analysis_parameters& params) const {
    loadflow_parameters.load_to_c_struct(params.loadflow_parameters);
    providerParametersToCStruct(params.provider_parameters, provider_parameters_keys, provider_parameters_values);
}

FlowDecompositionParameters::FlowDecompositionParameters(flow_decomposition_parameters* src) {
    enable_losses_compensation = (bool) src->enable_losses_compensation;
    losses_compensation_epsilon = (float) src->losses_compensation_epsilon;
    sensitivity_epsilon = (float) src->sensitivity_epsilon;
    rescale_mode = static_cast<RescaleMode>(src->rescale_mode);
    dc_fallback_enabled_after_ac_divergence = (bool) src->dc_fallback_enabled_after_ac_divergence;
    sensitivity_variable_batch_size = (int) src->sensitivity_variable_batch_size;
}

std::shared_ptr<flow_decomposition_parameters> FlowDecompositionParameters::to_c_struct() const {
    flow_decomposition_parameters* res = new flow_decomposition_parameters();
    res->enable_losses_compensation = (unsigned char) enable_losses_compensation;
    res->losses_compensation_epsilon = losses_compensation_epsilon;
    res->sensitivity_epsilon = sensitivity_epsilon;
    res->rescale_mode = (int) rescale_mode;
    res->dc_fallback_enabled_after_ac_divergence = (unsigned char) dc_fallback_enabled_after_ac_divergence;
    res->sensitivity_variable_batch_size = (int) sensitivity_variable_batch_size;
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<flow_decomposition_parameters>(res, [](flow_decomposition_parameters* ptr){
        delete ptr;
    });
}

void setJavaLibraryPath(const std::string& javaLibraryPath) {
    PowsyblCaller::get()->callJava<>(::setJavaLibraryPath, (char*) javaLibraryPath.data());
}

void logMaxMemory() {
    PowsyblCaller::get()->callJava<>(::logMaxMemory);
}

void setConfigRead(bool configRead) {
    PowsyblCaller::get()->callJava<>(::setConfigRead, configRead);
}

void setDefaultLoadFlowProvider(const std::string& loadFlowProvider) {
    PowsyblCaller::get()->callJava<>(::setDefaultLoadFlowProvider, (char*) loadFlowProvider.data());
}

void setDefaultSecurityAnalysisProvider(const std::string& securityAnalysisProvider) {
    PowsyblCaller::get()->callJava<>(::setDefaultSecurityAnalysisProvider, (char*) securityAnalysisProvider.data());
}

void setDefaultSensitivityAnalysisProvider(const std::string& sensitivityAnalysisProvider) {
    PowsyblCaller::get()->callJava<>(::setDefaultSensitivityAnalysisProvider, (char*) sensitivityAnalysisProvider.data());
}

std::string getDefaultLoadFlowProvider() {
    return toString(PowsyblCaller::get()->callJava<char*>(::getDefaultLoadFlowProvider));
}

std::string getDefaultSecurityAnalysisProvider() {
    return toString(PowsyblCaller::get()->callJava<char*>(::getDefaultSecurityAnalysisProvider));
}

std::string getDefaultSensitivityAnalysisProvider() {
    return toString(PowsyblCaller::get()->callJava<char*>(::getDefaultSensitivityAnalysisProvider));
}

bool isConfigRead() {
    return PowsyblCaller::get()->callJava<bool>(::isConfigRead);
}

std::string getVersionTable() {
    return toString(PowsyblCaller::get()->callJava<char*>(::getVersionTable));
}

JavaHandle createNetwork(const std::string& name, const std::string& id, bool allowVariantMultiThreadAccess) {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createNetwork, (char*) name.data(), (char*) id.data(), allowVariantMultiThreadAccess);
}

JavaHandle merge(std::vector<JavaHandle>& networks) {
    std::vector<void*> networksPtrs;
    networksPtrs.reserve(networks.size());
    for (int i = 0; i < networks.size(); ++i) {
        void* ptr = networks[i];
        networksPtrs.push_back(ptr);
    }
    int networkCount = networksPtrs.size();
    void** networksData = (void**) networksPtrs.data();

    return PowsyblCaller::get()->callJava<JavaHandle>(::merge, networksData, networkCount);
}

JavaHandle getSubNetwork(const JavaHandle& network, const std::string& subNetworkId) {
    return PowsyblCaller::get()->callJava<JavaHandle>(::getSubNetwork, network, (char*) subNetworkId.data());
}

JavaHandle detachSubNetwork(const JavaHandle& subNetwork) {
    return PowsyblCaller::get()->callJava<JavaHandle>(::detachSubNetwork, subNetwork);
}

std::vector<std::string> getNetworkImportFormats() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkImportFormats);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getNetworkExportFormats() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkExportFormats);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getNetworkImportPostProcessors() {
    auto postProcessorsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkImportPostProcessors);
    ToStringVector postProcessors(postProcessorsArrayPtr);
    return postProcessors.get();
}

std::vector<std::string> getNetworkImportSupportedExtensions() {
    auto supportedExtensionsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkImportSupportedExtensions);
    ToStringVector supportedExtensions(supportedExtensionsArrayPtr);
    return supportedExtensions.get();
}

std::vector<std::string> getLoadFlowProviderNames() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getLoadFlowProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getSingleLineDiagramComponentLibraryNames() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getSingleLineDiagramComponentLibraryNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getSecurityAnalysisProviderNames() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getSecurityAnalysisProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getSensitivityAnalysisProviderNames() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getSensitivityAnalysisProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

SeriesArray* createImporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::createImporterParametersSeriesArray, (char*) format.data()));
}

SeriesArray* createExporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::createExporterParametersSeriesArray, (char*) format.data()));
}

std::shared_ptr<network_metadata> getNetworkMetadata(const JavaHandle& network) {
    network_metadata* attributes = PowsyblCaller::get()->callJava<network_metadata*>(::getNetworkMetadata, network);
    return std::shared_ptr<network_metadata>(attributes, [](network_metadata* ptr){
        PowsyblCaller::get()->callJava(::freeNetworkMetadata, ptr);
    });
}

bool isNetworkLoadable(const std::string& file) {
    return PowsyblCaller::get()->callJava<bool>(::isNetworkLoadable, (char*) file.data());
}

JavaHandle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters, const std::vector<std::string>& postProcessors,
                       JavaHandle* reportNode, bool allowVariantMultiThreadAccess) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    ToCharPtrPtr postProcessorsPtr(postProcessors);
    return PowsyblCaller::get()->callJava<JavaHandle>(::loadNetwork, (char*) file.data(), parameterNamesPtr.get(), parameterNames.size(),
                              parameterValuesPtr.get(), parameterValues.size(), postProcessorsPtr.get(), postProcessors.size(),
                              (reportNode == nullptr) ? nullptr : *reportNode, allowVariantMultiThreadAccess);
}

JavaHandle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters,
                                 const std::vector<std::string>& postProcessors, JavaHandle* reportNode, bool allowVariantMultiThreadAccess) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    ToCharPtrPtr postProcessorsPtr(postProcessors);
    return PowsyblCaller::get()->callJava<JavaHandle>(::loadNetworkFromString, (char*) fileName.data(), (char*) fileContent.data(),
                           parameterNamesPtr.get(), parameterNames.size(), parameterValuesPtr.get(), parameterValues.size(),
                           postProcessorsPtr.get(), postProcessors.size(), (reportNode == nullptr) ? nullptr : *reportNode,
                           allowVariantMultiThreadAccess);
}

void saveNetwork(const JavaHandle& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    PowsyblCaller::get()->callJava(::saveNetwork, network, (char*) file.data(), (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
                parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode);
}

std::string saveNetworkToString(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reportNode) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    ToCharPtrPtr parameterNamesPtr(parameterNames);
    ToCharPtrPtr parameterValuesPtr(parameterValues);
    return toString(PowsyblCaller::get()->callJava<char*>(::saveNetworkToString, network, (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
             parameterValuesPtr.get(), parameterValues.size(), (reportNode == nullptr) ? nullptr : *reportNode));
}

void reduceNetwork(const JavaHandle& network, double v_min, double v_max, const std::vector<std::string>& ids,
                   const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines) {
    ToCharPtrPtr elementIdPtr(ids);
    ToCharPtrPtr vlsPtr(vls);
    ToIntPtr depthsPtr(depths);
    PowsyblCaller::get()->callJava(::reduceNetwork, network, v_min, v_max, elementIdPtr.get(), ids.size(), vlsPtr.get(), vls.size(), depthsPtr.get(), depths.size(), withDangLingLines);
}

bool updateSwitchPosition(const JavaHandle& network, const std::string& id, bool open) {
    return PowsyblCaller::get()->callJava<bool>(::updateSwitchPosition, network, (char*) id.data(), open);
}

bool updateConnectableStatus(const JavaHandle& network, const std::string& id, bool connected) {
    return PowsyblCaller::get()->callJava<bool>(::updateConnectableStatus, network, (char*) id.data(), connected);
}

std::vector<std::string> getNetworkElementsIds(const JavaHandle& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides) {
    ToDoublePtr nominalVoltagePtr(nominalVoltages);
    ToCharPtrPtr countryPtr(countries);
    auto elementsIdsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkElementsIds, network, elementType,
                                                       nominalVoltagePtr.get(), nominalVoltages.size(),
                                                       countryPtr.get(), countries.size(), mainCc, mainSc,
                                                       notConnectedToSameBusAtBothSides);
    ToStringVector elementsIds(elementsIdsArrayPtr);
    return elementsIds.get();
}

LoadFlowParameters* createLoadFlowParameters() {
    loadflow_parameters* parameters_ptr = PowsyblCaller::get()->callJava<loadflow_parameters*>(::createLoadFlowParameters);
    auto parameters = std::shared_ptr<loadflow_parameters>(parameters_ptr, [](loadflow_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       PowsyblCaller::get()->callJava(::freeLoadFlowParameters, ptr);
    });
    return new LoadFlowParameters(parameters.get());
}

RaoParameters* createRaoParameters() {
    rao_parameters* parameters_ptr = PowsyblCaller::get()->callJava<rao_parameters*>(::createRaoParameters);
    auto parameters = std::shared_ptr<rao_parameters>(parameters_ptr, [](rao_parameters* ptr){
        //Memory has been allocated on java side, we need to clean it up on java side
        PowsyblCaller::get()->callJava(::freeRaoParameters, ptr);
    });
    return new RaoParameters(parameters.get());
}

LoadFlowValidationParameters* createValidationConfig() {
    loadflow_validation_parameters* parameters_ptr = PowsyblCaller::get()->callJava<loadflow_validation_parameters*>(::createValidationConfig);
    auto parameters = std::shared_ptr<loadflow_validation_parameters>(parameters_ptr, [](loadflow_validation_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       PowsyblCaller::get()->callJava(::freeValidationConfig, ptr);
    });
    return new LoadFlowValidationParameters(parameters.get());
}


SecurityAnalysisParameters* createSecurityAnalysisParameters() {
    security_analysis_parameters* parameters_ptr = PowsyblCaller::get()->callJava<security_analysis_parameters*>(::createSecurityAnalysisParameters);
    auto parameters = std::shared_ptr<security_analysis_parameters>(parameters_ptr, [](security_analysis_parameters* ptr){
        PowsyblCaller::get()->callJava(::freeSecurityAnalysisParameters, ptr);
    });
    return new SecurityAnalysisParameters(parameters.get());
}

SensitivityAnalysisParameters* createSensitivityAnalysisParameters() {
    sensitivity_analysis_parameters* parameters_ptr = PowsyblCaller::get()->callJava<sensitivity_analysis_parameters*>(::createSensitivityAnalysisParameters);
    return createSensitivityAnalysisParametersFromCStruct(parameters_ptr);
}

SensitivityAnalysisParameters* createSensitivityAnalysisParametersFromCStruct(sensitivity_analysis_parameters* parameters_ptr) {
    auto parameters = std::shared_ptr<sensitivity_analysis_parameters>(parameters_ptr, [](sensitivity_analysis_parameters* ptr){
        PowsyblCaller::get()->callJava(::freeSensitivityAnalysisParameters, ptr);
    });
    return new SensitivityAnalysisParameters(parameters.get());
}

LoadFlowComponentResultArray* runLoadFlow(const JavaHandle& network, bool dc, const LoadFlowParameters& parameters,
                                          const std::string& provider, JavaHandle* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return new LoadFlowComponentResultArray(
            PowsyblCaller::get()->callJava<array*>(::runLoadFlow, network, dc, c_parameters.get(), (char *) provider.data(), (reportNode == nullptr) ? nullptr : *reportNode));
}

SeriesArray* runLoadFlowValidation(const JavaHandle& network, validation_type validationType, const LoadFlowValidationParameters& loadflow_validation_parameters) {
    auto c_validation_parameters = loadflow_validation_parameters.to_c_struct();
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::runLoadFlowValidation, network, validationType, c_validation_parameters.get()));
}

void writeSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    PowsyblCaller::get()->callJava(::writeSingleLineDiagramSvg, network, (char*) containerId.data(), (char*) svgFile.data(), (char*) metadataFile.data(), c_parameters.get());
}

void writeMatrixMultiSubstationSingleLineDiagramSvg(const JavaHandle& network, const std::vector<std::vector<std::string>>& matrixIds, const std::string& svgFile, const std::string& metadataFile, const SldParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    int nbRows = matrixIds.size();
    std::vector<std::string> substationIds;
    for (int row = 0; row < nbRows; ++row) {
        const std::vector<std::string>& colIds = matrixIds[row];
        for (int col = 0; col < matrixIds[row].size(); ++col) {
            substationIds.push_back(colIds[col]);
        }
    }
    ToCharPtrPtr substationIdPtr(substationIds);
    PowsyblCaller::get()->callJava(::writeMatrixMultiSubstationSingleLineDiagramSvg, network, substationIdPtr.get(), substationIds.size(), nbRows, (char*) svgFile.data(), (char*) metadataFile.data(), c_parameters.get());
}

std::string getSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId) {
    return toString(PowsyblCaller::get()->callJava<char*>(::getSingleLineDiagramSvg, network, (char*) containerId.data()));
}

std::vector<std::string> getSingleLineDiagramSvgAndMetadata(const JavaHandle& network, const std::string& containerId, const SldParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    auto svgAndMetadataArrayPtr = PowsyblCaller::get()->callJava<array*>(::getSingleLineDiagramSvgAndMetadata, network, (char*) containerId.data(), c_parameters.get());
    ToStringVector svgAndMetadata(svgAndMetadataArrayPtr);
    return svgAndMetadata.get();
}

std::vector<std::string> getMatrixMultiSubstationSvgAndMetadata(const JavaHandle& network, const std::vector<std::vector<std::string>>& matrixIds, const SldParameters& parameters){
    auto c_parameters = parameters.to_c_struct();
    int nbRows = matrixIds.size();
    std::vector<std::string> substationIds;
    for (int row = 0; row < nbRows; ++row) {
        const std::vector<std::string>& colIds = matrixIds[row];
        for (int col = 0; col < matrixIds[row].size(); ++col) {
            substationIds.push_back(colIds[col]);
        }
    }
    ToCharPtrPtr substationIdPtr(substationIds);
    auto svgAndMetadataArrayPtr = PowsyblCaller::get()->callJava<array*>(::getMatrixMultiSubstationSvgAndMetadata, network, substationIdPtr.get(), substationIds.size(), nbRows, c_parameters.get());
    ToStringVector svgAndMetadata(svgAndMetadataArrayPtr);
    return svgAndMetadata.get();
}

void writeNetworkAreaDiagramSvg(const JavaHandle& network, const std::string& svgFile, const std::string& metadataFile, const std::vector<std::string>& voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters, dataframe* fixed_positions,
    dataframe* branch_labels, dataframe* three_wt_labels, dataframe* bus_descriptions, dataframe* vl_descriptions, dataframe* bus_node_styles, dataframe* edge_styles, dataframe* three_wt_styles) {
    auto c_parameters = parameters.to_c_struct();
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    PowsyblCaller::get()->callJava(::writeNetworkAreaDiagramSvg, network, (char*) svgFile.data(), (char*) metadataFile.data(),
        voltageLevelIdPtr.get(), voltageLevelIds.size(), depth, highNominalVoltageBound, lowNominalVoltageBound, c_parameters.get(), fixed_positions, branch_labels, three_wt_labels, bus_descriptions, vl_descriptions, bus_node_styles, edge_styles, three_wt_styles);
}

std::string getNetworkAreaDiagramSvg(const JavaHandle& network, const std::vector<std::string>&  voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    return toString(PowsyblCaller::get()->callJava<char*>(::getNetworkAreaDiagramSvg, network, voltageLevelIdPtr.get(), voltageLevelIds.size(), depth, highNominalVoltageBound, lowNominalVoltageBound, c_parameters.get()));
}

std::vector<std::string> getNetworkAreaDiagramSvgAndMetadata(const JavaHandle& network, const std::vector<std::string>&  voltageLevelIds, int depth, double highNominalVoltageBound, double lowNominalVoltageBound, const NadParameters& parameters, dataframe* fixed_positions,
    dataframe* branch_labels, dataframe* three_wt_labels, dataframe* bus_descriptions, dataframe* vl_descriptions, dataframe* bus_node_styles, dataframe* edge_styles, dataframe* three_wt_styles) {
    auto c_parameters = parameters.to_c_struct();
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    auto svgAndMetadataArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkAreaDiagramSvgAndMetadata, network, voltageLevelIdPtr.get(), voltageLevelIds.size(), depth, highNominalVoltageBound, lowNominalVoltageBound, c_parameters.get(), fixed_positions, branch_labels, three_wt_labels, bus_descriptions, vl_descriptions, bus_node_styles, edge_styles, three_wt_styles);
    ToStringVector svgAndMetadata(svgAndMetadataArrayPtr);
    return svgAndMetadata.get();
}

std::vector<std::string> getNetworkAreaDiagramDisplayedVoltageLevels(const JavaHandle& network, const std::vector<std::string>& voltageLevelIds, int depth) {
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    auto displayedVoltageLevelIdsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getNetworkAreaDiagramDisplayedVoltageLevels, network, voltageLevelIdPtr.get(), voltageLevelIds.size(), depth);
    ToStringVector displayedVoltageLevelIds(displayedVoltageLevelIdsArrayPtr);
    return displayedVoltageLevelIds.get();
}

SeriesArray* getNetworkAreaDiagramDefaultBranchLabels(const JavaHandle& network) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNetworkAreaDiagramDefaultBranchLabels, network));
}

SeriesArray* getNetworkAreaDiagramDefaultTwtLabels(const JavaHandle& network) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNetworkAreaDiagramDefaultThreeWtLabels, network));
}

SeriesArray* getNetworkAreaDiagramDefaultBusDescriptions(const JavaHandle& network) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNetworkAreaDiagramDefaultBusDescriptions, network));
}

SeriesArray* getNetworkAreaDiagramDefaultVoltageLevelDescriptions(const JavaHandle& network) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNetworkAreaDiagramDefaultVlDescriptions, network));
}

JavaHandle createSecurityAnalysis() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createSecurityAnalysis);
}

void addContingency(const JavaHandle& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    PowsyblCaller::get()->callJava(::addContingency, analysisContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

void addContingencyFromJsonFile(const JavaHandle& analysisContext, const std::string& jsonFilePath) {
    PowsyblCaller::get()->callJava(::addContingencyFromJsonFile, analysisContext, (char*) jsonFilePath.data());
}

void exportToJson(const JavaHandle& securityAnalysisResult, const std::string& jsonFilePath) {
    PowsyblCaller::get()->callJava(::exportToJson, securityAnalysisResult, (char*) jsonFilePath.data());
}

JavaHandle runSecurityAnalysis(const JavaHandle& securityAnalysisContext, const JavaHandle& network, const SecurityAnalysisParameters& parameters,
                               const std::string& provider, bool dc, JavaHandle* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return PowsyblCaller::get()->callJava<JavaHandle>(::runSecurityAnalysis, securityAnalysisContext, network, c_parameters.get(), (char *) provider.data(), dc, (reportNode == nullptr) ? nullptr : *reportNode);
}

JavaHandle createSensitivityAnalysis() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createSensitivityAnalysis);
}

void addLoadActivePowerAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double activePower) {
    PowsyblCaller::get()->callJava(::addLoadActivePowerAction, analysisContext, (char*) actionId.data(), (char*) loadId.data(), relativeValue, activePower);
}

void addLoadReactivePowerAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& loadId, bool relativeValue, double reactivePower) {
    PowsyblCaller::get()->callJava(::addLoadReactivePowerAction, analysisContext, (char*) actionId.data(), (char*) loadId.data(), relativeValue, reactivePower);
}

void addGeneratorActivePowerAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& generatorId, bool relativeValue, double activePower) {
    PowsyblCaller::get()->callJava(::addGeneratorActivePowerAction, analysisContext, (char*) actionId.data(), (char*) generatorId.data(), relativeValue, activePower);
}

void addSwitchAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& switchId, bool open) {
    PowsyblCaller::get()->callJava(::addSwitchAction, analysisContext, (char*) actionId.data(), (char*) switchId.data(), open);
}

void addPhaseTapChangerPositionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& transformerId,
                                      bool isRelative, int tapPosition, ThreeSide side) {
    PowsyblCaller::get()->callJava(::addPhaseTapChangerPositionAction, analysisContext, (char*) actionId.data(), (char*) transformerId.data(), isRelative, tapPosition, side);
}

void addRatioTapChangerPositionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& transformerId,
                                      bool isRelative, int tapPosition, ThreeSide side) {
    PowsyblCaller::get()->callJava(::addRatioTapChangerPositionAction, analysisContext, (char*) actionId.data(), (char*) transformerId.data(), isRelative, tapPosition, side);
}

void addShuntCompensatorPositionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& shuntId,
                                       int sectionCount) {
    PowsyblCaller::get()->callJava(::addShuntCompensatorPositionAction, analysisContext, (char*) actionId.data(), (char*) shuntId.data(), sectionCount);
}

void addTerminalsConnectionAction(const JavaHandle& analysisContext, const std::string& actionId, const std::string& elementId,
                                       ThreeSide side, bool opening) {
    PowsyblCaller::get()->callJava(::addTerminalsConnectionAction, analysisContext, (char*) actionId.data(), (char*) elementId.data(),
     side, opening);
}

void addOperatorStrategy(const JavaHandle& analysisContext, std::string operatorStrategyId, std::string contingencyId, const std::vector<std::string>& actionsIds,
                         condition_type conditionType, const std::vector<std::string>& subjectIds, const std::vector<violation_type>& violationTypesFilters) {
    ToCharPtrPtr actionsPtr(actionsIds);
    ToCharPtrPtr subjectIdsPtr(subjectIds);
    std::vector<int> violationTypes;
    for(int i = 0; i < violationTypesFilters.size(); ++i) {
        violationTypes.push_back(violationTypesFilters[i]);
    }
    ToIntPtr violationTypesPtr(violationTypes);
    PowsyblCaller::get()->callJava(::addOperatorStrategy, analysisContext, (char*) operatorStrategyId.data(), (char*) contingencyId.data(), actionsPtr.get(), actionsIds.size(),
        conditionType, subjectIdsPtr.get(), subjectIds.size(), violationTypesPtr.get(), violationTypesFilters.size());
}

void addActionFromJsonFile(const JavaHandle& analysisContext, const std::string& jsonFilePath) {
      PowsyblCaller::get()->callJava(::addActionFromJsonFile, analysisContext, (char*) jsonFilePath.data());
}

void addOperatorStrategyFromJsonFile(const JavaHandle& analysisContext, const std::string& jsonFilePath) {
      PowsyblCaller::get()->callJava(::addOperatorStrategyFromJsonFile, analysisContext, (char*) jsonFilePath.data());
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

class ZonesPtr {
public:
    ZonesPtr(const std::vector<zone*>& vector)
        : vector_(vector) {
    }

    ~ZonesPtr() {
        for (auto z : vector_) {
            deleteZone(z);
        }
    }

    ::zone** get() const {
        return (::zone**) &vector_[0];
    }

private:
    const std::vector<::zone*>& vector_;
};

void setZones(const JavaHandle& sensitivityAnalysisContext, const std::vector<::zone*>& zones) {
    ZonesPtr zonesPtr(zones);
    PowsyblCaller::get()->callJava(::setZones, sensitivityAnalysisContext, zonesPtr.get(), zones.size());
}

void addFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                     const std::vector<std::string>& variablesIds, const std::vector<std::string>& contingenciesIds, contingency_context_type ContingencyContextType,
                     sensitivity_function_type sensitivityFunctionType, sensitivity_variable_type sensitivityVariableType) {
       ToCharPtrPtr branchIdPtr(branchesIds);
       ToCharPtrPtr variableIdPtr(variablesIds);
       ToCharPtrPtr contingenciesIdPtr(contingenciesIds);
       PowsyblCaller::get()->callJava(::addFactorMatrix, sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                  variableIdPtr.get(), variablesIds.size(), contingenciesIdPtr.get(), contingenciesIds.size(), 
                  (char*) matrixId.c_str(), ContingencyContextType, sensitivityFunctionType, sensitivityVariableType);
}

JavaHandle runSensitivityAnalysis(const JavaHandle& sensitivityAnalysisContext, const JavaHandle& network, bool dc, SensitivityAnalysisParameters& parameters, const std::string& provider, JavaHandle* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return PowsyblCaller::get()->callJava<JavaHandle>(::runSensitivityAnalysis, sensitivityAnalysisContext, network, dc, c_parameters.get(), (char *) provider.data(), (reportNode == nullptr) ? nullptr : *reportNode);
}

matrix* getSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId) {
    return PowsyblCaller::get()->callJava<matrix*>(::getSensitivityMatrix, sensitivityAnalysisResultContext,
                                (char*) matrixId.c_str(), (char*) contingencyId.c_str());
}

matrix* getReferenceMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId) {
    return PowsyblCaller::get()->callJava<matrix*>(::getReferenceMatrix, sensitivityAnalysisResultContext,
                                (char*) matrixId.c_str(), (char*) contingencyId.c_str());
}

SeriesArray* createNetworkElementsSeriesArray(const JavaHandle& network, element_type elementType, filter_attributes_type filterAttributesType, const std::vector<std::string>& attributes, dataframe* dataframe, bool perUnit, double nominalApparentPower) {
	ToCharPtrPtr attributesPtr(attributes);
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::createNetworkElementsSeriesArray, network, elementType, filterAttributesType, attributesPtr.get(), attributes.size(), dataframe, perUnit, nominalApparentPower));
}

SeriesArray* createNetworkElementsExtensionSeriesArray(const JavaHandle& network, const std::string& extensionName, const std::string& tableName) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::createNetworkElementsExtensionSeriesArray, network, (char*) extensionName.c_str(), (char*) tableName.c_str()));
}

std::vector<std::string> getExtensionsNames() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getExtensionsNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

SeriesArray* getExtensionsInformation() {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getExtensionsInformation));
}

std::string getWorkingVariantId(const JavaHandle& network) {
    return toString(PowsyblCaller::get()->callJava<char*>(::getWorkingVariantId, network));
}

void setWorkingVariant(const JavaHandle& network, std::string& variant) {
    PowsyblCaller::get()->callJava<>(::setWorkingVariant, network, (char*) variant.c_str());
}

void removeVariant(const JavaHandle& network, std::string& variant) {
    PowsyblCaller::get()->callJava<>(::removeVariant, network, (char*) variant.c_str());
}

void cloneVariant(const JavaHandle& network, std::string& src, std::string& variant, bool mayOverwrite) {
    PowsyblCaller::get()->callJava<>(::cloneVariant, network, (char*) src.c_str(), (char*) variant.c_str(), mayOverwrite);
}

std::vector<std::string> getVariantsIds(const JavaHandle& network) {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getVariantsIds, network);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

void addMonitoredElements(const JavaHandle& securityAnalysisContext, contingency_context_type contingencyContextType, const std::vector<std::string>& branchIds,
                      const std::vector<std::string>& voltageLevelIds, const std::vector<std::string>& threeWindingsTransformerIds,
                      const std::vector<std::string>& contingencyIds) {
    ToCharPtrPtr branchIdsPtr(branchIds);
    ToCharPtrPtr voltageLevelIdsPtr(voltageLevelIds);
    ToCharPtrPtr threeWindingsTransformerIdsPtr(threeWindingsTransformerIds);
    ToCharPtrPtr contingencyIdsPtr(contingencyIds);
    PowsyblCaller::get()->callJava<>(::addMonitoredElements, securityAnalysisContext, contingencyContextType, branchIdsPtr.get(), branchIds.size(),
    voltageLevelIdsPtr.get(), voltageLevelIds.size(), threeWindingsTransformerIdsPtr.get(),
    threeWindingsTransformerIds.size(), contingencyIdsPtr.get(), contingencyIds.size());
}

PostContingencyResultArray* getPostContingencyResults(const JavaHandle& securityAnalysisResult) {
    return new PostContingencyResultArray(PowsyblCaller::get()->callJava<array*>(::getPostContingencyResults, securityAnalysisResult));
}

OperatorStrategyResultArray* getOperatorStrategyResults(const JavaHandle& securityAnalysisResult) {
    return new OperatorStrategyResultArray(PowsyblCaller::get()->callJava<array*>(::getOperatorStrategyResults, securityAnalysisResult));
}

pre_contingency_result* getPreContingencyResult(const JavaHandle& securityAnalysisResult) {
    return PowsyblCaller::get()->callJava<pre_contingency_result*>(::getPreContingencyResult, securityAnalysisResult);
}

SeriesArray* getLimitViolations(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getLimitViolations, securityAnalysisResult));
}

SeriesArray* getBranchResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getBranchResults, securityAnalysisResult));
}

SeriesArray* getBusResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getBusResults, securityAnalysisResult));
}

SeriesArray* getThreeWindingsTransformerResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getThreeWindingsTransformerResults, securityAnalysisResult));
}

SeriesArray* getNodeBreakerViewSwitches(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNodeBreakerViewSwitches, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getNodeBreakerViewNodes(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNodeBreakerViewNodes, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getNodeBreakerViewInternalConnections(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getNodeBreakerViewInternalConnections, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getBusBreakerViewSwitches(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getBusBreakerViewSwitches, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getBusBreakerViewBuses(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getBusBreakerViewBuses, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getBusBreakerViewElements(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getBusBreakerViewElements, network, (char*) voltageLevel.c_str()));
}

void updateNetworkElementsWithSeries(pypowsybl::JavaHandle network, dataframe* dataframe, element_type elementType, bool perUnit, double nominalApparentPower) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::updateNetworkElementsWithSeries, network, elementType, dataframe, perUnit, nominalApparentPower);
}

std::vector<SeriesMetadata> convertDataframeMetadata(dataframe_metadata* dataframeMetadata) {
    std::vector<SeriesMetadata> res;
    for (int i = 0; i < dataframeMetadata->attributes_count; i++) {
        const series_metadata& series = dataframeMetadata->attributes_metadata[i];
        res.push_back(SeriesMetadata(series.name, series.type, series.is_index, series.is_modifiable, series.is_default));
    }
    return res;
}

std::vector<SeriesMetadata> getNetworkDataframeMetadata(element_type elementType) {
    dataframe_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframe_metadata*>(::getSeriesMetadata, elementType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    PowsyblCaller::get()->callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<std::vector<SeriesMetadata>> getNetworkElementCreationDataframesMetadata(element_type elementType) {

    dataframes_metadata* allDataframesMetadata = pypowsybl::PowsyblCaller::get()->callJava<dataframes_metadata*>(::getCreationMetadata, elementType);
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < allDataframesMetadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(allDataframesMetadata->dataframes_metadata + i));
    }
    pypowsybl::PowsyblCaller::get()->callJava(::freeDataframesMetadata, allDataframesMetadata);
    return res;
}

void createElement(pypowsybl::JavaHandle network, dataframe_array* dataframes, element_type elementType) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::createElement, network, elementType, dataframes);
}

::validation_level_type getValidationLevel(const JavaHandle& network) {
    // TBD
    //return validation_level_type::EQUIPMENT;
    return PowsyblCaller::get()->callJava<validation_level_type>(::getValidationLevel, network);
}

::validation_level_type validate(const JavaHandle& network) {
    // TBD
    //return validation_level_type::STEADY_STATE_HYPOTHESIS;
    return PowsyblCaller::get()->callJava<validation_level_type>(::validate, network);
}

void setMinValidationLevel(pypowsybl::JavaHandle network, validation_level_type validationLevel) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::setMinValidationLevel, network, validationLevel);
}

void setupLoggerCallback(void *& callback) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::setupLoggerCallback, callback);
}

void removeNetworkElements(const JavaHandle& network, const std::vector<std::string>& elementIds) {
    ToCharPtrPtr elementIdsPtr(elementIds);
    pypowsybl::PowsyblCaller::get()->callJava<>(::removeNetworkElements, network, elementIdsPtr.get(), elementIds.size());
}

void addNetworkElementProperties(pypowsybl::JavaHandle network, dataframe* dataframe) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::addNetworkElementProperties, network, dataframe);
}

void removeNetworkElementProperties(pypowsybl::JavaHandle network, const std::vector<std::string>& ids, const std::vector<std::string>& properties) {
    ToCharPtrPtr idsPtr(ids);
    ToCharPtrPtr propertiesPtr(properties);
    pypowsybl::PowsyblCaller::get()->callJava<>(::removeNetworkElementProperties, network, idsPtr.get(), ids.size(), propertiesPtr.get(), properties.size());
}

std::vector<std::string> getLoadFlowProviderParametersNames(const std::string& loadFlowProvider) {
    auto providerParametersArrayPtr = pypowsybl::PowsyblCaller::get()->callJava<array*>(::getLoadFlowProviderParametersNames, (char*) loadFlowProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

SeriesArray* createLoadFlowProviderParametersSeriesArray(const std::string& provider) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::createLoadFlowProviderParametersSeriesArray, (char*) provider.data()));
}

std::vector<std::string> getSecurityAnalysisProviderParametersNames(const std::string& securityAnalysisProvider) {
    auto providerParametersArrayPtr = pypowsybl::PowsyblCaller::get()->callJava<array*>(::getSecurityAnalysisProviderParametersNames, (char*) securityAnalysisProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

std::vector<std::string> getSensitivityAnalysisProviderParametersNames(const std::string& sensitivityAnalysisProvider) {
    auto providerParametersArrayPtr = pypowsybl::PowsyblCaller::get()->callJava<array*>(::getSensitivityAnalysisProviderParametersNames, (char*) sensitivityAnalysisProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

void updateNetworkElementsExtensionsWithSeries(pypowsybl::JavaHandle network, std::string& name, std::string& tableName, dataframe* dataframe) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::updateNetworkElementsExtensionsWithSeries, network, (char*) name.data(), (char*) tableName.data(), dataframe);
}

void removeExtensions(const JavaHandle& network, std::string& name, const std::vector<std::string>& ids) {
    ToCharPtrPtr idsPtr(ids);
    pypowsybl::PowsyblCaller::get()->callJava<>(::removeExtensions, network, (char*) name.data(), idsPtr.get(), ids.size());
}

std::vector<SeriesMetadata> getNetworkExtensionsDataframeMetadata(std::string& name, std::string& tableName) {
    dataframe_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframe_metadata*>(::getExtensionSeriesMetadata, (char*) name.data(), (char*) tableName.data());
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    PowsyblCaller::get()->callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<std::vector<SeriesMetadata>> getNetworkExtensionsCreationDataframesMetadata(std::string& name) {
    dataframes_metadata* allDataframesMetadata = pypowsybl::PowsyblCaller::get()->callJava<dataframes_metadata*>(::getExtensionsCreationMetadata, (char*) name.data());
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < allDataframesMetadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(allDataframesMetadata->dataframes_metadata + i));
    }
    pypowsybl::PowsyblCaller::get()->callJava(::freeDataframesMetadata, allDataframesMetadata);
    return res;
}

void createExtensions(pypowsybl::JavaHandle network, dataframe_array* dataframes, std::string& name) {
        pypowsybl::PowsyblCaller::get()->callJava<>(::createExtensions, network, (char*) name.data(), dataframes);

}

JavaHandle createGLSKdocument(std::string& filename) {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createGLSKdocument, (char*) filename.c_str());
}

std::vector<std::string> getGLSKinjectionkeys(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant) {
    auto keysArrayPtr = PowsyblCaller::get()->callJava<array*>(::getGLSKinjectionkeys, network, importer, (char*) country.c_str(), instant);
    ToStringVector keys(keysArrayPtr);
    return keys.get();
}

std::vector<std::string> getGLSKcountries(const JavaHandle& importer) {
    auto countriesArrayPtr = PowsyblCaller::get()->callJava<array*>(::getGLSKcountries, importer);
    ToStringVector countries(countriesArrayPtr);
    return countries.get();
}

std::vector<double> getGLSKInjectionFactors(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant) {
    auto countriesArrayPtr = PowsyblCaller::get()->callJava<array*>(::getInjectionFactor, network, importer, (char*) country.c_str(), instant);
    ToPrimitiveVector<double> values(countriesArrayPtr);
    return values.get();
}

long getInjectionFactorStartTimestamp(const JavaHandle& importer) {
    return PowsyblCaller::get()->callJava<long>(::getInjectionFactorStartTimestamp, importer);
}

long getInjectionFactorEndTimestamp(const JavaHandle& importer) {
    return PowsyblCaller::get()->callJava<long>(::getInjectionFactorEndTimestamp, importer);
}

JavaHandle createReportNode(const std::string& taskKey, const std::string& defaultName) {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createReportNode, (char*) taskKey.data(), (char*) defaultName.data());
}

std::string printReport(const JavaHandle& reportNodeModel) {
    return toString(PowsyblCaller::get()->callJava<char*>(::printReport, reportNodeModel));
}

std::string jsonReport(const JavaHandle& reportNodeModel) {
    return toString(PowsyblCaller::get()->callJava<char*>(::jsonReport, reportNodeModel));
}

JavaHandle createFlowDecomposition() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createFlowDecomposition);
}

void addContingencyForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    PowsyblCaller::get()->callJava(::addContingencyForFlowDecomposition, flowDecompositionContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

void addPrecontingencyMonitoredElementsForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::vector<std::string>& branchIds) {
    ToCharPtrPtr branchIdPtr(branchIds);
    PowsyblCaller::get()->callJava(::addPrecontingencyMonitoredElementsForFlowDecomposition, flowDecompositionContext, branchIdPtr.get(), branchIds.size());
}

void addPostcontingencyMonitoredElementsForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::vector<std::string>& branchIds, const std::vector<std::string>& contingencyIds) {
    ToCharPtrPtr branchIdPtr(branchIds);
    ToCharPtrPtr contingencyIdPtr(contingencyIds);
    PowsyblCaller::get()->callJava(::addPostcontingencyMonitoredElementsForFlowDecomposition, flowDecompositionContext, branchIdPtr.get(), branchIds.size(), contingencyIdPtr.get(), contingencyIds.size());
}

void addAdditionalXnecProviderForFlowDecomposition(const JavaHandle& flowDecompositionContext, DefaultXnecProvider defaultXnecProvider) {
    PowsyblCaller::get()->callJava(::addAdditionalXnecProviderForFlowDecomposition, flowDecompositionContext, defaultXnecProvider);
}

SeriesArray* runFlowDecomposition(const JavaHandle& flowDecompositionContext, const JavaHandle& network, const FlowDecompositionParameters& flow_decomposition_parameters, const LoadFlowParameters& loadflow_parameters) {
    auto c_flow_decomposition_parameters = flow_decomposition_parameters.to_c_struct();
    auto c_loadflow_parameters  = loadflow_parameters.to_c_struct();
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::runFlowDecomposition, flowDecompositionContext, network, c_flow_decomposition_parameters.get(), c_loadflow_parameters.get()));
}

FlowDecompositionParameters* createFlowDecompositionParameters() {
    flow_decomposition_parameters* parameters_ptr = PowsyblCaller::get()->callJava<flow_decomposition_parameters*>(::createFlowDecompositionParameters);
    auto parameters = std::shared_ptr<flow_decomposition_parameters>(parameters_ptr, [](flow_decomposition_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       PowsyblCaller::get()->callJava(::freeFlowDecompositionParameters, ptr);
    });
    return new FlowDecompositionParameters(parameters.get());
}

SeriesArray* getConnectablesOrderPositions(const JavaHandle& network, const std::string voltage_level_id) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getConnectablesOrderPositions, network, (char*) voltage_level_id.c_str()));
}

std::vector<int> getUnusedConnectableOrderPositions(const pypowsybl::JavaHandle network, const std::string busbarSectionId, const std::string beforeOrAfter) {
    auto positionsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getUnusedConnectableOrderPositions, network, (char*) busbarSectionId.c_str(), (char*) beforeOrAfter.c_str());
    ToPrimitiveVector<int> res(positionsArrayPtr);
    return res.get();
}

void removeAliases(pypowsybl::JavaHandle network, dataframe* dataframe) {
    pypowsybl::PowsyblCaller::get()->callJava(::removeAliases, network, dataframe);
}

void removeInternalConnections(pypowsybl::JavaHandle network, dataframe* dataframe) {
    pypowsybl::PowsyblCaller::get()->callJava(::removeInternalConnections, network, dataframe);
}

void closePypowsybl() {
    pypowsybl::PowsyblCaller::get()->callJava(::closePypowsybl);
    freeArgv();
}

SldParameters::SldParameters(sld_parameters* src) {
    use_name = (bool) src->use_name;
    center_name = (bool) src->center_name;
    diagonal_label = (bool) src->diagonal_label;
    nodes_infos = (bool) src->nodes_infos;
    tooltip_enabled = (bool) src->tooltip_enabled;
    topological_coloring = (bool) src->topological_coloring;
    component_library = toString(src->component_library);
    display_current_feeder_info = (bool) src->display_current_feeder_info;
    active_power_unit =  toString(src->active_power_unit);
    reactive_power_unit =  toString(src->reactive_power_unit);
    current_unit =  toString(src->current_unit);
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
    layout_type = static_cast<NadLayoutType>(src->layout_type);
    scaling_factor = src->scaling_factor;
    radius_factor = src->radius_factor;
    edge_info_displayed = static_cast<EdgeInfoType>(src->edge_info_displayed);
    voltage_level_details = (bool) src->voltage_level_details;
}

void SldParameters::sld_to_c_struct(sld_parameters& res) const {
    res.use_name = (unsigned char) use_name;
    res.center_name = (unsigned char) center_name;
    res.diagonal_label = (unsigned char) diagonal_label;
    res.nodes_infos = (unsigned char) nodes_infos;
    res.tooltip_enabled = (unsigned char) tooltip_enabled;
    res.topological_coloring = (unsigned char) topological_coloring;
    res.component_library = copyStringToCharPtr(component_library);
    res.display_current_feeder_info = (unsigned char) display_current_feeder_info;
    res.active_power_unit = copyStringToCharPtr(active_power_unit);
    res.reactive_power_unit = copyStringToCharPtr(reactive_power_unit);
    res.current_unit = copyStringToCharPtr(current_unit);
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
    res.layout_type = (int) layout_type;
    res.scaling_factor = scaling_factor;
    res.radius_factor = radius_factor;
    res.edge_info_displayed = (int) edge_info_displayed;
    res.voltage_level_details = (unsigned char) voltage_level_details;
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

SldParameters* createSldParameters() {
    sld_parameters* parameters_ptr = PowsyblCaller::get()->callJava<sld_parameters*>(::createSldParameters);
    auto parameters = std::shared_ptr<sld_parameters>(parameters_ptr, [](sld_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       PowsyblCaller::get()->callJava(::freeSldParameters, ptr);
    });
    return new SldParameters(parameters.get());
}

NadParameters* createNadParameters() {
    nad_parameters* parameters_ptr = PowsyblCaller::get()->callJava<nad_parameters*>(::createNadParameters);
    auto parameters = std::shared_ptr<nad_parameters>(parameters_ptr, [](nad_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       PowsyblCaller::get()->callJava(::freeNadParameters, ptr);
    });
    return new NadParameters(parameters.get());
}

void removeElementsModification(pypowsybl::JavaHandle network, const std::vector<std::string>& connectableIds, dataframe* dataframe, remove_modification_type removeModificationType, bool throwException, JavaHandle* reportNode) {
    ToCharPtrPtr connectableIdsPtr(connectableIds);
    pypowsybl::PowsyblCaller::get()->callJava(::removeElementsModification, network, connectableIdsPtr.get(), connectableIds.size(), dataframe, removeModificationType, throwException, (reportNode == nullptr) ? nullptr : *reportNode);
}

/*---------------------------------DYNAMIC MODELLING WITH DYNAWO---------------------------*/
JavaHandle createDynamicSimulationContext() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createDynamicSimulationContext);
}

JavaHandle createDynamicModelMapping() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createDynamicModelMapping);
}

JavaHandle createTimeseriesMapping() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createTimeseriesMapping);
}

JavaHandle createEventMapping() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createEventMapping);
}

JavaHandle runDynamicModel(JavaHandle dynamicModelContext, JavaHandle network, JavaHandle dynamicMapping, JavaHandle eventMapping, JavaHandle timeSeriesMapping, int start, int stop, JavaHandle *reportNode) {
    return PowsyblCaller::get()->callJava<JavaHandle>(::runDynamicModel, dynamicModelContext, network, dynamicMapping, eventMapping, timeSeriesMapping, start, stop, (reportNode == nullptr) ? nullptr : *reportNode);
}

void addDynamicMappings(JavaHandle dynamicMappingHandle, DynamicMappingType mappingType, dataframe_array* dataframes) {
    PowsyblCaller::get()->callJava<>(::addDynamicMappings, dynamicMappingHandle, mappingType, dataframes);
}

void addEventMappings(JavaHandle eventMappingHandle, EventMappingType mappingType, dataframe* mappingDf) {
    PowsyblCaller::get()->callJava<>(::addEventMappings, eventMappingHandle, mappingType, mappingDf);
}

void addOutputVariables(JavaHandle outputVariablesHandle, std::string dynamicId, std::vector<std::string>& variables, bool isDynamic, OutputVariableType variableType) {
    ToCharPtrPtr variablesPtr(variables);
    PowsyblCaller::get()->callJava<>(::addOutputVariables, outputVariablesHandle, (char*) dynamicId.c_str(), variablesPtr.get(), variables.size(), isDynamic, variableType);
}

DynamicSimulationStatus getDynamicSimulationResultsStatus(JavaHandle resultsHandle) {
    return PowsyblCaller::get()->callJava<DynamicSimulationStatus>(::getDynamicSimulationResultsStatus, resultsHandle);
}

std::string getDynamicSimulationResultsStatusText(JavaHandle resultsHandle) {
    return PowsyblCaller::get()->callJava<std::string>(::getDynamicSimulationResultsStatusText, resultsHandle);
}

SeriesArray* getDynamicCurve(JavaHandle resultHandle, std::string curveName) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getDynamicCurve, resultHandle, (char*) curveName.c_str()));
}

std::vector<std::string> getAllDynamicCurvesIds(JavaHandle resultHandle) {
    ToStringVector vector(PowsyblCaller::get()->callJava<array*>(::getAllDynamicCurvesIds, resultHandle));
    return vector.get();
}

SeriesArray* getFinalStateValues(JavaHandle resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getFinalStateValues, resultHandle));
}

SeriesArray* getTimeline(JavaHandle resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getTimeline, resultHandle));
}

std::vector<std::string> getSupportedModels(DynamicMappingType mappingType) {
    ToStringVector vector(PowsyblCaller::get()->callJava<array*>(::getSupportedModels, mappingType));
    return vector.get();
}

std::vector<std::vector<SeriesMetadata>> getDynamicMappingsMetaData(DynamicMappingType mappingType) {
    dataframes_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframes_metadata*>(::getDynamicMappingsMetaData, mappingType);
    std::vector<std::vector<SeriesMetadata>> res;
        for (int i =0; i < metadata->dataframes_count; i++) {
            res.push_back(convertDataframeMetadata(metadata->dataframes_metadata + i));
        }
        pypowsybl::PowsyblCaller::get()->callJava(::freeDataframesMetadata, metadata);
        return res;
}

std::vector<SeriesMetadata> getEventMappingsMetaData(EventMappingType mappingType) {
    dataframe_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframe_metadata*>(::getEventMappingsMetaData, mappingType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    PowsyblCaller::get()->callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<SeriesMetadata> getModificationMetadata(network_modification_type networkModificationType) {
    dataframe_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframe_metadata*>(::getModificationMetadata, networkModificationType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    PowsyblCaller::get()->callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<std::vector<SeriesMetadata>> getModificationMetadataWithElementType(network_modification_type networkModificationType, element_type elementType) {
    dataframes_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframes_metadata*>(::getModificationMetadataWithElementType, networkModificationType, elementType);
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < metadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(metadata->dataframes_metadata + i));
    }
    pypowsybl::PowsyblCaller::get()->callJava(::freeDataframesMetadata, metadata);
    return res;
}

void createNetworkModification(pypowsybl::JavaHandle network, dataframe_array* dataframes,  network_modification_type networkModificationType, bool throwException, JavaHandle* reportNode) {
    pypowsybl::PowsyblCaller::get()->callJava(::createNetworkModification, network, dataframes, networkModificationType, throwException, (reportNode == nullptr) ? nullptr : *reportNode);
}

void splitOrMergeTransformers(pypowsybl::JavaHandle network, const std::vector<std::string>& transformerIds, bool merge, JavaHandle* reportNode) {
    ToCharPtrPtr transformerIdsPtr(transformerIds);
    pypowsybl::PowsyblCaller::get()->callJava(::splitOrMergeTransformers, network, transformerIdsPtr.get(), transformerIds.size(), merge, (reportNode == nullptr) ? nullptr : *reportNode);
}

/*---------------------------------SHORT-CIRCUIT ANALYSIS---------------------------*/

void deleteShortCircuitAnalysisParameters(shortcircuit_analysis_parameters* ptr) {
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_keys, ptr->provider_parameters.provider_parameters_keys_count);
    pypowsybl::deleteCharPtrPtr(ptr->provider_parameters.provider_parameters_values, ptr->provider_parameters.provider_parameters_values_count);
}

ShortCircuitAnalysisParameters::ShortCircuitAnalysisParameters(shortcircuit_analysis_parameters* src)
{
    with_feeder_result = (bool) src->with_feeder_result;
    with_limit_violations = (bool) src->with_limit_violations;
    study_type = static_cast<ShortCircuitStudyType>(src->study_type);
    with_fortescue_result = (bool) src->with_fortescue_result;
    with_voltage_result = (bool) src->with_voltage_result;
    min_voltage_drop_proportional_threshold = (double) src->min_voltage_drop_proportional_threshold;
    initial_voltage_profile_mode = static_cast<InitialVoltageProfileMode>(src->initial_voltage_profile_mode);

    providerParametersFromCStruct(src->provider_parameters, provider_parameters_keys, provider_parameters_values);
}

std::shared_ptr<shortcircuit_analysis_parameters> ShortCircuitAnalysisParameters::to_c_struct() const {
    shortcircuit_analysis_parameters* res = new shortcircuit_analysis_parameters();
    res->with_voltage_result = (bool) with_voltage_result;
    res->with_feeder_result = (bool) with_feeder_result;
    res->with_limit_violations = (bool) with_limit_violations;
    res->study_type = study_type;
    res->with_fortescue_result = (bool) with_fortescue_result;
    res->min_voltage_drop_proportional_threshold = min_voltage_drop_proportional_threshold;
    res->initial_voltage_profile_mode = initial_voltage_profile_mode;

    providerParametersToCStruct(res->provider_parameters, provider_parameters_keys, provider_parameters_values);

    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<shortcircuit_analysis_parameters>(res, [](shortcircuit_analysis_parameters* ptr){
        deleteShortCircuitAnalysisParameters(ptr);
        delete ptr;
    });
}

void setDefaultShortCircuitAnalysisProvider(const std::string& shortCircuitAnalysisProvider) {
    PowsyblCaller::get()->callJava<>(::setDefaultShortCircuitAnalysisProvider, (char*) shortCircuitAnalysisProvider.data());
}

std::string getDefaultShortCircuitAnalysisProvider() {
    return toString(PowsyblCaller::get()->callJava<char*>(::getDefaultShortCircuitAnalysisProvider));
}

std::vector<std::string> getShortCircuitAnalysisProviderNames() {
    auto formatsArrayPtr = PowsyblCaller::get()->callJava<array*>(::getShortCircuitAnalysisProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getShortCircuitAnalysisProviderParametersNames(const std::string& shortCircuitAnalysisProvider) {
    auto providerParametersArrayPtr = pypowsybl::PowsyblCaller::get()->callJava<array*>(::getShortCircuitAnalysisProviderParametersNames, (char*) shortCircuitAnalysisProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

JavaHandle createShortCircuitAnalysis() {
    return PowsyblCaller::get()->callJava<JavaHandle>(::createShortCircuitAnalysis);
}

JavaHandle runShortCircuitAnalysis(const JavaHandle& shortCircuitAnalysisContext, const JavaHandle& network, const ShortCircuitAnalysisParameters& parameters,
    const std::string& provider, JavaHandle* reportNode) {
    auto c_parameters = parameters.to_c_struct();
    return PowsyblCaller::get()->callJava<JavaHandle>(::runShortCircuitAnalysis, shortCircuitAnalysisContext, network, c_parameters.get(), (char *) provider.data(), (reportNode == nullptr) ? nullptr : *reportNode);
}

ShortCircuitAnalysisParameters* createShortCircuitAnalysisParameters() {
    shortcircuit_analysis_parameters* parameters_ptr = PowsyblCaller::get()->callJava<shortcircuit_analysis_parameters*>(::createShortCircuitAnalysisParameters);
    auto parameters = std::shared_ptr<shortcircuit_analysis_parameters>(parameters_ptr, [](shortcircuit_analysis_parameters* ptr){
        PowsyblCaller::get()->callJava(::freeShortCircuitAnalysisParameters, ptr);
    });
    return new ShortCircuitAnalysisParameters(parameters.get());
}

std::vector<SeriesMetadata> getFaultsMetaData() {
    dataframe_metadata* metadata = pypowsybl::PowsyblCaller::get()->callJava<dataframe_metadata*>(::getFaultsDataframeMetaData);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    PowsyblCaller::get()->callJava(::freeDataframeMetadata, metadata);
    return res;
}

void setFaults(pypowsybl::JavaHandle analysisContext, dataframe* dataframe) {
    pypowsybl::PowsyblCaller::get()->callJava<>(::setFaults, analysisContext, dataframe);
}

SeriesArray* getFaultResults(const JavaHandle& shortCircuitAnalysisResult, bool withFortescueResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getShortCircuitAnalysisFaultResults, shortCircuitAnalysisResult, withFortescueResult));
}

SeriesArray* getFeederResults(const JavaHandle& shortCircuitAnalysisResult, bool withFortescueResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getShortCircuitAnalysisFeederResults, shortCircuitAnalysisResult, withFortescueResult));
}

SeriesArray* getShortCircuitLimitViolations(const JavaHandle& shortCircuitAnalysisResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getShortCircuitAnalysisLimitViolationsResults, shortCircuitAnalysisResult));
}

SeriesArray* getShortCircuitBusResults(const JavaHandle& shortCircuitAnalysisResult, bool withFortescueResult) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getShortCircuitAnalysisBusResults, shortCircuitAnalysisResult, withFortescueResult));
}

JavaHandle createVoltageInitializerParams() {
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::createVoltageInitializerParams);
}

void voltageInitializerAddSpecificLowVoltageLimits(const JavaHandle& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerAddSpecificLowVoltageLimits, paramsHandle, (char*) voltageLevelId.c_str(), isRelative, limit);
}

void voltageInitializerAddSpecificHighVoltageLimits(const JavaHandle& paramsHandle, const std::string& voltageLevelId, bool isRelative, double limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerAddSpecificHighVoltageLimits, paramsHandle, (char*) voltageLevelId.c_str(), isRelative, limit);
}

void voltageInitializerAddVariableShuntCompensators(const JavaHandle& paramsHandle, const std::string& idPtr) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerAddVariableShuntCompensators, paramsHandle, (char*) idPtr.c_str());
}

void voltageInitializerAddConstantQGenerators(const JavaHandle& paramsHandle, const std::string& idPtr) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerAddConstantQGenerators, paramsHandle, (char*) idPtr.c_str());
}

void voltageInitializerAddVariableTwoWindingsTransformers(const JavaHandle& paramsHandle, const std::string& idPtr) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerAddVariableTwoWindingsTransformers, paramsHandle, (char*) idPtr.c_str());
}

void voltageInitializerAddConfiguredReactiveSlackBuses(const JavaHandle& paramsHandle, const std::string& idPtr) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerAddConfiguredReactiveSlackBuses, paramsHandle, (char*) idPtr.c_str());
}

void voltageInitializerSetObjective(const JavaHandle& paramsHandle, VoltageInitializerObjective cObjective) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetObjective, paramsHandle, cObjective);
}

void voltageInitializerSetObjectiveDistance(const JavaHandle& paramsHandle, double dist) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetObjectiveDistance, paramsHandle, dist);
}

void voltageInitializerSetLogLevelAmpl(const JavaHandle& paramsHandle, VoltageInitializerLogLevelAmpl logLevelAmpl) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetLogLevelAmpl, paramsHandle, logLevelAmpl);
}

void voltageInitializerSetLogLevelSolver(const JavaHandle& paramsHandle, VoltageInitializerLogLevelSolver logLevelSolver) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetLogLevelSolver, paramsHandle, logLevelSolver);
}

void voltageInitializerSetReactiveSlackBusesMode(const JavaHandle& paramsHandle, VoltageInitializerReactiveSlackBusesMode reactiveSlackBusesMode) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetReactiveSlackBusesMode, paramsHandle, reactiveSlackBusesMode);
}

void voltageInitializerSetMinPlausibleLowVoltageLimit(const JavaHandle& paramsHandle, double min_plausible_low_voltage_limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetMinPlausibleLowVoltageLimit, paramsHandle, min_plausible_low_voltage_limit);
}

void voltageInitializerSetMaxPlausibleHighVoltageLimit(const JavaHandle& paramsHandle, double max_plausible_high_voltage_limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetMaxPlausibleHighVoltageLimit, paramsHandle, max_plausible_high_voltage_limit);
}

void voltageInitializerSetActivePowerVariationRate(const JavaHandle& paramsHandle, double active_power_variation_rate) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetActivePowerVariationRate, paramsHandle, active_power_variation_rate);
}

void voltageInitializerSetMinPlausibleActivePowerThreshold(const JavaHandle& paramsHandle, double min_plausible_active_power_threshold) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetMinPlausibleActivePowerThreshold, paramsHandle, min_plausible_active_power_threshold);
}

void voltageInitializerSetLowImpedanceThreshold(const JavaHandle& paramsHandle, double low_impedance_threshold) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetLowImpedanceThreshold, paramsHandle, low_impedance_threshold);
}

void voltageInitializerSetMinNominalVoltageIgnoredBus(const JavaHandle& paramsHandle, double min_nominal_voltage_ignored_bus) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetMinNominalVoltageIgnoredBus, paramsHandle, min_nominal_voltage_ignored_bus);
}

void voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds(const JavaHandle& paramsHandle, double min_nominal_voltage_ignored_voltage_bounds) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds, paramsHandle, min_nominal_voltage_ignored_voltage_bounds);
}

void voltageInitializerSetMaxPlausiblePowerLimit(const JavaHandle& paramsHandle, double max_plausible_power_limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetMaxPlausiblePowerLimit, paramsHandle, max_plausible_power_limit);
}

void voltageInitializerSetDefaultMinimalQPRange(const JavaHandle& paramsHandle, double default_minimal_qp_range) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetDefaultMinimalQPRange, paramsHandle, default_minimal_qp_range);
}

void voltageInitializerSetHighActivePowerDefaultLimit(const JavaHandle& paramsHandle, double high_active_power_default_limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetHighActivePowerDefaultLimit, paramsHandle, high_active_power_default_limit);
}

void voltageInitializerSetLowActivePowerDefaultLimit(const JavaHandle& paramsHandle, double low_active_power_default_limit) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetLowActivePowerDefaultLimit, paramsHandle, low_active_power_default_limit);
}

void voltageInitializerSetDefaultQmaxPmaxRatio(const JavaHandle& paramsHandle, double default_qmax_pmax_ratio) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetDefaultQmaxPmaxRatio, paramsHandle, default_qmax_pmax_ratio);
}

void voltageInitializerSetDefaultVariableScalingFactor(const JavaHandle& paramsHandle, double defaultVariableScalingFactor) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetDefaultVariableScalingFactor, paramsHandle, defaultVariableScalingFactor);
}

void voltageInitializerSetDefaultConstraintScalingFactor(const JavaHandle& paramsHandle, double defaultConstraintScalingFactor) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetDefaultConstraintScalingFactor, paramsHandle, defaultConstraintScalingFactor);
}

void voltageInitializerSetReactiveSlackVariableScalingFactor(const JavaHandle& paramsHandle, double reactiveSlackVariableScalingFactor) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetReactiveSlackVariableScalingFactor, paramsHandle, reactiveSlackVariableScalingFactor);
}

void voltageInitializerSetTwoWindingTransformerRatioVariableScalingFactor(const JavaHandle& paramsHandle, double twoWindingTransformerRatioVariableScalingFactor) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerSetTwoWindingTransformerRatioVariableScalingFactor, paramsHandle, twoWindingTransformerRatioVariableScalingFactor);

}

void voltageInitializerApplyAllModifications(const JavaHandle& resultHandle, const JavaHandle& networkHandle) {
    pypowsybl::PowsyblCaller::get()->callJava(::voltageInitializerApplyAllModifications, resultHandle, networkHandle);
}

VoltageInitializerStatus voltageInitializerGetStatus(const JavaHandle& resultHandle) {
    return pypowsybl::PowsyblCaller::get()->callJava<VoltageInitializerStatus>(::voltageInitializerGetStatus, resultHandle);
}

std::map<std::string, std::string> voltageInitializerGetIndicators(const JavaHandle& resultHandle) {
    string_map* indicators = pypowsybl::PowsyblCaller::get()->callJava<string_map*>(::voltageInitializerGetIndicators, resultHandle);
    return convertMapStructToStdMap(indicators);
}

JavaHandle runVoltageInitializer(bool debug, const JavaHandle& networkHandle, const JavaHandle& paramsHandle) {
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::runVoltageInitializer, debug, networkHandle, paramsHandle);
}

JavaHandle createRao() {
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::createRao);
}

RaoComputationStatus getRaoResultStatus(const JavaHandle& raoResult) {
    return pypowsybl::PowsyblCaller::get()->callJava<RaoComputationStatus>(::getRaoResultStatus, raoResult);
}

SeriesArray* getFlowCnecResults(const JavaHandle& cracHandle, const JavaHandle& resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getFlowCnecResults, cracHandle, resultHandle));
}

SeriesArray* getAngleCnecResults(const JavaHandle& cracHandle, const JavaHandle& resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getAngleCnecResults, cracHandle, resultHandle));
}

SeriesArray* getVoltageCnecResults(const JavaHandle& cracHandle, const JavaHandle& resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getVoltageCnecResults, cracHandle, resultHandle));
}

SeriesArray* getRaResults(const JavaHandle& cracHandle, const JavaHandle& resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getRaResults, cracHandle, resultHandle));
}

SeriesArray* getCostResults(const JavaHandle& cracHandle, const JavaHandle& resultHandle) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getCostResults, cracHandle, resultHandle));
}

std::vector<std::string> getVirtualCostNames(const JavaHandle& resultHandle) {
    auto virtulCostArrayPtr = pypowsybl::PowsyblCaller::get()->callJava<array*>(::getVirtualCostNames, resultHandle);
    ToStringVector virtalCosts(virtulCostArrayPtr);
    return virtalCosts.get();
}

SeriesArray* getVirtualCostsResults(const JavaHandle& cracHandle, const JavaHandle& resultHandle, const std::string& virtualCostName) {
    return new SeriesArray(PowsyblCaller::get()->callJava<array*>(::getVirtualCostResults, cracHandle, resultHandle, (char*) virtualCostName.c_str()));
}

JavaHandle getCrac(const JavaHandle& raoContext) {
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::getCrac, raoContext);
}

JavaHandle runRaoWithParameters(const JavaHandle& networkHandle, const JavaHandle& raoHandle, const RaoParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::runRao, networkHandle, raoHandle, c_parameters.get());
}

JavaHandle runVoltageMonitoring(const JavaHandle& networkHandle, const JavaHandle& resultHandle, const JavaHandle& contextHandle, const LoadFlowParameters& parameters, const std::string& provider) {
    auto c_loadflow_parameters = parameters.to_c_struct();
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::runVoltageMonitoring, networkHandle, resultHandle, contextHandle, c_loadflow_parameters.get(), (char *) provider.data());
}

JavaHandle runAngleMonitoring(const JavaHandle& networkHandle, const JavaHandle& resultHandle, const JavaHandle& contextHandle, const LoadFlowParameters& parameters, const std::string& provider) {
    auto c_loadflow_parameters = parameters.to_c_struct();
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::runAngleMonitoring, networkHandle, resultHandle, contextHandle, c_loadflow_parameters.get(), (char *) provider.data());
}


JavaHandle createDefaultRaoParameters() {
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::createDefaultRaoParameters);
}

JavaHandle createGrid2opBackend(const JavaHandle& networkHandle, bool considerOpenBranchReactiveFlow, bool checkIsolatedAndDisconnectedInjections, int busesPerVoltageLevel, bool connectAllElementsToFirstBus) {
    return pypowsybl::PowsyblCaller::get()->callJava<JavaHandle>(::createGrid2opBackend, networkHandle, considerOpenBranchReactiveFlow, checkIsolatedAndDisconnectedInjections, busesPerVoltageLevel, connectAllElementsToFirstBus);
}

void freeGrid2opBackend(const JavaHandle& backendHandle) {
    pypowsybl::PowsyblCaller::get()->callJava(::freeGrid2opBackend, backendHandle);
}

std::vector<std::string> getGrid2opStringValue(const JavaHandle& backendHandle, Grid2opStringValueType valueType) {
    auto stringValueArrayPtr = pypowsybl::PowsyblCaller::get()->callJava<array*>(::getGrid2opStringValue, backendHandle, valueType);
    return toVector<std::string>(stringValueArrayPtr); // do not release, will be done when freeing backend
}

array* getGrid2opIntegerValue(const JavaHandle& backendHandle, Grid2opIntegerValueType valueType) {
    return pypowsybl::PowsyblCaller::get()->callJava<array*>(::getGrid2opIntegerValue, backendHandle, valueType); // do not release, will be done when freeing backend
}

array* getGrid2opDoubleValue(const JavaHandle& backendHandle, Grid2opDoubleValueType valueType) {
    return pypowsybl::PowsyblCaller::get()->callJava<array*>(::getGrid2opDoubleValue, backendHandle, valueType); // do not release, will be done when freeing backend
}

void updateGrid2opDoubleValue(const JavaHandle& backendHandle, Grid2opUpdateDoubleValueType valueType, double* valuePtr, int* changedPtr) {
    pypowsybl::PowsyblCaller::get()->callJava(::updateGrid2opDoubleValue, backendHandle, valueType, valuePtr, changedPtr);
}

void updateGrid2opIntegerValue(const JavaHandle& backendHandle, Grid2opUpdateIntegerValueType valueType, int* valuePtr, int* changedPtr) {
    pypowsybl::PowsyblCaller::get()->callJava(::updateGrid2opIntegerValue, backendHandle, valueType, valuePtr, changedPtr);
}

bool checkGrid2opIsolatedAndDisconnectedInjections(const JavaHandle& backendHandle) {
    return pypowsybl::PowsyblCaller::get()->callJava<bool>(::checkGrid2opIsolatedAndDisconnectedInjections, backendHandle);
}

LoadFlowComponentResultArray* runGrid2opLoadFlow(const JavaHandle& network, bool dc, const LoadFlowParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    return new LoadFlowComponentResultArray(PowsyblCaller::get()->callJava<array*>(::runGrid2opLoadFlow, network, dc, c_parameters.get()));
}

}
