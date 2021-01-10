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
            .def(py::init([](bool transformerVoltageControlOn, bool noGeneratorReactiveLimits, bool phaseShifterRegulationOn,
                    bool twtSplitShuntAdmittance, bool simulShunt, bool readSlackBus, bool writeSlackBus, bool distributedSlack) {
                auto parameters = new load_flow_parameters();
                parameters->transformer_voltage_control_on = transformerVoltageControlOn;
                parameters->no_generator_reactive_limits = noGeneratorReactiveLimits;
                parameters->phase_shifter_regulation_on = phaseShifterRegulationOn;
                parameters->twt_split_shunt_admittance = twtSplitShuntAdmittance;
                parameters->simul_shunt = simulShunt;
                parameters->read_slack_bus = readSlackBus;
                parameters->write_slack_bus = writeSlackBus;
                parameters->distributed_slack = distributedSlack;
                return parameters;
            }), py::arg("transformer_voltage_control_on") = false, py::arg("no_generator_reactive_limits") = false,
                 py::arg("phase_shifter_regulation_on") = false, py::arg("twt_split_shunt_admittance") = false,
                 py::arg("simul_shunt") = false, py::arg("read_slack_bus") = false,
                 py::arg("write_slack_bus") = false, py::arg("distributed_slack") = true)
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
