# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
import warnings
from os import PathLike
from typing import Union, Dict, List
import pypowsybl._pypowsybl as _pp
from pypowsybl.report import ReportNode
from pypowsybl.utils import path_to_str
from .network import Network

DEPRECATED_REPORTER_WARNING = "Use of deprecated attribute reporter. Use report_node instead."


def _create_network(name: str, network_id: str = '', allow_variant_multi_thread_access: bool = False) -> Network:
    return Network(_pp.create_network(name, network_id, allow_variant_multi_thread_access))


def create_empty(network_id: str = "Default", allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an empty network.

    Args:
        network_id: id of the network, defaults to 'Default'

    Returns:
        a new empty network
    """
    return _create_network('empty', network_id, allow_variant_multi_thread_access)


def create_ieee9(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of IEEE 9 bus network

    Returns:
        a new instance of IEEE 9 bus network
    """
    return _create_network('ieee9', '', allow_variant_multi_thread_access)


def create_ieee14(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of IEEE 14 bus network

    Returns:
        a new instance of IEEE 14 bus network
    """
    return _create_network('ieee14', '', allow_variant_multi_thread_access)


def create_ieee30(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of IEEE 30 bus network

    Returns:
        a new instance of IEEE 30 bus network
    """
    return _create_network('ieee30', '', allow_variant_multi_thread_access)


def create_ieee57(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of IEEE 57 bus network

    Returns:
        a new instance of IEEE 57 bus network
    """
    return _create_network('ieee57', '', allow_variant_multi_thread_access)


def create_ieee118(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of IEEE 118 bus network

    Returns:
        a new instance of IEEE 118 bus network
    """
    return _create_network('ieee118', '', allow_variant_multi_thread_access)


def create_ieee300(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of IEEE 300 bus network

    Returns:
        a new instance of IEEE 300 bus network
    """
    return _create_network('ieee300', '', allow_variant_multi_thread_access)


def create_eurostag_tutorial_example1_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial

    Returns:
        a new instance of example 1 network of Eurostag tutorial
    """
    return _create_network('eurostag_tutorial_example1', '', allow_variant_multi_thread_access)

def create_eurostag_tutorial_example1_with_more_generators_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial, with a second generator

    Returns:
        a new instance of example 1 network of Eurostag tutorial with a second generator
    """
    return _create_network('eurostag_tutorial_example1_with_more_generators', '', allow_variant_multi_thread_access)


def create_eurostag_tutorial_example1_with_power_limits_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial with Power limits

    Returns:
        a new instance of example 1 network of Eurostag tutorial with Power limits
    """
    return _create_network('eurostag_tutorial_example1_with_power_limits', '', allow_variant_multi_thread_access)


def create_eurostag_tutorial_example1_with_tie_lines_and_areas(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial with tie lines and areas

    Returns:
        a new instance of example 1 network of Eurostag tutorial with tie lines and areas
    """
    return _create_network('eurostag_tutorial_example1_with_tie_lines_and_areas', '', allow_variant_multi_thread_access)


def create_four_substations_node_breaker_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of powsybl "4 substations" test case.

    It is meant to contain most network element types that can be
    represented in powsybl networks.
    The topology is in node-breaker representation.

    Returns:
        a new instance of powsybl "4 substations" test case
    """
    return _create_network('four_substations_node_breaker', '', allow_variant_multi_thread_access)


def create_four_substations_node_breaker_network_with_extensions(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of powsybl "4 substations" test case with ConnectablePosition and BusbarSectionPosition extensions.

    The topology is in node-breaker representation.
    """
    return _create_network('four_substations_node_breaker_with_extensions', '', allow_variant_multi_thread_access)


def create_micro_grid_be_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of micro grid BE CGMES test case

    Returns:
        a new instance of micro grid BE CGMES test case
    """
    return _create_network('micro_grid_be', '', allow_variant_multi_thread_access)


def create_micro_grid_nl_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of micro grid NL CGMES test case

    Returns:
        a new instance of micro grid NL CGMES test case
    """
    return _create_network('micro_grid_nl', '', allow_variant_multi_thread_access)


def create_metrix_tutorial_six_buses_network(allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Create an instance of metrix tutorial six buses test case

    Returns:
        a new instance of metrix tutorial six buses test case
    """
    return _create_network('metrix_tutorial_six_buses', '', allow_variant_multi_thread_access)


def is_loadable(file: Union[str, PathLike]) -> bool:
    """
      Check if a file is a loadable network.

      A file is loadable if:
       - file exists
       - it is a network format and format version supported by powsybl (so an importer exists for it)
       - the network file is well formatted
       - it is either not compressed or compressed with a supported compression format (gzip, bzip2, zip, xz)

      Args:
         file: path to the supposed network file

      Returns:
          True is the file is a loadable network, False otherwise
    """
    file = path_to_str(file)
    return _pp.is_network_loadable(file)


def load(file: Union[str, PathLike], parameters: Dict[str, str] = None, post_processors: List[str] = None, reporter: ReportNode = None,
         report_node: ReportNode = None, allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Load a network from a file. File should be in a supported format.

    Basic compression formats are also supported (gzip, bzip2).

    Args:
       file:       path to the network file
       parameters: a dictionary of import parameters
       post_processors: a list of import post processors (will be added to the ones defined by the platform config)
       reporter: deprecated, use report_node instead
       report_node: the reporter to be used to create an execution report, default is None (no report)
       allow_variant_multi_thread_access: allow multi-thread access to variant (default: False)

    Returns:
        The loaded network

    Examples:

        Some examples of file loading, including relative or absolute paths, and compressed files:

        .. code-block:: python

            network = pp.network.load('network.xiidm')
            network = pp.network.load('/path/to/network.xiidm')
            network = pp.network.load('network.xiidm.gz')
            network = pp.network.load('network.uct')
            ...
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    file = path_to_str(file)
    return Network(_pp.load_network(file,
                                    {} if parameters is None else parameters,
                                    [] if post_processors is None else post_processors,
                                    None if report_node is None else report_node._report_node,  # pylint: disable=protected-access
                                    allow_variant_multi_thread_access))


def load_from_binary_buffer(buffer: io.BytesIO, parameters: Dict[str, str] = None, post_processors: List[str] = None,
                            reporter: ReportNode = None, report_node: ReportNode = None, allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Load a network from a binary buffer.

    Args:
       buffer:    The BytesIO data buffer
       parameters:  A dictionary of import parameters
       post_processors: a list of import post processors (will be added to the ones defined by the platform config)
       reporter: deprecated, use report_node instead
       report_node: the reporter to be used to create an execution report, default is None (no report)
       allow_variant_multi_thread_access: allow multi-thread access to variant (default: False)

    Returns:
        The loaded network
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    return load_from_binary_buffers([buffer],
                                    {} if parameters is None else parameters,
                                    [] if post_processors is None else post_processors,
                                    reporter,
                                    report_node,
                                    allow_variant_multi_thread_access)


def load_from_binary_buffers(buffers: List[io.BytesIO], parameters: Dict[str, str] = None, post_processors: List[str] = None,
                             reporter: ReportNode = None, report_node: ReportNode = None, allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Load a network from a list of binary buffers. Only zipped CGMES are supported for several zipped source load.

    Args:
       buffers:  The list of BytesIO data buffer
       parameters:  A dictionary of import parameters
       post_processors: a list of import post processors (will be added to the ones defined by the platform config)
       reporter: deprecated, use report_node instead
       report_node: the reporter to be used to create an execution report, default is None (no report)
       allow_variant_multi_thread_access: allow multi-thread access to variant (default: False)

    Returns:
        The loaded network
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    buffer_list = []
    for buff in buffers:
        buffer_list.append(buff.getbuffer())
    return Network(_pp.load_network_from_binary_buffers(buffer_list,
                                                        {} if parameters is None else parameters,
                                                        [] if post_processors is None else post_processors,
                                                        None if report_node is None else report_node._report_node,  # pylint: disable=protected-access
                                                        allow_variant_multi_thread_access))


def load_from_string(file_name: str, file_content: str, parameters: Dict[str, str] = None, post_processors: List[str] = None,
                     reporter: ReportNode = None, report_node: ReportNode = None, allow_variant_multi_thread_access: bool = False) -> Network:
    """
    Load a network from a string. File content should be in a supported format.

    Args:
       file_name:    file name
       file_content: file content
       parameters:   a dictionary of import parameters
       post_processors: a list of import post processors (will be added to the ones defined by the platform config)
       reporter: deprecated, use report_node instead
       report_node: the reporter to be used to create an execution report, default is None (no report)
       allow_variant_multi_thread_access: allow multi-thread access to variant (default: False)

    Returns:
        The loaded network
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    return Network(_pp.load_network_from_string(file_name, file_content,
                                                {} if parameters is None else parameters,
                                                [] if post_processors is None else post_processors,
                                                None if report_node is None else report_node._report_node,  # pylint: disable=protected-access
                                                allow_variant_multi_thread_access))
