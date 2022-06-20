#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pytest
import pypowsybl as pp
from dateutil import parser
import datetime
import pathlib

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'


@pytest.fixture(autouse=True)
def no_config():
    pp.set_config_read(False)


def test_import_glsk():
    n = pp.network.load(DATA_DIR / 'simple-eu.uct')
    glsk_document = pp.glsk.load(DATA_DIR / 'glsk_sample.xml')
    assert glsk_document.get_countries() == ['10YFR-RTE------C', '10YNL----------L', '10YBE----------2', '10YCB-GERMANY--8']
    assert glsk_document.get_gsk_time_interval_start().timestamp() == parser.parse('2019-01-07T23:00Z').timestamp()
    assert glsk_document.get_gsk_time_interval_end().timestamp() == parser.parse('2019-01-08T23:00Z').timestamp()
    t = glsk_document.get_gsk_time_interval_start()
    assert glsk_document.get_points_for_country(n, '10YFR-RTE------C', t) == ['FFR1AA1 _generator', 'FFR2AA1 _generator', 'FFR3AA1 _generator']
    assert glsk_document.get_glsk_factors(n, '10YFR-RTE------C', t) == [0.2857142984867096, 0.2857142984867096, 0.4285714328289032]


def test_zone():
    n = pp.network.load(DATA_DIR / 'simple-eu.uct')
    glsk_document = pp.glsk.load(DATA_DIR / 'glsk_sample.xml')
    t = glsk_document.get_gsk_time_interval_start()
    de_generators = glsk_document.get_points_for_country(n, '10YCB-GERMANY--8', t)
    de_shift_keys = glsk_document.get_glsk_factors(n, '10YCB-GERMANY--8', t)

    assert de_generators == ['DDE1AA1 _generator', 'DDE2AA1 _generator', 'DDE3AA1 _generator']
    assert de_shift_keys == [0.4166666567325592, 0.3333333432674408, 0.25]
    zone_de = pp.sensitivity.create_zone_from_injections_and_shift_keys('DE', de_generators, de_shift_keys)
    assert zone_de.shift_keys_by_injections_ids == {'DDE1AA1 _generator': 0.4166666567325592, 'DDE2AA1 _generator': 0.3333333432674408, 'DDE3AA1 _generator': 0.25}


def test_zones():
    n = pp.network.load(DATA_DIR / 'simple-eu.uct')
    zones = pp.sensitivity.create_zones_from_glsk_file(n, DATA_DIR / 'glsk_sample.xml', datetime.datetime(2019, 1, 8, 0, 0))
    zone_fr = next(z for z in zones if z.id == '10YFR-RTE------C')
    zone_nl = next(z for z in zones if z.id == '10YNL----------L')
    zone_be = next(z for z in zones if z.id == '10YBE----------2')
    zone_de = next(z for z in zones if z.id == '10YCB-GERMANY--8')
    assert zone_fr.shift_keys_by_injections_ids == {'FFR1AA1 _generator': 0.2857142984867096, 'FFR2AA1 _generator': 0.2857142984867096, 'FFR3AA1 _generator': 0.4285714328289032}
    assert zone_nl.shift_keys_by_injections_ids == {'NNL1AA1 _generator': 0.3333333432674408, 'NNL2AA1 _generator': 0.1111111119389534, 'NNL3AA1 _generator': 0.5555555820465088}
    assert zone_be.shift_keys_by_injections_ids == {'BBE1AA1 _generator': 0.2142857164144516, 'BBE2AA1 _generator': 0.4285714328289032, 'BBE3AA1 _generator': 0.3571428656578064}
    assert zone_de.shift_keys_by_injections_ids == {'DDE1AA1 _generator': 0.4166666567325592, 'DDE2AA1 _generator': 0.3333333432674408, 'DDE3AA1 _generator': 0.25}


