Network
=======


.. testsetup:: *

    import pandas as pd
    pd.set_option('display.max_columns', None)
    pd.set_option('display.expand_frame_repr', False)


Create an empty network
-----------------------

.. autofunction:: pypowsybl.network.create_empty
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_empty()

Load a network from a file
--------------------------

.. autofunction:: pypowsybl.network.get_import_formats
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> pp.network.get_import_formats()
   ['CGMES', 'MATPOWER', 'IEEE-CDF', 'PSS/E', 'UCTE', 'XIIDM']

.. autofunction:: pypowsybl.network.get_import_parameters
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> parameters = pp.network.get_import_parameters('PSS/E')
   >>> parameters.index.tolist()
   ['psse.import.ignore-base-voltage']
   >>> parameters['description']['psse.import.ignore-base-voltage']
   'Ignore base voltage specified in the file'
   >>> parameters['type']['psse.import.ignore-base-voltage']
   'BOOLEAN'
   >>> parameters['default']['psse.import.ignore-base-voltage']
   'false'

.. autofunction:: pypowsybl.network.load
   :noindex:

Example:

.. code-block:: python

   >>> pp.network.load('ieee14.raw', {'psse.import.ignore-base-voltage': 'true'})

Save a network to a file
------------------------

.. autofunction:: pypowsybl.network.get_export_formats
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> pp.network.get_export_formats()
   ['CGMES', 'UCTE', 'XIIDM', 'ADN']

.. autofunction:: pypowsybl.network.Network.dump
   :noindex:

Example:

.. code-block:: python

   >>> n = pp.network.create_ieee14()
   >>> n.dump('ieee14.xiidm')


Create and update network elements with a ``Pandas`` data frame
---------------------------------------------------------------

To get network elements data, we can create a ``Pandas`` data frame for a given type of element. Supported elements are
buses, lines, 2 windings transformers, 3 windings transformers, generators, loads, shunt compensators, dangling lines,
LCC and VSC converters stations, static var compensators, switches, voltage levels, substations, busbar sections,
HVDC lines, ratio and phase tap changer steps associated to a 2 windings transformers. Each element attribute is
mapped to a ``Pandas`` series (so a data frame column) and there is as many values in the series as there is elements
in the network.

This generic method can be called with the element type to get a data frame:

.. autofunction:: pypowsybl.network.Network.get_elements
   :noindex:

Or for each element type, there is also a dedicated method, like for generators:

.. autofunction:: pypowsybl.network.Network.get_generators
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_eurostag_tutorial_example1_network()
   >>> generators = n.get_generators()
   >>> print(generators) # doctest: +NORMALIZE_WHITESPACE
       energy_source  target_p    min_p    max_p          min_q          max_q  target_v  target_q  voltage_regulator_on   p   q   i  voltage_level_id   bus_id  connected
   id
   GEN         OTHER     607.0 -9999.99  9999.99  -9.999990e+03   9.999990e+03      24.5     301.0                  True NaN NaN NaN             VLGEN  VLGEN_0       True
   GEN2        OTHER     607.0 -9999.99  9999.99 -1.797693e+308  1.797693e+308      24.5     301.0                  True NaN NaN NaN             VLGEN  VLGEN_0       True
Ratio and phase tap changer steps data frames have a multi-index based on the transformer ID and the step position to
be able to easily get steps related to just one transformer:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_eurostag_tutorial_example1_network()
   >>> steps = n.get_ratio_tap_changer_steps()
   >>> print(steps) # doctest: +NORMALIZE_WHITESPACE
                             rho    r    x    g    b
   id         position
   NHV2_NLOAD 0         0.850567  0.0  0.0  0.0  0.0
              1         1.000667  0.0  0.0  0.0  0.0
              2         1.150767  0.0  0.0  0.0  0.0
   >>> transformer_step = steps.loc['NHV2_NLOAD']
   >>> print(transformer_step) # doctest: +NORMALIZE_WHITESPACE
                  rho    r    x    g    b
   position
   0         0.850567  0.0  0.0  0.0  0.0
   1         1.000667  0.0  0.0  0.0  0.0
   2         1.150767  0.0  0.0  0.0  0.0


We can also update network elements with a data frame:

.. code-block:: python

   >>> df = pd.DataFrame(data=[[500, 300]], columns=['p0','q0'], index=['LOAD']
   >>> n.update_loads(df)

.. list-table:: Available equipment types and attributes
  :widths: auto
  :header-rows: 1

  * - Equipment type
    - Attributes (columns)
  * - Load, Dangling line, Battery
    - p0 (double), q0 (double)
  * - Generator
    - voltage_regulator_on (bool), target_p (double), target_q (double), target_v (double)
  * - Switch
    - open (bool), retained (bool)
  * - VSC converter station
    - voltage_setpoint (double), reactive_power_setpoint (double),  voltage_regulator_on (bool)
  * - Static var compensator
    - voltage_setpoint (double), reactive_power_setpoint (double), regulation_mode (str)
  * - HVDC line
    - converters_mode (str), active_power_setpoint (double)
  * - Two windings transformer
    - ratio_tap_position (int), phase_tap_position (int)



Working with multiple variants of a network
-------------------------------------------

You may want to change the state of the network while keeping in memory its initial state.
In order to achieve that, you can use variants management:

.. doctest::
   :skipif: pp is None

   >>> network = pp.network.create_eurostag_tutorial_example1_network()
   >>> network.get_variant_ids()  # all networks initially have only one variant
   ['InitialState']
   >>> network.clone_variant('InitialState', 'Variant')  # creates a variant by cloning the initial one
   >>> network.get_variant_ids()  # all networks initially have only one variant
   ['InitialState', 'Variant']
   >>> network.set_working_variant('Variant')  # set our new variant as the working variant
   >>> network.update_generators(pd.DataFrame(index=['GEN'], columns=['target_p'], data=[[700]]))
   >>> network.get_generators()['target_p']['GEN']  # Our generator now produces 700 MW on this variant
   700.0
   >>> network.set_working_variant('InitialState')  # let's go back to our initial variant
   >>> network.get_generators()['target_p']['GEN']  # We still have our initial value, 600 MW
   607.0
   >>> network.remove_variant('Variant')

