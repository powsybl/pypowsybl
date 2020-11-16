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
- [GraalVM 20.1](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-20.1.0) with [native image](https://www.graalvm.org/reference-manual/native-image/#install-native-image)

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
..Powsybl versions:
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| Repository name       | Maven project version | Git branch | Git version                              | Build timestamp               |
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| powsybl-open-loadflow | 0.7.0                 | UNKNOWN    | 56e24b2262aaae28a065e3947caeb5759d753f97 | 2020-10-19T16:39:20.924+02:00 |
| powsybl-core          | 3.7.1                 | v3.7.1     | 44627f289a1ae0e7535f12ae9c3378f9a17182a1 | 2020-10-16T23:04:45.020+02:00 |
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+

.{network_0_iterations=3, network_0_status=CONVERGED}
.
----------------------------------------------------------------------
Ran 4 tests in 0.015s

OK
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
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| Repository name       | Maven project version | Git branch | Git version                              | Build timestamp               |
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| powsybl-open-loadflow | 0.7.0                 | UNKNOWN    | 56e24b2262aaae28a065e3947caeb5759d753f97 | 2020-10-19T16:39:20.924+02:00 |
| powsybl-core          | 3.7.1                 | v3.7.1     | 44627f289a1ae0e7535f12ae9c3378f9a17182a1 | 2020-10-16T23:04:45.020+02:00 |
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
```

Finally, we can create an IEEE 14 buses network and run a load flow computation:
```python
n = gp.network.create_ieee14()
r = gp.loadflow.run(n)
print(r.is_ok())
```

This will produce the following output:
```bash
{network_0_iterations=3, network_0_status=CONVERGED}
True
```

To go further, you can also load a case file instead of creating the IEEE 14 buses network:
```python
n = gp.network.load('test.uct')
```

