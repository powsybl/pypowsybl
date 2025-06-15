#
# Copyright (c) 2025, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import logging

import numpy as np
import pandas as pd
import pytest
from pandas import DataFrame

import pypowsybl as pp
from pypowsybl.network import Network
from pypowsybl.opf.impl.model.bounds import Bounds
from pypowsybl.opf.impl.opf import OptimalPowerFlowParameters
from pypowsybl.opf.impl.parameters import OptimalPowerFlowMode


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(1)
    pd.options.display.max_columns = None
    pd.options.display.expand_frame_repr = False


def test_reactive_range():
    b = Bounds(10, 20)
    assert b.min_value == 10
    assert b.max_value == 20
    assert str(b) == "[10, 20]"
    assert b.mirror().min_value == -20
    assert b.mirror().max_value == -10
    assert b.mirror().min_value_with_margin == -20.000001
    assert b.mirror().max_value_with_margin == -9.999999
    assert b.contains(10)
    assert b.contains(20)
    assert not b.contains(5)
    assert not b.contains(21)
    assert b.contains(9.99999999)
    assert not b.contains(9.999)
    assert b.contains(20.00000001)
    assert not b.contains(20.001)
    assert b.reduce(0.1).min_value == 11.0
    assert b.reduce(0.1).max_value == 18.0
    assert b.mirror().reduce(0.1).min_value == -18.0
    assert b.mirror().reduce(0.1).max_value == -11.0
    assert b.mirror().reduce(0.1).min_value_with_margin == -18.000001
    assert b.mirror().reduce(0.1).max_value_with_margin == -10.999999


def create_loadflow_parameters():
    return pp.loadflow.Parameters(voltage_init_mode=pp.loadflow.VoltageInitMode.DC_VALUES,
                                  provider_parameters={'plausibleActivePowerLimit': '10000.0'})


def run_opf_then_lf(network: pp.network.Network,
                    opf_parameters: OptimalPowerFlowParameters = OptimalPowerFlowParameters(),
                    iteration_count: int = 1):
    lf_parameters = create_loadflow_parameters()
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED

    assert pp.opf.run_ac(network, opf_parameters)

    validate(network)

    lf_parameters.voltage_init_mode = pp.loadflow.VoltageInitMode.PREVIOUS_VALUES
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED
    assert lf_result[0].iteration_count == iteration_count


def validate(network: Network):
    validation_parameters = pp.loadflow.ValidationParameters(threshold=1, check_main_component_only=True)
    validation_types = [
        pp.loadflow.ValidationType.BUSES,
        pp.loadflow.ValidationType.FLOWS,
        # pp.loadflow.ValidationType.SHUNTS, to fix because active power is expected nan
        # pp.loadflow.ValidationType.TWTS, to fix
        pp.loadflow.ValidationType.TWTS3W,
        # pp.loadflow.ValidationType.GENERATORS # to fix because remote voltage not taken into account
    ]
    result = pp.loadflow.run_validation(network, validation_types, validation_parameters)
    # print(result.buses[result.buses['validated'] == False])
    assert result.valid


def calculate_overloading(network: Network) -> DataFrame:
    sa = pp.security.create_analysis()
    lf_parameters = create_loadflow_parameters()
    sa_results = sa.run_ac(network, lf_parameters)
    print(sa_results.pre_contingency_result.limit_violations)
    limit_violations = sa_results.limit_violations
    limit_violations = limit_violations[
        (limit_violations['limit_type'] == 'CURRENT') & (limit_violations['limit_name'] == 'permanent')]
    limit_violations['loading'] = limit_violations['value'] / limit_violations['limit']
    limit_violations = limit_violations[['side', 'loading']]
    return limit_violations


def increase_load(network: Network, value: float):
    loads = network.get_loads()
    load_sum = loads['p0'].sum()
    loads['p0'] = loads['p0'] * (1 + value / load_sum)
    network.update_loads(id=loads.index, p0=loads['p0'])


def test_esg_tuto1():
    run_opf_then_lf(pp.network.create_eurostag_tutorial_example1_network())


def test_ieee9():
    run_opf_then_lf(pp.network.create_ieee9())


def test_ieee14():
    run_opf_then_lf(pp.network.create_ieee14())


def test_ieee14_redispatching():
    network = pp.network.create_ieee14()

    # add a current limit on L7-9-1
    network.create_operational_limits(pd.DataFrame.from_records(index='element_id', data=[
        {'element_id': 'L7-9-1', 'name': 'permanent', 'side': 'ONE',
         'type': 'CURRENT', 'value': 1000, 'acceptable_duration': np.inf,
         'fictitious': False, 'group_name': 'GROUP1'},
    ]))
    network.update_lines(id=['L7-9-1'], selected_limits_group_1=['GROUP1'])

    lf_parameters = create_loadflow_parameters()
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED

    assert network.get_lines(attributes=['i1']).loc['L7-9-1'].i1 == pytest.approx(1113.514, rel=1e-3, abs=1e-3)
    assert len(calculate_overloading(network)) == 1

    opf_parameters = OptimalPowerFlowParameters(mode=OptimalPowerFlowMode.REDISPATCHING)
    assert pp.opf.run_ac(network, opf_parameters)

    lf_parameters.voltage_init_mode = pp.loadflow.VoltageInitMode.PREVIOUS_VALUES
    lf_result = pp.loadflow.run_ac(network, lf_parameters)
    assert lf_result[0].status == pp.loadflow.ComponentStatus.CONVERGED
    assert lf_result[0].iteration_count == 1

    assert network.get_lines(attributes=['i1']).loc['L7-9-1'].i1 == pytest.approx(947.535, rel=1e-3, abs=1e-3)
    assert len(calculate_overloading(network)) == 0


def test_ieee14_open_side_1_branch():
    network = pp.network.create_ieee14()
    network.update_lines(id=['L3-4-1'], connected1=[False])
    run_opf_then_lf(network)
    row = network.get_branches(id=['L3-4-1'], attributes=['p1', 'q1', 'p2', 'q2' ]).head(1)
    assert row.p1.values[0] == 0
    assert row.q1.values[0] == 0
    assert row.p2.values[0] != 0
    assert row.q2.values[0] != 0


def test_ieee14_open_side_2_branch():
    network = pp.network.create_ieee14()
    network.update_lines(id=['L3-4-1'], connected2=[False])
    run_opf_then_lf(network)
    row = network.get_branches(id=['L3-4-1'], attributes=['p1', 'q1', 'p2', 'q2' ]).head(1)
    assert row.p1.values[0] != 0
    assert row.q1.values[0] != 0
    assert row.p2.values[0] == 0
    assert row.q2.values[0] == 0


def test_ieee30():
    run_opf_then_lf(pp.network.create_ieee30())


def test_ieee57():
    run_opf_then_lf(pp.network.create_ieee57())


def test_ieee118():
    run_opf_then_lf(pp.network.create_ieee118())


def test_ieee300():
    run_opf_then_lf(pp.network.create_ieee300())


def test_metrix_tutorial_six_buses():
    run_opf_then_lf(pp.network.create_metrix_tutorial_six_buses_network())


def test_micro_grid_be():
    run_opf_then_lf(pp.network.create_micro_grid_be_network())


def test_micro_grid_nl():
    run_opf_then_lf(pp.network.create_micro_grid_nl_network())
