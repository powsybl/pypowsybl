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

# change shared library rpath to resolve java library in same directory
# only works on linux
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_INSTALL_RPATH "$ORIGIN")

set(MAVEN_TARGET_DIR ${CMAKE_CURRENT_BINARY_DIR}/java)
set(GRIDPY_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}gridpy-java${CMAKE_SHARED_LIBRARY_SUFFIX})
set(GRIDPY_JAVA_OLD_LIB gridpy-java${CMAKE_SHARED_LIBRARY_SUFFIX})

# on MacOS, java library is created with an absolute path id, we need to fix it using install_name_tool before
# linking with our shared library
if(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${GRIDPY_JAVA_LIB} ${MAVEN_TARGET_DIR}/${GRIDPY_JAVA_LIB})
endif()

# as GraalVm does not follow same library naming convention (lib prefix is missing on Linux and MacOS) we need to rename
# it in the install command step
ExternalProject_Add(gridpy-java
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../java/
    PATCH_COMMAND mvn --batch-mode -PalternateBuildDir -Dalt.build.dir=${MAVEN_TARGET_DIR} package
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${MAVEN_TARGET_DIR}/${GRIDPY_JAVA_OLD_LIB} ${MAVEN_TARGET_DIR}/${GRIDPY_JAVA_LIB} ${INSTALL_EXTRA_COMMAND}
)

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
