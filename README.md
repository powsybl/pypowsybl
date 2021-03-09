# GridPy

[![Actions Status](https://github.com/gridsuite/gridpy/workflows/CI/badge.svg)](https://github.com/gridsuite/gridpy/actions)
[![PyPI Latest Release](https://img.shields.io/pypi/v/gridpy.svg)](https://pypi.org/project/gridpy/)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

A PowSyBl Python binding, based on GraalVM.


## Installation

GridPy is released on [PyPi](https://pypi.org/project/gridpy/), you can install it using pip:
```bash
pip3 install gridpy --user
```

## Build from sources

Requirements:

- Maven >= 3.1
- Cmake >= 3.14
- C++11 compiler 
- Python >= 3.7
- [GraalVM 20.3.0](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-20.3.0) with [native image](https://www.graalvm.org/reference-manual/native-image/#install-native-image)

To build from sources and install GridPy package:
```bash
git clone --recursive https://github.com/gridsuite/gridpy.git
export JAVA_HOME=<path to GraalVM>
pip3 install . --user
```

To run unit tests:
```bash
python3 -m unittest tests/test.py
```

## Usage

First, we have to import the network and load flow modules:
```python
import gridpy.network
import gridpy.loadflow
import gridpy as gp
```

Then we can display the version of the PowSyBl modules:
```python
gp.print_version()
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
n = gp.network.create_ieee14()
results = gp.loadflow.run_ac(n)
for result in results:
    print(result)
```

```bash
LoadFlowComponentResult(component_num=0, status=CONVERGED, iteration_count=3, slack_bus_id='VL4_0', slack_bus_active_power_mismatch=-0.006081)
```

We can re-run the load flow computation in DC mode:
```python
results = gp.loadflow.run_dc(n)
```

Or with different parameters:
```python
parameters = gp.loadflow.Parameters(distributed_slack=False)
results = gp.loadflow.run_ac(n, parameters)
```

We can now iterate over buses and print calculated voltage:
```python
for bus in n.buses:
    print(f"Bus {bus.id!r}: v_mag={bus.v_magnitude}, v_ang={bus.v_angle}")
```

```bash
Bus 'VL1_0': v_mag=1.06, v_ang=10.313243381060664
Bus 'VL2_0': v_mag=1.045, v_ang=5.330504871947214
Bus 'VL3_0': v_mag=1.01, v_ang=-2.4121176767072106
Bus 'VL4_0': v_mag=1.0176698517255092, v_ang=0.0
Bus 'VL5_0': v_mag=1.019513126069881, v_ang=1.5391224927328597
Bus 'VL6_0': v_mag=1.07, v_ang=-3.908001888907669
Bus 'VL7_0': v_mag=1.0615190502807328, v_ang=-3.0467156954546497
Bus 'VL8_0': v_mag=1.09, v_ang=-3.0467156954546497
Bus 'VL9_0': v_mag=1.0559312123363436, v_ang=-4.625603385486276
Bus 'VL10_0': v_mag=1.0509841969760743, v_ang=-4.784365794405052
Bus 'VL11_0': v_mag=1.0569062925416597, v_ang=-4.477688311883925
Bus 'VL12_0': v_mag=1.0551885297773924, v_ang=-4.762642162506649
Bus 'VL13_0': v_mag=1.0503816324228432, v_ang=-4.843335457191098
Bus 'VL14_0': v_mag=1.0355296164107972, v_ang=-5.720717197261967
```

We can also get buses data (like any other network elements) as a [Pandas](https://pandas.pydata.org/) dataframe:
```python
df = n.create_buses_data_frame()
print(df)
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
n = gp.network.load('test.uct')
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
import gridpy.security_analysis
```

```python
sa = gp.security_analysis.create()
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
import gridpy.sensitivity_analysis
```

```python
sa = gp.sensitivity_analysis.create()
sa.add_single_element_contingency('L1-2-1')
sa.set_factor_matrix(['L1-5-1', 'L2-3-1'], ['B1-G', 'B2-G', 'B3-G'])
sa_result = sa.run_dc(n)
df = sa_result.get_post_contingency_sensitivity_matrix('L1-2-1')
print(df)
```

```bash
      L1-5-1    L2-3-1
B1-G     0.5 -0.084423
B2-G    -0.5  0.084423
B3-G    -0.5 -0.490385
```