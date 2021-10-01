#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl-cpp)

include(ExternalProject)

set(CMAKE_CXX_STANDARD 11)

# change shared library rpath to resolve java library in same directory
# only works on linux
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_INSTALL_RPATH "$ORIGIN")

set(PYPOWSYBL_JAVA_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../java)
set(PYPOWSYBL_JAVA_BIN_DIR ${CMAKE_CURRENT_BINARY_DIR}/java)
set(PYPOWSYBL_JAVA_OLD_LIB pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})
set(PYPOWSYBL_JAVA_LIB ${CMAKE_SHARED_LIBRARY_PREFIX}pypowsybl-java${CMAKE_SHARED_LIBRARY_SUFFIX})

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
    set(POWSYBL_MATH_NATIVE_LIB math.dll)
    set(POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR natives/windows_64)
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
    set(POWSYBL_MATH_NATIVE_LIB libmath.so)
    set(POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR natives/linux_64)
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
    set(POWSYBL_MATH_NATIVE_LIB libmath.dylib)
    set(POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR natives/osx_64)
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

if(${CMAKE_SYSTEM_NAME} STREQUAL "Linux" AND ${CMAKE_BUILD_TYPE} STREQUAL "Debug") 
    set(NATIVE_IMAGE_BUILD_OPTIONS "-H:GenerateDebugInfo=1 ")
endif()

# as GraalVm does not follow same library naming convention (lib prefix is missing on Linux and MacOS) we need to rename
# it in the install command step
ExternalProject_Add(native-image
    DEPENDS mvn
    SOURCE_DIR ${PYPOWSYBL_JAVA_BIN_DIR}
    DOWNLOAD_COMMAND ""
    PATCH_COMMAND $ENV{JAVA_HOME}/bin/native-image ${NATIVE_IMAGE_BUILD_OPTIONS} --class-path ${PYPOWSYBL_JAVA_SRC_DIR}/target/pypowsybl-java.jar${EXTRA_JARS} --no-fallback --allow-incomplete-classpath --shared -H:Name=pypowsybl-java -H:CLibraryPath=${PYPOWSYBL_JAVA_SRC_DIR}/src/main/resources
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_OLD_LIB} ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${NATIVE_IMAGE_INSTALL_EXTRA_COMMAND}
)

set(SOURCE_DIR "src")

include_directories(${SOURCE_DIR} ${PYPOWSYBL_JAVA_BIN_DIR} ${PYPOWSYBL_JAVA_SRC_DIR}/src/main/resources)
set(SOURCES "${SOURCE_DIR}/pypowsybl.cpp")

link_directories(${PYPOWSYBL_JAVA_BIN_DIR})

add_subdirectory(lib/pybind11)
pybind11_add_module(_pypowsybl ${SOURCES} "${SOURCE_DIR}/bindings.cpp")

# extract powsybl math native from jar for current platform
# powsybl-math-native.jar has been previously copied by maven build
add_custom_target(math-native ALL COMMAND ${CMAKE_COMMAND} -E tar x ${PYPOWSYBL_JAVA_SRC_DIR}/target/dependency/powsybl-math-native.jar ${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB} ${MATH_NATIVE_INSTALL_EXTRA_COMMAND})

add_dependencies(_pypowsybl native-image math-native)
add_dependencies(math-native native-image) # because mvn command also copy math native jar
target_link_libraries(_pypowsybl PRIVATE ${PYPOWSYBL_JAVA_LIB})

# copy auxiliary java lib so that it can be installed with module one
if(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
    add_custom_command(TARGET _pypowsybl POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy ${PYPOWSYBL_JAVA_BIN_DIR}/${PYPOWSYBL_JAVA_LIB} ${CMAKE_CURRENT_BINARY_DIR}/${POWSYBL_MATH_NATIVE_JAR_ENTRY_DIR}/${POWSYBL_MATH_NATIVE_LIB} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
endif(DEFINED CMAKE_LIBRARY_OUTPUT_DIRECTORY)
