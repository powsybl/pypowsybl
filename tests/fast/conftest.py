#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
"""Networks of the fast path tests.

Always: small networks shipped with pypowsybl (node/breaker and bus/breaker).
When present: every ``tests/assets/*.biidm`` (real snapshots, not committed) and the files listed, separated by
``os.pathsep``, in the ``PYPOWSYBL_FAST_TEST_NETWORKS`` environment variable.
"""
import os
import pathlib

import pytest

import pypowsybl as pp

ASSETS = pathlib.Path(__file__).parent.parent / 'assets'

FACTORIES = {
    'four_substations': pp.network.create_four_substations_node_breaker_network,
    'ieee14': pp.network.create_ieee14,
    'eurostag': pp.network.create_eurostag_tutorial_example1_network,
}


def _files():
    files = sorted(ASSETS.glob('*.biidm')) if ASSETS.is_dir() else []
    files += [pathlib.Path(p) for p in os.environ.get('PYPOWSYBL_FAST_TEST_NETWORKS', '').split(os.pathsep) if p]
    return files


def pytest_generate_tests(metafunc):
    if 'network' in metafunc.fixturenames:
        params = list(FACTORIES) + [str(f) for f in _files()]
        ids = list(FACTORIES) + [f.name for f in _files()]
        metafunc.parametrize('network', params, ids=ids, indirect=True)


@pytest.fixture
def network(request):
    """A fresh network per test: tests modify it."""
    if request.param in FACTORIES:
        return FACTORIES[request.param]()
    return pp.network.load(request.param)
