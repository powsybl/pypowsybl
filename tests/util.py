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