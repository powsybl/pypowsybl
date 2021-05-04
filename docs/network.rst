Network
=======

Create an empty network
***********************

.. autofunction:: pypowsybl.network.create_empty
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_empty()

Load a network from a file
**************************

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
**************************

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

Retrieve and modify network via dataframe
****************************************************

Loads
---------------

.. autofunction:: pypowsybl.network.Network.create_loads_data_frame
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_eurostag_tutorial_example1_network()
   >>> n.create_loads_data_frame()
              type     p0     q0   p   q    bus_id
   LOAD  UNDEFINED  600.0  200.0 NaN NaN  VLLOAD_0


.. autofunction:: pypowsybl.network.Network.update_loads_with_data_frame
  :noindex:

Example:

.. code-block:: python

   >>> df = pd.DataFrame(data=[[500, 300]], columns=['p0','q0'], index=['LOAD']
   >>> n.update_loads_with_data_frame(df)
