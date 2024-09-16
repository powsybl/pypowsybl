The network model
=================

.. currentmodule:: pypowsybl.network

The :class:`Network` object is the main data structure of pypowsybl.
It contains all the data of a power network: substations, generators, lines,
transformers, ...

pypowsybl provides methods to create networks, and to access and modify their data.


Create a network
----------------

pypowsybl provides several factory methods to create well known network models.
For example, you can create the IEEE 9-bus network case:

.. doctest::

    >>> network = pp.network.create_ieee9()

Another common way of creating a network is to load it from a file:

.. code-block:: python

    >>> network = pp.network.load('my-network.xiidm')

The supported formats are the following:

.. doctest::

   >>> pp.network.get_import_formats()
   ['BIIDM', 'CGMES', 'IEEE-CDF', 'JIIDM', 'MATPOWER', 'POWER-FACTORY', 'PSS/E', 'UCTE', 'XIIDM']

.. Note::

    Import formats may support specific parameters,
    which you can find by using :func:`get_import_parameters`.

    .. code-block:: python

       network = pp.network.load('ieee14.raw', {'psse.import.ignore-base-voltage': 'true'})


Loading a network from a binary byte buffer (io.BytesIO) is possible.
Only zipped network loading are supported for now, but inside the zip file the supported network format are the same as pp.network.load.

.. doctest::

        with open('battery_xiidm.zip', "rb") as fh:
            n = pp.network.load_from_binary_buffer(io.BytesIO(fh.read()))

You may also create your own network from scratch, see below.


Save a network
--------------

Networks can be written to the filesystem, using one of the available export formats:

.. code-block:: python

   network.save('network.xiidm', format='XIIDM')

You can also serialize networks to a string:

.. code-block:: python

   xiidm_str = network.save_to_string('XIIDM')

And also to a zip file as a (io.BytesIO) binary buffer.

.. code-block:: python

   zipped_xiidm = network.save_to_binary_buffer('XIIDM')

The supported formats are:

.. doctest::

   >>> pp.network.get_export_formats()
   ['AMPL', 'BIIDM', 'CGMES', 'JIIDM', 'MATPOWER', 'PSS/E', 'UCTE', 'XIIDM']

.. Note::

    Export formats may support specific parameters,
    which you can find by using :func:`get_export_parameters`.

Reading network elements data
-----------------------------

All network elements data can be read as :class:`DataFrames <pandas.DataFrame>`.
Supported elements are:

 - buses (from bus view)
 - buses from bus/breaker view
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
 - identifiables that are all the equipment on the network
 - injections
 - branches (lines and two windings transformers)
 - terminals are a practical view of those objects which are very important in the java implementation

Each element of the network is mapped to one row of the dataframe, an each element attribute
is mapped to one column of the dataframe (a :class:`~pandas.Series`).

For example, you can retrieve generators data as follows:

.. doctest::

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> network.get_generators() # doctest: +NORMALIZE_WHITESPACE
         name energy_source  target_p    min_p   max_p          min_q          max_q  rated_s reactive_limits_kind  target_v  target_q  voltage_regulator_on regulated_element_id   p   q   i voltage_level_id   bus_id  connected
    id
    GEN               OTHER     607.0 -9999.99  4999.0  -9.999990e+03   9.999990e+03      NaN              MIN_MAX      24.5     301.0                  True                  GEN NaN NaN NaN            VLGEN  VLGEN_0       True
    GEN2              OTHER     607.0 -9999.99  4999.0 -1.797693e+308  1.797693e+308      NaN              MIN_MAX      24.5     301.0                  True                 GEN2 NaN NaN NaN            VLGEN  VLGEN_0       True

Most dataframes are indexed on the ID of the elements.
However, some more complex dataframes have a multi-index: for example,
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
That information appears as the ``connected`` and ``bus_id`` columns:

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
   >>> network.get_generators()['target_p']['GEN'].item()
   700.0

If you switch back to the initial variant, you will see that
its state has not changed, our generator still produces 607 MW:

.. doctest::

   >>> network.set_working_variant('InitialState')
   >>> network.get_generators()['target_p']['GEN'].item()
   607.0

Once you're done working with your variant, you can remove it:

.. doctest::

   >>> network.remove_variant('Variant')


Create network elements
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


Let's connect these buses with a line:

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

Now let's see how to add a three-winding transformer to the network. First, let's add two voltage levels and their associated buses to the substation `S1`.

.. testcode::

    voltage_levels = pd.DataFrame.from_records(index='id', data=[
        {'substation_id': 'S1', 'id': 'VL3', 'topology_kind': 'BUS_BREAKER', 'nominal_v': 225},
        {'substation_id': 'S1', 'id': 'VL4', 'topology_kind': 'BUS_BREAKER', 'nominal_v': 90},
    ])
    network.create_voltage_levels(voltage_levels)
    network.create_buses(id=['B3', 'B4'], voltage_level_id=['VL3', 'VL4'])

Now let's add a three-winding transformer between VL1, VL2 and VL3:

.. testcode::

    network.create_3_windings_transformers(id='T1', rated_u0 = 225, voltage_level1_id='VL1', bus1_id='B1',
                                           voltage_level2_id='VL3', bus2_id='B3',
                                           voltage_level3_id='VL4', bus3_id='B4',
                                           b1=1e-6, g1=1e-6, r1=0.5, x1=10, rated_u1=400,
                                           b2=1e-6, g2=1e-6, r2=0.5, x2=10, rated_u2=225,
                                           b3=1e-6, g3=1e-6, r3=0.5, x3=10, rated_u3=90)

You can add a ratio tap changer on the leg 1 of the three-winding transformer with:

.. testcode::

    rtc_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'target_deadband', 'target_v', 'on_load', 'low_tap', 'tap', 'side'],
        data=[('T1', 2, 200, False, 0, 1, 'ONE')])
    steps_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'b', 'g', 'r', 'x', 'rho'],
        data=[('T1', 2, 2, 1, 1, 0.5),
              ('T1', 2, 2, 1, 1, 0.5),
              ('T1', 2, 2, 1, 1, 0.8)])
    network.create_ratio_tap_changers(rtc_df, steps_df)

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

.. image:: ../_static/images/node_breaker_network/test_network_vl1_only_bbs_without_extensions.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_only_bbs.svg
   :class: forced-white-background

As you can see on the diagram of *VL1*, the busbar sections are not positioned in any chosen way. It is possible to add position extensions on these busbar sections to precise relative positions. Use *busbarSectionPosition* extension for that purpose. If you want to put the three busbar sections of *VL1* on the same slice, then they need to have the same *section_index*. As they belong to three distinct busbars, their *busbar_index* are different:

.. testcode::

    n.create_extensions('busbarSectionPosition', id='BBS1', busbar_index=1, section_index=1)
    n.create_extensions('busbarSectionPosition', id='BBS2', busbar_index=2, section_index=1)
    n.create_extensions('busbarSectionPosition', id='BBS3', busbar_index=3, section_index=1)
    n.create_extensions('busbarSectionPosition', id='BBS4', busbar_index=1, section_index=1)

You can draw the single line diagram of *VL1* again to check that the busbar sections are at right positions.

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_parallel_bbs.svg
   :class: forced-white-background

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

.. image:: ../_static/images/node_breaker_network/test_network_vl1_before_adding_extensions.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_before_adding_extensions.svg
   :class: forced-white-background

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

.. image:: ../_static/images/node_breaker_network/test_network_vl1_after_adding_extensions.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_after_adding_extensions.svg
   :class: forced-white-background

Done but fastidious! That is why Pypowsybl provides ready-for-use methods to create an equipment and its bay with a single line.
The switches are created implicitly. The methods take a busbar section on which the disconnector is
closed as an argument (note that switches on the other parallel busbar sections are open). You also need to fill the position of the equipment on the voltage level
as well as its characteristics. Optionally, you can indicate the direction of the equipment drawing - by default, on the bottom for injections and on top for lines and two windings transformers -,
if an exception should be raised in case of problem - by default, True - and a report node to get logs.

You can add a load and connect it to *BBS3* between the line and the load1 (order position between 10 and 20) with:

.. testcode::

    pp.network.create_load_bay(n, id="load2", p0=10.0, q0=3.0, bus_or_busbar_section_id='BBS3', position_order=15)

You can check that the load was added correctly by drawing a single line diagram of *VL1*:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_after_adding_load.svg
   :class: forced-white-background

Now let's connect a generator on *BBS1* on the left of *load1*, a
dangling line on the right of *line1* on *BBS3* and a shunt on *BBS4*:

.. testcode::

    pp.network.create_generator_bay(n, id='generator1', max_p=1000, min_p=0, voltage_regulator_on=True,
                               target_p=100, target_q=150, target_v=225, bus_or_busbar_section_id='BBS1',
                               position_order=5)
    pp.network.create_dangling_line_bay(n, id='dangling_line1', p0=100, q0=150, r=2, x=2, g=1, b=1, position_order=30, bus_or_busbar_section_id='BBS3', direction='TOP')
    shunt_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'model_type', 'section_count', 'target_v',
                 'target_deadband', 'bus_or_busbar_section_id', 'position_order'],
        data=[('shunt1', 'LINEAR', 1, 221, 2, 'BBS4', 20)])
    model_df = pd.DataFrame.from_records(
        index='id',
        columns=['id', 'g_per_section', 'b_per_section', 'max_section_count'],
        data=[('shunt1', 0.014, 0.0001, 2)])
    pp.network.create_shunt_compensator_bay(n, shunt_df=shunt_df, linear_model_df=model_df)

You can draw the new single line diagrams:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_after_adding_everything.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_after_adding_everything.svg
   :class: forced-white-background

These methods exist for every type of injections and also work with bus/breaker voltage levels. Then the injection is simply
added to the given bus.

You can also add lines and two-windings transformers to a network with simple functions that create automatically the topology on both sides.
For example, you can add a line and connect it between **BBS3** and **BBS4**, right next to line1 (order positions 25 on VL1 and 15 on VL2) with:

.. testcode::

    pp.network.create_line_bays(n, id='line2', r=0.1, x=10, g1=0, b1=0, g2=0, b2=0,
                                            bus_or_busbar_section_id_1='BBS3',
                                            position_order_1=25,
                                            direction_1='TOP',
                                            bus_or_busbar_section_id_2='BBS4',
                                            position_order_2=15,
                                            direction_2='TOP')

Here the direction of the line is specified in the argument, but it is optional and by default TOP.
You can draw the single line diagrams of both voltage levels to check that the line was added correctly:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_with_new_line2.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_with_new_line2.svg
   :class: forced-white-background

Now let's see how to add a two windings transformer to our network. Both voltage levels VL1 and VL2 have a nominal voltage of 225kV.
First, you need a new voltage level VL3, let's say of nominal voltage 63kV, to connect to one of the existing voltage level. Both voltage levels connected through a two-windings
transformer must be in the same substation, let's pick VL1. You can create VL3 with:

.. testcode::

    n.create_voltage_levels(substation_id='S1', id='VL3', topology_kind='NODE_BREAKER', nominal_v=63)

Now voltage level VL3 is created, but it is empty. The next step is to create the topology of the voltage level, i.e. the busbar sections and disconnectors or switches.
You can use the same methods as to create VL1 and VL2, or use the built-in function allowing to create in one line the topology of symmetrical voltage levels.
Let's create the topology with two busbar sections and three sections with breakers between sections one and two
and disconnectors between sections two and three. You can do that with:

.. testcode::

    pp.network.create_voltage_level_topology(n, id='VL3', aligned_buses_or_busbar_count=2, switch_kinds='BREAKER, DISCONNECTOR')

To check that the topology was correctly created, you can draw the single line diagram of voltage level VL3:

.. code-block:: python

    >>> n.get_single_line_diagram('VL3')

.. image:: ../_static/images/node_breaker_network/test_network_vl3_only_bbs.svg
   :class: forced-white-background

Now you can add some coupling devices between each section of the new voltage level. For that, you can use the built-in method and you just
have to specify the two busbar sections on which the switches should be closed. Open switches will automatically be created on the parallel busbar
sections:

.. testcode::

    pp.network.create_coupling_device(n, bus_or_busbar_section_id_1=['VL3_1_1', 'VL3_2_2'], bus_or_busbar_section_id_2=['VL3_1_2', 'VL3_2_3'])

You can create the single line diagram to check that the coupling devices were well created:

.. code-block:: python

    >>> n.get_single_line_diagram('VL3')

.. image:: ../_static/images/node_breaker_network/test_network_vl3_with_coupling_device.svg
   :class: forced-white-background

Now, you can create a two windings transformer between VL1 and VL3. The features of the transformer must be specified, as well
as the busbar sections on which it should be connected. You can connect it to BBS1 and to VL3_1_1. The position wanted for the
transformer must be given for both ends and it is possible to indicate the direction for the diagram:

.. testcode::

    pp.network.create_2_windings_transformer_bays(n, id='two_windings_transformer', b=1e-6, g=1e-6, r=0.5, x=10, rated_u1=225, rated_u2=63,
                bus_or_busbar_section_id_1='BBS1', position_order_1=35, direction_1='BOTTOM',
                bus_or_busbar_section_id_2='VL3_1_1', position_order_2=5, direction_2='TOP')

Let's draw the single line diagrams of VL1 and of VL3 to check that the two windings transformer is where it should be:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_with_transformer.svg
   :class: forced-white-background


.. code-block:: python

    >>> n.get_single_line_diagram('VL3')

.. image:: ../_static/images/node_breaker_network/test_network_vl3_with_transformer.svg
   :class: forced-white-background

To add a HVDC line to the network, you can first add the two converter stations, just like any other injection.
Let's add one on the busbar section BBS3 of VL1 on the right and one on BBS4 on the right too:

.. testcode::

    pp.network.create_vsc_converter_station_bay(n, id=['VSC1', 'VSC2'], target_q=[200, 200], voltage_regulator_on=[True, True], loss_factor=[1.0, 1.0],
                target_v=[230, 230], bus_or_busbar_section_id=['BBS3', 'BBS4'], position_order=[30, 40])

Now you can add the HVDC line with:

.. testcode::

    n.create_hvdc_lines(id='HVDC_line', converter_station1_id='VSC1', converter_station2_id='VSC2',
                          r=1.0, nominal_v=400, converters_mode='SIDE_1_RECTIFIER_SIDE_2_INVERTER',
                          max_p=1000, target_p=800)

The single line diagrams of voltage levels VL1 and VL2 are now:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_with_hvdc.svg
   :class: forced-white-background


.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_with_hvdc.svg
   :class: forced-white-background

Now you know how to create a node-breaker voltage level and its topology, injections, lines and two-windings transformer with the built-in methods
available in pypowsybl. For a reference of the available methods, please refer to: :doc:`documentation </reference/network>`.

Remove groups of elements
-------------------------
PyPowsybl provides build-in methods to remove existing elements such as feeders, voltage levels and HVDC lines.
With these methods, it is possible to easily remove injections, lines and two windings transformers as well as the switches connecting them to a voltage level.
Let's work on the network created in the section above.

You can remove the load1 with:

.. testcode::

    pp.network.remove_feeder_bays(n, 'load1')

The single line diagram of VL1 is then:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_without_load1.svg
   :class: forced-white-background

You can see that the load was removed, as well as all the breaker and disconnectors that was connecting it to the busbar section.

If you want to remove a HVDC line, you can use the built-in method that will remove not only the line but also the two converting stations and their switches.

You can remove HVDC_line with:

.. testcode::

    pp.network.remove_hvdc_lines(n, 'HVDC_line')

You can check on the single line diagram that everything went good:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_without_hvdc.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL2')

.. image:: ../_static/images/node_breaker_network/test_network_vl2_without_hvdc.svg
   :class: forced-white-background

Finally, it is also possible to remove a full voltage level, with all its connectables. The lines and two windings transformers will be removed
as well as their topology on both sides and the HVDC lines will be removed as well as their converter stations on both sides too.

For example, you can remove VL2 with:

.. testcode::

    pp.network.remove_voltage_levels(n, 'VL2')

The remaining voltage levels VL1 and VL3 are then:

.. code-block:: python

    >>> n.get_single_line_diagram('VL1')

.. image:: ../_static/images/node_breaker_network/test_network_vl1_after_removing_vl2.svg
   :class: forced-white-background

.. code-block:: python

    >>> n.get_single_line_diagram('VL3')

.. image:: ../_static/images/node_breaker_network/test_network_vl3_with_transformer.svg
   :class: forced-white-background

On the diagrams, you can see that all the lines that were connecting VL1 to VL2 have been removed as well as their switches. On VL3, nothing was done as nothing was connected between VL2 and VL3.

Network merging
---------------

Pypowsybl provides methods to merge and detach networks. For example we can merge the 2 CGMES microgrids like this:

.. doctest::

    >>> be = pp.network.create_micro_grid_be_network()
    >>> nl = pp.network.create_micro_grid_nl_network()
    >>> be.merge(nl)

After the merge BE network has absorbed the NL network. So NL network is empty and BE network contains all the equipments
that where in BE network before.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> nl.get_substations()
    Empty DataFrame
    Columns: [name, TSO, geo_tags, country]
    Index: []

    >>> be.get_substations()
                                                  name TSO         geo_tags country
    id
    37e14a0f-5e34-4647-a062-8bfd9305fa9d   PP_Brussels        ELIA-Brussels      BE
    87f7002b-056f-4a6a-a872-1744eea757e3        Anvers          ELIA-Anvers      BE
    c49942d6-8b01-4b01-b5e8-f1180f84906c  PP_Amsterdam      TENNET TSO B.V.      NL

Once merged, we can still keep track of original networks. They are called "sub networks" and can be listed from their
parent networks like this:

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> be.get_sub_networks()
    Empty DataFrame
    Columns: []
    Index: [urn:uuid:d400c631-75a0-4c30-8aed-832b0d282e73, urn:uuid:77b55f87-fc1e-4046-9599-6c6b4f991a86]

We can see that the sub network dataframe has not yet columns and contains two rows corresponding to the 2 BE and NL
sub networks.
We can also get a sub network from its parent network and then only work and focus on a particular sub network. When we
get substations from the NL sub network we obviously only get one substation like in the original non merged network.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> nl_sub = be.get_sub_network('urn:uuid:77b55f87-fc1e-4046-9599-6c6b4f991a86')
    >>> nl_sub.get_substations()
                                                  name TSO         geo_tags country
    id
    c49942d6-8b01-4b01-b5e8-f1180f84906c  PP_Amsterdam      TENNET TSO B.V.      NL

Original networks can be recovered thanks to the "detach" method. In that case the sub network is removed from its
parent network and become again a standalone network.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> nl_sub.detach()

Reducing a network
------------------

Pypowsybl provides methods to reduce a network to a smaller one. It can be done with different parameters.
It can be decided according to the voltage with the parameters v_min and v_max. It can also be by indicating the
Voltage Levels that will be kept and also indicating the depth around these voltage levels.

For this example we will keep only voltage levels with voltage superior or equal to 400 kV

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> net = pp.network.create_four_substations_node_breaker_network()
    >>> net.get_voltage_levels()
          name substation_id  nominal_v  high_voltage_limit  low_voltage_limit
    id
    S1VL1                 S1      225.0               240.0              220.0
    S1VL2                 S1      400.0               440.0              390.0
    S2VL1                 S2      400.0               440.0              390.0
    S3VL1                 S3      400.0               440.0              390.0
    S4VL1                 S4      400.0               440.0              390.0

    >>> net.reduce(v_min=400)
    >>> net.get_voltage_levels()
          name substation_id  nominal_v  high_voltage_limit  low_voltage_limit
    id
    S1VL2                 S1      400.0               440.0              390.0
    S2VL1                 S2      400.0               440.0              390.0
    S3VL1                 S3      400.0               440.0              390.0
    S4VL1                 S4      400.0               440.0              390.0

For the next example we will keep voltage level S1VL1 with a depth of 1.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> net = pp.network.create_four_substations_node_breaker_network()
    >>> net.get_voltage_levels()
          name substation_id  nominal_v  high_voltage_limit  low_voltage_limit
    id
    S1VL1                 S1      225.0               240.0              220.0
    S1VL2                 S1      400.0               440.0              390.0
    S2VL1                 S2      400.0               440.0              390.0
    S3VL1                 S3      400.0               440.0              390.0
    S4VL1                 S4      400.0               440.0              390.0
    >>> net.reduce(vl_depths=[['S1VL1', 1]])
    >>> net.get_voltage_levels()
          name substation_id  nominal_v  high_voltage_limit  low_voltage_limit
    id
    S1VL1                 S1      225.0               240.0              220.0
    S1VL2                 S1      400.0               440.0              390.0

S1VL1 is connected to S1VL2 by the transformer TWT, so it is kept after the network reduction.
It is the only voltage level connected to S1VL1 by one branch.

the parameter "ids" can be used to specify the exact voltage levels that will be kept
