#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl-cpp)

# Enable static linkage to prevent any future runtime binary compatibility issue
if(MSVC)
  set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")
  set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /MTd")
endif()

set(POWSYBL_CPP_SOURCE_DIR "../powsybl-cpp")
set(PYPOWSYBL_JAVA_SOURCE_DIR "../pypowsybl-java")
include_directories(${POWSYBL_CPP_SOURCE_DIR} ${PYPOWSYBL_JAVA_SOURCE_DIR} ${PYPOWSYBL_JAVA_INCLUDE_DIR})

link_directories(${PYPOWSYBL_JAVA_LIBRARY_DIR})
pybind11_add_module(_pypowsybl pylogging.cpp bindings.cpp)

add_dependencies(_pypowsybl native-image math-native)
add_dependencies(math-native native-image) # because mvn command also copy math native jar

# Fix related to issue describred here https://github.com/actions/runner-images/issues/10004#issuecomment-2156109231
# Should fix incompatibility between MSVC runtime 14.40.XXX and previous version
if(MSVC)
   target_compile_definitions(_pypowsybl
     PRIVATE _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
endif()

target_link_libraries(_pypowsybl PRIVATE powsybl-cpp)
