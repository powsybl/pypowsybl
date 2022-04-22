The network model
=================

.. currentmodule:: pypowsybl.network

.. testsetup:: *

    import pandas as pd
    pd.set_option('display.max_columns', None)
    pd.set_option('display.expand_frame_repr', False)


The :class:`Network` object is the main data structure of pypowsybl.
It contains all the data of a power network : substations, generators, lines,
transformers, ...

pypowsybl provides methods to create networks, and to access and modify their data.


Create a network
----------------

pypowsybl provides several factory methods to create well known network models.
For example, you can create the IEEE 9-bus network case :

.. doctest::

    >>> network = pp.network.create_ieee9()

Another common way of creating a network is to load it from a file :

.. code-block:: python

    >>> network = pp.network.load('my-network.xiidm')

The supported formats are the following:

.. doctest::

   >>> pp.network.get_import_formats()
   ['CGMES', 'MATPOWER', 'IEEE-CDF', 'PSS/E', 'UCTE', 'XIIDM', 'POWER-FACTORY']

.. Note::

    Import formats may support specific parameters,
    which you can find by using :func:`get_import_parameters`.

    .. code-block:: python

       network = pp.network.load('ieee14.raw', {'psse.import.ignore-base-voltage': 'true'})


You may also create your own network from scratch, see below.


Save a network
--------------

Networks can be written to the filesystem, using one of the available export formats:

.. code-block:: python

   network.dump('network.xiidm', format='XIIDM')

You can also serialize networks to a string:

.. code-block:: python

   xiidm_str = network.dump_to_string('XIIDM')

The supported formats are:

.. doctest::

   >>> pp.network.get_export_formats()
   ['CGMES', 'PSS/E', 'UCTE', 'XIIDM']

.. Note::

    Export formats may support specific parameters,
    which you can find by using :func:`get_export_parameters`.

Reading network elements data
-----------------------------

All network elements data can be read as :class:`DataFrames <pandas.DataFrame>`.
Supported elements are:

 - buses
 - lines
 - 2 windings transformers
 - 3 windings transformers
 - generators
 - loads
 - shunt compensators
 - dangling lines
 - LCC and VSC converters stations
 - static var compensators
 - switches
 - voltage levels
 - substations
 - busbar sections
 - HVDC lines
 - ratio and phase tap changer steps associated to a 2 windings transformers

Each element of the network is mapped to one row of the dataframe, an each element attribute
is mapped to one column of the dataframe (a :class:`~pandas.Series`).

For example, you can retrieve generators data as follows:

.. doctest::

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> network.get_generators() # doctest: +NORMALIZE_WHITESPACE
         name energy_source  target_p    min_p   max_p          min_q          max_q reactive_limits_kind  target_v  target_q  voltage_regulator_on regulated_element_id   p   q   i voltage_level_id   bus_id  connected
    id
    GEN               OTHER     607.0 -9999.99  4999.0  -9.999990e+03   9.999990e+03              MIN_MAX      24.5     301.0                  True                      NaN NaN NaN            VLGEN  VLGEN_0       True
    GEN2              OTHER     607.0 -9999.99  4999.0 -1.797693e+308  1.797693e+308              MIN_MAX      24.5     301.0                  True                      NaN NaN NaN            VLGEN  VLGEN_0       True

Most dataframes are indexed on the ID of the elements.
However, some more complex dataframes have a multi-index : for example,
ratio and phase tap changer steps are indexed on their transformer ID together with
the step position:

.. doctest::

    >>> network.get_ratio_tap_changer_steps() # doctest: +NORMALIZE_WHITESPACE
                              rho    r    x    g    b
    id         position
    NHV2_NLOAD 0         0.850567  0.0  0.0  0.0  0.0
               1         1.000667  0.0  0.0  0.0  0.0
               2         1.150767  0.0  0.0  0.0  0.0

This allows to easily get steps related to just one transformer:

.. doctest::

    >>> network.get_ratio_tap_changer_steps().loc['NHV2_NLOAD'] # doctest: +NORMALIZE_WHITESPACE
                   rho    r    x    g    b
    position
    0         0.850567  0.0  0.0  0.0  0.0
    1         1.000667  0.0  0.0  0.0  0.0
    2         1.150767  0.0  0.0  0.0  0.0

For a detailed description of each dataframe, please refer
to the reference API :doc:`documentation </reference/network>`.

Updating network elements
-------------------------

Network elements can also be updated, using either simple values or list arguments,
or :class:`DataFrames <pandas.DataFrame>` for more advanced cases.
Not all attributes are candidates for update, for example element IDs cannot be
updated. For a detailed description of what attributes can be updated please refer
to the reference API :doc:`documentation </reference/network>`.

For example, to set the active power and reactive power of the load *LOAD*,
the 3 following forms are equivalent:

- simple values as named arguments:

.. code-block:: python

    >>> network.update_loads(id='LOAD', p0=500, q0=300)

- lists or any sequence type as named arguments. Obviously this will be more useful if you need to update
  multiple elements at once. You must provide sequences with the same length (here 1):

.. code-block:: python

    >>> network.update_loads(id=['LOAD'], p0=[500], q0=[300])

- a full dataframe. This may be useful if you want to use data manipulation features offered by pandas library:

.. doctest::

    >>> df = pd.DataFrame(index=['LOAD'], columns=['p0','q0'], data=[[500, 300]])
    >>> network.update_loads(df)

You can check that the load data has indeed been updated:

.. doctest::

    >>> network.get_loads()[['p0','q0']] # doctest: +NORMALIZE_WHITESPACE
             p0     q0
    id
    LOAD  500.0  300.0


Basic topology changes
----------------------

Most elements dataframes contain information about "is this element connected?" and "where is it connected?".
That information appears as the ``connected`` and ``bus_id`` columns :

.. doctest::

    >>> network.get_generators()[['connected','bus_id']] # doctest: +NORMALIZE_WHITESPACE
          connected   bus_id
    id
    GEN        True  VLGEN_0
    GEN2       True  VLGEN_0

You can disconnect or connect an element exactly the same way you update other attributes:

.. doctest::

    >>> network.update_generators(id='GEN', connected=False)
    >>> network.get_generators()[['connected','bus_id']] # doctest: +NORMALIZE_WHITESPACE
          connected   bus_id
    id
    GEN       False
    GEN2       True  VLGEN_0

You can see that the generator *GEN* has been disconnected from its bus.

Working with multiple variants
------------------------------

You may want to change the state of the network while keeping in memory its initial state.
In order to achieve that, you can use variants management:

After creation, a network has only one variant, called 'InitialState':

.. doctest::

   >>> network = pp.network.create_eurostag_tutorial_example1_network()
   >>> network.get_variant_ids()
   ['InitialState']

You can then add more variants by cloning an existing variant:

.. doctest::

   >>> network.clone_variant('InitialState', 'Variant')
   >>> network.get_variant_ids()
   ['InitialState', 'Variant']

You can then switch you "working" variant to the one you just created,
and perform some operations on it, for example changing the target power
of a generator to 700 MW:

.. doctest::

   >>> network.set_working_variant('Variant')
   >>> network.update_generators(id='GEN', target_p=700)
   >>> network.get_generators()['target_p']['GEN']
   700.0

If you switch back to the initial variant, you will see that
its state has not changed, our generator still produces 607 MW:

.. doctest::

   >>> network.set_working_variant('InitialState')
   >>> network.get_generators()['target_p']['GEN']
   607.0

Once you're done working with your variant, you can remove it:

.. doctest::

   >>> network.remove_variant('Variant')


Creating network elements
-------------------------

pypowsybl provides methods to add new elements (substations, lines, ...)
to the network. This enables you to adapt an existing network, or even to create
one from scratch.

As for updates, most creation methods accept arguments either as a dataframe
or as named argument.

Let's create our network!

.. testcode::

    network = pp.network.create_empty()

First, we need to create some substations, let's create 2 of them:

.. testcode::

    network.create_substations(id=['S1', 'S2'])

Then, let's add some voltage levels inside those substations, this time with a dataframe:

.. testcode::

    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'BUS_BREAKER', 'nominal_v': 400},
        {'substation_id': 'S2', 'id': 'VL2', 'topology_kind': 'BUS_BREAKER', 'nominal_v': 400},
    ])
    network.create_voltage_levels(voltage_levels)

Let's now create some buses inside those voltage levels:

.. testcode::

   network.create_buses(id=['B1', 'B2'], voltage_level_id=['VL1', 'VL2'])


Let's connect thoses buses with a line:

.. testcode::

    network.create_lines(id='LINE', voltage_level1_id='VL1', bus1_id='B1',
                         voltage_level2_id='VL2', bus2_id='B2',
                         b1=0, b2=0, g1=0, g2=0, r=0.5, x=10)

Finally, let's add a load, and a generator to feed it through our line:

.. testcode::

    network.create_loads(id='LOAD', voltage_level_id='VL2', bus_id='B2', p0=100, q0=10)
    network.create_generators(id='GEN', voltage_level_id='VL1', bus_id='B1',
                              min_p=0, max_p=200, target_p=100,
                              voltage_regulator_on=True, target_v=400)


You can now run a loadflow to check our network actually works !

.. doctest::

    >>> import pypowsybl.loadflow as lf
    >>> res = lf.run_ac(network)
    >>> str(res[0].status)
    'ComponentStatus.CONVERGED'

For more details and examples about network elements creations,
please refer to the API reference :doc:`documentation </reference/network>`.