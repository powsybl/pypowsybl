
import math
from importlib import util
from typing import Dict

import numpy as np
import pandas as pd
import pypowsybl._pypowsybl as _pp
from pandas import Series

from .network import Network
from .network_creation_util import create_empty

DEFAULT_MIN_P = -4999.0
DEFAULT_MAX_P = 4999.0


def convert_from_pandapower(n_pdp) -> Network:
    if util.find_spec("pandapower") is None:
        raise _pp.PyPowsyblError("pandapower is not installed")
    else:
        n = create_empty(n_pdp.name if n_pdp.name else 'network')

        # create one giant substation
        n.create_substations(id='s')
        create_buses(n, n_pdp)
        create_loads(n, n_pdp)
        slack_weight_by_gen_id = {}
        create_generators(n, n_pdp, slack_weight_by_gen_id)
        create_shunts(n, n_pdp)
        create_lines(n, n_pdp)
        create_transformers(n, n_pdp)

        # create slack bus extension
        slack_gen_ids = [key for key, value in slack_weight_by_gen_id.items() if value == 1.0]
        if len(slack_gen_ids) > 0:
            generators = n.get_generators(attributes=['voltage_level_id'])
            slack_gen = generators.loc[slack_gen_ids[0]]
            n.create_extensions(extension_name='slackTerminal', element_id=slack_gen_ids[0], voltage_level_id=slack_gen['voltage_level_id'])

        return n


def get_name(df: pd.DataFrame, name: str) -> pd.Series:
    name = df[name]
    replace_none = np.vectorize(lambda x: '' if x is None else x, otypes=[np.string_])
    name_cleaned = replace_none(name)
    return name_cleaned.astype(str)


def build_voltage_level_id(bus: Series):
    return 'sub_' + bus


def build_bus_id(bus: Series):
    return 'bus_' + bus


def build_injection_id(prefix, bus, index):
    return "{}_{}_{}".format(prefix, bus, index) # because it is required by grid2op to build IDs like this is case of missing name


def generate_injection_id(df: pd.DataFrame, prefix: str) -> pd.Series:
    return df.apply(lambda row: build_injection_id(prefix, row['bus'], row.name), axis=1)


def build_line_id(row, index):
    from_bus = row['from_bus']
    to_bus = row['to_bus']
    return "{}_{}_{}".format(from_bus, to_bus, index) # because it is required by grid2op to build IDs like this is case of missing name


def generate_line_id(df: pd.DataFrame) -> pd.Series:
    return df.apply(lambda row: build_line_id(row, row.name), axis=1)


def build_transformer_id(row, index, index_offset: int):
    hv_bus = row['hv_bus']
    lv_bus = row['lv_bus']
    return "{}_{}_{}".format(hv_bus, lv_bus, index_offset + index) # because it is required by grid2op to build IDs like this is case of missing name


def generate_transformer_id(df: pd.DataFrame, index_offset: int) -> pd.Series:
    return df.apply(lambda row: build_transformer_id(row, row.name, index_offset), axis=1)


def create_transformers(n, n_pdp):
    if len(n_pdp.trafo) > 0:
        bus = n_pdp.bus[['vn_kv']]
        trafo_and_bus = n_pdp.trafo.merge(bus.rename(columns=lambda x: x + '_lv_bus'), left_on='lv_bus', right_index=True, how='inner')
        id = generate_transformer_id(trafo_and_bus, len(n_pdp.line))
        name = get_name(trafo_and_bus, 'name')
        vl1_id = build_voltage_level_id(trafo_and_bus['hv_bus'].astype(str))
        vl2_id = build_voltage_level_id(trafo_and_bus['lv_bus'].astype(str))
        bus1_id = build_bus_id(trafo_and_bus['hv_bus'].astype(str))
        bus2_id = build_bus_id(trafo_and_bus['lv_bus'].astype(str))
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
        bm = - np.sqrt(ym ** 2 - gm ** 2)

        zb_tr = (trafo_and_bus['vn_kv_lv_bus'] ** 2) / n_pdp.sn_mva
        r = rk * zb_tr / trafo_and_bus['parallel']
        x = xk * zb_tr / trafo_and_bus['parallel']
        g = gm / zb_tr * trafo_and_bus['parallel']
        b = bm / zb_tr * trafo_and_bus['parallel']

        n.create_2_windings_transformers(id=id, name=name,
                                         voltage_level1_id=vl1_id, bus1_id=bus1_id,
                                         voltage_level2_id=vl2_id, bus2_id=bus2_id,
                                         rated_u1=rated_u1, rated_u2=rated_u2,
                                         r=r, x=x, g=g, b=b)


def create_lines(n, n_pdp):
    if len(n_pdp.line) > 0:
        id = generate_line_id(n_pdp.line)
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

        n.create_lines(id=id, name=name, voltage_level1_id=vl1_id, connectable_bus1_id=connectable_bus1_id, bus1_id=bus1_id, voltage_level2_id=vl2_id,
                       connectable_bus2_id=connectable_bus2_id, bus2_id=bus2_id, r=r, x=x, g1=g, g2=g, b1=b, b2=b)


def create_shunts(n, n_pdp):
    if len(n_pdp.shunt) > 0:
        id = generate_injection_id(n_pdp.shunt, 'shunt')
        name = get_name(n_pdp.shunt, 'name').tolist()
        vl_id = build_voltage_level_id(n_pdp.shunt['bus'].astype(str)).tolist()
        bus_id = build_bus_id(n_pdp.shunt['bus'].astype(str)).tolist()
        model_type = ['LINEAR'] * len(n_pdp.shunt)
        section_count = n_pdp.shunt['step'].tolist()
        shunt_df = pd.DataFrame(data={
            'name': name,
            'voltage_level_id': vl_id,
            'bus_id': bus_id,
            'model_type': model_type,
            'section_count': section_count
        }, index=id)
        g_per_section = (n_pdp.shunt['p_mw'] / (n_pdp.shunt['vn_kv'] ** 2) * -1.0).tolist()
        b_per_section = (n_pdp.shunt['q_mvar'] / (n_pdp.shunt['vn_kv'] ** 2) * -1.0).tolist()
        max_section_count = n_pdp.shunt['max_step'].tolist()
        linear_model_df = pd.DataFrame(data={
            'g_per_section': g_per_section,
            'b_per_section': b_per_section,
            'max_section_count': max_section_count,
        }, index=id)
        n.create_shunt_compensators(shunt_df=shunt_df, linear_model_df=linear_model_df)


def _create_generators(n, gen, bus, slack_weight_by_gen_id: Dict[str, float], ext_grid: bool):
    if len(gen) > 0:
        gen_and_bus = gen.merge(bus, left_on='bus', right_index=True, how='inner', suffixes=('', '_x'))
        id = generate_injection_id(gen_and_bus, 'gen')
        name = get_name(gen_and_bus, 'name')
        vl_id = build_voltage_level_id(gen_and_bus['bus'].astype(str))
        bus_id = build_bus_id(gen_and_bus['bus'].astype(str))
        target_p = [0.0001] * len(gen_and_bus) if ext_grid else gen_and_bus['p_mw']
        voltage_regulator_on = [True] * len(gen_and_bus)
        target_v = gen_and_bus['vm_pu'] * gen_and_bus['vn_kv']
        min_p = [DEFAULT_MIN_P] * len(gen_and_bus) if ext_grid or 'min_p_mw' not in gen_and_bus.columns else np.nan_to_num(gen_and_bus['min_p_mw'], nan=DEFAULT_MIN_P)
        max_p = [DEFAULT_MAX_P] * len(gen_and_bus) if ext_grid or 'max_p_mw' not in gen_and_bus.columns else np.nan_to_num(gen_and_bus['max_p_mw'], nan=DEFAULT_MAX_P)
        for index, row in gen_and_bus.iterrows():
            slack_weight_by_gen_id[build_injection_id('gen', row['bus'], index)] = row['slack_weight']

        n.create_generators(id=id, name=name, voltage_level_id=vl_id, bus_id=bus_id, target_p=target_p,
                            voltage_regulator_on=voltage_regulator_on,
                            target_v=target_v, min_p=min_p, max_p=max_p)


def create_generators(n, n_pdp, slack_weight_by_gen_id):
    _create_generators(n, n_pdp.gen, n_pdp.bus, slack_weight_by_gen_id, False)
    _create_generators(n, n_pdp.ext_grid, n_pdp.bus, slack_weight_by_gen_id, True)


def create_loads(n, n_pdp):
    if len(n_pdp.load) > 0:
        id = generate_injection_id(n_pdp.load, 'load')
        name = get_name(n_pdp.load, 'name')
        vl_id = build_voltage_level_id(n_pdp.load['bus'].astype(str))
        bus_id = build_bus_id(n_pdp.load['bus'].astype(str))
        p0 = n_pdp.load['p_mw']
        q0 = n_pdp.load['q_mvar']
        n.create_loads(id=id, name=name, voltage_level_id=vl_id, bus_id=bus_id, p0=p0, q0=q0)


def create_buses(n, n_pdp):
    if len(n_pdp.bus) > 0:
        vl_id = build_voltage_level_id(n_pdp.bus.index.astype(str))
        topology_kind = ['BUS_BREAKER'] * len(n_pdp.bus)
        nominal_v = n_pdp.bus['vn_kv']
        low_voltage_limit = n_pdp.bus['min_vm_pu'] * nominal_v if 'min_vm_pu' in n_pdp.bus.columns else None
        high_voltage_limit = n_pdp.bus['max_vm_pu'] * nominal_v if 'max_vm_pu' in n_pdp.bus.columns else None
        substation_id = ['s'] * len(n_pdp.bus)
        # FIXME topology kind should have a default value
        n.create_voltage_levels(id=vl_id, substation_id=substation_id, topology_kind=topology_kind, nominal_v=nominal_v,
                                low_voltage_limit=low_voltage_limit, high_voltage_limit=high_voltage_limit)
        id = build_bus_id(n_pdp.bus.index.astype(str))
        name = get_name(n_pdp.bus, 'name')
        n.create_buses(id=id, name=name, voltage_level_id=vl_id)
