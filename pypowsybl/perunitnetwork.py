#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import math as _math

import _pypowsybl

import pypowsybl.network as _net
import pandas as _pd


class PerUnitNetwork(_net.Network):
    def __init__(self, sn, handle):
        super().__init__(handle)
        self.sn = sn
        self.sqrt3 = _math.sqrt(3)

    def get_buses(self) -> _pd.DataFrame:
        join = _pd.merge(self.get_elements(_pypowsybl.ElementType.BUS), self.get_voltage_levels()['nominal_v'],
                         left_on='voltage_level_id', right_index=True)
        join['v_mag'] = join['v_mag'] / join['nominal_v']
        return join.drop('nominal_v', axis=1)

    def get_generators(self) -> _pd.DataFrame:
        """ Get generators as a ``Pandas`` data frame.

        Returns:
            a generators data frame
        """
        generators = self.get_elements(_pypowsybl.ElementType.GENERATOR)
        generators['target_p'] /= self.sn
        generators['target_q'] /= self.sn
        generators['min_p'] /= self.sn
        generators['min_q'] /= self.sn
        generators['max_p'] /= self.sn
        generators['max_q'] /= self.sn
        generators['p'] /= self.sn
        generators['q'] /= self.sn
        join = _pd.merge(generators, self.get_voltage_levels()['nominal_v'],
                         left_on='voltage_level_id', right_index=True)
        join['target_v'] = join['target_v'] / join['nominal_v']
        join['i'] /= self.sn * 10 ** 3 / (self.sqrt3 * join['nominal_v'])
        return join.drop('nominal_v', axis=1)

    def get_loads(self) -> _pd.DataFrame:
        """ Get loads as a ``Pandas`` data frame.

        Returns:
            a loads data frame
        """
        loads = self.get_elements(_pypowsybl.ElementType.LOAD)
        loads['p0'] /= self.sn
        loads['q0'] /= self.sn
        loads['p'] /= self.sn
        loads['q'] /= self.sn
        join = _pd.merge(loads, self.get_voltage_levels()['nominal_v'],
                         left_on='voltage_level_id', right_index=True)
        join['i'] /= self.sn * 10 ** 3 / (self.sqrt3 * join['nominal_v'])
        return join.drop('nominal_v', axis=1)

    def get_lines(self) -> _pd.DataFrame:
        """ Get lines as a ``Pandas`` data frame.

        Returns:
            a lines data frame
        """
        lines = self.get_elements(_pypowsybl.ElementType.LINE)
        lines['p1'] /= self.sn
        lines['p2'] /= self.sn
        lines['q1'] /= self.sn
        lines['q2'] /= self.sn
        lines = _pd.merge(lines, self.get_voltage_levels()['nominal_v'],
                          left_on='voltage_level2_id', right_index=True)
        lines['i1'] /= self.sn * 10 ** 3 / (self.sqrt3 * lines['nominal_v'])
        lines['i2'] /= self.sn * 10 ** 3 / (self.sqrt3 * lines['nominal_v'])
        lines['r'] /= lines['nominal_v'] ** 2 / self.sn
        lines['x'] /= lines['nominal_v'] ** 2 / self.sn
        lines['g1'] /= self.sn / lines['nominal_v'] ** 2
        lines['g2'] /= self.sn / lines['nominal_v'] ** 2
        lines['b1'] /= self.sn / lines['nominal_v'] ** 2
        lines['b2'] /= self.sn / lines['nominal_v'] ** 2
        return lines.drop('nominal_v', axis=1)

    def get_2_windings_transformers(self) -> _pd.DataFrame:
        """ Get 2 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 2 windings transformers data frame
        """
        two_windings_transformers = self.get_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER)
        two_windings_transformers['p1'] /= self.sn
        two_windings_transformers['q1'] /= self.sn
        two_windings_transformers['p2'] /= self.sn
        two_windings_transformers['q2'] /= self.sn
        two_windings_transformers = _pd.merge(two_windings_transformers, self.get_voltage_levels()['nominal_v'],
                                              left_on='voltage_level1_id', right_index=True)
        two_windings_transformers.rename(columns={'nominal_v': 'nominal_v1'}, inplace=True)
        two_windings_transformers = _pd.merge(two_windings_transformers, self.get_voltage_levels()['nominal_v'],
                                              left_on='voltage_level2_id', right_index=True)
        two_windings_transformers.rename(columns={'nominal_v': 'nominal_v2'}, inplace=True)
        two_windings_transformers['i1'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * two_windings_transformers['nominal_v1'])
        two_windings_transformers['i2'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * two_windings_transformers['nominal_v2'])
        two_windings_transformers['r'] /= two_windings_transformers['nominal_v2'] ** 2 / self.sn
        two_windings_transformers['x'] /= two_windings_transformers['nominal_v2'] ** 2 / self.sn
        two_windings_transformers['g'] /= self.sn / two_windings_transformers['nominal_v2'] ** 2
        two_windings_transformers['b'] /= self.sn / two_windings_transformers['nominal_v2'] ** 2
        two_windings_transformers['rated_u1'] /= two_windings_transformers['nominal_v1']
        two_windings_transformers['rated_u2'] /= two_windings_transformers['nominal_v2']
        return two_windings_transformers.drop(['nominal_v1', 'nominal_v2'], axis=1)

    def get_3_windings_transformers(self) -> _pd.DataFrame:
        """ Get 3 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 3 windings transformers data frame
        """
        three_windings_transformers = self.get_elements(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER)
        three_windings_transformers['p1'] /= self.sn
        three_windings_transformers['q1'] /= self.sn
        three_windings_transformers['p2'] /= self.sn
        three_windings_transformers['q2'] /= self.sn
        three_windings_transformers['p3'] /= self.sn
        three_windings_transformers['q3'] /= self.sn
        three_windings_transformers = _pd.merge(three_windings_transformers, self.get_voltage_levels()['nominal_v'],
                                                left_on='voltage_level1_id', right_index=True)
        three_windings_transformers.rename(columns={'nominal_v': 'nominal_v1'}, inplace=True)
        three_windings_transformers = _pd.merge(three_windings_transformers, self.get_voltage_levels()['nominal_v'],
                                                left_on='voltage_level2_id', right_index=True)
        three_windings_transformers.rename(columns={'nominal_v': 'nominal_v2'}, inplace=True)
        three_windings_transformers = _pd.merge(three_windings_transformers, self.get_voltage_levels()['nominal_v'],
                                                left_on='voltage_level3_id', right_index=True)
        three_windings_transformers.rename(columns={'nominal_v': 'nominal_v3'}, inplace=True)
        three_windings_transformers['i1'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * three_windings_transformers['nominal_v1'])
        three_windings_transformers['i2'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * three_windings_transformers['nominal_v2'])
        three_windings_transformers['i3'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * three_windings_transformers['nominal_v3'])
        three_windings_transformers['r1'] /= three_windings_transformers['rated_u0'] ** 2 / self.sn
        three_windings_transformers['x1'] /= three_windings_transformers['rated_u0'] ** 2 / self.sn
        three_windings_transformers['g1'] /= self.sn / three_windings_transformers[
            'rated_u0'] ** 2
        three_windings_transformers['b1'] /= self.sn / three_windings_transformers[
            'rated_u0'] ** 2
        three_windings_transformers['r2'] /= three_windings_transformers['rated_u0'] ** 2 / self.sn
        three_windings_transformers['x2'] /= three_windings_transformers['rated_u0'] ** 2 / self.sn
        three_windings_transformers['g2'] /= self.sn / three_windings_transformers[
            'rated_u0'] ** 2
        three_windings_transformers['b2'] /= self.sn / three_windings_transformers[
            'rated_u0'] ** 2
        three_windings_transformers['r3'] /= three_windings_transformers['rated_u0'] ** 2 / self.sn
        three_windings_transformers['x3'] /= three_windings_transformers['rated_u0'] ** 2 / self.sn
        three_windings_transformers['g3'] /= self.sn / three_windings_transformers[
            'rated_u0'] ** 2
        three_windings_transformers['b3'] /= self.sn / three_windings_transformers[
            'rated_u0'] ** 2
        three_windings_transformers['rated_u1'] /= three_windings_transformers['nominal_v1']
        three_windings_transformers['rated_u2'] /= three_windings_transformers['nominal_v2']
        three_windings_transformers['rated_u3'] /= three_windings_transformers['nominal_v3']
        three_windings_transformers['rated_u0'] = 1
        return three_windings_transformers.drop(['nominal_v1', 'nominal_v2', 'nominal_v3'],
                                                axis=1)

    def get_shunt_compensators(self) -> _pd.DataFrame:
        """ Get shunt compensators as a ``Pandas`` data frame.

        Returns:
            a shunt compensators data frame
        """
        shunt_compensators = self.get_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR)
        shunt_compensators = _pd.merge(shunt_compensators, self.get_voltage_levels()['nominal_v'],
                                       left_on='voltage_level_id', right_index=True)
        shunt_compensators['p'] /= self.sn
        shunt_compensators['q'] /= self.sn
        shunt_compensators['g'] /= self.sn / shunt_compensators['nominal_v'] ** 2
        shunt_compensators['b'] /= self.sn / shunt_compensators['nominal_v'] ** 2
        return shunt_compensators.drop('nominal_v', axis=1)

    def get_dangling_lines(self) -> _pd.DataFrame:
        """ Get dangling lines as a ``Pandas`` data frame.

        Returns:
            a dangling lines data frame
        """
        dangling_lines = self.get_elements(_pypowsybl.ElementType.DANGLING_LINE)
        dangling_lines['p'] /= self.sn
        dangling_lines['q'] /= self.sn
        dangling_lines['p0'] /= self.sn
        dangling_lines['q0'] /= self.sn
        dangling_lines = _pd.merge(dangling_lines, self.get_voltage_levels()['nominal_v'],
                                   left_on='voltage_level_id', right_index=True)
        dangling_lines['i'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * dangling_lines['nominal_v'])
        dangling_lines['r'] /= dangling_lines['nominal_v'] ** 2 / self.sn
        dangling_lines['x'] /= dangling_lines['nominal_v'] ** 2 / self.sn
        dangling_lines['g'] /= self.sn / dangling_lines['nominal_v'] ** 2
        dangling_lines['b'] /= self.sn / dangling_lines['nominal_v'] ** 2
        return dangling_lines.drop('nominal_v', axis=1)

    def get_lcc_converter_stations(self) -> _pd.DataFrame:
        """ Get LCC converter stations as a ``Pandas`` data frame.

        Returns:
            a LCC converter stations data frame
        """
        lcc_converter_stations = self.get_elements(_pypowsybl.ElementType.LCC_CONVERTER_STATION)
        lcc_converter_stations['p'] /= self.sn
        lcc_converter_stations['q'] /= self.sn
        lcc_converter_stations = _pd.merge(lcc_converter_stations, self.get_voltage_levels()['nominal_v'],
                                           left_on='voltage_level_id', right_index=True)
        lcc_converter_stations['i'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * lcc_converter_stations['nominal_v'])
        return lcc_converter_stations.drop('nominal_v', axis=1)

    def get_vsc_converter_stations(self) -> _pd.DataFrame:
        """ Get VSC converter stations as a ``Pandas`` data frame.

        Returns:
            a VSC converter stations data frame
        """
        vsc_converter_stations = self.get_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION)
        vsc_converter_stations['p'] /= self.sn
        vsc_converter_stations['q'] /= self.sn
        vsc_converter_stations['reactive_power_setpoint'] /= self.sn
        vsc_converter_stations = _pd.merge(vsc_converter_stations, self.get_voltage_levels()['nominal_v'],
                                           left_on='voltage_level_id', right_index=True)
        vsc_converter_stations['i'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * vsc_converter_stations['nominal_v'])
        vsc_converter_stations['voltage_setpoint'] /= vsc_converter_stations['nominal_v']
        return vsc_converter_stations.drop('nominal_v', axis=1)

    def get_static_var_compensators(self) -> _pd.DataFrame:
        """ Get static var compensators as a ``Pandas`` data frame.

        Returns:
            a static var compensators data frame
        """
        static_var_compensators = self.get_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR)
        static_var_compensators['p'] /= self.sn
        static_var_compensators['q'] /= self.sn
        static_var_compensators['reactive_power_setpoint'] /= self.sn
        static_var_compensators = _pd.merge(static_var_compensators, self.get_voltage_levels()['nominal_v'],
                                            left_on='voltage_level_id', right_index=True)
        static_var_compensators['i'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * static_var_compensators['nominal_v'])
        static_var_compensators['voltage_setpoint'] /= static_var_compensators['nominal_v']
        return static_var_compensators.drop('nominal_v', axis=1)

    def get_voltage_levels(self) -> _pd.DataFrame:
        """ Get voltage levels as a ``Pandas`` data frame.

        Returns:
            a voltage levels data frame
        """
        voltage_levels = self.get_elements(_pypowsybl.ElementType.VOLTAGE_LEVEL)
        voltage_levels['high_voltage_limit'] /= voltage_levels['nominal_v']
        voltage_levels['low_voltage_limit'] /= voltage_levels['nominal_v']
        return voltage_levels

    def get_busbar_sections(self) -> _pd.DataFrame:
        """ Get busbar sections as a ``Pandas`` data frame.

        Returns:
            a busbar sections data frame
        """
        busbar_sections = self.get_elements(_pypowsybl.ElementType.BUSBAR_SECTION)
        join = _pd.merge(busbar_sections, self.get_voltage_levels()['nominal_v'],
                         left_on='voltage_level_id', right_index=True)
        join['v'] /= join['nominal_v']
        return join.drop('nominal_v', axis=1)

    def get_hvdc_lines(self) -> _pd.DataFrame:
        """ Get HVDC lines as a ``Pandas`` data frame.

        Returns:
            a HVDC lines data frame
        """
        hvdc_lines = self.get_elements(_pypowsybl.ElementType.HVDC_LINE)

        hvdc_lines['max_p'] /= self.sn
        hvdc_lines['active_power_setpoint'] /= self.sn
        hvdc_lines['r'] /= hvdc_lines['nominal_v'] ** 2 / self.sn
        return hvdc_lines

    def get_reactive_capability_curve_points(self) -> _pd.DataFrame:
        """ Get reactive capability curve points as a ``Pandas`` data frame.

        Returns:
            a reactive capability curve points data frame
        """
        reactive_capability_curve_points = self.get_elements(_pypowsybl.ElementType.REACTIVE_CAPABILITY_CURVE_POINT)
        reactive_capability_curve_points['p'] /= self.sn
        reactive_capability_curve_points['min_q'] /= self.sn
        reactive_capability_curve_points['max_q'] /= self.sn
        return reactive_capability_curve_points

    def get_batteries(self) -> _pd.DataFrame:
        batteries = self.get_elements(_pypowsybl.ElementType.BATTERY)
        batteries['p0'] /= self.sn
        batteries['q0'] /= self.sn
        batteries['p'] /= self.sn
        batteries['q'] /= self.sn
        batteries['min_p'] /= self.sn
        batteries['max_p'] /= self.sn
        batteries = _pd.merge(batteries, self.get_voltage_levels()['nominal_v'],
                              left_on='voltage_level_id', right_index=True)
        batteries['i'] /= self.sn * 10 ** 3 / (
                self.sqrt3 * batteries['nominal_v'])
        return batteries.drop(['nominal_v'], axis=1)

    def get_ratio_tap_changers(self) -> _pd.DataFrame:
        ratio_tap_changers = self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER)
        ratio_tap_changers = ratio_tap_changers.merge(
            self.get_2_windings_transformers()[['voltage_level1_id', 'voltage_level2_id']],
            left_index=True, right_index=True)
        ratio_tap_changers = ratio_tap_changers.merge(self.get_voltage_levels()['nominal_v'],
                                                      left_on='voltage_level1_id', right_index=True)
        ratio_tap_changers.rename(columns={'nominal_v': 'nominal_v1'}, inplace=True)
        ratio_tap_changers = ratio_tap_changers.merge(self.get_voltage_levels()['nominal_v'],
                                                      left_on='voltage_level2_id', right_index=True)
        ratio_tap_changers.rename(columns={'nominal_v': 'nominal_v2'}, inplace=True)
        ratio_tap_changers['rho'] *= ratio_tap_changers['nominal_v1'] / ratio_tap_changers['nominal_v2']
        return ratio_tap_changers.drop(['nominal_v1', 'nominal_v2', 'voltage_level1_id', 'voltage_level2_id'], axis=1)

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
