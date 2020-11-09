#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
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
set(GRIDPY_JAVA_LIB gridpy-java${CMAKE_SHARED_LIBRARY_SUFFIX})

set(SOURCE_DIR "src")

include_directories(${SOURCE_DIR} ${MAVEN_TARGET_DIR})
set(SOURCES "${SOURCE_DIR}/gridpy.cpp")

link_directories(${MAVEN_TARGET_DIR})

add_subdirectory(lib/pybind11)
pybind11_add_module(gridpy ${SOURCES} "${SOURCE_DIR}/bindings.cpp")

add_dependencies(gridpy gridpy-java)
target_link_libraries(gridpy PRIVATE ${GRIDPY_JAVA_LIB})
