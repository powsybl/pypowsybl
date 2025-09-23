Running a RAO
===========================

The RAO is currently in **beta** version.

You can use the module :mod:`pypowsybl.rao` in order to perform a remedial actions optimization on a network.

For detailed documentation of involved classes and methods, please refer to the :mod:`API reference <pypowsybl.rao>`.

For detailed documentation of the Powsybl OpenRAO please refer to the `PowSyBl RAO documentation <https://powsybl.readthedocs.io/projects/openrao/en/stable/>`_.

Inputs for a RAO
----------------
To run a RAO you need:

- a network in a PyPowsybl supported exchange format
- a CRAC file (Contingency list, Remedial Actions and additional Constraints) in json
- optionally a GLSK file (Generation and Load Shift Keys) in json
- optionally a parameters file, in json, allowing to override the RAO parameters

Here is a code example of how to configure and run the RAO:

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> import pypowsybl as pp
    >>> from pypowsybl.rao import Parameters as RaoParameters
    >>>
    >>> network =  pp.network.load(str(DATA_DIR.joinpath("rao/rao_network.uct")))
    >>> parameters = RaoParameters()
    >>> parameters.load_from_file_source(str(DATA_DIR.joinpath("rao/rao_parameters.json")))
    >>> rao_runner = pp.rao.create_rao()
    >>> rao_runner.set_crac_file_source(network, str(DATA_DIR.joinpath("rao/rao_crac.json")))
    >>> rao_runner.set_glsk_file_source(network, str(DATA_DIR.joinpath("rao/rao_glsk.xml")))
    >>> rao_result = rao_runner.run(network, parameters)
    >>> rao_result.status()
    <RaoComputationStatus.DEFAULT: 0>

Monitoring API
--------------

Rao monitoring can run through the following API using an already produced rao result :

    >>> result_with_voltage_monitoring = rao_runner.run_voltage_monitoring(network, rao_result)
    >>> result_with_angle_monitoring = rao_runner.run_angle_monitoring(network, rao_result)

The returned rao result object are the original result enhanced with voltage or angle monitoring data.

Outputs of a RAO
----------------

The RAO results can be explored through the `RaoResult` object returned by the run function of the rao runner.
Results are exposed in pandas dataframe format using the following API.

Retrieve the global result status (can be DEFAULT, FAILURE or PARTIAL_FAILURE):

.. doctest::

    >>> rao_result.status()
    <RaoComputationStatus.DEFAULT: 0>

Retrieve the result for the flow CNEC:

.. doctest::

    >>> flow_cnec = rao_result.get_flow_cnec_results()
    >>> flow_cnec.columns
    Index(['cnec_id', 'optimized_instant', 'contingency', 'side', 'flow', 'margin',
           'relative_margin', 'commercial_flow', 'loop_flow', 'ptdf_zonal_sum'],
          dtype='object')

Each line represent a flow cnec result for an optimized instant and a contingency context.

When monitoring has been executed, voltage and angle cnec results can also be retrieved through pandas dataframes:

.. doctest::

    >>> voltage_cnec = rao_result.get_voltage_cnec_results()
    >>> voltage_cnec.columns
    Index(['cnec_id', 'optimized_instant', 'contingency', 'side', 'min_voltage',
           'max_voltage', 'margin'],
          dtype='object')
    >>> angle_cnecs = rao_result.get_angle_cnec_results()
    >>> angle_cnecs.columns
    Index(['cnec_id', 'optimized_instant', 'contingency', 'angle', 'margin'], dtype='object')


Remedial action results are also available in a pandas dataframe :

.. doctest::

    >>> ra_results = rao_result.get_remedial_action_results()
    >>> ra_results.columns
    Index(['remedial_action_id', 'optimized_instant', 'contingency'], dtype='object')

For each remedial action, optimized instant and a contingency (if applicable) the activation information is available.
For range actions the optimized tap is also available for PstRangeAction and optimized set point for all other RangeActions.
Optimized tap and optimized set point are set to NaN when not applicable (not a range action).

It is possible to get the results of activated remedial actions for a specific type of remedial action only.

For network actions:

.. doctest::

    >>> ra_results = rao_result.get_network_action_results()
    >>> ra_results.columns
    Index(['remedial_action_id', 'optimized_instant', 'contingency'], dtype='object')

For PST range actions:

.. doctest::

    >>> ra_results = rao_result.get_pst_range_action_results()
    >>> ra_results.columns
    Index(['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_tap'], dtype='object')

For other non-PST range actions:

.. doctest::

    >>> ra_results = rao_result.get_range_action_results()
    >>> ra_results.columns
    Index(['remedial_action_id', 'optimized_instant', 'contingency', 'optimized_set_point'], dtype='object')

Finally cost results can also be retrieved. Generic cost results are available in a dataframe :

.. doctest::

    >>> cost_results = rao_result.get_cost_results()
    >>> cost_results.columns
    Index(['functional_cost', 'virtual_cost', 'cost'], dtype='object')

With optimized instant as an index, functional cost, virtual cost and the sum of the two as cost for each optimized instant are available.
Details for virtual cost can also be queried for a given virtual cost with the list of virtual cost names available.
Cost for a given virtual cost name is returned as a pandas dataframe with cost value for each instant.

.. doctest::

    >>> virtual_cost_names = rao_result.get_virtual_cost_names()
    >>> virtual_cost_names
    ['sensitivity-failure-cost']
    >>> sensi_cost = rao_result.get_virtual_cost_results('sensitivity-failure-cost')
    >>> sensi_cost.index
    Index(['initial', 'preventive', 'outage', 'auto', 'curative'], dtype='object', name='optimized_instant')
    >>> sensi_cost.columns
    Index(['sensitivity-failure-cost'], dtype='object')
    >>> sensi_cost.loc['curative', 'sensitivity-failure-cost']
    np.float64(0.0)

The 'RaoResult' object can also be serialized to json:

.. doctest::

    >>> rao_result.serialize(str(DATA_DIR.joinpath("rao/results.json")))
