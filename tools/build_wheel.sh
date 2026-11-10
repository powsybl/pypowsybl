#!/usr/bin/env bash
# Builds the wheel (maven + GraalVM native-image + C++ extension) with uv, into dist/.
#
#   JAVA_HOME=<Oracle GraalVM 21> tools/build_wheel.sh [--incremental] [uv build options...]
#
# A full build takes about 5 minutes on 8 cores and needs ~27 GB for native-image.
# --incremental keeps build/ so that maven and native-image are not run again: only for changes limited to
# cpp/ and pypowsybl/ (a change under java/ needs a full build).
# NATIVE_IMAGE_OPTIONS (e.g. "-J-Xmx32g") is honoured by native-image; on a shared machine run this script
# through your resource limiter.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -x "${JAVA_HOME:-}/bin/native-image" ]; then
  echo "JAVA_HOME must point to an Oracle GraalVM 21 (bin/native-image not found in '${JAVA_HOME:-}')" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"

if [ "${1:-}" = "--incremental" ]; then
  shift
  rm -rf build/lib.* build/bdist.*
else
  rm -rf build
fi
exec uv build --wheel --out-dir dist "$@"
