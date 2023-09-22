# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import io
from os import PathLike
from typing import Union, Dict, List
import pypowsybl._pypowsybl as _pp
from pypowsybl.report import Reporter
from pypowsybl.utils import path_to_str
from .network import Network


def _create_network(name: str, network_id: str = '') -> Network:
    return Network(_pp.create_network(name, network_id))


def create_empty(network_id: str = "Default") -> Network:
    """
    Create an empty network.

    Args:
        network_id: id of the network, defaults to 'Default'

    Returns:
        a new empty network
    """
    return _create_network('empty', network_id=network_id)


def create_ieee9() -> Network:
    """
    Create an instance of IEEE 9 bus network

    Returns:
        a new instance of IEEE 9 bus network
    """
    return _create_network('ieee9')


def create_ieee14() -> Network:
    """
    Create an instance of IEEE 14 bus network

    Returns:
        a new instance of IEEE 14 bus network
    """
    return _create_network('ieee14')


def create_ieee30() -> Network:
    """
    Create an instance of IEEE 30 bus network

    Returns:
        a new instance of IEEE 30 bus network
    """
    return _create_network('ieee30')


def create_ieee57() -> Network:
    """
    Create an instance of IEEE 57 bus network

    Returns:
        a new instance of IEEE 57 bus network
    """
    return _create_network('ieee57')


def create_ieee118() -> Network:
    """
    Create an instance of IEEE 118 bus network

    Returns:
        a new instance of IEEE 118 bus network
    """
    return _create_network('ieee118')


def create_ieee300() -> Network:
    """
    Create an instance of IEEE 300 bus network

    Returns:
        a new instance of IEEE 300 bus network
    """
    return _create_network('ieee300')


def create_eurostag_tutorial_example1_network() -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial

    Returns:
        a new instance of example 1 network of Eurostag tutorial
    """
    return _create_network('eurostag_tutorial_example1')


def create_eurostag_tutorial_example1_with_power_limits_network() -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial with Power limits

    Returns:
        a new instance of example 1 network of Eurostag tutorial with Power limits
    """
    return _create_network('eurostag_tutorial_example1_with_power_limits')


def create_four_substations_node_breaker_network() -> Network:
    """
    Create an instance of powsybl "4 substations" test case.

    It is meant to contain most network element types that can be
    represented in powsybl networks.
    The topology is in node-breaker representation.

    Returns:
        a new instance of powsybl "4 substations" test case
    """
    return _create_network('four_substations_node_breaker')


def create_four_substations_node_breaker_network_with_extensions() -> Network:
    """
    Create an instance of powsybl "4 substations" test case with ConnectablePosition and BusbarSectionPosition extensions.

    The topology is in node-breaker representation.
    """
    return _create_network('four_substations_node_breaker_with_extensions')


def create_micro_grid_be_network() -> Network:
    """
    Create an instance of micro grid BE CGMES test case

    Returns:
        a new instance of micro grid BE CGMES test case
    """
    return _create_network('micro_grid_be')


def create_micro_grid_nl_network() -> Network:
    """
    Create an instance of micro grid NL CGMES test case

    Returns:
        a new instance of micro grid NL CGMES test case
    """
    return _create_network('micro_grid_nl')


def load(file: Union[str, PathLike], parameters: Dict[str, str] = None, reporter: Reporter = None) -> Network:
    """
    Load a network from a file. File should be in a supported format.

    Basic compression formats are also supported (gzip, bzip2).

    Args:
       file:       path to the network file
       parameters: a dictionary of import parameters
       reporter:   the reporter to be used to create an execution report, default is None (no report)

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
    file = path_to_str(file)
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network(file, parameters,
                                    None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access


def load_from_binary_buffer(buffer: io.BytesIO, parameters: Dict[str, str] = None,
                            reporter: Reporter = None) -> Network:
    """
    Load a network from a binary buffer.

    Args:
       buffer:    The BytesIO data buffer
       parameters:  A dictionary of import parameters
       reporter: The reporter

    Returns:
        The loaded network
    """
    return load_from_binary_buffers([buffer], parameters, reporter)


def load_from_binary_buffers(buffers: List[io.BytesIO], parameters: Dict[str, str] = None,
                             reporter: Reporter = None) -> Network:
    """
    Load a network from a list of binary buffers. Only zipped CGMES are supported for several zipped source load.

    Args:
       buffers:  The list of BytesIO data buffer
       parameters:  A dictionary of import parameters
       reporter: The reporter

    Returns:
        The loaded network
    """
    if parameters is None:
        parameters = {}
    buffer_list = []
    for b in buffers:
        buffer_list.append(b.getbuffer())
    return Network(_pp.load_network_from_binary_buffers(buffer_list, parameters,
                                                        None if reporter is None else reporter._reporter_model))


def load_from_string(file_name: str, file_content: str, parameters: Dict[str, str] = None,
                     reporter: Reporter = None) -> Network:
    """
    Load a network from a string. File content should be in a supported format.

    Args:
       file_name:    file name
       file_content: file content
       parameters:   a dictionary of import parameters

    Returns:
        The loaded network
    """
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network_from_string(file_name, file_content, parameters,
                                                None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access
