#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import os
import re
import sys
import platform
import subprocess

from setuptools import setup, find_packages, Extension
from setuptools.command.install import install
from setuptools.command.build_ext import build_ext
from distutils.version import LooseVersion


extra_jars = ''

class PyPowsyblExtension(Extension):
    def __init__(self):
        Extension.__init__(self, '_pypowsybl', sources=['cpp/CMakeLists.txt',
                                                     'cpp/src/bindings.cpp',
                                                     'cpp/src/pypowsybl.cpp',
                                                     'cpp/src/pypowsybl.h',
                                                     'cpp/lib/pybind11/CMakeLists.txt',
                                                     'cpp/lib/pybind11/tools/FindCatch.cmake',
                                                     'cpp/lib/pybind11/tools/FindPythonLibsNew.cmake',
                                                     'cpp/lib/pybind11/tools/FindEigen3.cmake',
                                                     'cpp/lib/pybind11/tools/pybind11NewTools.cmake',
                                                     'cpp/lib/pybind11/tools/pybind11Tools.cmake',
                                                     'cpp/lib/pybind11/tools/pybind11Common.cmake',
                                                     'cpp/lib/pybind11/include/pybind11/detail/class.h',
                                                     'cpp/lib/pybind11/include/pybind11/detail/common.h',
                                                     'cpp/lib/pybind11/include/pybind11/detail/descr.h',
                                                     'cpp/lib/pybind11/include/pybind11/detail/init.h',
                                                     'cpp/lib/pybind11/include/pybind11/detail/internals.h',
                                                     'cpp/lib/pybind11/include/pybind11/detail/typeid.h',
                                                     'cpp/lib/pybind11/include/pybind11/attr.h',
                                                     'cpp/lib/pybind11/include/pybind11/buffer_info.h',
                                                     'cpp/lib/pybind11/include/pybind11/cast.h',
                                                     'cpp/lib/pybind11/include/pybind11/chrono.h',
                                                     'cpp/lib/pybind11/include/pybind11/common.h',
                                                     'cpp/lib/pybind11/include/pybind11/complex.h',
                                                     'cpp/lib/pybind11/include/pybind11/options.h',
                                                     'cpp/lib/pybind11/include/pybind11/eigen.h',
                                                     'cpp/lib/pybind11/include/pybind11/embed.h',
                                                     'cpp/lib/pybind11/include/pybind11/eval.h',
                                                     'cpp/lib/pybind11/include/pybind11/iostream.h',
                                                     'cpp/lib/pybind11/include/pybind11/functional.h',
                                                     'cpp/lib/pybind11/include/pybind11/numpy.h',
                                                     'cpp/lib/pybind11/include/pybind11/operators.h',
                                                     'cpp/lib/pybind11/include/pybind11/pybind11.h',
                                                     'cpp/lib/pybind11/include/pybind11/pytypes.h',
                                                     'cpp/lib/pybind11/include/pybind11/stl.h',
                                                     'cpp/lib/pybind11/include/pybind11/stl_bind.h',
                                                     'java/pom.xml',
                                                     'java/src/main/java/com/powsybl/python/PyPowsyblApi.java',
                                                     'java/src/main/resources/pypowsybl-api.h',
                                                     'java/src/main/resources/logback.xml',
                                                     'java/src/main/resources/META-INF/native-image/jni-config.json',
                                                     'java/src/main/resources/META-INF/native-image/proxy-config.json',
                                                     'java/src/main/resources/META-INF/native-image/reflect-config.json',
                                                     'java/src/main/resources/META-INF/native-image/resource-config.json'])


class PyPowsyblBuild(build_ext):
    def run(self):
        try:
            out = subprocess.check_output(['cmake', '--version'])
        except OSError:
            raise RuntimeError("CMake must be installed to build the following extensions: " +
                               ", ".join(e.name for e in self.extensions))

        if platform.system() == "Windows":
            cmake_version = LooseVersion(re.search(r'version\s*([\d.]+)', out.decode()).group(1))
            if cmake_version < '3.1.0':
                raise RuntimeError("CMake >= 3.1.0 is required on Windows")

        for ext in self.extensions:
            self.build_extension(ext)

    def build_extension(self, ext):
        extdir = os.path.abspath(os.path.dirname(self.get_ext_fullpath(ext.name)))
        # required for auto-detection of auxiliary "native" libs
        if not extdir.endswith(os.path.sep):
            extdir += os.path.sep
        global extra_jars
        if extra_jars:
            extra_jars = ':' + extra_jars
        cmake_args = ['-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=' + extdir,
                      '-DPYTHON_EXECUTABLE=' + sys.executable,
                      '-DEXTRA_JARS=' + extra_jars]

        cfg = 'Debug' if self.debug else 'Release'
        build_args = ['--config', cfg]

        if platform.system() == "Windows":
            cmake_args += ['-DCMAKE_LIBRARY_OUTPUT_DIRECTORY_{}={}'.format(cfg.upper(), extdir)]
            if sys.maxsize > 2**32:
                cmake_args += ['-A', 'x64']
            build_args += ['--', '/m']
        else:
            cmake_args += ['-DCMAKE_BUILD_TYPE=' + cfg]
            build_args += ['--', '-j2']

        env = os.environ.copy()
        env['CXXFLAGS'] = '{} -DVERSION_INFO=\\"{}\\"'.format(env.get('CXXFLAGS', ''),
                                                              self.distribution.get_version())
        if not os.path.exists(self.build_temp):
            os.makedirs(self.build_temp)

        cpp_source_dir=os.path.abspath('cpp')
        subprocess.check_call(['cmake', cpp_source_dir] + cmake_args, cwd=self.build_temp, env=env)
        subprocess.check_call(['cmake', '--build', '.'] + build_args, cwd=self.build_temp)


# long description from the github readme
with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()


class InstallCommand(install):
    user_options = install.user_options + [
        ('jars=', None, 'absolute path to jar would be included, separated by colon.'),
    ]

    def initialize_options(self):
        install.initialize_options(self)
        self.jars = ''

    def finalize_options(self):
        install.finalize_options(self)

    def run(self):
        global extra_jars
        extra_jars = self.jars
        install.run(self)


setup(
    name='pypowsybl',
    author='Geoffroy Jamgotchian',
    author_email="geoffroy.jamgotchian@gmail.com",
    description='A PowSyBl Python API',
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/powsybl/pypowsybl",
    packages=find_packages(),
    ext_modules=[PyPowsyblExtension()],
    cmdclass=dict(install=InstallCommand, build_ext=PyPowsyblBuild),
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
        'pandas'
    ],
)
