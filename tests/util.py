# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from pypowsybl.network import Network
import pypowsybl as pp

def create_battery_network() -> Network:
    return pp.network._create_network('batteries')


def create_dangling_lines_network() -> Network:
    return pp.network._create_network('dangling_lines')


def create_three_windings_transformer_network() -> Network:
    return pp.network._create_network('three_windings_transformer')


def create_non_linear_shunt_network() -> Network:
    return pp.network._create_network('non_linear_shunt')

def create_three_windings_transformer_with_current_limits_network() -> Network:
    return pp.network._create_network('three_windings_transformer_with_current_limits')