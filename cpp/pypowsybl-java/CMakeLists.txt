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

set(math-native-jar ${PYPOWSYBL_JAVA_SRC_DIR}/target/dependency/powsybl-math-native.jar)
set(ortools-jar ${PYPOWSYBL_JAVA_SRC_DIR}/target/dependency/ortools-${CMAKE_SYSTEM_NAME}-${ARCH_ID}.jar)

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
    extract_and_install_native_lib(${math-native-jar} natives/windows_${OS_BITS} math.dll)
    extract_and_install_native_lib(${ortools-jar} ortools-win32-${ARCH_ID} jniortools.dll)
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
    extract_and_install_native_lib(${math-native-jar} natives/linux_${OS_BITS} libmath.so)
    # FIXME libraries retrieved from ortool distributed jar are not ABI compatible with manylinux2014_x86_64
    # Thus making the audit on the wheel fail. To remove and replace by the following line when image is update
    #   extract_and_install_native_lib(${ortools-jar} ortools-linux-${ARCH_ID} libjniortools.so)
    #   extract_and_install_native_lib(${ortools-jar} ortools-linux-${ARCH_ID} libortools.so.9)
    # Thus download and package compatible libraries
    set(compatible-jar-path ${PYPOWSYBL_JAVA_SRC_DIR}/or-tools-centos-7)
    ExternalProject_Add(or-tools-centos-7
        URL "https://github.com/google/or-tools/releases/download/v9.10/or-tools_amd64_centos-7_java_v9.10.4067.tar.gz"
        URL_HASH SHA256=0bc48cab508f13d15c7245a47b598e6b31eca209ef2f4d45e5f15d21643e0d86
        SOURCE_DIR ${compatible-jar-path}
        CONFIGURE_COMMAND ""
        BUILD_COMMAND ""
        INSTALL_COMMAND ""
    )
    extract_and_install_native_lib(${compatible-jar-path}/ortools-linux-x86-64-9.10.4067.jar ortools-linux-x86-64 libjniortools.so)
    extract_and_install_native_lib(${compatible-jar-path}/ortools-linux-x86-64-9.10.4067.jar ortools-linux-x86-64 libortools.so.9)
    add_dependencies(native-install-libjniortools.so or-tools-centos-7)
    add_dependencies(native-install-libortools.so.9 or-tools-centos-7)
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

if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET native-image POST_BUILD ${POWSYBL_CPP_INSTALL_EXTRA_COMMAND} COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
