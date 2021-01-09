/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include "gridpy.h"

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

PYBIND11_MODULE(_gridpy, m) {
    gridpy::init();

    m.doc() = "PowSyBl Python API";

    m.def("print_version", &gridpy::printVersion, "Print a table with all PowSybBl modules version");

    m.def("create_empty_network", &gridpy::createEmptyNetwork, "Create an empty network",
          py::arg("id"));

    m.def("create_ieee14_network", &gridpy::createIeee14Network, "Create an IEEE 14 network");

    m.def("create_eurostag_tutorial_example1_network", &gridpy::createEurostagTutorialExample1Network, "Create an Eurostag tutorial example 1 network");

    m.def("update_switch_position", &gridpy::updateSwitchPosition, "Update a switch position");

    m.def("update_connectable_status", &gridpy::updateConnectableStatus, "Update a connectable (branch or injection) status");

    m.def("load_network", &gridpy::loadNetwork, "Load a network from a file");

    m.def("dump_network", &gridpy::dumpNetwork, "Dump network to a file in a given format");

    py::enum_<gridpy::LoadFlowComponentStatus>(m, "LoadFlowComponentStatus")
            .value("CONVERGED", gridpy::LoadFlowComponentStatus::CONVERGED)
            .value("FAILED", gridpy::LoadFlowComponentStatus::FAILED)
            .value("MAX_ITERATION_REACHED", gridpy::LoadFlowComponentStatus::MAX_ITERATION_REACHED)
            .value("SOLVER_FAILED", gridpy::LoadFlowComponentStatus::SOLVER_FAILED)
            .export_values();

    py::class_<load_flow_component_result>(m, "LoadFlowComponentResult")
            .def_property_readonly("component_num", [](const load_flow_component_result& r) {
                return r.component_num;
            })
            .def_property_readonly("status", [](const load_flow_component_result& r) {
                return static_cast<gridpy::LoadFlowComponentStatus>(r.status);
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

    bindArray<gridpy::LoadFlowComponentResultArray>(m, "LoadFlowComponentResultArray");

    py::class_<load_flow_parameters>(m, "LoadFlowParameters")
            .def(py::init([](bool distributedSlack) {
                auto parameters = new load_flow_parameters();
                parameters->distributed_slack = distributedSlack;
                return parameters;
            }), py::arg("distributed_slack") = true)
            .def_property("distributed_slack", [](const load_flow_parameters& p) {
                return p.distributed_slack != 0;
            }, [](load_flow_parameters& p, bool distributedSlack) {
                p.distributed_slack = distributedSlack;
            });

    m.def("run_load_flow", &gridpy::runLoadFlow, "Run a load flow", py::arg("network"),
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
        });

    bindArray<gridpy::BusArray>(m, "BusArray");

    m.def("get_buses", &gridpy::getBusArray, "Get network buses", py::arg("network"),
          py::arg("bus_breaker_view"));

    m.def("write_single_line_diagram_svg", &gridpy::writeSingleLineDiagramSvg, "Write single line diagram SVG",
          py::arg("network"), py::arg("container_id"), py::arg("svg_file"));

    m.def("create_security_analysis", &gridpy::createSecurityAnalysis, "Create a security analysis");

    m.def("add_contingency_to_security_analysis", &gridpy::addContingencyToSecurityAnalysis, "Add a contingency to the security analysis",
          py::arg("security_analysis_context"), py::arg("contingency_id"), py::arg("elements_ids"));

    py::enum_<gridpy::LimitType>(m, "LimitType")
            .value("CURRENT", gridpy::LimitType::CURRENT)
            .value("LOW_VOLTAGE", gridpy::LimitType::LOW_VOLTAGE)
            .value("HIGH_VOLTAGE", gridpy::LimitType::HIGH_VOLTAGE)
            .export_values();

    py::enum_<gridpy::Side>(m, "Side")
            .value("NONE", gridpy::Side::NONE)
            .value("ONE", gridpy::Side::ONE)
            .value("TWO", gridpy::Side::TWO)
            .export_values();

    py::class_<limit_violation>(m, "LimitViolation")
            .def_property_readonly("subject_id", [](const limit_violation& v) {
                return v.subject_id;
            })
            .def_property_readonly("subject_name", [](const limit_violation& v) {
                return v.subject_name;
            })
            .def_property_readonly("limit_type", [](const limit_violation& v) {
                return static_cast<gridpy::LimitType>(v.limit_type);
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
                return static_cast<gridpy::Side>(v.side);
            })
            .def("__repr__", [](const limit_violation & v) {
                return format("LimitViolation(subject_id='%s', subject_name='%s', limit_type=%s, limit=%f, limit_name='%s', acceptable_duration=%d, limit_reduction=%f, value=%f, side=%s)",
                              v.subject_id, v.subject_name, gridpy::str(static_cast<gridpy::LimitType>(v.limit_type)).c_str(), v.limit,
                              v.limit_name, v.acceptable_duration, v.limit_reduction, v.value, gridpy::str(static_cast<gridpy::Side>(v.side)).c_str());
            });

    bindArray<gridpy::LimitViolationArray>(m, "LimitViolationArray");

    py::class_<contingency_result>(m, "ContingencyResult")
            .def_property_readonly("contingency_id", [](const contingency_result& r) {
                return r.contingency_id;
            })
            .def_property_readonly("status", [](const contingency_result& r) {
                return static_cast<gridpy::LoadFlowComponentStatus>(r.status);
            })
            .def_property_readonly("limit_violations", [](const contingency_result& r) {
                return gridpy::LimitViolationArray((array*) &r.limit_violations);
            })
            .def("__repr__", [](const contingency_result& r) {
                return format("ContingencyResult(contingency_id='%s', status=%s, limit_violations=[%d])",
                              r.contingency_id, gridpy::str(static_cast<gridpy::LoadFlowComponentStatus>(r.status)).c_str(),
                              r.limit_violations.length);
            });

    bindArray<gridpy::ContingencyResultArray>(m, "ContingencyResultArray");

    m.def("run_security_analysis", &gridpy::runSecurityAnalysis, "Run a security analysis",
          py::arg("security_analysis_context"), py::arg("network"), py::arg("parameters"));

    m.def("destroy_object_handle", &gridpy::destroyObjectHandle, "Destroy Java object handle", py::arg("object_handle"));
}
