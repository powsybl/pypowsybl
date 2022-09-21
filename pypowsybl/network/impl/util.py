import pypowsybl._pypowsybl as _pp

from network.impl.network import _path_to_str
from pypowsybl.report import Reporter
from network import Network
from os import PathLike
from typing import (List, Union, Dict)
from pandas import DataFrame

from pypowsybl.util import create_data_frame_from_series_array


def _create_network(name: str, network_id: str = '') -> Network:
    return Network(_pp.create_network(name, network_id))


def create_empty(id: str = "Default") -> Network:
    """
    Create an empty network.

    Args:
        id: id of the network, defaults to 'Default'

    Returns:
        a new empty network
    """
    return _create_network('empty', network_id=id)


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

def create_battery_network() -> Network:
    """
    Create an instance of test case with batteries

    Returns:
        a new instance of test case with batteries
    """
    return _create_network('batteries')


def create_dangling_lines_network() -> Network:
    """
    Create an instance of test case with dangling lines

    Returns:
        a new instance of test case with dangling lines
    """
    return _create_network('dangling_lines')


def create_three_windings_transformer_network() -> Network:
    """
    Create an instance of test case with three windings transformers

    Returns:
        a new instance of test case with three windings transformers
    """
    return _create_network('three_windings_transformer')


def create_non_linear_shunt_network() -> Network:
    """
    Create an instance of test case with non linear shunts

    Returns:
        a new instance of test case with non linear shunts
    """
    return _create_network('non_linear_shunt')

def create_three_windings_transformer_with_current_limits_network() -> Network:
    """
   Create an instance of test case with three windings transformers with current limits

   Returns:
       a new instance of test case with three windings transformers with current limits
   """
    return _create_network('three_windings_transformer_with_current_limits')


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
    return create_data_frame_from_series_array(series_array)


def get_export_parameters(fmt: str) -> DataFrame:
    """
    Get supported export parameters infos for a given format

    Args:
       fmt (str): the format

    Returns:
        export parameters dataframe
    """
    series_array = _pp.create_exporter_parameters_series_array(fmt)
    return create_data_frame_from_series_array(series_array)


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
    file = _path_to_str(file)
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network(file, parameters,
                                    None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access


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


def get_extensions_names() -> List[str]:
    """
    Get the list of available extensions.

    Returns:
        the names of the available extensions
    """
    return _pp.get_extensions_names()
