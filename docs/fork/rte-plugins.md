# Carrying the RTE plugins of pypowsybl-rte

This fork is built from the open-source pypowsybl (tag `v1.16.1`). The wheel RTE uses internally,
`pypowsybl-rte 1.16.1`, is the same code plus private plugins. This note lists what differs and what it would
take to produce a fast-path wheel that includes them. Nothing here has been attempted.

## What the RTE build has that this fork does not

Measured by comparing the two installed wheels (same powsybl-core 7.3.0, same Python API version):

| | Open build / this fork | Added by pypowsybl-rte |
|---|---|---|
| Import formats | BIIDM, CGMES, IEEE-CDF, JIIDM, MATPOWER, POWER-FACTORY, PSS/E, UCTE, XIIDM | **DIE** (the `.arc` / `.arc.xz` snapshots) |
| Export formats | AMPL, BIIDM, CGMES, JIIDM, MATPOWER, PSS/E, UCTE, XIIDM | **ADN** |
| Load flow / security analysis / sensitivity providers | OpenLoadFlow, DynaFlow | **Hades2** |
| Import post-processors | geoJsonImporter, loadflowResultsCompletion, replaceTieLinesByLines | **cvgLimitsGroupNamesPostProcessor**, **hades2** |
| Network extensions | the open-source set | **activeSeason**, **congestionManagement**, **currentLimitsPerSeason**, **stateOfCharge** (and `voltageLevelInvalidity`, present in the files) |

One consequence is already visible with `.biidm` files: the France snapshots contain `currentLimitsPerSeason`
(9,148 entries in the 2026-08-31 snapshot) and `voltageLevelInvalidity` (283) extensions. The open build logs
`Extensions [currentLimitsPerSeason, voltageLevelInvalidity] not found!` and drops them. Everything else in the
file loads identically. Code that needs seasonal limits has to keep using the RTE build until the plugins are
carried over.

## Why it should be a build-time change only

All of the above are powsybl plugins discovered through `ServiceLoader` (importers, exporters, extension
serializers, providers, post-processors). The fast paths of this fork do not touch any plugin mechanism:

- `FastNetwork` works on the `Network` object, whatever importer produced it. A network loaded from `.arc`
  by the DIE importer would be served the same way.
- The partial loading parameter (`pypowsybl.import.skip`) only applies to `.biidm` files. It has no effect on
  `.arc` loading; a similar option for DIE would have to be written inside the DIE importer itself.

So the work is to compile the fork's Java sources together with the RTE jars into the native image.

## What it would take

1. **Access to the RTE artifacts.** The plugin jars (DIE importer, ADN exporter, Hades2 integration, RTE
   extensions) and the Maven repository that serves them. They are not in `~/.m2` on this machine and are not
   public.
2. **The RTE build recipe.** Either:
   - the private repository that produces `pypowsybl-rte` (preferred: apply this fork's commits on top of it,
     or rebase it on this fork). The fork's changes are additive: three new Java classes in
     `com.powsybl.python.fast`, one branch in `NetworkCFunctions.loadNetwork`, one new C++ file, one Python
     package. Conflicts are unlikely.
   - or, starting from this fork: add the RTE jars as dependencies of `java/pypowsybl/pom.xml` (they are
     shaded into `pypowsybl-java.jar`), or pass them through the existing `EXTRA_JARS` variable of
     `cpp/pypowsybl-java/CMakeLists.txt`, which appends jars to the native-image class path.
3. **Native-image metadata for the plugins.** GraalVM needs reflection / resource / JNI configuration for
   anything loaded reflectively (`java/pypowsybl/src/main/resources/META-INF/native-image/`). The RTE build
   already has this for its plugins; it must be carried along with the jars. This is the part most likely to
   cost time if the recipe is not available: missing entries only fail at run time.
4. **Native libraries.** Hades2 is a native solver. The RTE wheel must ship or locate its binaries; how it
   does so (bundled in the wheel, or found on the machine) has to be read from the RTE recipe.
5. **Validation.** Run `tests/fast` against `.arc.xz` snapshots (the test harness only needs the loader to
   accept the file: add the pattern in `tests/fast/conftest.py`), and compare a `.arc` load in the new build
   with the same load in the current `pypowsybl-rte`.

## What is not known from here

- Whether `pypowsybl-rte` contains changes to the pypowsybl sources themselves, beyond added jars. The
  two `libpypowsybl-java.so` differ in size by about 11 MB, which is consistent with added plugins but proves
  nothing about source changes.
- The licence terms under which the RTE jars can be combined into a wheel built outside the RTE pipeline.
