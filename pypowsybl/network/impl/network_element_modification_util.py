# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Optional, Union, Dict

import warnings
import pandas as pd
from pandas import DataFrame
from numpy.typing import ArrayLike
import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import ElementType
from pypowsybl._pypowsybl import NetworkModificationType
from pypowsybl.report import ReportNode
from pypowsybl.utils import (
    _adapt_df_or_kwargs,
    _create_c_dataframe,
    _create_properties_c_dataframe,
    create_data_frame_from_series_array
)
from .network import Network

DEPRECATED_REPORTER_WARNING = "Use of deprecated attribute reporter. Use report_node instead."


def create_line_on_line(network: Network,
                        df: DataFrame = None, raise_exception: bool = True,
                        reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Connects an existing voltage level to an existing line through a tee point.

    Connects an existing voltage level (in practice a voltage level where we have some loads or generations)
    to an existing line through a tee point. This method cuts an existing line in two, creating a fictitious
    voltage level between them (on a fictitious substation if asked).
    Then it links an existing voltage level to this fictitious voltage level by creating a new line
    described in the provided dataframe.

    Args:
        network: the network
        df: attributes as a dataframe, it should contain:

          - bbs_or_bus_id: the ID of the existing bus or bus bar section of the voltage level voltage_level_id.
          - new_line_id: ID of the new line
          - new_line_r: resistance of the new line, in ohms
          - new_line_x: reactance of the new line, in ohms
          - new_line_b1: shunt susceptance on side 1 of the new line
          - new_line_b2: shunt susceptance on side 2 of the new line
          - new_line_g1: shunt conductance on side 1 of the new line
          - new_line_g2: shunt conductance on side 2 of the new line
          - line_id: the id on of the line on which we want to create a tee point.
          - line1_id: when the initial line is cut, the line segment at side 1 has a given ID (optional).
          - line1_name: when the initial line is cut, the line segment at side 1 has a given name (optional).
          - line2_id: when the initial line is cut, the line segment at side 2 has a given ID (optional).
          - line2_name: when the initial line is cut, the line segment at side 2 has a given name (optional).
          - position_percent: when the existing line is cut in two lines, percent is equal to the ratio between the parameters of the first line
            and the parameters of the line that is cut multiplied by 100. 100 minus percent is equal to the ratio
            between the parameters of the second line and the parameters of the line that is cut multiplied by 100.
          - create_fictitious_substation: True to create the fictitious voltage level inside a fictitious substation (false by default).
          - fictitious_voltage_level_id: the ID of the fictitious voltage level (optional) containing the tee point.
          - fictitious_voltage_level_name: the name of the fictitious voltage level (optional) containing the tee point.
          - fictitious_substation_id: the ID of the fictitious substation (optional).
          - fictitious_substation_name: the name of the fictitious substation (optional).

        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
                         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
                  bbs_or_bus_id: the ID of the existing bus or bus bar section of the voltage level voltage_level_id.
        **kwargs: attributes as keyword arguments

    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    metadata = _pp.get_network_modification_metadata(NetworkModificationType.CREATE_LINE_ON_LINE)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df], NetworkModificationType.CREATE_LINE_ON_LINE,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def revert_create_line_on_line(network: Network, df: DataFrame = None, raise_exception: bool = True,
                               reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: str) -> None:
    """
    This method reverses the action done in the create_line_on_line method.
    It replaces 3 existing lines (with the same voltage level as the one on their side) with a new line,
    and eventually removes the existing voltage levels (tee point and tapped voltage level), if they contain no equipments
    anymore, except bus or bus bar section.

    Args:
        network: the network
        df: attributes as a dataframe, it should contain:

          - line_to_be_merged1_id: The id of the first line connected to the tee point.
          - line_to_be_merged2_id: The id of the second line connected to the tee point.
          - line_to_be_deleted: The tee point line that will be deleted
          - merged_line_id: The id of the new line from the two lines to be merged
          - merged_line_name: The name of the new line from the two lines to be merged (default to line id)

        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
                         be logged. Default is True.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        **kwargs: attributes as keyword arguments
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    metadata = _pp.get_network_modification_metadata(NetworkModificationType.REVERT_CREATE_LINE_ON_LINE)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df], NetworkModificationType.REVERT_CREATE_LINE_ON_LINE,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def connect_voltage_level_on_line(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                  reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Cuts an existing line in two lines and connects an existing voltage level between them.

    This method cuts an existing line in two lines and connect an existing voltage level between them. The voltage level should
    be added to the network just before calling this method, and should contain at least a configured bus in bus/breaker topology or a bus bar section in node/breaker topology.

    Args:
        network: the network
        df: attributes as a dataframe, it should contain:

          - bbs_or_bus_id: The ID of the configured bus or bus bar section to which the lines will be connected.
          - line_id: the ID ot the line on which the voltage level should be connected.
          - position_percent: when the existing line is cut, percent is equal to the ratio between the parameters of the first line
                              and the parameters of the line that is cut multiplied by 100. 100 minus percent is equal to the ratio
                              between the parameters of the second line and the parameters of the line that is cut multiplied by 100.
          - line1_id: when the initial line is cut, the line segment at side 1 will receive this ID (optional).
          - line1_name: when the initial line is cut, the line segment at side 1 will receive this name (optional).
          - line2_id: when the initial line is cut, the line segment at side 2 will receive this ID (optional).
          - line2_name: when the initial line is cut, the line segment at side 2 will receive this name (optional).

        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
                         be logged. Default is True.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        **kwargs: attributes as keyword arguments
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    metadata = _pp.get_network_modification_metadata(NetworkModificationType.CONNECT_VOLTAGE_LEVEL_ON_LINE)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df], NetworkModificationType.CONNECT_VOLTAGE_LEVEL_ON_LINE,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def revert_connect_voltage_level_on_line(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                         reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    This method reverses the action done in the connect_voltage_level_on_line method.
    It replaces 2 existing lines (with the same voltage level at one of their side) with a new line,
    and eventually removes the voltage level in common (switching voltage level),
    if it contains no equipments anymore, except bus or bus bar section.

    Args:
        network: the network
        df: attributes as a dataframe, it should contain:
            line1_id: The id of the first existing line
            line2_id: The id of the second existing line
            line_id: The id of the new line to be created
            line_name: The name of the line to be created (default to line_id)
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        **kwargs: attributes as keyword arguments
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    metadata = _pp.get_network_modification_metadata(NetworkModificationType.REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df],
                                    NetworkModificationType.REVERT_CONNECT_VOLTAGE_LEVEL_ON_LINE,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def create_load_bay(network: Network, df: DataFrame = None, raise_exception: bool = True, reporter: ReportNode = None,
                    report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Creates a load, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the load
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the load is connected to the busbar with a breaker and a closed
        disconnector. If the network has position extensions, the load will also be connected to every parallel
        busbar section with an open disconnector. If the voltage level is bus/breaker, the load is just connected to
        the bus.

        Valid attributes are:

        - **id**: the identifier of the new load
        - **name**: an optional human-readable name
        - **type**: optionally, the type of load (UNDEFINED, AUXILIARY, FICTITIOUS)
        - **p0**: active power load, in MW
        - **q0**: reactive power load, in MVar
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the load, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the load, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.LOAD, raise_exception, reporter, report_node, **kwargs)


def create_battery_bay(network: Network, df: DataFrame = None, raise_exception: bool = True,
                       reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Creates a battery, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the battery
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the battery is connected to the busbar with a breaker and a closed
        disconnector. If the network has position extensions, the battery will also be connected to every parallel
        busbar section with an open disconnector. If the voltage level is bus/breaker, the battery is just connected to
        the bus.

        Valid attributes are:

        - **id**: the identifier of the new battery
        - **name**: an optional human-readable name
        - **min_p**: minimum active power, in MW
        - **max_p**: maximum active power, in MW
        - **target_p**: active power consumption, in MW
        - **target_q**: reactive power consumption, in MVar
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the battery, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the battery, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.BATTERY, raise_exception, reporter, report_node, **kwargs)


def create_generator_bay(network: Network, df: DataFrame = None, raise_exception: bool = True,
                         reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Creates a generator, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the generator
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: Attributes as keyword arguments.

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the generator is connected to the busbar with a breaker and a closed
        disconnector. If the network has position extensions, the generator will also be connected to every parallel
        busbar section with an open disconnector. If the voltage level is bus/breaker, the generator is just connected to
        the bus.

        Valid attributes are:

        - **id**: the identifier of the new generator
        - **energy_source**: the type of energy source (HYDRO, NUCLEAR, ...)
        - **max_p**: maximum active power in MW
        - **min_p**: minimum active power in MW
        - **target_p**: target active power in MW
        - **target_q**: target reactive power in MVar, when the generator does not regulate voltage
        - **rated_s**: nominal power in MVA
        - **target_v**: target voltage in kV, when the generator regulates voltage
        - **voltage_regulator_on**: true if the generator regulates voltage
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the generator, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the generator, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.GENERATOR, raise_exception, reporter, report_node, **kwargs)


def create_dangling_line_bay(network: Network, df: DataFrame = None, generation_df: DataFrame = pd.DataFrame(),
                             raise_exception: bool = True, reporter: ReportNode = None, report_node: ReportNode = None,
                             **kwargs: ArrayLike) -> None:
    """
    Creates a dangling line, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the dangling line
        df: Attributes as a dataframe.
        generation_df: Optional dangling lines' generation part, only as a dataframe
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the dangling line is connected to the busbar with a breaker and a closed
        disconnector. If the network has position extensions, the dangling line will also be connected to every parallel
        busbar section with an open disconnector. If the voltage level is bus/breaker, the dangling line is just
        connected to the bus.

        Valid attributes for dangling line dataframe or named arguments are:

        - **id**: the identifier of the new line
        - **name**: an optional human-readable name
        - **p0**: the active power consumption, in MW
        - **q0**: the reactive power consumption, in MVar
        - **r**: the resistance, in Ohms
        - **x**: the reactance, in Ohms
        - **g**: the shunt conductance, in S
        - **b**: the shunt susceptance, in S
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the dangling line, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the dangling line, will fill the ConnectablePosition extension, default is BOTTOM.

        Dangling line generation information must be provided as a dataframe.
        Valid attributes are:

        - **id**: Identifier of the dangling line that contains this generation part
        - **min_p**: Minimum active power output of the dangling line's generation part
        - **max_p**: Maximum active power output of the dangling line's generation part
        - **target_p**: Active power target of the generation part
        - **target_q**: Reactive power target of the generation part
        - **target_v**: Voltage target of the generation part
        - **voltage_regulator_on**: ``True`` if the generation part regulates voltage
    """
    return _create_feeder_bay(network, [df, generation_df], ElementType.DANGLING_LINE, raise_exception, reporter, report_node,
                              **kwargs)


def create_shunt_compensator_bay(network: Network, shunt_df: DataFrame,
                                 linear_model_df: Optional[DataFrame] = None,
                                 non_linear_model_df: Optional[DataFrame] = None,
                                 raise_exception: bool = True, reporter: ReportNode = None,
                                 report_node: ReportNode = None) -> None:
    """
    Creates a shunt compensator, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the shunt compensator
        shunt_df: dataframe for shunt compensators data
        linear_model_df: dataframe for linear model sections data
        non_linear_model_df: dataframe for sections data
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the shunt compensator is connected to the busbar with a breaker and a
        closed disconnector. If the network has position extensions, the shunt compensator will also be connected to
        every parallel busbar section with an open disconnector. If the voltage level is bus/breaker, the
        shunt compensator is just connected to the bus.

        Valid attributes for the shunt compensators dataframe are:

        - **id**: the identifier of the new shunt
        - **name**: an optional human-readable name
        - **model_type**: either LINEAR or NON_LINEAR
        - **section_count**: the current count of connected sections
        - **target_v**: an optional target voltage in kV
        - **target_v**: an optional deadband for the target voltage, in kV
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the shunt compensator, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the shunt compensator, will fill the ConnectablePosition extension, default is BOTTOM.

        Valid attributes for the linear sections models are:

        - **id**: the identifier of the new shunt
        - **g_per_section**: the conductance, in Ohm, for each section
        - **b_per_section**: the susceptance, in Ohm, for each section
        - **max_section_count**: the maximum number of connectable sections

        This dataframe must have only one row for each shunt compensator.

        Valid attributes for the non-linear sections models are:

        - **id**: the identifier of the new shunt
        - **g**: the conductance, in Ohm, for this section
        - **b**: the susceptance, in Ohm, for this section

    """
    if linear_model_df is None:
        linear_model_df = pd.DataFrame()
    if non_linear_model_df is None:
        non_linear_model_df = pd.DataFrame()
    dfs: List[Optional[DataFrame]] = [shunt_df, linear_model_df, non_linear_model_df]
    return _create_feeder_bay(network, dfs, ElementType.SHUNT_COMPENSATOR, raise_exception, reporter, report_node)


def create_static_var_compensator_bay(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                      reporter: ReportNode = None, report_node: ReportNode = None,
                                      **kwargs: ArrayLike) -> None:
    """
    Creates a static var compensator, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the static var compensator
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the static var compensator is connected to the busbar with a breaker and
        a closed disconnector. If the network has position extensions, the static var compensator will also be
        connected to every parallel busbar section with an open disconnector. If the voltage level is bus/breaker, the
        static var compensator is just connected to the bus.

        Valid attributes are:

        - **id**: the identifier of the new SVC
        - **name**: an optional human-readable name
        - **b_max**: the maximum susceptance, in S
        - **b_min**: the minimum susceptance, in S
        - **regulation_mode**: the regulation mode (VOLTAGE, REACTIVE_POWER, OFF)
        - **target_v**: the target voltage, in kV, when the regulation mode is VOLTAGE
        - **target_q**: the target reactive power, in MVar, when the regulation mode is not VOLTAGE
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the static var compensator, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the static var compensator, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.STATIC_VAR_COMPENSATOR, raise_exception, reporter, report_node,
                              **kwargs)


def create_lcc_converter_station_bay(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                     reporter: ReportNode = None, report_node: ReportNode = None,
                                     **kwargs: ArrayLike) -> None:
    """
    Creates a lcc converter station, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the lcc converter station
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the lcc converter station is connected to the busbar with a breaker and
        a closed disconnector. If the network has position extensions, the lcc converter station will also be connected
        to every parallel busbar section with an open disconnector. If the voltage level is bus/breaker, the
        lcc converter station is just connected to the bus.

        Valid attributes are:

        - **id**: the identifier of the new station
        - **name**: an optional human-readable name
        - **power_factor**: the power factor (ratio of the active power to the apparent power)
        - **loss_factor**: the loss factor of the station
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the lcc converter station, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the lcc converter station, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.LCC_CONVERTER_STATION, raise_exception, reporter, report_node,
                              **kwargs)


def create_vsc_converter_station_bay(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                     reporter: ReportNode = None, report_node: ReportNode = None,
                                     **kwargs: ArrayLike) -> None:
    """
    Creates a vsc converter station, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the vsc converter station
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the vsc converter station is connected to the busbar with a breaker and
        a closed disconnector. If the network has position extensions, the vsc converter station will also be connected
        to every parallel busbar section with an open disconnector. If the voltage level is bus/breaker, the
        vsc converter station is just connected to the bus.

        Valid attributes are:

        - **id**: the identifier of the new station
        - **name**: an optional human-readable name
        - **loss_factor**: the loss factor of the new station
        - **voltage_regulator_on**: true if the station regulated voltage
        - **target_v**: the target voltage, in kV, when the station regulates voltage
        - **target_q**: the target reactive power, in MVar, when the station does not regulate voltage
        - **bus_or_busbar_section_id**: id of the bus or of the busbar section to which the injection will be connected with a closed disconnector.
        - **position_order**: in node/breaker, the order of the vsc converter station, will fill the ConnectablePosition extension
        - **direction**: optionally, in node/breaker, the direction of the vsc converter station, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.VSC_CONVERTER_STATION, raise_exception, reporter, report_node,
                              **kwargs)


def _create_feeder_bay(network: Network, dfs: List[Optional[DataFrame]], element_type: _pp.ElementType,
                       raise_exception: bool, reporter: Optional[ReportNode], report_node: Optional[ReportNode],
                       **kwargs: ArrayLike) -> None:
    """
    Creates an injection, connects it to the network on a given bus or busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the feeder
        dfs: Attributes as a dataframe.
        element_type: the type of the element to be added.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section can be described in node/breaker or bus/breaker topology.
        If the voltage level is node/breaker, the injection is connected to the busbar with a breaker and a closed
        disconnector. If the network has position extensions, the injection will also be connected to every parallel
        busbar section with an open disconnector. If the voltage level is bus/breaker, the injection is just connected
        to the bus.

    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    metadata = _pp.get_network_modification_metadata_with_element_type(NetworkModificationType.CREATE_FEEDER_BAY,
                                                                       element_type)
    c_dfs = _get_c_dataframes_and_add_element_type(dfs, metadata, element_type, **kwargs)
    _pp.create_network_modification(network._handle, c_dfs, NetworkModificationType.CREATE_FEEDER_BAY, raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def replace_tee_point_by_voltage_level_on_line(network: Network, df: DataFrame = None,
                                               raise_exception: bool = True, reporter: ReportNode = None, report_node: ReportNode = None,
                                               **kwargs: ArrayLike) -> None:
    """
    This method transforms the action done in the create_line_on_line function into the action done in the connect_voltage_level_on_line.

    Args:
        network: the network in which the busbar sections are.
        df: Attributes as a dataframe. It should contain:

          - tee_point_line1: The ID of the existing line connecting the first voltage level to the tee point
          - tee_point_line2: The ID of the existing line connecting the tee point to the second voltage level
          - tee_point_line_to_remove: The ID of the existing line connecting the tee point to the attached voltage level
          - bbs_or_bus_id: The ID of the existing bus or bus bar section in the attached voltage level voltageLevelId,
            where we want to connect the new lines new line 1 and new line 2
          - new_line1_id: The ID of the new line connecting the first voltage level to the attached voltage level
          - new_line2_id: The ID of the new line connecting the second voltage level to the attached voltage level
          - new_line1_name: The optional name of the new line connecting the first voltage level to the attached voltage level
          - new_line2_name: The optional name of the new line connecting the second voltage level to the attached voltage level

        raise_exception: whether an exception should be raised if a problem occurs. By default, true.
        reporter: an optional reporter to get functional logs.
        kwargs: attributes as keyword arguments.

    Notes:
        It replaces 3 existing lines (with the same voltage level at one of their side (tee point)) with two new lines,
        and removes the tee point.
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter
    metadata = _pp.get_network_modification_metadata(NetworkModificationType.REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df],
                                    NetworkModificationType.REPLACE_TEE_POINT_BY_VOLTAGE_LEVEL_ON_LINE,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def create_voltage_level_topology(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                  reporter: ReportNode = None, report_node: ReportNode = None,
                                  **kwargs: ArrayLike) -> None:
    """
    Creates the topology of a given symmetrical voltage level, containing a given number of busbar with a given number
    of sections.

    Args:
        network: the network in which the busbar sections are.
        df: Attributes as a dataframe.
        raise_exception: whether an exception should be raised if a problem occurs. By default, true.
        reporter: deprecated, use report_node instead
        report_node: an optional reporter to get functional logs.
        kwargs: attributes as keyword arguments.
    Notes:
        The voltage level must be created and in node/breaker or bus/breaker topology.
        In node/breaker topology, busbar sections will be created, as well as disconnectors or breakers between each
        section depending on the switch_kind list.
        In bus/breaker topology, a matrix of buses will be created containing section_count x aligned_buses_or_busbar_count
        buses. The buses on the same row of the matrix will be connected via a breaker.

        The input dataframe expects these attributes:
        - **voltage_level_id**: the identifier of the voltage level where the topology should be created.
        - **low_bus_or_busbar_index**: the lowest bus or busbar index to be used. By default, 1 (no other buses or
        busbar sections).
        - **aligned_buses_or_busbar_count**: the total number of busbar or rows of buses to be created.
        - **low_section_index**: the lowest section index to be used. By default, 1.
        - **bus_or_busbar_section_prefix_id**: an optional prefix to put on the names of the created buses or
        busbar sections. By default, nothing.
        - **switch_prefix_id**: an optional prefix to put on the names of the created switches. By default, nothing.
        - **switch_kinds**: string or list containing the type of switch between each section. It should contain
        section_count - 1 switches and should look like that 'BREAKER, DISCONNECTOR' or ['BREAKER', 'DISCONNECTOR'].
        - **section_count**: optionally in node/breaker, required in bus/breaker, the number of sections to be created.

    Examples:

    .. code-block:: python

       pp.network.create_voltage_level_topology(network=network, raise_exception=True, id='VL',
                                                aligned_buses_or_busbar_count=3, switch_kinds='BREAKER, DISCONNECTOR')
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    metadata = _pp.get_network_modification_metadata(NetworkModificationType.VOLTAGE_LEVEL_TOPOLOGY_CREATION)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    if 'switch_kinds' in df.columns:
        df['switch_kinds'] = df['switch_kinds'].map(transform_list_to_str)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df], NetworkModificationType.VOLTAGE_LEVEL_TOPOLOGY_CREATION,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def transform_list_to_str(entry: Union[str, List[str]]) -> str:
    if isinstance(entry, list):
        return ','.join(str(e.replace(' ', '')) for e in entry)
    return entry.replace(' ', '')


def create_coupling_device(network: Network, df: DataFrame = None, raise_exception: bool = True,
                           reporter: ReportNode = None, report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Creates a coupling device on the network between two busbar sections of a same voltage level.

    Args:
        network: the network in which the busbar sections are.
        df: Attributes as a dataframe.
        raise_exception: an optional boolean indicating if an exception should be raised in case an error occurs during
        computation. By default, true.
        reporter: deprecated, use report_node instead
        report_node: an optional reporter to store the funtional logs.
        kwargs: Attributes as keyword arguments.

    Notes:
        The voltage level containing the busbar sections can be described in node/breaker or bus/breaker topology.
        In node/breaker topology, a closed breaker will be created as well as a closed disconnector on both given busbar
        sections to connect them. If the topology extensions are present on the busbar sections then on every parallel
        busbar section, an open disconnectors will be created to connect them to the breaker. If the two given busbar
        sections are the only two parallel busbar sections, and they have the same section index then, only two closed
        disconnectors will be created.
        In bus/breaker topology, a closed breaker will be created between two buses.

        The input dataframe expects these attributes:

        - **bus_or_busbar_section_id_1**: the identifier of the bus or of the busbar section on side 1
        - **bus_or_busbar_section_id_2**: the identifier of the bus or of the busbar section on side 2
        - **switch_prefix_id**: an optional prefix for all the switches

    Examples:

        .. code-block:: python

            pp.network.create_coupling_device(
                            network, bus_or_busbar_section_id_1='BBS1', bus_or_busbar_section_id_2='BBS2',
                            switch_prefix_id='sw')

    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    metadata = _pp.get_network_modification_metadata(NetworkModificationType.CREATE_COUPLING_DEVICE)
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df], NetworkModificationType.CREATE_COUPLING_DEVICE,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def _get_c_dataframes_and_add_element_type(dfs: List[Optional[DataFrame]],
                                           metadata: List[List[_pp.SeriesMetadata]],
                                           element_type: _pp.ElementType, **kwargs: ArrayLike) -> List[
    Optional[_pp.Dataframe]]:
    c_dfs: List[Optional[_pp.Dataframe]] = []
    dfs[0] = _adapt_df_or_kwargs(metadata[0], dfs[0], **kwargs)
    if dfs[0] is not None:
        dfs[0]['feeder_type'] = element_type.name
    for i, df in enumerate(dfs):
        if df is None:
            c_dfs.append(None)
        else:
            c_dfs.append(_create_c_dataframe(df, metadata[i]))
    return c_dfs


def get_unused_order_positions_after(network: Network, busbar_section_id: str) -> Optional[pd.Interval]:
    """
    Gets all the available order positions after a busbar section.

    Args:
        network: the network in which the busbar section is.
        busbar_section_id: the id of the busbar section from which we want to get all the available positions after.

    Notes:
        Gets all the available positions between the highest used position of a given busbar section and the
        lowest used position of the busbar section with the section index equal to the section index of the given
        busbar section plus one. The result is a list with the lowest available value as the first integer and the
        highest available value as the second integer.
        About order positions: order positions represent the relative positions of every connectable
        compared to each other on a busbar section. It is filled in the ConnectablePosition extension
        under Order. Each connectable has as many positions as it has feeders.

    Examples:
        Let's take two busbar sections. The first one, bbs1, has 3 feeders with taken order positions 5,6,7. The
        second, bbs2 has two feeder with taken order positions 11 and 12.
        Then, get_unused_order_positions_after(bbs1) will return [8, 10] as an interval and
        get_unused_order_positions_before(bbs2) will return [13, +infinity] as an interval.
    """
    positions = _pp.get_unused_order_positions(network._handle, busbar_section_id, 'AFTER')
    if len(positions) == 0:
        return None
    return pd.Interval(left=positions[0], right=positions[1], closed='both')


def remove_voltage_levels(network: Network, voltage_level_ids: Union[str, List[str]], raise_exception: bool = True,
                          reporter: ReportNode = None, report_node: ReportNode = None) -> None:
    """
    Remove all voltage levels from a list and all their connectables.
    The lines and two windings transformers will also be removed in the voltage level on the other side as well as their switches.
    The HVDC lines will be removed and their converter station too on both side.

    Args:
        network: the network from which we want to remove the voltage levels
        voltage_level_ids: either a list or a single string to indicate which voltage levels should be removed.
        raise_exception: optionally, whether the calculation should raise exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    if isinstance(voltage_level_ids, str):
        voltage_level_ids = [voltage_level_ids]
    _pp.remove_elements_modification(network._handle, voltage_level_ids, None,
                                     _pp.RemoveModificationType.REMOVE_VOLTAGE_LEVEL, raise_exception,
                                     None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def remove_hvdc_lines(network: Network, hvdc_line_ids: Union[str, List[str]],
                      shunt_compensator_ids: Dict[str, Union[str, List[str]]] = None,
                      raise_exception: bool = True, reporter: ReportNode = None,
                      report_node: ReportNode = None) -> None:
    """
    Removes hvdc lines and their LCC or SVC converter stations. In the case of a LCC converter station, a list of shunt
    compensators can be specified to be deleted as well.

    Args:
        network: the network containing the HVDC lines
        hvdc_line_ids: the ids of the HVDC lines, either as a string or a list of strings.
        shunt_compensator_ids: the ids of the shunt compensators associated to
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    c_df = None
    if isinstance(hvdc_line_ids, str):
        hvdc_line_ids = [hvdc_line_ids]
    if shunt_compensator_ids is not None:
        shunt_compensator_ids = {k: ', '.join(map(str, v)) if isinstance(v, list) else v for k, v in
                                 shunt_compensator_ids.items()}
        df = pd.DataFrame(shunt_compensator_ids, index=['shunt_compensator'])
        df.index.name = 'shunt_compensator'
        c_df = _create_properties_c_dataframe(df)
    _pp.remove_elements_modification(network._handle, hvdc_line_ids, c_df, _pp.RemoveModificationType.REMOVE_HVDC_LINE,
                                     raise_exception,
                                     None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def get_connectables_order_positions(network: Network, voltage_level_id: str) -> DataFrame:
    """
    Gets the order positions of every connectable of a given voltage level in a dataframe.

    Args:
        network: the network containing the voltage level.
        voltage_level_id: id of the voltage level for which we want to get the order positions.

    Note:
        About order positions: order positions represent the relative positions of every connectable
        compared to each other on a busbar section. It is filled in the ConnectablePosition extension
        under Order. Each connectable has as many positions as it has feeders. In this method, we get all the
        taken order positions by every connectable at the scale of a voltage level.
    """
    series_array = _pp.get_connectables_order_positions(network._handle, voltage_level_id)
    position_df = create_data_frame_from_series_array(series_array).sort_values(by=['order_position'])
    position_df['extension_name'] = position_df.apply(lambda row: row['extension_name'].rstrip(), axis=1)
    return position_df


def get_unused_order_positions_before(network: Network, busbar_section_id: str) -> Optional[pd.Interval]:
    """
    Gets all the available order positions before a busbar section.

    Args:
        network: the network in which the busbar section is.
        busbar_section_id: the id of the busbar section from which we want to get all the available positions before.

    Notes:
        Gets all the available positions between the lowest used position of a given busbar section and the
        highest used position of the busbar section with the section index equal to the section index of the given
        busbar section minus one. The result is an interval that includes the lowest available value and the
        highest available value.
        About order positions: order positions represent the relative positions of every connectable
        compared to each other on a busbar section. It is filled in the ConnectablePosition extension
        under Order. Each connectable has as many positions as it has feeders.

    Examples:
        Let's take two busbar sections. The first one, bbs1, has 3 feeders with taken order positions 5,6,7. The
        second, bbs2 has two feeder with taken order positions 11 and 12.
        Then, get_unused_order_positions_before(bbs1) will return [-infinity, 4] as an interval and
        get_unused_order_positions_before(bbs2) will return [8,10] as an interval.

    """
    positions = _pp.get_unused_order_positions(network._handle, busbar_section_id, 'BEFORE')
    if len(positions) == 0:
        return None
    return pd.Interval(left=positions[0], right=positions[1], closed='both')


def create_line_bays(network: Network, df: DataFrame = None, raise_exception: bool = True, reporter: ReportNode = None,
                     report_node: ReportNode = None, **kwargs: ArrayLike) -> None:
    """
    Creates a line and connects it to buses or busbar sections through standard feeder bays.

    In node/breaker topology, the created bays are composed of one breaker, and one disconnector for each busbar section
    parallel to the section specified in arguments. Only the disconnector on the specified
    section is closed, others are left open.
    In bus/breaker topology, the line is connected to the bus.

    Args:
        network: the network to which we want to add the new line
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
                         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: Attributes as keyword arguments.

    Notes:

        The input dataframe expects same attributes as :meth:`Network.create_lines`, except for the
        additional following attributes:

        - **bus_or_busbar_section_id_1**: the identifier of the bus or of the busbar section on side 1
        - **position_order_1**: in node/breaker, the position of the line on side 1
        - **direction_1**: optionally, in node/breaker, the direction, TOP or BOTTOM, of the line on side 1
        - **bus_or_busbar_section_id_2**: the identifier of the bus or of the busbar section on side 2
        - **position_order_2**: in node/breaker, the position of the line on side 2
        - **direction_2**: optionally, in node/breaker, the direction, TOP or BOTTOM, of the line on side 2

    Examples:

        .. code-block:: python

            pp.network.create_line_bays(network, id='L', r=0.1, x=10, g1=0, b1=0, g2=0, b2=0,
                                        bus_or_busbar_section_id_1='BBS1',
                                        position_order_1=115,
                                        direction_1='TOP',
                                        bus_or_busbar_section_id_2='BBS2',
                                        position_order_2=121,
                                        direction_2='BOTTOM')

    See Also:
        :meth:`Network.create_lines`
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    metadata = _pp.get_network_modification_metadata_with_element_type(NetworkModificationType.CREATE_LINE_FEEDER,
                                                                       ElementType.LINE)[0]
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df], NetworkModificationType.CREATE_LINE_FEEDER,
                                    raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def create_2_windings_transformer_bays(network: Network, df: DataFrame = None, raise_exception: bool = True,
                                       reporter: ReportNode = None, report_node: ReportNode = None,
                                       **kwargs: ArrayLike) -> None:
    """
    Creates a transformer and connects it to buses or busbar sections through standard feeder bays.

    In node/breaker topology, the created bays are composed of one breaker, and one disconnector for each busbar section
    parallel to the section specified in arguments. Only the disconnector on the specified
    section is closed, others are left open.

    In bus/breaker topology, the transformer is simply connected to the buses.

    Args:
        network: the network to which we want to add the new line
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: Attributes as keyword arguments.

    Notes:

        The input dataframe expects same attributes as :meth:`Network.create_2_windings_transformers`, except for the
        additional following attributes:

        - **bus_or_busbar_section_id_1**: the identifier of the bus or of the busbar section on side 1
        - **position_order_1**: in node/breaker topology, the position of the transformer on side 1
        - **direction_1**: optionally, in node/breaker, the direction, TOP or BOTTOM, of the transformer on side 1
        - **bus_or_busbar_section_id_2**: the identifier of the bus or of the busbar section on side 2
        - **position_order_2**: in node/breaker, the position of the transformer on side 2
        - **direction_2**: optionally, in node/breaker, the direction, TOP or BOTTOM, of the transformer on side 2

    Examples:

        .. code-block:: python

            pp.network.create_2_windings_transformers_bays(
                            network, id='L', b=1e-6, g=1e-6, r=0.5, x=10, rated_u1=400, rated_u2=225,
                            bus_or_busbar_section_id_1='BBS1',
                            position_order_1=115,
                            direction_1='TOP',
                            bus_or_busbar_section_id_2='BBS2',
                            position_order_2=121,
                            direction_2='BOTTOM')

    See Also:
        :meth:`Network.create_2_windings_transformers`
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    metadata = _pp.get_network_modification_metadata_with_element_type(
        NetworkModificationType.CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER, ElementType.TWO_WINDINGS_TRANSFORMER)[0]
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_network_modification(network._handle, [c_df],
                                    NetworkModificationType.CREATE_TWO_WINDINGS_TRANSFORMER_FEEDER, raise_exception,
                                    None if report_node is None else report_node._report_node)  # pylint: disable=protected-access


def remove_feeder_bays(network: Network, connectable_ids: Union[str, List[str]], raise_exception: bool = True,
                       reporter: ReportNode = None, report_node: ReportNode = None) -> None:
    """
    Remove all feeders from a list as well as their bays: the connectables will be removed and all equipment connecting
    them to a bus or busbar (breaker, disconnector, ...).

    Args:
        network: the network to which we want to remove the feeder bay
        connectable_ids: either a list or a single string to indicate which equipment will be removed with their feeder bay.
        raise_exception: optionally, whether the calculation should raise exceptions. In any case, errors will
         be logged. Default is True.
        reporter: deprecated, use report_node instead
        report_node: optionally, the reporter to be used to create an execution report, default is None (no report).

    Examples:

    .. code-block:: python

        pp.network.remove_feeder_bays(network, connectable_ids=['load1', 'line3'])
    """
    if reporter is not None:
        warnings.warn(DEPRECATED_REPORTER_WARNING, DeprecationWarning)
        report_node = reporter

    if isinstance(connectable_ids, str):
        connectable_ids = [connectable_ids]
    _pp.remove_elements_modification(network._handle, connectable_ids, None, _pp.RemoveModificationType.REMOVE_FEEDER,
                                     raise_exception,
                                     None if report_node is None else report_node._report_node)  # pylint: disable=protected-access
