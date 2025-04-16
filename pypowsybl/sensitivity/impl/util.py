# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from datetime import datetime
from enum import Enum
from typing import List

from pypowsybl._pypowsybl import PyPowsyblError

from pypowsybl import glsk
from pypowsybl.network import Network
from pypowsybl import _pypowsybl
from .zone import Zone
from .dc_sensitivity_analysis import DcSensitivityAnalysis
from .ac_sensitivity_analysis import AcSensitivityAnalysis


class ZoneKeyType(Enum):
    GENERATOR_TARGET_P = 0
    GENERATOR_MAX_P = 1
    LOAD_P0 = 2


def create_empty_zone(id: str) -> Zone:
    return Zone(id)


def create_country_zone(network: Network, country: str,
                        key_type: ZoneKeyType = ZoneKeyType.GENERATOR_TARGET_P) -> Zone:
    substations = network.get_substations()
    voltage_levels = network.get_voltage_levels()
    if key_type in (ZoneKeyType.GENERATOR_MAX_P, ZoneKeyType.GENERATOR_TARGET_P):
        # join generators, voltage levels and substations to get generators with countries
        generators = network.get_generators()
        generators_with_countries = generators.join(
            voltage_levels[['substation_id']].join(substations[['country']], on=['substation_id']),
            on=['voltage_level_id'])

        # filter generators for specified country
        filtered_generators = generators_with_countries[generators_with_countries['country'] == country]
        shift_keys = filtered_generators.target_p if key_type == ZoneKeyType.GENERATOR_TARGET_P else filtered_generators.max_p
        shift_keys_by_id = dict(zip(filtered_generators.index, shift_keys))
    elif key_type == ZoneKeyType.LOAD_P0:
        # join loads, voltage levels and substations to get generators with countries
        loads = network.get_loads()
        loads_with_countries = loads.join(
            voltage_levels[['substation_id']].join(substations[['country']], on=['substation_id']),
            on=['voltage_level_id'])

        # filter loads for specified country
        filtered_loads = loads_with_countries[loads_with_countries['country'] == country]
        shift_keys_by_id = dict(zip(filtered_loads.index, filtered_loads.p0))
    else:
        raise PyPowsyblError(f'Unknown key type {key_type}')

    return Zone(country, shift_keys_by_id)


def create_zone_from_injections_and_shift_keys(id: str, injection_index: List[str], shift_keys: List[float]) -> Zone:
    """ Create country zone with custom generator name and shift keys
        Args:
            country: Identifier of the zone
            injection_index: IDs of the injection
            shift_keys: shift keys for the generators
        Returns:
            The zone object
    """
    shift_keys_by_id = dict(zip(injection_index, shift_keys))
    return Zone(id, shift_keys_by_id)


def create_zones_from_glsk_file(network: Network, glsk_file: str, instant: datetime) -> List[Zone]:
    """ Create country zones from glsk file for a given datetime
        Args:
            glsk_file: UCTE glsk file
            instant: timepoint at which to select glsk data
        Returns:
            A list of zones created from glsk file
    """
    glsk_document = glsk.load(glsk_file)
    countries = glsk_document.get_countries()
    zones = []
    for country in countries:
        c_generators = glsk_document.get_points_for_country(network, country, instant)
        c_shift_keys = glsk_document.get_glsk_factors(network, country, instant)
        zone = create_zone_from_injections_and_shift_keys(country, c_generators, c_shift_keys)
        zones.append(zone)
    return zones


def create_dc_analysis() -> DcSensitivityAnalysis:
    """
    Creates a new DC sensitivity analysis.

    Returns:
        a new DC sensitivity analysis
    """
    return DcSensitivityAnalysis(_pypowsybl.create_sensitivity_analysis())


def create_ac_analysis() -> AcSensitivityAnalysis:
    """
    Creates a new AC sensitivity analysis.

    Returns:
        a new AC sensitivity analysis
    """
    return AcSensitivityAnalysis(_pypowsybl.create_sensitivity_analysis())


def set_default_provider(provider: str) -> None:
    """
    Set the default sensitivity analysis provider

    Args:
        provider: name of the default sensitivity analysis provider to set
    """
    _pypowsybl.set_default_sensitivity_analysis_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default sensitivity analysis provider.

    Returns:
        the name of the current default sensitivity analysis provider
    """
    return _pypowsybl.get_default_sensitivity_analysis_provider()


def get_provider_names() -> List[str]:
    """
    Get list of supported provider names

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_sensitivity_analysis_provider_names()


def get_provider_parameters_names(provider: str = '') -> List[str]:
    """
    Get list of parameters for the specified sensitivity analysis provider.

    If not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_sensitivity_analysis_provider_parameters_names(provider)
