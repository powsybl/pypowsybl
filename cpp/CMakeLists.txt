cmake_minimum_required(VERSION 3.14)
project(gridpy-cpp)

include(ExternalProject)

set(CMAKE_CXX_STANDARD 11)

set(MAVEN_TARGET_DIR ${CMAKE_CURRENT_BINARY_DIR}/java)

ExternalProject_Add(gridpy-java
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../java/
    PATCH_COMMAND mvn -PalternateBuildDir -Dalt.build.dir=${MAVEN_TARGET_DIR} package
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
)

set(SOURCE_DIR "src")

include_directories(${SOURCE_DIR} ${MAVEN_TARGET_DIR})
set(SOURCES "${SOURCE_DIR}/gridpy.cpp")

link_directories(${MAVEN_TARGET_DIR})

add_subdirectory(lib/pybind11)
pybind11_add_module(gridpy ${SOURCES} "${SOURCE_DIR}/bindings.cpp")

add_dependencies(gridpy gridpy-java)
target_link_libraries(gridpy PRIVATE gridpy-java.so)
