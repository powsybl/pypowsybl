Network
=======

Create an empty network
***********************

.. testsetup::
   :skipif: pp is None

   import pypowsybl.network

.. autofunction:: pypowsybl.network.create_empty
   :noindex:

Example:

.. doctest::
   :skipif: pp is None

   >>> n = pp.network.create_empty()

Load a network from a file
**************************

