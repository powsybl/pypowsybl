# PyPowSyBl

[![Actions Status](https://github.com/powsybl/pypowsybl/workflows/CI/badge.svg)](https://github.com/powsybl/pypowsybl/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=powsybl_pypowsybl&metric=alert_status)](https://sonarcloud.io/dashboard?id=powsybl_pypowsybl)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=powsybl_pypowsybl&metric=coverage)](https://sonarcloud.io/dashboard?id=powsybl_pypowsybl)
[![PyPI Latest Release](https://img.shields.io/pypi/v/pypowsybl.svg)](https://pypi.org/project/pypowsybl/)
[![Documentation Status](https://readthedocs.org/projects/pypowsybl/badge/?version=latest)](https://pypowsybl.readthedocs.io/en/latest/?badge=latest)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-rzvbuzjk-nxi0boim1RKPS5PjieI0rA)

The PyPowSyBl project gives access PowSyBl Java framework to Python developers. This Python integration relies on
GraalVM to compile Java code to a native library.

## Documentation

Latest version of the documentation with API reference and many code samples is [here](https://pypowsybl.readthedocs.io/).  

## Notebooks

Notebooks demonstrating PyPowSyBl features can be found in this [repository](https://github.com/powsybl/pypowsybl-notebooks).

## Installation

PyPowSyBl is released on [PyPi](https://pypi.org/project/pypowsybl/) for Python 3.8 to 3.13, on Linux, Windows and MacOS.

First, make sure you have an up-to-date version of pip and setuptools:
```bash
pip install --upgrade setuptools pip
```

Then you can install PyPowSyBl using pip:
```bash
pip install pypowsybl
```

## Getting started

First, we have to import pypowsybl:
```python
import pypowsybl as pp
```

We can create an IEEE 14 buses network and run a load flow computation:
```python
n = pp.network.create_ieee14()
results = pp.loadflow.run_ac(n)
print(results)
```

```bash
[ComponentResult(connected_component_num=0, synchronous_component_num=0, status=CONVERGED, status_text=CONVERGED, iteration_count=3, reference_bus_id='VL1_0', slack_bus_results=[SlackBusResult(id='VL1_0', active_power_mismatch=-0.006730108618313579)], distributed_active_power=0.0)]
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

This is just a quick appetizer of PyPowSyBl features. PyPowsybl provides a lot more features:
security analysis, sensitivity analysis, handling of multiple file formats (including CGMES),
substation and network diagrams generation, ...
For more details and examples, go to the documentation and Jupyter notebooks.

## Build from sources

That section is intended for developers who wish to build pypowsybl from the sources in this repository.

Requirements:

- Maven >= 3.1
- Cmake >= 3.14
- C++11 compiler
- Python >= 3.8 for Linux, Windows and MacOS (amd64 and arm64)
- [Oracle GraalVM Java 17](https://www.graalvm.org/downloads/)

To build from sources and install PyPowSyBl package:

```bash
git clone --recursive https://github.com/powsybl/pypowsybl.git
export JAVA_HOME=<path to GraalVM>
pip install --upgrade setuptools pip
pip install -r requirements.txt
pip install .
```

While developing, you may find it convenient to use the developer (or editable)
mode of installation:

```bash
pip install -e .
# or, to build the C extension with debug symbols:
python setup.py build --debug develop --user
```

Please refer to pip and setuptools documentations for more information.

To run unit tests:

```bash
pytest tests
```

To run static type checking with `mypy`:
```bash
mypy -p pypowsybl
```

To run linting inspection with `pylint`:
```bash
pylint pypowsybl
```

## Contribute to documentation

To run the tests included in the documentation:

```bash
cd docs/
make doctest
```

And then, to build the documentation:

```bash
make html
```

Web pages are generated in repository _build/html/ for preview before opening a pull request.
You can for example open it with firefox browser:

```bash
firefox _build/html/index.html
```
