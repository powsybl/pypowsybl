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

template<typename... Args>
std::string format(const char* fmt, Args... args) {
    size_t size = snprintf(nullptr, 0, fmt, args...);
    std::string buf;
    buf.reserve(size + 1);
    buf.resize(size);
    snprintf(&buf[0], size + 1, fmt, args...);
    return buf;
}

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

    m.def("update_switch_position", &gridpy::updateSwitchPosition, "Update a switch position");

    m.def("update_connectable_status", &gridpy::updateConnectableStatus, "Update a connectable (branch or injection) status");

    m.def("load_network", &gridpy::loadNetwork, "Load a network from a file");

    m.def("dump_network", &gridpy::dumpNetwork, "Dump network to a file in a given format");

    py::class_<load_flow_component_result>(m, "LoadFlowComponentResult")
            .def_property_readonly("component_num", [](const load_flow_component_result& r) {
                return r.component_num;
            })
            .def_property_readonly("status", [](const load_flow_component_result& r) {
                return r.status;
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
            .def("__repr__", [](const load_flow_component_result& r) {
                return format("LoadFlowComponentResult(component_num=%d, status='%s', iteration_count=%d, slack_bus_id='%s', slack_bus_active_power_mismatch=%f)",
                              r.component_num, r.status, r.iteration_count, r.slack_bus_id, r.slack_bus_active_power_mismatch);
            });

    bindArray<gridpy::LoadFlowComponentResultArray>(m, "LoadFlowComponentResultArray");

    m.def("run_load_flow", &gridpy::runLoadFlow, "Run a load flow", py::arg("network"),
          py::arg("distributed_slack"), py::arg("dc"));

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
        .def("__repr__", [](const bus& b) {
            return format("Bus(id='%s', v_magnitude=%f, v_angle=%f)", b.id, b.v_magnitude, b.v_angle);
        });

    bindArray<gridpy::BusArray>(m, "BusArray");

    m.def("get_buses", &gridpy::getBusArray, "Get network buses", py::arg("network"),
          py::arg("bus_breaker_view"));

    m.def("write_single_line_diagram_svg", &gridpy::writeSingleLineDiagramSvg, "Write single line diagram SVG",
          py::arg("network"), py::arg("container_id"), py::arg("svg_file"));

    m.def("create_security_analysis", &gridpy::createSecurityAnalysis, "Create a security analysis");

    m.def("add_contingency_to_security_analysis", &gridpy::addContingencyToSecurityAnalysis, "Add a contingency to the security analysis",
          py::arg("security_analysis_context"), py::arg("contingency_id"), py::arg("elements_ids"));

    py::class_<limit_violation>(m, "LimitViolation")
            .def_property_readonly("subject_id", [](const limit_violation& v) {
                return v.subject_id;
            })
            .def_property_readonly("limit", [](const limit_violation& v) {
                return v.limit;
            })
            .def_property_readonly("value", [](const limit_violation& v) {
                return v.value;
            })
            .def("__repr__", [](const limit_violation & v) {
                return format("LimitViolation(subject_id='%s', limit=%f, value=%f)", v.subject_id, v.limit, v.value);
            });

    bindArray<gridpy::LimitViolationArray>(m, "LimitViolationArray");

    py::class_<security_analysis_result>(m, "SecurityAnalysisResult")
            .def_property_readonly("contingency_id", [](const security_analysis_result& r) {
                return r.contingency_id;
            })
            .def_property_readonly("status", [](const security_analysis_result& r) {
                return r.status;
            })
            .def_property_readonly("limit_violations", [](const security_analysis_result& r) {
                return gridpy::LimitViolationArray((array*) &r.limit_violations);
            })
            .def("__repr__", [](const security_analysis_result& r) {
                return format("SecurityAnalysisResult(contingency_id='%s', status='%s', limit_violations=...)", r.contingency_id, r.status);
            });

    bindArray<gridpy::SecurityAnalysisResultArray>(m, "SecurityAnalysisResultArray");

    m.def("run_security_analysis", &gridpy::runSecurityAnalysis, "Run a security analysis",
          py::arg("security_analysis_context"), py::arg("network"));

    m.def("destroy_object_handle", &gridpy::destroyObjectHandle, "Destroy Java object handle", py::arg("object_handle"));
}
