# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pickle
import warnings

import pandas as pd
from numpy.typing import ArrayLike

from .network import Network


class PerUnitView:  # pylint: disable=too-many-public-methods
    """
    A per-unit view of a network, providing getters and update methods for some of the
    network tables.

    .. deprecated:: 1.8.0
      Use per unit mode of network directly instead.

    """

    def __init__(self, network: Network, sn: float = 100):
        """
        Creates a per unit view of the provided network, using SN as base power.

        Args:
            network: the underlying network
            sn:      the base power, in MW
        """
        warnings.warn("Per-unit view is deprecated and slow (make a deep copy of the network), use per unit mode of the network instead", DeprecationWarning)
        self._network = pickle.loads(pickle.dumps(network))
        self._network.per_unit = True
        self._network.nominal_apparent_power = sn

    @property
    def network(self) -> Network:
        """
        The underlying network
        """
        return self._network

    @property
    def sn(self) -> float:
        """
        The base power, in MW, used for per-uniting
        """
        return self._network.nominal_apparent_power

    def get_buses(self) -> pd.DataFrame:
        return self._network.get_buses()

    def get_generators(self) -> pd.DataFrame:
        """
        A per-united dataframe of generators.

        Returns:
            a per-united dataframe of generators.
        """
        return self._network.get_generators()

    def get_loads(self) -> pd.DataFrame:
        """
        A per-united dataframe of loads.

        Returns:
            a per-united dataframe of loads.
        """
        return self._network.get_loads()

    def get_lines(self) -> pd.DataFrame:
        """
        A per-united dataframe of lines.

        Returns:
            a per-united dataframe of lines.
        """
        return self._network.get_lines()

    def get_2_windings_transformers(self) -> pd.DataFrame:
        """
        A per-united dataframe of 2 windings transformers.

        Returns:
            a per-united dataframe of 2 windings transformers.
        """
        return self._network.get_2_windings_transformers()

    def get_3_windings_transformers(self) -> pd.DataFrame:
        """
        A per-united dataframe of 3 windings transformers.

        Returns:
            a per-united dataframe of 3 windings transformers.
        """
        return self._network.get_3_windings_transformers()

    def get_shunt_compensators(self) -> pd.DataFrame:
        """
        A per-united dataframe of shunt compensators.

        Returns:
            a per-united dataframe of shunt compensators.
        """
        return self._network.get_shunt_compensators()

    def get_dangling_lines(self) -> pd.DataFrame:
        """
        A per-united dataframe of dangling lines.

        Returns:
            a per-united dataframe of dangling lines.
        """
        return self._network.get_dangling_lines()

    def get_lcc_converter_stations(self) -> pd.DataFrame:
        """
        A per-united dataframe of LCC converter stations.

        Returns:
            a per-united dataframe of LCC converter stations.
        """
        return self._network.get_lcc_converter_stations()

    def get_vsc_converter_stations(self) -> pd.DataFrame:
        """
        A per-united dataframe of VSC converter stations.

        Returns:
            a per-united dataframe of VSC converter stations.
        """
        return self._network.get_vsc_converter_stations()

    def get_static_var_compensators(self) -> pd.DataFrame:
        """
        A per-united dataframe of static var compensators.

        Returns:
            a per-united dataframe of static var compensators.
        """
        return self._network.get_static_var_compensators()

    def get_voltage_levels(self) -> pd.DataFrame:
        """
        A per-united dataframe of voltage levels.

        Returns:
            a per-united dataframe of voltage levels.
        """
        return self._network.get_voltage_levels()

    def get_busbar_sections(self) -> pd.DataFrame:
        """
        A per-united dataframe of busbar sections.

        Returns:
            a per-united dataframe of busbar sections.
        """
        return self._network.get_busbar_sections()

    def get_hvdc_lines(self) -> pd.DataFrame:
        """
        A per-united dataframe of HVDC lines.

        Returns:
            a per-united dataframe of HVDC lines.
        """
        return self._network.get_hvdc_lines()

    def get_reactive_capability_curve_points(self) -> pd.DataFrame:
        """
        A per-united dataframe of reactive capability curves.

        Returns:
            A per-united dataframe of reactive capability curves.
        """
        return self._network.get_reactive_capability_curve_points()

    def get_batteries(self) -> pd.DataFrame:
        """
        A per-united dataframe of batteries.

        Returns:
            A per-united dataframe of batteries.
        """
        return self._network.get_batteries()

    def get_ratio_tap_changers(self) -> pd.DataFrame:
        """
        A per-united dataframe of ratio tap changers.

        Returns:
            A per-united dataframe of ratio tap changers.
        """
        return self._network.get_ratio_tap_changers()

    def update_buses(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update buses from per-united data.
        """
        self._network.update_buses(df, **kwargs)

    def update_generators(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update generators from per-united data.
        """
        self._network.update_generators(df, **kwargs)

    def update_loads(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update loads from per-united data.
        """
        self._network.update_loads(df, **kwargs)

    def update_batteries(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update batteries from per-united data.
        """
        self._network.update_batteries(df, **kwargs)

    def update_dangling_lines(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update dangling lines from per-united data.
        """
        self._network.update_dangling_lines(df, **kwargs)

    def update_vsc_converter_stations(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update VSC converter stations from per-united data.
        """
        self._network.update_vsc_converter_stations(df, **kwargs)

    def update_static_var_compensators(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update static var compensators from per-united data.
        """
        self._network.update_static_var_compensators(df, **kwargs)

    def update_hvdc_lines(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update HVDC lines from per-united data.
        """
        self._network.update_hvdc_lines(df, **kwargs)

    def update_lines(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update lines from per-united data.
        """
        self._network.update_lines(df, **kwargs)

    def update_2_windings_transformers(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update 2 windings transformers from per-united data.
        """
        self._network.update_2_windings_transformers(df, **kwargs)

    def update_3_windings_transformers(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update 3 windings transformers from per-united data.
        """
        self._network.update_3_windings_transformers(df, **kwargs)

    def update_lcc_converter_station(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update LCC converter stations from per-united data.
        """
        self._network.update_lcc_converter_stations(df, **kwargs)


def per_unit_view(network: Network, sn: float = 100) -> PerUnitView:
    """
    Creates a per unit view of the provided network, using SN as base power.

    Args:
        network: the underlying network
        sn:      the base power, in MW

    Returns:
        a per-unit view of the network
    """
    return PerUnitView(network=network, sn=sn)
