#include "pylogging.h"
#include "pypowsybl.h"

CppToPythonLogger *CppToPythonLogger::singleton_ = nullptr;
bool CppToPythonLogger::initialized_ = false;

void CppToPythonLogger::logFromJava(int level, int timestamp, char* loggerName, char* message) {

    py::gil_scoped_acquire acquire;
    if (CppToPythonLogger::get()->loggerInitialized()) {
        CppToPythonLogger::get()->getLogger().attr("log")(level, message);
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