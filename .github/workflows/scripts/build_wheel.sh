#!/bin/bash
#
# Builds the pypowsybl wheel into dist/ as the CI workflows expect it.
#
# The native libraries are built with profile-guided optimization (tools/pgo/build_pgo.sh) unless:
#   - PYPOWSYBL_NATIVE_BUILD_DIR is set: the native libraries come from the CI cache and are only packaged,
#   - PGO_BUILD is set to anything but "true": plain native image build, e.g. to work around a GraalVM PGO issue
#     on one platform (set it per workflow or per matrix entry).
#
# Environment variables: PYTHON (interpreter to use), BDIST_WHEEL_ARGS (extra "setup.py bdist_wheel" arguments).
set -euo pipefail

PYTHON="${PYTHON:-python3}"
PGO_BUILD="${PGO_BUILD:-true}"
BDIST_WHEEL_ARGS="${BDIST_WHEEL_ARGS:-}"

if [ -n "${PYPOWSYBL_NATIVE_BUILD_DIR:-}" ]; then
    echo "Packaging cached native libraries from $PYPOWSYBL_NATIVE_BUILD_DIR"
    # shellcheck disable=SC2086
    "$PYTHON" setup.py bdist_wheel $BDIST_WHEEL_ARGS
elif [ "$PGO_BUILD" != "true" ]; then
    echo "PGO_BUILD=$PGO_BUILD: building native libraries"
    # shellcheck disable=SC2086
    "$PYTHON" setup.py bdist_wheel $BDIST_WHEEL_ARGS
else
    echo "Building native libraries with profile-guided optimization"
    PYTHON="$PYTHON" BDIST_WHEEL_ARGS="$BDIST_WHEEL_ARGS" tools/pgo/build_pgo.sh
fi
