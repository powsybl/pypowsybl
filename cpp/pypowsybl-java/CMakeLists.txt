#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl-java)

set(CMAKE_CXX_STANDARD 17)

include(ExternalProject)

if(NOT DEFINED PYPOWSYBL_JAVA_SRC_DIR)
    set(PYPOWSYBL_JAVA_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../java)
endif()
set(PYPOWSYBL_DATA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../data)
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

cmake_path(SET math-native-jar "${PYPOWSYBL_JAVA_SRC_DIR}/pypowsybl/target/dependency/powsybl-math-native.jar")
cmake_path(SET ortools-jar "${PYPOWSYBL_JAVA_SRC_DIR}/pypowsybl/target/dependency/ortools-${CMAKE_SYSTEM_NAME}-${ARCH_ID}.jar")

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

if(DEFINED ENV{PYPOWSYBL_PGO})
    set(NATIVE_IMAGE_EXTRA_DEPENDS "native-image-pgo")
    set(NATIVE_IMAGE_PGO_OPTION "--pgo=${PYPOWSYBL_JAVA_BIN_DIR}/default.iprof")
    # first build a profiling binary, run it to generate an iprof file
    # and then pass the iprof file to the library build
    ExternalProject_Add(native-image-pgo
        DEPENDS mvn
        SOURCE_DIR ${PYPOWSYBL_JAVA_BIN_DIR}
        DOWNLOAD_COMMAND ""
        PATCH_COMMAND $ENV{JAVA_HOME}/bin/native-image ${NATIVE_IMAGE_BUILD_OPTIONS} --class-path ${PYPOWSYBL_JAVA_SRC_DIR}/pypowsybl-pgo/target/pypowsybl-java-pgo.jar${EXTRA_JARS} --pgo-instrument -march=compatibility --no-fallback --gc=${NATIVE_IMAGE_GC} -H:Name=pypowsybl-java-pgo -H:CLibraryPath=${CMAKE_CURRENT_SOURCE_DIR} com.powsybl.python.pgo.Main
        CONFIGURE_COMMAND ""
        BUILD_COMMAND ""
        INSTALL_COMMAND ${PYPOWSYBL_JAVA_BIN_DIR}/pypowsybl-java-pgo ${PYPOWSYBL_DATA_DIR} COMMAND ${CMAKE_COMMAND} -E copy default.iprof ${PYPOWSYBL_JAVA_BIN_DIR}/
    )
endif()

# as GraalVm does not follow same library naming convention (lib prefix is missing on Linux and MacOS) we need to rename
# it in the install command step
ExternalProject_Add(native-image
    DEPENDS mvn ${NATIVE_IMAGE_EXTRA_DEPENDS}
    SOURCE_DIR ${PYPOWSYBL_JAVA_BIN_DIR}
    DOWNLOAD_COMMAND ""
    PATCH_COMMAND $ENV{JAVA_HOME}/bin/native-image ${NATIVE_IMAGE_BUILD_OPTIONS} --class-path ${PYPOWSYBL_JAVA_SRC_DIR}/pypowsybl/target/pypowsybl-java.jar${EXTRA_JARS} ${NATIVE_IMAGE_PGO_OPTION} -march=compatibility --no-fallback --shared --gc=${NATIVE_IMAGE_GC} -H:Name=pypowsybl-java -H:CLibraryPath=${CMAKE_CURRENT_SOURCE_DIR}
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_OLD_LIB} ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${NATIVE_IMAGE_INSTALL_EXTRA_COMMAND}
)

if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET native-image POST_BUILD ${POWSYBL_CPP_INSTALL_EXTRA_COMMAND} COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
