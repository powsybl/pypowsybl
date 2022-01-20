#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import math as _math

from pypowsybl import _pypowsybl

import pypowsybl.network as _net
import pandas as _pd


class PerUnitView:
    """
    A per-unit view of a network, providing getters and update methods.
    """

    def __init__(self, network: _net.Network, sn: float = 100):
        """
        Creates a per unit view of the provided network, using SN as base power.

        Args:
            network: the underlying network
            sn:      the base power, in MW
        """
        self._network = network
        self.sn = sn
        self.sqrt3 = _math.sqrt(3)

    def _get_nominal_v(self) -> _pd.Series:
        return self._network.get_voltage_levels()['nominal_v']

    def _get_indexed_nominal_v(self, df: _pd.DataFrame, vl_attr: str = 'voltage_level_id') -> _pd.Series:
        return _pd.merge(df, self._get_nominal_v(),
                         left_on=vl_attr, right_index=True)['nominal_v']

    def _per_unit_p(self, df: _pd.DataFrame, columns):
        df[columns] /= self.sn

    def _per_unit_v(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        for col in columns:
            df[col] /= nominal_v

    def _per_unit_r(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            df[col] /= factor

    def _per_unit_g(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            df[col] *= factor

    def _per_unit_i(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        factor = self.sn * 10 ** 3 / (self.sqrt3 * nominal_v)
        for col in columns:
            df[col] /= factor

    def _un_per_unit_p(self, df: _pd.DataFrame, columns):
        for col in columns:
            if col in df.columns:
                df[col] *= self.sn

    def _un_per_unit_v(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        for col in columns:
            if col in df.columns:
                df[col] *= nominal_v

    def _un_per_unit_r(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            if col in df.columns:
                df[col] *= factor

    def _un_per_unit_g(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        factor = nominal_v ** 2 / self.sn
        for col in columns:
            if col in df.columns:
                df[col] /= factor

    def _un_per_unit_i(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        factor = self.sn * 10 ** 3 / (self.sqrt3 * nominal_v)
        for col in columns:
            if col in df.columns:
                df[col] *= factor

    def get_buses(self) -> _pd.DataFrame:
        buses = self._network.get_buses()
        nominal_v = self._get_indexed_nominal_v(buses)
        self._per_unit_v(buses, ['v_mag'], nominal_v)
        return buses

    def get_generators(self) -> _pd.DataFrame:
        """ Get generators as a ``Pandas`` data frame.

        Returns:
            a generators data frame
        """
        generators = self._network.get_generators()
        nominal_v = self._get_indexed_nominal_v(generators)
        self._per_unit_p(generators, ['target_p', 'target_q', 'min_p', 'min_q', 'max_p', 'max_q', 'p', 'q'])
        self._per_unit_v(generators, ['target_v'], nominal_v)
        self._per_unit_i(generators, ['i'], nominal_v)
        return generators

    def get_loads(self) -> _pd.DataFrame:
        """ Get loads as a ``Pandas`` data frame.

        Returns:
            a loads data frame
        """
        loads = self._network.get_loads()
        self._per_unit_p(loads, ['p0', 'q0', 'p', 'q'])
        nominal_v = _pd.merge(loads, self._get_nominal_v(),
                              left_on='voltage_level_id', right_index=True)['nominal_v']
        self._per_unit_i(loads, 'i', nominal_v)
        return loads

    def get_lines(self) -> _pd.DataFrame:
        """ Get lines as a ``Pandas`` data frame.

        Returns:
            a lines data frame
        """
        lines = self._network.get_lines()
        nominal_v = self._get_indexed_nominal_v(lines, 'voltage_level2_id')
        self._per_unit_p(lines, ['p1', 'p2', 'q1', 'q2'])
        self._per_unit_i(lines, ['i1', 'i2'], nominal_v)
        self._per_unit_r(lines, ['r', 'x'], nominal_v)
        self._per_unit_g(lines, ['g1', 'g2', 'b1', 'b2'], nominal_v)
        return lines

    def get_2_windings_transformers(self) -> _pd.DataFrame:
        """ Get 2 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 2 windings transformers data frame
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

    def get_3_windings_transformers(self) -> _pd.DataFrame:
        """ Get 3 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 3 windings transformers data frame
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

    def get_shunt_compensators(self) -> _pd.DataFrame:
        """ Get shunt compensators as a ``Pandas`` data frame.

        Returns:
            a shunt compensators data frame
        """
        shunt_compensators = self._network.get_shunt_compensators()
        nominal_v = self._get_indexed_nominal_v(shunt_compensators)
        self._per_unit_p(shunt_compensators, ['p', 'q'])
        self._per_unit_g(shunt_compensators, ['g', 'b'], nominal_v)
        self._per_unit_i(shunt_compensators, ['i'], nominal_v)
        return shunt_compensators

    def get_dangling_lines(self) -> _pd.DataFrame:
        """ Get dangling lines as a ``Pandas`` data frame.

        Returns:
            a dangling lines data frame
        """
        dangling_lines = self._network.get_dangling_lines()
        nominal_v = self._get_indexed_nominal_v(dangling_lines)
        self._per_unit_p(dangling_lines, ['p', 'q', 'q0', 'p0'])
        self._per_unit_i(dangling_lines, ['i'], nominal_v)
        self._per_unit_r(dangling_lines, ['r', 'x'], nominal_v)
        self._per_unit_g(dangling_lines, ['g', 'b'], nominal_v)
        return dangling_lines

    def get_lcc_converter_stations(self) -> _pd.DataFrame:
        """ Get LCC converter stations as a ``Pandas`` data frame.

        Returns:
            a LCC converter stations data frame
        """
        lcc_converter_stations = self._network.get_lcc_converter_stations()
        nominal_v = self._get_indexed_nominal_v(lcc_converter_stations)
        self._per_unit_p(lcc_converter_stations, ['p', 'q'])
        self._per_unit_i(lcc_converter_stations, ['i'], nominal_v)
        return lcc_converter_stations

    def get_vsc_converter_stations(self) -> _pd.DataFrame:
        """ Get VSC converter stations as a ``Pandas`` data frame.

        Returns:
            a VSC converter stations data frame
        """
        vsc_converter_stations = self._network.get_vsc_converter_stations()
        nominal_v = self._get_indexed_nominal_v(vsc_converter_stations)
        self._per_unit_p(vsc_converter_stations, ['p', 'q', 'reactive_power_setpoint'])
        self._per_unit_i(vsc_converter_stations, ['i'], nominal_v)
        self._per_unit_v(vsc_converter_stations, ['voltage_setpoint'], nominal_v)
        return vsc_converter_stations

    def get_static_var_compensators(self) -> _pd.DataFrame:
        """ Get static var compensators as a ``Pandas`` data frame.

        Returns:
            a static var compensators data frame
        """
        static_var_compensators = self._network.get_static_var_compensators()
        nominal_v = self._get_indexed_nominal_v(static_var_compensators)
        self._per_unit_p(static_var_compensators, ['p', 'q', 'reactive_power_setpoint'])
        self._per_unit_i(static_var_compensators, ['i'], nominal_v)
        self._per_unit_v(static_var_compensators, ['voltage_setpoint'], nominal_v)
        return static_var_compensators

    def get_voltage_levels(self) -> _pd.DataFrame:
        """ Get voltage levels as a ``Pandas`` data frame.

        Returns:
            a voltage levels data frame
        """
        voltage_levels = self._network.get_voltage_levels()
        self._per_unit_v(voltage_levels, ['low_voltage_limit', 'high_voltage_limit'], voltage_levels['nominal_v'])
        return voltage_levels

    def get_busbar_sections(self) -> _pd.DataFrame:
        """ Get busbar sections as a ``Pandas`` data frame.

        Returns:
            a busbar sections data frame
        """
        busbar_sections = self._network.get_busbar_sections()
        nominal_v = self._get_indexed_nominal_v(busbar_sections)
        self._per_unit_v(busbar_sections, ['v'], nominal_v)
        return busbar_sections

    def get_hvdc_lines(self) -> _pd.DataFrame:
        """ Get HVDC lines as a ``Pandas`` data frame.

        Returns:
            a HVDC lines data frame
        """
        hvdc_lines = self._network.get_hvdc_lines()
        self._per_unit_p(hvdc_lines, ['max_p', 'active_power_setpoint'])
        self._per_unit_r(hvdc_lines, ['r'], hvdc_lines['nominal_v'])
        return hvdc_lines

    def get_reactive_capability_curve_points(self) -> _pd.DataFrame:
        """ Get reactive capability curve points as a ``Pandas`` data frame.

        Returns:
            a reactive capability curve points data frame
        """
        reactive_capability_curve_points = self._network.get_reactive_capability_curve_points()
        self._per_unit_p(reactive_capability_curve_points, ['p', 'min_q', 'max_q'])
        return reactive_capability_curve_points

    def get_batteries(self) -> _pd.DataFrame:
        batteries = self._network.get_batteries()
        nominal_v = self._get_indexed_nominal_v(batteries)
        self._per_unit_p(batteries, ['p0', 'q0', 'p', 'q', 'min_p', 'max_p'])
        self._per_unit_i(batteries, ['i'], nominal_v)
        return batteries

    def get_ratio_tap_changers(self) -> _pd.DataFrame:
        ratio_tap_changers = self._network.get_ratio_tap_changers()
        voltage_levels = ratio_tap_changers[[]].merge(
            self.get_2_windings_transformers()[['voltage_level1_id', 'voltage_level2_id']],
            left_index=True, right_index=True)
        nominal_v1 = self._get_indexed_nominal_v(voltage_levels, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(voltage_levels, 'voltage_level2_id')
        ratio_tap_changers['rho'] *= nominal_v1 / nominal_v2
        return ratio_tap_changers

    def update_buses(self, df: _pd.DataFrame = None, **kwargs):
        """ Update buses with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_buses())
        self._un_per_unit_v(to_update, ['v_mag'], nominal_v)
        self._network.update_buses(to_update)

    def update_generators(self, df: _pd.DataFrame = None, **kwargs):
        """ Update generators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_generators())
        self._un_per_unit_v(to_update, ['target_v'], nominal_v)
        self._un_per_unit_p(to_update, ['target_p', 'target_q', 'p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_generators(to_update)

    def update_loads(self, df: _pd.DataFrame = None, **kwargs):
        """ Update loads with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_loads())
        self._un_per_unit_p(to_update, ['p0', 'q0', 'p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_loads(to_update)

    def update_batteries(self, df: _pd.DataFrame = None, **kwargs):
        """ Update batteries with a ``Pandas`` data frame.

        Available columns names:
        - p0
        - q0
        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_batteries())
        self._un_per_unit_p(to_update, ['p0', 'q0', 'p', 'q', 'max_p', 'min_p'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_batteries(to_update)

    def update_dangling_lines(self, df: _pd.DataFrame = None, **kwargs):
        """ Update dangling lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_dangling_lines())
        self._un_per_unit_p(to_update, ['p0', 'q0', 'p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._un_per_unit_r(to_update, ['r', 'x'], nominal_v)
        self._un_per_unit_g(to_update, ['g', 'b'], nominal_v)
        self._network.update_dangling_lines(to_update)

    def update_vsc_converter_stations(self, df: _pd.DataFrame = None, **kwargs):
        """ Update VSC converter stations with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_vsc_converter_stations())
        self._un_per_unit_p(to_update, ['p', 'q', 'reactive_power_setpoint'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._un_per_unit_v(to_update, ['voltage_setpoint'], nominal_v)
        self._network.update_vsc_converter_stations(to_update)

    def update_static_var_compensators(self, df: _pd.DataFrame = None, **kwargs):
        """ Update static var compensators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_static_var_compensators())
        self._un_per_unit_p(to_update, ['p', 'q', 'reactive_power_setpoint'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._un_per_unit_v(to_update, ['voltage_setpoint'], nominal_v)
        self._network.update_static_var_compensators(to_update)

    def update_hvdc_lines(self, df: _pd.DataFrame = None, **kwargs):
        """ Update HVDC lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._network.get_hvdc_lines()['nominal_v']
        self._un_per_unit_p(to_update, ['active_power_setpoint'])
        self._un_per_unit_r(to_update, ['r'], nominal_v)
        self._network.update_hvdc_lines(to_update)

    def update_lines(self, df: _pd.DataFrame = None, **kwargs):
        """ Update lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_lines(), 'voltage_level1_id')
        self._un_per_unit_p(to_update, ['p1', 'p2', 'q1', 'q2'])
        self._un_per_unit_r(to_update, ['r', 'x'], nominal_v)
        self._un_per_unit_g(to_update, ['g1', 'g2', 'b1', 'b2'], nominal_v)
        self._network.update_lines(to_update)

    def update_2_windings_transformers(self, df: _pd.DataFrame = None, **kwargs):
        """Update 2 windings transformers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        ref = self._network.get_2_windings_transformers()
        nominal_v1 = self._get_indexed_nominal_v(ref, 'voltage_level1_id')
        nominal_v2 = self._get_indexed_nominal_v(ref, 'voltage_level2_id')
        self._un_per_unit_p(to_update, ['p1', 'p2', 'q1', 'q2'])
        self._un_per_unit_r(to_update, ['r', 'x'], nominal_v2)
        self._un_per_unit_g(to_update, ['g', 'b'], nominal_v2)
        self._un_per_unit_v(to_update, ['rated_u1'], nominal_v1)
        self._un_per_unit_v(to_update, ['rated_u2'], nominal_v2)
        self._network.update_2_windings_transformers(to_update)

    def update_3_windings_transformers(self, df: _pd.DataFrame):
        """Update 3 windings transformers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
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
        self._network.update_elements(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER, to_update)

    def update_lcc_converter_station(self, df: _pd.DataFrame):
        """Update lcc converter stations with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        to_update = df.copy()
        nominal_v = self._get_indexed_nominal_v(self._network.get_lcc_converter_stations())
        self._un_per_unit_p(to_update, ['p', 'q'])
        self._un_per_unit_i(to_update, ['i'], nominal_v)
        self._network.update_elements(_pypowsybl.ElementType.LCC_CONVERTER_STATION, to_update)
