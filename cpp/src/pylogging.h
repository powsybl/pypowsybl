/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <pybind11/pybind11.h>
#include <mutex>
#include "pypowsybl.h"

class MyPypowsyblJavaCaller {
public:
  template<typename F, typename... ARGS>
  static void callJava(F f, ARGS... args) {
      pypowsybl::GraalVmGuard guard;
      exception_handler exc;

      pypowsybl::setLogLevelFromPythonLogger(guard.thread(), &exc);

      f(guard.thread(), args..., &exc);
      if (exc.message) {
          throw pypowsybl::PyPowsyblError(pypowsybl::toString(exc.message));
      }
      {
          py::gil_scoped_acquire acquire;
          if (PyErr_Occurred() != nullptr) {
              throw py::error_already_set();
          }
      }
  }

  template<typename T, typename F, typename... ARGS>
  static T callJava(F f, ARGS... args) {
      pypowsybl::GraalVmGuard guard;
      exception_handler exc;

      pypowsybl::setLogLevelFromPythonLogger(guard.thread(), &exc);

      auto r = f(guard.thread(), args..., &exc);
      if (exc.message) {
          throw pypowsybl::PyPowsyblError(pypowsybl::toString(exc.message));
      }
      {
          py::gil_scoped_acquire acquire;
          if (PyErr_Occurred() != nullptr) {
              throw py::error_already_set();
          }
      }
      return r;
  }
};

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

void logFromJava(int level, long timestamp, char* loggerName, char* message);

void setLogger(py::object& logger);

py::object getLogger();
