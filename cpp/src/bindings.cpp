/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include "gridpy.h"

namespace py = pybind11;

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

    py::class_<gridpy::LoadFlowResult>(m, "LoadFlowResult")
        .def_property_readonly("ok", &gridpy::LoadFlowResult::isOk);

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
        });

    py::class_<gridpy::BusArray>(m, "BusArray")
        .def("__len__", [](const gridpy::BusArray& a) {
            return a.length();
        })
        .def("__iter__", [](gridpy::BusArray& a) {
            return py::make_iterator(a.begin(), a.end());
        }, py::keep_alive<0, 1>());

    m.def("get_buses", &gridpy::getBusArray, "Get network buses", py::arg("network"),
          py::arg("bus_breaker_view"));

    m.def("write_single_line_diagram_svg", &gridpy::writeSingleLineDiagramSvg, "Write single line diagram SVG",
          py::arg("network"), py::arg("container_id"), py::arg("svg_file"));

    m.def("destroy_object_handle", &gridpy::destroyObjectHandle, "Destroy Java object handle", py::arg("object_handle"));
}
