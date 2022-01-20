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


class PerUnitNetwork(_net.Network):

    def __init__(self, sn, handle):
        super().__init__(handle)
        self.sn = sn
        self.sqrt3 = _math.sqrt(3)

    def _get_nominal_v(self) -> _pd.Series:
        return super().get_voltage_levels()['nominal_v']

    def _get_indexed_nominal_v(self, df: _pd.DataFrame, vl_attr: str = 'voltage_level_id') -> _pd.Series:
        return _pd.merge(df, self._get_nominal_v(),
                         left_on=vl_attr, right_index=True)['nominal_v']

    def _per_unit_p(self, df: _pd.DataFrame, columns):
        df[columns] /= self.sn

    def _per_unit_v(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        for col in columns:
            df[col] /= nominal_v

    def _per_unit_r(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        for col in columns:
            df[col] /= nominal_v ** 2 / self.sn

    def _per_unit_g(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        for col in columns:
            df[col] *= nominal_v ** 2 / self.sn

    def _per_unit_i(self, df: _pd.DataFrame, columns, nominal_v: _pd.Series):
        for col in columns:
            df[col] /= self.sn * 10 ** 3 / (self.sqrt3 * nominal_v)

    def get_buses(self) -> _pd.DataFrame:
        buses = super().get_buses()
        nominal_v = self._get_indexed_nominal_v(buses)
        self._per_unit_v(buses, ['v_mag'], nominal_v)
        return buses

    def get_generators(self) -> _pd.DataFrame:
        """ Get generators as a ``Pandas`` data frame.

        Returns:
            a generators data frame
        """
        generators = super().get_generators()
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
        loads = super().get_loads()
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
        lines = super().get_lines()
        nominal_v = _pd.merge(lines, self._get_nominal_v(),
                              left_on='voltage_level2_id', right_index=True)['nominal_v']
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
        two_windings_transformers = super().get_2_windings_transformers()
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
        three_windings_transformers = super().get_3_windings_transformers()
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
        shunt_compensators = super().get_shunt_compensators()
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
        dangling_lines = super().get_dangling_lines()
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
        lcc_converter_stations = super().get_lcc_converter_stations()
        nominal_v = self._get_indexed_nominal_v(lcc_converter_stations)
        self._per_unit_p(lcc_converter_stations, ['p', 'q'])
        self._per_unit_i(lcc_converter_stations, ['i'], nominal_v)
        return lcc_converter_stations

    def get_vsc_converter_stations(self) -> _pd.DataFrame:
        """ Get VSC converter stations as a ``Pandas`` data frame.

        Returns:
            a VSC converter stations data frame
        """
        vsc_converter_stations = super().get_vsc_converter_stations()
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
        static_var_compensators = super().get_static_var_compensators()
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
        voltage_levels = super().get_voltage_levels()
        self._per_unit_v(voltage_levels, ['low_voltage_limit', 'high_voltage_limit'], voltage_levels['nominal_v'])
        return voltage_levels

    def get_busbar_sections(self) -> _pd.DataFrame:
        """ Get busbar sections as a ``Pandas`` data frame.

        Returns:
            a busbar sections data frame
        """
        busbar_sections = super().get_busbar_sections()
        nominal_v = self._get_indexed_nominal_v(busbar_sections)
        self._per_unit_v(busbar_sections, ['v'], nominal_v)
        return busbar_sections

    def get_hvdc_lines(self) -> _pd.DataFrame:
        """ Get HVDC lines as a ``Pandas`` data frame.

        Returns:
            a HVDC lines data frame
        """
        hvdc_lines = super().get_hvdc_lines()
        self._per_unit_p(hvdc_lines, ['max_p', 'active_power_setpoint'])
        self._per_unit_r(hvdc_lines, ['r'], hvdc_lines['nominal_v'])
        return hvdc_lines

    def get_reactive_capability_curve_points(self) -> _pd.DataFrame:
        """ Get reactive capability curve points as a ``Pandas`` data frame.

        Returns:
            a reactive capability curve points data frame
        """
        reactive_capability_curve_points = super().get_reactive_capability_curve_points()
        self._per_unit_p(reactive_capability_curve_points, ['p', 'min_q', 'max_q'])
        return reactive_capability_curve_points

    def get_batteries(self) -> _pd.DataFrame:
        batteries = super().get_batteries()
        nominal_v = self._get_indexed_nominal_v(batteries)
        self._per_unit_p(batteries, ['p0', 'q0', 'p', 'q', 'min_p', 'max_p'])
        self._per_unit_i(batteries, ['i'], nominal_v)
        return batteries

    def get_ratio_tap_changers(self) -> _pd.DataFrame:
        ratio_tap_changers = super().get_ratio_tap_changers()
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
        keys = df.columns
        to_update = df.copy()
        to_update = _pd.merge(to_update, self.get_buses()['voltage_level_id'],
                              left_index=True, right_index=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'], left_on='voltage_level_id',
                              right_index=True)
        if 'v_mag' in keys:
            to_update['v_mag'] *= to_update['nominal_v']
        to_update = to_update.drop(['nominal_v', 'voltage_level_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.BUS, to_update)

    def update_generators(self, df: _pd.DataFrame = None, **kwargs):
        """ Update generators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        to_update = _pd.merge(to_update, self.get_generators()['voltage_level_id'],
                              left_index=True, right_index=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'], left_on='voltage_level_id',
                              right_index=True)
        if 'target_v' in keys:
            to_update['target_v'] *= to_update['nominal_v']
        if 'target_p' in keys:
            to_update['target_p'] *= self.sn
        if 'target_q' in keys:
            to_update['target_q'] *= self.sn
        to_update = to_update.drop(['nominal_v', 'voltage_level_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.GENERATOR, to_update)

    def update_loads(self, df: _pd.DataFrame = None, **kwargs):
        """ Update loads with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'p0' in keys:
            to_update['p0'] *= self.sn
        if 'q0' in keys:
            to_update['q0'] *= self.sn
        self.update_elements(_pypowsybl.ElementType.LOAD, to_update)

    def update_batteries(self, df: _pd.DataFrame = None, **kwargs):
        """ Update batteries with a ``Pandas`` data frame.

        Available columns names:
        - p0
        - q0
        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'p0' in keys:
            to_update['p0'] *= self.sn
        if 'q0' in keys:
            to_update['q0'] *= self.sn
        if 'p' in keys:
            to_update['p'] *= self.sn
        if 'q' in keys:
            to_update['q'] *= self.sn
        if 'max_p' in keys:
            to_update['max_p'] *= self.sn
        if 'min_p' in keys:
            to_update['min_p'] *= self.sn
        self.update_elements(_pypowsybl.ElementType.BATTERY, to_update)

    def update_dangling_lines(self, df: _pd.DataFrame = None, **kwargs):
        """ Update dangling lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'p0' in keys:
            to_update['p0'] *= self.sn
        if 'q0' in keys:
            to_update['q0'] *= self.sn
        to_update = _pd.merge(to_update, self.get_dangling_lines()['voltage_level_id'],
                                  left_index=True, right_index=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level_id', right_index=True)
        constant = self.sn / to_update['nominal_v'] ** 2
        if 'r' in keys:
            to_update['r'] *= 1 / constant
        if 'x' in keys:
            to_update['x'] *= 1 / constant
        if 'g' in keys:
            to_update['g'] *= constant
        if 'b' in keys:
            to_update['b'] *= constant
        to_update = to_update.drop(['nominal_v', 'voltage_level_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.DANGLING_LINE, to_update)

    def update_vsc_converter_stations(self, df: _pd.DataFrame = None, **kwargs):
        """ Update VSC converter stations with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'reactive_power_setpoint' in keys:
            to_update['reactive_power_setpoint'] *= self.sn
        to_update = _pd.merge(to_update, self.get_vsc_converter_stations()['voltage_level_id'],
                              left_index=True, right_index=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'], left_on='voltage_level_id',
                              right_index=True)
        if 'voltage_setpoint' in keys:
            to_update['voltage_setpoint'] *= to_update['nominal_v']
        to_update = to_update.drop(['nominal_v', 'voltage_level_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION, to_update)

    def update_static_var_compensators(self, df: _pd.DataFrame = None, **kwargs):
        """ Update static var compensators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'reactive_power_setpoint' in keys:
            to_update['reactive_power_setpoint'] *= self.sn
        to_update = _pd.merge(to_update, self.get_static_var_compensators()['voltage_level_id'],
                              left_index=True, right_index=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'], left_on='voltage_level_id',
                              right_index=True)
        if 'voltage_setpoint' in keys:
            to_update['voltage_setpoint'] *= to_update['nominal_v']
        to_update = to_update.drop(['nominal_v', 'voltage_level_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR, to_update)

    def update_hvdc_lines(self, df: _pd.DataFrame = None, **kwargs):
        """ Update HVDC lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'active_power_setpoint' in keys:
            to_update['active_power_setpoint'] *= self.sn
        if 'r' in keys:
            to_update = _pd.merge(to_update, self.get_static_var_compensators()['voltage_level_id'],
                                  left_index=True, right_index=True)
            to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'], left_on='voltage_level_id',
                                  right_index=True)
            to_update['r'] /= self.sn / to_update['nominal_v'] ** 2
            to_update = to_update.drop(['nominal_v', 'voltage_level_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.HVDC_LINE, to_update)

    def update_lines(self, df: _pd.DataFrame = None, **kwargs):
        """ Update lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        to_update = _pd.merge(to_update, self.get_lines()['voltage_level1_id'],
                              left_index=True, right_index=True)
        if 'p1' in keys:
            to_update['p1'] *= self.sn
        if 'p2' in keys:
            to_update['p2'] *= self.sn
        if 'q1' in keys:
            to_update['q1'] *= self.sn
        if 'q2' in keys:
            to_update['q2'] *= self.sn
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level1_id', right_index=True)
        constant = self.sn / to_update['nominal_v'] ** 2
        if 'r' in keys:
            to_update['r'] *= 1 / constant
        if 'x' in keys:
            to_update['x'] *= 1 / constant
        if 'g1' in keys:
            to_update['g1'] *= constant
        if 'g2' in keys:
            to_update['g2'] *= constant
        if 'b1' in keys:
            to_update['b1'] *= constant
        if 'b2' in keys:
            to_update['b2'] *= constant
        to_update = to_update.drop(['nominal_v', 'voltage_level1_id'], axis=1)
        self.update_elements(_pypowsybl.ElementType.LINE, to_update)

    def update_2_windings_transformers(self, df: _pd.DataFrame = None, **kwargs):
        """Update 2 windings transformers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        to_update = _pd.merge(to_update, self.get_2_windings_transformers()[['voltage_level1_id', 'voltage_level2_id']],
                              left_index=True, right_index=True)
        if 'p1' in keys:
            to_update['p1'] *= self.sn
        if 'q1' in keys:
            to_update['q1'] *= self.sn
        if 'p2' in keys:
            to_update['p2'] *= self.sn
        if 'q2' in keys:
            to_update['q2'] *= self.sn
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level1_id', right_index=True)
        to_update.rename(columns={'nominal_v': 'nominal_v1'}, inplace=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level2_id', right_index=True)
        to_update.rename(columns={'nominal_v': 'nominal_v2'}, inplace=True)
        if 'r' in keys:
            to_update['r'] *= to_update['nominal_v2'] ** 2 / self.sn
        if 'x' in keys:
            to_update['x'] *= to_update['nominal_v2'] ** 2 / self.sn
        if 'g' in keys:
            to_update['g'] *= self.sn / to_update['nominal_v2'] ** 2
        if 'b' in keys:
            to_update['b'] *= self.sn / to_update['nominal_v2'] ** 2
        if 'rated_u1' in keys:
            to_update['rated_u1'] *= to_update['nominal_v1']
        if 'rated_u2' in keys:
            to_update['rated_u2'] *= to_update['nominal_v2']
        to_update = to_update.drop(['nominal_v1', 'nominal_v2', 'voltage_level1_id', 'voltage_level2_id'],
                                   axis=1)
        self.update_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER, to_update)

    def update_3_windings_transformers(self, df: _pd.DataFrame):
        """Update 3 windings transformers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        to_update = _pd.merge(to_update,
                              super().get_3_windings_transformers()[
                                  ['voltage_level1_id', 'voltage_level2_id', 'voltage_level3_id', 'rated_u0']],
                              left_index=True, right_index=True)
        if 'p1' in keys:
            to_update['p1'] *= self.sn
        if 'q1' in keys:
            to_update['q1'] *= self.sn
        if 'p2' in keys:
            to_update['p2'] *= self.sn
        if 'q2' in keys:
            to_update['q2'] *= self.sn
        if 'p3' in keys:
            to_update['p3'] *= self.sn
        if 'q3' in keys:
            to_update['q3'] *= self.sn
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level1_id', right_index=True)
        to_update.rename(columns={'nominal_v': 'nominal_v1'}, inplace=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level2_id', right_index=True)
        to_update.rename(columns={'nominal_v': 'nominal_v2'}, inplace=True)
        to_update = _pd.merge(to_update, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level3_id', right_index=True)
        to_update.rename(columns={'nominal_v': 'nominal_v3'}, inplace=True)
        if 'r1' in keys:
            to_update['r1'] *= to_update['rated_u0'] ** 2 / self.sn
        if 'x1' in keys:
            to_update['x1'] *= to_update['rated_u0'] ** 2 / self.sn
        if 'g1' in keys:
            to_update['g1'] *= self.sn / to_update['rated_u0'] ** 2
        if 'b1' in keys:
            to_update['b1'] *= self.sn / to_update['rated_u0'] ** 2
        if 'r2' in keys:
            to_update['r2'] *= to_update['rated_u0'] ** 2 / self.sn
        if 'x2' in keys:
            to_update['x2'] *= to_update['rated_u0'] ** 2 / self.sn
        if 'g2' in keys:
            to_update['g2'] *= self.sn / to_update['rated_u0'] ** 2
        if 'b2' in keys:
            to_update['b2'] *= self.sn / to_update['rated_u0'] ** 2
        if 'r3' in keys:
            to_update['r3'] *= to_update['rated_u0'] ** 2 / self.sn
        if 'x3' in keys:
            to_update['x3'] *= to_update['rated_u0'] ** 2 / self.sn
        if 'g3' in keys:
            to_update['g3'] *= self.sn / to_update['rated_u0'] ** 2
        if 'b3' in keys:
            to_update['b3'] *= self.sn / to_update['rated_u0'] ** 2
        if 'rated_u1' in keys:
            to_update['rated_u1'] *= to_update['nominal_v1']
        if 'rated_u2' in keys:
            to_update['rated_u2'] *= to_update['nominal_v2']
        if 'rated_u3' in keys:
            to_update['rated_u3'] *= to_update['nominal_v3']
        to_update = to_update.drop(['nominal_v1', 'nominal_v2', 'nominal_v3',
                                    'voltage_level1_id', 'voltage_level2_id', 'voltage_level3_id', 'rated_u0'],
                                   axis=1)
        self.update_elements(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER, to_update)

    def update_lcc_converter_station(self, df: _pd.DataFrame):
        """Update lcc converter stations with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        keys = df.columns
        to_update = df.copy()
        if 'p' in keys:
            to_update['p'] *= self.sn
        if 'q' in keys:
            to_update['q'] *= self.sn
        self.update_elements(_pypowsybl.ElementType.LCC_CONVERTER_STATION, to_update)
