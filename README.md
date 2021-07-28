# PyPowSyBl

[![Actions Status](https://github.com/powsybl/pypowsybl/workflows/CI/badge.svg)](https://github.com/powsybl/pypowsybl/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=powsybl_pypowsybl&metric=alert_status)](https://sonarcloud.io/dashboard?id=powsybl_pypowsybl)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=powsybl_pypowsybl&metric=coverage)](https://sonarcloud.io/dashboard?id=powsybl_pypowsybl)
[![PyPI Latest Release](https://img.shields.io/pypi/v/pypowsybl.svg)](https://pypi.org/project/pypowsybl/)
[![Documentation Status](https://readthedocs.org/projects/pypowsybl/badge/?version=latest)](https://pypowsybl.readthedocs.io/en/latest/?badge=latest)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/powsybl)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-rzvbuzjk-nxi0boim1RKPS5PjieI0rA)


The PyPowSyBl project gives access PowSyBl Java framework to Python developers. This Python integration relies on
GraalVM to compile Java code to a native library.

## Installation

PyPowSyBl is released on [PyPi](https://pypi.org/project/pypowsybl/).

First, make sure you have an up-to-date version of pip and setuptools:
```bash
pip3 install --upgrade setuptools pip --user
```

Then you can install PyPowSyBl using pip:
```bash
pip3 install pypowsybl --user
```

## Build from sources

Requirements:

- Maven >= 3.1
- Cmake >= 3.14
- C++11 compiler
- Python >= 3.7
- [GraalVM 21.2.0](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-21.2.0) with [native image](https://www.graalvm.org/reference-manual/native-image/#install-native-image)

To build from sources and install PyPowSyBl package:
```bash
git clone --recursive https://github.com/powsybl/pypowsybl.git
export JAVA_HOME=<path to GraalVM>
pip3 install --upgrade setuptools pip --user
pip3 install . --user
```

To run unit tests:
```bash
python3 -m unittest discover --start-directory tests
```

## Usage

First, we have to import pypowsybl:
```python
import pypowsybl as pp
```

Then we can display the version of the PowSyBl modules:
```python
pp.print_version()
```

```bash
Powsybl versions:
+-----------------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| Repository name             | Maven project version | Git branch | Git version                              | Build timestamp               |
+-----------------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| powsybl-open-loadflow       | X.Y.Z                 |            |                                          |                               |
| powsybl-single-line-diagram | X.Y.Z                 |            |                                          |                               |
| powsybl-core                | X.Y.Z                 |            |                                          |                               |
+-----------------------------+-----------------------+------------+------------------------------------------+-------------------------------+
```

We can create an IEEE 14 buses network and run a load flow computation:
```python
n = pp.network.create_ieee14()
results = pp.loadflow.run_ac(n)
for result in results:
    print(result)
```

```bash
LoadFlowComponentResult(component_num=0, status=CONVERGED, iteration_count=3, slack_bus_id='VL4_0', slack_bus_active_power_mismatch=-0.006081)
```

We can re-run the load flow computation in DC mode:
```python
results = pp.loadflow.run_dc(n)
```

By default, the application read configs from `${HOME}/.itools/config.yml`
We can disable this with command :
```python
pp.set_config_read(False)
```

We can now get buses data (like any other network elements) as a [Pandas](https://pandas.pydata.org/) dataframe:
```python
buses = n.get_buses()
print(buses)
```

```bash
        v_mag  v_angle
VL1_0   1.060     0.00
VL2_0   1.045    -4.98
VL3_0   1.010   -12.72
VL4_0   1.019   -10.33
VL5_0   1.020    -8.78
VL6_0   1.070   -14.22
VL7_0   1.062   -13.37
VL8_0   1.090   -13.36
VL9_0   1.056   -14.94
VL10_0  1.051   -15.10
VL11_0  1.057   -14.79
VL12_0  1.055   -15.07
VL13_0  1.050   -15.16
VL14_0  1.036   -16.04
```

To disconnect or reconnect a line:
```python
n.disconnect('L1-2-1')
n.connect('L1-2-1')
```

To open or close a switch:
```python
n.open_switch('a_switch')
n.close_switch('a_switch')
```

To go further, you can also load a case file instead of creating the IEEE 14 buses network:
```python
n = pp.network.load('test.uct')
```

And dump the network to another format:
```python
n.dump('test.xiidm', 'XIIDM')
```

We can generate a single line diagram for a voltage level in the SVG format:
```python
n.write_single_line_diagram_svg('VL1', '/tmp/VL1.svg')
```

To run a security analysis and print results table:

```python
sa = pp.security.create_analysis()
sa.add_single_element_contingency('L1-2-1', 'c1')
sa.add_single_element_contingency('L2-3-1', 'c2')
sa.add_multiple_elements_contingency(['L1-2-1', 'L1-5-1'], 'c3')
sa_result = sa.run_ac(n)
print(sa_result.get_table())
```

```bash
+----------------+-----------+--------------+----------------+------------+-------+------------+---------------------+-----------------+-------+------+
| Contingency ID |   Status  | Equipment ID | Equipment name | Limit type | Limit | Limit name | Acceptable duration | Limit reduction | Value | Side |
+----------------+-----------+--------------+----------------+------------+-------+------------+---------------------+-----------------+-------+------+
|       c3       | CONVERGED |              |                |            |       |            |                     |                 |       |      |
|       c1       | CONVERGED |              |                |            |       |            |                     |                 |       |      |
|       c2       | CONVERGED |              |                |            |       |            |                     |                 |       |      |
+----------------+-----------+--------------+----------------+------------+-------+------------+---------------------+-----------------+-------+------+
```

To run a sensitivity analysis and print post contingency sensitivity matrix ([Pandas](https://pandas.pydata.org/) dataframe):

```python
sa = pp.sensitivity.create_dc_analysis()
sa.add_single_element_contingency('L1-2-1')
sa.set_branch_flow_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
sa_result = sa.run(n)
df = sa_result.get_branch_flows_sensitivity_matrix('L1-2-1')
print(df)
```

```bash
      L1-5-1    L2-3-1
B1-G     0.5 -0.084423
B2-G    -0.5  0.084423
B3-G    -0.5 -0.490385
```

To run a load flow with hades2 instead of OLF:

Download [Hades2](https://rte-france.github.io/hades2/index.html)

Create a `config.yml` under `$HOME/.itools/`
```yaml
hades2:
    homeDir: <path-to-hades2>
```

Then specify Hades2 provider:
```python
pp.loadflow.run_ac(n, provider="Hades2")
```
