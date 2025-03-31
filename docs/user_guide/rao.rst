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


Outputs of a RAO
----------------

The RAO result is readable in a `RaoResult` object that can be serialized in json. It contains the optimal list of remedial actions to be applied in both basecase and after contingencies provided in the input CRAC file.
