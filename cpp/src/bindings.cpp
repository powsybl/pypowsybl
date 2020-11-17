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
          py::arg("id") = "Default");

    m.def("create_ieee14_network", &gridpy::createIeee14Network, "Create an IEEE 14 network");

    m.def("load_network", &gridpy::loadNetwork, "Load a network from a file");

    py::class_<gridpy::LoadFlowResult>(m, "LoadFlowResult")
        .def("is_ok", &gridpy::LoadFlowResult::isOk);

    m.def("run_load_flow", &gridpy::runLoadFlow, "Run a load flow", py::arg("network"));

    py::class_<bus>(m, "Bus")
        .def("get_id", [](const bus& b) {
            return b.id;
        })
        .def("get_v_magnitude", [](const bus& b) {
            return b.v_magnitude;
        })
        .def("get_v_angle", [](const bus& b) {
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

    m.def("destroy_object_handle", &gridpy::destroyObjectHandle, "Destroy Java object handle", py::arg("object_handle"));
}
