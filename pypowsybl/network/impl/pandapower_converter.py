
import math
from importlib import util

import numpy as np
import pandas as pd

import pypowsybl._pypowsybl as _pp
from .network import Network
from .network_creation_util import create_empty


def convert_from_pandapower(n_pdp) -> Network:
    if util.find_spec("pandapower") is None:
        raise _pp.PyPowsyblError("pandapower is not installed")
    else:
        import pandapower as pdp

        n = create_empty(n_pdp.name if n_pdp.name else 'network')

        # create one giant substation
        n.create_substations(id='s')
        create_buses(n, n_pdp)
        create_loads(n, n_pdp)
        create_generators(n, n_pdp)
        create_shunts(n, n_pdp)
        create_lines(n, n_pdp)
        create_transformers(n, n_pdp)

        return n


def get_name(df: pd.DataFrame, name: str) -> pd.Series:
    name = df[name]
    replace_none = np.vectorize(lambda x: '' if x is None else x, otypes=[np.string_])
    name_cleaned = replace_none(name)
    return name_cleaned.astype(str)


def build_injection_id(prefix, row, index):
    bus = row['bus']
    return "{}_{}_{}".format(prefix, bus, index) # because it is required by grid2op to build IDs like this is case of missing name


def generate_injection_id(df: pd.DataFrame, prefix: str) -> pd.Series:
    return df.apply(lambda row: build_injection_id(prefix, row, row.name), axis=1)


def create_transformers(n, n_pdp):
    id = 'tfo_' + n_pdp.trafo.index.astype(str)
    name = get_name(n_pdp.trafo, 'name')
    vl1_id = 'vl_' + n_pdp.trafo['lv_bus'].astype(str)
    vl2_id = 'vl_' + n_pdp.trafo['hv_bus'].astype(str)
    bus1_id = 'bus_' + n_pdp.trafo['lv_bus'].astype(str)
    bus2_id = 'bus_' + n_pdp.trafo['hv_bus'].astype(str)
    rated_u1 = n_pdp.trafo['vn_lv_kv']
    rated_u2 = n_pdp.trafo['vn_hv_kv']
    r = n_pdp.trafo['vkr_percent'] / 100 * n_pdp.sn_mva / n_pdp.trafo['sn_mva']
    z = n_pdp.trafo['vk_percent'] / 100 * n_pdp.sn_mva / n_pdp.trafo['sn_mva']
    x = np.sqrt(z * z - r * r)
    y = n_pdp.trafo['i0_percent'] / 100
    g = n_pdp.trafo['pfe_kw'] / (n_pdp.trafo['sn_mva'] * 1000) * n_pdp.sn_mva / n_pdp.trafo['sn_mva']
    b = np.sqrt(y * y - g * g)
    n.create_2_windings_transformers(id=id, name=name, voltage_level1_id=vl1_id, bus1_id=bus1_id,
                                     voltage_level2_id=vl2_id,
                                     bus2_id=bus2_id, rated_u1=rated_u1, rated_u2=rated_u2, r=r, x=x, g=g, b=b)


def create_lines(n, n_pdp):
    id = 'line_' + n_pdp.line.index.astype(str)
    name = get_name(n_pdp.line, 'name')
    vl1_id = 'vl_' + n_pdp.line['from_bus'].astype(str)
    vl2_id = 'vl_' + n_pdp.line['to_bus'].astype(str)
    bus1_id = 'bus_' + n_pdp.line['from_bus'].astype(str)
    bus2_id = 'bus_' + n_pdp.line['to_bus'].astype(str)
    r = n_pdp.line['length_km'] * n_pdp.line['r_ohm_per_km']
    x = n_pdp.line['length_km'] * n_pdp.line['x_ohm_per_km']
    b = n_pdp.line['length_km'] * n_pdp.line['c_nf_per_km'] * 1e-9 * 2 * math.pi * 50 / 2
    n.create_lines(id=id, name=name, voltage_level1_id=vl1_id, bus1_id=bus1_id, voltage_level2_id=vl2_id,
                   bus2_id=bus2_id, r=r, x=x, b1=b, b2=b)


def create_shunts(n, n_pdp):
    id = generate_injection_id(n_pdp.shunt, 'shunt')
    name = get_name(n_pdp.shunt, 'name').tolist()
    vl_id = ('vl_' + n_pdp.shunt['bus'].astype(str)).tolist()
    bus_id = ('bus_' + n_pdp.shunt['bus'].astype(str)).tolist()
    model_type = ['LINEAR'] * len(n_pdp.shunt)
    section_count = n_pdp.shunt['step'].tolist()
    shunt_df = pd.DataFrame(data={
        'name': name,
        'voltage_level_id': vl_id,
        'bus_id': bus_id,
        'model_type': model_type,
        'section_count': section_count
    }, index=id)
    g_per_section = (n_pdp.shunt['p_mw'] / n_pdp.shunt['vn_kv'].pow(2)).tolist()
    b_per_section = (n_pdp.shunt['q_mvar'] / n_pdp.shunt['vn_kv'].pow(2)).tolist()
    max_section_count = n_pdp.shunt['max_step'].tolist()
    linear_model_df = pd.DataFrame(data={
        'g_per_section': g_per_section,
        'b_per_section': b_per_section,
        'max_section_count': max_section_count,
    }, index=id)
    n.create_shunt_compensators(shunt_df=shunt_df, linear_model_df=linear_model_df)


def create_generators(n, n_pdp):
    gen = n_pdp.gen.merge(n_pdp.bus, left_on='bus', right_index=True, how='inner')
    id = generate_injection_id(gen, 'gen')
    name = get_name(n_pdp.gen, 'name')
    vl_id = 'vl_' + gen['bus'].astype(str)
    bus_id = 'bus_' + gen['bus'].astype(str)
    target_p = gen['p_mw']
    voltage_regulator_on = [True] * len(gen)
    target_v = gen['vm_pu'] * gen['vn_kv']
    min_p = [0] * len(gen)
    max_p = [99999] * len(gen)
    n.create_generators(id=id, name=name, voltage_level_id=vl_id, bus_id=bus_id, target_p=target_p,
                        voltage_regulator_on=voltage_regulator_on,
                        target_v=target_v, min_p=min_p, max_p=max_p)


def create_loads(n, n_pdp):
    id = generate_injection_id(n_pdp.load, 'load')
    name = get_name(n_pdp.load, 'name')
    vl_id = 'vl_' + n_pdp.load['bus'].astype(str)
    bus_id = 'bus_' + n_pdp.load['bus'].astype(str)
    p0 = n_pdp.load['p_mw']
    q0 = n_pdp.load['q_mvar']
    n.create_loads(id=id, name=name, voltage_level_id=vl_id, bus_id=bus_id, p0=p0, q0=q0)


def create_buses(n, n_pdp):
    vl_id = 'vl_' + n_pdp.bus.index.astype(str)
    topology_kind = ['BUS_BREAKER'] * len(n_pdp.bus)
    nominal_v = n_pdp.bus['vn_kv']
    low_voltage_limit = n_pdp.bus['min_vm_pu'] * nominal_v
    high_voltage_limit = n_pdp.bus['max_vm_pu'] * nominal_v
    substation_id = ['s'] * len(n_pdp.bus)
    # FIXME topology kind should have a default value
    n.create_voltage_levels(id=vl_id, substation_id=substation_id, topology_kind=topology_kind, nominal_v=nominal_v,
                            low_voltage_limit=low_voltage_limit, high_voltage_limit=high_voltage_limit)
    id = 'bus_' + n_pdp.bus.index.astype(str)
    name = get_name(n_pdp.bus, 'name')
    n.create_buses(id=id, name=name, voltage_level_id=vl_id)
