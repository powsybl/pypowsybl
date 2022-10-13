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
   ['CGMES', 'MATPOWER', 'PSS/E', 'UCTE', 'XIIDM']

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
    GEN               OTHER     607.0 -9999.99  4999.0  -9.999990e+03   9.999990e+03              MIN_MAX      24.5     301.0                  True                  GEN NaN NaN NaN            VLGEN  VLGEN_0       True
    GEN2              OTHER     607.0 -9999.99  4999.0 -1.797693e+308  1.797693e+308              MIN_MAX      24.5     301.0                  True                 GEN2 NaN NaN NaN            VLGEN  VLGEN_0       True

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


Creating node/breaker groups of elements
----------------------------------------

Pypowsybl provides ready for use methods to create a network with voltage levels in node/breaker topology from scratch or to modify an existing network
in node/breaker topology. Before describing these methods, we are going to detailed how to do with elementary methods. Let's see how it works by creating a network from scratch. First, you need to create an empty network:

.. testcode::

    n = pp.network.create_empty()

Then you can add substations on the network. Below, two substations are added from a dataframe, called *S1* and *S2*.

.. testcode::

    stations = pd.DataFrame.from_records(index='id', data=[
       {'id': 'S1'},
       {'id': 'S2'}
    ])
    n.create_substations(stations)

On each of these substations, you can create a node/breaker voltage level, here called *VL1* and *VL2*:

.. testcode::

    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL1', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 225},
        {'substation_id': 'S2', 'id': 'VL2', 'topology_kind': 'NODE_BREAKER', 'nominal_v': 225},
    ])
    n.create_voltage_levels(voltage_levels)

Now, you can create busbar sections on every voltage level. Here, we create three busbar sections in voltage level
*VL1* and one busbar section in voltage level *VL2*.

.. testcode::

    busbars = pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'BBS1', 'node': 0},
        {'voltage_level_id': 'VL1', 'id': 'BBS2', 'node': 1},
        {'voltage_level_id': 'VL1', 'id': 'BBS3', 'node': 2},
        {'voltage_level_id': 'VL2', 'id': 'BBS4', 'node': 0},
    ])
    n.create_busbar_sections(busbars)

You can draw single line diagrams for both the voltage levels to check that the busbar sections were added
correctly. For more information about single line diagrams, check the :doc:`related documentation </user_guide/network_visualization>`.

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/test_network_vl1_only_bbs_without_extensions.svg

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/test_network_vl2_only_bbs.svg

As you can see on the diagram of *VL1*, the busbar sections are not positioned in any chosen way. It is possible to add position extensions on these busbar sections to precise relative positions. Use *busbarSectionPosition* extension for that purpose. If you want to put the three busbar sections of *VL1* on the same slice, then they need to have the same *section_index*. As they belong to three distinct busbars, their *busbar_index* are different:

.. testcode::

    n.create_extensions('busbarSectionPosition', id='BBS1', busbar_index=1, section_index=1)
    n.create_extensions('busbarSectionPosition', id='BBS2', busbar_index=2, section_index=1)
    n.create_extensions('busbarSectionPosition', id='BBS3', busbar_index=3, section_index=1)
    n.create_extensions('busbarSectionPosition', id='BBS4', busbar_index=1, section_index=1)

You can draw the single line diagram of *VL1* again to check that the busbar sections are at right positions.

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/test_network_vl1_parallel_bbs.svg

Now let's connect a load in a close to reality way. The first thing to do is to create switches. To connect a load on *VL1*, you need to add a disconnector
on each busbar sections of a slice, two open and one closed, and a breaker between the disconnectors and the load.
You can add the switches with (note that the switch on *BBS1* is closed):

.. testcode::

    n.create_switches(pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'DISC-BBS1-LOAD', 'kind': 'DISCONNECTOR', 'node1': 0, 'node2': 3, 'open':True},
        {'voltage_level_id': 'VL1', 'id': 'DISC-BBS2-LOAD', 'kind': 'DISCONNECTOR', 'node1': 1, 'node2': 3, 'open':False},
        {'voltage_level_id': 'VL1', 'id': 'DISC-BBS3-LOAD', 'kind': 'DISCONNECTOR', 'node1': 2, 'node2': 3, 'open':True},
        {'voltage_level_id': 'VL1', 'id': 'BREAKER-BBS1-LOAD', 'kind': 'BREAKER', 'node1': 3, 'node2': 4, 'open':False},
    ]))

Now you can add the load on node 4:

.. testcode::

    n.create_loads(id='load1', voltage_level_id='VL1', node=4, p0=100, q0=10)

Now let's add a line between *VL1* and *VL2*. You need to add the switches first, three disconnectors and one breaker on *VL1*
and one closed disconnector and one breaker on *VL2*:

.. testcode::

    n.create_switches(pd.DataFrame.from_records(index='id', data=[
        {'voltage_level_id': 'VL1', 'id': 'DISC-BBS1-LINE', 'kind': 'DISCONNECTOR', 'node1': 0, 'node2': 5, 'open':False},
        {'voltage_level_id': 'VL1', 'id': 'DISC-BBS2-LINE', 'kind': 'DISCONNECTOR', 'node1': 1, 'node2': 5, 'open':True},
        {'voltage_level_id': 'VL1', 'id': 'DISC-BBS3-LINE', 'kind': 'DISCONNECTOR', 'node1': 2, 'node2': 5, 'open':True},
        {'voltage_level_id': 'VL2', 'id': 'DISC-BBS4-LINE', 'kind': 'DISCONNECTOR', 'node1': 0, 'node2': 1, 'open':False},
        {'voltage_level_id': 'VL1', 'id': 'BREAKER-BBS1-LINE', 'kind': 'BREAKER', 'node1': 5, 'node2': 6, 'open':False},
        {'voltage_level_id': 'VL2', 'id': 'BREAKER-BBS4-LINE', 'kind': 'BREAKER', 'node1': 1, 'node2': 2, 'open':False},
    ]))

Then you can add the line with:

.. testcode::

    n.create_lines(id='line1', voltage_level1_id='VL1', voltage_level2_id='VL2', node1=6, node2=2, r=0.1, x=1.0, g1=0, b1=1e-6, g2=0, b2=1e-6)

Now you can draw the single line diagrams of *VL1* and *VL2* to check that the load and the line have been correctly added:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/test_network_vl1_before_adding_extensions.svg

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/test_network_vl2_before_adding_extensions.svg

Here, similarly to busbar sections, the load and the line are randomly localized on the diagram.
You can add extensions on the line and on the load to specify where they are localized in the busbar sections and if they must be 
drawn on the top or on the bottom. We choose that the load *load1* is the first feeder of the busbar
section and on the bottom of *VL1*. The line *line1* is the second one and directed to the top on *VL1*. On *VL2*, the line
is also on top. The relative position between the load and the line is specified with the order in the position
extensions. The order of the load must be lower than the order of the line. You can use orders than do not follow each
other to be able to add feeder later on.

.. testcode::

    n.create_extensions('position', id="load1", side="", order=10, feeder_name='load1', direction='BOTTOM')
    n.create_extensions('position', id=["line1", "line1"], side=['ONE', 'TWO'], order= [20, 10], feeder_name=['line1VL1', 'line1VL2'], direction=['TOP', 'TOP'])

Now you can draw the single line diagrams for both voltage levels again and see that the line and the load are now
correctly positioned.

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/test_network_vl1_after_adding_extensions.svg

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/test_network_vl2_after_adding_extensions.svg

Done but fastidious! That is why Pypowsybl provides ready-for-use methods to create an injection and its bay with a single line.
The switches are created implicitly. The methods take a busbar section on which the disconnector is
closed as an argument (note that switches on the other parallel busbar sections are open). You also need to fill the position of the injection
as well as its characteristics. Optionnally, you can indicate the direction of the injection drawing - by default, on the bottom -,
if an exception should be raised in case of problem - by default, False - and a reporter to get logs.

You can add a load and connect it to *BBS3* between the line and the load1 (order position between 10 and 20) with:

.. testcode::

    pp.network.create_load_bay(n, id="load2", p0=10.0, q0=3.0, busbar_section_id='BBS3', position_order=15)

You can check that the load was added correctly by drawing a single line diagram of *VL1*:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/test_network_vl1_after_adding_load.svg

Now let's connect a generator on *BBS1* on the left of *load1*, a
dangling line on the right of *line1* on *BBS3* and a shunt and a VSC converter station on *BBS4*:

.. testcode::

    pp.network.create_generator_bay(n, id='generator1', max_p=1000, min_p=0, voltage_regulator_on=True,
                               target_p=100, target_q=150, target_v=225, busbar_section_id='BBS1',
                               position_order=5)
    pp.network.create_dangling_line_bay(n, id='dangling_line1', p0=100, q0=150, r=2, x=2, g=1, b=1, position_order=30, busbar_section_id='BBS3', direction='TOP')
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'busbar_section_id', 'position_order'],
        data=[('shunt1', 'LINEAR', 1, 221, 2, 'BBS4', 20)])
    model_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'g_per_section', 'b_per_section', 'max_section_count'],
        data=[('shunt1', 0.014, 0.0001, 2)])
    pp.network.create_shunt_compensator_bay(n, shunt_df=shunt_df, linear_model_df=model_df)
    pp.network.create_vsc_converter_station_bay(n, id='VSC1', target_q=200, voltage_regulator_on=True, loss_factor=1.0, target_v=230, busbar_section_id='BBS4', position_order=30)

You can draw the new single line diagrams:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/test_network_vl1_after_adding_everything.svg

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/test_network_vl2_after_adding_everything.svg

These methods exist for every type of injections.
To see them all, please refer to the reference API :doc:`documentation </reference/network>`.
