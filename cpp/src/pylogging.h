#include <pybind11/pybind11.h>
#include <mutex>

namespace py = pybind11;

class CppToPythonLogger
{
  public:
    static CppToPythonLogger* get();

    static void logFromJava(int level, char* timestamp, char* loggerName, char* message);

    void setLogger(py::object pPythonLogger);

    py::object getLogger();

    bool loggerInitialized();

  private:
    static CppToPythonLogger* singleton_;
    py::object logger_;
    static bool initialized_;
    static std::mutex initMutex_;
};

void setLogger(py::object logger);

py::object getLogger();