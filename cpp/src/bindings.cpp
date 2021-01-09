/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
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

    m.def("destroy_object_handle", &gridpy::destroyObjectHandle, "Destroy Java object handle", py::arg("object_handle"));
}
