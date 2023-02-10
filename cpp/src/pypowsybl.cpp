/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "pypowsybl.h"
#include "pylogging.h"
#include "pypowsybl-java.h"
#include <iostream>

namespace pypowsybl {

graal_isolate_t* isolate = nullptr;

void init() {
    graal_isolatethread_t* thread = nullptr;

    if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
        throw std::runtime_error("graal_create_isolate error");
    }
}

class GraalVmGuard {
public:
    GraalVmGuard() {
        if (!isolate) {
            throw std::runtime_error("isolate has not been created");
        }
        //if thread already attached to the isolate,
        //we assume it's a nested call --> do nothing

        thread_ = graal_get_current_thread(isolate);
        if (thread_ == nullptr) {
            if (graal_attach_thread(isolate, &thread_) != 0) {
                throw std::runtime_error("graal_create_isolate error");
            }
            shouldDetach = true;
       }
    }

    ~GraalVmGuard() noexcept(false) {
        if (shouldDetach && graal_detach_thread(thread_) != 0) {
            throw std::runtime_error("graal_detach_thread error");
        }
    }

    graal_isolatethread_t * thread() const {
        return thread_;
    }

private:
    bool shouldDetach = false;
    graal_isolatethread_t* thread_ = nullptr;
};

//copies to string and frees memory allocated by java
std::string toString(char* cstring);

//Explicitly update log level on java side
void setLogLevelFromPythonLogger(GraalVmGuard* guard, exception_handler* exc) {
    py::object logger = CppToPythonLogger::get()->getLogger();
    if (!logger.is_none()) {
        py::gil_scoped_acquire acquire;
        py::object level = logger.attr("level");
        ::setLogLevel(guard->thread(), level.cast<int>(), exc);
     }
}

template<typename F, typename... ARGS>
void callJava(F f, ARGS... args) {
    GraalVmGuard guard;
    exception_handler exc;

    setLogLevelFromPythonLogger(&guard, &exc);

    f(guard.thread(), args..., &exc);
    if (exc.message) {
        throw PyPowsyblError(toString(exc.message));
    }
    {
        py::gil_scoped_acquire acquire;
        if (PyErr_Occurred() != nullptr) {
            throw py::error_already_set();
        }
    }
}

template<typename T, typename F, typename... ARGS>
T callJava(F f, ARGS... args) {
    GraalVmGuard guard;
    exception_handler exc;

    setLogLevelFromPythonLogger(&guard, &exc);

    auto r = f(guard.thread(), args..., &exc);
    if (exc.message) {
        throw PyPowsyblError(toString(exc.message));
    }
    {
        py::gil_scoped_acquire acquire;
        if (PyErr_Occurred() != nullptr) {
            throw py::error_already_set();
        }
    }
    return r;
}

//Destruction of java object when the shared_ptr has no more references
JavaHandle::JavaHandle(void* handle):
    handle_(handle, [](void* to_be_deleted) {
        if (to_be_deleted) {
            callJava<>(::destroyObjectHandle, to_be_deleted);
        }
    })
{
}

template<>
Array<loadflow_component_result>::~Array() {
    callJava<>(::freeLoadFlowComponentResultPointer, delegate_);
}

template<>
Array<post_contingency_result>::~Array() {
    callJava<>(::freeContingencyResultArrayPointer, delegate_);
}

template<>
Array<limit_violation>::~Array() {
    // already freed by contingency_result
}

template<>
Array<series>::~Array() {
    callJava<>(::freeSeriesArray, delegate_);
}

template<typename T>
class ToPtr {
public:
    ~ToPtr() {
        delete[] ptr_;
    }

    T* get() const {
        return ptr_;
    }

protected:
    explicit ToPtr(size_t size)
            : ptr_(new T[size])
    {}

    T* ptr_;
};

class ToCharPtrPtr : public ToPtr<char*> {
public:
    explicit ToCharPtrPtr(const std::vector<std::string>& strings)
            : ToPtr<char*>(strings.size())
    {
        for (int i = 0; i < strings.size(); i++) {
            ptr_[i] = (char*) strings[i].data();
        }
    }
};

class ToIntPtr : public ToPtr<int> {
public:
    explicit ToIntPtr(const std::vector<int>& ints)
            : ToPtr<int>(ints.size())
    {
        for (int i = 0; i < ints.size(); i++) {
            ptr_[i] = ints[i];
        }
    }
};

class ToDoublePtr : public ToPtr<double> {
public:
    explicit ToDoublePtr(const std::vector<double>& doubles)
            : ToPtr<double>(doubles.size())
    {
        for (int i = 0; i < doubles.size(); i++) {
            ptr_[i] = doubles[i];
        }
    }
};

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
        callJava<>(::freeStringArray, arrayPtr_);
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
        callJava<>(::freeArray, arrayPtr_);
    }

    std::vector<T> get() {
        return toVector<T>(arrayPtr_);
    }

private:
    array* arrayPtr_;
};


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
    callJava<>(::freeString, str);
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
    no_generator_reactive_limits = (bool) src->no_generator_reactive_limits;
    phase_shifter_regulation_on = (bool) src->phase_shifter_regulation_on;
    twt_split_shunt_admittance = (bool) src->twt_split_shunt_admittance;
    simul_shunt = (bool) src->simul_shunt;
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
    res.no_generator_reactive_limits = (unsigned char) no_generator_reactive_limits;
    res.phase_shifter_regulation_on = (unsigned char) phase_shifter_regulation_on;
    res.twt_split_shunt_admittance = (unsigned char) twt_split_shunt_admittance;
    res.simul_shunt = (unsigned char) simul_shunt;
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

void setJavaLibraryPath(const std::string& javaLibraryPath) {
    callJava<>(::setJavaLibraryPath, (char*) javaLibraryPath.data());
}

void setConfigRead(bool configRead) {
    callJava<>(::setConfigRead, configRead);
}

void setDefaultLoadFlowProvider(const std::string& loadFlowProvider) {
    callJava<>(::setDefaultLoadFlowProvider, (char*) loadFlowProvider.data());
}

void setDefaultSecurityAnalysisProvider(const std::string& securityAnalysisProvider) {
    callJava<>(::setDefaultSecurityAnalysisProvider, (char*) securityAnalysisProvider.data());
}

void setDefaultSensitivityAnalysisProvider(const std::string& sensitivityAnalysisProvider) {
    callJava<>(::setDefaultSensitivityAnalysisProvider, (char*) sensitivityAnalysisProvider.data());
}

std::string getDefaultLoadFlowProvider() {
    return toString(callJava<char*>(::getDefaultLoadFlowProvider));
}

std::string getDefaultSecurityAnalysisProvider() {
    return toString(callJava<char*>(::getDefaultSecurityAnalysisProvider));
}

std::string getDefaultSensitivityAnalysisProvider() {
    return toString(callJava<char*>(::getDefaultSensitivityAnalysisProvider));
}

bool isConfigRead() {
    return callJava<bool>(::isConfigRead);
}

std::string getVersionTable() {
    return toString(callJava<char*>(::getVersionTable));
}

JavaHandle createNetwork(const std::string& name, const std::string& id) {
    return callJava<JavaHandle>(::createNetwork, (char*) name.data(), (char*) id.data());
}

void merge(JavaHandle network, std::vector<JavaHandle>& others) {
    std::vector<void*> othersPtrs;
    othersPtrs.reserve(others.size());
    for(int i = 0; i < others.size(); ++i) {
      void* ptr = others[i];
      othersPtrs.push_back(ptr);
    }
    int count = othersPtrs.size();
    void** networksData = (void**)othersPtrs.data();

    callJava<>(::merge, network, networksData, count);
}

std::vector<std::string> getNetworkImportFormats() {
    auto formatsArrayPtr = callJava<array*>(::getNetworkImportFormats);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getNetworkExportFormats() {
    auto formatsArrayPtr = callJava<array*>(::getNetworkExportFormats);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getLoadFlowProviderNames() {
    auto formatsArrayPtr = callJava<array*>(::getLoadFlowProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getSecurityAnalysisProviderNames() {
    auto formatsArrayPtr = callJava<array*>(::getSecurityAnalysisProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

std::vector<std::string> getSensitivityAnalysisProviderNames() {
    auto formatsArrayPtr = callJava<array*>(::getSensitivityAnalysisProviderNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

SeriesArray* createImporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray(callJava<array*>(::createImporterParametersSeriesArray, (char*) format.data()));
}

SeriesArray* createExporterParametersSeriesArray(const std::string& format) {
    return new SeriesArray(callJava<array*>(::createExporterParametersSeriesArray, (char*) format.data()));
}

std::shared_ptr<network_metadata> getNetworkMetadata(const JavaHandle& network) {
    network_metadata* attributes = callJava<network_metadata*>(::getNetworkMetadata, network);
    return std::shared_ptr<network_metadata>(attributes, [](network_metadata* ptr){
        callJava(::freeNetworkMetadata, ptr);
    });
}

JavaHandle loadNetwork(const std::string& file, const std::map<std::string, std::string>& parameters, JavaHandle* reporter) {
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
    return callJava<JavaHandle>(::loadNetwork, (char*) file.data(), parameterNamesPtr.get(), parameterNames.size(),
                              parameterValuesPtr.get(), parameterValues.size(), (reporter == nullptr) ? nullptr : *reporter);
}

JavaHandle loadNetworkFromString(const std::string& fileName, const std::string& fileContent, const std::map<std::string, std::string>& parameters, JavaHandle* reporter) {
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
    return callJava<JavaHandle>(::loadNetworkFromString, (char*) fileName.data(), (char*) fileContent.data(),
                           parameterNamesPtr.get(), parameterNames.size(),
                           parameterValuesPtr.get(), parameterValues.size(), (reporter == nullptr) ? nullptr : *reporter);
}

void dumpNetwork(const JavaHandle& network, const std::string& file, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reporter) {
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
    callJava(::dumpNetwork, network, (char*) file.data(), (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
                parameterValuesPtr.get(), parameterValues.size(), (reporter == nullptr) ? nullptr : *reporter);
}

std::string dumpNetworkToString(const JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, JavaHandle* reporter) {
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
    return toString(callJava<char*>(::dumpNetworkToString, network, (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
             parameterValuesPtr.get(), parameterValues.size(), (reporter == nullptr) ? nullptr : *reporter));
}

void reduceNetwork(const JavaHandle& network, double v_min, double v_max, const std::vector<std::string>& ids,
                   const std::vector<std::string>& vls, const std::vector<int>& depths, bool withDangLingLines) {
    ToCharPtrPtr elementIdPtr(ids);
    ToCharPtrPtr vlsPtr(vls);
    ToIntPtr depthsPtr(depths);
    callJava(::reduceNetwork, network, v_min, v_max, elementIdPtr.get(), ids.size(), vlsPtr.get(), vls.size(), depthsPtr.get(), depths.size(), withDangLingLines);
}

bool updateSwitchPosition(const JavaHandle& network, const std::string& id, bool open) {
    return callJava<bool>(::updateSwitchPosition, network, (char*) id.data(), open);
}

bool updateConnectableStatus(const JavaHandle& network, const std::string& id, bool connected) {
    return callJava<bool>(::updateConnectableStatus, network, (char*) id.data(), connected);
}

std::vector<std::string> getNetworkElementsIds(const JavaHandle& network, element_type elementType, const std::vector<double>& nominalVoltages,
                                               const std::vector<std::string>& countries, bool mainCc, bool mainSc,
                                               bool notConnectedToSameBusAtBothSides) {
    ToDoublePtr nominalVoltagePtr(nominalVoltages);
    ToCharPtrPtr countryPtr(countries);
    auto elementsIdsArrayPtr = callJava<array*>(::getNetworkElementsIds, network, elementType,
                                                       nominalVoltagePtr.get(), nominalVoltages.size(),
                                                       countryPtr.get(), countries.size(), mainCc, mainSc,
                                                       notConnectedToSameBusAtBothSides);
    ToStringVector elementsIds(elementsIdsArrayPtr);
    return elementsIds.get();
}

LoadFlowParameters* createLoadFlowParameters() {
    loadflow_parameters* parameters_ptr = callJava<loadflow_parameters*>(::createLoadFlowParameters);
    auto parameters = std::shared_ptr<loadflow_parameters>(parameters_ptr, [](loadflow_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       callJava(::freeLoadFlowParameters, ptr);
    });
    return new LoadFlowParameters(parameters.get());
}

LoadFlowValidationParameters* createValidationConfig() {
    loadflow_validation_parameters* parameters_ptr = callJava<loadflow_validation_parameters*>(::createValidationConfig);
    auto parameters = std::shared_ptr<loadflow_validation_parameters>(parameters_ptr, [](loadflow_validation_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       callJava(::freeValidationConfig, ptr);
    });
    return new LoadFlowValidationParameters(parameters.get());
}


SecurityAnalysisParameters* createSecurityAnalysisParameters() {
    security_analysis_parameters* parameters_ptr = callJava<security_analysis_parameters*>(::createSecurityAnalysisParameters);
    auto parameters = std::shared_ptr<security_analysis_parameters>(parameters_ptr, [](security_analysis_parameters* ptr){
        callJava(::freeSecurityAnalysisParameters, ptr);
    });
    return new SecurityAnalysisParameters(parameters.get());
}

SensitivityAnalysisParameters* createSensitivityAnalysisParameters() {
    sensitivity_analysis_parameters* parameters_ptr = callJava<sensitivity_analysis_parameters*>(::createSensitivityAnalysisParameters);
     auto parameters = std::shared_ptr<sensitivity_analysis_parameters>(parameters_ptr, [](sensitivity_analysis_parameters* ptr){
        callJava(::freeSensitivityAnalysisParameters, ptr);
    });
    return new SensitivityAnalysisParameters(parameters.get());
}

LoadFlowComponentResultArray* runLoadFlow(const JavaHandle& network, bool dc, const LoadFlowParameters& parameters,
                                          const std::string& provider, JavaHandle* reporter) {
    auto c_parameters = parameters.to_c_struct();
    return new LoadFlowComponentResultArray(
            callJava<array*>(::runLoadFlow, network, dc, c_parameters.get(), (char *) provider.data(), (reporter == nullptr) ? nullptr : *reporter));
}

SeriesArray* runLoadFlowValidation(const JavaHandle& network, validation_type validationType, const LoadFlowValidationParameters& loadflow_validation_parameters) {
    auto c_validation_parameters = loadflow_validation_parameters.to_c_struct();
    return new SeriesArray(callJava<array*>(::runLoadFlowValidation, network, validationType, c_validation_parameters.get()));
}

void writeSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId, const std::string& svgFile, const std::string& metadataFile, const LayoutParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    callJava(::writeSingleLineDiagramSvg, network, (char*) containerId.data(), (char*) svgFile.data(), (char*) metadataFile.data(), c_parameters.get());
}

std::string getSingleLineDiagramSvg(const JavaHandle& network, const std::string& containerId) {
    return toString(callJava<char*>(::getSingleLineDiagramSvg, network, (char*) containerId.data()));
}

std::vector<std::string> getSingleLineDiagramSvgAndMetadata(const JavaHandle& network, const std::string& containerId, const LayoutParameters& parameters) {
    auto c_parameters = parameters.to_c_struct();
    auto svgAndMetadataArrayPtr = callJava<array*>(::getSingleLineDiagramSvgAndMetadata, network, (char*) containerId.data(), c_parameters.get());
    ToStringVector svgAndMetadata(svgAndMetadataArrayPtr);
    return svgAndMetadata.get();
}

void writeNetworkAreaDiagramSvg(const JavaHandle& network, const std::string& svgFile, const std::vector<std::string>& voltageLevelIds, int depth) {
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    callJava(::writeNetworkAreaDiagramSvg, network, (char*) svgFile.data(), voltageLevelIdPtr.get(), voltageLevelIds.size(), depth);
}

std::string getNetworkAreaDiagramSvg(const JavaHandle& network, const std::vector<std::string>&  voltageLevelIds, int depth) {
    ToCharPtrPtr voltageLevelIdPtr(voltageLevelIds);
    return toString(callJava<char*>(::getNetworkAreaDiagramSvg, network, voltageLevelIdPtr.get(), voltageLevelIds.size(), depth));
}

JavaHandle createSecurityAnalysis() {
    return callJava<JavaHandle>(::createSecurityAnalysis);
}

void addContingency(const JavaHandle& analysisContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    callJava(::addContingency, analysisContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

JavaHandle runSecurityAnalysis(const JavaHandle& securityAnalysisContext, const JavaHandle& network, const SecurityAnalysisParameters& parameters,
                               const std::string& provider, bool dc, JavaHandle* reporter) {
    auto c_parameters = parameters.to_c_struct();
    return callJava<JavaHandle>(::runSecurityAnalysis, securityAnalysisContext, network, c_parameters.get(), (char *) provider.data(), dc, (reporter == nullptr) ? nullptr : *reporter);
}

JavaHandle createSensitivityAnalysis() {
    return callJava<JavaHandle>(::createSensitivityAnalysis);
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
    callJava(::setZones, sensitivityAnalysisContext, zonesPtr.get(), zones.size());
}

void addBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                               const std::vector<std::string>& variablesIds) {
       ToCharPtrPtr branchIdPtr(branchesIds);
       ToCharPtrPtr variableIdPtr(variablesIds);
       callJava(::addBranchFlowFactorMatrix, sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                 variableIdPtr.get(), variablesIds.size(), (char*) matrixId.c_str());
}

void addPreContingencyBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                               const std::vector<std::string>& variablesIds) {
       ToCharPtrPtr branchIdPtr(branchesIds);
       ToCharPtrPtr variableIdPtr(variablesIds);
       callJava(::addPreContingencyBranchFlowFactorMatrix, sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                  variableIdPtr.get(), variablesIds.size(), (char*) matrixId.c_str());
}

void addPostContingencyBranchFlowFactorMatrix(const JavaHandle& sensitivityAnalysisContext, std::string matrixId, const std::vector<std::string>& branchesIds,
                               const std::vector<std::string>& variablesIds, const std::vector<std::string>& contingenciesIds) {
       ToCharPtrPtr branchIdPtr(branchesIds);
       ToCharPtrPtr variableIdPtr(variablesIds);
       ToCharPtrPtr contingenciesIdPtr(contingenciesIds);
       callJava(::addPostContingencyBranchFlowFactorMatrix, sensitivityAnalysisContext, branchIdPtr.get(), branchesIds.size(),
                  variableIdPtr.get(), variablesIds.size(), contingenciesIdPtr.get(), contingenciesIds.size(), (char*) matrixId.c_str());
}

void setBusVoltageFactorMatrix(const JavaHandle& sensitivityAnalysisContext, const std::vector<std::string>& busIds,
                               const std::vector<std::string>& targetVoltageIds) {
    ToCharPtrPtr busVoltageIdPtr(busIds);
    ToCharPtrPtr targetVoltageIdPtr(targetVoltageIds);
    callJava(::setBusVoltageFactorMatrix, sensitivityAnalysisContext, busVoltageIdPtr.get(),
                busIds.size(), targetVoltageIdPtr.get(), targetVoltageIds.size());
}

JavaHandle runSensitivityAnalysis(const JavaHandle& sensitivityAnalysisContext, const JavaHandle& network, bool dc, SensitivityAnalysisParameters& parameters, const std::string& provider, JavaHandle* reporter) {
    auto c_parameters = parameters.to_c_struct();
    return callJava<JavaHandle>(::runSensitivityAnalysis, sensitivityAnalysisContext, network, dc, c_parameters.get(), (char *) provider.data(), (reporter == nullptr) ? nullptr : *reporter);
}

matrix* getBranchFlowsSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId) {
    return callJava<matrix*>(::getBranchFlowsSensitivityMatrix, sensitivityAnalysisResultContext,
                                (char*) matrixId.c_str(), (char*) contingencyId.c_str());
}

matrix* getBusVoltagesSensitivityMatrix(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId) {
    return callJava<matrix*>(::getBusVoltagesSensitivityMatrix, sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

matrix* getReferenceFlows(const JavaHandle& sensitivityAnalysisResultContext, const std::string& matrixId, const std::string& contingencyId) {
    return callJava<matrix*>(::getReferenceFlows, sensitivityAnalysisResultContext,
                                (char*) matrixId.c_str(), (char*) contingencyId.c_str());
}

matrix* getReferenceVoltages(const JavaHandle& sensitivityAnalysisResultContext, const std::string& contingencyId) {
    return callJava<matrix*>(::getReferenceVoltages, sensitivityAnalysisResultContext,
                                (char*) contingencyId.c_str());
}

SeriesArray* createNetworkElementsSeriesArray(const JavaHandle& network, element_type elementType, filter_attributes_type filterAttributesType, const std::vector<std::string>& attributes, dataframe* dataframe) {
	ToCharPtrPtr attributesPtr(attributes);
    return new SeriesArray(callJava<array*>(::createNetworkElementsSeriesArray, network, elementType, filterAttributesType, attributesPtr.get(), attributes.size(), dataframe));
}

SeriesArray* createNetworkElementsExtensionSeriesArray(const JavaHandle& network, const std::string& extensionName, const std::string& tableName) {
    return new SeriesArray(callJava<array*>(::createNetworkElementsExtensionSeriesArray, network, (char*) extensionName.c_str(), (char*) tableName.c_str()));
}

std::vector<std::string> getExtensionsNames() {
    auto formatsArrayPtr = callJava<array*>(::getExtensionsNames);
    ToStringVector formats(formatsArrayPtr);
    return formats.get();
}

SeriesArray* getExtensionsInformation() {
    return new SeriesArray(callJava<array*>(::getExtensionsInformation));
}

std::string getWorkingVariantId(const JavaHandle& network) {
    return toString(callJava<char*>(::getWorkingVariantId, network));
}

void setWorkingVariant(const JavaHandle& network, std::string& variant) {
    callJava<>(::setWorkingVariant, network, (char*) variant.c_str());
}

void removeVariant(const JavaHandle& network, std::string& variant) {
    callJava<>(::removeVariant, network, (char*) variant.c_str());
}

void cloneVariant(const JavaHandle& network, std::string& src, std::string& variant, bool mayOverwrite) {
    callJava<>(::cloneVariant, network, (char*) src.c_str(), (char*) variant.c_str(), mayOverwrite);
}

std::vector<std::string> getVariantsIds(const JavaHandle& network) {
    auto formatsArrayPtr = callJava<array*>(::getVariantsIds, network);
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
    callJava<>(::addMonitoredElements, securityAnalysisContext, contingencyContextType, branchIdsPtr.get(), branchIds.size(),
    voltageLevelIdsPtr.get(), voltageLevelIds.size(), threeWindingsTransformerIdsPtr.get(),
    threeWindingsTransformerIds.size(), contingencyIdsPtr.get(), contingencyIds.size());
}

PostContingencyResultArray* getPostContingencyResults(const JavaHandle& securityAnalysisResult) {
    return new PostContingencyResultArray(callJava<array*>(::getPostContingencyResults, securityAnalysisResult));
}

pre_contingency_result* getPreContingencyResult(const JavaHandle& securityAnalysisResult) {
    return callJava<pre_contingency_result*>(::getPreContingencyResult, securityAnalysisResult);
}

SeriesArray* getLimitViolations(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getLimitViolations, securityAnalysisResult));
}

SeriesArray* getBranchResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getBranchResults, securityAnalysisResult));
}

SeriesArray* getBusResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getBusResults, securityAnalysisResult));
}

SeriesArray* getThreeWindingsTransformerResults(const JavaHandle& securityAnalysisResult) {
    return new SeriesArray(callJava<array*>(::getThreeWindingsTransformerResults, securityAnalysisResult));
}

SeriesArray* getNodeBreakerViewSwitches(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(callJava<array*>(::getNodeBreakerViewSwitches, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getNodeBreakerViewNodes(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(callJava<array*>(::getNodeBreakerViewNodes, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getNodeBreakerViewInternalConnections(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(callJava<array*>(::getNodeBreakerViewInternalConnections, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getBusBreakerViewSwitches(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(callJava<array*>(::getBusBreakerViewSwitches, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getBusBreakerViewBuses(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(callJava<array*>(::getBusBreakerViewBuses, network, (char*) voltageLevel.c_str()));
}

SeriesArray* getBusBreakerViewElements(const JavaHandle& network, std::string& voltageLevel) {
    return new SeriesArray(callJava<array*>(::getBusBreakerViewElements, network, (char*) voltageLevel.c_str()));
}

void updateNetworkElementsWithSeries(pypowsybl::JavaHandle network, dataframe* dataframe, element_type elementType) {
    pypowsybl::callJava<>(::updateNetworkElementsWithSeries, network, elementType, dataframe);
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
    dataframe_metadata* metadata = pypowsybl::callJava<dataframe_metadata*>(::getSeriesMetadata, elementType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<std::vector<SeriesMetadata>> getNetworkElementCreationDataframesMetadata(element_type elementType) {

    dataframes_metadata* allDataframesMetadata = pypowsybl::callJava<dataframes_metadata*>(::getCreationMetadata, elementType);
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < allDataframesMetadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(allDataframesMetadata->dataframes_metadata + i));
    }
    pypowsybl::callJava(::freeDataframesMetadata, allDataframesMetadata);
    return res;
}

void createElement(pypowsybl::JavaHandle network, dataframe_array* dataframes, element_type elementType) {
    pypowsybl::callJava<>(::createElement, network, elementType, dataframes);
}

::validation_level_type getValidationLevel(const JavaHandle& network) {
    // TBD
    //return validation_level_type::EQUIPMENT;
    return callJava<validation_level_type>(::getValidationLevel, network);
}

::validation_level_type validate(const JavaHandle& network) {
    // TBD
    //return validation_level_type::STEADY_STATE_HYPOTHESIS;
    return callJava<validation_level_type>(::validate, network);
}

void setMinValidationLevel(pypowsybl::JavaHandle network, validation_level_type validationLevel) {
    pypowsybl::callJava<>(::setMinValidationLevel, network, validationLevel);
}

void setupLoggerCallback(void *& callback) {
    pypowsybl::callJava<>(::setupLoggerCallback, callback);
}

void removeNetworkElements(const JavaHandle& network, const std::vector<std::string>& elementIds) {
    ToCharPtrPtr elementIdsPtr(elementIds);
    pypowsybl::callJava<>(::removeNetworkElements, network, elementIdsPtr.get(), elementIds.size());
}

void addNetworkElementProperties(pypowsybl::JavaHandle network, dataframe* dataframe) {
    pypowsybl::callJava<>(::addNetworkElementProperties, network, dataframe);
}

void removeNetworkElementProperties(pypowsybl::JavaHandle network, const std::vector<std::string>& ids, const std::vector<std::string>& properties) {
    ToCharPtrPtr idsPtr(ids);
    ToCharPtrPtr propertiesPtr(properties);
    pypowsybl::callJava<>(::removeNetworkElementProperties, network, idsPtr.get(), ids.size(), propertiesPtr.get(), properties.size());
}

std::vector<std::string> getLoadFlowProviderParametersNames(const std::string& loadFlowProvider) {
    auto providerParametersArrayPtr = pypowsybl::callJava<array*>(::getLoadFlowProviderParametersNames, (char*) loadFlowProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

std::vector<std::string> getSecurityAnalysisProviderParametersNames(const std::string& securityAnalysisProvider) {
    auto providerParametersArrayPtr = pypowsybl::callJava<array*>(::getSecurityAnalysisProviderParametersNames, (char*) securityAnalysisProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

std::vector<std::string> getSensitivityAnalysisProviderParametersNames(const std::string& sensitivityAnalysisProvider) {
    auto providerParametersArrayPtr = pypowsybl::callJava<array*>(::getSensitivityAnalysisProviderParametersNames, (char*) sensitivityAnalysisProvider.c_str());
    ToStringVector providerParameters(providerParametersArrayPtr);
    return providerParameters.get();
}

void updateNetworkElementsExtensionsWithSeries(pypowsybl::JavaHandle network, std::string& name, std::string& tableName, dataframe* dataframe) {
    pypowsybl::callJava<>(::updateNetworkElementsExtensionsWithSeries, network, (char*) name.data(), (char*) tableName.data(), dataframe);
}

void removeExtensions(const JavaHandle& network, std::string& name, const std::vector<std::string>& ids) {
    ToCharPtrPtr idsPtr(ids);
    pypowsybl::callJava<>(::removeExtensions, network, (char*) name.data(), idsPtr.get(), ids.size());
}

std::vector<SeriesMetadata> getNetworkExtensionsDataframeMetadata(std::string& name, std::string& tableName) {
    dataframe_metadata* metadata = pypowsybl::callJava<dataframe_metadata*>(::getExtensionSeriesMetadata, (char*) name.data(), (char*) tableName.data());
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<std::vector<SeriesMetadata>> getNetworkExtensionsCreationDataframesMetadata(std::string& name) {
    dataframes_metadata* allDataframesMetadata = pypowsybl::callJava<dataframes_metadata*>(::getExtensionsCreationMetadata, (char*) name.data());
    std::vector<std::vector<SeriesMetadata>> res;
    for (int i =0; i < allDataframesMetadata->dataframes_count; i++) {
        res.push_back(convertDataframeMetadata(allDataframesMetadata->dataframes_metadata + i));
    }
    pypowsybl::callJava(::freeDataframesMetadata, allDataframesMetadata);
    return res;
}

void createExtensions(pypowsybl::JavaHandle network, dataframe_array* dataframes, std::string& name) {
        pypowsybl::callJava<>(::createExtensions, network, (char*) name.data(), dataframes);

}

JavaHandle createGLSKdocument(std::string& filename) {
    return callJava<JavaHandle>(::createGLSKdocument, (char*) filename.c_str());
}

std::vector<std::string> getGLSKinjectionkeys(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant) {
    auto keysArrayPtr = callJava<array*>(::getGLSKinjectionkeys, network, importer, (char*) country.c_str(), instant);
    ToStringVector keys(keysArrayPtr);
    return keys.get();
}

std::vector<std::string> getGLSKcountries(const JavaHandle& importer) {
    auto countriesArrayPtr = callJava<array*>(::getGLSKcountries, importer);
    ToStringVector countries(countriesArrayPtr);
    return countries.get();
}

std::vector<double> getGLSKInjectionFactors(pypowsybl::JavaHandle network, const JavaHandle& importer, std::string& country, long instant) {
    auto countriesArrayPtr = callJava<array*>(::getInjectionFactor, network, importer, (char*) country.c_str(), instant);
    ToPrimitiveVector<double> values(countriesArrayPtr);
    return values.get();
}

long getInjectionFactorStartTimestamp(const JavaHandle& importer) {
    return callJava<long>(::getInjectionFactorStartTimestamp, importer);
}

long getInjectionFactorEndTimestamp(const JavaHandle& importer) {
    return callJava<long>(::getInjectionFactorEndTimestamp, importer);
}

JavaHandle createReporterModel(const std::string& taskKey, const std::string& defaultName) {
    return callJava<JavaHandle>(::createReporterModel, (char*) taskKey.data(), (char*) defaultName.data());
}

std::string printReport(const JavaHandle& reporterModel) {
    return toString(callJava<char*>(::printReport, reporterModel));
}

std::string jsonReport(const JavaHandle& reporterModel) {
    return toString(callJava<char*>(::jsonReport, reporterModel));
}

JavaHandle createFlowDecomposition() {
    return callJava<JavaHandle>(::createFlowDecomposition);
}

void addContingencyForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::string& contingencyId, const std::vector<std::string>& elementsIds) {
    ToCharPtrPtr elementIdPtr(elementsIds);
    callJava(::addContingencyForFlowDecomposition, flowDecompositionContext, (char*) contingencyId.data(), elementIdPtr.get(), elementsIds.size());
}

void addPrecontingencyMonitoredElementsForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::vector<std::string>& branchIds) {
    ToCharPtrPtr branchIdPtr(branchIds);
    callJava(::addPrecontingencyMonitoredElementsForFlowDecomposition, flowDecompositionContext, branchIdPtr.get(), branchIds.size());
}

void addPostcontingencyMonitoredElementsForFlowDecomposition(const JavaHandle& flowDecompositionContext, const std::vector<std::string>& branchIds, const std::vector<std::string>& contingencyIds) {
    ToCharPtrPtr branchIdPtr(branchIds);
    ToCharPtrPtr contingencyIdPtr(contingencyIds);
    callJava(::addPostcontingencyMonitoredElementsForFlowDecomposition, flowDecompositionContext, branchIdPtr.get(), branchIds.size(), contingencyIdPtr.get(), contingencyIds.size());
}

void addAdditionalXnecProviderForFlowDecomposition(const JavaHandle& flowDecompositionContext, DefaultXnecProvider defaultXnecProvider) {
    callJava(::addAdditionalXnecProviderForFlowDecomposition, flowDecompositionContext, defaultXnecProvider);
}

SeriesArray* runFlowDecomposition(const JavaHandle& flowDecompositionContext, const JavaHandle& network, const FlowDecompositionParameters& flow_decomposition_parameters, const LoadFlowParameters& loadflow_parameters) {
    auto c_flow_decomposition_parameters = flow_decomposition_parameters.to_c_struct();
    auto c_loadflow_parameters  = loadflow_parameters.to_c_struct();
    return new SeriesArray(callJava<array*>(::runFlowDecomposition, flowDecompositionContext, network, c_flow_decomposition_parameters.get(), c_loadflow_parameters.get()));
}

FlowDecompositionParameters* createFlowDecompositionParameters() {
    flow_decomposition_parameters* parameters_ptr = callJava<flow_decomposition_parameters*>(::createFlowDecompositionParameters);
    auto parameters = std::shared_ptr<flow_decomposition_parameters>(parameters_ptr, [](flow_decomposition_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       callJava(::freeFlowDecompositionParameters, ptr);
    });
    return new FlowDecompositionParameters(parameters.get());
}

void createLineOnLine(pypowsybl::JavaHandle network, std::string bbsIdBusId,
        std::string newLineId, float newLineR, float newLineX, float newLineB1, float newLineB2, float newLineG1, float newLineG2,
        std::string lineId, std::string line1Id, std::string line1Name, std::string line2Id, std::string line2Name, float positionPercent, bool createFictitiousSubstation,
        std::string fictitiousVoltageLevelId, std::string fictitiousVoltageLevelName, std::string fictitiousSubstationId, std::string fictitiousSubstationName) {
    pypowsybl::callJava(::createLineOnLine, network, (char*) bbsIdBusId.c_str(),
                        (char*) newLineId.c_str(), newLineR, newLineX, newLineB1, newLineB2, newLineG1, newLineG2,
                        (char*) lineId.c_str(), (char*) line1Id.c_str(), (char*) line1Name.c_str(), (char*) line2Id.c_str(), (char*) line2Name.c_str(),
                        positionPercent, createFictitiousSubstation, (char*) fictitiousVoltageLevelId.c_str(), (char*) fictitiousVoltageLevelName.c_str(),
                        (char*) fictitiousSubstationId.c_str(), (char*) fictitiousSubstationName.c_str());
}

void revertCreateLineOnLine(pypowsybl::JavaHandle network, std::string lineToBeMerged1Id, std::string lineToBeMerged2Id, std::string lineToBeDeletedId,
    std::string mergedLineId, std::string mergedLineName) {
    pypowsybl::callJava(::revertCreateLineOnLine, network, (char*) lineToBeMerged1Id.c_str(), (char*) lineToBeMerged2Id.c_str(),
                        (char*) lineToBeDeletedId.c_str(), (char*) mergedLineId.c_str(), (char*) mergedLineName.c_str());
}

void connectVoltageLevelOnLine(pypowsybl::JavaHandle network, std::string bbsIdBusId, std::string lineId,
        std::string line1Id, std::string line1Name, std::string line2Id, std::string line2Name, float positionPercent) {
    pypowsybl::callJava(::connectVoltageLevelOnLine, network, (char*) bbsIdBusId.c_str(), (char*) lineId.c_str(),
                        (char*) line1Id.c_str(), (char*) line1Name.c_str(), (char*) line2Id.c_str(), (char*) line2Name.c_str(), positionPercent);
}

void revertConnectVoltageLevelOnLine(pypowsybl::JavaHandle network, std::string line1Id, std::string line2Id, std::string lineId,
    std::string lineName) {
    pypowsybl::callJava(::revertConnectVoltageLevelOnLine, network, (char*) line1Id.c_str(), (char*) line2Id.c_str(),
                        (char*) lineId.c_str(), (char*) lineName.c_str());
}

void createFeederBay(pypowsybl::JavaHandle network, bool throwException, JavaHandle* reporter, dataframe_array* dataframes, element_type elementType) {
    pypowsybl::callJava(::createFeederBay, network, throwException, (reporter == nullptr) ? nullptr : *reporter, dataframes, elementType);
}

void createBranchFeederBaysLine(pypowsybl::JavaHandle network, dataframe* dataframe) {
  pypowsybl::callJava(::createBranchFeederBaysLine, network, dataframe);
}

void createBranchFeederBaysTwt(pypowsybl::JavaHandle network, dataframe* dataframe) {
  pypowsybl::callJava(::createBranchFeederBaysTwt, network, dataframe);
}

std::vector<SeriesMetadata> getLineFeederBaysMetadata() {
    dataframe_metadata* metadata = pypowsybl::callJava<dataframe_metadata*>(::getLineFeederBaysMetadata);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    callJava(::freeDataframeMetadata, metadata);
    return res;
}

std::vector<SeriesMetadata> getTwtFeederBaysMetadata() {
    dataframe_metadata* metadata = pypowsybl::callJava<dataframe_metadata*>(::getTwtFeederBaysMetadata);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    callJava(::freeDataframeMetadata, metadata);
    return res;
}

SeriesArray* getConnectablesOrderPositions(const JavaHandle& network, const std::string voltage_level_id) {
    return new SeriesArray(callJava<array*>(::getConnectablesOrderPositions, network, (char*) voltage_level_id.c_str()));
}

std::vector<int> getUnusedConnectableOrderPositions(const pypowsybl::JavaHandle network, const std::string busbarSectionId, const std::string beforeOrAfter) {
    auto positionsArrayPtr = callJava<array*>(::getUnusedConnectableOrderPositions, network, (char*) busbarSectionId.c_str(), (char*) beforeOrAfter.c_str());
    ToPrimitiveVector<int> res(positionsArrayPtr);
    return res.get();
}

void removeAliases(pypowsybl::JavaHandle network, dataframe* dataframe) {
    pypowsybl::callJava(::removeAliases, network, dataframe);
}

void closePypowsybl() {
    pypowsybl::callJava(::closePypowsybl);
}

LayoutParameters::LayoutParameters(layout_parameters* src) {
    use_name = (bool) src->use_name;
    center_name = (bool) src->center_name;
    diagonal_label = (bool) src->diagonal_label;
    topological_coloring = (bool) src->topological_coloring;
}

void LayoutParameters::layout_to_c_struct(layout_parameters& res) const {
    res.use_name = (unsigned char) use_name;
    res.center_name = (unsigned char) center_name;
    res.diagonal_label = (unsigned char) diagonal_label;
    res.topological_coloring = (unsigned char) topological_coloring;
}

std::shared_ptr<layout_parameters> LayoutParameters::to_c_struct() const {
    layout_parameters* res = new layout_parameters();
    layout_to_c_struct(*res);
    //Memory has been allocated here on C side, we need to clean it up on C side (not java side)
    return std::shared_ptr<layout_parameters>(res, [](layout_parameters* ptr){
        delete ptr;
    });
}

LayoutParameters* createLayoutParameters() {
    layout_parameters* parameters_ptr = callJava<layout_parameters*>(::createLayoutParameters);
    auto parameters = std::shared_ptr<layout_parameters>(parameters_ptr, [](layout_parameters* ptr){
       //Memory has been allocated on java side, we need to clean it up on java side
       callJava(::freeLayoutParameters, ptr);
    });
    return new LayoutParameters(parameters.get());
}

void replaceTeePointByVoltageLevelOnLine(pypowsybl::JavaHandle network, std::string teePointLine1, std::string teePointLine2, std::string teePointLineToRemove,
        std::string bbsOrBusId, std::string newLine1Id, std::string newLine1Name, std::string newLine2Id, std::string newLine2Name) {
    pypowsybl::callJava(::replaceTeePointByVoltageLevelOnLine, network, (char*)teePointLine1.c_str(), (char*)teePointLine2.c_str(), (char*)teePointLineToRemove.c_str(),
     (char*)bbsOrBusId.c_str(), (char*)newLine1Id.c_str(), (char*)newLine1Name.c_str(), (char*)newLine2Id.c_str(),
     (char*)newLine2Name.c_str());
}

void removeFeederBays(pypowsybl::JavaHandle network, const std::vector<std::string>&  connectableIds) {
    ToCharPtrPtr connectableIdsPtr(connectableIds);
    pypowsybl::callJava(::removeFeederBays, network, connectableIdsPtr.get(), connectableIds.size());
}

std::vector<SeriesMetadata> getModificationMetadata(network_modification_type networkModificationType) {
    dataframe_metadata* metadata = pypowsybl::callJava<dataframe_metadata*>(::getModificationMetadata, networkModificationType);
    std::vector<SeriesMetadata> res = convertDataframeMetadata(metadata);
    callJava(::freeDataframeMetadata, metadata);
    return res;
}

void createNetworkModification(pypowsybl::JavaHandle network, dataframe* dataframe,  network_modification_type networkModificationType, bool throwException, JavaHandle* reporter) {
    pypowsybl::callJava(::createNetworkModification, network, dataframe, networkModificationType, throwException, (reporter == nullptr) ? nullptr : *reporter);
}

}
