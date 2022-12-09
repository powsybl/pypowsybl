Flow decomposition
==================

.. module:: pypowsybl.flowdecomposition

The flow decomposition module allows to decompose active flows on cross-border relevant network element with contingency (XNEC) based on the ACER methodology.  
This python interface is based on the java implementation in the `PowSyBl ENTSO-E repository <https://github.com/powsybl/powsybl-entsoe>`_.  

This simple version of flow decomposition will evolve with next versions of flow decomposition Java version.  
Here are the assumptions that we made:

- XNEC = lines specified by the user
- zone = country  
- country GSK  
- no HVDC management  

Run a flow decomposition
------------------------

The general idea of this API is to create a decomposition object.
Then, you can define contingencies if necessary.
Then, you can define XNE and XNEC. XNEC definition requires pre-defined contingencies.
Finally, you can run the flow decomposition with some flow decomposition and/or load flow parameters.

To do so, you can use the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    create_decomposition
    FlowDecomposition.add_single_element_contingencies
    FlowDecomposition.add_multiple_elements_contingency
    FlowDecomposition.add_monitored_elements
    FlowDecomposition.add_precontingency_monitored_elements
    FlowDecomposition.add_postcontingency_monitored_elements
    FlowDecomposition.run


Parameters
----------

The execution of the flowdecomposition can be customized using flowdecomposition parameters.

.. autosummary::
   :nosignatures:

    Parameters

.. include it in the toctree
.. toctree::
   :hidden:

   flowdecomposition/parameters
