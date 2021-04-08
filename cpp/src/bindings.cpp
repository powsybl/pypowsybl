/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <limits>
#include "pypowsybl.h"

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

PYBIND11_MODULE(_pypowsybl, m) {
    pypowsybl::init();

    m.doc() = "PowSyBl Python API";

    py::register_exception<pypowsybl::PyPowsyblError>(m, "PyPowsyblError");

    m.def("set_debug_mode", &pypowsybl::setDebugMode, "Set debug mode");

    m.def("get_version_table", &pypowsybl::getVersionTable, "Get an ASCII table with all PowSybBl modules version");

    m.def("create_empty_network", &pypowsybl::createEmptyNetwork, "Create an empty network",
          py::arg("id"));

    m.def("create_ieee14_network", &pypowsybl::createIeee14Network, "Create an IEEE 14 network");

    m.def("create_eurostag_tutorial_example1_network", &pypowsybl::createEurostagTutorialExample1Network, "Create an Eurostag tutorial example 1 network");

    m.def("update_switch_position", &pypowsybl::updateSwitchPosition, "Update a switch position");

    m.def("update_connectable_status", &pypowsybl::updateConnectableStatus, "Update a connectable (branch or injection) status");

    py::enum_<element_type>(m, "ElementType")
            .value("BUS", element_type::BUS)
            .value("LINE", element_type::LINE)
            .value("TWO_WINDINGS_TRANSFORMER", element_type::TWO_WINDINGS_TRANSFORMER)
            .value("THREE_WINDINGS_TRANSFORMER", element_type::THREE_WINDINGS_TRANSFORMER)
            .value("GENERATOR", element_type::GENERATOR)
            .value("LOAD", element_type::LOAD)
            .value("SHUNT_COMPENSATOR", element_type::SHUNT_COMPENSATOR)
            .value("DANGLING_LINE", element_type::DANGLING_LINE)
            .value("LCC_CONVERTER_STATION", element_type::LCC_CONVERTER_STATION)
            .value("VSC_CONVERTER_STATION", element_type::VSC_CONVERTER_STATION)
            .value("STATIC_VAR_COMPENSATOR", element_type::STATIC_VAR_COMPENSATOR)
            .export_values();

    m.def("get_network_elements_ids", &pypowsybl::getNetworkElementsIds, "Get network elements ids for a given element type",
          py::arg("network"), py::arg("element_type"), py::arg("nominal_voltages"),
          py::arg("countries"), py::arg("main_connected_component"), py::arg("main_synchronous_component"),
          py::arg("not_connected_to_same_bus_at_both_sides"));

    m.def("load_network", &pypowsybl::loadNetwork, "Load a network from a file");

    m.def("dump_network", &pypowsybl::dumpNetwork, "Dump network to a file in a given format");

    m.def("reduce_network", &pypowsybl::reduceNetwork, "Reduce network", py::arg("network"), py::arg("v_min"), py::arg("v_max"),
          py::arg("ids"), py::arg("vls"), py::arg("depths"), py::arg("with_dangling_lines"));

    py::enum_<pypowsybl::LoadFlowComponentStatus>(m, "LoadFlowComponentStatus")
            .value("CONVERGED", pypowsybl::LoadFlowComponentStatus::CONVERGED)
            .value("FAILED", pypowsybl::LoadFlowComponentStatus::FAILED)
            .value("MAX_ITERATION_REACHED", pypowsybl::LoadFlowComponentStatus::MAX_ITERATION_REACHED)
            .value("SOLVER_FAILED", pypowsybl::LoadFlowComponentStatus::SOLVER_FAILED)
            .export_values();

    py::class_<load_flow_component_result>(m, "LoadFlowComponentResult")
            .def_property_readonly("component_num", [](const load_flow_component_result& r) {
                return r.component_num;
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
            });

    bindArray<pypowsybl::LoadFlowComponentResultArray>(m, "LoadFlowComponentResultArray");

    py::enum_<pypowsybl::VoltageInitMode>(m, "VoltageInitMode")
            .value("UNIFORM_VALUES", pypowsybl::VoltageInitMode::UNIFORM_VALUES)
            .value("PREVIOUS_VALUES", pypowsybl::VoltageInitMode::PREVIOUS_VALUES)
            .value("DC_VALUES", pypowsybl::VoltageInitMode::DC_VALUES)
            .export_values();

    py::enum_<pypowsybl::BalanceType>(m, "BalanceType")
            .value("PROPORTIONAL_TO_GENERATION_P", pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_P)
            .value("PROPORTIONAL_TO_GENERATION_P_MAX", pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_P_MAX)
            .value("PROPORTIONAL_TO_LOAD", pypowsybl::BalanceType::PROPORTIONAL_TO_LOAD)
            .value("PROPORTIONAL_TO_CONFORM_LOAD", pypowsybl::BalanceType::PROPORTIONAL_TO_CONFORM_LOAD)
            .export_values();

    py::class_<load_flow_parameters>(m, "LoadFlowParameters")
            .def(py::init([](pypowsybl::VoltageInitMode voltageInitMode, bool transformerVoltageControlOn, bool noGeneratorReactiveLimits,
                             bool phaseShifterRegulationOn, bool twtSplitShuntAdmittance, bool simulShunt, bool readSlackBus, bool writeSlackBus,
                             bool distributedSlack, pypowsybl::BalanceType balanceType) {
                auto parameters = new load_flow_parameters();
                parameters->voltage_init_mode = voltageInitMode;
                parameters->transformer_voltage_control_on = transformerVoltageControlOn;
                parameters->no_generator_reactive_limits = noGeneratorReactiveLimits;
                parameters->phase_shifter_regulation_on = phaseShifterRegulationOn;
                parameters->twt_split_shunt_admittance = twtSplitShuntAdmittance;
                parameters->simul_shunt = simulShunt;
                parameters->read_slack_bus = readSlackBus;
                parameters->write_slack_bus = writeSlackBus;
                parameters->distributed_slack = distributedSlack;
                parameters->balance_type = balanceType;
                return parameters;
            }), py::arg("voltage_init_mode") = pypowsybl::VoltageInitMode::UNIFORM_VALUES, py::arg("transformer_voltage_control_on") = false,
                 py::arg("no_generator_reactive_limits") = false, py::arg("phase_shifter_regulation_on") = false,
                 py::arg("twt_split_shunt_admittance") = false, py::arg("simul_shunt") = false,
                 py::arg("read_slack_bus") = false, py::arg("write_slack_bus") = false,
                 py::arg("distributed_slack") = true, py::arg("balance_type") = pypowsybl::BalanceType::PROPORTIONAL_TO_GENERATION_P_MAX)
            .def_property("voltage_init_mode", [](const load_flow_parameters& p) {
                return static_cast<pypowsybl::VoltageInitMode>(p.voltage_init_mode);
            }, [](load_flow_parameters& p, pypowsybl::VoltageInitMode voltageInitMode) {
                p.voltage_init_mode = voltageInitMode;
            })
            .def_property("transformer_voltage_control_on", [](const load_flow_parameters& p) {
                return (bool) p.transformer_voltage_control_on;
            }, [](load_flow_parameters& p, bool transformerVoltageControlOn) {
                p.transformer_voltage_control_on = transformerVoltageControlOn;
            })
            .def_property("no_generator_reactive_limits", [](const load_flow_parameters& p) {
                return (bool) p.no_generator_reactive_limits;
            }, [](load_flow_parameters& p, bool noGeneratorReactiveLimits) {
                p.no_generator_reactive_limits = noGeneratorReactiveLimits;
            })
            .def_property("phase_shifter_regulation_on", [](const load_flow_parameters& p) {
                return (bool) p.phase_shifter_regulation_on;
            }, [](load_flow_parameters& p, bool phaseShifterRegulationOn) {
                p.phase_shifter_regulation_on = phaseShifterRegulationOn;
            })
            .def_property("twt_split_shunt_admittance", [](const load_flow_parameters& p) {
                return (bool) p.twt_split_shunt_admittance;
            }, [](load_flow_parameters& p, bool twtSplitShuntAdmittance) {
                p.twt_split_shunt_admittance = twtSplitShuntAdmittance;
            })
            .def_property("simul_shunt", [](const load_flow_parameters& p) {
                return (bool) p.simul_shunt;
            }, [](load_flow_parameters& p, bool simulShunt) {
                p.simul_shunt = simulShunt;
            })
            .def_property("read_slack_bus", [](const load_flow_parameters& p) {
                return (bool) p.read_slack_bus;
            }, [](load_flow_parameters& p, bool readSlackBus) {
                p.read_slack_bus = readSlackBus;
            })
            .def_property("write_slack_bus", [](const load_flow_parameters& p) {
                return (bool) p.write_slack_bus;
            }, [](load_flow_parameters& p, bool writeSlackBus) {
                p.write_slack_bus = writeSlackBus;
            })
            .def_property("distributed_slack", [](const load_flow_parameters& p) {
                return (bool) p.distributed_slack;
            }, [](load_flow_parameters& p, bool distributedSlack) {
                p.distributed_slack = distributedSlack;
            })
            .def_property("balance_type", [](const load_flow_parameters& p) {
                return static_cast<pypowsybl::BalanceType>(p.balance_type);
            }, [](load_flow_parameters& p, pypowsybl::BalanceType balanceType) {
                p.balance_type = balanceType;
            });

    m.def("run_load_flow", &pypowsybl::runLoadFlow, "Run a load flow", py::arg("network"),
          py::arg("dc"), py::arg("parameters"));

    py::class_<bus>(m, "Bus")
        .def_property_readonly("id", [](const bus& b) {
            return b.id;
        })
        .def_property_readonly("v_magnitude", [](const bus& b) {
            return b.v_magnitude;
        })
        .def_property_readonly("v_angle", [](const bus& b) {
            return b.v_angle;
        })
        .def_property_readonly("component_num", [](const bus& b) {
            return b.component_num;
        });

    bindArray<pypowsybl::BusArray>(m, "BusArray");

    m.def("get_buses", &pypowsybl::getBusArray, "Get network buses", py::arg("network"));

    py::class_<generator>(m, "Generator")
            .def_property_readonly("id", [](const generator& g) {
                return g.id;
            })
            .def_property_readonly("target_p", [](const generator& g) {
                return g.target_p;
            })
            .def_property_readonly("min_p", [](const generator& g) {
                return g.min_p;
            })
            .def_property_readonly("max_p", [](const generator& g) {
                return g.max_p;
            })
            .def_property_readonly("nominal_voltage", [](const generator& g) {
                return g.nominal_voltage;
            })
            .def_property_readonly("country", [](const generator& g) {
                return g.country;
            })
            .def_property_readonly("bus", [](const generator& g) {
                return g.bus_;
            });

    bindArray<pypowsybl::GeneratorArray>(m, "GeneratorArray");

    m.def("get_generators", &pypowsybl::getGeneratorArray, "Get network generators", py::arg("network"));

    py::class_<load>(m, "Load")
            .def_property_readonly("id", [](const load& l) {
                return l.id;
            })
            .def_property_readonly("p0", [](const load& l) {
                return l.p0;
            })
            .def_property_readonly("nominal_voltage", [](const load& l) {
                return l.nominal_voltage;
            })
            .def_property_readonly("country", [](const load& l) {
                return l.country;
            })
            .def_property_readonly("bus", [](const load& l) {
                return l.bus_;
            });

    bindArray<pypowsybl::LoadArray>(m, "LoadArray");

    m.def("get_loads", &pypowsybl::getLoadArray, "Get network loads", py::arg("network"));

    m.def("write_single_line_diagram_svg", &pypowsybl::writeSingleLineDiagramSvg, "Write single line diagram SVG",
          py::arg("network"), py::arg("container_id"), py::arg("svg_file"));

    m.def("create_security_analysis", &pypowsybl::createSecurityAnalysis, "Create a security analysis");

    m.def("add_contingency", &pypowsybl::addContingency, "Add a contingency to a security analysis or sensitivity analysis",
          py::arg("analysis_context"), py::arg("contingency_id"), py::arg("elements_ids"));

    py::enum_<pypowsybl::LimitType>(m, "LimitType")
            .value("CURRENT", pypowsybl::LimitType::CURRENT)
            .value("LOW_VOLTAGE", pypowsybl::LimitType::LOW_VOLTAGE)
            .value("HIGH_VOLTAGE", pypowsybl::LimitType::HIGH_VOLTAGE)
            .export_values();

    py::enum_<pypowsybl::Side>(m, "Side")
            .value("NONE", pypowsybl::Side::NONE)
            .value("ONE", pypowsybl::Side::ONE)
            .value("TWO", pypowsybl::Side::TWO)
            .export_values();

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

    py::class_<contingency_result>(m, "ContingencyResult")
            .def_property_readonly("contingency_id", [](const contingency_result& r) {
                return r.contingency_id;
            })
            .def_property_readonly("status", [](const contingency_result& r) {
                return static_cast<pypowsybl::LoadFlowComponentStatus>(r.status);
            })
            .def_property_readonly("limit_violations", [](const contingency_result& r) {
                return pypowsybl::LimitViolationArray((array *) & r.limit_violations);
            });

    bindArray<pypowsybl::ContingencyResultArray>(m, "ContingencyResultArray");

    m.def("run_security_analysis", &pypowsybl::runSecurityAnalysis, "Run a security analysis",
          py::arg("security_analysis_context"), py::arg("network"), py::arg("parameters"));

    m.def("create_sensitivity_analysis", &pypowsybl::createSensitivityAnalysis, "Create a sensitivity analysis");

    m.def("set_factor_matrix", &pypowsybl::setFactorMatrix, "Set a factor matrix to a sensitivity analysis",
          py::arg("sensitivity_analysis_context"), py::arg("branches_ids"), py::arg("injections_or_transfos_ids"));

    m.def("run_sensitivity_analysis", &pypowsybl::runSensitivityAnalysis, "Run a sensitivity analysis",
          py::arg("sensitivity_analysis_context"), py::arg("network"), py::arg("parameters"));

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
          py::arg("sensitivity_analysis_result_context"), py::arg("contingency_id"));

    m.def("get_reference_flows", &pypowsybl::getReferenceFlows, "Get sensitivity analysis result reference flows for a given contingency",
          py::arg("sensitivity_analysis_result_context"), py::arg("contingency_id"));

    py::class_<series>(m, "Series")
            .def_property_readonly("name", [](const series& s) {
                return s.name;
            })
            .def_property_readonly("type", [](const series& s) {
                return s.type;
            })
            .def_property_readonly("str_data", [](const series& s) {
                return pypowsybl::toVector<std::string>((array *) & s.data);
            })
            .def_property_readonly("double_data", [](const series& s) {
                return pypowsybl::toVector<double>((array *) & s.data);
            })
            .def_property_readonly("int_data", [](const series& s) {
                return pypowsybl::toVector<int>((array *) & s.data);
            })
            .def_property_readonly("boolean_data", [](const series& s) {
                return pypowsybl::toVector<bool>((array *) & s.data);
            });

    m.def("create_network_elements_series_array", &pypowsybl::createNetworkElementsSeriesArray, "Create a network elements series array for a given element type",
          py::arg("network"), py::arg("element_type"));

    bindArray<pypowsybl::SeriesArray>(m, "SeriesArray");

    m.def("destroy_object_handle", &pypowsybl::destroyObjectHandle, "Destroy Java object handle", py::arg("object_handle"));
}
