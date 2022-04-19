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

void logFromJava(int level, long timestamp, char* loggerName, char* message) {
    py::object logger = CppToPythonLogger::get()->getLogger();
    if (!logger.is_none()) {
        py::gil_scoped_acquire acquire;
        py::dict d("java_logger_name"_a=loggerName, "java_timestamp"_a=timestamp);
        CppToPythonLogger::get()->getLogger().attr("log")(level, message, "extra"_a=d);
    }
}

void setLogger(py::object& logger) {
    CppToPythonLogger::get()->setLogger(logger);
    auto fptr = &::logFromJava;
    pypowsybl::setupCallback(reinterpret_cast<void *&>(fptr));
}

py::object getLogger() {
    return CppToPythonLogger::get()->getLogger();
}
