/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include "pylogging.h"
#include "pypowsybl.h"
#include <iostream>

using namespace pybind11::literals;

CppToPythonLogger *CppToPythonLogger::singleton_ = nullptr;
std::mutex CppToPythonLogger::initMutex_;

CppToPythonLogger* CppToPythonLogger::get() {
    std::lock_guard<std::mutex> guard(initMutex_);
    if (!singleton_) {
        singleton_ = new CppToPythonLogger();
    }
    return singleton_;
}

CppToPythonLogger::CppToPythonLogger()
    : logger_(py::none()) {
}

void CppToPythonLogger::setLogger(py::object& logger) {
    std::lock_guard<std::mutex> guard(loggerMutex_);
    logger_ = logger;
}

py::object CppToPythonLogger::getLogger() {
    std::lock_guard<std::mutex> guard(loggerMutex_);
    return logger_;
}

/// Saves error and restores it at the end of the scope,
/// unless another one has been set in the meantime.
struct save_python_error {
    PyObject *type, *value, *trace;
    save_python_error() { PyErr_Fetch(&type, &value, &trace); }

    ~save_python_error() {
        if (PyErr_Occurred() == nullptr) {
            PyErr_Restore(type, value, trace); 
        }
    }
};

void logFromJava(int level, long timestamp, char* loggerName, char* message) {
    py::gil_scoped_acquire acquire;
    save_python_error previousError;  // to keep and restore the previously set exception, if any
    py::object logger = CppToPythonLogger::get()->getLogger();
    if (!logger.is_none()) {
        try {
          py::dict d("java_logger_name"_a=loggerName, "java_timestamp"_a=timestamp);
          CppToPythonLogger::get()->getLogger().attr("log")(level, message, "extra"_a=d);
        } catch (py::error_already_set& err) {
          err.restore();
        }
    }
}

void setLogger(py::object& logger) {
    CppToPythonLogger::get()->setLogger(logger);
    auto fptr = &::logFromJava;
    pypowsybl::PowsyblInterface<MyPypowsyblJavaCaller>::setupLoggerCallback(reinterpret_cast<void *&>(fptr));
}

py::object getLogger() {
    return CppToPythonLogger::get()->getLogger();
}
