#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl-cpp)

#native-image math-native and powsybl-cpp targets
add_subdirectory(powsybl-cpp)

set(PYPOWSYBL_CPP_SOURCE_DIR "pypowsybl-cpp")
set(POWSYBL_CPP_SOURCE_DIR "powsybl-cpp")
set(PYPOWSYBL_JAVA_BIN_DIR ${CMAKE_CURRENT_BINARY_DIR}/java)
set(PYPOWSYBL_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})

include_directories(${PYPOWSYBL_CPP_SOURCE_DIR} ${POWSYBL_CPP_SOURCE_DIR} ${PYPOWSYBL_JAVA_BIN_DIR})

set(SOURCES ${POWSYBL_CPP_SOURCES} "${PYPOWSYBL_CPP_SOURCE_DIR}/pylogging.cpp")

link_directories(${PYPOWSYBL_JAVA_BIN_DIR})

add_subdirectory(lib/pybind11)
pybind11_add_module(_pypowsybl ${SOURCES} "${PYPOWSYBL_CPP_SOURCE_DIR}/bindings.cpp")

add_dependencies(_pypowsybl native-image math-native)
add_dependencies(math-native native-image) # because mvn command also copy math native jar
target_link_libraries(_pypowsybl PRIVATE powsybl-cpp)

# copy auxiliary java lib so that it can be installed with module one
if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET _pypowsybl POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_CURRENT_BINARY_DIR}/${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
