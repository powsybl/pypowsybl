#
# Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# SPDX-License-Identifier: MPL-2.0
#
"""
Training workload for the profile-guided optimization (PGO) of the pypowsybl native image.

Currently, only CGMES load and loadflow are implemented, the rest of the code paths will not be optimized.
On CGMES load, a major performance boost can be observed due to heavy branching logic, loadflow is less affected.

The profile is written to default.iprof in the current directory when the native image shutdown hooks
run, which pypowsybl only does (on exit) when the PYPOWSYBL_RUN_SHUTDOWN_HOOKS environment variable is set.
"""
import argparse
import io
import os
import sys
import time
from pathlib import Path
from typing import List

import pypowsybl as pp
import pypowsybl.loadflow as lf

DATA_DIR = Path(__file__).resolve().parents[2] / 'data'

CGMES_CASES = [
    'CGMES_Full.zip',
    'MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip',
]


def load_cgmes_full(case: str) -> pp.network.Network:
    return pp.network.load(DATA_DIR / case)


def load_cgmes_with_boundary() -> pp.network.Network:
    # CGMES_Partial.zip has no boundary set, it must be imported together with Boundary.zip
    buffers = []
    for name in ('CGMES_Partial.zip', 'Boundary.zip'):
        with open(DATA_DIR / name, 'rb') as fh:
            buffers.append(io.BytesIO(fh.read()))
    return pp.network.load_from_binary_buffers(buffers)


def run_loadflows(network: pp.network.Network) -> None:
    ac_params = lf.Parameters(distributed_slack=True)
    ac_result = lf.run_ac(network, ac_params)
    assert ac_result, 'AC load flow returned no result'
    dc_result = lf.run_dc(network)
    assert dc_result, 'DC load flow returned no result'
    # touch a few network data frames, they are used right after an import in most workflows
    network.get_buses()
    network.get_lines()
    network.get_generators()
    network.get_loads()


def iteration(extra_cases: List[Path]) -> None:
    for case in CGMES_CASES:
        run_loadflows(load_cgmes_full(case))
    run_loadflows(load_cgmes_with_boundary())
    for case in extra_cases:
        run_loadflows(pp.network.load(case))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--iterations', type=int, default=5,
                        help='Number of times the CGMES import + load flow sequence is run (default: 5)')
    parser.add_argument('--cases', type=Path, nargs='*', default=[], metavar='FILE',
                        help='Additional CGMES files (zip or directory) to import and run a load flow on. The bundled '
                             'test cases are small, real-size grids give a more representative profile.')
    args = parser.parse_args()

    if not os.environ.get('PYPOWSYBL_RUN_SHUTDOWN_HOOKS'):
        print('warning: PYPOWSYBL_RUN_SHUTDOWN_HOOKS is not set, an instrumented build will not write its profile',
              file=sys.stderr)

    print(f'pypowsybl {pp.__version__} loaded from {pp.__file__}')
    for i in range(args.iterations):
        start = time.perf_counter()
        iteration(args.cases)
        print(f'iteration {i + 1}/{args.iterations} done in {time.perf_counter() - start:.1f}s')


if __name__ == '__main__':
    main()
