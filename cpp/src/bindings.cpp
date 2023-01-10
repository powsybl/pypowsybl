/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>
#include "pypowsybl.h"
#include "pylogging.h"

namespace py = pybind11;

template<typename T>
void bindArray(py::module_& m, const std::string& className) {
    py::class_<T>(m, className.c_str())
            .def("__len__", [](const T& a) {
                return a.length();
            })
            .def("__iter__", [](T& a) {
                return py::make_iterator(a.begin(), a.end());
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

void createElement(pypowsybl::JavaHandle network, const std::vector<dataframe*>& dataframes, element_type elementType) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::createElement(network, dataframeArray.get(), elementType);
}

void createFeederBay(pypowsybl::JavaHandle network, bool throwException, pypowsybl::JavaHandle* reporter, const std::vector<dataframe*>& dataframes, element_type elementType) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::createFeederBay(network, throwException, reporter, dataframeArray.get(), elementType);
}

void createExtensions(pypowsybl::JavaHandle network, const std::vector<dataframe*>& dataframes, std::string& name) {
    std::shared_ptr<dataframe_array> dataframeArray = ::createDataframeArray(dataframes);
    pypowsybl::createExtensions(network, dataframeArray.get(), name);
}

template<typename T>
py::array seriesAsNumpyArray(const series& series) {
	//Last argument is to bind lifetime of series to the returned array
    return py::array(py::dtype::of<T>(), series.data.length, series.data.ptr, py::cast(series));
}

PYBIND11_MODULE(_pypowsybl, m) {
    pypowsybl::init();

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

    m.def("update_connectable_status", &pypowsybl::updateConnectableStatus, "Update a connectable (branch or injection) status");

    py::enum_<element_type>(m, "ElementType")
            .value("BUS", element_type::BUS)
            .value("LINE", element_type::LINE)
            .value("TWO_WINDINGS_TRANSFORMER", element_type::TWO_WINDINGS_TRANSFORMER)
            .value("THREE_WINDINGS_TRANSFORMER", element_type::THREE_WINDINGS_TRANSFORMER)
            .value("GENERATOR", element_type::GENERATOR)
            .value("LOAD", element_type::LOAD)
            .value("BATTERY", element_type::BATTERY)
            .value("SHUNT_COMPENSATOR", element_type::SHUNT_COMPENSATOR)
            .value("NON_LINEAR_SHUNT_COMPENSATOR_SECTION", element_type::NON_LINEAR_SHUNT_COMPENSATOR_SECTION)
            .value("LINEAR_SHUNT_COMPENSATOR_SECTION", element_type::LINEAR_SHUNT_COMPENSATOR_SECTION)
            .value("DANGLING_LINE", element_type::DANGLING_LINE)
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
            .value("MINMAX_REACTIVE_LIMITS", element_type::MINMAX_REACTIVE_LIMITS)
            .value("ALIAS", element_type::ALIAS);

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

    m.def("get_network_elements_ids", &pypowsybl::getNetworkElementsIds, "Get network elements ids for a given element type",
          py::arg("network"), py::arg("element_type"), py::arg("nominal_voltages"),
          py::arg("countries"), py::arg("main_connected_component"), py::arg("main_synchronous_component"),
          py::arg("not_connected_to_same_bus_at_both_sides"));

    m.def("get_network_import_formats", &pypowsybl::getNetworkImportFormats, "Get supported import formats");
    m.def("get_network_export_formats", &pypowsybl::getNetworkExportFormats, "Get supported export formats");


    m.def("get_loadflow_provider_names", &pypowsybl::getLoadFlowProviderNames, "Get supported loadflow providers");
    m.def("get_security_analysis_provider_names", &pypowsybl::getSecurityAnalysisProviderNames, "Get supported security analysis providers");
    m.def("get_sensitivity_analysis_provider_names", &pypowsybl::getSensitivityAnalysisProviderNames, "Get supported sensitivity analysis providers");

    m.def("create_importer_parameters_series_array", &pypowsybl::createImporterParametersSeriesArray, "Create a parameters series array for a given import format",
          py::arg("format"));

    m.def("create_exporter_parameters_series_array", &pypowsybl::createExporterParametersSeriesArray, "Create a parameters series array for a given export format",
          py::arg("format"));

    m.def("load_network", &pypowsybl::loadNetwork, "Load a network from a file", py::call_guard<py::gil_scoped_release>(),
          py::arg("file"), py::arg("parameters"), py::arg("reporter"));

    m.def("load_network_from_string", &pypowsybl::loadNetworkFromString, "Load a network from a string", py::call_guard<py::gil_scoped_release>(),
          py::arg("file_name"), py::arg("file_content"),py::arg("parameters"), py::arg("reporter"));

    m.def("dump_network", &pypowsybl::dumpNetwork, "Dump network to a file in a given format", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("file"),py::arg("format"), py::arg("parameters"), py::arg("reporter"));

    m.def("dump_network_to_string", &pypowsybl::dumpNetworkToString, "Dump network in a given format", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("format"), py::arg("parameters"), py::arg("reporter"));

    m.def("reduce_network", &pypowsybl::reduceNetwork, "Reduce network", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("v_min"), py::arg("v_max"),
          py::arg("ids"), py::arg("vls"), py::arg("depths"), py::arg("with_dangling_lines"));

    py::enum_<pypowsybl::LoadFlowComponentStatus>(m, "LoadFlowComponentStatus", "Loadflow status for one connected component.")
            .value("CONVERGED", pypowsybl::LoadFlowComponentStatus::CONVERGED, "The loadflow has converged.")
            .value("FAILED", pypowsybl::LoadFlowComponentStatus::FAILED, "The loadflow has failed.")
            .value("MAX_ITERATION_REACHED", pypowsybl::LoadFlowComponentStatus::MAX_ITERATION_REACHED, "The loadflow has reached its maximum iterations count.")
            .value("SOLVER_FAILED", pypowsybl::LoadFlowComponentStatus::SOLVER_FAILED, "The loadflow numerical solver has failed.");
    
    py::enum_<pypowsybl::PostContingencyComputationStatus>(m, "PostContingencyComputationStatus", "Loadflow status for one connected component after contingency for security analysis.")
            .value("CONVERGED", pypowsybl::PostContingencyComputationStatus::CONVERGED, "The loadflow has converged.")
            .value("FAILED", pypowsybl::PostContingencyComputationStatus::FAILED, "The loadflow has failed.")
            .value("MAX_ITERATION_REACHED", pypowsybl::PostContingencyComputationStatus::MAX_ITERATION_REACHED, "The loadflow has reached its maximum iterations count.")
            .value("SOLVER_FAILED", pypowsybl::PostContingencyComputationStatus::SOLVER_FAILED, "The loadflow numerical solver has failed.")
            .value("NO_IMPACT", pypowsybl::PostContingencyComputationStatus::NO_IMPACT, "The contingency has no impact.");

    py::class_<load_flow_component_result>(m, "LoadFlowComponentResult", "Loadflow result for one connected component of the network.")
            .def_property_readonly("connected_component_num", [](const load_flow_component_result& r) {
                return r.connected_component_num;
            })
            .def_property_readonly("synchronous_component_num", [](const load_flow_component_result& r) {
                return r.synchronous_component_num;
            })
            .def_property_readonly("status", [](const load_flow_component_result& r) {
                return static_cast<pypowsybl::LoadFlowComponentStatus>(r.status);
            })
            .def_property_readonly("iteration_count", [](const load_flow_component_result& r) {
                return r.iteration_count;
            })
            .def_property_readonly("slack_bus_id", [](const load_flow_component_result& r) {
                return r.slack_bus_id;
            })
            .def_property_readonly("slack_bus_active_power_mismatch", [](const load_flow_component_result& r) {
                return r.slack_bus_active_power_mismatch;
            })
            .def_property_readonly("distributed_active_power", [](const load_flow_component_result& r) {
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
            .def_readwrite("no_generator_reactive_limits", &pypowsybl::LoadFlowParameters::no_generator_reactive_limits)
            .def_readwrite("phase_shifter_regulation_on", &pypowsybl::LoadFlowParameters::phase_shifter_regulation_on)
            .def_readwrite("twt_split_shunt_admittance", &pypowsybl::LoadFlowParameters::twt_split_shunt_admittance)
            .def_readwrite("simul_shunt", &pypowsybl::LoadFlowParameters::simul_shunt)
            .def_readwrite("read_slack_bus", &pypowsybl::LoadFlowParameters::read_slack_bus)
            .def_readwrite("write_slack_bus", &pypowsybl::LoadFlowParameters::write_slack_bus)
            .def_readwrite("distributed_slack", &pypowsybl::LoadFlowParameters::distributed_slack)
            .def_readwrite("balance_type", &pypowsybl::LoadFlowParameters::balance_type)
            .def_readwrite("dc_use_transformer_ratio", &pypowsybl::LoadFlowParameters::dc_use_transformer_ratio)
            .def_readwrite("countries_to_balance", &pypowsybl::LoadFlowParameters::countries_to_balance)
            .def_readwrite("connected_component_mode", &pypowsybl::LoadFlowParameters::connected_component_mode)
            .def_readwrite("provider_parameters_keys", &pypowsybl::LoadFlowParameters::provider_parameters_keys)
            .def_readwrite("provider_parameters_values", &pypowsybl::LoadFlowParameters::provider_parameters_values);

    py::class_<pypowsybl::SecurityAnalysisParameters>(m, "SecurityAnalysisParameters")
            .def(py::init(&pypowsybl::createSecurityAnalysisParameters))
            .def_readwrite("load_flow_parameters", &pypowsybl::SecurityAnalysisParameters::load_flow_parameters)
            .def_readwrite("flow_proportional_threshold", &pypowsybl::SecurityAnalysisParameters::flow_proportional_threshold)
            .def_readwrite("low_voltage_proportional_threshold", &pypowsybl::SecurityAnalysisParameters::low_voltage_proportional_threshold)
            .def_readwrite("low_voltage_absolute_threshold", &pypowsybl::SecurityAnalysisParameters::low_voltage_absolute_threshold)
            .def_readwrite("high_voltage_proportional_threshold", &pypowsybl::SecurityAnalysisParameters::high_voltage_proportional_threshold)
            .def_readwrite("high_voltage_absolute_threshold", &pypowsybl::SecurityAnalysisParameters::high_voltage_absolute_threshold)
            .def_readwrite("provider_parameters_keys", &pypowsybl::SecurityAnalysisParameters::provider_parameters_keys)
            .def_readwrite("provider_parameters_values", &pypowsybl::SecurityAnalysisParameters::provider_parameters_values);

    py::class_<pypowsybl::SensitivityAnalysisParameters>(m, "SensitivityAnalysisParameters")
            .def(py::init(&pypowsybl::createSensitivityAnalysisParameters))
            .def_readwrite("load_flow_parameters", &pypowsybl::SensitivityAnalysisParameters::load_flow_parameters)
            .def_readwrite("provider_parameters_keys", &pypowsybl::SensitivityAnalysisParameters::provider_parameters_keys)
            .def_readwrite("provider_parameters_values", &pypowsybl::SensitivityAnalysisParameters::provider_parameters_values);

    m.def("run_load_flow", &pypowsybl::runLoadFlow, "Run a load flow", py::call_guard<py::gil_scoped_release>(),
          py::arg("network"), py::arg("dc"), py::arg("parameters"), py::arg("provider"), py::arg("reporter"));

    m.def("run_load_flow_validation", &pypowsybl::runLoadFlowValidation, "Run a load flow validation", py::arg("network"), py::arg("validation_type"));

    py::class_<pypowsybl::LayoutParameters>(m, "LayoutParameters")
        .def(py::init(&pypowsybl::createLayoutParameters))
        .def_readwrite("use_name", &pypowsybl::LayoutParameters::use_name)
        .def_readwrite("center_name", &pypowsybl::LayoutParameters::center_name)
        .def_readwrite("diagonal_label", &pypowsybl::LayoutParameters::diagonal_label)
        .def_readwrite("topological_coloring", &pypowsybl::LayoutParameters::topological_coloring);

    m.def("write_single_line_diagram_svg", &pypowsybl::writeSingleLineDiagramSvg, "Write single line diagram SVG",
          py::arg("network"), py::arg("container_id"), py::arg("svg_file"), py::arg("metadata_file"), py::arg("layout_parameters"));

    m.def("get_single_line_diagram_svg", &pypowsybl::getSingleLineDiagramSvg, "Get single line diagram SVG as a string",
          py::arg("network"), py::arg("container_id"));

    m.def("get_single_line_diagram_svg_and_metadata", &pypowsybl::getSingleLineDiagramSvgAndMetadata, "Get single line diagram SVG and its metadata as a list of strings",
          py::arg("network"), py::arg("container_id"), py::arg("layout_parameters"));

    m.def("write_network_area_diagram_svg", &pypowsybl::writeNetworkAreaDiagramSvg, "Write network area diagram SVG",
          py::arg("network"), py::arg("svg_file"), py::arg("voltage_level_ids"), py::arg("depth"));

    m.def("get_network_area_diagram_svg", &pypowsybl::getNetworkAreaDiagramSvg, "Get network area diagram SVG as a string",
          py::arg("network"), py::arg("voltage_level_ids"), py::arg("depth"));

    m.def("create_security_analysis", &pypowsybl::createSecurityAnalysis, "Create a security analysis");

    m.def("add_contingency", &pypowsybl::addContingency, "Add a contingency to a security analysis or sensitivity analysis",
          py::arg("analysis_context"), py::arg("contingency_id"), py::arg("elements_ids"));

    py::enum_<pypowsybl::LimitType>(m, "LimitType")
            .value("CURRENT", pypowsybl::LimitType::CURRENT)
            .value("LOW_VOLTAGE", pypowsybl::LimitType::LOW_VOLTAGE)
            .value("HIGH_VOLTAGE", pypowsybl::LimitType::HIGH_VOLTAGE);

    py::enum_<pypowsybl::Side>(m, "Side")
            .value("NONE", pypowsybl::Side::NONE)
            .value("ONE", pypowsybl::Side::ONE)
            .value("TWO", pypowsybl::Side::TWO);

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
                return static_cast<pypowsybl::Side>(v.side);
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

    py::class_<pre_contingency_result>(m, "PreContingencyResult")
            .def_property_readonly("status", [](const pre_contingency_result& r) {
                return static_cast<pypowsybl::LoadFlowComponentStatus>(r.status);
            })
            .def_property_readonly("limit_violations", [](const pre_contingency_result& r) {
                return pypowsybl::LimitViolationArray((array *) & r.limit_violations);
            });

    m.def("run_security_analysis", &pypowsybl::runSecurityAnalysis, "Run a security analysis", py::call_guard<py::gil_scoped_release>(),
          py::arg("security_analysis_context"), py::arg("network"), py::arg("parameters"),
          py::arg("provider"), py::arg("dc"), py::arg("reporter"));

    m.def("create_sensitivity_analysis", &pypowsybl::createSensitivityAnalysis, "Create run_sea sensitivity analysis");

    py::class_<::zone>(m, "Zone")
            .def(py::init([](const std::string& id, const std::vector<std::string>& injectionsIds, const std::vector<double>& injectionsShiftKeys) {
                return pypowsybl::createZone(id, injectionsIds, injectionsShiftKeys);
            }), py::arg("id"), py::arg("injections_ids"), py::arg("injections_shift_keys"));

    m.def("set_zones", &pypowsybl::setZones, "Add zones to sensitivity analysis",
          py::arg("sensitivity_analysis_context"), py::arg("zones"));

    m.def("add_branch_flow_factor_matrix", &pypowsybl::addBranchFlowFactorMatrix, "Add a branch_flow factor matrix to a sensitivity analysis",
              py::arg("sensitivity_analysis_context"), py::arg("matrix_id"), py::arg("branches_ids"), py::arg("variables_ids"));

    m.def("add_precontingency_branch_flow_factor_matrix", &pypowsybl::addPreContingencyBranchFlowFactorMatrix, "Add a branch_flow factor matrix to a sensitivity analysis",
                  py::arg("sensitivity_analysis_context"), py::arg("matrix_id"), py::arg("branches_ids"), py::arg("variables_ids"));

    m.def("add_postcontingency_branch_flow_factor_matrix", &pypowsybl::addPostContingencyBranchFlowFactorMatrix, "Add a branch_flow factor matrix to a sensitivity analysis",
                  py::arg("sensitivity_analysis_context"), py::arg("matrix_id"), py::arg("branches_ids"), py::arg("variables_ids"), py::arg("contingencies_ids"));

    m.def("set_bus_voltage_factor_matrix", &pypowsybl::setBusVoltageFactorMatrix, "Add a bus_voltage factor matrix to a sensitivity analysis",
          py::arg("sensitivity_analysis_context"), py::arg("bus_ids"), py::arg("target_voltage_ids"));

    m.def("run_sensitivity_analysis", &pypowsybl::runSensitivityAnalysis, "Run a sensitivity analysis", py::call_guard<py::gil_scoped_release>(),
          py::arg("sensitivity_analysis_context"), py::arg("network"), py::arg("dc"), py::arg("parameters"), py::arg("provider"), py::arg("reporter"));

    py::class_<matrix>(m, "Matrix", py::buffer_protocol())
            .def_buffer([](matrix& m) -> py::buffer_info {
                return py::buffer_info(m.values,
                                       sizeof(double),
                                       py::format_descriptor<double>::format(),
                                       2,
                                       { m.row_count, m.column_count },
                                       { sizeof(double) * m.column_count, sizeof(double) });
            });

    m.def("get_branch_flows_sensitivity_matrix", &pypowsybl::getBranchFlowsSensitivityMatrix, "Get sensitivity analysis result matrix for a given contingency",
              py::arg("sensitivity_analysis_result_context"), py::arg("matrix_id"), py::arg("contingency_id"));

    m.def("get_bus_voltages_sensitivity_matrix", &pypowsybl::getBusVoltagesSensitivityMatrix, "Get sensitivity analysis result matrix for a given contingency",
          py::arg("sensitivity_analysis_result_context"), py::arg("contingency_id"));

    m.def("get_reference_flows", &pypowsybl::getReferenceFlows, "Get sensitivity analysis result reference flows for a given contingency",
          py::arg("sensitivity_analysis_result_context"), py::arg("matrix_id"), py::arg("contingency_id"));

    m.def("get_reference_voltages", &pypowsybl::getReferenceVoltages, "Get sensitivity analysis result reference voltages for a given contingency",
          py::arg("sensitivity_analysis_result_context"), py::arg("contingency_id"));

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
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("element_type"), py::arg("filter_attributes_type"), py::arg("attributes"), py::arg("array"));

    m.def("create_network_elements_extension_series_array", &pypowsybl::createNetworkElementsExtensionSeriesArray, "Create a network elements extensions series array for a given extension name",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("extension_name"));

    m.def("get_extensions_names", &pypowsybl::getExtensionsNames, "get all the extensions names available");
    
    m.def("update_network_elements_with_series", pypowsybl::updateNetworkElementsWithSeries, "Update network elements for a given element type with a series",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("dataframe"), py::arg("element_type"));

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
            .value("SPECIFIC", contingency_context_type::SPECIFIC);

    m.def("get_post_contingency_results", &pypowsybl::getPostContingencyResults, "get post contingency results of a security analysis", py::arg("result"));
    m.def("get_pre_contingency_result", &pypowsybl::getPreContingencyResult, "get pre contingency result of a security analysis", py::arg("result"));
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
    m.def("create_element", ::createElement, "create a new element on the network", py::arg("network"),  py::arg("dataframes"),  py::arg("elementType"));

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
    m.def("get_security_analysis_provider_parameters_names", &pypowsybl::getSecurityAnalysisProviderParametersNames, "get provider parameters for a security analysis provider", py::arg("provider"));
    m.def("get_sensitivity_analysis_provider_parameters_names", &pypowsybl::getSensitivityAnalysisProviderParametersNames, "get provider parameters for a sensitivity analysis provider", py::arg("provider"));
    m.def("update_extensions", pypowsybl::updateNetworkElementsExtensionsWithSeries, "Update extensions of network elements for a given element type with a series",
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("name"), py::arg("dataframe"));
    m.def("remove_extensions", &pypowsybl::removeExtensions, "Remove extensions from network elements", 
          py::call_guard<py::gil_scoped_release>(), py::arg("network"), py::arg("name"), py::arg("ids"));
    m.def("get_network_extensions_dataframe_metadata", &pypowsybl::getNetworkExtensionsDataframeMetadata, "Get dataframe metadata for a given network element extension",
          py::arg("name"));
    m.def("get_network_extensions_creation_dataframes_metadata", &pypowsybl::getNetworkExtensionsCreationDataframesMetadata, "Get network extension creation tables metadata for a given network element extension",
          py::arg("name"));
    m.def("create_extensions", ::createExtensions, "create extensions of network elements given the extension name", 
          py::call_guard<py::gil_scoped_release>(), py::arg("network"),  py::arg("dataframes"),  py::arg("name"));
    m.def("create_reporter_model", &pypowsybl::createReporterModel, "Create a reporter model", py::arg("task_key"), py::arg("default_name"));
    m.def("print_report", &pypowsybl::printReport, "Print a report", py::arg("reporter_model"));
	m.def("json_report", &pypowsybl::jsonReport, "Print a report in json format", py::arg("reporter_model"));
    m.def("create_glsk_document", &pypowsybl::createGLSKdocument, "Create a glsk importer.", py::arg("filename"));

    m.def("get_glsk_injection_keys", &pypowsybl::getGLSKinjectionkeys, "Get glsk injection keys available for a country", py::arg("network"), py::arg("importer"), py::arg("country"), py::arg("instant"));

    m.def("get_glsk_countries", &pypowsybl::getGLSKcountries, "Get glsk countries", py::arg("importer"));

    m.def("get_glsk_factors", &pypowsybl::getGLSKInjectionFactors, "Get glsk factors", py::arg("network"), py::arg("importer"), py::arg("country"), py::arg("instant"));

    m.def("get_glsk_factors_start_timestamp", &pypowsybl::getInjectionFactorStartTimestamp, "Get glsk start timestamp", py::arg("importer"));

    m.def("get_glsk_factors_end_timestamp", &pypowsybl::getInjectionFactorEndTimestamp, "Get glsk end timestamp", py::arg("importer"));
    
    m.def("create_flow_decomposition", &pypowsybl::createFlowDecomposition, "Create a security analysis");

    m.def("add_precontingency_monitored_elements_for_flow_decomposition", &pypowsybl::addPrecontingencyMonitoredElementsForFlowDecomposition, "Add elements to be monitored for a flow decomposition",
          py::arg("flow_decomposition_context"), py::arg("elements_ids"));

    m.def("run_flow_decomposition", &pypowsybl::runFlowDecomposition, "Run flow decomposition on a network",
          py::call_guard<py::gil_scoped_release>(), py::arg("flow_decomposition_context"), py::arg("network"), py::arg("flow_decomposition_parameters"), py::arg("load_flow_parameters"));

    py::class_<pypowsybl::FlowDecompositionParameters>(m, "FlowDecompositionParameters")
                .def(py::init(&pypowsybl::createFlowDecompositionParameters))
                .def_readwrite("enable_losses_compensation", &pypowsybl::FlowDecompositionParameters::enable_losses_compensation)
                .def_readwrite("losses_compensation_epsilon", &pypowsybl::FlowDecompositionParameters::losses_compensation_epsilon)
                .def_readwrite("sensitivity_epsilon", &pypowsybl::FlowDecompositionParameters::sensitivity_epsilon)
                .def_readwrite("rescale_enabled", &pypowsybl::FlowDecompositionParameters::rescale_enabled)
                .def_readwrite("dc_fallback_enabled_after_ac_divergence", &pypowsybl::FlowDecompositionParameters::dc_fallback_enabled_after_ac_divergence)
                .def_readwrite("sensitivity_variable_batch_size", &pypowsybl::FlowDecompositionParameters::sensitivity_variable_batch_size);

    m.def("create_line_on_line", &pypowsybl::createLineOnLine, "create a new line between a tee point and an existing voltage level", py::arg("network"), py::arg("bbs_or_bus_id"),
            py::arg("new_line_id"), py::arg("new_line_r"), py::arg("new_line_x"), py::arg("new_line_b1"), py::arg("new_line_b2"), py::arg("new_line_g1"), py::arg("new_line_g2"),
            py::arg("line_id"), py::arg("line1_id"), py::arg("line1_name"), py::arg("line2_id"), py::arg("line2_name"), py::arg("position_percent"),
            py::arg("create_fictitious_substation"), py::arg("fictitious_voltage_level_id"), py::arg("fictitious_voltage_level_name"), py::arg("fictitious_substation_id"), py::arg("fictitious_substation_name"));
    m.def("revert_create_line_on_line", &pypowsybl::revertCreateLineOnLine, "reverses the action done in the create_line_on_line", py::arg("network"), py::arg("line_to_be_merged1_id"), py::arg("line_to_be_merged2_id"),
            py::arg("line_to_be_deleted_id"), py::arg("merged_line_id"), py::arg("merged_line_name"));

    m.def("connect_voltage_level_on_line", &pypowsybl::connectVoltageLevelOnLine, "connect a voltage level on a line", py::arg("network"), py::arg("bbs_or_bus_id"), py::arg("line_id"),
            py::arg("line1_id"), py::arg("line1_name"), py::arg("line2_id"), py::arg("line2_name"), py::arg("position_percent"));
    m.def("revert_connect_voltage_level_on_line", &pypowsybl::revertConnectVoltageLevelOnLine, "reverses the action done in connect_voltage_level_on_line", py::arg("network"), py::arg("line1_id"), py::arg("line2_id"), py::arg("line_id"), py::arg("line_name"));

    m.def("create_feeder_bay", ::createFeederBay, "Create feeder bay", py::arg("network"), py::arg("throw_exception"), py::arg("reporter"), py::arg("dataframe"), py::arg("element_type"));

    m.def("get_line_feeder_bays_metadata", &pypowsybl::getLineFeederBaysMetadata, "Get metadata for line branch feeder bay creation dataframe.");
    m.def("create_branch_feeder_bays_line", &pypowsybl::createBranchFeederBaysLine, "Create branch feeder bays", py::arg("network"), py::arg("dataframe"));

    m.def("get_twt_feeder_bays_metadata", &pypowsybl::getTwtFeederBaysMetadata, "Get metadata for twt branch feeder bay creation dataframe.");
    m.def("create_branch_feeder_bays_twt", &pypowsybl::createBranchFeederBaysTwt, "Create branch feeder bays", py::arg("network"), py::arg("dataframe"));


    m.def("get_connectables_order_positions", &pypowsybl::getConnectablesOrderPositions, "Get connectables order positions", py::arg("network"), py::arg("voltage_level_id"));

    m.def("get_unused_order_positions", &pypowsybl::getUnusedConnectableOrderPositions, "Get unused order positions before or after", py::arg("network"), py::arg("busbar_section_id"), py::arg("before_or_after"));

    m.def("remove_aliases", &pypowsybl::removeAliases, "remove specified aliases on a network", py::arg("network"), py::arg("dataframe"));

    m.def("close", &pypowsybl::closePypowsybl, "Closes pypowsybl module.");
}
