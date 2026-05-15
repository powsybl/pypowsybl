#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import logging
from typing import NamedTuple

TRACE_LEVEL = 5
logging.addLevelName(TRACE_LEVEL, "TRACE")


class BranchRow(NamedTuple):
    Index: str
    bus1_id: str
    bus2_id: str
    r: float
    x: float
    g1: float
    g2: float
    b1: float
    b2: float


class Transformer2WRow(BranchRow):
    r_at_current_tap: float
    x_at_current_tap: float
    g_at_current_tap: float
    b_at_current_tap: float
    rho: float
    alpha: float


class Transformer3WRow(NamedTuple):
    Index: str
    bus1_id: str
    bus2_id: str
    bus3_id: str
    r1_at_current_tap: float
    r2_at_current_tap: float
    r3_at_current_tap: float
    x1_at_current_tap: float
    x2_at_current_tap: float
    x3_at_current_tap: float
    g1_at_current_tap: float
    g2_at_current_tap: float
    g3_at_current_tap: float
    b1_at_current_tap: float
    b2_at_current_tap: float
    b3_at_current_tap: float
    rho1: float
    rho2: float
    rho3: float
    alpha1: float
    alpha2: float
    alpha3: float


class ConnectableRow(NamedTuple):
    Index: str
    bus_id: str


class LoadRow(ConnectableRow):
    p0: float
    q0: float


class BoundaryLineRow(ConnectableRow):
    r: float
    x: float
    g: float
    b: float
    p0: float
    q0: float


class GeneratorRow(ConnectableRow):
    target_q: float
    target_p: float
    min_p: float
    max_p: float


class HvdcRow(NamedTuple):
    Index: str
    converter_station1_id: str
    converter_station2_id: str
    converters_mode: str
    r: float
    nominal_v: float


class ConverterStationRow(HvdcRow):
    bus_id: str
    target_v: float
    target_p: float
    voltage_regulator_on: bool
    min_p: float
    max_p: float

class BusRow(NamedTuple):
    Index: str
    low_voltage_limit: float
    high_voltage_limit: float