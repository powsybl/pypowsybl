/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>

//Necessary for PyPowsyblError, declared in a seperated shared library, to be correctly registered in pybind11
//Otherwise PyPowsyblError are not catched properly on python side
//see https://github.com/pybind/pybind11/issues/1272
namespace pypowsybl {
class PYBIND11_EXPORT PyPowsyblError;
}
#include "powsybl-cpp.h"
#include "pylogging.h"

namespace py = pybind11;

//Explicitly update log level on java side
void setLogLevelFromPythonLogger(pypowsybl::GraalVmGuard* guard, exception_handler* exc);

pypowsybl::JavaHandle loadNetworkFromBinaryBuffersPython(std::vector<py::buffer> byteBuffers, const std::map<std::string, std::string>& parameters, const std::vector<std::string>& postProcessors, pypowsybl::JavaHandle* reportNode);

py::bytes saveNetworkToBinaryBufferPython(const pypowsybl::JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, pypowsybl::JavaHandle* reportNode);

template<typename T>
void bindArray(py::module_& m, const std::string& className) {
    py::class_<T>(m, className.c_str())
            .def("__len__", [](const T& a) {
                return a.length();
            })
            .def("__iter__", [](T& a) {
                return py::make_iterator(a.begin(), a.end());
            }, py::keep_alive<0, 1>())
            .def("__getitem__", [](T& a, size_t index) {
                if (index >= a.length()) {
                    throw pypowsybl::PyPowsyblError("Index out of bounds.");
                }
                return *(a.begin() + index);
            }, py::keep_alive<0, 1>());
}

void deleteDataframe(dataframe* df) {
    for (int indice = 0 ; indice < df->series_count; indice ++) {
        series* column = df->series + indice;
        if (column->type == 0) {
            pypowsybl::deleteCharPtrPtr((char**) column->data.ptr, column->data.length);
        } else if (column->type == 1) {
            delete[] (double*) column->data.ptr;
        } else if (column->type == 2 || column->type == 3) {
            delete[] (int*) column->data.ptr;
        }
        delete[] column->name;
    }
    delete[] df->series;
    delete df;
}

std::shared_ptr<dataframe> createDataframe(py::list columnsValues, const std::vector<std::string>& columnsNames, const std::vector<int>& columnsTypes, const std::vector<bool>& isIndex) {
    int columnsNumber = columnsNames.size();
    std::shared_ptr<dataframe> dataframe(new ::dataframe(), ::deleteDataframe);
    series* columns = new series[columnsNumber];
    for (int indice = 0 ; indice < columnsNumber ; indice ++ ) {
        series* column = columns + indice;
        py::str name = (py::str) columnsNames[indice];
        column->name = strdup(((std::string) name).c_str());
        column->index = int(isIndex[indice]);
        int type = columnsTypes[indice];
        column->type = type;
        if (type == 0) {
            try {
                std::vector<std::string> values = py::cast<std::vector<std::string>>(columnsValues[indice]);
                column->data.length = values.size();
                column->data.ptr = pypowsybl::copyVectorStringToCharPtrPtr(values);
            }
            catch(const py::cast_error& e) {
                throw pypowsybl::PyPowsyblError("Data of column \"" + columnsNames[indice] + "\" has the wrong type, expected string");
            }
        } else if (type == 1) {
            try {
                std::vector<double> values = py::cast<std::vector<double>>(columnsValues[indice]);
                column->data.length = values.size();
                column->data.ptr = pypowsybl::copyVectorDouble(values);
            }
            catch(const py::cast_error& e) {
                throw pypowsybl::PyPowsyblError("Data of column \"" + columnsNames[indice] + "\" has the wrong type, expected float");
            }
        } else if (type == 2 || type == 3) {
            try {
                std::vector<int> values = py::cast<std::vector<int>>(columnsValues[indice]);
                column->data.length = values.size();
                column->data.ptr = pypowsybl::copyVectorInt(values);
            }
            catch(const py::cast_error& e) {
                std::string expected = type == 2 ? "int" : "bool";
                throw pypowsybl::PyPowsyblError("Data of column \"" + columnsNames[indice] + "\" has the wrong type, expected " + expected);
            }
        }
    }
    dataframe->series_count = columnsNumber;
    dataframe->series = columns;
    return dataframe;
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

void createElementBind(pypowsybl::JavaHandle network, const std::vector<dataframe*>& dataframes, element_type elementType) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::createElement(network, dataframeArray.get(), elementType);
}

void createNetworkModificationBind(pypowsybl::JavaHandle network, const std::vector<dataframe*>& dataframes, network_modification_type networkModificationType, bool throwException, pypowsybl::JavaHandle* reportNode) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::createNetworkModification(network, dataframeArray.get(), networkModificationType, throwException, reportNode);
}

void createExtensionsBind(pypowsybl::JavaHandle network, const std::vector<dataframe*>& dataframes, std::string& name) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::createExtensions(network, dataframeArray.get(), name);
}

void addDynamicMappingsBind(pypowsybl::JavaHandle dynamic_mapping_handle, DynamicMappingType mapping_type, const std::vector<dataframe*>& dataframes) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::addDynamicMappings(dynamic_mapping_handle, mapping_type, dataframeArray.get());
}

template<typename T>
py::array seriesAsNumpyArray(const series& series) {
	//Last argument is to bind lifetime of series to the returned array
    return py::array(py::dtype::of<T>(), series.data.length, series.data.ptr, py::cast(series));
}

py::memoryview pyGetGrid2opIntegerValue(const pypowsybl::JavaHandle& backendHandle, Grid2opIntegerValueType valueType) {
    ::array* array = pypowsybl::getGrid2opIntegerValue(backendHandle, valueType);
    return py::memoryview::from_buffer(
            static_cast<int*>(array->ptr),
            { array->length },
            { sizeof(int) }
    );
}

py::memoryview pyGetGrid2opDoubleValue(const pypowsybl::JavaHandle& backendHandle, Grid2opDoubleValueType valueType) {
    ::array* array = pypowsybl::getGrid2opDoubleValue(backendHandle, valueType);
    return py::memoryview::from_buffer(
            static_cast<double*>(array->ptr),
            { array->length },
            { sizeof(double) }
    );
}

void pyUpdateGrid2opDoubleValue(const pypowsybl::JavaHandle& backendHandle, Grid2opUpdateDoubleValueType valueType,
                                py::array_t<double, py::array::c_style | py::array::forcecast> value_array,
                                py::array_t<int, py::array::c_style | py::array::forcecast> changed_array) {
    pypowsybl::updateGrid2opDoubleValue(backendHandle, valueType, value_array.mutable_data(), changed_array.mutable_data());
}

void pyUpdateGrid2opIntegerValue(const pypowsybl::JavaHandle& backendHandle, Grid2opUpdateIntegerValueType valueType,
                                 py::array_t<int, py::array::c_style | py::array::forcecast> value_array,
                                 py::array_t<int, py::array::c_style | py::array::forcecast> changed_array) {
    pypowsybl::updateGrid2opIntegerValue(backendHandle, valueType, value_array.mutable_data(), changed_array.mutable_data());
}

void dynamicSimulationBindings(py::module_& m) {

    py::enum_<DynamicMappingType>(m, "DynamicMappingType")
        .value("BASE_LOAD", DynamicMappingType::BASE_LOAD)
        .value("LOAD_ONE_TRANSFORMER", DynamicMappingType::LOAD_ONE_TRANSFORMER)
        .value("LOAD_ONE_TRANSFORMER_TAP_CHANGER", DynamicMappingType::LOAD_ONE_TRANSFORMER_TAP_CHANGER)
        .value("LOAD_TWO_TRANSFORMERS", DynamicMappingType::LOAD_TWO_TRANSFORMERS)
        .value("LOAD_TWO_TRANSFORMERS_TAP_CHANGERS", DynamicMappingType::LOAD_TWO_TRANSFORMERS_TAP_CHANGERS)
        .value("BASE_GENERATOR", DynamicMappingType::BASE_GENERATOR)
        .value("SYNCHRONIZED_GENERATOR", DynamicMappingType::SYNCHRONIZED_GENERATOR)
        .value("SYNCHRONOUS_GENERATOR", DynamicMappingType::SYNCHRONOUS_GENERATOR)
        .value("WECC", DynamicMappingType::WECC)
        .value("GRID_FORMING_CONVERTER", DynamicMappingType::GRID_FORMING_CONVERTER)
        .value("SIGNAL_N_GENERATOR", DynamicMappingType::SIGNAL_N_GENERATOR)
        .value("HVDC_P", DynamicMappingType::HVDC_P)
        .value("HVDC_VSC", DynamicMappingType::HVDC_VSC)
        .value("BASE_TRANSFORMER", DynamicMappingType::BASE_TRANSFORMER)
        .value("BASE_STATIC_VAR_COMPENSATOR", DynamicMappingType::BASE_STATIC_VAR_COMPENSATOR)
        .value("BASE_LINE", DynamicMappingType::BASE_LINE)
        .value("BASE_BUS", DynamicMappingType::BASE_BUS)
        .value("INFINITE_BUS", DynamicMappingType::INFINITE_BUS)
        .value("OVERLOAD_MANAGEMENT_SYSTEM", DynamicMappingType::OVERLOAD_MANAGEMENT_SYSTEM)
        .value("TWO_LEVELS_OVERLOAD_MANAGEMENT_SYSTEM", DynamicMappingType::TWO_LEVELS_OVERLOAD_MANAGEMENT_SYSTEM)
        .value("UNDER_VOLTAGE", DynamicMappingType::UNDER_VOLTAGE)
        .value("PHASE_SHIFTER_I", DynamicMappingType::PHASE_SHIFTER_I)
        .value("PHASE_SHIFTER_P", DynamicMappingType::PHASE_SHIFTER_P)
        .value("PHASE_SHIFTER_BLOCKING_I", DynamicMappingType::PHASE_SHIFTER_BLOCKING_I)
        .value("TAP_CHANGER", DynamicMappingType::TAP_CHANGER)
        .value("TAP_CHANGER_BLOCKING", DynamicMappingType::TAP_CHANGER_BLOCKING);

    py::enum_<EventMappingType>(m, "EventMappingType")
            .value("DISCONNECT", EventMappingType::DISCONNECT)
            .value("NODE_FAULT", EventMappingType::NODE_FAULT)
            .value("ACTIVE_POWER_VARIATION", EventMappingType::ACTIVE_POWER_VARIATION);

    //entrypoints for constructors
    m.def("create_dynamic_simulation_context", &pypowsybl::createDynamicSimulationContext);
    m.def("create_dynamic_model_mapping", &pypowsybl::createDynamicModelMapping);
    m.def("create_timeseries_mapping", &pypowsybl::createTimeseriesMapping);
    m.def("create_event_mapping", &pypowsybl::createEventMapping);

    //running simulations
    m.def("run_dynamic_model", &pypowsybl::runDynamicModel, py::call_guard<py::gil_scoped_release>(),
        py::arg("dynamic_model"), py::arg("network"), py::arg("dynamic_mapping"), py::arg("event_mapping"), py::arg("timeseries_mapping"), py::arg("start"), py::arg("stop"), py::arg("report_node"));

    //model mapping
    m.def("add_all_dynamic_mappings", ::addDynamicMappingsBind, py::arg("dynamic_mapping_handle"), py::arg("mapping_type"), py::arg("dataframes"));
    m.def("get_dynamic_mappings_meta_data", &pypowsybl::getDynamicMappingsMetaData, py::arg("mapping_type"));
    m.def("get_supported_models", &pypowsybl::getSupportedModels, py::arg("mapping_type"));

    // timeseries/curves mapping
    m.def("add_curve", &pypowsybl::addCurve, py::arg("curve_mapping_handle"), py::arg("dynamic_id"), py::arg("variable"));

    // events mapping
    m.def("add_all_event_mappings", &pypowsybl::addEventMappings, py::arg("event_mapping_handle"), py::arg("mapping_type"), py::arg("mapping_df"));
    m.def("get_event_mappings_meta_data", &pypowsybl::getEventMappingsMetaData, py::arg("mapping_type"));

    // Simulation results
    m.def("get_dynamic_simulation_results_status", &pypowsybl::getDynamicSimulationResultsStatus, py::arg("result_handle"));
    m.def("get_dynamic_curve", &pypowsybl::getDynamicCurve, py::arg("report_handle"), py::arg("curve_name"));
    m.def("get_all_dynamic_curves_ids", &pypowsybl::getAllDynamicCurvesIds, py::arg("report_handle"));
}

void voltageInitializerBinding(py::module_& m) {
    py::enum_<VoltageInitializerStatus>(m, "VoltageInitializerStatus")
        .value("OK", VoltageInitializerStatus::OK)
        .value("NOT_OK", VoltageInitializerStatus::NOT_OK);

    py::enum_<VoltageInitializerObjective>(m, "VoltageInitializerObjective")
        .value("MIN_GENERATION", VoltageInitializerObjective::MIN_GENERATION)
        .value("BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT", VoltageInitializerObjective::BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT)
        .value("SPECIFIC_VOLTAGE_PROFILE", VoltageInitializerObjective::SPECIFIC_VOLTAGE_PROFILE);

    py::enum_<VoltageInitializerLogLevelAmpl>(m, "VoltageInitializerLogLevelAmpl")
        .value("DEBUG", VoltageInitializerLogLevelAmpl::LOG_AMPL_DEBUG)
        .value("INFO", VoltageInitializerLogLevelAmpl::LOG_AMPL_INFO)
        .value("WARNING", VoltageInitializerLogLevelAmpl::LOG_AMPL_WARNING)
        .value("ERROR", VoltageInitializerLogLevelAmpl::LOG_AMPL_ERROR);

    py::enum_<VoltageInitializerLogLevelSolver>(m, "VoltageInitializerLogLevelSolver")
        .value("NOTHING", VoltageInitializerLogLevelSolver::NOTHING)
        .value("ONLY_RESULTS", VoltageInitializerLogLevelSolver::ONLY_RESULTS)
        .value("EVERYTHING", VoltageInitializerLogLevelSolver::EVERYTHING);

    py::enum_<VoltageInitializerReactiveSlackBusesMode>(m, "VoltageInitializerReactiveSlackBusesMode")
        .value("CONFIGURED", VoltageInitializerReactiveSlackBusesMode::CONFIGURED)
        .value("NO_GENERATION", VoltageInitializerReactiveSlackBusesMode::NO_GENERATION)
        .value("ALL_BUSES", VoltageInitializerReactiveSlackBusesMode::ALL_BUSES);

    m.def("create_voltage_initializer_params", &pypowsybl::createVoltageInitializerParams);

    m.def("voltage_initializer_add_variable_shunt_compensators", &pypowsybl::voltageInitializerAddVariableShuntCompensators, py::arg("params_handle"), py::arg("id_ptr"));
    m.def("voltage_initializer_add_constant_q_generators", &pypowsybl::voltageInitializerAddConstantQGenerators, py::arg("params_handle"), py::arg("id_ptr"));
    m.def("voltage_initializer_add_variable_two_windings_transformers", &pypowsybl::voltageInitializerAddVariableTwoWindingsTransformers, py::arg("params_handle"), py::arg("id_ptr"));
    m.def("voltage_initializer_add_specific_low_voltage_limits", &pypowsybl::voltageInitializerAddSpecificLowVoltageLimits, py::arg("params_handle"), py::arg("voltage_level_id"), py::arg("is_relative"), py::arg("limit"));
    m.def("voltage_initializer_add_specific_high_voltage_limits", &pypowsybl::voltageInitializerAddSpecificHighVoltageLimits, py::arg("params_handle"), py::arg("voltage_level_id"), py::arg("is_relative"), py::arg("limit"));
    m.def("voltage_initializer_add_configured_reactive_slack_buses", &pypowsybl::voltageInitializerAddConfiguredReactiveSlackBuses, py::arg("params_handle"), py::arg("id_ptr"));

    m.def("voltage_initializer_set_objective", &pypowsybl::voltageInitializerSetObjective, py::arg("params_handle"), py::arg("c_objective"));
    m.def("voltage_initializer_set_objective_distance", &pypowsybl::voltageInitializerSetObjectiveDistance, py::arg("params_handle"), py::arg("dist"));

    m.def("voltage_initializer_set_default_variable_scaling_factor", &pypowsybl::voltageInitializerSetDefaultVariableScalingFactor, py::arg("params_handle"), py::arg("default_variable_scaling_factor"));
    m.def("voltage_initializer_set_default_constraint_scaling_factor", &pypowsybl::voltageInitializerSetDefaultConstraintScalingFactor, py::arg("params_handle"), py::arg("default_constraint_scaling_factor"));
    m.def("voltage_initializer_set_reactive_slack_variable_scaling_factor", &pypowsybl::voltageInitializerSetReactiveSlackVariableScalingFactor, py::arg("params_handle"), py::arg("reactive_slack_variable_scaling_factor"));
    m.def("voltage_initializer_set_twt_ratio_variable_scaling_factor", &pypowsybl::voltageInitializerSetTwoWindingTransformerRatioVariableScalingFactor, py::arg("params_handle"), py::arg("twt_ratio_variable_scaling_factor"));

    m.def("run_voltage_initializer", &pypowsybl::runVoltageInitializer, py::arg("debug"), py::arg("network_handle"), py::arg("params_handle"));

    m.def("voltage_initializer_set_log_level_ampl", &pypowsybl::voltageInitializerSetLogLevelAmpl, py::arg("params_handle"), py::arg("log_level_ampl"));
    m.def("voltage_initializer_set_log_level_solver", &pypowsybl::voltageInitializerSetLogLevelSolver, py::arg("params_handle"), py::arg("log_level_solver"));
    m.def("voltage_initializer_set_reactive_slack_buses_mode", &pypowsybl::voltageInitializerSetReactiveSlackBusesMode, py::arg("params_handle"), py::arg("reactive_slack_buses_mode"));
    m.def("voltage_initializer_set_min_plausible_low_voltage_limit", &pypowsybl::voltageInitializerSetMinPlausibleLowVoltageLimit, py::arg("params_handle"), py::arg("min_plausible_low_voltage_limit"));
    m.def("voltage_initializer_set_max_plausible_high_voltage_limit", &pypowsybl::voltageInitializerSetMaxPlausibleHighVoltageLimit, py::arg("params_handle"), py::arg("max_plausible_high_voltage_limit"));
    m.def("voltage_initializer_set_active_power_variation_rate", &pypowsybl::voltageInitializerSetActivePowerVariationRate, py::arg("params_handle"), py::arg("active_power_variation_rate"));
    m.def("voltage_initializer_set_min_plausible_active_power_threshold", &pypowsybl::voltageInitializerSetMinPlausibleActivePowerThreshold, py::arg("params_handle"), py::arg("min_plausible_active_power_threshold"));
    m.def("voltage_initializer_set_low_impedance_threshold", &pypowsybl::voltageInitializerSetLowImpedanceThreshold, py::arg("params_handle"), py::arg("low_impedance_threshold"));
    m.def("voltage_initializer_set_min_nominal_voltage_ignored_bus", &pypowsybl::voltageInitializerSetMinNominalVoltageIgnoredBus, py::arg("params_handle"), py::arg("min_nominal_voltage_ignored_bus"));
    m.def("voltage_initializer_set_min_nominal_voltage_ignored_voltage_bounds", &pypowsybl::voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds, py::arg("params_handle"), py::arg("min_nominal_voltage_ignored_voltage_bounds"));
    m.def("voltage_initializer_set_max_plausible_power_limit", &pypowsybl::voltageInitializerSetMaxPlausiblePowerLimit, py::arg("params_handle"), py::arg("max_plausible_power_limit"));
    m.def("voltage_initializer_set_high_active_power_default_limit", &pypowsybl::voltageInitializerSetHighActivePowerDefaultLimit, py::arg("params_handle"), py::arg("high_active_power_default_limit"));
    m.def("voltage_initializer_set_low_active_power_default_limit", &pypowsybl::voltageInitializerSetLowActivePowerDefaultLimit, py::arg("params_handle"), py::arg("low_active_power_default_limit"));
    m.def("voltage_initializer_set_default_minimal_qp_range", &pypowsybl::voltageInitializerSetDefaultMinimalQPRange, py::arg("params_handle"), py::arg("default_minimal_qp_range"));
    m.def("voltage_initializer_set_default_qmax_pmax_ratio", &pypowsybl::voltageInitializerSetDefaultQmaxPmaxRatio, py::arg("params_handle"), py::arg("default_qmax_pmax_ratio"));

    m.def("voltage_initializer_apply_all_modifications", &pypowsybl::voltageInitializerApplyAllModifications, py::arg("result_handle"), py::arg("network_handle"));
    m.def("voltage_initializer_get_status", &pypowsybl::voltageInitializerGetStatus, py::arg("result_handle"));
    m.def("voltage_initializer_get_indicators", &pypowsybl::voltageInitializerGetIndicators, py::arg("result_handle"));
}

PYBIND11_MODULE(_pypowsybl, m) {
    auto preJavaCall = [](pypowsybl::GraalVmGuard* guard, exception_handler* exc){
      setLogLevelFromPythonLogger(guard, exc);
    };
    auto postJavaCall = [](){
      py::gil_scoped_acquire acquire;
      if (PyErr_Occurred() != nullptr) {
        throw py::error_already_set();
      }
    };
    pypowsybl::init(preJavaCall, postJavaCall);
    m.doc() = "PowSyBl Python API";

    py::register_exception<pypowsybl::PyPowsyblError>(m, "PyPowsyblError");

    py::class_<pypowsybl::JavaHandle>(m, "JavaHandle");

    m.def("set_java_library_path", &pypowsybl::setJavaLibraryPath, "Set java.library.path JVM property");

    m.def("set_config_read", &pypowsybl::setConfigRead, "Set config read mode");

    m.def("set_default_loadflow_provider", &pypowsybl::setDefaultLoadFlowProvider, "Set default loadflow provider", py::arg("provider"));

    m.def("set_default_security_analysis_provider", &pypowsybl::setDefaultSecurityAnalysisProvider, "Set default security analysis provider", py::arg("provider"));

    m.def("set_default_sensitivity_analysis_provider", &pypowsybl::setDefaultSensitivityAnalysisProvider, "Set default sensitivity analysis provider", py::arg("provider"));

    m.def("is_config_read", &pypowsybl::isConfigRead, "Get config read mode");

    m.def("get_default_loadflow_provider", &pypowsybl::getDefaultLoadFlowProvider, "Get default loadflow provider");

    m.def("get_default_security_analysis_provider", &pypowsybl::getDefaultSecurityAnalysisProvider, "Get default security analysis provider");

    m.def("get_default_sensitivity_analysis_provider", &pypowsybl::getDefaultSensitivityAnalysisProvider, "Get default sensitivity analysis provider");

    m.def("get_version_table", &pypowsybl::getVersionTable, "Get an ASCII table with all PowSybBl modules version");

    m.def("create_network", &pypowsybl::createNetwork, "Create an example network", py::arg("name"), py::arg("id"));

    m.def("update_switch_position", &pypowsybl::updateSwitchPosition, "Update a switch position");

    m.def("merge", &pypowsybl::merge, "Merge several networks");

    m.def("get_sub_network", &pypowsybl::getSubNetwork, "Get a sub network from its ID", py::arg("network"), py::arg("sub_network_id"));

    m.def("detach_sub_network", &pypowsybl::detachSubNetwork, "Detach a sub network from its parent", py::arg("sub_network"));

    m.def("update_connectable_status", &pypowsybl::updateConnectableStatus, "Update a connectable (branch or injection) status");

    py::enum_<element_type>(m, "ElementType")
            .value("BUS", element_type::BUS)
            .value("BUS_FROM_BUS_BREAKER_VIEW", element_type::BUS_FROM_BUS_BREAKER_VIEW)
            .value("LINE", element_type::LINE)
            .value("TWO_WINDINGS_TRANSFORMER", element_type::TWO_WINDINGS_TRANSFORMER)
            .value("THREE_WINDINGS_TRANSFORMER", element_type::THREE_WINDINGS_TRANSFORMER)
            .value("GENERATOR", element_type::GENERATOR)
            .value("LOAD", element_type::LOAD)
            .value("GROUND", element_type::GROUND)
            .value("BATTERY", element_type::BATTERY)
            .value("SHUNT_COMPENSATOR", element_type::SHUNT_COMPENSATOR)
            .value("NON_LINEAR_SHUNT_COMPENSATOR_SECTION", element_type::NON_LINEAR_SHUNT_COMPENSATOR_SECTION)
            .value("LINEAR_SHUNT_COMPENSATOR_SECTION", element_type::LINEAR_SHUNT_COMPENSATOR_SECTION)
            .value("DANGLING_LINE", element_type::DANGLING_LINE)
            .value("DANGLING_LINE_GENERATION", element_type::DANGLING_LINE_GENERATION)
            .value("TIE_LINE", element_type::TIE_LINE)
            .value("LCC_CONVERTER_STATION", element_type::LCC_CONVERTER_STATION)
            .value("VSC_CONVERTER_STATION", element_type::VSC_CONVERTER_STATION)
            .value("STATIC_VAR_COMPENSATOR", element_type::STATIC_VAR_COMPENSATOR)
            .value("SWITCH", element_type::SWITCH)
            .value("VOLTAGE_LEVEL", element_type::VOLTAGE_LEVEL)
            .value("SUBSTATION", element_type::SUBSTATION)
            .value("BUSBAR_SECTION", element_type::BUSBAR_SECTION)
            .value("HVDC_LINE", element_type::HVDC_LINE)
            .value("RATIO_TAP_CHANGER_STEP", element_type::RATIO_TAP_CHANGER_STEP)
            .value("PHASE_TAP_CHANGER_STEP", element_type::PHASE_TAP_CHANGER_STEP)
            .value("RATIO_TAP_CHANGER", element_type::RATIO_TAP_CHANGER)
            .value("PHASE_TAP_CHANGER", element_type::PHASE_TAP_CHANGER)
            .value("REACTIVE_CAPABILITY_CURVE_POINT", element_type::REACTIVE_CAPABILITY_CURVE_POINT)
            .value("OPERATIONAL_LIMITS", element_type::OPERATIONAL_LIMITS)
            .value("SELECTED_OPERATIONAL_LIMITS", element_type::SELECTED_OPERATIONAL_LIMITS)
            .value("MINMAX_REACTIVE_LIMITS", element_type::MINMAX_REACTIVE_LIMITS)
            .value("ALIAS", element_type::ALIAS)
            .value("IDENTIFIABLE", element_type::IDENTIFIABLE)
            .value("INJECTION", element_type::INJECTION)
            .value("BRANCH", element_type::BRANCH)
            .value("TERMINAL", element_type::TERMINAL)
            .value("SUB_NETWORK", element_type::SUB_NETWORK)
            .value("AREA", element_type::AREA)
            .value("AREA_VOLTAGE_LEVELS", element_type::AREA_VOLTAGE_LEVELS)
            .value("AREA_BOUNDARIES", element_type::AREA_BOUNDARIES)
            .value("INTERNAL_CONNECTION", element_type::INTERNAL_CONNECTION);

    py::enum_<filter_attributes_type>(m, "FilterAttributesType")
            .value("ALL_ATTRIBUTES", filter_attributes_type::ALL_ATTRIBUTES)
            .value("DEFAULT_ATTRIBUTES", filter_attributes_type::DEFAULT_ATTRIBUTES)
            .value("SELECTION_ATTRIBUTES", filter_attributes_type::SELECTION_ATTRIBUTES);

    py::enum_<validation_type>(m, "ValidationType")
            .value("FLOWS", validation_type::FLOWS)
            .value("GENERATORS", validation_type::GENERATORS)
            .value("BUSES", validation_type::BUSES)
            .value("SVCS", validation_type::SVCS)
            .value("SHUNTS", validation_type::SHUNTS)
            .value("TWTS", validation_type::TWTS)
            .value("TWTS3W", validation_type::TWTS3W);

    py::enum_<network_modification_type>(m, "NetworkModificationType")
            .value("VOLTAGE_LEVEL_TOPOLOGY_CREATION", network_modification_type::VOLTAGE_LEVEL_TOPOLOGY_CREATION)
            .value("CREATE_COUPLING_DEVICE", network_modification_type::CREATE_COUPLING_DEVICE)
            .value("CREATE_FEEDER_BAY", network_modification_type::CREATE_FEEDER_BAY)
            .value("CREATE_LINE_FEEDER", network_modification_type::CREATE_LINE_FEEDER)
            .value("CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER", network_modification_type::CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER)
            .value("CREATE_LINE_ON_LINE", network_modification_type::CREATE_LINE_ON_LINE)
            .value("REVERT_CREATE_LINE_ON_LINE", network_modification_type::REVERT_CREATE_LINE_ON_LINE)
            .value("CONNECT_VOLTAGE_LEVEL_ON_LINE", network_modification_type::CONNECT_VOLTAGE_LEVEL_ON_LINE)
            .value("REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE", network_modification_type::REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE)
            .value("REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE", network_modification_type::REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE);

    py::enum_<remove_modification_type>(m, "RemoveModificationType")
        .value("REMOVE_FEEDER", remove_modification_type::REMOVE_FEEDER)
        .value("REMOVE_VOLTAGE_LEVEL", remove_modification_type::REMOVE_VOLTAGE_LEVEL)
        .value("REMOVE_HVDC_LINE", remove_modification_type::REMOVE_HVDC_LINE);

    m.def("get_network_elements_ids", &pypowsybl::getNetworkElementsIds, "Get network elements ids for a given element type",
          py::arg("network"), py::arg("element_type"), py::arg("nominal_voltages"),
          py::arg("countries"), py::arg("main_connected_component"), py::arg("main_synchronous_component"),
          py::arg("not_connected_to_same_bus_at_both_sides"));

    m.def("get_network_import_formats", &pypowsybl::getNetworkImportFormats, "Get supported import formats");
    m.def("get_network_import_supported_extensions", &pypowsybl::getNetworkImportSupportedExtensions, "Get supported import extensions");
    m.def("get_network_export_formats", &pypowsybl::getNetworkExportFormats, "Get supported export formats");
    m.def("get_network_import_post_processors", &pypowsybl::getNetworkImportPostProcessors, "Get supported import post processors");

    m.def("get_loadflow_provider_names", &pypowsybl::getLoadFlowProviderNames, "Get supported loadflow providers");
    m.def("get_security_analysis_provider_names", &pypowsybl::getSecurityAnalysisProviderNames, "Get supported security analysis providers");
    m.def("get_sensitivity_analysis_provider_names", &pypowsybl::getSensitivityAnalysisProviderNames, "Get supported sensitivity analysis providers");

    m.def("create_importer_parameters_series_array", &pypowsybl::createImporterParametersSeriesArray, "Create a parameters series array for a given import format",
          py::arg("format"));

    m.def("create_exporter_parameters_series_array", &pypowsybl::createExporterParametersSeriesArray, "Create a parameters series array for a given export format",
          py::arg("format"));

    m.def("load_network", &pypowsybl::loadNetwork, "Load a network from a file", py::call_guard<py::gil_scoped_release>(),
          py::arg("file"), py::arg("parameters"), py::arg("post_processors"), py::arg("report_node"));

    m.def("load_network_from_string", &pypowsybl::loadNetworkFromString, "Load a network from a string", py::call_guard<py::gil_scoped_release>(),
              py::arg("file_name"), py::arg("file_content"),py::arg("parameters"), py::arg("post_processors"), py::arg("report_node"));

    m.def("load_network_from_binary_buffers", ::loadNetworkFromBinaryBuffersPython, "Load a network from a list of binary buffer", py::call_guard<py::gil_scoped_release>(),
              py::arg("buffers"), py::arg("parameters"), py::arg("post_processors"), py::arg("report_node"));

    m.def("save_network", &pypowsybl::saveNetwork, "Save network to a file in a given format", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("file"),py::arg("format"), py::arg("parameters"), py::arg("report_node"));

    m.def("save_network_to_string", &pypowsybl::saveNetworkToString, "Save network in a given format to a string", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("format"), py::arg("parameters"), py::arg("report_node"));

    m.def("save_network_to_binary_buffer", ::saveNetworkToBinaryBufferPython, "Save network in a given format to a binary byffer", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("format"), py::arg("parameters"), py::arg("report_node"));

    m.def("reduce_network", &pypowsybl::reduceNetwork, "Reduce network", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("v_min"), py::arg("v_max"),
          py::arg("ids"), py::arg("vls"), py::arg("depths"), py::arg("with_dangling_lines"));

    py::enum_<pypowsybl::LoadFlowComponentStatus>(m, "LoadFlowComponentStatus", "Loadflow status for one connected component.")
            .value("CONVERGED", pypowsybl::LoadFlowComponentStatus::CONVERGED, "The loadflow has converged.")
            .value("FAILED", pypowsybl::LoadFlowComponentStatus::FAILED, "The loadflow has failed.")
            .value("MAX_ITERATION_REACHED", pypowsybl::LoadFlowComponentStatus::MAX_ITERATION_REACHED, "The loadflow has reached its maximum iterations count.")
            .value("NO_CALCULATION", pypowsybl::LoadFlowComponentStatus::NO_CALCULATION, "The component was not calculated.")
            .def("__bool__", [](const pypowsybl::LoadFlowComponentStatus& status) {
                return status == pypowsybl::LoadFlowComponentStatus::CONVERGED;
            });

    py::enum_<pypowsybl::PostContingencyComputationStatus>(m, "PostContingencyComputationStatus", "Loadflow status for one connected component after contingency for security analysis.")
            .value("CONVERGED", pypowsybl::PostContingencyComputationStatus::CONVERGED, "The loadflow has converged.")
            .value("FAILED", pypowsybl::PostContingencyComputationStatus::FAILED, "The loadflow has failed.")
            .value("MAX_ITERATION_REACHED", pypowsybl::PostContingencyComputationStatus::MAX_ITERATION_REACHED, "The loadflow has reached its maximum iterations count.")
            .value("SOLVER_FAILED", pypowsybl::PostContingencyComputationStatus::SOLVER_FAILED, "The loadflow numerical solver has failed.")
            .value("NO_IMPACT", pypowsybl::PostContingencyComputationStatus::NO_IMPACT, "The contingency has no impact.");

    py::class_<slack_bus_result>(m, "SlackBusResult")
            .def_property_readonly("id", [](const slack_bus_result& v) {
                return v.id;
            })
            .def_property_readonly("active_power_mismatch", [](const slack_bus_result& v) {
                return v.active_power_mismatch;
            });

    bindArray<pypowsybl::SlackBusResultArray>(m, "SlackBusResultArray");

    py::class_<loadflow_component_result>(m, "LoadFlowComponentResult", "Loadflow result for one connected component of the network.")
            .def_property_readonly("connected_component_num", [](const loadflow_component_result& r) {
                return r.connected_component_num;
            })
            .def_property_readonly("synchronous_component_num", [](const loadflow_component_result& r) {
                return r.synchronous_component_num;
            })
            .def_property_readonly("status", [](const loadflow_component_result& r) {
                return static_cast<pypowsybl::LoadFlowComponentStatus>(r.status);
            })
            .def_property_readonly("status_text", [](const loadflow_component_result& r) {
                return r.status_text;
            })
            .def_property_readonly("iteration_count", [](const loadflow_component_result& r) {
                return r.iteration_count;
            })
            .def_property_readonly("slack_bus_results", [](const loadflow_component_result& r) {
                return pypowsybl::SlackBusResultArray((array *) & r.slack_bus_results);
            })
            .def_property_readonly("reference_bus_id", [](const loadflow_component_result& r) {
                return r.reference_bus_id;
            })
            .def_property_readonly("distributed_active_power", [](const loadflow_component_result& r) {
                return r.distributed_active_power;
            });

    bindArray<pypowsybl::LoadFlowComponentResultArray>(m, "LoadFlowComponentResultArray");

    py::enum_<pypowsybl::VoltageInitMode>(m, "VoltageInitMode", "Define the computation starting point.")
            .value("UNIFORM_VALUES", pypowsybl::VoltageInitMode::UNIFORM_VALUES, "Initialize voltages to uniform values based on nominale voltage.")
            .value("PREVIOUS_VALUES", pypowsybl::VoltageInitMode::PREVIOUS_VALUES, "Use previously computed voltage values as as starting point.")
            .value("DC_VALUES", pypowsybl::VoltageInitMode::DC_VALUES, "Use values computed by a DC loadflow as a starting point.");

    py::enum_<pypowsybl::BalanceType>(m, "BalanceType", "Define how to distribute slack bus imbalance.")
            .value("PROPORTIONAL_TO_GENERATION_P", pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_P,
                   "Distribute slack on generators, in proportion of target P")
            .value("PROPORTIONAL_TO_GENERATION_P_MAX", pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_P_MAX,
                   "Distribute slack on generators, in proportion of max P")
            .value("PROPORTIONAL_TO_GENERATION_REMAINING_MARGIN", pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_REMAINING_MARGIN,
                   "Distribute slack on generators, in proportion of max P - target P")
            .value("PROPORTIONAL_TO_GENERATION_PARTICIPATION_FACTOR", pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_PARTICIPATION_FACTOR,
                   "Distribute slack on generators, in proportion of participationFactor (see ActivePowerControl extension)")
            .value("PROPORTIONAL_TO_LOAD", pypowsybl::BalanceType::PROPORTIONAL_TO_LOAD,
                   "Distribute slack on loads, in proportion of load")
            .value("PROPORTIONAL_TO_CONFORM_LOAD", pypowsybl::BalanceType::PROPORTIONAL_TO_CONFORM_LOAD,
                   "Distribute slack on loads, in proportion of conform load");

    py::enum_<pypowsybl::ConnectedComponentMode>(m, "ConnectedComponentMode", "Define which connected components to run on.")
            .value("ALL", pypowsybl::ConnectedComponentMode::ALL, "Run on all connected components")
            .value("MAIN", pypowsybl::ConnectedComponentMode::MAIN, "Run only on the main connected component");

    py::class_<array_struct, std::shared_ptr<array_struct>>(m, "ArrayStruct")
            .def(py::init());

    py::class_<dataframe, std::shared_ptr<dataframe>>(m, "Dataframe");

    py::class_<pypowsybl::LoadFlowParameters>(m, "LoadFlowParameters")
            .def(py::init(&pypowsybl::createLoadFlowParameters))
            .def_readwrite("voltage_init_mode", &pypowsybl::LoadFlowParameters::voltage_init_mode)
            .def_readwrite("transformer_voltage_control_on", &pypowsybl::LoadFlowParameters::transformer_voltage_control_on)
            .def_readwrite("use_reactive_limits", &pypowsybl::LoadFlowParameters::use_reactive_limits)
            .def_readwrite("phase_shifter_regulation_on", &pypowsybl::LoadFlowParameters::phase_shifter_regulation_on)
            .def_readwrite("twt_split_shunt_admittance", &pypowsybl::LoadFlowParameters::twt_split_shunt_admittance)
            .def_readwrite("shunt_compensator_voltage_control_on", &pypowsybl::LoadFlowParameters::shunt_compensator_voltage_control_on)
            .def_readwrite("read_slack_bus", &pypowsybl::LoadFlowParameters::read_slack_bus)
            .def_readwrite("write_slack_bus", &pypowsybl::LoadFlowParameters::write_slack_bus)
            .def_readwrite("distributed_slack", &pypowsybl::LoadFlowParameters::distributed_slack)
            .def_readwrite("balance_type", &pypowsybl::LoadFlowParameters::balance_type)
            .def_readwrite("dc_use_transformer_ratio", &pypowsybl::LoadFlowParameters::dc_use_transformer_ratio)
            .def_readwrite("countries_to_balance", &pypowsybl::LoadFlowParameters::countries_to_balance)
            .def_readwrite("connected_component_mode", &pypowsybl::LoadFlowParameters::connected_component_mode)
            .def_readwrite("dc_power_factor", &pypowsybl::LoadFlowParameters::dc_power_factor)
            .def_readwrite("provider_parameters_keys", &pypowsybl::LoadFlowParameters::provider_parameters_keys)
            .def_readwrite("provider_parameters_values", &pypowsybl::LoadFlowParameters::provider_parameters_values);

    py::class_<pypowsybl::LoadFlowValidationParameters>(m, "LoadFlowValidationParameters")
            .def(py::init(&pypowsybl::createValidationConfig))
            .def_readwrite("threshold", &pypowsybl::LoadFlowValidationParameters::threshold)
            .def_readwrite("verbose", &pypowsybl::LoadFlowValidationParameters::verbose)
            .def_readwrite("loadflow_name", &pypowsybl::LoadFlowValidationParameters::loadflow_name)
            .def_readwrite("epsilon_x", &pypowsybl::LoadFlowValidationParameters::epsilon_x)
            .def_readwrite("apply_reactance_correction", &pypowsybl::LoadFlowValidationParameters::apply_reactance_correction)
            .def_readwrite("loadflow_parameters", &pypowsybl::LoadFlowValidationParameters::loadflow_parameters)
            .def_readwrite("ok_missing_values", &pypowsybl::LoadFlowValidationParameters::ok_missing_values)
            .def_readwrite("no_requirement_if_reactive_bound_inversion", &pypowsybl::LoadFlowValidationParameters::no_requirement_if_reactive_bound_inversion)
            .def_readwrite("compare_results", &pypowsybl::LoadFlowValidationParameters::compare_results)
            .def_readwrite("check_main_component_only", &pypowsybl::LoadFlowValidationParameters::check_main_component_only)
            .def_readwrite("no_requirement_if_setpoint_outside_power_bounds", &pypowsybl::LoadFlowValidationParameters::no_requirement_if_setpoint_outside_power_bounds);

    py::class_<pypowsybl::SecurityAnalysisParameters>(m, "SecurityAnalysisParameters")
            .def(py::init(&pypowsybl::createSecurityAnalysisParameters))
            .def_readwrite("loadflow_parameters", &pypowsybl::SecurityAnalysisParameters::loadflow_parameters)
            .def_readwrite("flow_proportional_threshold", &pypowsybl::SecurityAnalysisParameters::flow_proportional_threshold)
            .def_readwrite("low_voltage_proportional_threshold", &pypowsybl::SecurityAnalysisParameters::low_voltage_proportional_threshold)
            .def_readwrite("low_voltage_absolute_threshold", &pypowsybl::SecurityAnalysisParameters::low_voltage_absolute_threshold)
            .def_readwrite("high_voltage_proportional_threshold", &pypowsybl::SecurityAnalysisParameters::high_voltage_proportional_threshold)
            .def_readwrite("high_voltage_absolute_threshold", &pypowsybl::SecurityAnalysisParameters::high_voltage_absolute_threshold)
            .def_readwrite("provider_parameters_keys", &pypowsybl::SecurityAnalysisParameters::provider_parameters_keys)
            .def_readwrite("provider_parameters_values", &pypowsybl::SecurityAnalysisParameters::provider_parameters_values);

    py::class_<pypowsybl::SensitivityAnalysisParameters>(m, "SensitivityAnalysisParameters")
            .def(py::init(&pypowsybl::createSensitivityAnalysisParameters))
            .def_readwrite("loadflow_parameters", &pypowsybl::SensitivityAnalysisParameters::loadflow_parameters)
            .def_readwrite("provider_parameters_keys", &pypowsybl::SensitivityAnalysisParameters::provider_parameters_keys)
            .def_readwrite("provider_parameters_values", &pypowsybl::SensitivityAnalysisParameters::provider_parameters_values);

    m.def("run_loadflow", &pypowsybl::runLoadFlow, "Run a load flow", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("dc"), py::arg("parameters"), py::arg("provider"), py::arg("report_node"));

    m.def("run_loadflow_validation", &pypowsybl::runLoadFlowValidation, "Run a load flow validation", py::arg("network"),
          py::arg("validation_type"), py::arg("validation_parameters"));

    py::class_<pypowsybl::SldParameters>(m, "SldParameters")
        .def(py::init(&pypowsybl::createSldParameters))
        .def_readwrite("use_name", &pypowsybl::SldParameters::use_name)
        .def_readwrite("center_name", &pypowsybl::SldParameters::center_name)
        .def_readwrite("diagonal_label", &pypowsybl::SldParameters::diagonal_label)
        .def_readwrite("nodes_infos", &pypowsybl::SldParameters::nodes_infos)
        .def_readwrite("tooltip_enabled", &pypowsybl::SldParameters::tooltip_enabled)
        .def_readwrite("topological_coloring", &pypowsybl::SldParameters::topological_coloring)
        .def_readwrite("component_library", &pypowsybl::SldParameters::component_library)
        .def_readwrite("display_current_feeder_info", &pypowsybl::SldParameters::display_current_feeder_info)
        .def_readwrite("active_power_unit", &pypowsybl::SldParameters::active_power_unit)
        .def_readwrite("reactive_power_unit", &pypowsybl::SldParameters::reactive_power_unit)
        .def_readwrite("current_unit", &pypowsybl::SldParameters::current_unit);

    py::enum_<pypowsybl::NadLayoutType>(m, "NadLayoutType")
            .value("FORCE_LAYOUT", pypowsybl::NadLayoutType::FORCE_LAYOUT)
            .value("GEOGRAPHICAL", pypowsybl::NadLayoutType::GEOGRAPHICAL);

    py::enum_<pypowsybl::EdgeInfoType>(m, "EdgeInfoType")
            .value("ACTIVE_POWER", pypowsybl::EdgeInfoType::ACTIVE_POWER)
            .value("REACTIVE_POWER", pypowsybl::EdgeInfoType::REACTIVE_POWER)
            .value("CURRENT", pypowsybl::EdgeInfoType::CURRENT);

    py::class_<pypowsybl::NadParameters>(m, "NadParameters")
        .def(py::init(&pypowsybl::createNadParameters))
        .def_readwrite("edge_name_displayed", &pypowsybl::NadParameters::edge_name_displayed)
        .def_readwrite("edge_info_along_edge", &pypowsybl::NadParameters::edge_info_along_edge)
        .def_readwrite("power_value_precision", &pypowsybl::NadParameters::power_value_precision)
        .def_readwrite("current_value_precision", &pypowsybl::NadParameters::current_value_precision)
        .def_readwrite("angle_value_precision", &pypowsybl::NadParameters::angle_value_precision)
        .def_readwrite("voltage_value_precision", &pypowsybl::NadParameters::voltage_value_precision)
        .def_readwrite("id_displayed", &pypowsybl::NadParameters::id_displayed)
        .def_readwrite("bus_legend", &pypowsybl::NadParameters::bus_legend)
        .def_readwrite("substation_description_displayed", &pypowsybl::NadParameters::substation_description_displayed)
        .def_readwrite("layout_type", &pypowsybl::NadParameters::layout_type)
        .def_readwrite("scaling_factor", &pypowsybl::NadParameters::scaling_factor)
        .def_readwrite("radius_factor", &pypowsybl::NadParameters::radius_factor)
        .def_readwrite("edge_info_displayed",&pypowsybl::NadParameters::edge_info_displayed);

    m.def("write_single_line_diagram_svg", &pypowsybl::writeSingleLineDiagramSvg, "Write single line diagram SVG",
          py::arg("network"), py::arg("container_id"), py::arg("svg_file"), py::arg("metadata_file"), py::arg("sld_parameters"));

    m.def("write_matrix_multi_substation_single_line_diagram_svg", &pypowsybl::writeMatrixMultiSubstationSingleLineDiagramSvg, "Write matrix multi-substation single line diagram SVG",
          py::arg("network"), py::arg("matrix_ids"), py::arg("svg_file"), py::arg("metadata_file"), py::arg("sld_parameters"));

    m.def("get_single_line_diagram_svg", &pypowsybl::getSingleLineDiagramSvg, "Get single line diagram SVG as a string",
          py::arg("network"), py::arg("container_id"));

    m.def("get_single_line_diagram_svg_and_metadata", &pypowsybl::getSingleLineDiagramSvgAndMetadata, "Get single line diagram SVG and its metadata as a list of strings",
          py::arg("network"), py::arg("container_id"), py::arg("sld_parameters"));

    m.def("get_matrix_multi_substation_single_line_diagram_svg_and_metadata", &pypowsybl::getMatrixMultiSubstationSvgAndMetadata, "Get matrix multi-substation single line diagram SVG and its metadata as a list of strings",
          py::arg("network"), py::arg("matrix_ids"), py::arg("sld_parameters"));

    m.def("get_single_line_diagram_component_library_names", &pypowsybl::getSingleLineDiagramComponentLibraryNames, "Get supported component library providers for single line diagram");

    m.def("write_network_area_diagram_svg", &pypowsybl::writeNetworkAreaDiagramSvg, "Write network area diagram SVG",
          py::arg("network"), py::arg("svg_file"), py::arg("metadata_file"), py::arg("voltage_level_ids"),
          py::arg("depth"), py::arg("high_nominal_voltage_bound"), py::arg("low_nominal_voltage_bound"), py::arg("nad_parameters"));

    m.def("get_network_area_diagram_svg", &pypowsybl::getNetworkAreaDiagramSvg, "Get network area diagram SVG as a string",
          py::arg("network"), py::arg("voltage_level_ids"), py::arg("depth"), py::arg("high_nominal_voltage_bound"), py::arg("low_nominal_voltage_bound"), py::arg("nad_parameters"));
          
    m.def("get_network_area_diagram_svg_and_metadata", &pypowsybl::getNetworkAreaDiagramSvgAndMetadata, "Get network area diagram SVG and its metadata as a list of strings",
          py::arg("network"), py::arg("voltage_level_ids"), py::arg("depth"), py::arg("high_nominal_voltage_bound"), py::arg("low_nominal_voltage_bound"), py::arg("nad_parameters"));

    m.def("get_network_area_diagram_displayed_voltage_levels", &pypowsybl::getNetworkAreaDiagramDisplayedVoltageLevels, "Get network area diagram displayed voltage level",
          py::arg("network"), py::arg("voltage_level_ids"), py::arg("depth"));

    m.def("create_security_analysis", &pypowsybl::createSecurityAnalysis, "Create a security analysis");

    m.def("add_contingency", &pypowsybl::addContingency, "Add a contingency to a security analysis or sensitivity analysis",
          py::arg("analysis_context"), py::arg("contingency_id"), py::arg("elements_ids"));

    m.def("add_load_active_power_action", &pypowsybl::addLoadActivePowerAction, "Add a load active power remedial action",
          py::arg("analysis_context"), py::arg("action_id"), py::arg("load_id"), py::arg("is_relative"), py::arg("active_power"));

    m.def("add_load_reactive_power_action", &pypowsybl::addLoadReactivePowerAction, "Add a load reactive power remedial action",
          py::arg("analysis_context"), py::arg("action_id"), py::arg("load_id"), py::arg("is_relative"), py::arg("reactive_power"));

    m.def("add_generator_active_power_action", &pypowsybl::addGeneratorActivePowerAction, "Add a generator active power remedial action",
          py::arg("analysis_context"), py::arg("action_id"), py::arg("generator_id"), py::arg("is_relative"), py::arg("active_power"));

    m.def("add_switch_action", &pypowsybl::addSwitchAction, "Add a switch action",
          py::arg("analysis_context"), py::arg("action_id"), py::arg("switch_id"), py::arg("open"));

    m.def("add_phase_tap_changer_position_action", &pypowsybl::addPhaseTapChangerPositionAction, "Add a phase tap changer position action",
           py::arg("analysis_context"), py::arg("action_id"), py::arg("transformer_id"), py::arg("is_relative"), py::arg("tap_position"), py::arg("side"));

    m.def("add_ratio_tap_changer_position_action", &pypowsybl::addRatioTapChangerPositionAction, "Add a ratio tap changer position action",
           py::arg("analysis_context"), py::arg("action_id"), py::arg("transformer_id"), py::arg("is_relative"), py::arg("tap_position"), py::arg("side"));

    m.def("add_shunt_compensator_position_action", &pypowsybl::addShuntCompensatorPositionAction, "Add a shunt compensator position action",
              py::arg("analysis_context"), py::arg("action_id"), py::arg("shunt_id"), py::arg("section_count"));

    m.def("add_operator_strategy", &pypowsybl::addOperatorStrategy, "Add an operator strategy",
          py::arg("analysis_context"), py::arg("operator_strategy_id"), py::arg("contingency_id"), py::arg("action_ids"),
          py::arg("condition_type"), py::arg("subject_ids"), py::arg("violation_types"));

    py::enum_<pypowsybl::LimitType>(m, "LimitType")
            .value("ACTIVE_POWER", pypowsybl::LimitType::ACTIVE_POWER)
            .value("APPARENT_POWER", pypowsybl::LimitType::APPARENT_POWER)
            .value("CURRENT", pypowsybl::LimitType::CURRENT)
            .value("LOW_VOLTAGE", pypowsybl::LimitType::LOW_VOLTAGE)
            .value("HIGH_VOLTAGE", pypowsybl::LimitType::HIGH_VOLTAGE)
            .value("LOW_VOLTAGE_ANGLE", pypowsybl::LimitType::LOW_VOLTAGE_ANGLE)
            .value("HIGH_VOLTAGE_ANGLE", pypowsybl::LimitType::HIGH_VOLTAGE_ANGLE)
            .value("LOW_SHORT_CIRCUIT_CURRENT", pypowsybl::LimitType::LOW_SHORT_CIRCUIT_CURRENT)
            .value("HIGH_SHORT_CIRCUIT_CURRENT", pypowsybl::LimitType::HIGH_SHORT_CIRCUIT_CURRENT)
            .value("OTHER", pypowsybl::LimitType::OTHER);

    py::enum_<ThreeSide>(m, "Side")
            .value("NONE", ThreeSide::UNDEFINED)
            .value("ONE", ThreeSide::ONE)
            .value("TWO", ThreeSide::TWO)
            .value("THREE", ThreeSide::THREE);

    py::enum_<violation_type>(m, "ViolationType")
            .value("ACTIVE_POWER", violation_type::ACTIVE_POWER)
            .value("APPARENT_POWER", violation_type::APPARENT_POWER)
            .value("CURRENT", violation_type::CURRENT)
            .value("LOW_VOLTAGE", violation_type::LOW_VOLTAGE)
            .value("HIGH_VOLTAGE", violation_type::HIGH_VOLTAGE)
            .value("LOW_SHORT_CIRCUIT_CURRENT", violation_type::LOW_SHORT_CIRCUIT_CURRENT)
            .value("HIGH_SHORT_CIRCUIT_CURRENT", violation_type::HIGH_SHORT_CIRCUIT_CURRENT)
            .value("OTHER", violation_type::OTHER);

    py::enum_<condition_type>(m, "ConditionType")
            .value("TRUE_CONDITION", condition_type::TRUE_CONDITION)
            .value("ALL_VIOLATION_CONDITION", condition_type::ALL_VIOLATION_CONDITION)
            .value("ANY_VIOLATION_CONDITION", condition_type::ANY_VIOLATION_CONDITION)
            .value("AT_LEAST_ONE_VIOLATION_CONDITION", condition_type::AT_LEAST_ONE_VIOLATION_CONDITION);

    py::class_<network_metadata, std::shared_ptr<network_metadata>>(m, "NetworkMetadata")
            .def_property_readonly("id", [](const network_metadata& att) {
                return att.id;
            })
            .def_property_readonly("name", [](const network_metadata& att) {
                return att.name;
            })
            .def_property_readonly("source_format", [](const network_metadata& att) {
                return att.source_format;
            })
            .def_property_readonly("forecast_distance", [](const network_metadata& att) {
                return att.forecast_distance;
            })
            .def_property_readonly("case_date", [](const network_metadata& att) {
                return att.case_date;
            });

    py::class_<limit_violation>(m, "LimitViolation")
            .def_property_readonly("subject_id", [](const limit_violation& v) {
                return v.subject_id;
            })
            .def_property_readonly("subject_name", [](const limit_violation& v) {
                return v.subject_name;
            })
            .def_property_readonly("limit_type", [](const limit_violation& v) {
                return static_cast<pypowsybl::LimitType>(v.limit_type);
            })
            .def_property_readonly("limit", [](const limit_violation& v) {
                return v.limit;
            })
            .def_property_readonly("limit_name", [](const limit_violation& v) {
                return v.limit_name;
            })
            .def_property_readonly("acceptable_duration", [](const limit_violation& v) {
                return v.acceptable_duration;
            })
            .def_property_readonly("limit_reduction", [](const limit_violation& v) {
                return v.limit_reduction;
            })
            .def_property_readonly("value", [](const limit_violation& v) {
                return v.value;
            })
            .def_property_readonly("side", [](const limit_violation& v) {
                return static_cast<ThreeSide>(v.side);
            });

    bindArray<pypowsybl::LimitViolationArray>(m, "LimitViolationArray");

    py::class_<post_contingency_result>(m, "PostContingencyResult")
            .def_property_readonly("contingency_id", [](const post_contingency_result& r) {
                return r.contingency_id;
            })
            .def_property_readonly("status", [](const post_contingency_result& r) {
                return static_cast<pypowsybl::PostContingencyComputationStatus>(r.status);
            })
            .def_property_readonly("limit_violations", [](const post_contingency_result& r) {
                return pypowsybl::LimitViolationArray((array *) & r.limit_violations);
            });
    bindArray<pypowsybl::PostContingencyResultArray>(m, "PostContingencyResultArray");

    py::class_<operator_strategy_result>(m, "OperatorStrategyResult")
            .def_property_readonly("operator_strategy_id", [](const operator_strategy_result& r) {
                return r.operator_strategy_id;
            })
            .def_property_readonly("status", [](const operator_strategy_result& r) {
                return static_cast<pypowsybl::PostContingencyComputationStatus>(r.status);
            })
            .def_property_readonly("limit_violations", [](const operator_strategy_result& r) {
                return pypowsybl::LimitViolationArray((array *) & r.limit_violations);
            });
    bindArray<pypowsybl::OperatorStrategyResultArray>(m, "OperatorStrategyResultArray");

    py::class_<pre_contingency_result>(m, "PreContingencyResult")
            .def_property_readonly("status", [](const pre_contingency_result& r) {
                return static_cast<pypowsybl::LoadFlowComponentStatus>(r.status);
            })
            .def_property_readonly("limit_violations", [](const pre_contingency_result& r) {
                return pypowsybl::LimitViolationArray((array *) & r.limit_violations);
            });

    m.def("run_security_analysis", &pypowsybl::runSecurityAnalysis, "Run a security analysis", py::call_guard<py::gil_scoped_release>(),
          py::arg("security_analysis_context"), py::arg("network"), py::arg("parameters"),
          py::arg("provider"), py::arg("dc"), py::arg("report_node"));

    m.def("create_sensitivity_analysis", &pypowsybl::createSensitivityAnalysis, "Create run_sea sensitivity analysis");

    py::class_<::zone>(m, "Zone")
            .def(py::init([](const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys) {
                return pypowsybl::createZone(id, injectionsIds, injectionsShiftKeys);
            }), py::arg("id"), py::arg("injections_ids"), py::arg("injections_shift_keys"));

    m.def("set_zones", &pypowsybl::setZones, "Add zones to sensitivity analysis",
          py::arg("sensitivity_analysis_context"), py::arg("zones"));

    m.def("add_factor_matrix", &pypowsybl::addFactorMatrix, "Add a factor matrix to a sensitivity analysis",
          py::arg("sensitivity_analysis_context"), py::arg("matrix_id"), py::arg("branches_ids"), py::arg("variables_ids"),
          py::arg("contingencies_ids"), py::arg("contingency_context_type"), py::arg("sensitivity_function_type"),
          py::arg("sensitivity_variable_type"));

    m.def("run_sensitivity_analysis", &pypowsybl::runSensitivityAnalysis, "Run a sensitivity analysis", py::call_guard<py::gil_scoped_release>(),
          py::arg("sensitivity_analysis_context"), py::arg("network"), py::arg("dc"), py::arg("parameters"), py::arg("provider"), py::arg("report_node"));

    py::class_<matrix>(m, "Matrix", py::buffer_protocol())
            .def_buffer([](matrix& m) -> py::buffer_info {
                return py::buffer_info(m.values,
                                       sizeof(double),
                                       py::format_descriptor<double>::format(),
                                       2,
                                       { m.row_count, m.column_count },
                                       { sizeof(double) * m.column_count, sizeof(double) });
            });

    m.def("get_sensitivity_matrix", &pypowsybl::getSensitivityMatrix, "Get sensitivity analysis result matrix for a given contingency",
              py::arg("sensitivity_analysis_result_context"), py::arg("matrix_id"), py::arg("contingency_id"));

    m.def("get_reference_matrix", &pypowsybl::getReferenceMatrix, "Get sensitivity analysis result reference matrix for a given contingency",
          py::arg("sensitivity_analysis_result_context"), py::arg("matrix_id"), py::arg("contingency_id"));

    py::class_<series>(m, "Series")
            .def_property_readonly("name", [](const series& s) {
                return s.name;
            })
            .def_property_readonly("index", [](const series& s) {
                return (bool) s.index;
            })
            .def_property_readonly("data", [](const series& s) -> py::object {
                switch(s.type) {
                    case 0:
                        return py::cast(pypowsybl::toVector<std::string>((array *) & s.data));
                    case 1:
                        return seriesAsNumpyArray<double>(s);
                    case 2:
                        return seriesAsNumpyArray<int>(s);
                    case 3:
                        return seriesAsNumpyArray<bool>(s);
                    default:
                        throw pypowsybl::PyPowsyblError("Series type not supported: " + std::to_string(s.type));
                }
            });
    bindArray<pypowsybl::SeriesArray>(m, "SeriesArray");

    py::class_<pypowsybl::SeriesMetadata>(m, "SeriesMetadata", "Metadata about one series")
            .def(py::init<const std::string&, int, bool, bool, bool>())
            .def_property_readonly("name", &pypowsybl::SeriesMetadata::name, "Name of this series.")
            .def_property_readonly("type", &pypowsybl::SeriesMetadata::type)
            .def_property_readonly("is_index", &pypowsybl::SeriesMetadata::isIndex)
            .def_property_readonly("is_modifiable", &pypowsybl::SeriesMetadata::isModifiable)
            .def_property_readonly("is_default", &pypowsybl::SeriesMetadata::isDefault);

    m.def("get_network_elements_dataframe_metadata", &pypowsybl::getNetworkDataframeMetadata, "Get dataframe metadata for a given network element type",
          py::arg("element_type"));

    m.def("get_network_elements_creation_dataframes_metadata", &pypowsybl::getNetworkElementCreationDataframesMetadata, "Get network elements creation tables metadata",
        py::arg("element_type"));

    m.def("create_network_elements_series_array", &pypowsybl::createNetworkElementsSeriesArray, "Create a network elements series array for a given element type",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("element_type"), py::arg("filter_attributes_type"), py::arg("attributes"), py::arg("array"), py::arg("per_unit"), py::arg("nominal_apparent_power"));

    m.def("create_network_elements_extension_series_array", &pypowsybl::createNetworkElementsExtensionSeriesArray, "Create a network elements extensions series array for a given extension name",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("extension_name"), py::arg("table_name"));

    m.def("get_extensions_names", &pypowsybl::getExtensionsNames, "get all the extensions names available");

    m.def("get_extensions_information", &pypowsybl::getExtensionsInformation, "get more information about all extensions");

    m.def("update_network_elements_with_series", pypowsybl::updateNetworkElementsWithSeries, "Update network elements for a given element type with a series",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("dataframe"), py::arg("element_type"), py::arg("per_unit"), py::arg("nominal_apparent_power"));

    m.def("create_dataframe", ::createDataframe, "create dataframe to update or create new elements", py::arg("columns_values"), py::arg("columns_names"), py::arg("columns_types"),
          py::arg("is_index"));

    m.def("get_network_metadata", &pypowsybl::getNetworkMetadata, "get attributes", py::arg("network"));
    m.def("get_working_variant_id", &pypowsybl::getWorkingVariantId, "get the current working variant id", py::arg("network"));
    m.def("set_working_variant", &pypowsybl::setWorkingVariant, "set working variant", py::arg("network"), py::arg("variant"));
    m.def("remove_variant", &pypowsybl::removeVariant, "remove a variant", py::arg("network"), py::arg("variant"));
    m.def("clone_variant", &pypowsybl::cloneVariant, "clone a variant", py::arg("network"), py::arg("src"), py::arg("variant"), py::arg("may_overwrite"));
    m.def("get_variant_ids", &pypowsybl::getVariantsIds, "get all variant ids from a network", py::arg("network"));
    m.def("add_monitored_elements", &pypowsybl::addMonitoredElements, "Add monitors to get specific results on network after security analysis process", py::arg("security_analysis_context"),
          py::arg("contingency_context_type"), py::arg("branch_ids"), py::arg("voltage_level_ids"), py::arg("three_windings_transformer_ids"),
          py::arg("contingency_ids"));

    py::enum_<contingency_context_type>(m, "ContingencyContextType")
            .value("ALL", contingency_context_type::ALL)
            .value("NONE", contingency_context_type::NONE)
            .value("SPECIFIC", contingency_context_type::SPECIFIC)
            .value("ONLY_CONTINGENCIES", contingency_context_type::ONLY_CONTINGENCIES);

    py::enum_<sensitivity_function_type>(m, "SensitivityFunctionType")
            .value("BRANCH_ACTIVE_POWER_1", sensitivity_function_type::BRANCH_ACTIVE_POWER_1)
            .value("BRANCH_CURRENT_1",sensitivity_function_type::BRANCH_CURRENT_1)
            .value("BRANCH_REACTIVE_POWER_1",sensitivity_function_type::BRANCH_REACTIVE_POWER_1)
            .value("BRANCH_ACTIVE_POWER_2",sensitivity_function_type::BRANCH_ACTIVE_POWER_2)
            .value("BRANCH_CURRENT_2",sensitivity_function_type::BRANCH_CURRENT_2)
            .value("BRANCH_REACTIVE_POWER_2",sensitivity_function_type::BRANCH_REACTIVE_POWER_2)
            .value("BRANCH_ACTIVE_POWER_3",sensitivity_function_type::BRANCH_ACTIVE_POWER_3)
            .value("BRANCH_CURRENT_3",sensitivity_function_type::BRANCH_CURRENT_3)
            .value("BRANCH_REACTIVE_POWER_3",sensitivity_function_type::BRANCH_REACTIVE_POWER_3)
            .value("BUS_REACTIVE_POWER",sensitivity_function_type::BUS_REACTIVE_POWER)
            .value("BUS_VOLTAGE",sensitivity_function_type::BUS_VOLTAGE);

    py::enum_<sensitivity_variable_type>(m, "SensitivityVariableType")
            .value("AUTO_DETECT", sensitivity_variable_type::AUTO_DETECT)
            .value("INJECTION_ACTIVE_POWER", sensitivity_variable_type::INJECTION_ACTIVE_POWER)
            .value("INJECTION_REACTIVE_POWER", sensitivity_variable_type::INJECTION_REACTIVE_POWER)
            .value("TRANSFORMER_PHASE", sensitivity_variable_type::TRANSFORMER_PHASE)
            .value("BUS_TARGET_VOLTAGE", sensitivity_variable_type::BUS_TARGET_VOLTAGE)
            .value("HVDC_LINE_ACTIVE_POWER", sensitivity_variable_type::HVDC_LINE_ACTIVE_POWER)
            .value("TRANSFORMER_PHASE_1", sensitivity_variable_type::TRANSFORMER_PHASE_1)
            .value("TRANSFORMER_PHASE_2", sensitivity_variable_type::TRANSFORMER_PHASE_2)
            .value("TRANSFORMER_PHASE_3", sensitivity_variable_type::TRANSFORMER_PHASE_3);

    m.def("get_post_contingency_results", &pypowsybl::getPostContingencyResults, "get post contingency results of a security analysis", py::arg("result"));
    m.def("get_pre_contingency_result", &pypowsybl::getPreContingencyResult, "get pre contingency result of a security analysis", py::arg("result"));
    m.def("get_operator_strategy_results", &pypowsybl::getOperatorStrategyResults, "get operator strategy results of a security analysis", py::arg("result"));
    m.def("get_node_breaker_view_nodes", &pypowsybl::getNodeBreakerViewNodes, "get all nodes for a voltage level", py::arg("network"), py::arg("voltage_level"));
    m.def("get_node_breaker_view_internal_connections", &pypowsybl::getNodeBreakerViewInternalConnections,
    "get all internal connections for a voltage level", py::arg("network"), py::arg("voltage_level"));
    m.def("get_node_breaker_view_switches", &pypowsybl::getNodeBreakerViewSwitches, "get all switches for a voltage level in bus breaker view", py::arg("network"), py::arg("voltage_level"));
    m.def("get_bus_breaker_view_elements", &pypowsybl::getBusBreakerViewElements, "get all elements for a voltage level in bus breaker view", py::arg("network"), py::arg("voltage_level"));
    m.def("get_bus_breaker_view_buses", &pypowsybl::getBusBreakerViewBuses,
    "get all buses for a voltage level in bus breaker view", py::arg("network"), py::arg("voltage_level"));
    m.def("get_bus_breaker_view_switches", &pypowsybl::getBusBreakerViewSwitches, "get all switches for a voltage level", py::arg("network"), py::arg("voltage_level"));
    m.def("get_limit_violations", &pypowsybl::getLimitViolations, "get limit violations of a security analysis", py::arg("result"));

    m.def("get_branch_results", &pypowsybl::getBranchResults, "create a table with all branch results computed after security analysis",
          py::arg("result"));
    m.def("get_bus_results", &pypowsybl::getBusResults, "create a table with all bus results computed after security analysis",
          py::arg("result"));
    m.def("get_three_windings_transformer_results", &pypowsybl::getThreeWindingsTransformerResults,
          "create a table with all three windings transformer results computed after security analysis", py::arg("result"));
    m.def("create_element", ::createElementBind, "create a new element on the network", py::arg("network"),  py::arg("dataframes"),  py::arg("elementType"));

    py::enum_<validation_level_type>(m, "ValidationLevel")
        .value("EQUIPMENT", validation_level_type::EQUIPMENT)
        .value("STEADY_STATE_HYPOTHESIS", validation_level_type::STEADY_STATE_HYPOTHESIS)
        .export_values();

    m.def("get_validation_level", &pypowsybl::getValidationLevel, "get the validation level", py::arg("network"));

    m.def("validate", &pypowsybl::validate, "validate", py::arg("network"));

    m.def("set_min_validation_level", pypowsybl::setMinValidationLevel, "set minimum validation level",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("validation_level"));
    m.def("set_logger", &setLogger, "Setup the logger", py::arg("logger"));
    m.def("get_logger", &getLogger, "Retrieve the logger");
    m.def("remove_elements", &pypowsybl::removeNetworkElements, "delete elements on the network", py::arg("network"),  py::arg("elementIds"));
    m.def("add_network_element_properties", &pypowsybl::addNetworkElementProperties, "add properties on network elements", py::arg("network"), py::arg("dataframe"));
    m.def("remove_network_element_properties", &pypowsybl::removeNetworkElementProperties, "remove properties on network elements", py::arg("network"), py::arg("ids"), py::arg("properties"));
    m.def("get_loadflow_provider_parameters_names", &pypowsybl::getLoadFlowProviderParametersNames, "get provider parameters for a loadflow provider", py::arg("provider"));
    m.def("create_loadflow_provider_parameters_series_array", &pypowsybl::createLoadFlowProviderParametersSeriesArray, "Create a parameters series array for a given loadflow provider",
          py::arg("provider"));
    m.def("get_security_analysis_provider_parameters_names", &pypowsybl::getSecurityAnalysisProviderParametersNames, "get provider parameters for a security analysis provider", py::arg("provider"));
    m.def("get_sensitivity_analysis_provider_parameters_names", &pypowsybl::getSensitivityAnalysisProviderParametersNames, "get provider parameters for a sensitivity analysis provider", py::arg("provider"));
    m.def("update_extensions", pypowsybl::updateNetworkElementsExtensionsWithSeries, "Update extensions of network elements for a given element type with a series",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("name"), py::arg("table_name"), py::arg("dataframe"));
    m.def("remove_extensions", &pypowsybl::removeExtensions, "Remove extensions from network elements",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("name"), py::arg("ids"));
    m.def("get_network_extensions_dataframe_metadata", &pypowsybl::getNetworkExtensionsDataframeMetadata, "Get dataframe metadata for a given network element extension",
          py::arg("name"), py::arg("table_name"));
    m.def("get_network_extensions_creation_dataframes_metadata", &pypowsybl::getNetworkExtensionsCreationDataframesMetadata, "Get network extension creation tables metadata for a given network element extension",
          py::arg("name"));
    m.def("create_extensions", ::createExtensionsBind, "create extensions of network elements given the extension name",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"),  py::arg("dataframes"),  py::arg("name"));
    m.def("create_report_node", &pypowsybl::createReportNode, "Create a report node", py::arg("task_key"), py::arg("default_name"));
    m.def("print_report", &pypowsybl::printReport, "Print a report", py::arg("report_node"));
	m.def("json_report", &pypowsybl::jsonReport, "Print a report in json format", py::arg("report_node"));
    m.def("create_glsk_document", &pypowsybl::createGLSKdocument, "Create a glsk importer.", py::arg("filename"));

    m.def("get_glsk_injection_keys", &pypowsybl::getGLSKinjectionkeys, "Get glsk injection keys available for a country", py::arg("network"), py::arg("importer"), py::arg("country"), py::arg("instant"));

    m.def("get_glsk_countries", &pypowsybl::getGLSKcountries, "Get glsk countries", py::arg("importer"));

    m.def("get_glsk_factors", &pypowsybl::getGLSKInjectionFactors, "Get glsk factors", py::arg("network"), py::arg("importer"), py::arg("country"), py::arg("instant"));

    m.def("get_glsk_factors_start_timestamp", &pypowsybl::getInjectionFactorStartTimestamp, "Get glsk start timestamp", py::arg("importer"));

    m.def("get_glsk_factors_end_timestamp", &pypowsybl::getInjectionFactorEndTimestamp, "Get glsk end timestamp", py::arg("importer"));

    m.def("create_flow_decomposition", &pypowsybl::createFlowDecomposition, "Create a security analysis");

    m.def("add_contingency_for_flow_decomposition", &pypowsybl::addContingencyForFlowDecomposition, "Add a contingency for flow decomposition",
          py::arg("flow_decomposition_context"), py::arg("contingency_id"), py::arg("elements_ids"));

    m.def("add_precontingency_monitored_elements_for_flow_decomposition", &pypowsybl::addPrecontingencyMonitoredElementsForFlowDecomposition, "Add elements before contingency to be monitored for a flow decomposition",
          py::arg("flow_decomposition_context"), py::arg("branch_ids"));

    m.def("add_postcontingency_monitored_elements_for_flow_decomposition", &pypowsybl::addPostcontingencyMonitoredElementsForFlowDecomposition, "Add elements after contingency to be monitored for a flow decomposition",
          py::arg("flow_decomposition_context"), py::arg("branch_ids"), py::arg("contingency_ids"));

    m.def("add_additional_xnec_provider_for_flow_decomposition", &pypowsybl::addAdditionalXnecProviderForFlowDecomposition, "Add an additional default xnec provider for a flow decomposition",
          py::arg("flow_decomposition_context"), py::arg("default_xnec_provider"));

    m.def("run_flow_decomposition", &pypowsybl::runFlowDecomposition, "Run flow decomposition on a network",
          py::call_guard<py::gil_scoped_release>(), py::arg("flow_decomposition_context"), py::arg("network"), py::arg("flow_decomposition_parameters"), py::arg("loadflow_parameters"));

    py::enum_<pypowsybl::RescaleMode>(m, "RescaleMode")
            .value("NONE", pypowsybl::RescaleMode::NONE)
            .value("ACER_METHODOLOGY", pypowsybl::RescaleMode::ACER_METHODOLOGY)
            .value("PROPORTIONAL", pypowsybl::RescaleMode::PROPORTIONAL)
            .value("MAX_CURRENT_OVERLOAD", pypowsybl::RescaleMode::MAX_CURRENT_OVERLOAD);

    py::class_<pypowsybl::FlowDecompositionParameters>(m, "FlowDecompositionParameters")
                .def(py::init(&pypowsybl::createFlowDecompositionParameters))
                .def_readwrite("enable_losses_compensation", &pypowsybl::FlowDecompositionParameters::enable_losses_compensation)
                .def_readwrite("losses_compensation_epsilon", &pypowsybl::FlowDecompositionParameters::losses_compensation_epsilon)
                .def_readwrite("sensitivity_epsilon", &pypowsybl::FlowDecompositionParameters::sensitivity_epsilon)
                .def_readwrite("rescale_mode", &pypowsybl::FlowDecompositionParameters::rescale_mode)
                .def_readwrite("dc_fallback_enabled_after_ac_divergence", &pypowsybl::FlowDecompositionParameters::dc_fallback_enabled_after_ac_divergence)
                .def_readwrite("sensitivity_variable_batch_size", &pypowsybl::FlowDecompositionParameters::sensitivity_variable_batch_size);

    py::enum_<pypowsybl::DefaultXnecProvider>(m, "DefaultXnecProvider", "Define the default xnec providers")
            .value("GT_5_PERC_ZONE_TO_ZONE_PTDF", pypowsybl::DefaultXnecProvider::GT_5_PERC_ZONE_TO_ZONE_PTDF, "Select branches on base case with greater than 5 perc zone to zone PTDF or that is an interconnection.")
            .value("ALL_BRANCHES", pypowsybl::DefaultXnecProvider::ALL_BRANCHES, "Select all branches in a network.")
            .value("INTERCONNECTIONS", pypowsybl::DefaultXnecProvider::INTERCONNECTIONS, "Select all the interconnections in a network.");

    m.def("get_connectables_order_positions", &pypowsybl::getConnectablesOrderPositions, "Get connectables order positions", py::arg("network"), py::arg("voltage_level_id"));

    m.def("get_unused_order_positions", &pypowsybl::getUnusedConnectableOrderPositions, "Get unused order positions before or after", py::arg("network"), py::arg("busbar_section_id"), py::arg("before_or_after"));

    m.def("remove_aliases", &pypowsybl::removeAliases, "remove specified aliases on a network", py::arg("network"), py::arg("dataframe"));

    m.def("remove_internal_connections", &pypowsybl::removeInternalConnections, "remove specified internal connections", py::arg("network"), py::arg("dataframe"));

    m.def("close", &pypowsybl::closePypowsybl, "Closes pypowsybl module.");

    m.def("remove_elements_modification", &pypowsybl::removeElementsModification, "remove a list of feeder bays", py::arg("network"), py::arg("connectable_ids"), py::arg("extraDataDf"), py::arg("remove_modification_type"), py::arg("raise_exception"), py::arg("report_node"));

    dynamicSimulationBindings(m);
    voltageInitializerBinding(m);

    m.def("get_network_modification_metadata", &pypowsybl::getModificationMetadata, "Get network modification metadata", py::arg("network_modification_type"));

    m.def("get_network_modification_metadata_with_element_type", &pypowsybl::getModificationMetadataWithElementType, "Get network modification metadata with element type", py::arg("network_modification_type"), py::arg("element_type"));

    m.def("create_network_modification", ::createNetworkModificationBind, "Create and apply network modification", py::arg("network"), py::arg("dataframe"), py::arg("network_modification_type"), py::arg("raise_exception"), py::arg("report_node"));

    py::enum_<pypowsybl::InitialVoltageProfileMode>(m, "InitialVoltageProfileMode", "configure the voltage profile to use for the short-circuit study")
            .value("NOMINAL", pypowsybl::InitialVoltageProfileMode::NOMINAL,
                   "Nominal voltages are used for the calculation.")
            .value("PREVIOUS_VALUE", pypowsybl::InitialVoltageProfileMode::PREVIOUS_VALUE,
                   "Voltage profile calculated by the load flow is used for the calculation.");

    py::enum_<pypowsybl::ShortCircuitStudyType>(m, "ShortCircuitStudyType", "Indicates the type of short circuit study")
            .value("SUB_TRANSIENT", pypowsybl::ShortCircuitStudyType::SUB_TRANSIENT,
                   "It is the first stage of the short circuit, right when the fault happens. The subtransient reactance of generators will be used.")
            .value("TRANSIENT", pypowsybl::ShortCircuitStudyType::TRANSIENT,
                   "The second stage of the short circuit, before the system stabilizes. The transient reactance of generators will be used.")
            .value("STEADY_STATE", pypowsybl::ShortCircuitStudyType::STEADY_STATE,
                   "The last stage of the short circuit, once all transient effects are gone.");

    py::class_<pypowsybl::ShortCircuitAnalysisParameters>(m, "ShortCircuitAnalysisParameters")
        .def(py::init(&pypowsybl::createShortCircuitAnalysisParameters))
        .def_readwrite("with_voltage_result", &pypowsybl::ShortCircuitAnalysisParameters::with_voltage_result)
        .def_readwrite("with_feeder_result", &pypowsybl::ShortCircuitAnalysisParameters::with_feeder_result)
        .def_readwrite("with_limit_violations", &pypowsybl::ShortCircuitAnalysisParameters::with_limit_violations)
        .def_readwrite("study_type", &pypowsybl::ShortCircuitAnalysisParameters::study_type)
        .def_readwrite("with_fortescue_result", &pypowsybl::ShortCircuitAnalysisParameters::with_fortescue_result)
        .def_readwrite("min_voltage_drop_proportional_threshold", &pypowsybl::ShortCircuitAnalysisParameters::min_voltage_drop_proportional_threshold)
        .def_readwrite("initial_voltage_profile_mode", &pypowsybl::ShortCircuitAnalysisParameters::initial_voltage_profile_mode)
        .def_readwrite("provider_parameters_keys", &pypowsybl::ShortCircuitAnalysisParameters::provider_parameters_keys)
        .def_readwrite("provider_parameters_values", &pypowsybl::ShortCircuitAnalysisParameters::provider_parameters_values);

    m.def("set_default_shortcircuit_analysis_provider", &pypowsybl::setDefaultShortCircuitAnalysisProvider, "Set default short-circuit analysis provider", py::arg("provider"));
    m.def("get_default_shortcircuit_analysis_provider", &pypowsybl::getDefaultShortCircuitAnalysisProvider, "Get default short-circuit analysis provider");
    m.def("get_shortcircuit_provider_names", &pypowsybl::getShortCircuitAnalysisProviderNames, "Get supported short-circuit analysis providers");
    m.def("get_shortcircuit_provider_parameters_names", &pypowsybl::getShortCircuitAnalysisProviderParametersNames, "get provider parameters for a short-circuit analysis provider", py::arg("provider"));
    m.def("create_shortcircuit_analysis", &pypowsybl::createShortCircuitAnalysis, "Create a short-circuit analysis");
    m.def("run_shortcircuit_analysis", &pypowsybl::runShortCircuitAnalysis, "Run a short-circuit analysis", py::call_guard<py::gil_scoped_release>(),
          py::arg("shortcircuit_analysis_context"), py::arg("network"), py::arg("parameters"),
          py::arg("provider"), py::arg("report_node"));

    m.def("get_faults_dataframes_metadata", &pypowsybl::getFaultsMetaData, "Get faults metadata");
    m.def("set_faults", &pypowsybl::setFaults, "define faults for a short-circuit analysis", py::arg("analysisContext"),  py::arg("dataframe"));
    m.def("get_fault_results", &pypowsybl::getFaultResults, "gets the fault results computed after short-circuit analysis",
          py::arg("result"), py::arg("with_fortescue_result"));
    m.def("get_feeder_results", &pypowsybl::getFeederResults, "gets the feeder results computed after short-circuit analysis",
          py::arg("result"), py::arg("with_fortescue_result"));
    m.def("get_short_circuit_limit_violations", &pypowsybl::getShortCircuitLimitViolations, "gets the limit violations of a short-circuit analysis", py::arg("result"));
    m.def("get_short_circuit_bus_results", &pypowsybl::getShortCircuitBusResults, "gets the bus results of a short-circuit analysis", py::arg("result"), py::arg("with_fortescue_result"));

    py::enum_<Grid2opStringValueType>(m, "Grid2opStringValueType")
            .value("VOLTAGE_LEVEL_NAME", Grid2opStringValueType::VOLTAGE_LEVEL_NAME)
            .value("LOAD_NAME", Grid2opStringValueType::LOAD_NAME)
            .value("GENERATOR_NAME", Grid2opStringValueType::GENERATOR_NAME)
            .value("SHUNT_NAME", Grid2opStringValueType::SHUNT_NAME)
            .value("BRANCH_NAME", Grid2opStringValueType::BRANCH_NAME);

    py::enum_<Grid2opIntegerValueType>(m, "Grid2opIntegerValueType")
            .value("LOAD_VOLTAGE_LEVEL_NUM", Grid2opIntegerValueType::LOAD_VOLTAGE_LEVEL_NUM)
            .value("GENERATOR_VOLTAGE_LEVEL_NUM", Grid2opIntegerValueType::GENERATOR_VOLTAGE_LEVEL_NUM)
            .value("SHUNT_VOLTAGE_LEVEL_NUM", Grid2opIntegerValueType::SHUNT_VOLTAGE_LEVEL_NUM)
            .value("BRANCH_VOLTAGE_LEVEL_NUM_1", Grid2opIntegerValueType::BRANCH_VOLTAGE_LEVEL_NUM_1)
            .value("BRANCH_VOLTAGE_LEVEL_NUM_2", Grid2opIntegerValueType::BRANCH_VOLTAGE_LEVEL_NUM_2)
            .value("TOPO_VECT", Grid2opIntegerValueType::TOPO_VECT);

    py::enum_<Grid2opDoubleValueType>(m, "Grid2opDoubleValueType")
            .value("LOAD_P", Grid2opDoubleValueType::LOAD_P)
            .value("LOAD_Q", Grid2opDoubleValueType::LOAD_Q)
            .value("LOAD_V", Grid2opDoubleValueType::LOAD_V)
            .value("GENERATOR_P", Grid2opDoubleValueType::GENERATOR_P)
            .value("GENERATOR_Q", Grid2opDoubleValueType::GENERATOR_Q)
            .value("GENERATOR_V", Grid2opDoubleValueType::GENERATOR_V)
            .value("SHUNT_P", Grid2opDoubleValueType::SHUNT_P)
            .value("SHUNT_Q", Grid2opDoubleValueType::SHUNT_Q)
            .value("SHUNT_V", Grid2opDoubleValueType::SHUNT_V)
            .value("BRANCH_P1", Grid2opDoubleValueType::BRANCH_P1)
            .value("BRANCH_P2", Grid2opDoubleValueType::BRANCH_P2)
            .value("BRANCH_Q1", Grid2opDoubleValueType::BRANCH_Q1)
            .value("BRANCH_Q2", Grid2opDoubleValueType::BRANCH_Q2)
            .value("BRANCH_V1", Grid2opDoubleValueType::BRANCH_V1)
            .value("BRANCH_V2", Grid2opDoubleValueType::BRANCH_V2)
            .value("BRANCH_I1", Grid2opDoubleValueType::BRANCH_I1)
            .value("BRANCH_I2", Grid2opDoubleValueType::BRANCH_I2);

    py::enum_<Grid2opUpdateDoubleValueType>(m, "Grid2opUpdateDoubleValueType")
            .value("UPDATE_LOAD_P", Grid2opUpdateDoubleValueType::UPDATE_LOAD_P)
            .value("UPDATE_LOAD_Q", Grid2opUpdateDoubleValueType::UPDATE_LOAD_Q)
            .value("UPDATE_GENERATOR_P", Grid2opUpdateDoubleValueType::UPDATE_GENERATOR_P)
            .value("UPDATE_GENERATOR_V", Grid2opUpdateDoubleValueType::UPDATE_GENERATOR_V);

    py::enum_<Grid2opUpdateIntegerValueType>(m, "Grid2opUpdateIntegerValueType")
            .value("UPDATE_LOAD_BUS", Grid2opUpdateIntegerValueType::UPDATE_LOAD_BUS)
            .value("UPDATE_GENERATOR_BUS", Grid2opUpdateIntegerValueType::UPDATE_GENERATOR_BUS)
            .value("UPDATE_BRANCH_BUS1", Grid2opUpdateIntegerValueType::UPDATE_BRANCH_BUS1)
            .value("UPDATE_BRANCH_BUS2", Grid2opUpdateIntegerValueType::UPDATE_BRANCH_BUS1);

    m.def("create_grid2op_backend", &pypowsybl::createGrid2opBackend, "Create a Grid2op backend", py::arg("network"), py::arg("buses_per_voltage_level"), py::arg("connect_all_elements_to_first_bus"));
    m.def("free_grid2op_backend", &pypowsybl::freeGrid2opBackend, "Free a Grid2op backend", py::arg("backend"));
    m.def("get_grid2op_string_value", &pypowsybl::getGrid2opStringValue, "From a Grid2op backend get a string value vector", py::arg("backend"), py::arg("value_type"));
    m.def("get_grid2op_integer_value", &::pyGetGrid2opIntegerValue, "From a Grid2op backend get a integer value vector", py::arg("backend"), py::arg("value_type"));
    m.def("get_grid2op_double_value", &::pyGetGrid2opDoubleValue, "From a Grid2op backend get a double value vector", py::arg("backend"), py::arg("value_type"));
    m.def("update_grid2op_double_value", &::pyUpdateGrid2opDoubleValue, "From a Grid2op backend update a double value vector", py::arg("backend"), py::arg("value_type"), py::arg("value"), py::arg("changed"));
    m.def("update_grid2op_integer_value", &::pyUpdateGrid2opIntegerValue, "From a Grid2op backend update a integer value vector", py::arg("backend"), py::arg("value_type"), py::arg("value"), py::arg("changed"));
}

void setLogLevelFromPythonLogger(pypowsybl::GraalVmGuard* guard, exception_handler* exc) {
    py::object logger = CppToPythonLogger::get()->getLogger();
    if (!logger.is_none()) {
        py::gil_scoped_acquire acquire;
        py::object level = logger.attr("level");
        ::setLogLevel(guard->thread(), level.cast<int>(), exc);
     }
}

pypowsybl::JavaHandle loadNetworkFromBinaryBuffersPython(std::vector<py::buffer> byteBuffers, const std::map<std::string, std::string>& parameters, const std::vector<std::string>& postProcessors, pypowsybl::JavaHandle* reportNode) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    pypowsybl::ToCharPtrPtr parameterNamesPtr(parameterNames);
    pypowsybl::ToCharPtrPtr parameterValuesPtr(parameterValues);
    pypowsybl::ToCharPtrPtr postProcessorsPtr(postProcessors);

    char** dataPtrs = new char*[byteBuffers.size()];
    int* dataSizes = new int[byteBuffers.size()];
    for(int i=0; i < byteBuffers.size(); ++i) {
        py::buffer_info info = byteBuffers[i].request();
        dataPtrs[i] = static_cast<char*>(info.ptr);
        dataSizes[i] = info.size;
    }

    pypowsybl::JavaHandle networkHandle = pypowsybl::PowsyblCaller::get()->callJava<pypowsybl::JavaHandle>(::loadNetworkFromBinaryBuffers, dataPtrs, dataSizes, byteBuffers.size(),
                           parameterNamesPtr.get(), parameterNames.size(), parameterValuesPtr.get(), parameterValues.size(),
                           postProcessorsPtr.get(), postProcessors.size(), (reportNode == nullptr) ? nullptr : *reportNode);
    delete[] dataPtrs;
    delete[] dataSizes;
    return networkHandle;
}

py::bytes saveNetworkToBinaryBufferPython(const pypowsybl::JavaHandle& network, const std::string& format, const std::map<std::string, std::string>& parameters, pypowsybl::JavaHandle* reportNode) {
    std::vector<std::string> parameterNames;
    std::vector<std::string> parameterValues;
    parameterNames.reserve(parameters.size());
    parameterValues.reserve(parameters.size());
    for (std::pair<std::string, std::string> p : parameters) {
        parameterNames.push_back(p.first);
        parameterValues.push_back(p.second);
    }
    pypowsybl::ToCharPtrPtr parameterNamesPtr(parameterNames);
    pypowsybl::ToCharPtrPtr parameterValuesPtr(parameterValues);
    array* byteArray = pypowsybl::PowsyblCaller::get()->callJava<array*>(::saveNetworkToBinaryBuffer, network, (char*) format.data(), parameterNamesPtr.get(), parameterNames.size(),
                     parameterValuesPtr.get(), parameterValues.size(), reportNode == nullptr ? nullptr : *reportNode);
    py::gil_scoped_acquire acquire;
    py::bytes bytes((char*) byteArray->ptr, byteArray->length);
    pypowsybl::PowsyblCaller::get()->callJava<>(::freeNetworkBinaryBuffer, byteArray);
    return bytes;
}
