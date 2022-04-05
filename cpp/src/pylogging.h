#include <pybind11/pybind11.h>

namespace py = pybind11;

class CppToPythonLogger
{
  public:
    static CppToPythonLogger* get() {
      if (!singleton_) {
        singleton_ = new CppToPythonLogger();
        initialized_ = false;
      }
      return singleton_;
    }

    static void logFromJava(int level, int timestamp, char* loggerName, char* message);

    void setLogger(py::object pPythonLogger) {
      this->logger_ = pPythonLogger;
      initialized_ = true;
    }

    py::object getLogger() {
      return this->logger_;
    }

    bool loggerInitialized() {
      return this->initialized_;
    }

  private:
    static CppToPythonLogger* singleton_;
    py::object logger_;
    static bool initialized_;
};

void setLogger(py::object logger);

py::object getLogger();