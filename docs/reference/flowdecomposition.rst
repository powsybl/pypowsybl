Flow decomposition
==================

.. module:: pypowsybl.flowdecomposition

The flow decomposition module allows to decompose active flows on cross-border relevant network element with contingency (XNEC) based on the ACER methodology.
This python interface is based on the java implementation in the `PowSyBl ENTSO-E repository <https://github.com/powsybl/powsybl-entsoe>`_.

This simple version of flow decomposition will evolve with next versions of flow decomposition Java version.
Here are the assumptions that we made:
- no contingency management
- XNEC = interconnexion branch
- zone = country
- country GSK
- no HVDC management

Run a flow decomposition
------------------------

You can run a flow decomposition using the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    run
