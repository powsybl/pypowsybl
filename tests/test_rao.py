#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import io
import unittest

import pypowsybl as pp
from pypowsybl._pypowsybl import RaoComputationStatus
from pypowsybl.rao import Parameters as RaoParameters

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'

def test_default_rao_parameters():
    parameters = RaoParameters()
    json_param = parameters.to_json()
    assert json_param['version'] == '2.4'
    assert json_param['objective-function']['type'] == 'MAX_MIN_MARGIN_IN_MEGAWATT'

def test_rao_parameters():
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json"))
    json_param = parameters.to_json()
    assert json_param['range-actions-optimization']['max-mip-iterations'] == 15
    assert json_param['objective-function']['type'] == 'MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT'

def test_rao_from_files():
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))
    parameters = RaoParameters()
    parameters.load_from_file_source(DATA_DIR.joinpath("rao/rao_parameters.json"))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_file_source(network, DATA_DIR.joinpath("rao/rao_crac.json"))
    rao_runner.set_glsk_file_source(network, DATA_DIR.joinpath("rao/rao_glsk.xml"))
    result = rao_runner.run(network, parameters)
    assert RaoComputationStatus.DEFAULT == result.status()

def test_rao_from_buffers():
    network =  pp.network.load(DATA_DIR.joinpath("rao/rao_network.uct"))
    crac = io.BytesIO(open(DATA_DIR.joinpath("rao/rao_crac.json"), "rb").read())
    glsks = io.BytesIO(open(DATA_DIR.joinpath("rao/rao_glsk.xml"), "rb").read())

    parameters = RaoParameters()
    parameters.load_from_buffer_source(
        io.BytesIO(open(DATA_DIR.joinpath("rao/rao_parameters.json"), "rb").read()))

    rao_runner = pp.rao.create_rao()
    rao_runner.set_crac_buffer_source(network, crac)
    rao_runner.set_glsk_buffer_source(network, glsks)
    result = rao_runner.run(network, parameters)
    assert RaoComputationStatus.DEFAULT == result.status()
    json_result = result.to_json()

    assert json_result["computationStatus"] == "default"
    assert list(json_result.keys()) == ['type', 'version', 'info', 'computationStatus', 'executionDetails', 'costResults',
                                    'computationStatusMap', 'flowCnecResults', 'angleCnecResults', 'voltageCnecResults',
                                    'networkActionResults', 'rangeActionResults']

if __name__ == '__main__':
    unittest.main()