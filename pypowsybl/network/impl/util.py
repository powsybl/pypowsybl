# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from os import PathLike
from typing import List, Optional, Dict, Union

from pandas import DataFrame

import pypowsybl._pypowsybl as _pp

from pypowsybl.util import _create_data_frame_from_series_array

# Type definition
ParamsDict = Optional[Dict[str, str]]
PathOrStr = Union[str, PathLike]


def _series_metadata_repr(self: _pp.SeriesMetadata) -> str:
    return f'SeriesMetadata(name={self.name}, type={self.type}, ' \
           f'is_index={self.is_index}, is_modifiable={self.is_modifiable}, is_default={self.is_default})'


_pp.SeriesMetadata.__repr__ = _series_metadata_repr  # type: ignore


def _path_to_str(path: PathOrStr) -> str:
    if isinstance(path, str):
        return path
    return path.__fspath__()


def get_import_formats() -> List[str]:
    """
    Get list of supported import formats

    Returns:
         the list of supported import formats
    """
    return _pp.get_network_import_formats()


def get_export_formats() -> List[str]:
    """
    Get list of supported export formats

    Returns:
        the list of supported export formats
    """
    return _pp.get_network_export_formats()


def get_import_parameters(fmt: str) -> DataFrame:
    """
    Supported import parameters for a given format.

    Args:
       fmt (str): the format

    Returns:
        import parameters dataframe

    Examples:
       .. doctest::

           >>> parameters = pp.network.get_import_parameters('PSS/E')
           >>> parameters.index.tolist()
           ['psse.import.ignore-base-voltage']
           >>> parameters['description']['psse.import.ignore-base-voltage']
           'Ignore base voltage specified in the file'
           >>> parameters['type']['psse.import.ignore-base-voltage']
           'BOOLEAN'
           >>> parameters['default']['psse.import.ignore-base-voltage']
           'false'
    """
    series_array = _pp.create_importer_parameters_series_array(fmt)
    return _create_data_frame_from_series_array(series_array)


def get_export_parameters(fmt: str) -> DataFrame:
    """
    Get supported export parameters infos for a given format

    Args:
       fmt (str): the format

    Returns:
        export parameters dataframe
    """
    series_array = _pp.create_exporter_parameters_series_array(fmt)
    return _create_data_frame_from_series_array(series_array)


def get_extensions_names() -> List[str]:
    """
    Get the list of available extensions.

    Returns:
        the names of the available extensions
    """
    return _pp.get_extensions_names()


def get_extensions_information() -> DataFrame:
    """
    Get more information about extensions

    Returns:
        a dataframe with information about extensions
    """
    return _create_data_frame_from_series_array(_pp.get_extensions_information())


def get_single_line_diagram_component_library_names() -> List[str]:
    """

    :return: the list of component library names that can be used with single line diagram
    """
    return _pp.get_single_line_diagram_component_library_names()
