#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
import sys
from _pypowsybl import ElementType
from _pypowsybl import PyPowsyblError
from typing import List
from typing import Set

import pandas as pd
import datetime

from pypowsybl.util import create_data_frame_from_series_array


class SingleLineDiagram:
    """ This class represents a single line diagram."""

    def __init__(self, svg: str):
        self._svg = svg

    @property
    def svg(self):
        return self._svg

    def __str__(self):
        return self._svg

    def _repr_svg_(self):
        return self._svg


class Network(object):

    def __init__(self, handle):
        self._handle = handle
        att = _pypowsybl.get_network_metadata(self._handle)
        self._id = att.id
        self._name = att.name
        self._source_format = att.source_format
        self._forecast_distance = datetime.timedelta(minutes=att.forecast_distance)
        self._case_date = datetime.datetime.utcfromtimestamp(att.case_date)

    @property
    def id(self) -> str:
        """
        ID of this network
        """
        return self._id

    @property
    def name(self) -> str:
        """
        Name of this network
        """
        return self._name

    @property
    def source_format(self) -> str:
        """
        Format of the source where this network came from.
        """
        return self._source_format

    @property
    def case_date(self) -> datetime.datetime:
        """
        Date of this network case, in UTC timezone.
        """
        return self._case_date

    @property
    def forecast_distance(self) -> datetime.timedelta:
        """
        The forecast distance: 0 for a snapshot.
        """
        return self._forecast_distance

    def __str__(self) -> str:
        return f'Network(id={self.id}, name={self.name}, case_date={self.case_date}, ' \
               f'forecast_distance={self.forecast_distance}, source_format={self.source_format})'

    def __repr__(self) -> str:
        return str(self)

    def __getstate__(self):
        return {'xml': self.dump_to_string()}

    def __setstate__(self, state):
        xml = state['xml']
        n = _pypowsybl.load_network_from_string('tmp.xiidm', xml, {})
        self._handle = n

    def open_switch(self, id: str):
        return _pypowsybl.update_switch_position(self._handle, id, True)

    def close_switch(self, id: str):
        return _pypowsybl.update_switch_position(self._handle, id, False)

    def connect(self, id: str):
        return _pypowsybl.update_connectable_status(self._handle, id, True)

    def disconnect(self, id: str):
        return _pypowsybl.update_connectable_status(self._handle, id, False)

    def dump(self, file: str, format: str = 'XIIDM', parameters: dict = {}):
        """Save a network to a file using a specified format.

        Args:
            file (str): a file
            format (str, optional): format to save the network, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters
        """
        _pypowsybl.dump_network(self._handle, file, format, parameters)

    def dump_to_string(self, format: str = 'XIIDM', parameters: dict = {}) -> str:
        """Save a network to a string using a specified format.

        Args:
            format (str, optional): format to export, only support mono file type, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters

        Returns:
            a string representing network
        """
        return _pypowsybl.dump_network_to_string(self._handle, format, parameters)

    def reduce(self, v_min: float = 0, v_max: float = sys.float_info.max, ids: List[str] = [],
               vl_depths: tuple = (), with_dangling_lines: bool = False):
        vls = []
        depths = []
        for v in vl_depths:
            vls.append(v[0])
            depths.append(v[1])
        _pypowsybl.reduce_network(self._handle, v_min, v_max, ids, vls, depths, with_dangling_lines)

    def write_single_line_diagram_svg(self, container_id: str, svg_file: str):
        """ Create a single line diagram in SVG format from a voltage level or a substation and write to a file.

        Args:
            container_id: a voltage level id or a substation id
            svg_file: a svg file path
        """
        _pypowsybl.write_single_line_diagram_svg(self._handle, container_id, svg_file)

    def get_single_line_diagram(self, container_id: str):
        """ Create a single line diagram from a voltage level or a substation.

        Args:
            container_id: a voltage level id or a substation id

        Returns:
            the single line diagram
        """
        return SingleLineDiagram(_pypowsybl.get_single_line_diagram_svg(self._handle, container_id))

    def get_elements_ids(self, element_type: _pypowsybl.ElementType, nominal_voltages: Set[float] = None,
                         countries: Set[str] = None,
                         main_connected_component: bool = True, main_synchronous_component: bool = True,
                         not_connected_to_same_bus_at_both_sides: bool = False) -> List[str]:
        return _pypowsybl.get_network_elements_ids(self._handle, element_type,
                                                   [] if nominal_voltages is None else list(nominal_voltages),
                                                   [] if countries is None else list(countries),
                                                   main_connected_component, main_synchronous_component,
                                                   not_connected_to_same_bus_at_both_sides)

    def get_elements(self, element_type: _pypowsybl.ElementType) -> pd.DataFrame:
        """ Get network elements as a ``Pandas`` data frame for a specified element type.

        Args:
            element_type (ElementType): the element type
        Returns:
            a network elements data frame for the specified element type
        """
        series_array = _pypowsybl.create_network_elements_series_array(self._handle, element_type)
        return create_data_frame_from_series_array(series_array)

    def get_buses(self) -> pd.DataFrame:
        """ Get buses as a ``Pandas`` data frame.

                Note:
                    The resulting dataframe will have the following columns:

                      - "v_mag": Get the voltage magnitude of the bus (in kV)
                      - "v_angle": the voltage angle of the bus (in degree)
                      - "connected_component": the number of terminals connected to this bus
                      - "synchronous_component": the number of synchronous components that the bus is part of
                      - "voltage_level_id": at which substation the bus is connected

                    This dataframe is index by the name of the LCC converter

                Examples

                    .. code-block:: python

                        import pypowsybl as pypo
                        net = pypo.network.create_four_substations_node_breaker_network()
                        net.get_buses()

                    It outputs something like:

                    ======= ======== ======= =================== ===================== ================
                          .    v_mag v_angle connected_component synchronous_component voltage_level_id
                    ======= ======== ======= =================== ===================== ================
                    id
                    S1VL1_0 224.6139  2.2822                   0                     1            S1VL1
                    S1VL2_0 400.0000  0.0000                   0                     1            S1VL2
                    S2VL1_0 408.8470  0.7347                   0                     0            S2VL1
                    S3VL1_0 400.0000  0.0000                   0                     0            S3VL1
                    S4VL1_0 400.0000 -1.1259                   0                     0            S4VL1
                    ======= ======== ======= =================== ===================== ================

                Returns:
                    a buses data frame
                """
        return self.get_elements(_pypowsybl.ElementType.BUS)

    def get_generators(self) -> pd.DataFrame:
        """  Get generators as a ``Pandas`` data frame.
            Returns:
                the generator data frame.

            Note:
                The resulting dataframe will have the following columns:

                  - "energy_source": the energy source used to fuel the generator
                  - "target_p": the target active value for the generator (in MW)
                  - "max_p": the maximum active value for the generator  (MW)
                  - "min_p": the minimum active value for the generator  (MW)
                  - "target_v": the target voltage magnitude value for the generator (in kV)
                  - "target_q": the target reactive value for the generator (in MVAr)
                  - "voltage_regulator_on":
                  - "p" the actual active production of the generator (Nan if no powerflow has been computed)
                  - "q" the actual reactive production of the generator (Nan if no powerflow has been computed)
                  - "voltage_level_id": at which substation this generator is connected
                  - "bus_id": at which bus this generator is computed

                This dataframe is index by the name of the generators

            Examples

                .. code-block:: python

                    import pypowsybl as pypo
                    net = pypo.network.create_ieee14()
                    net.get_generators()

                It outputs something like:

                ==== ============= ======== ====== ======= ======== ======== ==================== === === ================ ======
                   . energy_source target_p  max_p   min_p target_v target_q voltage_regulator_on   p   q voltage_level_id bus_id
                ==== ============= ======== ====== ======= ======== ======== ==================== === === ================ ======
                id
                B1-G         OTHER    232.4 9999.0 -9999.0    1.060    -16.9                 True NaN NaN              VL1  VL1_0
                B2-G         OTHER     40.0 9999.0 -9999.0    1.045     42.4                 True NaN NaN              VL2  VL2_0
                B3-G         OTHER      0.0 9999.0 -9999.0    1.010     23.4                 True NaN NaN              VL3  VL3_0
                B6-G         OTHER      0.0 9999.0 -9999.0    1.070     12.2                 True NaN NaN              VL6  VL6_0
                B8-G         OTHER      0.0 9999.0 -9999.0    1.090     17.4                 True NaN NaN              VL8  VL8_0
                ==== ============= ======== ====== ======= ======== ======== ==================== === === ================ ======


            .. warning::

                The "generator convention" is used for the "input" columns (`target_p`, `max_p`,
                `min_p`, `target_v` and `target_q`) while the "load convention" is used for the ouput columns
                (`p` and `q`).

                Most of the time, this means that `p` and `target_p` will have opposite sign. This also entails that
                `p` can be lower than `min_p`. Actually, the relation: :math:`\\text{min_p} <= -p <= \\text{max_p}`
                should hold.
                """
        return self.get_elements(_pypowsybl.ElementType.GENERATOR)

    def get_loads(self) -> pd.DataFrame:
        """ Get loads as a ``Pandas`` data frame.
        Returns:
            the load data frame

        Note:
            The resulting dataframe will have the following columns:

              - "type": type of load
              - "p0": the active load consumption setpoint (MW)
              - "q0": the reactive load consumption setpoint  (MVAr)
              - "p": the result active load consumption, it is Nan is not powerflow has been computed (MW)
              - "q": the result reactive load consumption, it is Nan is not powerflow has been computed (MVAr)
              - "i": the current on the load, Nan if no powerlow are computed (in A)
              - "voltage_level_id": at which substation this load is connected
              - "bus_id": at which bus this load is connected

            This dataframe is index by the name of the loads.

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_ieee14()
                net.get_loads()

            It outputs something like:

            ===== ========== ===== ===== === === ================ ======= =========
                .       type    p0    q0   p   q voltage_level_id  bus_id connected
            ===== ========== ===== ===== === === ================ ======= =========
            id
            B2-L   UNDEFINED  21.7  12.7 NaN NaN              VL2   VL2_0      True
            B3-L   UNDEFINED  94.2  19.0 NaN NaN              VL3   VL3_0      True
            B4-L   UNDEFINED  47.8  -3.9 NaN NaN              VL4   VL4_0      True
            B5-L   UNDEFINED   7.6   1.6 NaN NaN              VL5   VL5_0      True
            B6-L   UNDEFINED  11.2   7.5 NaN NaN              VL6   VL6_0      True
            B9-L   UNDEFINED  29.5  16.6 NaN NaN              VL9   VL9_0      True
            B10-L  UNDEFINED   9.0   5.8 NaN NaN             VL10  VL10_0      True
            B11-L  UNDEFINED   3.5   1.8 NaN NaN             VL11  VL11_0      True
            B12-L  UNDEFINED   6.1   1.6 NaN NaN             VL12  VL12_0      True
            B13-L  UNDEFINED  13.5   5.8 NaN NaN             VL13  VL13_0      True
            B14-L  UNDEFINED  14.9   5.0 NaN NaN             VL14  VL14_0      True
            ===== ========== ===== ===== === === ================ ======= =========

        """
        return self.get_elements(_pypowsybl.ElementType.LOAD)

    def get_batteries(self) -> pd.DataFrame:
        """ Get batteries as a ``Pandas`` data frame.

        Returns:
            a batteries data frame
        """
        return self.get_elements(_pypowsybl.ElementType.BATTERY)

    def get_lines(self) -> pd.DataFrame:
        """ Get lines as a ``Pandas`` data frame.

                Returns:
                    a lines data frame
                Note:
                    The resulting dataframe will have the following columns:

                      - "r": the resistance of the line (in Ohm)
                      - "x": the reactance of the line (in Ohm)
                      - "g1": the  conductance of line at its "1" side (in Siemens)
                      - "b1": the susceptance of line at its "1" side (in Siemens)
                      - "g2": the  conductance of line at its "2" side (in Siemens)
                      - "b2": the susceptance of line at its "2" side (in Siemens)
                      - "p1": the active flow on the line at its "1" side, Nan if no powerlow are computed (in MW)
                      - "q1": the reactive flow on the line at its "1" side, Nan if no powerlow are computed  (in MVAr)
                      - "i1": the current on the line at its "1" side, Nan if no powerlow are computed (in A)
                      - "p2": the active flow on the line at its "2" side, Nan if no powerlow are computed  (in MW)
                      - "q2": the reactive flow on the line at its "2" side, Nan if no powerlow are computed  (in MVAr)
                      - "i2": the current on the line at its "2" side, Nan if no powerlow are computed (in A)
                      - "voltage_level1_id": at which substation the "1" side of the powerline is connected
                      - "voltage_level2_id": at which substation the "2" side of the powerline is connected
                      - "bus1_id": at which bus the "1" side of the powerline is connected
                      - "bus2_id": at which bus the "2" side of the powerline is connected
                      - "connected1": indicate if the side "1" of the line is connected to a bus
                      - "connected2": indicate if the side "2" of the line is connected to a bus

                    This dataframe is index by the name of the powerlines

                Examples

                    .. code-block:: python

                        import pypowsybl as pypo
                        net = pypo.network.create_ieee14()
                        net.get_lines()

                    It outputs something like:

                    ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
                           .         r         x   g1    b1   g2    b2  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
                    ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
                    id
                    L1-2-1    0.000194  0.000592  0.0  2.64  0.0  2.64 NaN NaN NaN NaN NaN NaN               VL1               VL2   VL1_0   VL2_0       True       True
                    L1-5-1    0.000540  0.002230  0.0  2.46  0.0  2.46 NaN NaN NaN NaN NaN NaN               VL1               VL5   VL1_0   VL5_0       True       True
                    L2-3-1    0.000470  0.001980  0.0  2.19  0.0  2.19 NaN NaN NaN NaN NaN NaN               VL2               VL3   VL2_0   VL3_0       True       True
                    L2-4-1    0.000581  0.001763  0.0  1.70  0.0  1.70 NaN NaN NaN NaN NaN NaN               VL2               VL4   VL2_0   VL4_0       True       True
                    L2-5-1    0.000570  0.001739  0.0  1.73  0.0  1.73 NaN NaN NaN NaN NaN NaN               VL2               VL5   VL2_0   VL5_0       True       True
                    L3-4-1    0.000670  0.001710  0.0  0.64  0.0  0.64 NaN NaN NaN NaN NaN NaN               VL3               VL4   VL3_0   VL4_0       True       True
                    L4-5-1    0.000134  0.000421  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL4               VL5   VL4_0   VL5_0       True       True
                    L6-11-1   0.000950  0.001989  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL6              VL11   VL6_0  VL11_0       True       True
                    L6-12-1   0.001229  0.002558  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL6              VL12   VL6_0  VL12_0       True       True
                    L6-13-1   0.000661  0.001303  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL6              VL13   VL6_0  VL13_0       True       True
                    L7-8-1    0.000000  0.001762  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL7               VL8   VL7_0   VL8_0       True       True
                    L7-9-1    0.000000  0.001100  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL7               VL9   VL7_0   VL9_0       True       True
                    L9-10-1   0.000318  0.000845  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL9              VL10   VL9_0  VL10_0       True       True
                    L9-14-1   0.001271  0.002704  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN               VL9              VL14   VL9_0  VL14_0       True       True
                    L10-11-1  0.000821  0.001921  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN              VL10              VL11  VL10_0  VL11_0       True       True
                    L12-13-1  0.002209  0.001999  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN              VL12              VL13  VL12_0  VL13_0       True       True
                    L13-14-1  0.001709  0.003480  0.0  0.00  0.0  0.00 NaN NaN NaN NaN NaN NaN              VL13              VL14  VL13_0  VL14_0       True       True
                    ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
                """
        return self.get_elements(_pypowsybl.ElementType.LINE)

    def get_2_windings_transformers(self) -> pd.DataFrame:
        """ Get 2 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 2 windings transformers data frame
        Note:
            The resulting dataframe will have the following columns:

              -  "r": the resistance of the transformer at its "2" side  (in Ohm)
              -  "x": the reactance of the transformer at its "2" side (in Ohm)
              -  "b": the susceptance of transformer at its "2" side (in Siemens)
              -  "g": the  conductance of transformer at its "2" side (in Siemens)
              -  "rated_u1": The rated voltage of the transformer at side 1 (in kV)
              -  "rated_u2": The rated voltage of the transformer at side 2 (in kV)
              -  "rated_s":
              -  "p1": the active flow on the transformer at its "1" side, Nan if no powerlow are computed (in MW)
              -  "q1": the reactive flow on the transformer at its "1" side, Nan if no powerlow are computed  (in MVAr)
              -  "i1": the current on the transformer at its "1" side, Nan if no powerlow are computed (in A)
              -  "p2": the active flow on the transformer at its "2" side, Nan if no powerlow are computed  (in MW)
              -  "q2": the reactive flow on the transformer at its "2" side, Nan if no powerlow are computed  (in MVAr)
              -  "i2": the current on the transformer at its "2" side, Nan if no powerlow are computed (in A)
              -  "voltage_level1_id": at which substation the "1" side of the transformer is connected
              -  "voltage_level2_id": at which substation the "2" side of the transformer is connected
              -  "connected1": indicate if the side "1" of the transformer is connected to a bus
              -  "connected2": indicate if the side "2" of the transformer is connected to a bus
            This dataframe is index by the name of the two windings transformers
        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_ieee14()
                net.get_2_windings_transformers()

            It outputs something like:
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
                 .    r        x   g   b rated_u1 rated_u2 rated_s  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
            id
            T4-7-1  0.0 0.409875 0.0 0.0  132.030     14.0     NaN NaN NaN NaN NaN NaN NaN               VL4               VL7   VL4_0   VL7_0       True       True
            T4-9-1  0.0 0.800899 0.0 0.0  130.815     12.0     NaN NaN NaN NaN NaN NaN NaN               VL4               VL9   VL4_0   VL9_0       True       True
            T5-6-1  0.0 0.362909 0.0 0.0  125.820     12.0     NaN NaN NaN NaN NaN NaN NaN               VL5               VL6   VL5_0   VL6_0       True       True
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========

        """
        return self.get_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER)

    def get_3_windings_transformers(self) -> pd.DataFrame:
        """ Get 3 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 3 windings transformers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER)

    def get_shunt_compensators(self) -> pd.DataFrame:
        """ Get shunt compensators as a ``Pandas`` data frame.

        Returns:
            a shunt compensators data frame
        Note:
            The resulting dataframe will have the following columns:

              - "model_type":
              - "max_section_count": The maximum number of sections that may be switched on
              - "section_count": The current number of section that may be switched on
              - "p": the active flow on the shunt, Nan if no powerlow are computed (in MW)
              - "q": the reactive flow on the shunt, Nan if no powerlow are computed  (in MVAr)
              - "i": the current in the shunt, Nan if no powerlow are computed  (in A)
              - "voltage_level_id": at which substation the shunt is connected
              - "bus_id": indicate at which bus the shunt is connected
              - "connected": indicate if the shunt is connected to a bus

            This dataframe is index by the name of the shunt compensators

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_ieee14()
                net.get_shunt_compensators()

            It outputs something like:

            ===== ========== ================= ============= === === === ================ ====== =========
                . model_type max_section_count section_count   p   q   i voltage_level_id bus_id connected
            ===== ========== ================= ============= === === === ================ ====== =========
            id
            B9-SH     LINEAR                 1             1 NaN NaN NaN              VL9  VL9_0      True
            ===== ========== ================= ============= === === === ================ ====== =========
        """
        return self.get_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR)

    def get_dangling_lines(self) -> pd.DataFrame:
        """ Get dangling lines as a ``Pandas`` data frame.

        Returns:
            a dangling lines data frame

        Note:
            The resulting dataframe will have the following columns:

              - "r": The resistance of the dangling line (Ohm)
              - "x": The reactance of the dangling line (Ohm)
              - "g": the conductance of dangling line (in Siemens)
              - "b": the susceptance of dangling line (in Siemens)
              - "p0": The active power setpoint
              - "q0": The reactive power setpoint
              - "p": active flow on the dangling line, Nan if no powerlow are computed (in MW)
              - "q": the reactive flow on the dangling line, Nan if no powerlow are computed  (in MVAr)
              - "i": The current on the dangling line, Nan if no powerlow are computed (in A)
              - "voltage_level_id": at which substation the dangling line is connected
              - "bus_id": at which bus the dangling line is connected
              - "connected": indicate if the dangling line is connected to a bus

            This dataframe is index by the name of the dangling lines

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network._create_dangling_lines_network()
                net.get_dangling_lines()


            It outputs something like:

            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
             .    r   x      g       b   p0   q0   p   q   i voltage_level_id bus_id connected
            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
            id
            DL 10.0 1.0 0.0001 0.00001 50.0 30.0 NaN NaN NaN               VL   VL_0      True
            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
        """
        return self.get_elements(_pypowsybl.ElementType.DANGLING_LINE)

    def get_lcc_converter_stations(self) -> pd.DataFrame:
        """ Get LCC converter stations as a ``Pandas`` data frame.

        Note:
            The resulting dataframe will have the following columns:

              - "power_factor": the power factor
              - "loss_factor": the loss factor
              - "p": active flow on the LCC converter station, Nan if no powerlow are computed (in MW)
              - "q": the reactive flow on the LCC converter station, Nan if no powerlow are computed  (in MVAr)
              - "i": The current on the LCC converter station, Nan if no powerlow are computed (in A)
              - "voltage_level_id": at which substation the LCC converter station is connected
              - "bus_id": at which bus the LCC converter station is connected
              - "connected": indicate if the LCC converter station is connected to a bus

            This dataframe is index by the name of the LCC converter

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_lcc_converter_stations()



            It outputs something like:

            ======== ============ ===========  ====== === === ================ ======= =========
                .    power_factor loss_factor       p   q   i voltage_level_id  bus_id connected
            ======== ============ ===========  ====== === === ================ ======= =========
            id
                LCC1          0.6         1.1   80.88 NaN NaN            S1VL2 S1VL2_0      True
                LCC2          0.6         1.1  -79.12 NaN NaN            S3VL1 S3VL1_0      True
            ======== ============ ===========  ====== === === ================ ======= =========

        Returns:
            a LCC converter stations data frame
        """
        return self.get_elements(_pypowsybl.ElementType.LCC_CONVERTER_STATION)

    def get_vsc_converter_stations(self) -> pd.DataFrame:
        """ Get VSC converter stations as a ``Pandas`` data frame.

        Note:
            The resulting dataframe will have the following columns:

              - "voltage_setpoint": The voltage setpoint
              - "reactive_power_setpoint": The reactive power setpoint
              - "voltage_regulator_on": The voltage regulator status
              - "p": active flow on the VSC  converter station, Nan if no powerlow are computed (in MW)
              - "q": the reactive flow on the VSC converter station, Nan if no powerlow are computed  (in MVAr)
              - "i": The current on the VSC converter station, Nan if no powerlow are computed (in A)
              - "voltage_level_id": at which substation the VSC converter station is connected
              - "bus_id": at which bus the VSC converter station is connected
              - "connected": indicate if the VSC converter station is connected to a bus

            This dataframe is index by the name of the VSC converter

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_vsc_converter_stations()



            It outputs something like:

            ======== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
                .    voltage_setpoint reactive_power_setpoint voltage_regulator_on      p         q          i voltage_level_id  bus_id connected
            ======== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
            id
                VSC1            400.0                   500.0                 True  10.11 -512.0814 739.269871            S1VL2 S1VL2_0      True
                VSC2              0.0                   120.0                False  -9.89 -120.0000 170.031658            S2VL1 S2VL1_0      True
            ======== ================ ======================= ==================== ====== ========= ========== ================ ======= =========

        Returns:
            a VCS converter stations data frame
        """
        return self.get_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION)

    def get_static_var_compensators(self) -> pd.DataFrame:
        """ Get static var compensators as a ``Pandas`` data frame.

         Note:
            The resulting dataframe will have the following columns:

              - "voltage_setpoint": The voltage setpoint
              - "reactive_power_setpoint": The reactive power setpoint
              - "regulation_mode": The regulation mode
              - "p": active flow on the var compensator, Nan if no powerlow are computed (in MW)
              - "q": the reactive flow on the var compensator, Nan if no powerlow are computed  (in MVAr)
              - "i": The current on the var compensator, Nan if no powerlow are computed (in A)
              - "voltage_level_id": at which substation the var compensator is connected
              - "bus_id": at which bus the var compensator is connected
              - "connected": indicate if the var compensator is connected to a bus

            This dataframe is index by the name of the var compensator

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators()



            It outputs something like:

            ======== ================ ======================= =============== === ======== === ================ ======= =========
                .    voltage_setpoint reactive_power_setpoint regulation_mode  p        q   i  voltage_level_id  bus_id connected
            ======== ================ ======================= =============== === ======== === ================ ======= =========
            id
                 SVC            400.0                     NaN         VOLTAGE NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== ================ ======================= =============== === ======== === ================ ======= =========

        Returns:
            a static var compensators data frame
        """
        return self.get_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR)

    def get_voltage_levels(self) -> pd.DataFrame:
        """ Get voltage levels as a ``Pandas`` data frame.
        Note:
            The resulting dataframe will have the following columns:

              - "substation_id": at which substation the voltage level belongs
              - "nominal_v": The nominal voltage
              - "high_voltage_limit": the high voltage limit
              - "low_voltage_limit": the low voltage limit

            This dataframe is index by the name of the voltage levels

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_voltage_levels()



            It outputs something like:

            ========= ============= ========= ================== =================
                .     substation_id nominal_v high_voltage_limit low_voltage_limit
            ========= ============= ========= ================== =================
            id
                S1VL1            S1     225.0              240.0             220.0
                S1VL2            S1     400.0              440.0             390.0
                S2VL1            S2     400.0              440.0             390.0
                S3VL1            S3     400.0              440.0             390.0
                S4VL1            S4     400.0              440.0             390.0
            ========= ============= ========= ================== =================

        Returns:
            a voltage levels data frame
            Args:
                df (DataFrame): the ``Pandas`` data frame

            """
        return self.get_elements(_pypowsybl.ElementType.VOLTAGE_LEVEL)

    def get_busbar_sections(self) -> pd.DataFrame:
        """ Get busbar sections as a ``Pandas`` data frame.

        Note:
            The resulting dataframe will have the following columns:

              - "fictitious": indicate if the busbar section is part of the model and not of the actual network
              - "v": The voltage magnitude of the busbar section (in kV)
              - "angle": the voltage angle of the busbar section (in radian)
              - "voltage_level_id": at which substation the busbar section is connected
              - "connected": indicate if the busbar section is connected to a bus

            This dataframe is index by the name of the busbar sections

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_busbar_sections()



            It outputs something like:

            ========== ========== ======== ======== ================ =========
                 .     fictitious        v   angle voltage_level_id connected
            ========== ========== ======== ======== ================ =========
            id
             S1VL1_BBS      False 224.6139   2.2822            S1VL1      True
            S1VL2_BBS1      False 400.0000   0.0000            S1VL2      True
            S1VL2_BBS2      False 400.0000   0.0000            S1VL2      True
             S2VL1_BBS      False 408.8470   0.7347            S2VL1      True
             S3VL1_BBS      False 400.0000   0.0000            S3VL1      True
             S4VL1_BBS      False 400.0000  -1.1259            S4VL1      True
            ========== ========== ========  ======= ================ =========
        Returns:
            a busbar sections data frame
        """
        return self.get_elements(_pypowsybl.ElementType.BUSBAR_SECTION)

    def get_substations(self) -> pd.DataFrame:
        """ Get substations ``Pandas`` data frame.

        Returns:
            a substations data frame
        """
        return self.get_elements(_pypowsybl.ElementType.SUBSTATION)

    def get_hvdc_lines(self) -> pd.DataFrame:
        """ Get HVDC lines as a ``Pandas`` data frame.

        Note:
            The resulting dataframe will have the following columns:

              - "converters_mode":
              - "active_power_setpoint": (in MW)
              - "max_p": the maximum of active power that can pass through the hvdc line (in MW)
              - "nominal_v": nominal voltage (in kV)
              - "r": the resistance of the hvdc line (in Ohm)
              - "converter_station1_id": at which converter station the hvdc line is connected on side "1"
              - "converter_station2_id": at which converter station the hvdc line is connected on side "2"
              - "connected1": indicate if the busbar section on side "1" is connected to a bus
              - "connected2": indicate if the busbar section on side "2" is connected to a bus

            This dataframe is index by the name of the hvdc lines

        Examples

            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_hvdc_lines()

            It outputs something like:
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
                .                  converters_mode active_power_setpoint max_p nominal_v    r converter_station1_id converter_station2_id connected1 connected2
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            id
            HVDC1 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  10.0 300.0     400.0  1.0                  VSC1                  VSC2       True       True
            HVDC2 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  80.0 300.0     400.0  1.0                  LCC1                  LCC2       True       True
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
        Returns:
            a HVDC lines data frame
        """
        return self.get_elements(_pypowsybl.ElementType.HVDC_LINE)

    def get_switches(self) -> pd.DataFrame:
        """ Get switches as a ``Pandas`` data frame.
        Note:
            The resulting dataframe will have the following columns:

              - "kind": the kind of switch
              - "open": the open status of the switch
              - "retained": the retain status of the switch
              - "voltage_level_id": at which substation the switch is connected

            This dataframe is index by the name of the switches
        Examples
            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_switches()

            It outputs something like:

            ============================ ============ ====== ======== ================
                                       .         kind   open retained voltage_level_id
            ============================ ============ ====== ======== ================
            id
              S1VL1_BBS_LD1_DISCONNECTOR DISCONNECTOR  False    False            S1VL1
                       S1VL1_LD1_BREAKER      BREAKER  False     True            S1VL1
              S1VL1_BBS_TWT_DISCONNECTOR DISCONNECTOR  False    False            S1VL1
                       S1VL1_TWT_BREAKER      BREAKER  False     True            S1VL1
             S1VL2_BBS1_TWT_DISCONNECTOR DISCONNECTOR  False    False            S1VL2
             S1VL2_BBS2_TWT_DISCONNECTOR DISCONNECTOR   True    False            S1VL2
                       S1VL2_TWT_BREAKER      BREAKER  False     True            S1VL2
            S1VL2_BBS1_VSC1_DISCONNECTOR DISCONNECTOR   True    False            S1VL2
                                     ...          ...    ...      ...              ...
            ============================ ============ ====== ======== ================
        Returns:
            a switches data frame
        """
        return self.get_elements(_pypowsybl.ElementType.SWITCH)

    def get_ratio_tap_changer_steps(self) -> pd.DataFrame:
        """ Get ratio tap changer steps as a ``Pandas`` data frame.
        Note:
            The resulting dataframe will have the following columns:

              - "rho":
              - "r": the resistance of the ratio tap changer step (in Ohm)
              - "x": The reactance of the ratio tap changer step (Ohm)
              - "g": the conductance of the ratio tap changer step (in Siemens)
              - "b": the susceptance of the ratio tap changer step (in Siemens)
            This dataframe is index by the id of the transformer and the position of the ratio tap changer step
        Examples
            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changer_steps()

            It outputs something like:

            ========== ======== ======== === === === ===
                     .        .      rho   r   x   g   b
            ========== ======== ======== === === === ===
            id         position
            NHV2_NLOAD        0 0.850567 0.0 0.0 0.0 0.0
                              1 1.000667 0.0 0.0 0.0 0.0
                              2 1.150767 0.0 0.0 0.0 0.0
            ========== ======== ======== === === === ===

        Returns:
            a ratio tap changer steps data frame
        """
        return self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER_STEP)

    def get_phase_tap_changer_steps(self) -> pd.DataFrame:
        """ Get phase tap changer steps as a ``Pandas`` data frame.
        Note:
            The resulting dataframe will have the following columns:

              - "rho": the voltage ratio (in per unit)
              - "alpha": the angle difference (in degree)
              - "r": the resistance of the phase tap changer step (in Ohm)
              - "x": The reactance of the phase tap changer step (Ohm)
              - "g": the conductance of the phase tap changer step (in Siemens)
              - "b": the susceptance of the phase tap changer step (in Siemens)
            This dataframe is index by the id of the transformer and the position of the phase tap changer step
        Examples
            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changer_steps()

            It outputs something like:

            === ======== ==== ====== ========= ========= === ===
              .        .  rho  alpha         r         x   g   b
            === ======== ==== ====== ========= ========= === ===
            id  position
            TWT        0  1.0 -42.80 39.784730 29.784725 0.0 0.0
                       1  1.0 -40.18 31.720245 21.720242 0.0 0.0
                       2  1.0 -37.54 23.655737 13.655735 0.0 0.0
                       3  1.0 -34.90 16.263271  6.263268 0.0 0.0
                       4  1.0 -32.26  9.542847  4.542842 0.0 0.0
                       5  1.0 -29.60  3.494477  3.494477 0.0 0.0
                     ...  ...    ...       ...       ... ... ...
            === ======== ==== ====== ========= ========= === ===
        Returns:
            a phase tap changer steps data frame
        """
        return self.get_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER_STEP)

    def get_ratio_tap_changers(self) -> pd.DataFrame:
        """ Create a ratio tap changers``Pandas`` data frame.
        Note:
            The resulting dataframe will have the following columns:

              - "tap":
              - "low_tap":
              - "high_tap":
              - "step_count":
              - "on_load":
              - "regulating":
              - "target_v":
              - "target_deadband":
              - "regulationg_bus_id":
            This dataframe is index by the name of the transformer
        Examples
            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changers()

            It outputs something like:

            ========== === ======= ======== ========== ======= ========== ======== =============== =================
                     . tap low_tap high_tap step_count on_load regulating target_v target_deadband regulating_bus_id
            ========== === ======= ======== ========== ======= ========== ======== =============== =================
            id
            NHV2_NLOAD   1       0        2          3    True       True    158.0             0.0          VLLOAD_0
            ========== === ======= ======== ========== ======= ========== ======== =============== =================
        Returns:
            the ratio tap changers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER)

    def get_phase_tap_changers(self) -> pd.DataFrame:
        """ Create a phase tap changers``Pandas`` data frame.
        Note:
            The resulting dataframe will have the following columns:

              - "tap":
              - "low_tap":
              - "high_tap":
              - "step_count":
              - "regulating":
              - "regulation_mode":
              - "target_deadband":
              - "regulationg_bus_id":
            This dataframe is index by the name of the transformer
        Examples
            .. code-block:: python

                import pypowsybl as pypo
                net = pypo.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changers()

            It outputs something like:

            === === ======= ======== ========== ========== =============== ================ =============== =================
              . tap low_tap high_tap step_count regulating regulation_mode regulation_value target_deadband regulating_bus_id
            === === ======= ======== ========== ========== =============== ================ =============== =================
            id
            TWT  15       0       32         33      False       FIXED_TAP              NaN             NaN           S1VL1_0
            === === ======= ======== ========== ========== =============== ================ =============== =================
        Returns:
            the phase tap changers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER)

    def get_reactive_capability_curve_points(self) -> pd.DataFrame:
        """ Get reactive capability curve points as a ``Pandas`` data frame.

        Returns:
            a reactive capability curve points data frame
        """
        return self.get_elements(_pypowsybl.ElementType.REACTIVE_CAPABILITY_CURVE_POINT)

    def update_elements(self, element_type: _pypowsybl.ElementType, df: pd.DataFrame):
        """ Update network elements with a ``Pandas`` data frame for a specified element type.
        The data frame columns are mapped to IIDM element attributes and each row is mapped to an element using the
        index.

        Args:
            element_type (ElementType): the element type
            df (DataFrame): the ``Pandas`` data frame
        """
        for series_name in df.columns.values:
            series = df[series_name]
            series_type = _pypowsybl.get_series_type(element_type, series_name)
            if series_type == 2 or series_type == 3:
                _pypowsybl.update_network_elements_with_int_series(self._handle, element_type, series_name,
                                                                   df.index.values,
                                                                   series.values, len(series))
            elif series_type == 1:
                _pypowsybl.update_network_elements_with_double_series(self._handle, element_type, series_name,
                                                                      df.index.values,
                                                                      series.values, len(series))
            elif series_type == 0:
                _pypowsybl.update_network_elements_with_string_series(self._handle, element_type, series_name,
                                                                      df.index.values,
                                                                      series.values, len(series))
            else:
                raise PyPowsyblError(
                    f'Unsupported series type {series_type}, element type: {element_type}, series_name: {series_name}')

    def update_buses(self, df: pd.DataFrame):
        """ Update buses with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_buses()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "v_mag"
                - "v_angle"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.BUS, df)

    def update_switches(self, df: pd.DataFrame):
        """ Update switches with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_switches()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "open"
                - "retained"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.SWITCH, df)

    def update_generators(self, df: pd.DataFrame):
        """ Update generators with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_generators()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "target_p"
                - "max_p"
                - "min_p"
                - "target_v"
                - "target_q"
                - "voltage_regulator_on"
                - "p"
                - "q"
                - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.GENERATOR, df)

    def update_loads(self, df: pd.DataFrame):
        """ Update loads with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_loads()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "p0"
                - "q0"
                - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.LOAD, df)

    def update_batteries(self, df: pd.DataFrame):
        """ Update batteries with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_batteries()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "p0"
                - "q0"
                - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.BATTERY, df)

    def update_dangling_lines(self, df: pd.DataFrame):
        """ Update dangling lines with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_dangling_lines()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "r"
                - "x"
                - "g"
                - "b"
                - "p0"
                - "q0"
                - "p"
                - "q"
                - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.DANGLING_LINE, df)

    def update_vsc_converter_stations(self, df: pd.DataFrame):
        """ Update VSC converter stations with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_vsc_converter_stations()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
             columns that can be updated :
                - "voltage_setpoint"
                - "reactive_power_setpoint"
                - "voltage_regulator_on"
                - "p"
                - "q"
                - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION, df)

    def update_static_var_compensators(self, df: pd.DataFrame):
        """ Update static var compensators with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_static_var_compensators()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "voltage_setpoint"
                - "reactive_power_setpoint"
                - "regulation_mode"
                - "p"
                - "q"
                - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR, df)

    def update_hvdc_lines(self, df: pd.DataFrame):
        """ Update HVDC lines with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_hvdc_lines()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "converters_mode"
                - "active_power_setpoint"
                - "max_p"
                - "nominal_v"
                - "r"
                - "connected1"
                - "connected2"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.HVDC_LINE, df)

    def update_lines(self, df: pd.DataFrame):
        """ Update lines with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_lines()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "r"
                - "x"
                - "g1"
                - "b1"
                - "g2"
                - "b2"
                - "p1"
                - "q1"
                - "p2"
                - "q2"
                - "connected1"
                - "connected2"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.LINE, df)

    def update_2_windings_transformers(self, df: pd.DataFrame):
        """ Update 2 windings transformers with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_2_windings_transformers()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "r"
                - "x"
                - "g"
                - "b"
                - "rated_u1"
                - "rated_u2"
                - "rated_s"
                - "p1"
                - "q1"
                - "p2"
                - "q2"
                - "connected1"
                - "connected2"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER, df)

    def update_ratio_tap_changers(self, df: pd.DataFrame):
        """ Update ratio tap changers with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_ratio_tap_changers()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "tap"
                - "on_load"
                - "regulating"
                - "target_v"
                - "target_deadband"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER, df)

    def update_phase_tap_changers(self, df: pd.DataFrame):
        """ Update phase tap changers with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_phase_tap_changers()`
        Args:
            df (DataFrame): the ``Pandas`` data frame
            columns that can be updated :
                - "tap"
                - "regulating"
                - "regulation_mode"
                - "regulation_value"
                - "target_deadband"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER, df)

    def update_shunt_compensators(self, df: pd.DataFrame):
        """ Update shunt compensators with a ``Pandas`` data frame.
            This method updates element of :func:`~pypowsybl.network.get_shunt_compensators()`
        Args:
           df (DataFrame): the ``Pandas`` data frame
           columns that can be updated :
               - "section_count"
               - "p"
               - "q"
               - "connected"
        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR, df)

    def get_working_variant_id(self):
        """ The current working variant ID

        Returns:
            the id of the currently selected variant

        """
        return _pypowsybl.get_working_variant_id(self._handle)

    def clone_variant(self, src: str, target: str, may_overwrite=True):
        """ Creates a copy of the source variant

        Args:
            src: variant to copy
            target: id of the new variant that will be a copy of src
            may_overwrite: indicates if the target can be overwritten when it already exists
        """
        _pypowsybl.clone_variant(self._handle, src, target, may_overwrite)

    def set_working_variant(self, variant: str):
        """ Changes the working variant. The provided variant ID must correspond
        to an existing variant, for example created by a call to `clone_variant`.

        Args:
            variant: id of the variant selected (it must exist)
        """
        _pypowsybl.set_working_variant(self._handle, variant)

    def remove_variant(self, variant: str):
        """
        Removes a variant from the network.

        Args:
            variant: id of the variant to be deleted
        """
        _pypowsybl.remove_variant(self._handle, variant)

    def get_variant_ids(self):
        """
        Get the list of existing variant IDs.

        Returns:
            all the ids of the existing variants
        """
        return _pypowsybl.get_variant_ids(self._handle)


def create_empty(id: str = "Default") -> Network:
    """ Create an empty network.

    :param id: id of the network, defaults to 'Default'
    :type id: str, optional
    :return: an empty network
    :rtype: Network
    """
    return Network(_pypowsybl.create_empty_network(id))


def create_ieee9() -> Network:
    return Network(_pypowsybl.create_ieee_network(9))


def create_ieee14() -> Network:
    return Network(_pypowsybl.create_ieee_network(14))


def create_ieee30() -> Network:
    return Network(_pypowsybl.create_ieee_network(30))


def create_ieee57() -> Network:
    return Network(_pypowsybl.create_ieee_network(57))


def create_ieee118() -> Network:
    return Network(_pypowsybl.create_ieee_network(118))


def create_ieee300() -> Network:
    return Network(_pypowsybl.create_ieee_network(300))


def create_eurostag_tutorial_example1_network() -> Network:
    return Network(_pypowsybl.create_eurostag_tutorial_example1_network())


def _create_battery_network() -> Network:
    return Network(_pypowsybl.create_battery_network())


def _create_dangling_lines_network() -> Network:
    return Network(_pypowsybl.create_dangling_line_network())


def create_four_substations_node_breaker_network() -> Network:
    return Network(_pypowsybl.create_four_substations_node_breaker_network())


def get_import_formats() -> List[str]:
    """ Get list of supported import formats

    :return: the list of supported import formats
    :rtype: List[str]
    """
    return _pypowsybl.get_network_import_formats()


def get_export_formats() -> List[str]:
    """ Get list of supported export formats

    :return: the list of supported export formats
    :rtype: List[str]
    """
    return _pypowsybl.get_network_export_formats()


def get_import_parameters(format: str) -> pd.DataFrame:
    """ Get supported parameters infos for a given format

    :param format: the format
    :return: parameters infos
    :rtype: pd.DataFrame
    """
    series_array = _pypowsybl.create_importer_parameters_series_array(format)
    return create_data_frame_from_series_array(series_array)


def load(file: str, parameters: dict = {}) -> Network:
    """ Load a network from a file. File should be in a supported format.

    Args:
       file (str): a file
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    return Network(_pypowsybl.load_network(file, parameters))


def load_from_string(file_name: str, file_content: str, parameters: dict = {}) -> Network:
    """ Load a network from a string. File content should be in a supported format.

    Args:
       file_name (str): file name
       file_content (str): file content
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    return Network(_pypowsybl.load_network_from_string(file_name, file_content, parameters))
