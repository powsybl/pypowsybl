#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(gridpy-cpp)

include(ExternalProject)

set(CMAKE_MACOSX_RPATH 1)
set(CMAKE_CXX_STANDARD 11)

# no need to do anything on windows as current path is automatically used
if(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
    set(RPATH "$ORIGIN")
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(RPATH "@loader_path")
endif()

# change shared librairy rpath to resolve java library in same directory
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_INSTALL_RPATH ${RPATH})

set(MAVEN_TARGET_DIR ${CMAKE_CURRENT_BINARY_DIR}/java)

ExternalProject_Add(gridpy-java
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../java/
    PATCH_COMMAND mvn -PalternateBuildDir -Dalt.build.dir=${MAVEN_TARGET_DIR} package
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
)
set(GRIDPY_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}gridpy-java${CMAKE_SHARED_LIBRARY_SUFFIX})

set(SOURCE_DIR "src")

include_directories(${SOURCE_DIR} ${MAVEN_TARGET_DIR})
set(SOURCES "${SOURCE_DIR}/gridpy.cpp")

link_directories(${MAVEN_TARGET_DIR})

add_subdirectory(lib/pybind11)
pybind11_add_module(gridpy ${SOURCES} "${SOURCE_DIR}/bindings.cpp")

add_dependencies(gridpy gridpy-java)
target_link_libraries(gridpy PRIVATE ${GRIDPY_JAVA_LIB})

# copy auxiliary java lib so that it can be installed with module one
if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET gridpy POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy ${MAVEN_TARGET_DIR}/${GRIDPY_JAVA_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
