/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include <mutex>

namespace py = pybind11;

class CppToPythonLogger {
public:
    CppToPythonLogger();

    static CppToPythonLogger* get();

    void setLogger(py::object& logger);

    py::object getLogger();

private:
    py::object logger_;
    std::mutex loggerMutex_;

    static CppToPythonLogger* singleton_;
    static std::mutex initMutex_;
};

int logFromJava(int level, long timestamp, char* loggerName, char* message);

void setLogger(py::object& logger);

py::object getLogger();

void dummyLong();
