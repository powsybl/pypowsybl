Per Unit data
-------------

PyPowSyBl provides methods to per unit the scientific data. They are part of the network api.
To per-unit the data, the attribute per_unit of the network has to be set.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> net = pp.network.create_four_substations_node_breaker_network()
    >>> net.per_unit=True
    >>> net.get_lines()
              name         r         x   g1   b1   g2   b2        p1        q1        i1        p2        q2        i2 voltage_level1_id voltage_level2_id  bus1_id  bus2_id  connected1  connected2
    id
    LINE_S2S3       0.000006  0.011938  0.0  0.0  0.0  0.0  1.098893  1.900229  2.147594 -1.098864 -1.845171  2.147594             S2VL1             S3VL1  S2VL1_0  S3VL1_0        True        True
    LINE_S3S4       0.000006  0.008188  0.0  0.0  0.0  0.0  2.400036  0.021751  2.400135 -2.400000  0.025415  2.400135             S3VL1             S4VL1  S3VL1_0  S4VL1_0        True        True

For example for lines r, x, g1, b1, g2, b2, p1, q1, i1, p2, q2, i2 are per united according to the nominal voltage and to the nominal apparent power.
The nominal apparent power is by default 100 MVA. It can be set like this :

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> net.nominal_apparent_power=250
    >>> net.get_lines()
              name         r         x   g1   b1   g2   b2        p1        q1        i1        p2        q2        i2 voltage_level1_id voltage_level2_id  bus1_id  bus2_id  connected1  connected2
    id
    LINE_S2S3       0.000016  0.029844  0.0  0.0  0.0  0.0  0.439557  0.760092  0.859038 -0.439546 -0.738068  0.859037             S2VL1             S3VL1  S2VL1_0  S3VL1_0        True        True
    LINE_S3S4       0.000016  0.020469  0.0  0.0  0.0  0.0  0.960014  0.008700  0.960054 -0.960000  0.010166  0.960054             S3VL1             S4VL1  S3VL1_0  S4VL1_0        True        True

Per Unit formula
----------------

#. Resistance R

for network elements with only one nominal voltage :

.. math:: \frac{S_n}{V_nominal^2} R

with Sn the nominal apparent power
For two winding transformers, the nominal voltage is the nominal voltage of the side 2
For lines, it is according to both sides :

.. math:: \frac{S_n}{V_{nominal1} V_{nominal2}} R

#. Reactance X

for network elements with only one nominal voltage :

.. math:: \frac{S_n}{V_nominal^2} X

with Sn the nominal apparent power
For two winding transformers, the nominal voltage is the nominal voltage of the side 2
For lines, it is according to both sides :

.. math:: \frac{S_n}{V_{nominal1} V_{nominal2}} X

#. Susceptance B

for network elements with only one nominal voltage :

.. math:: \frac{V_{nominal}^2}{S_n} B

with Sn the nominal apparent power
For two winding transformers, to compute B, the nominal voltage is the nominal voltage of the side 2
For lines, B is **on side one** according to both sides :

.. math:: \frac{V_{nom1}^2 B + (V_{nominal1} - V_{nominal2}) V_{nominal1} Im(Y)}{S_n}

where Y is the admittance (Y = 1/Z where Z is the impedance) and Im() the imaginary part

#. Conductance G

for network elements with only one nominal voltage :

.. math:: \frac{V_{nominal}^2}{S_n} G

with Sn the nominal apparent power
For two winding transformers, to compute G, the nominal voltage is the nominal voltage of the side 2
For lines, G is **on side one** according to both sides :

.. math:: \frac{V_{nom1}^2 G + (V_{nominal1} - V_{nominal2}) V_{nominal1} Re(Y)}{S_n}

where Y is the admittance (Y = 1/Z where Z is the impedance) and Re() the real part
for side 2 just inverse Vnominal1 and Vnominal2

#. Voltage V

.. math:: \frac{V}{V_{nominal}}

the voltage is perunit by the nominal voltage. For network element with a target voltage, it per united by the nominal voltage of the target element.

#. Active Power P

.. math:: \frac{P}{S_{n}}

with Sn the nominal apparent power

#. Reactive Power Q

.. math:: \frac{Q}{S_{n}}

with Sn the nominal apparent power

#. Electric Current I

.. math:: \frac{ \sqrt{3} V_{nominal}}{S_{n} 10^3} I

with Sn the nominal apparent power

#. Angle

the angle are in degrees in PyPowSyBl, but when per-united it is in radian while it is not really related to per-uniting.