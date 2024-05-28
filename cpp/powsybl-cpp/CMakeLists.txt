#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(powsybl-cpp)

include(ExternalProject)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)

# change shared library rpath to resolve java library in same directory
# only works on linux
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_INSTALL_RPATH "$ORIGIN")
set(POWSYBL_CPP_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}powsybl-cpp${CMAKE_SHARED_LIBRARY_SUFFIX})

if(NOT DEFINED PYPOWSYBL_JAVA_SRC_DIR)
    set(PYPOWSYBL_JAVA_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../java)
endif()
set(PYPOWSYBL_JAVA_BIN_DIR ${CMAKE_CURRENT_BINARY_DIR}/../java)
set(PYPOWSYBL_JAVA_OLD_LIB pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})
set(PYPOWSYBL_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})


if (${CMAKE_SYSTEM_PROCESSOR} STREQUAL "x86_64" OR ${CMAKE_SYSTEM_PROCESSOR} STREQUAL "AMD64")
    set(OS_BITS 64)
elseif(${CMAKE_SYSTEM_PROCESSOR} STREQUAL "arm64")
    set(OS_BITS arm64)
else()
    set(OS_BITS 32)
endif()

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
    set(POWSYBL_MATH_NATIVE_LIB math.dll)
    set(POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR natives/windows_${OS_BITS})
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
    set(POWSYBL_MATH_NATIVE_LIB libmath.so)
    set(POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR natives/linux_${OS_BITS})
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(POWSYBL_MATH_NATIVE_LIB libmath.dylib)
    set(POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR natives/osx_${OS_BITS})
else()
    message(FATAL_ERROR "System not supported: ${CMAKE_SYSTEM_NAME}")
endif()

# on MacOS, java library is created with an absolute path id, we need to fix it using install_name_tool before
# linking with our shared library
if(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(NATIVE_IMAGE_INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${PYPOWSYBL_JAVA_LIB} ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB})
    set(MATH_NATIVE_INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${POWSYBL_MATH_NATIVE_LIB} ${CMAKE_CURRENT_BINARY_DIR}/${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB})
endif()

ExternalProject_Add(mvn
    SOURCE_DIR ${PYPOWSYBL_JAVA_SRC_DIR}
    PATCH_COMMAND mvn --batch-mode clean package
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
)

if("${CMAKE_BUILD_TYPE}" STREQUAL "Debug")
    set(NATIVE_IMAGE_BUILD_OPTIONS "-H:GenerateDebugInfo=1" "-Ob")
endif()

# as of GraalVM 23.0.0 G1 GC is only supported on Linux ADM64
if(${CMAKE_SYSTEM_NAME} STREQUAL "Linux" AND ${OS_BITS} STREQUAL "64")
    set(NATIVE_IMAGE_GC "G1")
else()
    set(NATIVE_IMAGE_GC "serial")
endif()

# as GraalVm does not follow same library naming convention (lib prefix is missing on Linux and MacOS) we need to rename
# it in the install command step
ExternalProject_Add(native-image
    DEPENDS mvn
    SOURCE_DIR ${PYPOWSYBL_JAVA_BIN_DIR}
    DOWNLOAD_COMMAND ""
    PATCH_COMMAND $ENV{JAVA_HOME}/bin/native-image ${NATIVE_IMAGE_BUILD_OPTIONS} --class-path ${PYPOWSYBL_JAVA_SRC_DIR}/target/pypowsybl-java.jar${EXTRA_JARS} -march=compatibility --no-fallback --shared --gc=${NATIVE_IMAGE_GC} -H:Name=pypowsybl-java -H:CLibraryPath=${CMAKE_CURRENT_SOURCE_DIR}
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_OLD_LIB} ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${NATIVE_IMAGE_INSTALL_EXTRA_COMMAND}
)

# extract powsybl math native from jar for current platform
# powsybl-math-native.jar has been previously copied by maven build
add_custom_target(math-native ALL COMMAND ${CMAKE_COMMAND} -E tar x ${PYPOWSYBL_JAVA_SRC_DIR}/target/dependency/powsybl-math-native.jar ${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB} ${MATH_NATIVE_INSTALL_EXTRA_COMMAND})

include_directories(${CMAKE_CURRENT_SOURCE_DIR} ${PYPOWSYBL_JAVA_BIN_DIR})

link_directories(${PYPOWSYBL_JAVA_BIN_DIR})

set(POWSYBL_CPP_SOURCES "powsybl-cpp.cpp")

add_library(powsybl-cpp SHARED ${POWSYBL_CPP_SOURCES})
add_dependencies(powsybl-cpp native-image)

if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET powsybl-cpp POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:powsybl-cpp> ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_CURRENT_BINARY_DIR}/${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)


target_link_libraries(powsybl-cpp PUBLIC ${PYPOWSYBL_JAVA_LIB})
