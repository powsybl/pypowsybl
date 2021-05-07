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


Create a network elements data frame
------------------------------------

To get network elements data, we can create a ``Pandas`` data frame for a given type of element. Supported elements are
buses, lines, 2 windings transformers, 3 windings transformers, generators, loads, shunt compensators, dangling lines,
LCC and VSC converters stations, static var compensators, switches, voltage levels, substations, busbar sections,
HVDC lines, ratio and phase tap changer steps associated to a 2 windings transformers. Each element attribute is
mapped to a ``Pandas`` series (so a data frame column) and there is as many values in the series as there is elements
in the network.

This generic method can be called with the element type to get a data frame:

.. autofunction:: pypowsybl.network.Network.create_elements_data_frame
   :noindex:

Or for each element type, there is also a dedicated method, like for generators:

.. autofunction:: pypowsybl.network.Network.create_generators_data_frame
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_eurostag_tutorial_example1_network()
   >>> generators = n.create_generators_data_frame()
   >>> print(generators) # doctest: +NORMALIZE_WHITESPACE
       energy_source  target_p    max_p    min_p  target_v  target_q  voltage_regulator_on   p   q   bus_id
   id
   GEN         OTHER     607.0  9999.99 -9999.99      24.5     301.0                  True NaN NaN  VLGEN_0

Ratio and phase tap changer steps data frames have a multi-index based on the transformer ID and the step position to
be able to easily get steps related to just one transformer:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_eurostag_tutorial_example1_network()
   >>> steps = n.create_ratio_tap_changer_steps_data_frame()
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


Measwhile, you could update the network with a `DataFrame`.

Update one value for one equipement
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Example:

.. code-block:: python

   >>> df = pd.DataFrame(data=[500], columns=['p0'], index=['LOAD']
   >>> n.update_loads_with_data_frame(df)


Update multi values for one equipement
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Example:

.. code-block:: python

   >>> df = pd.DataFrame(data=[[500, 300]], columns=['p0','q0'], index=['LOAD']
   >>> n.update_loads_with_data_frame(df)

.. list-table:: Available equipement types and attributes
  :widths: 25 25 50
  :header-rows: 1

  * - equipement type
    - attribute(column)
    - value type
  * - Load
    - p0, q0
    - double
  * - Generator
    - target_p, target_q, target_v
    - double
