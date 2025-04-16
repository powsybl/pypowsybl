#
# Copyright (c) 2020-2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import os
import re
import sys
import platform
import subprocess
import zipfile
import glob

from pybind11.setup_helpers import Pybind11Extension, build_ext
from setuptools import setup
from packaging.version import parse

class PyPowsyblExtension(Pybind11Extension):
    def __init__(self):
        Pybind11Extension.__init__(self, 'pypowsybl._pypowsybl',
                                   ["cpp/*"])


class PyPowsyblBuild(build_ext):
    def run(self):
        os.environ["USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM"] = 'false'
        try:
            out = subprocess.check_output(['cmake', '--version'])
        except OSError:
            raise RuntimeError("CMake must be installed to build the following extensions: " +
                               ", ".join(e.name for e in self.extensions))

        if platform.system() == "Windows":
            cmake_version = parse(re.search(r'version\s*([\d.]+)', out.decode()).group(1))
            if cmake_version < parse('3.1.0'):
                raise RuntimeError("CMake >= 3.1.0 is required on Windows")

        for ext in self.extensions:
            self.build_extension(ext)

    def build_extension(self, ext):
        extdir = os.path.abspath(os.path.dirname(self.get_ext_fullpath(ext.name)))
        # required for auto-detection of auxiliary "native" libs
        if not extdir.endswith(os.path.sep):
            extdir += os.path.sep
        cmake_args = ['-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=' + extdir,
                      '-DPython_EXECUTABLE=' + sys.executable]

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

        self.zipHeadersAndBinaries(extdir, cpp_source_dir)

    def zipHeadersAndBinaries(self, binary_dir, cpp_source_dir):
        binaries = dict()
        binaries['bin'] = []
        binaries['lib'] = []
        if platform.system() == "Windows":
            binaries['bin'] = [os.path.join(binary_dir, 'math.dll'),
                               os.path.join(binary_dir, 'pypowsybl-java.dll')]
            binaries['lib'] = [os.path.join(self.build_temp, 'java/pypowsybl-java.lib')]
        elif platform.system() == "Linux":
            binaries['lib'] = [os.path.join(binary_dir, 'libmath.so'),
                               os.path.join(binary_dir, 'libpypowsybl-java.so')]
        elif platform.system() == "Darwin" :
            binaries['lib'] = [os.path.join(binary_dir, 'libmath.dylib'),
                               os.path.join(binary_dir, 'libpypowsybl-java.dylib')]

        includes = glob.glob(os.path.join(cpp_source_dir, 'powsybl-cpp/') + '*.h')
        includes = includes + glob.glob(os.path.join(cpp_source_dir, 'pypowsybl-java/') + '*.h')
        includes = includes + glob.glob(os.path.join(self.build_temp, 'java/') + '*.h')

        if not os.path.exists(os.path.abspath('dist')):
            os.makedirs(os.path.abspath('dist'))

        binaries_archive = os.path.join(os.path.abspath('dist'), 'binaries.zip')
        if os.path.exists(binaries_archive):
            os.remove(binaries_archive)
        with zipfile.ZipFile(binaries_archive, mode='x') as archive:
            for binary in binaries['bin']:
                archive.write(binary, arcname=os.path.join('bin', os.path.basename(binary)))
            for lib in binaries['lib']:
                archive.write(lib, arcname=os.path.join('lib', os.path.basename(lib)))
            for include in includes:
                archive.write(include, arcname=os.path.join('include', os.path.basename(include)))


setup(
    ext_modules=[PyPowsyblExtension()],
    cmdclass=dict(build_ext=PyPowsyblBuild),
)
