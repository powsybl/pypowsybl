#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.20)
project(pypowsybl-java)

set(CMAKE_CXX_STANDARD 17)

include(ExternalProject)

if(NOT DEFINED PYPOWSYBL_JAVA_SRC_DIR)
    set(PYPOWSYBL_JAVA_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../java/pypowsybl)
endif()
set(PYPOWSYBL_JAVA_BIN_DIR ${CMAKE_CURRENT_BINARY_DIR}/../java)
set(PYPOWSYBL_JAVA_BIN_DIR ${PYPOWSYBL_JAVA_BIN_DIR}/../java PARENT_SCOPE)
set(PYPOWSYBL_JAVA_OLD_LIB pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})
set(PYPOWSYBL_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})

if (${CMAKE_SYSTEM_PROCESSOR} STREQUAL "x86_64" OR ${CMAKE_SYSTEM_PROCESSOR} STREQUAL "AMD64")
    set(OS_BITS 64)
    set(ARCH_ID "x86-64")
elseif(${CMAKE_SYSTEM_PROCESSOR} STREQUAL "arm64")
    set(OS_BITS arm64)
    set(ARCH_ID "aarch64")
else()
    set(OS_BITS 32)
    set(ARCH_ID "x86-32")
endif()

# extract needed native libraries from jar for current platform
# jars have been previously copied by maven build
macro(extract_and_install_native_lib jar_file jar_entry library_file)
  if(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(NATIVE_LIB_INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${library_file} ${CMAKE_CURRENT_BINARY_DIR}/${jar_entry}/${library_file})
  endif()

  add_custom_target(native-install-${library_file} ALL COMMAND ${CMAKE_COMMAND} -E tar x ${jar_file} ${jar_entry}/${library_file} ${NATIVE_LIB_INSTALL_EXTRA_COMMAND})
  add_dependencies(native-install-${library_file} mvn)
  if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET native-install-${library_file} POST_BUILD ${POWSYBL_CPP_INSTALL_EXTRA_COMMAND} COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_BINARY_DIR}/${jar_entry}/${library_file} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
  endif()
endmacro()

cmake_path(SET math-native-jar "${PYPOWSYBL_JAVA_SRC_DIR}/target/dependency/powsybl-math-native.jar")
cmake_path(SET ortools-jar "${PYPOWSYBL_JAVA_SRC_DIR}/target/dependency/ortools-${CMAKE_SYSTEM_NAME}-${ARCH_ID}.jar")

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
    extract_and_install_native_lib(${math-native-jar} natives/windows_${OS_BITS} math.dll)
    extract_and_install_native_lib(${ortools-jar} ortools-win32-${ARCH_ID} jniortools.dll)
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
    extract_and_install_native_lib(${math-native-jar} natives/linux_${OS_BITS} libmath.so)
    extract_and_install_native_lib(${ortools-jar} ortools-linux-${ARCH_ID} libjniortools.so)
    extract_and_install_native_lib(${ortools-jar} ortools-linux-${ARCH_ID} libortools.so.9)
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    extract_and_install_native_lib(${math-native-jar} natives/osx_${OS_BITS} libmath.dylib)
    extract_and_install_native_lib(${ortools-jar} ortools-darwin-${ARCH_ID} libjniortools.dylib)
    extract_and_install_native_lib(${ortools-jar} ortools-darwin-${ARCH_ID} libortools.9.dylib)
else()
    message(FATAL_ERROR "System not supported: ${CMAKE_SYSTEM_NAME}")
endif()

# on MacOS, java library is created with an absolute path id, we need to fix it using install_name_tool before
# linking with our shared library
if(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(NATIVE_IMAGE_INSTALL_EXTRA_COMMAND COMMAND install_name_tool -id @loader_path/${PYPOWSYBL_JAVA_LIB} ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB})
endif()

# Maven command
if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
    set(MAVEN_CMD "${CMAKE_CURRENT_SOURCE_DIR}/../../java/mvnw.cmd")
else()
    set(MAVEN_CMD "${CMAKE_CURRENT_SOURCE_DIR}/../../java/mvnw")
endif()

ExternalProject_Add(mvn
    SOURCE_DIR ${PYPOWSYBL_JAVA_SRC_DIR}
    PATCH_COMMAND ${MAVEN_CMD} -B -ntp clean package
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
)

if("${CMAKE_BUILD_TYPE}" STREQUAL "Debug")
    set(NATIVE_IMAGE_BUILD_OPTIONS "-H:GenerateDebugInfo=1" "-Ob")
endif()

# Profile-guided optimization (PGO), requires Oracle GraalVM (not Community Edition).
# This is a two-pass build:
#   1. PYPOWSYBL_PGO_INSTRUMENT=ON builds an instrumented library that writes a profile (default.iprof in
#      the working directory) when the native image shutdown hooks run. native-image disables these hooks
#      in shared libraries by default, ProfilingEnableProfileDumpHooks turns them back on, and pypowsybl
#      runs them from closePypowsybl when PYPOWSYBL_RUN_SHUTDOWN_HOOKS is set (tearing down the isolate
#      does not run them).
#   2. PYPOWSYBL_PGO_PROFILES=<profile.iprof>[,<other.iprof>] builds the optimized library using
#      the collected profile(s).
# See tools/pgo/build_pgo.sh for a driver that runs both passes with the training workload.
option(PYPOWSYBL_PGO_INSTRUMENT "Build an instrumented native image to collect PGO profiles" OFF)
set(PYPOWSYBL_PGO_PROFILES "" CACHE STRING "Comma-separated list of .iprof files to build a PGO optimized native image")
if(PYPOWSYBL_PGO_INSTRUMENT AND NOT "${PYPOWSYBL_PGO_PROFILES}" STREQUAL "")
    message(FATAL_ERROR "PYPOWSYBL_PGO_INSTRUMENT and PYPOWSYBL_PGO_PROFILES are mutually exclusive")
endif()
if(PYPOWSYBL_PGO_INSTRUMENT)
    message(STATUS "Building PGO instrumented native image")
    list(APPEND NATIVE_IMAGE_BUILD_OPTIONS "--pgo-instrument" "-H:+UnlockExperimentalVMOptions" "-H:+ProfilingEnableProfileDumpHooks" "-H:-UnlockExperimentalVMOptions")
elseif(NOT "${PYPOWSYBL_PGO_PROFILES}" STREQUAL "")
    message(STATUS "Building PGO optimized native image with profiles: ${PYPOWSYBL_PGO_PROFILES}")
    list(APPEND NATIVE_IMAGE_BUILD_OPTIONS "--pgo=${PYPOWSYBL_PGO_PROFILES}")
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
    PATCH_COMMAND $ENV{JAVA_HOME}/bin/native-image ${NATIVE_IMAGE_BUILD_OPTIONS} --class-path ${PYPOWSYBL_JAVA_SRC_DIR}/target/pypowsybl-java.jar${EXTRA_JARS} -march=compatibility --no-fallback --shared --gc=${NATIVE_IMAGE_GC} -o pypowsybl-java -H:CLibraryPath=${CMAKE_CURRENT_SOURCE_DIR} --add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.c=ALL-UNNAMED
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_OLD_LIB} ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${NATIVE_IMAGE_INSTALL_EXTRA_COMMAND}
)

if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET native-image POST_BUILD ${POWSYBL_CPP_INSTALL_EXTRA_COMMAND} COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
