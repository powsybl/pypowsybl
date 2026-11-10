/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Positional fast path: numeric columns exchanged with Java through numpy buffers, see pypowsybl/fast.py.
 */
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/numpy.h>

namespace pypowsybl {
class PYBIND11_EXPORT PyPowsyblError;
}
#include "powsybl-cpp.h"
#include "fast.h"

namespace py = pybind11;

namespace {

using pypowsybl::JavaHandle;

// Calls a C entry point without the per call hooks of PowsyblCaller (Python logger level synchronisation):
// these calls are meant to be made many times. The GIL is released while Java runs; no entry point of the fast
// path calls back into Python.
template<typename R, typename F, typename... ARGS>
R callFast(F f, ARGS... args) {
    exception_handler exc;
    exc.message = nullptr;
    R r;
    {
        py::gil_scoped_release release;
        pypowsybl::GraalVmGuard guard;
        r = f(guard.thread(), args..., &exc);
    }
    if (exc.message) {
        throw pypowsybl::PyPowsyblError(pypowsybl::toString(exc.message));
    }
    return r;
}

template<typename F, typename... ARGS>
void callFastVoid(F f, ARGS... args) {
    exception_handler exc;
    exc.message = nullptr;
    {
        py::gil_scoped_release release;
        pypowsybl::GraalVmGuard guard;
        f(guard.thread(), args..., &exc);
    }
    if (exc.message) {
        throw pypowsybl::PyPowsyblError(pypowsybl::toString(exc.message));
    }
}

// no py::array::forcecast: a buffer of the wrong dtype or layout is an error, never a silent copy
typedef py::array_t<double, py::array::c_style> DoubleArray;
typedef py::array_t<int, py::array::c_style> IntArray;

std::vector<std::string> toStrings(array* arrayPtr) {
    std::vector<std::string> strings;
    strings.reserve(arrayPtr->length);
    char** ptr = static_cast<char**>(arrayPtr->ptr);
    for (int i = 0; i < arrayPtr->length; i++) {
        strings.emplace_back(ptr[i] ? ptr[i] : "");
    }
    callFastVoid(::freeStringArray, arrayPtr);
    return strings;
}

JavaHandle create(JavaHandle network) {
    return JavaHandle(callFast<void*>(::createFastNetwork, static_cast<void*>(network)));
}

void refresh(JavaHandle fast) {
    callFastVoid(::refreshFastNetwork, static_cast<void*>(fast));
}

std::vector<std::string> typeNames() {
    return toStrings(callFast<array*>(::getFastTypeNames));
}

std::vector<std::string> columnDescriptions(JavaHandle fast, int type) {
    return toStrings(callFast<array*>(::getFastColumnDescriptions, static_cast<void*>(fast), type));
}

std::vector<std::string> ids(JavaHandle fast, int type) {
    return toStrings(callFast<array*>(::getFastIds, static_cast<void*>(fast), type));
}

int count(JavaHandle fast, int type) {
    return callFast<int>(::getFastCount, static_cast<void*>(fast), type);
}

int getDoubles(JavaHandle fast, int type, int attribute, DoubleArray out) {
    return callFast<int>(::getFastDoubles, static_cast<void*>(fast), type, attribute, out.mutable_data(), (int) out.size());
}

int getInts(JavaHandle fast, int type, int attribute, IntArray out) {
    return callFast<int>(::getFastInts, static_cast<void*>(fast), type, attribute, out.mutable_data(), (int) out.size());
}

int* positionsPtr(py::object& positions, IntArray& holder, py::ssize_t expectedSize) {
    if (positions.is_none()) {
        return nullptr;
    }
    holder = positions.cast<IntArray>();
    if (holder.size() != expectedSize) {
        throw pypowsybl::PyPowsyblError("positions and values must have the same length");
    }
    return holder.mutable_data();
}

void setDoubles(JavaHandle fast, int type, int attribute, py::object positions, DoubleArray values) {
    IntArray holder;
    int* pos = positionsPtr(positions, holder, values.size());
    callFastVoid(::setFastDoubles, static_cast<void*>(fast), type, attribute, pos, const_cast<double*>(values.data()), (int) values.size());
}

void setInts(JavaHandle fast, int type, int attribute, py::object positions, IntArray values) {
    IntArray holder;
    int* pos = positionsPtr(positions, holder, values.size());
    callFastVoid(::setFastInts, static_cast<void*>(fast), type, attribute, pos, const_cast<int*>(values.data()), (int) values.size());
}

void setDoubleAt(JavaHandle fast, int type, int attribute, int position, double value) {
    callFastVoid(::setFastDoubles, static_cast<void*>(fast), type, attribute, &position, &value, 1);
}

void setIntAt(JavaHandle fast, int type, int attribute, int position, int value) {
    callFastVoid(::setFastInts, static_cast<void*>(fast), type, attribute, &position, &value, 1);
}

int terminalCount(JavaHandle fast) {
    return callFast<int>(::getFastTerminalCount, static_cast<void*>(fast));
}

int applySwitchesAndGetTopology(JavaHandle fast, IntArray switchPositions, IntArray switchOpen, IntArray terminalBus, IntArray busComponent) {
    if (switchPositions.size() != switchOpen.size()) {
        throw pypowsybl::PyPowsyblError("switch positions and states must have the same length");
    }
    return callFast<int>(::applyFastSwitchesAndGetTopology, static_cast<void*>(fast),
                         const_cast<int*>(switchPositions.data()), const_cast<int*>(switchOpen.data()), (int) switchPositions.size(),
                         terminalBus.mutable_data(), (int) terminalBus.size(),
                         busComponent.mutable_data(), (int) busComponent.size());
}

int internalConnections(JavaHandle fast, IntArray out) {
    return callFast<int>(::getFastInternalConnections, static_cast<void*>(fast), out.mutable_data(), (int) (out.size() / 3));
}

}

void fastBindings(py::module_& m) {
    // noconvert: pybind11 would otherwise copy an array of another dtype and the caller's buffer would stay untouched
    m.def("fast_create", &create, py::arg("network"));
    m.def("fast_refresh", &refresh, py::arg("fast"));
    m.def("fast_type_names", &typeNames);
    m.def("fast_column_descriptions", &columnDescriptions, py::arg("fast"), py::arg("type"));
    m.def("fast_ids", &ids, py::arg("fast"), py::arg("type"));
    m.def("fast_count", &count, py::arg("fast"), py::arg("type"));
    m.def("fast_get_doubles", &getDoubles, py::arg("fast"), py::arg("type"), py::arg("attribute"), py::arg("out").noconvert());
    m.def("fast_get_ints", &getInts, py::arg("fast"), py::arg("type"), py::arg("attribute"), py::arg("out").noconvert());
    m.def("fast_set_doubles", &setDoubles, py::arg("fast"), py::arg("type"), py::arg("attribute"), py::arg("positions"), py::arg("values").noconvert());
    m.def("fast_set_ints", &setInts, py::arg("fast"), py::arg("type"), py::arg("attribute"), py::arg("positions"), py::arg("values").noconvert());
    m.def("fast_set_double_at", &setDoubleAt, py::arg("fast"), py::arg("type"), py::arg("attribute"), py::arg("position"), py::arg("value"));
    m.def("fast_set_int_at", &setIntAt, py::arg("fast"), py::arg("type"), py::arg("attribute"), py::arg("position"), py::arg("value"));
    m.def("fast_terminal_count", &terminalCount, py::arg("fast"));
    m.def("fast_apply_switches_and_get_topology", &applySwitchesAndGetTopology, py::arg("fast"),
          py::arg("switch_positions").noconvert(), py::arg("switch_open").noconvert(),
          py::arg("terminal_bus").noconvert(), py::arg("bus_component").noconvert());
    m.def("fast_internal_connections", &internalConnections, py::arg("fast"), py::arg("out").noconvert());
}
