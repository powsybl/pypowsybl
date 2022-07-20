Running a flow decomposition
============================

.. testsetup:: *

    import pathlib
    import pandas as pd

    import pypowsybl as pp
    
    pd.options.display.max_columns = None
    pd.options.display.expand_frame_repr = False
    import os
    cwd = os.getcwd()
    PROJECT_DIR = pathlib.Path(cwd).parent
    DATA_DIR = PROJECT_DIR / 'data'

You can use the module :mod:`pypowsybl.flowdecomposition` in order to run load flows on networks.
Please check out the examples below.

For detailed documentation of involved classes and methods, please refer to the :mod:`API reference <pypowsybl.flowdecomposition>`.

Start by importing the module:

.. code-block:: python

   import pypowsybl as pp

Flow decomposition
------------------

To performa a flow decomposition, you need at least a network.
The flow decomposition computer will return a dataframe containing the flow decomposition and the reference values.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> flow_decomposition_dataframe = pp.flowdecomposition.run(network)
    >>> flow_decomposition_dataframe
                   Allocated Flow  PST Flow  Loop Flow from BE  Loop Flow from FR  Reference AC Flow  Reference DC Flow
    XNEC id                                                                                                          
    NHV1_NHV2_1             0.0       0.0              300.0                0.0         302.444049              300.0
    NHV1_NHV2_2             0.0       0.0              300.0                0.0         302.444049              300.0

Here is another example with a more complex network containing a phase-shifting transformer (PST).

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR / 'NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct'))
    >>> flow_decomposition_dataframe = pp.flowdecomposition.run(network)
    >>> flow_decomposition_dataframe
                             Allocated Flow    PST Flow  Loop Flow from BE  Loop Flow from FR  Reference AC Flow  Reference DC Flow
    XNEC id                                                                                                                    
    FGEN  11 BLOAD 11 1       29.015809  163.652703          -2.007905          -2.007905         192.390656         188.652703
    FGEN  11 BLOAD 12 1      -87.047428  163.652703           6.023714           6.023714         -76.189072         -88.652703


This simple version of flow decomposition will evolve with next versions of flow decomposition Java version.