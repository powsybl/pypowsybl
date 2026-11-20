#!/usr/bin/env bash
#
# Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# SPDX-License-Identifier: MPL-2.0
#
# Builds a profile-guided optimized (PGO) pypowsybl wheel in three steps:
#   1. build a PGO instrumented wheel and install it,
#   2. run the training workload (CGMES import + load flow) to collect a profile,
#   3. build the final wheel with the collected profile.
#
# Requirements: Oracle GraalVM in JAVA_HOME (PGO is not available in GraalVM Community Edition),
# and a Python environment with the build requirements installed (pip install -r requirements.txt).
#
# Usage: tools/pgo/build_pgo.sh [extra args for the workload, e.g. --iterations 10]
#
# Environment variables:
#   PYTHON            Python interpreter to use (default: python3)
#   PGO_WORK_DIR      Where intermediate files are written (default: build/pgo)
#   PGO_PROFILE       Reuse an existing profile and skip steps 1 and 2 (default: none)
#   BDIST_WHEEL_ARGS  Extra arguments for "setup.py bdist_wheel" (e.g. --plat-name ...)
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PYTHON="${PYTHON:-python3}"
PGO_WORK_DIR="${PGO_WORK_DIR:-$REPO_DIR/build/pgo}"
BDIST_WHEEL_ARGS="${BDIST_WHEEL_ARGS:-}"

if [ -z "${JAVA_HOME:-}" ]; then
    echo "JAVA_HOME must point to an Oracle GraalVM installation" >&2
    exit 1
fi

# build_wheel <cmake build dir> <wheel output dir>
# Each pass gets its own cmake build dir: the Maven and native-image steps are cmake ExternalProjects whose
# stamp files can mark them as up to date in a reused build dir. The build steps are run explicitly and
# bdist_wheel is told to skip them, because bdist_wheel re-runs build_ext with its default build dir otherwise.
build_wheel() {
    # shellcheck disable=SC2086
    "$PYTHON" setup.py build_py build_ext --build-temp "$1" bdist_wheel --skip-build -d "$2" $BDIST_WHEEL_ARGS
}

mkdir -p "$PGO_WORK_DIR"
PGO_WORK_DIR="$(cd "$PGO_WORK_DIR" && pwd)"
PROFILE="${PGO_PROFILE:-$PGO_WORK_DIR/pypowsybl.iprof}"
cd "$REPO_DIR"

if [ -z "${PGO_PROFILE:-}" ]; then
    echo "==> Step 1/3: building PGO instrumented wheel"
    rm -rf "$PGO_WORK_DIR/instrumented"
    PYPOWSYBL_PGO_INSTRUMENT=ON build_wheel "$PGO_WORK_DIR/build-instrumented" "$PGO_WORK_DIR/instrumented"
    # first call installs the dependencies, second one replaces an already installed pypowsybl of the same version
    "$PYTHON" -m pip install "$PGO_WORK_DIR"/instrumented/*.whl
    "$PYTHON" -m pip install --force-reinstall --no-deps "$PGO_WORK_DIR"/instrumented/*.whl

    echo "==> Step 2/3: running training workload to collect profile in $PROFILE"
    rm -f "$PROFILE"
    # The instrumented library writes default.iprof in the working directory when pypowsybl closes. Run from a
    # dedicated directory, which also ensures the installed (instrumented) package is imported, not the sources.
    WORKLOAD_DIR="$PGO_WORK_DIR/workload"
    rm -rf "$WORKLOAD_DIR" && mkdir -p "$WORKLOAD_DIR"
    (cd "$WORKLOAD_DIR" && PYPOWSYBL_RUN_SHUTDOWN_HOOKS=1 "$PYTHON" "$REPO_DIR/tools/pgo/workload.py" "$@")
    if [ ! -s "$WORKLOAD_DIR/default.iprof" ]; then
        echo "The training workload did not produce $WORKLOAD_DIR/default.iprof" >&2
        exit 1
    fi
    mv "$WORKLOAD_DIR/default.iprof" "$PROFILE"
    # do not leave the instrumented library installed, later steps install the optimized wheel
    "$PYTHON" -m pip uninstall -y pypowsybl
    echo "Collected profile: $(du -h "$PROFILE" | cut -f1)"
else
    echo "==> Steps 1/3 and 2/3 skipped, reusing profile $PROFILE"
fi

echo "==> Step 3/3: building PGO optimized wheel with $PROFILE"
# shellcheck disable=SC2086
PYPOWSYBL_PGO_PROFILES="$PROFILE" build_wheel "$PGO_WORK_DIR/build-optimized" "$REPO_DIR/dist"
echo "PGO optimized wheel(s):"
ls -1 "$REPO_DIR"/dist/*.whl
