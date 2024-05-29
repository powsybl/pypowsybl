#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
cmake_minimum_required(VERSION 3.14)
project(pypowsybl)

#native-image math-native and powsybl-cpp targets
add_subdirectory(powsybl-cpp)
add_subdirectory(lib/pybind11)
add_subdirectory(pypowsybl-cpp)