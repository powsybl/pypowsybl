#include "pylogging.h"
#include "pypowsybl.h"
#include <iostream>

using namespace pybind11::literals;

CppToPythonLogger *CppToPythonLogger::singleton_ = nullptr;
bool CppToPythonLogger::initialized_ = false;
std::mutex CppToPythonLogger::initMutex_;

CppToPythonLogger* CppToPythonLogger::get() {
  if (!singleton_) {
    std::lock_guard<std::mutex> guard(initMutex_);
    singleton_ = new CppToPythonLogger();
    initialized_ = false;
  }
  return singleton_;
}

void CppToPythonLogger::setLogger(py::object pPythonLogger) {
  std::lock_guard<std::mutex> guard(initMutex_);
  this->logger_ = pPythonLogger;
  initialized_ = true;
}

py::object CppToPythonLogger::getLogger() {
  return this->logger_;
}

bool CppToPythonLogger::loggerInitialized() {
  std::lock_guard<std::mutex> guard(initMutex_);
  return this->initialized_;
}

void CppToPythonLogger::logFromJava(int level, char* timestamp, char* loggerName, char* message) {
    py::gil_scoped_acquire acquire;
    if (CppToPythonLogger::get()->loggerInitialized()) {
        py::dict d("loggername"_a=loggerName, "timestamp"_a=timestamp);
        CppToPythonLogger::get()->getLogger().attr("log")(level, message, "extra"_a=d);
    }
}


void setLogger(py::object logger) {

    CppToPythonLogger::get()->setLogger(logger);
    auto fptr = &CppToPythonLogger::logFromJava;
    pypowsybl::setupCallback(reinterpret_cast<void *&>(fptr));
}

py::object getLogger() {

    if (CppToPythonLogger::get()->loggerInitialized()) {
        return CppToPythonLogger::get()->getLogger();
    }
    else {
        return py::object(py::cast(nullptr));
    }
}