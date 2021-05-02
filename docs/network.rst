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
   >>> n.dump('/tmp/ieee14.xiidm')
