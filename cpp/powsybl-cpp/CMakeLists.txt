#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.20)
project(powsybl-cpp)

set(CMAKE_CXX_STANDARD 17)
include(ExternalProject)

if(NOT PYPOWSYBL_JAVA_LIBRARY_DIR OR NOT PYPOWSYBL_JAVA_INCLUDE_DIR)
  message( FATAL_ERROR "PYPOWSYBL_JAVA_LIBRARY_DIR or PYPOWSYBL_JAVA_INCLUDE_DIR is undefined. Cannot build powsybl-cpp")
endif()

# Enable static linkage to prevent any future runtime binary compatibility issue
if(MSVC)
  set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")
  set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /MTd")
  set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
endif()

set(POWSYBL_CPP_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}powsybl-cpp${CMAKE_SHARED_LIBRARY_SUFFIX})
set(PYPOWSYBL_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})

# on MacOS, java library is created with an absolute path id, we need to fix it using install_name_tool before
# linking with our shared library
if(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(POWSYBL_CPP_INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${POWSYBL_CPP_LIB} $<TARGET_FILE:powsybl-cpp>)
endif()

include_directories(${CMAKE_CURRENT_SOURCE_DIR} ${PYPOWSYBL_JAVA_INCLUDE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/../pypowsybl-java)
link_directories(${PYPOWSYBL_JAVA_LIBRARY_DIR})

set(POWSYBL_CPP_SOURCES "powsybl-cpp.cpp")

add_library(powsybl-cpp SHARED ${POWSYBL_CPP_SOURCES})

add_dependencies(powsybl-cpp native-image)

if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET powsybl-cpp POST_BUILD ${POWSYBL_CPP_INSTALL_EXTRA_COMMAND} COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:powsybl-cpp> $<TARGET_LINKER_FILE:powsybl-cpp> ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_CURRENT_BINARY_DIR}/${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)

# Fix related to issue described here https://github.com/actions/runner-images/issues/10004#issuecomment-2156109231
# Should fix incompatibility between MSVC runtime 14.40.XXX and previous version
if(MSVC)
   target_compile_definitions(powsybl-cpp
     PRIVATE _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR)
endif()

target_link_libraries(powsybl-cpp PUBLIC ${PYPOWSYBL_JAVA_LIB})
install(TARGETS powsybl-cpp)
