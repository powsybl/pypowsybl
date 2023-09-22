# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List
import math
import pandas as pd
import numpy as np
from numpy.typing import ArrayLike
from pypowsybl import _pypowsybl
from pypowsybl.utils import _adapt_df_or_kwargs
from .network import ElementType, Network


def _adapt_to_dataframe(element_type: ElementType, df: pd.DataFrame = None, **kwargs: ArrayLike) -> pd.DataFrame:
    metadata = _pypowsybl.get_network_elements_dataframe_metadata(element_type)
    return _adapt_df_or_kwargs(metadata, df, **kwargs)


class PerUnitView:  # pylint: disable=too-many-public-methods
    """
    A per-unit view of a network, providing getters and update methods for some of the
    network tables.
    """

    def __init__(self, network: Network, sn: float = 100):
        """
        Creates a per unit view of the provided network, using SN as base power.

        Args:
            network: the underlying network
            sn:      the base power, in MW
        """
        self._network = network
        self._sn = sn
        self.sqrt3 = math.sqrt(3)

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
        return self._sn

    def _get_nominal_v(self) -> pd.Series:
        """ A series of nominal voltages for all voltage levels
        """
        return self._network.get_voltage_levels()['nominal_v']

    def _get_indexed_nominal_v(self, df: pd.DataFrame, vl_attr: str = 'voltage_level_id') -> pd.Series:
        """ A series of nominal voltages indexed on the provided dataframe index.
        """
        return pd.merge(df, self._get_nominal_v(), left_on=vl_attr, right_index=True)['nominal_v']

    def _per_unit_p(self, df: pd.DataFrame, columns: List[str]) -> None:
        df[columns] /= self.sn

    def _per_unit_v(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        for col in columns:
            df[col] /= nominal_v

    def _per_unit_angle(self, df: pd.DataFrame, columns: List[str]) -> None:
        for col in columns:
            df[col] = np.deg2rad(df[col])

    def _per_unit_r(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            df[col] /= factor

    def _per_unit_r_not_same_nom_v(self, df: pd.DataFrame, columns: List[str], nominal_v1: pd.Series, nominal_v2: pd.Series) -> None:
        factor = nominal_v1 * nominal_v2 / self.sn
        for col in columns:
            df[col] /= factor

    def _per_unit_g(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            df[col] *= factor

    def _y(self, df: pd.DataFrame, r_col: str, x_col: str) -> pd.Series:
        return pd.Series(df.apply(lambda row: np.reciprocal(np.complex128(row[r_col] + row[x_col] * 1j)), axis=1))

    def _per_unit_g_not_same_nom_v(self, df: pd.DataFrame, column: str, ytr: pd.Series, nominal_v1: pd.Series, nominal_v2: pd.Series) -> None:
        df[column] = (df[column] * nominal_v1 * nominal_v1 + (nominal_v1 - nominal_v2) * nominal_v1 * ytr.apply(lambda row: row.real)) / self.sn

    def _per_unit_b_not_same_nom_v(self, df: pd.DataFrame, column: str, ytr: pd.Series, nominal_v1: pd.Series, nominal_v2: pd.Series) -> None:
        df[column] = (df[column] * nominal_v1 * nominal_v1 + (nominal_v1 - nominal_v2) * nominal_v1 * ytr.apply(lambda row: row.imag)) / self.sn

    def _per_unit_i(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        factor = self.sn * 10 ** 3 / (self.sqrt3 * nominal_v)
        for col in columns:
            df[col] /= factor

    def _un_per_unit_p(self, df: pd.DataFrame, columns: List[str]) -> None:
        for col in columns:
            if col in df.columns:
                df[col] *= self.sn

    def _un_per_unit_v(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        for col in columns:
            if col in df.columns:
                df[col] *= nominal_v

    def _un_per_unit_angle(self, df: pd.DataFrame, columns: List[str]) -> None:
        for col in columns:
            if col in df.columns:
                df[col] = np.rad2deg(df[col])

    def _un_per_unit_r(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            if col in df.columns:
                df[col] *= factor

    def _un_per_unit_g(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            if col in df.columns:
                df[col] /= factor

    def _un_per_unit_i(self, df: pd.DataFrame, columns: List[str], nominal_v: pd.Series) -> None:
        factor = self.sn * 10 ** 3 / (self.sqrt3 * nominal_v)
        for col in columns:
            if col in df.columns:
                df[col] *= factor

    def get_buses(self) -> pd.DataFrame:
        buses = self._network.get_buses()
        nominal_v = self._get_indexed_nominal_v(buses)
        self._per_unit_v(buses, ['v_mag'], nominal_v)
        self._per_unit_angle(buses, ['v_angle'])
        return buses

    def get_generators(self) -> pd.DataFrame:
        """
        A per-united dataframe of generators.

        Returns:
            a per-united dataframe of generators.
        """
        generators = self._network.get_generators()
        nominal_v = self._get_indexed_nominal_v(generators)
        self._per_unit_p(generators, ['target_p', 'target_q', 'min_p', 'min_q', 'max_p', 'max_q', 'p', 'q'])
        self._per_unit_v(generators, ['target_v'], nominal_v)
        self._per_unit_i(generators, ['i'], nominal_v)
        return generators

    def get_loads(self) -> pd.DataFrame:
        """
        A per-united dataframe of loads.

        Returns:
            a per-united dataframe of loads.
        """
        loads = self._network.get_loads()
        self._per_unit_p(loads, ['p0', 'q0', 'p', 'q'])
        nominal_v = pd.merge(loads, self._get_nominal_v(),
                             left_on='voltage_level_id', right_index=True)['nominal_v']
        self._per_unit_i(loads, ['i'], nominal_v)
        return loads

    def get_lines(self) -> pd.DataFrame:
        """
        A per-united dataframe of lines.

        Returns:
            a per-united dataframe of lines.
        """
        lines = self._network.get_lines()
        nominal_v1 = self._get_indexed_nominal_v(lines, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(lines, 'voltage_level2_id')
        self._per_unit_p(lines, ['p1', 'p2', 'q1', 'q2'])
        self._per_unit_i(lines, ['i1'], nominal_v1)
        self._per_unit_i(lines, ['i2'], nominal_v2)
        ytr = self._y(lines, 'r', 'x')
        self._per_unit_r_not_same_nom_v(lines, ['r', 'x'], nominal_v1, nominal_v2)
        self._per_unit_g_not_same_nom_v(lines, 'g1', ytr, nominal_v1, nominal_v2)
        self._per_unit_g_not_same_nom_v(lines, 'g2', ytr, nominal_v2, nominal_v1)
        self._per_unit_b_not_same_nom_v(lines, 'b1', ytr, nominal_v1, nominal_v2)
        self._per_unit_b_not_same_nom_v(lines, 'b2', ytr, nominal_v2, nominal_v1)
        return lines

    def get_2_windings_transformers(self) -> pd.DataFrame:
        """
        A per-united dataframe of 2 windings transformers.

        Returns:
            a per-united dataframe of 2 windings transformers.
        """
        two_windings_transformers = self._network.get_2_windings_transformers()
        self._per_unit_p(two_windings_transformers, ['p1', 'q1', 'p2', 'q2'])
        nominal_v1 = self._get_indexed_nominal_v(two_windings_transformers, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(two_windings_transformers, 'voltage_level2_id')
        self._per_unit_i(two_windings_transformers, ['i1'], nominal_v1)
        self._per_unit_i(two_windings_transformers, ['i2'], nominal_v2)
        self._per_unit_r(two_windings_transformers, ['r', 'x'], nominal_v2)
        self._per_unit_g(two_windings_transformers, ['g', 'b'], nominal_v2)
        two_windings_transformers['rated_u1'] /= nominal_v1
        two_windings_transformers['rated_u2'] /= nominal_v2
        return two_windings_transformers

    def get_3_windings_transformers(self) -> pd.DataFrame:
        """
        A per-united dataframe of 3 windings transformers.

        Returns:
            a per-united dataframe of 3 windings transformers.
        """
        three_windings_transformers = self._network.get_3_windings_transformers()
        self._per_unit_p(three_windings_transformers, ['p1', 'q1', 'p2', 'q2', 'p3', 'q3'])
        nominal_v1 = self._get_indexed_nominal_v(three_windings_transformers, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(three_windings_transformers, 'voltage_level2_id')
        nominal_v3 = self._get_indexed_nominal_v(three_windings_transformers, 'voltage_level3_id')
        nominal_v0 = three_windings_transformers['rated_u0']
        self._per_unit_i(three_windings_transformers, ['i1'], nominal_v1)
        self._per_unit_i(three_windings_transformers, ['i2'], nominal_v2)
        self._per_unit_i(three_windings_transformers, ['i3'], nominal_v3)
        self._per_unit_r(three_windings_transformers, ['r1', 'x1', 'r2', 'x2', 'r3', 'x3'], nominal_v0)
        self._per_unit_g(three_windings_transformers, ['g1', 'b1', 'g2', 'b2', 'g3', 'b3'], nominal_v0)
        three_windings_transformers['rated_u1'] /= nominal_v1
        three_windings_transformers['rated_u2'] /= nominal_v2
        three_windings_transformers['rated_u3'] /= nominal_v3
        three_windings_transformers['rated_u0'] = 1
        return three_windings_transformers

    def get_shunt_compensators(self) -> pd.DataFrame:
        """
        A per-united dataframe of shunt compensators.

        Returns:
            a per-united dataframe of shunt compensators.
        """
        shunt_compensators = self._network.get_shunt_compensators()
        nominal_v = self._get_indexed_nominal_v(shunt_compensators)
        self._per_unit_p(shunt_compensators, ['p', 'q'])
        self._per_unit_g(shunt_compensators, ['g', 'b'], nominal_v)
        self._per_unit_i(shunt_compensators, ['i'], nominal_v)
        return shunt_compensators

    def get_dangling_lines(self) -> pd.DataFrame:
        """
        A per-united dataframe of dangling lines.

        Returns:
            a per-united dataframe of dangling lines.
        """
        dangling_lines = self._network.get_dangling_lines()
        nominal_v = self._get_indexed_nominal_v(dangling_lines)
        self._per_unit_p(dangling_lines, ['p', 'q', 'q0', 'p0'])
        self._per_unit_i(dangling_lines, ['i'], nominal_v)
        self._per_unit_r(dangling_lines, ['r', 'x'], nominal_v)
        self._per_unit_g(dangling_lines, ['g', 'b'], nominal_v)
        return dangling_lines

    def get_lcc_converter_stations(self) -> pd.DataFrame:
        """
        A per-united dataframe of LCC converter stations.

        Returns:
            a per-united dataframe of LCC converter stations.
        """
        lcc_converter_stations = self._network.get_lcc_converter_stations()
        nominal_v = self._get_indexed_nominal_v(lcc_converter_stations)
        self._per_unit_p(lcc_converter_stations, ['p', 'q'])
        self._per_unit_i(lcc_converter_stations, ['i'], nominal_v)
        return lcc_converter_stations

    def get_vsc_converter_stations(self) -> pd.DataFrame:
        """
        A per-united dataframe of VSC converter stations.

        Returns:
            a per-united dataframe of VSC converter stations.
        """
        vsc_converter_stations = self._network.get_vsc_converter_stations()
        nominal_v = self._get_indexed_nominal_v(vsc_converter_stations)
        self._per_unit_p(vsc_converter_stations, ['p', 'q', 'target_q', 'max_q', 'min_q'])
        self._per_unit_i(vsc_converter_stations, ['i'], nominal_v)
        self._per_unit_v(vsc_converter_stations, ['target_v'], nominal_v)
        return vsc_converter_stations

    def get_static_var_compensators(self) -> pd.DataFrame:
        """
        A per-united dataframe of static var compensators.

        Returns:
            a per-united dataframe of static var compensators.
        """
        static_var_compensators = self._network.get_static_var_compensators()
        nominal_v = self._get_indexed_nominal_v(static_var_compensators)
        self._per_unit_p(static_var_compensators, ['p', 'q', 'target_q'])
        self._per_unit_i(static_var_compensators, ['i'], nominal_v)
        self._per_unit_v(static_var_compensators, ['target_v'], nominal_v)
        return static_var_compensators

    def get_voltage_levels(self) -> pd.DataFrame:
        """
        A per-united dataframe of voltage levels.

        Returns:
            a per-united dataframe of voltage levels.
        """
        voltage_levels = self._network.get_voltage_levels()
        self._per_unit_v(voltage_levels, ['low_voltage_limit', 'high_voltage_limit'], voltage_levels['nominal_v'])
        return voltage_levels

    def get_busbar_sections(self) -> pd.DataFrame:
        """
        A per-united dataframe of busbar sections.

        Returns:
            a per-united dataframe of busbar sections.
        """
        busbar_sections = self._network.get_busbar_sections()
        nominal_v = self._get_indexed_nominal_v(busbar_sections)
        self._per_unit_v(busbar_sections, ['v'], nominal_v)
        self._per_unit_angle(busbar_sections, ['angle'])
        return busbar_sections

    def get_hvdc_lines(self) -> pd.DataFrame:
        """
        A per-united dataframe of HVDC lines.

        Returns:
            a per-united dataframe of HVDC lines.
        """
        hvdc_lines = self._network.get_hvdc_lines()
        self._per_unit_p(hvdc_lines, ['max_p', 'target_p'])
        self._per_unit_r(hvdc_lines, ['r'], hvdc_lines['nominal_v'])
        return hvdc_lines

    def get_reactive_capability_curve_points(self) -> pd.DataFrame:
        """
        A per-united dataframe of reactive capability curves.

        Returns:
            A per-united dataframe of reactive capability curves.
        """
        reactive_capability_curve_points = self._network.get_reactive_capability_curve_points()
        self._per_unit_p(reactive_capability_curve_points, ['p', 'min_q', 'max_q'])
        return reactive_capability_curve_points

    def get_batteries(self) -> pd.DataFrame:
        """
        A per-united dataframe of batteries.

        Returns:
            A per-united dataframe of batteries.
        """
        batteries = self._network.get_batteries()
        nominal_v = self._get_indexed_nominal_v(batteries)
        self._per_unit_p(batteries, ['target_p', 'target_q', 'p', 'q', 'min_p', 'max_p', 'min_q', 'max_q'])
        self._per_unit_i(batteries, ['i'], nominal_v)
        return batteries

    def get_ratio_tap_changers(self) -> pd.DataFrame:
        """
        A per-united dataframe of ratio tap changers.

        Returns:
            A per-united dataframe of ratio tap changers.
        """
        ratio_tap_changers = self._network.get_ratio_tap_changers()
        voltage_levels = ratio_tap_changers[[]].merge(
            self.get_2_windings_transformers()[['voltage_level1_id', 'voltage_level2_id']],
            left_index=True, right_index=True)
        nominal_v1 = self._get_indexed_nominal_v(voltage_levels, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(voltage_levels, 'voltage_level2_id')
        ratio_tap_changers['rho'] *= nominal_v1 / nominal_v2
        self._per_unit_angle(ratio_tap_changers, ['alpha'])
        return ratio_tap_changers

    def update_buses(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update buses from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.BUS, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_buses())
        self._un_per_unit_v(to_update, ['v_mag'], nominal_v)
        self._un_per_unit_angle(to_update, ['v_angle'])
        self._network.update_buses(to_update)

    def update_generators(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update generators from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.GENERATOR, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_generators())
        self._un_per_unit_v(to_update, ['target_v'], nominal_v)
        self._un_per_unit_p(to_update, ['target_p', 'target_q', 'p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_generators(to_update)

    def update_loads(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update loads from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.LOAD, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_loads())
        self._un_per_unit_p(to_update, ['p0', 'q0', 'p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_loads(to_update)

    def update_batteries(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update batteries from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.BATTERY, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_batteries())
        self._un_per_unit_p(to_update, ['target_p', 'target_q', 'p', 'q', 'max_p', 'min_p', 'min_q', 'max_q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_batteries(to_update)

    def update_dangling_lines(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update dangling lines from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.DANGLING_LINE, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_dangling_lines())
        self._un_per_unit_p(to_update, ['p0', 'q0', 'p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._un_per_unit_r(to_update, ['r', 'x'], nominal_v)
        self._un_per_unit_g(to_update, ['g', 'b'], nominal_v)
        self._network.update_dangling_lines(to_update)

    def update_vsc_converter_stations(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update VSC converter stations from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.VSC_CONVERTER_STATION, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_vsc_converter_stations())
        self._un_per_unit_p(to_update, ['p', 'q', 'target_q', 'max_q', 'min_q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._un_per_unit_v(to_update, ['target_v'], nominal_v)
        self._network.update_vsc_converter_stations(to_update)

    def update_static_var_compensators(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update static var compensators from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.STATIC_VAR_COMPENSATOR, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_static_var_compensators())
        self._un_per_unit_p(to_update, ['p', 'q', 'target_q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._un_per_unit_v(to_update, ['target_v'], nominal_v)
        self._network.update_static_var_compensators(to_update)

    def update_hvdc_lines(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update HVDC lines from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.HVDC_LINE, df, **kwargs).copy()
        nominal_v = self._network.get_hvdc_lines()['nominal_v']
        self._un_per_unit_p(to_update, ['target_p'])
        self._un_per_unit_r(to_update, ['r'], nominal_v)
        self._network.update_hvdc_lines(to_update)

    def update_lines(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update lines from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.LINE, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_lines(), 'voltage_level1_id')
        self._un_per_unit_p(to_update, ['p1', 'p2', 'q1', 'q2'])
        self._un_per_unit_r(to_update, ['r', 'x'], nominal_v)
        self._un_per_unit_g(to_update, ['g1', 'g2', 'b1', 'b2'], nominal_v)
        self._network.update_lines(to_update)

    def update_2_windings_transformers(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update 2 windings transformers from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.TWO_WINDINGS_TRANSFORMER, df, **kwargs).copy()
        ref = self._network.get_2_windings_transformers()
        nominal_v1 = self._get_indexed_nominal_v(ref, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(ref, 'voltage_level2_id')
        self._un_per_unit_p(to_update, ['p1', 'p2', 'q1', 'q2'])
        self._un_per_unit_r(to_update, ['r', 'x'], nominal_v2)
        self._un_per_unit_g(to_update, ['g', 'b'], nominal_v2)
        self._un_per_unit_v(to_update, ['rated_u1'], nominal_v1)
        self._un_per_unit_v(to_update, ['rated_u2'], nominal_v2)
        self._network.update_2_windings_transformers(to_update)

    def update_3_windings_transformers(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update 3 windings transformers from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.THREE_WINDINGS_TRANSFORMER, df, **kwargs).copy()
        ref = self._network.get_3_windings_transformers()
        nominal_v1 = self._get_indexed_nominal_v(ref, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(ref, 'voltage_level2_id')
        nominal_v3 = self._get_indexed_nominal_v(ref, 'voltage_level3_id')
        nominal_v0 = ref['rated_u0']
        self._un_per_unit_p(to_update, ['p1', 'p2', 'p3', 'q1', 'q2', 'q3'])
        self._un_per_unit_r(to_update, ['r1', 'x1', 'r2', 'x2', 'r3', 'x3'], nominal_v0)
        self._un_per_unit_g(to_update, ['g1', 'b1', 'g2', 'b2', 'g3', 'b3'], nominal_v0)
        self._un_per_unit_v(to_update, ['rated_u1'], nominal_v1)
        self._un_per_unit_v(to_update, ['rated_u2'], nominal_v2)
        self._un_per_unit_v(to_update, ['rated_u3'], nominal_v3)
        self._network.update_3_windings_transformers(to_update)

    def update_lcc_converter_station(self, df: pd.DataFrame = None, **kwargs: ArrayLike) -> None:
        """
        Update LCC converter stations from per-united data.
        """
        to_update = _adapt_to_dataframe(ElementType.LCC_CONVERTER_STATION, df, **kwargs).copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_lcc_converter_stations())
        self._un_per_unit_p(to_update, ['p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_lcc_converter_stations(to_update)


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
