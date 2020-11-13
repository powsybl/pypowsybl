# GridPy

A PowSyBl Python binding POC, based on GraalVM.

Prerequisite:
 - Maven >= 3.5
 - Cmake >= 3.14
 - C++ compiler
 - Python >= 3.7
 - [GraalVM 20.1](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-20.1.0)

To build from sources and install GrydPy package:
```bash
git clone --recursive https://github.com/gridsuite/gridpy.git
export JAVA_HOME=<path to GraalVM>
pip3 install . --user
```

To run unit tests:
```bash
python3 -m unittest tests/test.py
```

Usage example:
```python
import gridpy.network
import gridpy.loadflow
import gridpy as gp
gp.print_version()
```
```bash
Powsybl versions:
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| Repository name       | Maven project version | Git branch | Git version                              | Build timestamp               |
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
| powsybl-open-loadflow | 0.7.0                 | UNKNOWN    | 56e24b2262aaae28a065e3947caeb5759d753f97 | 2020-10-19T16:39:20.924+02:00 |
| powsybl-core          | 3.7.1                 | v3.7.1     | 44627f289a1ae0e7535f12ae9c3378f9a17182a1 | 2020-10-16T23:04:45.020+02:00 |
+-----------------------+-----------------------+------------+------------------------------------------+-------------------------------+
```
```python
n = gp.network.create_ieee14()
gp.loadflow.run(n)
```
```bash
{network_0_iterations=3, network_0_status=CONVERGED}
```
