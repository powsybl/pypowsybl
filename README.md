# GridPy

[![Actions Status](https://github.com/gridsuite/gridpy/workflows/CI/badge.svg)](https://github.com/gridsuite/gridpy/actions)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

A PowSyBl Python binding POC, based on GraalVM.


## Requirements

To build this project, you need:
- Maven >= 3.5
- Cmake >= 3.14
- C++ compiler
- Python >= 3.7
- [GraalVM 20.3](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-20.3.0) with [native image](https://www.graalvm.org/reference-manual/native-image/#install-native-image)

## Build from sources

To build from sources and install GridPy package:
```bash
$> git clone --recursive https://github.com/gridsuite/gridpy.git
$> export JAVA_HOME=<path to GraalVM>
$> pip3 install . --user
```

To run unit tests:
```bash
$> python3 -m unittest tests/test.py
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

This will produce the following output:
```bash
Powsybl versions:
+-----------------------------+-----------------------+----------------------+------------------------------------------+-------------------------------+
| Repository name             | Maven project version | Git branch           | Git version                              | Build timestamp               |
+-----------------------------+-----------------------+----------------------+------------------------------------------+-------------------------------+
| powsybl-open-loadflow       | 0.8.0                 | release_local_branch | f9a97212f1dc8044ce594a955b99bd15d51166f0 | 2020-12-03T11:19:20.148+01:00 |
| powsybl-single-line-diagram | 1.8.0                 | release-v1.8.0       | 5f2ae361960b103859a59a398cb64b3feea3e6d5 | 2020-12-02T16:45:54.389+01:00 |
| powsybl-core                | 3.8.0                 | release-v3.8.0       | cc3a4fc671bc63d71af7d03b68f2e48b91fd2325 | 2020-12-02T10:34:03.925+01:00 |
+-----------------------------+-----------------------+----------------------+------------------------------------------+-------------------------------+
```

We can create an IEEE 14 buses network and run a load flow computation:
```python
n = gp.network.create_ieee14()
results = gp.loadflow.run_ac(n)
for result in results:
    print(result)
```

This will produce the following output:
```bash
LoadFlowComponentResult(component_num=0, status=CONVERGED, iteration_count=3, slack_bus_id='VL4_0', slack_bus_active_power_mismatch=-0.006081)
```

We can re-run the load flow computation in DC mode:
```python
gp.loadflow.run_dc(n)
```

Or with different parameters:
```python
parameters = gp.loadflow.Parameters(distributed_slack=False)
gp.loadflow.run_ac(n, parameters)
```

We can now iterate over buses and print calculated voltage
```python
for bus in n.get_buses():
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
n.write_single_line_diagram('VL1', '/tmp/VL1.svg')
```

To run a security analysis:
```python
sa = gp.security_analysis.create()
sa.add_single_element_contingency('L1-2-1', 'c1')
sa.add_single_element_contingency('L1-3-1', 'c2')
sa.add_multiple_elements_contingency(['L1-2-1', 'L1-3-1'], 'c3')
sa_result = sa.run_ac(n)
```

And print results table:
```python
print(sa_result.get_table())
```
