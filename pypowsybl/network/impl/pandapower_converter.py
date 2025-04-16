# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import math
import typing
from enum import Enum
from importlib import util
from typing import Dict

import numpy as np
import pandas as pd
from pandas import Series
from pandas import DataFrame
import pypowsybl._pypowsybl as _pp
try:
    from pandapower import pandapowerNet
except ImportError:
    pandapowerNet = any

from .network import Network
from .network_creation_util import create_empty

MIN_TARGET_P_TO_NOT_BE_DISCADED_FROM_ACTIVE_POWER_CONTROL = 0.0001

DEFAULT_MIN_P = -4999.0
DEFAULT_MAX_P = 4999.0


def convert_from_pandapower(n_pdp: pandapowerNet) -> Network:
    if util.find_spec("pandapower") is None:
        raise _pp.PyPowsyblError("pandapower is not installed")

    n = create_empty(n_pdp.name if n_pdp.name else 'network')

    # create one giant substation
    n.create_substations(id='s')
    create_buses(n, n_pdp)
    create_loads(n, n_pdp)
    slack_weight_by_gen_id: Dict[str, float] = {}
    create_generators(n, n_pdp, slack_weight_by_gen_id)
    create_shunts(n, n_pdp)
    create_lines(n, n_pdp)
    create_transformers(n, n_pdp)
    create_extensions(n, slack_weight_by_gen_id)

    return n


def create_extensions(n: Network, slack_weight_by_gen_id: Dict[str, float]) -> None:
    if len(slack_weight_by_gen_id) > 0:
        highest_weight_gen_id = max(slack_weight_by_gen_id, key=lambda k: slack_weight_by_gen_id[k])
        for index, (gen_id, weight) in enumerate(slack_weight_by_gen_id.items()):
            if gen_id == highest_weight_gen_id:
                # create slack bus extension for first one
                generators = n.get_generators(attributes=['voltage_level_id'])
                slack_gen = generators.loc[gen_id]
                n.create_extensions(extension_name='slackTerminal', element_id=gen_id,
                                    voltage_level_id=slack_gen['voltage_level_id'])

            # create active power control extension to define the distribution key
            n.create_extensions(extension_name='activePowerControl', id=gen_id,
                                participate=abs(weight) > 0.001, droop=weight)


def get_name(df: pd.DataFrame, name: str) -> pd.Series:
    name_col = df[name]
    replace_none = np.vectorize(lambda x: '' if x is None else x, otypes=[np.string_])
    name_cleaned = replace_none(name_col)
    return name_cleaned.astype(str)


def build_voltage_level_id(bus: Series) -> pd.Series:
    return 'sub_' + bus


def build_bus_id(bus: Series) -> pd.Series:
    return 'bus_' + bus


def build_injection_id(prefix: str, bus: pd.Series, index: int) -> str:
    return f'{prefix}_{bus}_{index}'  # because it is required by grid2op to build IDs like this is case of missing name


def generate_injection_id(df: pd.DataFrame, prefix: str) -> pd.Series:
    return df.apply(lambda row: build_injection_id(prefix, row['bus'], row.name), axis=1)


def build_line_id(row: pd.Series, index: int) -> str:
    from_bus = row['from_bus']
    to_bus = row['to_bus']
    return f'{from_bus}_{to_bus}_{index}'  # because it is required by grid2op to build IDs like this is case of missing name


def generate_line_id(df: pd.DataFrame) -> pd.Series:
    return df.apply(lambda row: build_line_id(row, row.name), axis=1)


def build_transformer_id(row: pd.Series, index: int, index_offset: int) -> str:
    hv_bus = row['hv_bus']
    lv_bus = row['lv_bus']
    return f'{hv_bus}_{lv_bus}_{index_offset + index}'  # because it is required by grid2op to build IDs like this is case of missing name


def generate_transformer_id(df: pd.DataFrame, index_offset: int) -> pd.Series:
    return df.apply(lambda row: build_transformer_id(row, row.name, index_offset), axis=1)


def create_transformers(n: Network, n_pdp: pandapowerNet) -> None:
    if len(n_pdp.trafo) > 0:
        bus = n_pdp.bus[['vn_kv']]
        trafo_and_bus = n_pdp.trafo.merge(bus.rename(columns=lambda x: x + '_lv_bus'), left_on='lv_bus', right_index=True, how='left')
        trafo_id = generate_transformer_id(trafo_and_bus, len(n_pdp.line))
        name = get_name(trafo_and_bus, 'name')
        vl1_id = build_voltage_level_id(trafo_and_bus['hv_bus'].astype(str))
        vl2_id = build_voltage_level_id(trafo_and_bus['lv_bus'].astype(str))
        connectable_bus1_id = build_bus_id(trafo_and_bus['hv_bus'].astype(str))
        connectable_bus2_id = build_bus_id(trafo_and_bus['lv_bus'].astype(str))
        bus1_id = np.where(trafo_and_bus['in_service'], connectable_bus1_id, "")
        bus2_id = np.where(trafo_and_bus['in_service'], connectable_bus2_id, "")
        n_tap = np.where(~np.isnan(trafo_and_bus['tap_pos']) & ~np.isnan(trafo_and_bus['tap_neutral']) & ~np.isnan(trafo_and_bus['tap_step_percent']),
                         1.0 + (trafo_and_bus['tap_pos'] - trafo_and_bus['tap_neutral']) * trafo_and_bus['tap_step_percent'] / 100.0, 1.0)
        rated_u1 = np.where(trafo_and_bus['tap_side'] == "hv", trafo_and_bus['vn_hv_kv'] * n_tap, trafo_and_bus['vn_hv_kv'])
        rated_u2 = np.where(trafo_and_bus['tap_side'] == "lv", trafo_and_bus['vn_lv_kv'] * n_tap, trafo_and_bus['vn_lv_kv'])
        c = n_pdp.sn_mva / n_pdp.trafo['sn_mva']
        rk = trafo_and_bus['vkr_percent'] / 100 * c
        zk = trafo_and_bus['vk_percent'] / 100 * c
        xk = np.sqrt(zk ** 2 - rk ** 2)
        ym = trafo_and_bus['i0_percent'] / 100
        gm = trafo_and_bus['pfe_kw'] / (trafo_and_bus['sn_mva'] * 1000) / c
        bm = -1 * np.sign(ym) * np.sqrt(ym ** 2 - gm ** 2)

        zb_tr = (rated_u2 ** 2) / n_pdp.sn_mva
        r = rk * zb_tr / trafo_and_bus['parallel']
        x = xk * zb_tr / trafo_and_bus['parallel']
        g = gm / zb_tr * trafo_and_bus['parallel']
        b = bm / zb_tr * trafo_and_bus['parallel']

        n.create_2_windings_transformers(id=trafo_id, name=name,
                                         voltage_level1_id=vl1_id, connectable_bus1_id=connectable_bus1_id, bus1_id=bus1_id,
                                         voltage_level2_id=vl2_id, connectable_bus2_id=connectable_bus2_id, bus2_id=bus2_id,
                                         rated_u1=rated_u1, rated_u2=rated_u2,
                                         r=r, x=x, g=g, b=b)


def create_limits(n: Network, n_pdp: pandapowerNet, id: pd.Series) -> None:
    limit_side = ['ONE'] * len(n_pdp.line)  # create on side on, why not...
    limit_name = ['permanent'] * len(n_pdp.line)
    limit_type = ['CURRENT'] * len(n_pdp.line)
    limit_value = n_pdp.line['max_i_ka'] * 1000.0 / n_pdp.line['parallel']
    limit_value *= n_pdp.line['df']
    acceptable_duration = [-1] * len(n_pdp.line)
    n.create_operational_limits(element_id=id, side=limit_side, name=limit_name, type=limit_type, value=limit_value,
                                acceptable_duration=acceptable_duration)

def create_lines(n: Network, n_pdp: pandapowerNet) -> None:
    if len(n_pdp.line) > 0:
        line_id = generate_line_id(n_pdp.line)
        name = get_name(n_pdp.line, 'name')
        vl1_id = build_voltage_level_id(n_pdp.line['from_bus'].astype(str))
        vl2_id = build_voltage_level_id(n_pdp.line['to_bus'].astype(str))
        connectable_bus1_id = build_bus_id(n_pdp.line['from_bus'].astype(str))
        connectable_bus2_id = build_bus_id(n_pdp.line['to_bus'].astype(str))
        bus1_id = np.where(n_pdp.line['in_service'], connectable_bus1_id, "")
        bus2_id = np.where(n_pdp.line['in_service'], connectable_bus2_id, "")
        r = n_pdp.line['length_km'] * n_pdp.line['r_ohm_per_km'] / n_pdp.line['parallel']
        x = n_pdp.line['length_km'] * n_pdp.line['x_ohm_per_km'] / n_pdp.line['parallel']
        g = n_pdp.line['length_km'] * n_pdp.line['g_us_per_km'] * 1e-6 * n_pdp.line['parallel'] / 2
        b = n_pdp.line['length_km'] * n_pdp.line['c_nf_per_km'] * 1e-9 * 2 * math.pi * n_pdp.f_hz * n_pdp.line['parallel'] / 2

        n.create_lines(id=line_id, name=name,
                       voltage_level1_id=vl1_id, connectable_bus1_id=connectable_bus1_id, bus1_id=bus1_id,
                       voltage_level2_id=vl2_id, connectable_bus2_id=connectable_bus2_id, bus2_id=bus2_id,
                       r=r, x=x, g1=g, g2=g, b1=b, b2=b)
        create_limits(n, n_pdp, line_id)


def create_shunts(n: Network, n_pdp: pandapowerNet) -> None:
    if len(n_pdp.shunt) > 0:
        shunt_id = generate_injection_id(n_pdp.shunt, 'shunt')
        name = get_name(n_pdp.shunt, 'name').tolist()
        vl_id = build_voltage_level_id(n_pdp.shunt['bus'].astype(str)).tolist()
        connectable_bus_id = build_bus_id(n_pdp.shunt['bus'].astype(str)).tolist()
        bus_id = np.where(n_pdp.shunt['in_service'], connectable_bus_id, "")
        model_type = ['LINEAR'] * len(n_pdp.shunt)
        section_count = n_pdp.shunt['step'].tolist()
        shunt_df = pd.DataFrame(data={
            'name': name,
            'voltage_level_id': vl_id,
            'connectable_bus_id': connectable_bus_id,
            'bus_id': bus_id,
            'model_type': model_type,
            'section_count': section_count
        }, index=shunt_id)
        g_per_section = (n_pdp.shunt['p_mw'] / (n_pdp.shunt['vn_kv'] ** 2) * -1.0).tolist()
        b_per_section = (n_pdp.shunt['q_mvar'] / (n_pdp.shunt['vn_kv'] ** 2) * -1.0).tolist()
        max_section_count = n_pdp.shunt['max_step'].tolist()
        linear_model_df = pd.DataFrame(data={
            'g_per_section': g_per_section,
            'b_per_section': b_per_section,
            'max_section_count': max_section_count,
        }, index=shunt_id)
        n.create_shunt_compensators(shunt_df=shunt_df, linear_model_df=linear_model_df)

class PandaPowerGeneratorType(Enum):
    GENERATOR = 1
    EXT_GRID = 2
    STATIC_GENERATOR = 3

def _create_generators(n: Network, gen: DataFrame, bus: DataFrame, slack_weight_by_gen_id: Dict[str, float], generator_type: PandaPowerGeneratorType) -> None:
    if len(gen) > 0:
        gen_and_bus: DataFrame = gen.merge(bus, left_on='bus', right_index=True, how='left', suffixes=('', '_x'))
        gen_id = generate_injection_id(gen_and_bus, 'gen' if generator_type != PandaPowerGeneratorType.STATIC_GENERATOR else 'sgen')
        name = get_name(gen_and_bus, 'name')
        vl_id = build_voltage_level_id(gen_and_bus['bus'].astype(str))
        connectable_bus_id = build_bus_id(gen_and_bus['bus'].astype(str)).tolist()
        bus_id = np.where(gen_and_bus['in_service'], connectable_bus_id, "")
        target_p = create_generator_target_p(gen_and_bus, generator_type)
        voltage_regulator_on = [generator_type != PandaPowerGeneratorType.STATIC_GENERATOR] * len(gen_and_bus)
        target_v = create_generator_target_v(gen_and_bus, generator_type)
        target_q = create_generator_target_q(gen_and_bus, generator_type)
        min_p = create_generator_min_p(gen_and_bus, generator_type)
        max_p = create_generator_max_p(gen_and_bus, generator_type)
        min_q = gen_and_bus['min_q_mvar'] if 'min_q_mvar' in gen_and_bus.columns else None
        max_q = gen_and_bus['max_q_mvar'] if 'max_q_mvar' in gen_and_bus.columns else None

        fill_generator_slack_weight(gen_and_bus, generator_type, slack_weight_by_gen_id)

        n.create_generators(id=gen_id, name=name,
                            voltage_level_id=vl_id, connectable_bus_id=connectable_bus_id, bus_id=bus_id,
                            target_p=target_p,  voltage_regulator_on=voltage_regulator_on, target_v=target_v, target_q=target_q,
                            min_p=min_p, max_p=max_p)
        if min_q is not None and max_q is not None:
            n.create_minmax_reactive_limits(id=gen_id, min_q=min_q, max_q=max_q)


def create_generator_target_p(gen_and_bus: DataFrame, generator_type: PandaPowerGeneratorType) -> pd.Series:
    return pd.Series([MIN_TARGET_P_TO_NOT_BE_DISCADED_FROM_ACTIVE_POWER_CONTROL] * len(
        gen_and_bus)) if generator_type == PandaPowerGeneratorType.EXT_GRID else gen_and_bus['p_mw'] # keep a small value of target_p to avoid be discarded to slack distribution


def create_generator_max_p(gen_and_bus: DataFrame, generator_type: PandaPowerGeneratorType) -> pd.Series:
    return pd.Series([DEFAULT_MAX_P] * len(
        gen_and_bus)) if generator_type == PandaPowerGeneratorType.EXT_GRID or 'max_p_mw' not in gen_and_bus.columns else pd.Series(
        np.nan_to_num(gen_and_bus['max_p_mw'], nan=DEFAULT_MAX_P))


def create_generator_min_p(gen_and_bus: DataFrame, generator_type: PandaPowerGeneratorType) -> pd.Series:
    return pd.Series([DEFAULT_MIN_P] * len(
        gen_and_bus)) if generator_type == PandaPowerGeneratorType.EXT_GRID or 'min_p_mw' not in gen_and_bus.columns else pd.Series(
        np.nan_to_num(gen_and_bus['min_p_mw'], nan=DEFAULT_MIN_P))


def create_generator_target_q(gen_and_bus: DataFrame, generator_type: PandaPowerGeneratorType) -> pd.Series:
    return gen_and_bus[
        'q_mvar'] if generator_type == PandaPowerGeneratorType.STATIC_GENERATOR and 'q_mvar' in gen_and_bus.columns else pd.Series(
        [0.0] * len(gen_and_bus))


def create_generator_target_v(gen_and_bus: DataFrame, generator_type: PandaPowerGeneratorType) -> pd.Series:
    return gen_and_bus['vm_pu'] * gen_and_bus[
        'vn_kv'] if generator_type != PandaPowerGeneratorType.STATIC_GENERATOR else pd.Series([1.0] * len(gen_and_bus))


def fill_generator_slack_weight(gen_and_bus: DataFrame, generator_type: PandaPowerGeneratorType, slack_weight_by_gen_id: Dict[str, float]) -> None:
    if generator_type != PandaPowerGeneratorType.STATIC_GENERATOR:
        for index, row in gen_and_bus.iterrows():
            index = typing.cast(int, index)  # safe cas from hashtable to int needed by mypy
            weight = row['slack_weight'] if generator_type == PandaPowerGeneratorType.EXT_GRID or row['slack'] else 0.0
            slack_weight_by_gen_id[build_injection_id('gen', row['bus'], index)] = weight


def create_generators(n: Network, n_pdp: pandapowerNet, slack_weight_by_gen_id: Dict[str, float]) -> None:
    _create_generators(n, n_pdp.gen, n_pdp.bus, slack_weight_by_gen_id, PandaPowerGeneratorType.GENERATOR)
    _create_generators(n, n_pdp.ext_grid, n_pdp.bus, slack_weight_by_gen_id, PandaPowerGeneratorType.EXT_GRID)
    _create_generators(n, n_pdp.sgen, n_pdp.bus, slack_weight_by_gen_id, PandaPowerGeneratorType.STATIC_GENERATOR)


def create_loads(n: Network, n_pdp: pandapowerNet) -> None:
    if len(n_pdp.load) > 0:
        load_id = generate_injection_id(n_pdp.load, 'load')
        name = get_name(n_pdp.load, 'name')
        vl_id = build_voltage_level_id(n_pdp.load['bus'].astype(str))
        connectable_bus_id = build_bus_id(n_pdp.load['bus'].astype(str)).tolist()
        bus_id = np.where(n_pdp.load['in_service'], connectable_bus_id, "")
        p0 = n_pdp.load['p_mw']
        q0 = n_pdp.load['q_mvar']
        n.create_loads(id=load_id, name=name,
                       voltage_level_id=vl_id, connectable_bus_id=connectable_bus_id, bus_id=bus_id,
                       p0=p0, q0=q0)


def create_buses(n: Network, n_pdp: pandapowerNet) -> None:
    if len(n_pdp.bus) > 0:
        vl_id = build_voltage_level_id(n_pdp.bus.index.astype(str))
        topology_kind = ['BUS_BREAKER'] * len(n_pdp.bus)
        nominal_v = n_pdp.bus['vn_kv']
        low_voltage_limit = n_pdp.bus['min_vm_pu'] * nominal_v if 'min_vm_pu' in n_pdp.bus.columns else None
        high_voltage_limit = n_pdp.bus['max_vm_pu'] * nominal_v if 'max_vm_pu' in n_pdp.bus.columns else None
        substation_id = ['s'] * len(n_pdp.bus)
        # TODO topology kind should have a default value
        n.create_voltage_levels(id=vl_id, substation_id=substation_id, topology_kind=topology_kind, nominal_v=nominal_v,
                                low_voltage_limit=low_voltage_limit, high_voltage_limit=high_voltage_limit)
        bus_id = build_bus_id(n_pdp.bus.index.astype(str))
        name = get_name(n_pdp.bus, 'name')
        n.create_buses(id=bus_id, name=name, voltage_level_id=vl_id)
