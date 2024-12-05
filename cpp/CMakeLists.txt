#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)

option(BUILD_PYPOWSYBL_JAVA "Build graalvm based powsybl java c api" ON)
option(BUILD_POWSYBL_CPP "Build cpp powsybl api" ON)
option(BUILD_PYTHON_BINDINGS "Build python bindings" ON)

# change shared library rpath to resolve java library in same directory
# only works on linux
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_INSTALL_RPATH "$ORIGIN")

if(BUILD_PYPOWSYBL_JAVA)
  #Build mvn, native-image and math-native targets
  add_subdirectory(pypowsybl-java)
  set(PYPOWSYBL_JAVA_LIBRARY_DIR ${PYPOWSYBL_JAVA_BIN_DIR})
  set(PYPOWSYBL_JAVA_INCLUDE_DIR ${PYPOWSYBL_JAVA_BIN_DIR})
else()
  add_custom_target(mvn ALL COMMAND echo "BUILD_PYPOWSYBL_JAVA set to OFF, mvn target will not be built.")
  add_custom_target(native-image ALL COMMAND echo "BUILD_PYPOWSYBL_JAVA set to OFF, native-image target will not be built.")
  add_custom_target(math-native ALL COMMAND echo "BUILD_PYPOWSYBL_JAVA set to OFF, math-native target will not be built.")
endif()

if(BUILD_POWSYBL_CPP)
  #Build powsybl cpp target
  add_subdirectory(powsybl-cpp)
endif()

if(BUILD_PYTHON_BINDINGS)
  #Build pythin bindings
  set(PYBIND11_FINDPYTHON ON)
  find_package(pybind11 CONFIG REQUIRED)
  add_subdirectory(pypowsybl-cpp)
endif()