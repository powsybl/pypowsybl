# Profile-guided optimization (PGO) of the native image

The Java part of pypowsybl is compiled ahead of time by GraalVM `native-image`. Without profiling
information the compiler has to guess which code is hot, how call sites are typed, etc. With
profile-guided optimization these decisions are driven by a profile collected from a real run,
which brings a significant speedup on branch-heavy workloads such as CGMES import.

PGO requires **Oracle GraalVM**, it is not available in GraalVM Community Edition.

## How it works

A PGO build is a three-pass build:

1. **Instrumented build**: `native-image --pgo-instrument` produces a library that records branch and type profiles while it runs, and writes them to `default.iprof` in the working directory when the native image shutdown hooks run.
2. **Instrumented run**: Running a training workload (workload.py) fills a profile. Getting the profile out of the shared library needs `-H:+ProfilingEnableProfileDumpHooks` and a shutdown hook that fully ends the jvm (enabled with PYPOWSYBL_RUN_SHUTDOWN_HOOKS env variable) in CommonCFunctions.
3. **Optimized build**: `native-image --pgo=<profile.iprof>` uses the collected profile.

Both build passes are exposed as CMake options of the `native-image` target (`PYPOWSYBL_PGO_INSTRUMENT`, `PYPOWSYBL_PGO_PROFILES`), and as environment variables read by `setup.py` (same names).


## Building a PGO optimized wheel

```bash
export JAVA_HOME=<path to Oracle GraalVM>
pip install -r requirements.txt
tools/pgo/build_pgo.sh
```

The script builds and installs an instrumented wheel, runs the training workload (`tools/pgo/workload.py`), then builds the optimized wheel into `dist/`. Intermediate files, including the collected profile `pypowsybl.iprof`, are kept in `build/pgo/`.

Options:

- `PYTHON=<interpreter>` to select the Python used for the build (default: `python3`).
- `PGO_PROFILE=<file.iprof>` to skip the first two steps and build directly with an existing profile.
- `BDIST_WHEEL_ARGS="..."` to forward extra arguments to `setup.py bdist_wheel`.
- Any other argument is forwarded to the workload, for example `--iterations 10` or
  `--cases /path/to/real_grid.zip` to add your own CGMES files to the training set (recommended,
  the bundled test cases are small).

## Continuous integration

The Full CI, Dev CI and fork workflows build the native libraries through
`.github/workflows/scripts/build_wheel.sh`, which runs `build_pgo.sh` whenever the native libraries
are actually rebuilt (the CI caches them, keyed on the Java, C++ and `tools/pgo` sources). A PGO build
roughly doubles the native image build time. Set the `PGO_BUILD` environment variable to `'false'`
at workflow level, or for one matrix entry, to fall back to a plain build. The snapshot workflow,
which only checks compatibility with upstream snapshots, uses a plain build.

## Running the steps by hand

```bash
# 1. instrumented build
PYPOWSYBL_PGO_INSTRUMENT=ON pip install .

# 2. collect a profile (run from outside the sources so that the installed package is used)
cd /tmp
PYPOWSYBL_RUN_SHUTDOWN_HOOKS=1 python <path to pypowsybl>/tools/pgo/workload.py
# -> /tmp/default.iprof

# 3. optimized build
PYPOWSYBL_PGO_PROFILES=/tmp/default.iprof pip install .
```

## Notes

- The instrumented library is noticeably slower than a regular build, this is expected. Do not use anywhere other than during training.
- `PYPOWSYBL_RUN_SHUTDOWN_HOOKS` is harmless on a regular build (the hooks run, nothing is written).
