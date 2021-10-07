#
# Copyright (c) 2020-2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from skbuild import setup

# long description from the github readme
with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setup(
    name='pypowsybl',
    author='Geoffroy Jamgotchian',
    author_email="geoffroy.jamgotchian@gmail.com",
    description='A PowSyBl Python API',
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/powsybl/pypowsybl",
    packages=['pypowsybl'],
    cmake_source_dir='cpp',
    zip_safe=False,
    classifiers=[
        "Development Status :: 2 - Pre-Alpha",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3 :: Only",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: Implementation :: CPython",
        "License :: OSI Approved :: Mozilla Public License 2.0 (MPL 2.0)",
        "Operating System :: POSIX :: Linux",
        "Operating System :: MacOS",
        "Operating System :: Microsoft :: Windows",
    ],
    python_requires='>=3.7',
    install_requires=[
        'prettytable',
        'pandas',
        'networkx'
    ],
)
