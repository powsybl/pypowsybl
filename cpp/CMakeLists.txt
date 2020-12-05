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

set(GRIDPY_JAVA_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../java)
set(GRIDPY_JAVA_BIN_DIR ${CMAKE_CURRENT_BINARY_DIR}/java)
set(GRIDPY_JAVA_OLD_LIB gridpy-java${CMAKE_SHARED_LIBRARY_SUFFIX})
set(GRIDPY_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}gridpy-java${CMAKE_SHARED_LIBRARY_SUFFIX})

# on MacOS, java library is created with an absolute path id, we need to fix it using install_name_tool before
# linking with our shared library
if(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${GRIDPY_JAVA_LIB} ${GRIDPY_JAVA_BIN_DIR}/${GRIDPY_JAVA_LIB})
endif()

ExternalProject_Add(mvn
    SOURCE_DIR ${GRIDPY_JAVA_SRC_DIR}
    PATCH_COMMAND mvn --batch-mode package
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
)

# as GraalVm does not follow same library naming convention (lib prefix is missing on Linux and MacOS) we need to rename
# it in the install command step
ExternalProject_Add(native-image
    DEPENDS mvn
    SOURCE_DIR ${GRIDPY_JAVA_BIN_DIR}
    DOWNLOAD_COMMAND ""
    PATCH_COMMAND $ENV{JAVA_HOME}/bin/native-image --class-path ${GRIDPY_JAVA_SRC_DIR}/target/gridpy-java.jar --no-fallback --allow-incomplete-classpath --shared -H:Name=gridpy-java -H:CLibraryPath=${GRIDPY_JAVA_SRC_DIR}/src/main/resources
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${GRIDPY_JAVA_BIN_DIR}/${GRIDPY_JAVA_OLD_LIB} ${GRIDPY_JAVA_BIN_DIR}/${GRIDPY_JAVA_LIB} ${INSTALL_EXTRA_COMMAND}
)

set(SOURCE_DIR "src")

include_directories(${SOURCE_DIR} ${GRIDPY_JAVA_BIN_DIR} ${GRIDPY_JAVA_SRC_DIR}/src/main/resources)
set(SOURCES "${SOURCE_DIR}/gridpy.cpp")

link_directories(${GRIDPY_JAVA_BIN_DIR})

add_subdirectory(lib/pybind11)
pybind11_add_module(_gridpy ${SOURCES} "${SOURCE_DIR}/bindings.cpp")

add_dependencies(_gridpy native-image)
target_link_libraries(_gridpy PRIVATE ${GRIDPY_JAVA_LIB})

# copy auxiliary java lib so that it can be installed with module one
if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET _gridpy POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy ${GRIDPY_JAVA_BIN_DIR}/${GRIDPY_JAVA_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
