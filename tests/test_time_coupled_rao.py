#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib

import pypowsybl as pp
from pypowsybl.rao import Parameters as RaoParameters
from pypowsybl.rao import TimeCoupledConstraints
from pypowsybl.rao import Crac
from pypowsybl.rao import TimeCoupledRaoInput
from pypowsybl.rao import TimeCoupledRao
from datetime import datetime

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data/rao/time_coupled_rao'

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
        time_coupled_input.add_data_point(datetime.strptime(t, date_format), n, Crac.from_file_source(n, DATA_DIR.joinpath("crac_" + t + ".json")))

    runner = TimeCoupledRao()
    df = runner.run(time_coupled_input, constraints, parameters)

    for t in timestamps:
        rao_result = df[datetime.strptime(t, date_format).strftime('%Y-%m-%dT%H:%M:%S+01:00')]
        assert RaoComputationStatus.DEFAULT == result.status()

if __name__ == '__main__':
    unittest.main()

