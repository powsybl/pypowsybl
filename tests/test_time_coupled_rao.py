#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import math

import pypowsybl as pp
from pypowsybl.rao import Parameters as RaoParameters
from pypowsybl.rao import TimeCoupledConstraints
from pypowsybl.rao import Crac
from pypowsybl.rao import TimeCoupledRaoInput
from pypowsybl.rao import TimeCoupledRao
from datetime import datetime
from pypowsybl._pypowsybl import RaoComputationStatus

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data/rao/time_coupled_rao'

def check_costs(results_df, timestamp, total_cost, functional_cost, rd_set_point):
    dt = datetime.strptime(timestamp, '%Y%m%d%H%M')
    rao_result = results_df.loc[results_df["timestamp"] == dt.strftime('%Y-%m-%dT%H:%M:%S+01:00'), "result"].iloc[0]
    df = rao_result.get_cost_results_for_timestamp(dt)
    assert math.isclose(df['functional_cost']['preventive'], functional_cost, rel_tol=1e-09)
    assert math.isclose(df['cost']['preventive'], total_cost, rel_tol=1e-03)
    df_ra = rao_result.get_range_action_results()
    if rd_set_point is not None:
        real_setpoint = df_ra.loc[df_ra['remedial_action_id'] == 'redispatchingAction']['optimized_set_point']
        assert math.isclose(rd_set_point, real_setpoint, rel_tol=1e-03)

def test_time_coupled_rao():
    parameters = RaoParameters.from_file_source(
      DATA_DIR.joinpath("RaoParameters_minCost_megawatt_dc_0_shift_penalty_100.json"))
    constraints = TimeCoupledConstraints.from_file_source(
      DATA_DIR.joinpath("time-coupled-constraints-with-lead-and-lag-times-and-gradients.json"))

    date_format = '%Y%m%d%H%M'
    timestamps = ["202511040030", "202511040130", "202511040230", "202511040330", "202511040430", "202511040530", "202511040630",
         "202511040730", "202511040830", "202511040930", "202511041030", "202511041130", "202511041230", "202511041330",
         "202511041430", "202511041530", "202511041630", "202511041730", "202511041830", "202511041930", "202511042030",
         "202511042130", "202511042230", "202511042330"]

    time_coupled_input = TimeCoupledRaoInput()
    for t in timestamps:
        n = pp.network.load(DATA_DIR.joinpath("6Nodes_Pmin1000_Pmax3000.xiidm"))
        time_coupled_input.add_temporal_data(datetime.strptime(t, date_format), n, Crac.from_file_source(n, DATA_DIR.joinpath("crac_" + t + ".json")))

    runner = TimeCoupledRao()
    results_df = runner.run(time_coupled_input, constraints, parameters)

    for t in timestamps:
        rao_result = results_df.loc[results_df["timestamp"] == datetime.strptime(t, '%Y%m%d%H%M').strftime('%Y-%m-%dT%H:%M:%S+01:00'), "result"].iloc[0]
        assert RaoComputationStatus.DEFAULT == rao_result.status()

    check_costs(results_df, "202511040030", total_cost=150010.0, functional_cost=150010.0, rd_set_point=3000.0)
    check_costs(results_df, "202511040130", total_cost=150010.0, functional_cost=150010.0, rd_set_point=3000.0)
    check_costs(results_df, "202511040230", total_cost=150010.0, functional_cost=150010.0, rd_set_point=3000.0)
    check_costs(results_df, "202511040330", total_cost=155010.0, functional_cost=145010.0, rd_set_point=2900.0)
    check_costs(results_df, "202511040430", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511040530", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511040630", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511040730", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511040830", total_cost=110000.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511040930", total_cost=55010.0, functional_cost=55010.0, rd_set_point=1100.0)
    check_costs(results_df, "202511041030", total_cost=55010.0, functional_cost=55010.0, rd_set_point=1100.0)
    check_costs(results_df, "202511041130", total_cost=55010.0, functional_cost=55010.0, rd_set_point=1100.0)
    check_costs(results_df, "202511041230", total_cost=55010.0, functional_cost=55010.0, rd_set_point=1100.0)
    check_costs(results_df, "202511041330", total_cost=55010.0, functional_cost=55010.0, rd_set_point=1100.0)
    check_costs(results_df, "202511041430", total_cost=55010.0, functional_cost=55010.0, rd_set_point=1100.0)
    check_costs(results_df, "202511041530", total_cost=110000.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511041630", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511041730", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511041830", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511041930", total_cost=0.0, functional_cost=0.0, rd_set_point=None)
    check_costs(results_df, "202511042030", total_cost=155010.00, functional_cost=145010.0, rd_set_point=2900.0)
    check_costs(results_df, "202511042130", total_cost=150010.0, functional_cost=150010.0, rd_set_point=3000.0)
    check_costs(results_df, "202511042230", total_cost=150010.0, functional_cost=150010.0, rd_set_point=3000.0)
    check_costs(results_df, "202511042330", total_cost=150010.0, functional_cost=150010.0, rd_set_point=3000.0)

if __name__ == '__main__':
    unittest.main()

