#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl-cpp)


set(POWSYBL_CPP_SOURCE_DIR "../powsybl-cpp")
set(PYPOWSYBL_JAVA_BIN_DIR ${CMAKE_CURRENT_BINARY_DIR}/../java)
include_directories(${POWSYBL_CPP_SOURCE_DIR} ${PYPOWSYBL_JAVA_BIN_DIR})

link_directories(${PYPOWSYBL_JAVA_BIN_DIR})
pybind11_add_module(_pypowsybl pylogging.cpp bindings.cpp)

add_dependencies(_pypowsybl native-image math-native)
add_dependencies(math-native native-image) # because mvn command also copy math native jar

# Workaround for fixing the CI builds on Windows
# See https://github.com/actions/runner-images/issues/10004 for more details.
target_compile_definitions(_pypowsybl
   PRIVATE _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)

target_link_libraries(_pypowsybl PRIVATE powsybl-cpp)
